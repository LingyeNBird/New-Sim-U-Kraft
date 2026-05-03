package com.xiaoliang.simukraft.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

import javax.annotation.Nonnull;

/**
 * 物流系统核心数据，持久化到 world/data/simukraft_logistics.dat
 */
public class LogisticsData extends SavedData {
    private static final String DATA_NAME = "simukraft_logistics";

    private final Map<UUID, Warehouse> warehouses = new HashMap<>();
    private final Map<UUID, LogisticsClient> clients = new HashMap<>();

    private static ServerLevel getOverworld(ServerLevel level) {
        ServerLevel overworld = level.getServer().getLevel(Objects.requireNonNull(Level.OVERWORLD));
        return overworld != null ? overworld : level;
    }

    // ══════════════════════════════════════
    //  Warehouse（仓库）
    // ══════════════════════════════════════

    public static class Warehouse {
        private final UUID warehouseId;
        private final BlockPos blockPos;
        private final UUID cityId;
        private UUID npcUuid;  // 仓库管理员，可空
        private final List<BlockPos> containerPositions = new ArrayList<>();
        private final List<LogisticsChannel> channels = new ArrayList<>();

        public Warehouse(UUID warehouseId, BlockPos blockPos, UUID cityId) {
            this.warehouseId = warehouseId;
            this.blockPos = blockPos;
            this.cityId = cityId;
        }

        public UUID getWarehouseId() { return warehouseId; }
        public BlockPos getBlockPos() { return blockPos; }
        public UUID getCityId() { return cityId; }
        public UUID getNpcUuid() { return npcUuid; }
        public void setNpcUuid(UUID npcUuid) { this.npcUuid = npcUuid; }
        public boolean hasNpc() { return npcUuid != null; }
        public List<BlockPos> getContainerPositions() { return containerPositions; }
        public List<LogisticsChannel> getChannels() { return channels; }
        public boolean hasContainers() { return !containerPositions.isEmpty(); }

        public LogisticsChannel getChannel(UUID channelId) {
            return channels.stream().filter(c -> c.channelId.equals(channelId)).findFirst().orElse(null);
        }

        public CompoundTag serialize() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("warehouseId", Objects.requireNonNull(warehouseId));
            tag.putLong("blockPos", blockPos.asLong());
            tag.putUUID("cityId", Objects.requireNonNull(cityId));
            if (npcUuid != null) tag.putUUID("npcUuid", npcUuid);

            ListTag containersTag = new ListTag();
            for (BlockPos pos : containerPositions) {
                containersTag.add(Objects.requireNonNull(NbtUtils.writeBlockPos(Objects.requireNonNull(pos))));
            }
            tag.put("containers", containersTag);

            ListTag channelsTag = new ListTag();
            for (LogisticsChannel channel : channels) {
                channelsTag.add(channel.serialize());
            }
            tag.put("channels", channelsTag);
            return tag;
        }

