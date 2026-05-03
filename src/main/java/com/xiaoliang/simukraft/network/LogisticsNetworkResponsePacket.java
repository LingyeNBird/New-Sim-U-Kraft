package com.xiaoliang.simukraft.network;

import com.xiaoliang.simukraft.client.gui.ChannelCreateScreen;
import com.xiaoliang.simukraft.client.gui.LogisticsClientData;
import com.xiaoliang.simukraft.client.gui.LogisticsNetworkScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * S→C 物流网络数据响应
 * 服务器返回仓库和客户端列表数据
 */
@SuppressWarnings({"null", "unused"})
public class LogisticsNetworkResponsePacket {
    private final BlockPos warehouseBlockPos;
    private final UUID warehouseId;
    private final List<BlockPos> warehouseContainers;
    private final List<ClientData> clients;
    private final List<ChannelData> channels;

    public LogisticsNetworkResponsePacket(BlockPos warehouseBlockPos, UUID warehouseId,
                                          List<BlockPos> warehouseContainers,
                                          List<RequestLogisticsNetworkPacket.ClientData> clients,
                                          List<ChannelData> channels) {
        this.warehouseBlockPos = warehouseBlockPos;
        this.warehouseId = warehouseId;
        this.warehouseContainers = new ArrayList<>(warehouseContainers);
        this.clients = new ArrayList<>();
        for (RequestLogisticsNetworkPacket.ClientData client : clients) {
            this.clients.add(new ClientData(client.clientId, client.blockPos, client.cityId, client.portPositions, client.name));
        }
        this.channels = channels != null ? new ArrayList<>(channels) : new ArrayList<>();
    }

    public LogisticsNetworkResponsePacket(FriendlyByteBuf buf) {
        this.warehouseBlockPos = buf.readBlockPos();
        this.warehouseId = buf.readUUID();

        int containerCount = buf.readVarInt();
        this.warehouseContainers = new ArrayList<>();
        for (int i = 0; i < containerCount; i++) {
            this.warehouseContainers.add(buf.readBlockPos());
        }

        int clientCount = buf.readVarInt();
        this.clients = new ArrayList<>();
        for (int i = 0; i < clientCount; i++) {
            UUID clientId = buf.readUUID();
            BlockPos blockPos = buf.readBlockPos();
            UUID cityId = buf.readUUID();
            int portCount = buf.readVarInt();
            List<BlockPos> ports = new ArrayList<>();
            for (int j = 0; j < portCount; j++) {
                ports.add(buf.readBlockPos());
            }
            String name = buf.readUtf();
            this.clients.add(new ClientData(clientId, blockPos, cityId, ports, name));
        }

        int channelCount = buf.readVarInt();
        this.channels = new ArrayList<>();
        for (int i = 0; i < channelCount; i++) {
            UUID channelId = buf.readUUID();
            String name = buf.readUtf();
            UUID targetClientId = buf.readUUID();
            String direction = buf.readUtf();
            boolean enabled = buf.readBoolean();
            int itemCount = buf.readVarInt();
            List<String> itemNames = new ArrayList<>();
            for (int j = 0; j < itemCount; j++) {
                itemNames.add(buf.readUtf());
            }
            this.channels.add(new ChannelData(channelId, name, targetClientId, direction, enabled, itemNames));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(warehouseBlockPos);
        buf.writeUUID(warehouseId);

        buf.writeVarInt(warehouseContainers.size());
        for (BlockPos pos : warehouseContainers) {
            buf.writeBlockPos(pos);
        }

        buf.writeVarInt(clients.size());
        for (ClientData client : clients) {
            buf.writeUUID(client.clientId);
            buf.writeBlockPos(client.blockPos);
            buf.writeUUID(client.cityId);
            buf.writeVarInt(client.portPositions.size());
            for (BlockPos pos : client.portPositions) {
                buf.writeBlockPos(pos);
            }
            buf.writeUtf(client.name != null ? client.name : "");
        }

        buf.writeVarInt(channels.size());
        for (ChannelData ch : channels) {
            buf.writeUUID(ch.channelId);
            buf.writeUtf(ch.name);
            buf.writeUUID(ch.targetClientId);
            buf.writeUtf(ch.direction);
            buf.writeBoolean(ch.enabled);
            buf.writeVarInt(ch.itemNames.size());
            for (String item : ch.itemNames) {
                buf.writeUtf(item);
            }
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();

            // 缓存客户端数据（包含名称）
            for (ClientData client : clients) {
                LogisticsClientData.updateClientName(client.clientId, client.name != null ? client.name : "");
            }

            // 通知当前打开的界面
            if (mc.screen instanceof LogisticsNetworkScreenReceiver receiver) {
                receiver.onLogisticsNetworkDataReceived(this);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public BlockPos getWarehouseBlockPos() {
        return warehouseBlockPos;
    }

    public UUID getWarehouseId() {
        return warehouseId;
    }

    public List<BlockPos> getWarehouseContainers() {
        return warehouseContainers;
    }

    public List<ClientData> getClients() {
        return clients;
    }

    public List<ChannelData> getChannels() {
        return channels;
    }

    /**
     * 频道数据
     */
    public static class ChannelData {
        public final UUID channelId;
        public final String name;
        public final UUID targetClientId;
        public final String direction;
        public final boolean enabled;
        public final List<String> itemNames;

        public ChannelData(UUID channelId, String name, UUID targetClientId, String direction,
                          boolean enabled, List<String> itemNames) {
            this.channelId = channelId;
            this.name = name;
            this.targetClientId = targetClientId;
            this.direction = direction;
            this.enabled = enabled;
            this.itemNames = new ArrayList<>(itemNames);
        }
    }

    /**
     * 客户端数据
     */
    public static class ClientData {
        public final UUID clientId;
        public final BlockPos blockPos;
        public final UUID cityId;
        public final List<BlockPos> portPositions;
        public final String name;

        public ClientData(UUID clientId, BlockPos blockPos, UUID cityId, List<BlockPos> portPositions, String name) {
            this.clientId = clientId;
            this.blockPos = blockPos;
            this.cityId = cityId;
            this.portPositions = portPositions;
            this.name = name != null ? name : "";
        }
    }

    /**
     * GUI 实现此接口以接收网络数据
     */
    public interface LogisticsNetworkScreenReceiver {
        void onLogisticsNetworkDataReceived(LogisticsNetworkResponsePacket packet);
    }
}
