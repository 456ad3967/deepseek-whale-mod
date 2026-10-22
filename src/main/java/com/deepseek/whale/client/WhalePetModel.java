package com.deepseek.whale.client;

import com.deepseek.whale.entity.WhalePetEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * 人机肥鱼模型 —— 原版几何（从原版 deepseek_whale-0.1.0.jar 的 WhaleModel 逆向移植）。
 * 结构：body > head / dorsal_fin / left_fin / right_fin / tail > second_tail > third_tail。
 * 贴图 32x32 UV 网格（贴图文件为 128x128）。
 */
public class WhalePetModel extends EntityModel<WhalePetEntity> {

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart tail;

    public WhalePetModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.tail = this.body.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.5F, 18.934177F, -3.5F, 0.0F, 0.0F, 0.0F));

        body.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-3.0F, -5.0F, -7.0F, 7.0F, 7.0F, 7.0F),
                PartPose.offsetAndRotation(-0.5F, 1.5F, 3.5F, 0.0F, 0.0F, 0.0F));

        PartDefinition dorsalFin = body.addOrReplaceChild("dorsal_fin", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, -4.0F, -2.0F, 0.0F, 0.0F, 0.0F));
        dorsalFin.addOrReplaceChild("dorsal_fin_r1", CubeListBuilder.create()
                        .texOffs(10, 22)
                        .addBox(0.0F, -1.31784F, -2.41421F, 1.0F, 2.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F));

        PartDefinition leftFin = body.addOrReplaceChild("left_fin", CubeListBuilder.create(),
                PartPose.offsetAndRotation(4.0F, 0.0F, -2.0F, 0.0F, 0.0F, 0.0F));
        leftFin.addOrReplaceChild("left_fin_r1", CubeListBuilder.create()
                        .texOffs(0, 20)
                        .addBox(-0.20574F, -0.37501F, -0.52197F, 2.0F, 1.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));

        PartDefinition rightFin = body.addOrReplaceChild("right_fin", CubeListBuilder.create(),
                PartPose.offsetAndRotation(-3.0F, 0.0F, -2.0F, 0.0F, 0.0F, 0.0F));
        rightFin.addOrReplaceChild("right_fin_r1", CubeListBuilder.create()
                        .texOffs(0, 20)
                        .addBox(-1.79426F, -0.37501F, -0.52197F, 2.0F, 1.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));

        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create(),
                PartPose.offsetAndRotation(-0.5F, 1.5F, 3.5F, 0.0F, 0.0F, 0.0F));
        tail.addOrReplaceChild("tail_r1", CubeListBuilder.create()
                        .texOffs(0, 14)
                        .addBox(-2.0F, -1.97254F, 0.78162F, 5.0F, 4.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F));

        PartDefinition secondTail = tail.addOrReplaceChild("second_tail", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, 0.0F, 2.0F, 0.0F, 0.0F, 0.0F));
        secondTail.addOrReplaceChild("second_tail_r1", CubeListBuilder.create()
                        .texOffs(14, 14)
                        .addBox(-1.0F, -1.06031F, -0.34202F, 3.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));

        PartDefinition thirdTail = secondTail.addOrReplaceChild("third_tail", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, -1.0F, 1.0F, 0.0F, 0.0F, 0.0F));
        thirdTail.addOrReplaceChild("third_tail_r1", CubeListBuilder.create()
                        .texOffs(0, 24)
                        .addBox(-0.33814F, -0.31462F, -0.64491F, 2.0F, 1.0F, 2.0F),
                PartPose.offsetAndRotation(1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        thirdTail.addOrReplaceChild("third_tail_r2", CubeListBuilder.create()
                        .texOffs(18, 22)
                        .addBox(-1.66186F, -0.31462F, -0.64491F, 2.0F, 1.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        thirdTail.addOrReplaceChild("third_tail_r3", CubeListBuilder.create()
                        .texOffs(10, 20)
                        .addBox(0.0F, -1.0F, 0.0F, 1.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(2.0F, -1.0F, 1.0F, 0.0F, 0.0F, 0.0F));
        thirdTail.addOrReplaceChild("third_tail_r4", CubeListBuilder.create()
                        .texOffs(24, 14)
                        .addBox(-1.0F, -1.0F, 0.0F, 1.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(-1.0F, -1.0F, 1.0F, 0.0F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(WhalePetEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // 呼吸式浮动 + 尾摆（风格化小动画）
        float bob = (float) (Math.sin(ageInTicks * 0.08D) * 0.04D);
        float sway = (float) (Math.sin(ageInTicks * 0.12D) * 0.1D);
        this.body.y = 18.934177F + bob;
        this.tail.yRot = sway;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer,
                               int packedLight, int packedOverlay, int color) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }

    public ModelPart root() {
        return this.root;
    }
}