        public static Warehouse deserialize(CompoundTag tag) {
            Warehouse w = new Warehouse(
                    tag.getUUID("warehouseId"),
                    BlockPos.of(tag.getLong("blockPos")),
                    tag.getUUID("cityId")
            );
            if (tag.hasUUID("npcUuid")) w.npcUuid = tag.getUUID("npcUuid");

            ListTag containersTag = tag.getList("containers", Tag.TAG_COMPOUND);
            for (int i = 0; i < containersTag.size(); i++) {
                w.containerPositions.add(Objects.requireNonNull(
                        NbtUtils.readBlockPos(Objects.requireNonNull(containersTag.getCompound(i)))
                ));
            }

            ListTag channelsTag = tag.getList("channels", Tag.TAG_COMPOUND);
            for (int i = 0; i < channelsTag.size(); i++) {
                w.channels.add(LogisticsChannel.deserialize(channelsTag.getCompound(i)));
            }
            return w;
        }
    }

    // ══════════════════════════════════════
    //  LogisticsClient（客户端端口）
    // ══════════════════════════════════════

    public static class LogisticsClient {
        private final UUID clientId;
        private final BlockPos blockPos;
        private final UUID cityId;
        private String name; // 客户端自定义名称
        private final List<BlockPos> portPositions = new ArrayList<>();

        public LogisticsClient(UUID clientId, BlockPos blockPos, UUID cityId) {
            this.clientId = clientId;
            this.blockPos = blockPos;
            this.cityId = cityId;
            this.name = ""; // 默认为空，显示时使用坐标
        }

        public UUID getClientId() { return clientId; }
        public BlockPos getBlockPos() { return blockPos; }
        public UUID getCityId() { return cityId; }
        public List<BlockPos> getPortPositions() { return portPositions; }
        public boolean hasPorts() { return !portPositions.isEmpty(); }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public boolean hasCustomName() { return name != null && !name.isBlank(); }

        /**
         * 获取显示名称，如果有自定义名称则返回自定义名称，否则返回坐标
         */
        public String getDisplayName() {
            if (hasCustomName()) {
                return name;
            }
            return blockPos.getX() + "," + blockPos.getY() + "," + blockPos.getZ();
        }

        public CompoundTag serialize() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("clientId", Objects.requireNonNull(clientId));
            tag.putLong("blockPos", blockPos.asLong());
            tag.putUUID("cityId", Objects.requireNonNull(cityId));
            if (name != null && !name.isEmpty()) {
                tag.putString("name", Objects.requireNonNull(name));
            }

            ListTag portsTag = new ListTag();
            for (BlockPos pos : portPositions) {
                portsTag.add(Objects.requireNonNull(NbtUtils.writeBlockPos(Objects.requireNonNull(pos))));
            }
            tag.put("ports", portsTag);
            return tag;
        }

        public static LogisticsClient deserialize(CompoundTag tag) {
            LogisticsClient c = new LogisticsClient(
                    tag.getUUID("clientId"),
                    BlockPos.of(tag.getLong("blockPos")),
                    tag.getUUID("cityId")
            );
            if (tag.contains("name")) {
                c.name = tag.getString("name");
            }
            ListTag portsTag = tag.getList("ports", Tag.TAG_COMPOUND);
            for (int i = 0; i < portsTag.size(); i++) {
                c.portPositions.add(Objects.requireNonNull(
                        NbtUtils.readBlockPos(Objects.requireNonNull(portsTag.getCompound(i)))
                ));
            }
            return c;
        }
    }

    // ══════════════════════════════════════
    //  LogisticsChannel（物流频道）
    // ══════════════════════════════════════

    public enum ChannelDirection { SEND, RECEIVE }

    public static class LogisticsChannel {
        private final UUID channelId;
        private String name;
        private final UUID targetClientId;
        private final ChannelDirection direction;
        private final List<ItemStack> itemFilters = new ArrayList<>();
        private boolean enabled = true;

        public LogisticsChannel(UUID channelId, String name, UUID targetClientId, ChannelDirection direction) {
            this.channelId = channelId;
            this.name = name;
            this.targetClientId = targetClientId;
            this.direction = direction;
        }

        public UUID getChannelId() { return channelId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public UUID getTargetClientId() { return targetClientId; }
        public ChannelDirection getDirection() { return direction; }
        public List<ItemStack> getItemFilters() { return itemFilters; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public CompoundTag serialize() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("channelId", Objects.requireNonNull(channelId));
            tag.putString("name", Objects.requireNonNull(name));
            tag.putUUID("targetClientId", Objects.requireNonNull(targetClientId));
            tag.putString("direction", Objects.requireNonNull(direction.name()));
            tag.putBoolean("enabled", enabled);

            ListTag filtersTag = new ListTag();
            for (ItemStack item : itemFilters) {
                CompoundTag itemTag = new CompoundTag();
                ResourceLocation key = ForgeRegistries.ITEMS.getKey(item.getItem());
                if (key != null) {
                    itemTag.putString("item", Objects.requireNonNull(key.toString()));
                }
                filtersTag.add(itemTag);
            }
            tag.put("itemFilters", filtersTag);
            return tag;
        }

        public static LogisticsChannel deserialize(CompoundTag tag) {
            LogisticsChannel ch = new LogisticsChannel(
                    tag.getUUID("channelId"),
                    tag.getString("name"),
                    tag.getUUID("targetClientId"),
                    ChannelDirection.valueOf(tag.getString("direction"))
            );
            ch.enabled = tag.getBoolean("enabled");

            ListTag filtersTag = tag.getList("itemFilters", Tag.TAG_COMPOUND);
            for (int i = 0; i < filtersTag.size(); i++) {
                String itemId = Objects.requireNonNull(filtersTag.getCompound(i).getString("item"));
                ResourceLocation itemLocation = ResourceLocation.tryParse(itemId);
                Item item = itemLocation == null ? null : ForgeRegistries.ITEMS.getValue(itemLocation);
                if (item != null) {
                    ch.itemFilters.add(new ItemStack(item));
                }
            }
            return ch;
        }
    }

    // ══════════════════════════════════════
    //  SavedData 生命周期
    // ══════════════════════════════════════

    public LogisticsData() {}

    public static LogisticsData get(ServerLevel level) {
        ServerLevel overworld = getOverworld(level);
        return overworld.getDataStorage().computeIfAbsent(LogisticsData::load, LogisticsData::new, DATA_NAME);
    }

    // ── 仓库操作 ──

    public Warehouse createWarehouse(BlockPos blockPos, UUID cityId) {
        Warehouse w = new Warehouse(UUID.randomUUID(), blockPos, cityId);
        warehouses.put(w.getWarehouseId(), w);
        setDirty();
        return w;
    }

    public Warehouse getWarehouseByBlockPos(BlockPos pos) {
        return warehouses.values().stream().filter(w -> w.blockPos.equals(pos)).findFirst().orElse(null);
    }

    public Warehouse getWarehouse(UUID warehouseId) { return warehouses.get(warehouseId); }
    public Collection<Warehouse> getAllWarehouses() { return warehouses.values(); }

    public void removeWarehouse(UUID warehouseId) {
        warehouses.remove(warehouseId);
        setDirty();
    }

    // ── 客户端操作 ──

    public LogisticsClient createClient(BlockPos blockPos, UUID cityId) {
        LogisticsClient c = new LogisticsClient(UUID.randomUUID(), blockPos, cityId);
        clients.put(c.getClientId(), c);
        setDirty();
        return c;
    }

    public LogisticsClient getClientByBlockPos(BlockPos pos) {
        return clients.values().stream().filter(c -> c.blockPos.equals(pos)).findFirst().orElse(null);
    }

    public LogisticsClient getClient(UUID clientId) { return clients.get(clientId); }
    public Collection<LogisticsClient> getAllClients() { return clients.values(); }

    public void removeClient(UUID clientId) {
        // 同时清理所有引用此客户端的频道
        for (Warehouse w : warehouses.values()) {
            w.channels.removeIf(ch -> ch.targetClientId.equals(clientId));
        }
        clients.remove(clientId);
        setDirty();
    }

    // ── 按城市查询 ──

    public List<Warehouse> getWarehousesByCity(UUID cityId) {
        return warehouses.values().stream().filter(w -> w.cityId.equals(cityId)).toList();
    }

    public List<LogisticsClient> getClientsByCity(UUID cityId) {
        return clients.values().stream().filter(c -> c.cityId.equals(cityId)).toList();
    }

    // ── 序列化 ──

    @Override
    public CompoundTag save(@Nonnull CompoundTag tag) {
        ListTag warehousesTag = new ListTag();
        for (Warehouse w : warehouses.values()) {
            warehousesTag.add(w.serialize());
        }
        tag.put("warehouses", warehousesTag);

        ListTag clientsTag = new ListTag();
        for (LogisticsClient c : clients.values()) {
            clientsTag.add(c.serialize());
        }
        tag.put("clients", clientsTag);
        return tag;
    }

    public static LogisticsData load(CompoundTag tag) {
        LogisticsData data = new LogisticsData();

        ListTag warehousesTag = tag.getList("warehouses", Tag.TAG_COMPOUND);
        for (int i = 0; i < warehousesTag.size(); i++) {
            Warehouse w = Warehouse.deserialize(warehousesTag.getCompound(i));
            data.warehouses.put(w.getWarehouseId(), w);
        }

        ListTag clientsTag = tag.getList("clients", Tag.TAG_COMPOUND);
        for (int i = 0; i < clientsTag.size(); i++) {
            LogisticsClient c = LogisticsClient.deserialize(clientsTag.getCompound(i));
            data.clients.put(c.getClientId(), c);
        }
        return data;
    }
}
