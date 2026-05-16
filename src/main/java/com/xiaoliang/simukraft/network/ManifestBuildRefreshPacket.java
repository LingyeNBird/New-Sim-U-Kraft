package com.xiaoliang.simukraft.network;

import com.xiaoliang.simukraft.building.ConstructionTask;
import com.xiaoliang.simukraft.client.gui.ManifestScreen;
import com.xiaoliang.simukraft.entity.CustomEntity;
import com.xiaoliang.simukraft.item.ManifestItem;
import com.xiaoliang.simukraft.world.BuildBoxHiredData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nonnull;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

@SuppressWarnings("null")
public class ManifestBuildRefreshPacket {
    private final long buildBoxPosLong;
    private final boolean remainingOnly;
    private final ItemStack manifestStack;

    public ManifestBuildRefreshPacket(long buildBoxPosLong, boolean remainingOnly, @Nonnull ItemStack manifestStack) {
        this.buildBoxPosLong = buildBoxPosLong;
        this.remainingOnly = remainingOnly;
        this.manifestStack = manifestStack.copy();
    }

    public ManifestBuildRefreshPacket(FriendlyByteBuf buf) {
        this.buildBoxPosLong = buf.readLong();
        this.remainingOnly = buf.readBoolean();
        this.manifestStack = buf.readItem();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeLong(buildBoxPosLong);
        buf.writeBoolean(remainingOnly);
        buf.writeItem(manifestStack);
    }

    public static ManifestBuildRefreshPacket decode(FriendlyByteBuf buf) {
        return new ManifestBuildRefreshPacket(buf);
    }

    public static void handle(ManifestBuildRefreshPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isServer()) {
                handleServer(packet, ctx.get().getSender());
            } else {
                handleClient(packet);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static void handleServer(ManifestBuildRefreshPacket packet, ServerPlayer player) {
        if (player == null || packet.buildBoxPosLong == Long.MIN_VALUE) {
            return;
        }

        ServerLevel level = player.serverLevel();
        BlockPos buildBoxPos = BlockPos.of(packet.buildBoxPosLong);
        UUID npcUuid = BuildBoxHiredData.loadHiredBuilders(level.getServer()).get(buildBoxPos);
        if (npcUuid == null) {
            return;
        }

        CustomEntity npc = BuildBoxHiredData.findNPCByUuid(level.getServer(), npcUuid);
        if (npc == null) {
            return;
        }

        ConstructionTask task = npc.getConstructionTask();
        if (task == null) {
            return;
        }

        ItemStack refreshedStack = packet.manifestStack.copy();
        CompoundTag tag = refreshedStack.getOrCreateTag();
        tag.putString("BuildingName", task.getDisplayName());
        tag.putLong("BuildBoxPos", buildBoxPos.asLong());
        tag.putString("SourceType", "build");

        Map<String, Boolean> checkedByItemId = ManifestItem.getCheckedByItemId(refreshedStack);

        Map<String, Integer> materials = packet.remainingOnly
            ? collectRemainingMaterials(task, level)
            : task.getRequiredMaterials();
        writeManifestMaterials(tag, materials);
        refreshedStack.setTag(tag);
        ManifestItem.reapplyCheckedByItemId(refreshedStack, checkedByItemId);

        NetworkManager.sendToPlayer(new ManifestBuildRefreshPacket(buildBoxPos.asLong(), packet.remainingOnly, refreshedStack), player);
    }

    private static Map<String, Integer> collectRemainingMaterials(ConstructionTask task, ServerLevel level) {
        Map<String, Integer> remaining = new LinkedHashMap<>();
        for (MaterialRequirementsResponsePacket.MaterialInfo material :
            MaterialRequirementsRequestPacket.collectMaterialsForTask(task, level)) {
            if (material.count > 0) {
                remaining.put(material.blockId, material.count);
            }
        }
        return remaining;
    }

    private static void writeManifestMaterials(CompoundTag tag, Map<String, Integer> materials) {
        net.minecraft.nbt.ListTag materialsList = new net.minecraft.nbt.ListTag();
        net.minecraft.nbt.ListTag checkedList = new net.minecraft.nbt.ListTag();
        for (Map.Entry<String, Integer> entry : materials.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() <= 0) {
                continue;
            }
            CompoundTag materialTag = new CompoundTag();
            materialTag.putString("Item", entry.getKey());
            materialTag.putInt("Count", entry.getValue());
            materialsList.add(materialTag);
            checkedList.add(net.minecraft.nbt.StringTag.valueOf("false"));
        }
        tag.put("Materials", materialsList);
        tag.put("Checked", checkedList);
        tag.remove("ProductGroups");
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(ManifestBuildRefreshPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof ManifestScreen screen) {
            screen.applyRefreshedManifest(packet.manifestStack.copy(), packet.remainingOnly);
        }
    }
}
