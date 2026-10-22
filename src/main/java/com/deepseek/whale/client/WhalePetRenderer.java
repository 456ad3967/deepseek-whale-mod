package com.deepseek.whale.client;

import com.deepseek.whale.DeepseekWhaleMod;
import com.deepseek.whale.entity.WhalePetEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * 胖虎鲸渲染器。
 */
public class WhalePetRenderer extends MobRenderer<WhalePetEntity, WhalePetModel> {

    public static final ModelLayerLocation WHALE_PET_LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(DeepseekWhaleMod.MODID, "whale_pet"), "main");

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(DeepseekWhaleMod.MODID, "textures/entity/whale_pet.png");

    public WhalePetRenderer(EntityRendererProvider.Context context) {
        super(context, new WhalePetModel(context.bakeLayer(WHALE_PET_LAYER)), 0.35F);
    }

    @Override
    public void render(WhalePetEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, VertexConsumer buffer, int packedLight) {
        // 整体缩小 15%：原版鲸鱼在 1.21.1 的比例下偏大
        poseStack.scale(0.85F, 0.85F, 0.85F);
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(WhalePetEntity entity) {
        return TEXTURE;
    }
}
