package com.deepseek.whale.client;

import com.deepseek.whale.DeepseekWhaleMod;
import com.deepseek.whale.client.particle.CodeRainParticle;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/** 客户端粒子注册（仅客户端加载）。 */
@EventBusSubscriber(modid = DeepseekWhaleMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientParticles {

    private ClientParticles() {
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(DeepseekWhaleMod.CODE_RAIN.get(), CodeRainParticle::provider);
    }
}
