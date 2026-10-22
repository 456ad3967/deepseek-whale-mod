package com.deepseek.whale.client.particle;

import com.deepseek.whale.entity.WhalePetEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;

/**
 * 蓝白环绕点（原版 TokenOrbitParticle 移植）：围绕最近的鲸鱼伙伴旋转扩散并上下浮动。
 */
public class TokenOrbitParticle extends TextureSheetParticle {

    private final float glowDelay;
    private final float glowPeak;
    private final float glowFlicker;
    private final float glowPhase;
    private final SpriteSet sprites;
    private double centreX;
    private double centreY;
    private double centreZ;
    private double offX;
    private double offZ;
    private final double bobPhase;
    private final double spin;
    private final double outward;
    private double yOffset;
    private boolean anchored;

    protected TokenOrbitParticle(ClientLevel level, double x, double y, double z,
                                 double offX, double offZ, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.glowDelay = 0.6F * this.random.nextFloat();
        this.glowPeak = 0.5F + this.random.nextFloat() * 0.45F;
        this.glowFlicker = 0.7F * this.random.nextFloat();
        this.glowPhase = this.random.nextFloat() * 6.2831855F;
        this.centreX = x;
        this.centreY = y;
        this.centreZ = z;
        // sendParticles 的 speed=0 时 provider 收到的 dx/dz 近似 0；
        // 此时自动在身体周围随机取一个轨道起点（原版 spawnAura 的随机半径）
        if (offX * offX + offZ * offZ < 0.0001D) {
            double angle = this.random.nextDouble() * Math.PI * 2.0D;
            double radius = 0.45D + this.random.nextDouble() * 0.55D;
            offX = Math.cos(angle) * radius;
            offZ = Math.sin(angle) * radius;
        }
        this.offX = offX;
        this.offZ = offZ;
        this.bobPhase = this.random.nextDouble() * 3.141592653589793D * 2.0D;
        this.spin = 0.16D + this.random.nextDouble() * 0.1D;
        this.outward = 1.003D + this.random.nextDouble() * 0.004D;
        this.lifetime = 24 + this.random.nextInt(12);
        this.quadSize = 0.05F + this.random.nextFloat() * 0.022F;
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.rCol = 0.72F + this.random.nextFloat() * 0.28F;
        this.gCol = 0.88F;
        this.bCol = 1.0F;
        this.alpha = 0.0F;
        this.pickSprite(sprites);
    }

    private void trackWhale() {
        // 2.5 格内找最近的鲸鱼伙伴并跟随（原版逻辑，实体类换成 WhalePetEntity）
        double reach = 2.5D;
        var candidates = this.level.getEntitiesOfClass(WhalePetEntity.class,
                new AABB(this.centreX - reach, this.centreY - reach, this.centreZ - reach,
                        this.centreX + reach, this.centreY + reach, this.centreZ + reach));
        WhalePetEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (WhalePetEntity w : candidates) {
            double d = w.distanceToSqr(this.centreX, this.centreY, this.centreZ);
            if (d < best) {
                best = d;
                nearest = w;
            }
        }
        if (nearest == null) {
            return;
        }
        if (!this.anchored) {
            this.yOffset = this.centreY - nearest.getY();
            this.anchored = true;
        }
        this.centreX = nearest.getX();
        this.centreY = nearest.getY() + this.yOffset;
        this.centreZ = nearest.getZ();
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
        this.trackWhale();
        double cos = Math.cos(this.spin);
        double sin = Math.sin(this.spin);
        double nx = this.offX * cos - this.offZ * sin;
        double nz = this.offX * sin + this.offZ * cos;
        this.offX = nx * this.outward;
        this.offZ = nz * this.outward;
        double bob = Math.sin(this.bobPhase + this.age * 0.22D) * 0.05D;
        this.x = this.centreX + this.offX;
        this.y = this.centreY + bob;
        this.z = this.centreZ + this.offZ;
        float fadeIn = Math.min(1.0F, this.age / 3.0F);
        float fadeOut = Math.min(1.0F, (this.lifetime - this.age) / 8.0F);
        this.alpha = Math.min(fadeIn, fadeOut);
        this.setSpriteFromAge(this.sprites);
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
        return (type, level, x, y, z, dx, dy, dz) ->
                new TokenOrbitParticle(level, x, y, z, dx, dz, sprites);
    }
}
