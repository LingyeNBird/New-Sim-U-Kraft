package com.xiaoliang.simukraft.network;

import com.xiaoliang.simukraft.client.ClientCityChunkData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

@SuppressWarnings("null")
public class BuyChunkResponsePacket {
    private final boolean success;
    private final UUID cityId;
    private final Set<Long> cityChunks;
    private final String errorMessage;
    private final long chunkPosLong;
    private final double cost;

    public BuyChunkResponsePacket(boolean success, UUID cityId, Set<Long> cityChunks, String errorMessage, long chunkPosLong, double cost) {
        this.success = success;
        this.cityId = cityId;
        this.cityChunks = cityChunks;
        this.errorMessage = errorMessage;
        this.chunkPosLong = chunkPosLong;
        this.cost = cost;
    }

    public BuyChunkResponsePacket(FriendlyByteBuf buf) {
        this.success = buf.readBoolean();
        this.cityId = buf.readUUID();
        int size = buf.readInt();
        this.cityChunks = new HashSet<>(size);
        for (int i = 0; i < size; i++) {
            this.cityChunks.add(buf.readLong());
        }
        this.errorMessage = buf.readUtf();
        this.chunkPosLong = buf.readLong();
        this.cost = buf.readDouble();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(success);
        buf.writeUUID(cityId);
        buf.writeInt(cityChunks.size());
        for (long chunkLong : cityChunks) {
            buf.writeLong(chunkLong);
        }
        buf.writeUtf(errorMessage);
        buf.writeLong(chunkPosLong);
        buf.writeDouble(cost);
    }

    public static BuyChunkResponsePacket decode(FriendlyByteBuf buf) {
        return new BuyChunkResponsePacket(buf);
    }

    public static void handle(BuyChunkResponsePacket message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 只在客户端处理
            if (ctx.get().getDirection().getReceptionSide().isClient()) {
                // 更新客户端城市区块数据
                ClientCityChunkData.getInstance().updateCityChunks(message.cityId, message.cityChunks);
                
                // 触发购买结果事件
                BuyChunkResultEvent.INSTANCE.fire(message.success, message.chunkPosLong, message.cost, message.errorMessage);
            }
        });
        ctx.get().setPacketHandled(true);
    }
    
    // 购买结果事件
    public static class BuyChunkResultEvent {
        public static final BuyChunkResultEvent INSTANCE = new BuyChunkResultEvent();
        private final Set<Listener> listeners = new HashSet<>();
        
        private BuyChunkResultEvent() {}
        
        public void register(Listener listener) {
            listeners.add(listener);
        }
        
        public void unregister(Listener listener) {
            listeners.remove(listener);
        }
        
        public void fire(boolean success, long chunkPosLong, double cost, String errorMessage) {
            for (Listener listener : listeners) {
                listener.onBuyChunkResult(success, chunkPosLong, cost, errorMessage);
            }
        }
        
        public interface Listener {
            void onBuyChunkResult(boolean success, long chunkPosLong, double cost, String errorMessage);
        }
    }
}