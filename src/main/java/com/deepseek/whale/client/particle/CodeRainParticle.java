package com.deepseek.whale.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * 红色代码流粒子：随机红色代码字符，短寿命下落 + 周期性换帧，形成"代码雨/数据流"感。
 */
public class CodeRainParticle extends TextureSheetParticle {

    private final SpriteSet sprites;

    private CodeRainParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.setParticleSpeed(
                (this.random.nextDouble() - 0.5D) * 0.3D,
                -0.18D - this.random.nextDouble() * 0.3D,
                (this.random.nextDouble() - 0.5D) * 0.3D);
        this.quadSize = 0.16F + this.random.nextFloat() * 0.3F;
        this.lifetime = 16 + this.random.nextInt(14);
        this.gravity = 0.0F;
        this.pickSprite(sprites);
        // 红色系（贴图本身红色，这里再压暗/提亮一些）
        this.setColor(1.0F, 0.15F + this.random.nextFloat() * 0.15F, 0.18F);
        this.alpha = 0.95F;
    }

    @Override
    public void tick() {
        super.tick();
        // 每 4 tick 换一帧字符，制造"流"动感
        if (this.age % 4 == 0) {
            this.pickSprite(this.sprites);
        }
    }

    @Override
    public int getLightColor(float partialTick) {
        return 15728880; // 全亮，代码字符不随环境变暗
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static ParticleProvider<SimpleParticleType> provider(SpriteSet sprites) {
        return (type, level, x, y, z, dx, dy, dz) -> new CodeRainParticle(level, x, y, z, sprites);
    }
}
