package com.deepseek.whale.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/**
 * 蓝白闪光点（原版 TokenSparkParticle 移植，26.3 SingleQuadParticle -> 1.21.1 TextureSheetParticle）。
 * 寿命 18-27 tick，alpha 快速渐入渐出，全亮发光。
 */
public class TokenSparkParticle extends TextureSheetParticle {

    private final SpriteSet sprites;
    private final float glowDelay;
    private final float glowPeak;
    private final float glowFlicker;
    private final float glowPhase;

    protected TokenSparkParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.glowDelay = 0.35F * this.random.nextFloat();
        this.glowPeak = 0.75F + this.random.nextFloat() * 0.25F;
        this.glowFlicker = 0.35F * this.random.nextFloat();
        this.glowPhase = this.random.nextFloat() * 6.2831855F;
        this.lifetime = 18 + this.random.nextInt(10);
        this.quadSize = 0.055F + this.random.nextFloat() * 0.025F;
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.xd = 0.0D;
        this.yd = 0.0D;
        this.zd = 0.0D;
        this.rCol = 0.72F + this.random.nextFloat() * 0.28F;
        this.gCol = 0.88F;
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
        float fadeIn = Math.min(1.0F, this.age / 3.0F);
        float fadeOut = Math.min(1.0F, (this.lifetime - this.age) / 8.0F);
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
        return (type, level, x, y, z, dx, dy, dz) -> new TokenSparkParticle(level, x, y, z, sprites);
    }
}
