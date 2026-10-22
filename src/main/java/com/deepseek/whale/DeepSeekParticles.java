package com.deepseek.whale;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 原版 token 系列粒子注册（逻辑照抄 deepseek_whale-0.1.0 的 DeepSeekParticles）。 */
public final class DeepSeekParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, DeepseekWhaleMod.MODID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> TOKEN_SPARK = register("token_spark");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> TOKEN_ORBIT = register("token_orbit");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> TOKEN_TRAIL = register("token_trail");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> TOKEN_FLOAT = register("token_float");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> KILL_TEXT = register("kill_text");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> KILL_IMPACT = register("kill_impact");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> TOKEN_ANGRY = register("token_angry");

    private DeepSeekParticles() {
    }

    public static void register(IEventBus bus) {
        PARTICLE_TYPES.register(bus);
    }

    private static DeferredHolder<ParticleType<?>, SimpleParticleType> register(String name) {
        return PARTICLE_TYPES.register(name, () -> new SimpleParticleType(false));
    }
}
