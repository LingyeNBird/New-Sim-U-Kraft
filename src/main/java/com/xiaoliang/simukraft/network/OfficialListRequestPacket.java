package com.xiaoliang.simukraft.network;

import com.xiaoliang.simukraft.world.CityData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@SuppressWarnings({"null", "unused"})
public class OfficialListRequestPacket {
    private final BlockPos cityCorePos;

    public OfficialListRequestPacket(BlockPos cityCorePos) {
        this.cityCorePos = cityCorePos;
    }

    public OfficialListRequestPacket(FriendlyByteBuf buf) {
        this.cityCorePos = buf.readBlockPos();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(cityCorePos);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player != null) {
                ServerLevel level = player.serverLevel();
                CityData cityData = CityData.get(level);
                CityData.CityInfo city = cityData.getCityByCorePos(cityCorePos);

                if (city != null) {
                    UUID mayorId = city.getMayorId();
                    String mayorName = city.getMayorName();
                    List<String> officialNames = city.getOfficials();

                    // 构建官员信息列表
                    List<OfficialListResponsePacket.OfficialInfo> officials = new ArrayList<>();

                    // 添加市长
                    officials.add(new OfficialListResponsePacket.OfficialInfo(mayorName, true));

                    // 添加官员（现在使用玩家名）
                    for (String officialName : officialNames) {
                        officials.add(new OfficialListResponsePacket.OfficialInfo(officialName, false));
                    }

                    // 发送响应包
                    OfficialListResponsePacket response = new OfficialListResponsePacket(cityCorePos, officials);
                    NetworkManager.sendToPlayer(response, player);
                }
            }
        });
        context.get().setPacketHandled(true);
    }
}
