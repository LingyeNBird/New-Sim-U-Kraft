package com.xiaoliang.simukraft.network;
import com.xiaoliang.simukraft.building.CommercialBuildingConfig;
import com.xiaoliang.simukraft.building.CommercialBuildingManager;
import com.xiaoliang.simukraft.utils.MoneyManager;
import com.xiaoliang.simukraft.world.CommercialHiredData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 玩家出售物品给NPC的数据包
 * 支持建材商店库存管理和收购限制
 */
public class SellToNPCPacket {
    private final BlockPos controlBoxPos;
    private final String buildingFileName;
    private final Map<String, Integer> itemsToSell;
    private final double totalPrice;

    public SellToNPCPacket(BlockPos controlBoxPos, String buildingFileName, Map<String, Integer> itemsToSell, double totalPrice) {
        this.controlBoxPos = controlBoxPos;
        this.buildingFileName = buildingFileName;
        this.itemsToSell = itemsToSell;
        this.totalPrice = totalPrice;
    }

    public static void encode(SellToNPCPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(Objects.requireNonNull(packet.controlBoxPos));
        buf.writeUtf(Objects.requireNonNull(packet.buildingFileName));
        buf.writeInt(packet.itemsToSell.size());
        for (Map.Entry<String, Integer> entry : packet.itemsToSell.entrySet()) {
            buf.writeUtf(Objects.requireNonNull(entry.getKey()));
            buf.writeInt(entry.getValue());
        }
        buf.writeDouble(packet.totalPrice);
    }

