package com.deepseek.whale.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * 白色冲击波（原版 KillImpactParticle 移植）：命中点快速扩散后消失。
 */
public class KillImpactParticle extends TextureSheetParticle {

    private final SpriteSet sprites;
    private final float startSize;

    protected KillImpactParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.lifetime = 10;
        this.startSize = 0.18F + this.random.nextFloat() * 0.06F;
        this.quadSize = this.startSize;
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.rCol = 1.0F;
        this.gCol = 0.55F;
        this.bCol = 0.55F;
        this.alpha = 1.0F;
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
        float progress = this.age / (float) this.lifetime;
        this.quadSize = this.startSize * (1.0F + 3.0F * progress);
        this.alpha = 1.0F - progress * 0.9F;
        this.setSpriteFromAge(this.sprites);
    }

    @Override
    public int getLightColor(float partialTick) {
        return 15728880;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static ParticleProvider<SimpleParticleType> provider(SpriteSet sprites) {
        return (type, level, x, y, z, dx, dy, dz) -> new KillImpactParticle(level, x, y, z, sprites);
    }
}
