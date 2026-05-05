package com.xiaoliang.simukraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xiaoliang.simukraft.entity.CustomEntity;
import com.xiaoliang.simukraft.entity.WorkStatus;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

@SuppressWarnings("null")
public class CustomEntityModel<T extends LivingEntity> extends PlayerModel<T> {
    private static final float CHILD_BODY_VISUAL_SCALE = 0.62F;
    private boolean renderChildModel = false;
    private final float baseHeadY;
    private final float baseHatY;
    private final float baseBodyY;
    private final float baseJacketY;
    private final float baseLeftArmX;
    private final float baseLeftArmY;
    private final float baseLeftSleeveX;
    private final float baseLeftSleeveY;
    private final float baseRightArmX;
    private final float baseRightArmY;
    private final float baseRightSleeveX;
    private final float baseRightSleeveY;
    private final float baseLeftLegX;
    private final float baseLeftLegY;
    private final float baseLeftPantsX;
    private final float baseLeftPantsY;
    private final float baseRightLegX;
    private final float baseRightLegY;
    private final float baseRightPantsX;
    private final float baseRightPantsY;

    @Nonnull
    private static <V> V nn(@Nullable V value) {
        return Objects.requireNonNull(value);
    }

    public CustomEntityModel(ModelPart root, boolean slim) {
        super(nn(root), slim);
        this.baseHeadY = this.head.y;
        this.baseHatY = this.hat.y;
        this.baseBodyY = this.body.y;
        this.baseJacketY = this.jacket.y;
        this.baseLeftArmX = this.leftArm.x;
        this.baseLeftArmY = this.leftArm.y;
        this.baseLeftSleeveX = this.leftSleeve.x;
        this.baseLeftSleeveY = this.leftSleeve.y;
        this.baseRightArmX = this.rightArm.x;
        this.baseRightArmY = this.rightArm.y;
        this.baseRightSleeveX = this.rightSleeve.x;
        this.baseRightSleeveY = this.rightSleeve.y;
        this.baseLeftLegX = this.leftLeg.x;
        this.baseLeftLegY = this.leftLeg.y;
        this.baseLeftPantsX = this.leftPants.x;
        this.baseLeftPantsY = this.leftPants.y;
        this.baseRightLegX = this.rightLeg.x;
        this.baseRightLegY = this.rightLeg.y;
        this.baseRightPantsX = this.rightPants.x;
        this.baseRightPantsY = this.rightPants.y;
    }

