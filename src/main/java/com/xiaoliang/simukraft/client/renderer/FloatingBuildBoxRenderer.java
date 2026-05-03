package com.xiaoliang.simukraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.xiaoliang.simukraft.Simukraft;
import com.xiaoliang.simukraft.client.model.FloatingBuildBoxModel;
import com.xiaoliang.simukraft.entity.FloatingBuildBoxEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

@SuppressWarnings("null")
public class FloatingBuildBoxRenderer extends EntityRenderer<FloatingBuildBoxEntity> {
    private static final ResourceLocation TEXTURE = 
            ResourceLocation.fromNamespaceAndPath(Simukraft.MOD_ID, "textures/entity/floating_build_box.png");
    
    private final FloatingBuildBoxModel model;

    public FloatingBuildBoxRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new FloatingBuildBoxModel(context.bakeLayer(com.xiaoliang.simukraft.client.ModModelLayers.FLOATING_BUILD_BOX));
    }

    @Override
    public ResourceLocation getTextureLocation(FloatingBuildBoxEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(FloatingBuildBoxEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        
        // 缩小尺寸到0.8f
        float scale = 0.5f; // 从2.0f缩小到0.8f
        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        
        // 增加浮动速度
        float floatOffset = (float) Math.sin((entity.tickCount + partialTicks) * 0.1f) * 0.1f;
        poseStack.translate(0.0, floatOffset, 0.0);
        
        // 增加旋转速度
        float rotation = (entity.tickCount + partialTicks) * 2.0f;
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        
        // 渲染模型
        VertexConsumer vertexconsumer = buffer.getBuffer(this.model.renderType(this.getTextureLocation(entity)));
        this.model.renderToBuffer(poseStack, vertexconsumer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        
        poseStack.popPose();
    }
}
