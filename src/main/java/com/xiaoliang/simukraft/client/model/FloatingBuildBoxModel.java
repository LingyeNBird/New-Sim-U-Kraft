package com.xiaoliang.simukraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xiaoliang.simukraft.entity.FloatingBuildBoxEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

import javax.annotation.Nonnull;

@SuppressWarnings("null")
public class FloatingBuildBoxModel extends EntityModel<FloatingBuildBoxEntity> {
    private final ModelPart box;

    public FloatingBuildBoxModel(ModelPart root) {
        this.box = root.getChild("box");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // 创建一个简单的立方体模型，支持64x32贴图布局
        // 使用标准的立方体创建方法，Minecraft会自动处理贴图映射
        partdefinition.addOrReplaceChild("box",
                CubeListBuilder.create()
                        .texOffs(0, 0) // 使用默认的贴图偏移
                        .addBox(-8.0F, -8.0F, -8.0F, 16.0F, 16.0F, 16.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 32); // 使用64x32的贴图尺寸
    }

    @Override
    public void setupAnim(@Nonnull FloatingBuildBoxEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // 不需要动画
    }

    @Override
    public void renderToBuffer(@Nonnull PoseStack poseStack, @Nonnull VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        box.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
