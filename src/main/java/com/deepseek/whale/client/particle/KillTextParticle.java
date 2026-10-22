package com.deepseek.whale.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * 红色标记粒子（原版 KillTextParticle 移植）：极短寿命，带随机旋转，飞行轨迹尖端使用。
 */
public class KillTextParticle extends TextureSheetParticle {

    private final SpriteSet sprites;

    protected KillTextParticle(ClientLevel level, double x, double y, double z,
                               double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.lifetime = 2;
        this.quadSize = 0.34F + this.random.nextFloat() * 0.06F;
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.rCol = 1.0F;
        this.gCol = 0.45F + this.random.nextFloat() * 0.15F;
        this.bCol = 0.45F + this.random.nextFloat() * 0.15F;
        this.alpha = 1.0F;
        this.roll = (this.random.nextFloat() - 0.5F) * 0.25F;
        this.oRoll = this.roll;
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
        this.alpha = 1.0F;
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
        return (type, level, x, y, z, dx, dy, dz) ->
                new KillTextParticle(level, x, y, z, dx, dy, dz, sprites);
    }
}
