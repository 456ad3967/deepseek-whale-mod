package com.deepseek.whale.client;

import com.deepseek.whale.DeepseekWhaleMod;
import com.deepseek.whale.entity.WhalePetEntity;
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
        super(context, new WhalePetModel(context.bakeLayer(WHALE_PET_LAYER)), 0.4F);
    }

    @Override
    public ResourceLocation getTextureLocation(WhalePetEntity entity) {
        return TEXTURE;
    }
}
