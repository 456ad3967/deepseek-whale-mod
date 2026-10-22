package com.deepseek.whale;

import com.deepseek.whale.attack.WhaleAttackSystem;
import com.deepseek.whale.client.WhalePetModel;
import com.deepseek.whale.client.WhalePetRenderer;
import com.deepseek.whale.entity.WhalePetEntity;
import com.deepseek.whale.protection.LowHealthProtection;
import com.deepseek.whale.spawn.PetSpawnHandler;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(DeepseekWhaleMod.MODID)
public class DeepseekWhaleMod {
    public static final String MODID = "deepseek_whale";

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<WhalePetEntity>> WHALE_PET =
            ENTITY_TYPES.register("whale_pet",
                    () -> EntityType.Builder.of(WhalePetEntity::new, MobCategory.AMBIENT)
                            .sized(1.2f, 1.2f)
                            .clientTrackingRange(10)
                            .build("whale_pet"));

    public DeepseekWhaleMod(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
        modBus.addListener(DeepseekWhaleMod::registerAttributes);
        modBus.addListener(DeepseekWhaleMod::registerRenderers);
        modBus.addListener(DeepseekWhaleMod::registerLayerDefinitions);

        // 运行时子系统
        WhaleAttackSystem.init();
        LowHealthProtection.init();
        PetSpawnHandler.init();

        // 游戏事件接线
        var gameBus = net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        gameBus.addListener(LowHealthProtection::onPlayerTick);
        gameBus.addListener(PetSpawnHandler::onLoggedIn);
        gameBus.addListener(PetSpawnHandler::onRespawn);
        gameBus.addListener(PetSpawnHandler::onChangedDimension);
        gameBus.addListener(PetSpawnHandler::onEntityJoinLevel);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(WHALE_PET.get(), WhalePetEntity.createAttributes().build());
    }

    private static void registerRenderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(WHALE_PET.get(), WhalePetRenderer::new);
    }

    private static void registerLayerDefinitions(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(WhalePetRenderer.WHALE_PET_LAYER, WhalePetModel::createBodyLayer);
    }
}