    public static SellToNPCPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = Objects.requireNonNull(buf.readBlockPos());
        String buildingFileName = Objects.requireNonNull(buf.readUtf());
        int size = buf.readInt();
        Map<String, Integer> items = new HashMap<>();
        for (int i = 0; i < size; i++) {
            String itemId = Objects.requireNonNull(buf.readUtf());
            int count = buf.readInt();
            items.put(itemId, count);
        }
        double price = buf.readDouble();
        return new SellToNPCPacket(pos, buildingFileName, items, price);
    }

    public static void handle(SellToNPCPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            MinecraftServer server = player.getServer();
            if (server == null) return;

            // 获取建筑配置
            CommercialBuildingConfig config = CommercialBuildingManager.getConfig(packet.buildingFileName);
            if (config == null) {
                player.sendSystemMessage(Objects.requireNonNull(
                        Component.translatable("message.simukraft.commercial.config_not_found")));
                return;
            }

            // 检查商店模式是否支持出售
            if (config.getShopMode() != CommercialBuildingConfig.ShopMode.PLAYER_SELL &&
                config.getShopMode() != CommercialBuildingConfig.ShopMode.MIXED) {
                player.sendSystemMessage(Objects.requireNonNull(
                        Component.translatable("message.simukraft.commercial.shop_not_support_sell")));
                return;
            }

            // 加载库存数据（统一使用 CommercialHiredData）
            CommercialHiredData.loadStockData(server);

            // 验证物品和价格
            double calculatedTotal = 0;
            Map<String, Integer> validItems = new HashMap<>();

            for (Map.Entry<String, Integer> entry : packet.itemsToSell.entrySet()) {
                String itemId = entry.getKey();
                int count = entry.getValue();

                // 查找收购配置
                CommercialBuildingConfig.BuyTradeItem buyTrade = findBuyTrade(config, itemId);
                if (buyTrade == null) {
                    player.sendSystemMessage(Objects.requireNonNull(
                            Component.translatable("message.simukraft.commercial.shop_not_buy_item", itemId)));
                    return;
                }

                // 检查玩家是否有足够的物品
                int playerHas = countPlayerItems(player, itemId);
                if (playerHas < count) {
                    player.sendSystemMessage(Objects.requireNonNull(
                            Component.translatable("message.simukraft.commercial.not_enough_items", itemId, count)));
                    return;
                }

                // 检查库存限制（统一使用 CommercialHiredData）
                CommercialHiredData.StockInfo stockInfo = CommercialHiredData.getStock(packet.controlBoxPos, itemId);
                int currentStock = stockInfo != null ? stockInfo.getCurrentStock() : 0;
                // BuyTradeItem 使用 maxBuyAmount 作为库存限制
                int maxBuyAmount = stockInfo != null && stockInfo.getMaxBuyAmount() > 0 ? stockInfo.getMaxBuyAmount() : buyTrade.getMaxBuyAmount();
                int dailyBoughtAmount = stockInfo != null ? stockInfo.getDailyBoughtAmount() : 0;
                // 计算最大库存（每组64个）
                int maxStock = maxBuyAmount * 64;

                int stacks = count / 64; // 转换为组数
                if (currentStock + count > maxStock || dailyBoughtAmount + stacks > maxBuyAmount) {
                    int remainingAmount = Math.min((maxStock - currentStock) / 64, maxBuyAmount - dailyBoughtAmount);
                    player.sendSystemMessage(Objects.requireNonNull(
                            Component.translatable("message.simukraft.commercial.stock_full_or_limit", itemId, remainingAmount)));
                    return;
                }

                // 计算价格（出售价格是收购价的85%，按组计算）
                double sellPrice = buyTrade.getSellPrice();
                calculatedTotal += sellPrice * stacks;
                validItems.put(itemId, count);
            }

            // 验证总价
            if (Math.abs(calculatedTotal - packet.totalPrice) > 0.01) {
                player.sendSystemMessage(Objects.requireNonNull(
                        Component.translatable("message.simukraft.commercial.price_verification_failed")));
                return;
            }

            // 扣除玩家物品并增加商店库存
            for (Map.Entry<String, Integer> entry : validItems.entrySet()) {
                String itemId = entry.getKey();
                int count = entry.getValue();

                removeItemsFromPlayer(player, itemId, count);

                // 增加库存（统一使用 CommercialHiredData）
                CommercialHiredData.StockInfo stockInfo = CommercialHiredData.getStock(packet.controlBoxPos, itemId);
                int currentStock = stockInfo != null ? stockInfo.getCurrentStock() : 0;
                int maxStock = stockInfo != null && stockInfo.getMaxStock() > 0 ? stockInfo.getMaxStock() : 640;
                int dailyBoughtAmount = stockInfo != null ? stockInfo.getDailyBoughtAmount() : 0;
                int maxBuyAmount = stockInfo != null && stockInfo.getMaxBuyAmount() > 0 ? stockInfo.getMaxBuyAmount() : maxStock / 64;

                int newStock = currentStock + count;
                int newDailyBoughtAmount = dailyBoughtAmount + count / 64;
                CommercialHiredData.updateStockFull(packet.controlBoxPos, itemId, newStock, maxStock,
                        server.overworld().getDayTime(), newDailyBoughtAmount, maxBuyAmount);
            }

            // 保存数据
            CommercialHiredData.saveStockData(server);

            // 给玩家钱
            MoneyManager.addMoney(player, calculatedTotal);

            // 发送成功消息（保留两位小数）
            player.sendSystemMessage(Objects.requireNonNull(
                    Component.translatable(
                            "message.simukraft.commercial.sell_success",
                            String.format(java.util.Locale.US, "%.2f", calculatedTotal)
                    )));

            // 播放音效
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                Objects.requireNonNull(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP),
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
        });

        ctx.get().setPacketHandled(true);
    }

    private static CommercialBuildingConfig.BuyTradeItem findBuyTrade(CommercialBuildingConfig config, String itemId) {
        for (CommercialBuildingConfig.BuyTradeItem buyTrade : config.getBuyTrades()) {
            if (buyTrade.getItemId().equals(itemId)) {
                return buyTrade;
            }
        }
        return null;
    }

    private static int countPlayerItems(Player player, String itemId) {
        int count = 0;
        String safeItemId = Objects.requireNonNull(itemId);

        // 解析目标物品ID
        var resourceLocation = net.minecraft.resources.ResourceLocation.tryParse(safeItemId);
        if (resourceLocation == null) return 0;

        var itemRegistry = player.level().registryAccess()
                .registry(Objects.requireNonNull(net.minecraft.core.registries.Registries.ITEM))
                .orElse(null);
        if (itemRegistry == null) return 0;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                var stackKey = itemRegistry.getKey(Objects.requireNonNull(stack.getItem()));
                if (stackKey != null && stackKey.equals(resourceLocation)) {
                    count += stack.getCount();
                }
            }
        }
        return count;
    }

    private static void removeItemsFromPlayer(Player player, String itemId, int count) {
        String safeItemId = Objects.requireNonNull(itemId);
        var resourceLocation = net.minecraft.resources.ResourceLocation.tryParse(safeItemId);
        if (resourceLocation == null) return;

        var itemRegistry = player.level().registryAccess()
                .registry(Objects.requireNonNull(net.minecraft.core.registries.Registries.ITEM))
                .orElse(null);
        if (itemRegistry == null) return;

        int remaining = count;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                var stackKey = itemRegistry.getKey(Objects.requireNonNull(stack.getItem()));
                if (stackKey != null && stackKey.equals(resourceLocation)) {
                    int toRemove = Math.min(remaining, stack.getCount());
                    stack.shrink(toRemove);
                    remaining -= toRemove;
                }
            }
        }
    }
}
