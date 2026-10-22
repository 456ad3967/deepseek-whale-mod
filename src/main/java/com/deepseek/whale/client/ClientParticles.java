package com.deepseek.whale.client;

import com.deepseek.whale.DeepSeekParticles;
import com.deepseek.whale.DeepseekWhaleMod;
import com.deepseek.whale.client.particle.CodeRainParticle;
import com.deepseek.whale.client.particle.KillImpactParticle;
import com.deepseek.whale.client.particle.KillTextParticle;
import com.deepseek.whale.client.particle.TokenFloatParticle;
import com.deepseek.whale.client.particle.TokenOrbitParticle;
import com.deepseek.whale.client.particle.TokenSparkParticle;
import com.deepseek.whale.client.particle.TokenTrailParticle;
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
        // 原版 token 系列（照抄 0.1.0 的 WhaleClient 绑定）
        event.registerSpriteSet(DeepSeekParticles.TOKEN_SPARK.get(), TokenSparkParticle::provider);
        event.registerSpriteSet(DeepSeekParticles.TOKEN_ORBIT.get(), TokenOrbitParticle::provider);
        event.registerSpriteSet(DeepSeekParticles.TOKEN_TRAIL.get(), TokenTrailParticle::provider);
        event.registerSpriteSet(DeepSeekParticles.TOKEN_FLOAT.get(), TokenFloatParticle::provider);
        event.registerSpriteSet(DeepSeekParticles.TOKEN_ANGRY.get(), TokenSparkParticle::provider);
        event.registerSpriteSet(DeepSeekParticles.KILL_TEXT.get(), KillTextParticle::provider);
        event.registerSpriteSet(DeepSeekParticles.KILL_IMPACT.get(), KillImpactParticle::provider);
        // 红色代码流（本 mod 定制）
        event.registerSpriteSet(DeepseekWhaleMod.CODE_RAIN.get(), CodeRainParticle::provider);
    }
}
