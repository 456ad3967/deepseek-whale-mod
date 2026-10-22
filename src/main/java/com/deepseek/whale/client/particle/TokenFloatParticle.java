package com.deepseek.whale.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/**
 * 蓝白浮游点（原版 TokenFloatParticle 移植）：绕出生点正弦漂移、缓慢上升、渐入渐出。
 */
public class TokenFloatParticle extends TextureSheetParticle {

    private final SpriteSet sprites;
    private final double homeX;
    private final double homeZ;
    private final double wanderPhase;
    private final double wanderRadius;
    private final float glowDelay;
    private final float glowPeak;
    private final float glowFlicker;
    private final float glowPhase;

    protected TokenFloatParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.glowDelay = 0.45F * this.random.nextFloat();
        this.glowPeak = 0.45F + this.random.nextFloat() * 0.45F;
        this.glowFlicker = 0.55F * this.random.nextFloat();
        this.glowPhase = this.random.nextFloat() * 6.2831855F;
        this.homeX = x;
        this.homeZ = z;
        this.wanderPhase = this.random.nextDouble() * 3.141592653589793D * 2.0D;
        this.wanderRadius = 0.18D + this.random.nextDouble() * 0.22D;
        this.lifetime = 60 + this.random.nextInt(45);
        this.quadSize = 0.053F + this.random.nextFloat() * 0.028F;
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.rCol = 0.62F + this.random.nextFloat() * 0.3F;
        this.gCol = 0.86F;
        this.bCol = 1.0F;
        this.alpha = 0.0F;
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.age++;
        if (this.age >= this.lifetime) {
            this.remove();
            return;
        }
        this.setSpriteFromAge(this.sprites);
        double progress = (double) this.age / this.lifetime;
        this.y += 0.012D * (1.0D - progress);
        this.x = this.homeX + Math.sin(this.wanderPhase + this.age * 0.018D) * this.wanderRadius;
        this.z = this.homeZ + Math.cos(this.wanderPhase + this.age * 0.014D) * this.wanderRadius;
        float fadeIn = Math.min(1.0F, this.age / 8.0F);
        float fadeOut = Math.min(1.0F, (this.lifetime - this.age) / 16.0F);
        this.alpha = Math.min(fadeIn, fadeOut);
    }

    @Override
    public int getLightColor(float partialTick) {
        float t = Mth.clamp((this.age + partialTick) / this.lifetime, 0.0F, 1.0F);
        float g = Mth.clamp((t - this.glowDelay) / (1.0F - this.glowDelay), 0.0F, 1.0F);
        g = g * g * (3.0F - 2.0F * g);
        if (this.glowFlicker > 0.0F) {
            float flick = 0.5F + 0.5F * Mth.sin((this.age + partialTick) * 1.7F + this.glowPhase);
            g *= 1.0F - this.glowFlicker * 0.45F * flick;
        }
        return 15728880;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static ParticleProvider<SimpleParticleType> provider(SpriteSet sprites) {
        return (type, level, x, y, z, dx, dy, dz) -> new TokenFloatParticle(level, x, y, z, sprites);
    }
}
