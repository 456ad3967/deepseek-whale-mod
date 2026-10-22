package com.deepseek.whale.client;

import com.deepseek.whale.entity.WhalePetEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * 胖虎鲸模型 —— 风格化简洁几何（对应概念图 DeepSeek 蓝紫配色）。
 * 结构：圆胖身体（头身一体）+ 背鳍 + 左右胸鳍 + 尾鳍 + 两侧眼睛。
 * 低面数、高辨识，帧率友好。
 */
public class WhalePetModel extends EntityModel<WhalePetEntity> {

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart tailFin;
    private final ModelPart leftFin;
    private final ModelPart rightFin;

    public WhalePetModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.tailFin = root.getChild("tailFin");
        this.leftFin = root.getChild("leftFin");
        this.rightFin = root.getChild("rightFin");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 圆胖身体：长(z)14、高(y)10、宽(x)10 —— 头身一体，腹部微胖
        root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-5.0F, -5.0F, -7.0F, 10.0F, 10.0F, 14.0F),
                PartPose.offset(0.0F, 22.0F, 0.0F));

        // 背鳍：小梯形感（用单方块）
        root.addOrReplaceChild("backFin", CubeListBuilder.create()
                        .texOffs(0, 24)
                        .addBox(-1.0F, -4.0F, 0.0F, 2.0F, 4.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 17.0F, -2.0F, 0.3F, 0.0F, 0.0F));

        // 尾鳍
        root.addOrReplaceChild("tailFin", CubeListBuilder.create()
                        .texOffs(20, 24)
                        .addBox(-1.0F, -2.0F, 0.0F, 2.0F, 6.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, 20.0F, 7.0F, 0.2F, 0.0F, 0.0F));

        // 左右胸鳍（小短鳍）
        root.addOrReplaceChild("leftFin", CubeListBuilder.create()
                        .texOffs(34, 24)
                        .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 1.0F, 5.0F),
                PartPose.offsetAndRotation(-5.0F, 19.0F, -1.0F, 0.0F, 0.0F, 0.15F));

        root.addOrReplaceChild("rightFin", CubeListBuilder.create()
                        .texOffs(30, 34)
                        .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 1.0F, 5.0F),
                PartPose.offsetAndRotation(5.0F, 19.0F, -1.0F, 0.0F, 0.0F, -0.15F));

        // 眼睛（左右各一，白底，瞳孔由贴图呈现）
        root.addOrReplaceChild("eyeLeft", CubeListBuilder.create()
                        .texOffs(52, 34)
                        .addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 1.0F),
                PartPose.offset(-4.5F, 18.5F, -6.5F));

        root.addOrReplaceChild("eyeRight", CubeListBuilder.create()
                        .texOffs(52, 40)
                        .addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 1.0F),
                PartPose.offset(4.5F, 18.5F, -6.5F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(WhalePetEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // 呼吸式浮动 + 摆尾 + 鳍扇动（风格化小动画）
        float bob = (float) (Math.sin(ageInTicks * 0.08D) * 0.06D);
        float sway = (float) (Math.sin(ageInTicks * 0.12D) * 0.08D);
        float finFlap = (float) (Math.sin(ageInTicks * 0.3D) * 0.25D);

        this.body.setPos(0.0F, 22.0F + bob, 0.0F);
        this.body.zRot = sway;
        this.tailFin.yRot = (float) (Math.sin(ageInTicks * 0.15D) * 0.35D);
        this.leftFin.zRot = 0.15F + finFlap;
        this.rightFin.zRot = -0.15F - finFlap;
    }

    @Override
    public ModelPart root() {
        return this.root;
    }
}
