package com.xiaoliang.simukraft.network;

import com.xiaoliang.simukraft.client.gui.CommercialClientData;
import com.xiaoliang.simukraft.world.CommercialHiredData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 同步建材商店库存数据包
 * 服务器 -> 客户端
 */
public class SyncBuildingMaterialStockPacket {
    private final BlockPos pos;
    private final Map<String, StockData> stockDataMap;
    private final long gameTime;

    public static class StockData {
        public final String itemId;
        public final int currentStock;
        public final int maxStock;
        public final int restockAmount;
        public final long lastRestockTime;

        public StockData(String itemId, int currentStock, int maxStock, int restockAmount, long lastRestockTime) {
            this.itemId = itemId;
            this.currentStock = currentStock;
            this.maxStock = maxStock;
            this.restockAmount = restockAmount;
            this.lastRestockTime = lastRestockTime;
        }
    }

    public SyncBuildingMaterialStockPacket(BlockPos pos, Map<String, StockData> stockDataMap, long gameTime) {
        this.pos = pos;
        this.stockDataMap = stockDataMap;
        this.gameTime = gameTime;
    }

    public static void encode(SyncBuildingMaterialStockPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(Objects.requireNonNull(packet.pos));
        buf.writeLong(packet.gameTime);
        buf.writeInt(packet.stockDataMap.size());
        for (Map.Entry<String, StockData> entry : packet.stockDataMap.entrySet()) {
            buf.writeUtf(Objects.requireNonNull(entry.getKey()));
            StockData data = entry.getValue();
            buf.writeUtf(Objects.requireNonNull(data.itemId));
            buf.writeInt(data.currentStock);
            buf.writeInt(data.maxStock);
            buf.writeInt(data.restockAmount);
            buf.writeLong(data.lastRestockTime);
        }
    }

    public static SyncBuildingMaterialStockPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = Objects.requireNonNull(buf.readBlockPos());
        long gameTime = buf.readLong();
        int size = buf.readInt();
        Map<String, StockData> stockDataMap = new HashMap<>();
        for (int i = 0; i < size; i++) {
            String key = Objects.requireNonNull(buf.readUtf());
            String itemId = Objects.requireNonNull(buf.readUtf());
            int currentStock = buf.readInt();
            int maxStock = buf.readInt();
            int restockAmount = buf.readInt();
            long lastRestockTime = buf.readLong();
            stockDataMap.put(key, new StockData(itemId, currentStock, maxStock, restockAmount, lastRestockTime));
        }
        return new SyncBuildingMaterialStockPacket(pos, stockDataMap, gameTime);
    }

    public static void handle(SyncBuildingMaterialStockPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 客户端处理
            if (ctx.get().getDirection().getReceptionSide().isClient()) {
                // 更新客户端库存数据
                for (Map.Entry<String, StockData> entry : packet.stockDataMap.entrySet()) {
                    StockData data = entry.getValue();
                    // 使用 CommercialClientData 存储库存数据 - 使用完整的构造函数
                    CommercialHiredData.StockInfo stockInfo = new CommercialHiredData.StockInfo(
                        data.itemId,
                        data.currentStock,
                        data.maxStock,
                        data.lastRestockTime,
                        0, // dailyBoughtAmount
                        data.restockAmount // maxBuyAmount (这里用restockAmount作为maxBuyAmount)
                    );
                    CommercialClientData.updateStock(packet.pos, data.itemId, stockInfo);
                }

                // 通知当前打开的购买界面更新库存显示
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.screen instanceof com.xiaoliang.simukraft.client.gui.CommercialBuyScreen) {
                    com.xiaoliang.simukraft.client.gui.CommercialBuyScreen screen =
                        (com.xiaoliang.simukraft.client.gui.CommercialBuyScreen) mc.screen;
                    screen.onStockSyncReceived();
                } else if (mc.screen instanceof com.xiaoliang.simukraft.client.gui.CommercialSellScreen) {
                    com.xiaoliang.simukraft.client.gui.CommercialSellScreen screen =
                        (com.xiaoliang.simukraft.client.gui.CommercialSellScreen) mc.screen;
                    screen.onStockSyncReceived();
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    /**
     * 发送库存同步包给玩家
     */
    public static void sendToPlayer(ServerPlayer player, BlockPos pos) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        // 加载数据（统一使用 CommercialHiredData）
        CommercialHiredData.loadStockData(server);

        Map<String, CommercialHiredData.StockInfo> stockMap = CommercialHiredData.getAllStockAtPos(pos);
        if (stockMap == null || stockMap.isEmpty()) {
            // 如果没有数据，尝试初始化
            com.xiaoliang.simukraft.building.CommercialBuildingConfig config =
                com.xiaoliang.simukraft.building.CommercialBuildingManager.getConfig("jcsd");
            if (config != null) {
                for (com.xiaoliang.simukraft.building.CommercialBuildingConfig.TradeItem trade : config.getTrades()) {
                    int initialStock = trade.getMaxStock() / 2;
                    int maxStock = trade.getMaxStock();
                    // 使用 updateStockFull 确保所有字段都被正确初始化
                    CommercialHiredData.updateStockFull(pos, trade.getItemId(), initialStock, maxStock,
                            server.overworld().getDayTime(), 0, maxStock / 64);
                }
                stockMap = CommercialHiredData.getAllStockAtPos(pos);
            }
        }

        if (stockMap == null || stockMap.isEmpty()) return;

        Map<String, StockData> syncMap = new HashMap<>();
        for (Map.Entry<String, CommercialHiredData.StockInfo> entry : stockMap.entrySet()) {
            CommercialHiredData.StockInfo stock = entry.getValue();
            syncMap.put(entry.getKey(), new StockData(
                stock.getItemId(),
                stock.getCurrentStock(),
                stock.getMaxStock() > 0 ? stock.getMaxStock() : stock.getCurrentStock(),
                64,
                stock.getLastRestockTime()
            ));
        }

        SyncBuildingMaterialStockPacket packet = new SyncBuildingMaterialStockPacket(
            pos,
            syncMap,
            server.overworld().getDayTime()
        );

        NetworkManager.INSTANCE.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }
}