    public static LayerDefinition createPlayerLikeLayer() {
        // 创建Alex纤细手臂模型（slim=true）
        MeshDefinition mesh = nn(createMesh(nn(CubeDeformation.NONE), true));
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition createSteveLayer() {
        // 创建Steve粗手臂模型（slim=false）
        MeshDefinition mesh = nn(createMesh(nn(CubeDeformation.NONE), false));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(@Nonnull T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(nn(entity), limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        // 添加自定义动画逻辑
        this.hat.copyFrom(nn(this.head));
        applyAdultLayout();

        // 检查是否为建筑师NPC或牧羊人NPC且正在工作
        if (entity instanceof CustomEntity customEntity) {
            if (customEntity.getWorkStatus() == WorkStatus.WORKING && 
                ("builder".equals(customEntity.getJob()) || "shepherd".equals(customEntity.getJob()) || "butcher".equals(customEntity.getJob()))) {
                // 挥手动画现在完全由CustomEntity的getAttackAnim方法控制
                // 这里不再需要额外的动画逻辑
            }
            this.renderChildModel = customEntity.isChildForm();
            if (this.renderChildModel) {
                applyChildLayout();
            }
        } else {
            this.renderChildModel = false;
        }
    }

    @Override
    public void renderToBuffer(@Nonnull PoseStack poseStack, @Nonnull VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        if (renderChildModel) {
            renderChildLikeModel(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
            return;
        }

        // 渲染两层皮肤
        this.head.render(nn(poseStack), nn(buffer), packedLight, packedOverlay);
        this.hat.render(nn(poseStack), nn(buffer), packedLight, packedOverlay);
        this.body.render(nn(poseStack), nn(buffer), packedLight, packedOverlay);
        this.jacket.render(nn(poseStack), nn(buffer), packedLight, packedOverlay);
        this.leftArm.render(nn(poseStack), nn(buffer), packedLight, packedOverlay);
        this.leftSleeve.render(nn(poseStack), nn(buffer), packedLight, packedOverlay);
        this.rightArm.render(nn(poseStack), nn(buffer), packedLight, packedOverlay);
        this.rightSleeve.render(nn(poseStack), nn(buffer), packedLight, packedOverlay);
        this.leftLeg.render(nn(poseStack), nn(buffer), packedLight, packedOverlay);


        this.leftPants.render(nn(poseStack), nn(buffer), packedLight, packedOverlay);
        this.rightLeg.render(nn(poseStack), nn(buffer), packedLight, packedOverlay);
        this.rightPants.render(nn(poseStack), nn(buffer), packedLight, packedOverlay);
    }

    private void renderChildLikeModel(@Nonnull PoseStack poseStack, @Nonnull VertexConsumer buffer,
                                      int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        this.head.render(nn(poseStack), nn(buffer), packedLight, packedOverlay, red, green, blue, alpha);
        this.hat.render(nn(poseStack), nn(buffer), packedLight, packedOverlay, red, green, blue, alpha);
        this.body.render(nn(poseStack), nn(buffer), packedLight, packedOverlay, red, green, blue, alpha);
        this.jacket.render(nn(poseStack), nn(buffer), packedLight, packedOverlay, red, green, blue, alpha);
        this.leftArm.render(nn(poseStack), nn(buffer), packedLight, packedOverlay, red, green, blue, alpha);
        this.leftSleeve.render(nn(poseStack), nn(buffer), packedLight, packedOverlay, red, green, blue, alpha);
        this.rightArm.render(nn(poseStack), nn(buffer), packedLight, packedOverlay, red, green, blue, alpha);
        this.rightSleeve.render(nn(poseStack), nn(buffer), packedLight, packedOverlay, red, green, blue, alpha);
        this.leftLeg.render(nn(poseStack), nn(buffer), packedLight, packedOverlay, red, green, blue, alpha);
        this.leftPants.render(nn(poseStack), nn(buffer), packedLight, packedOverlay, red, green, blue, alpha);
        this.rightLeg.render(nn(poseStack), nn(buffer), packedLight, packedOverlay, red, green, blue, alpha);
        this.rightPants.render(nn(poseStack), nn(buffer), packedLight, packedOverlay, red, green, blue, alpha);
    }

    private void applyAdultLayout() {
        this.head.y = baseHeadY;
        this.hat.y = baseHatY;
        this.body.y = baseBodyY;
        this.jacket.y = baseJacketY;

        this.leftArm.x = baseLeftArmX;
        this.leftArm.y = baseLeftArmY;
        this.leftSleeve.x = baseLeftSleeveX;
        this.leftSleeve.y = baseLeftSleeveY;
        this.rightArm.x = baseRightArmX;
        this.rightArm.y = baseRightArmY;
        this.rightSleeve.x = baseRightSleeveX;
        this.rightSleeve.y = baseRightSleeveY;

        this.leftLeg.x = baseLeftLegX;
        this.leftLeg.y = baseLeftLegY;
        this.leftPants.x = baseLeftPantsX;
        this.leftPants.y = baseLeftPantsY;
        this.rightLeg.x = baseRightLegX;
        this.rightLeg.y = baseRightLegY;
        this.rightPants.x = baseRightPantsX;
        this.rightPants.y = baseRightPantsY;

        setScale(this.head, 1.0F);
        setScale(this.hat, 1.0F);
        setScale(this.body, 1.0F);
        setScale(this.jacket, 1.0F);
        setScale(this.leftArm, 1.0F);
        setScale(this.leftSleeve, 1.0F);
        setScale(this.rightArm, 1.0F);
        setScale(this.rightSleeve, 1.0F);
        setScale(this.leftLeg, 1.0F);
        setScale(this.leftPants, 1.0F);
        setScale(this.rightLeg, 1.0F);
        setScale(this.rightPants, 1.0F);
    }

    private void applyChildLayout() {
        float scale = CHILD_BODY_VISUAL_SCALE;
        float rootOffset = 24.0F * (1.0F - scale);

        this.head.y = rootOffset;
        this.hat.y = rootOffset;
        this.body.y = rootOffset;
        this.jacket.y = rootOffset;

        this.leftArm.x = baseLeftArmX * scale;
        this.leftArm.y = rootOffset + 2.0F * scale;
        this.leftSleeve.x = baseLeftSleeveX * scale;
        this.leftSleeve.y = rootOffset + 2.0F * scale;
        this.rightArm.x = baseRightArmX * scale;
        this.rightArm.y = rootOffset + 2.0F * scale;
        this.rightSleeve.x = baseRightSleeveX * scale;
        this.rightSleeve.y = rootOffset + 2.0F * scale;

        this.leftLeg.x = baseLeftLegX * scale;
        this.leftLeg.y = rootOffset + 12.0F * scale;
        this.leftPants.x = baseLeftPantsX * scale;
        this.leftPants.y = rootOffset + 12.0F * scale;
        this.rightLeg.x = baseRightLegX * scale;
        this.rightLeg.y = rootOffset + 12.0F * scale;
        this.rightPants.x = baseRightPantsX * scale;
        this.rightPants.y = rootOffset + 12.0F * scale;

        setScale(this.head, 1.0F);
        setScale(this.hat, 1.0F);
        setScale(this.body, scale);
        setScale(this.jacket, scale);
        setScale(this.leftArm, scale);
        setScale(this.leftSleeve, scale);
        setScale(this.rightArm, scale);
        setScale(this.rightSleeve, scale);
        setScale(this.leftLeg, scale);
        setScale(this.leftPants, scale);
        setScale(this.rightLeg, scale);
        setScale(this.rightPants, scale);
    }

    private void setScale(@Nonnull ModelPart part, float scale) {
        part.xScale = scale;
        part.yScale = scale;
        part.zScale = scale;
    }
}
