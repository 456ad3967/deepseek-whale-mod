package com.deepseek.whale.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * 蓝色尾迹光点（原版 TokenTrailParticle 移植）：短寿命，沿速度漂移并渐出。
 */
public class TokenTrailParticle extends TextureSheetParticle {

    private final SpriteSet sprites;

    protected TokenTrailParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.xd = (this.random.nextDouble() - 0.5D) * 0.16D;
        this.yd = (this.random.nextDouble() - 0.5D) * 0.16D;
        this.zd = (this.random.nextDouble() - 0.5D) * 0.16D;
        this.lifetime = 8 + this.random.nextInt(8);
        this.quadSize = 0.05F + this.random.nextFloat() * 0.04F;
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.rCol = 0.72F;
        this.gCol = 0.9F;
        this.bCol = 1.0F;
        this.alpha = 0.9F;
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
        this.x += this.xd;
        this.y += this.yd;
        this.z += this.zd;
        this.xd *= 0.92D;
        this.yd *= 0.92D;
        this.zd *= 0.92D;
        this.alpha = 0.9F * (1.0F - this.age / (float) this.lifetime);
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
        return (type, level, x, y, z, dx, dy, dz) -> new TokenTrailParticle(level, x, y, z, sprites);
    }
}
