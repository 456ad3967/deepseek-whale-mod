package com.deepseek.whale.attack;

import com.deepseek.whale.DeepSeekParticles;
import com.deepseek.whale.DeepSeekSounds;
import com.deepseek.whale.DeepseekWhaleMod;
import com.deepseek.whale.entity.WhalePetEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 胖虎鲸攻击系统（需求 1 + 2）。
 * 触发方式：优先攻击伙伴当前目标（狼式目标选择器设定），无目标时自动索敌周围 12 格内最近的敌对生物，冷却 1.5 秒。
 * 需求1：主目标受 3 点伤害（1.5 心）+ 缓速 III（3 秒）。
 * 需求2：从主目标出发，在 7 格内连锁其他敌对生物，最多额外命中 3 个。
 * 特效：红色代码流（定制）+ 原版 kill_impact/kill_text/爆炸粒子与音效（照抄 0.1.0 WhaleAttack）。
 */
public final class WhaleAttackSystem {

    public static final int ATTACK_INTERVAL_TICKS = 30;      // 1.5 秒冷却
    public static final double ATTACK_RANGE = 12.0D;         // 自动索敌半径
    public static final float DAMAGE = 3.0F;                 // 3 点伤害 = 1.5 心
    public static final int SLOWNESS_TICKS = 60;             // 缓速 3 秒
    public static final int SLOWNESS_AMPLIFIER = 2;          // 等级 III
    public static final double CHAIN_RANGE = 7.0D;           // 连锁半径
    public static final int MAX_CHAIN_TARGETS = 3;           // 最多连锁 3 个额外目标

    private static final int SCAN_INTERVAL = 5;              // 每 5 tick 扫描一次
    private static final Map<UUID, Integer> lastAttackTick = new HashMap<>();

    private WhaleAttackSystem() {
    }

    public static void init() {
        // 无独立注册；由 WhalePetEntity#tick 驱动
    }

    /** 由实体 tick 调用（仅服务端）。 */
    public static void tickWhale(WhalePetEntity whale, ServerLevel level) {
        long now = level.getGameTime();
        if (now % SCAN_INTERVAL != 0L) {
            return;
        }
        int last = lastAttackTick.getOrDefault(whale.getUUID(), Integer.MIN_VALUE);
        if (now - last < ATTACK_INTERVAL_TICKS) {
            return;
        }

        LivingEntity primary = whale.getTarget();
        if (primary == null || !primary.isAlive() || primary == whale) {
            Player owner = whale.getOwnerId() == null ? null : level.getPlayerByUUID(whale.getOwnerId());
            if (owner == null) {
                return;
            }
            primary = findNearestHostileAround(level, whale, ATTACK_RANGE, whale, null);
        }
        if (primary == null) {
            return;
        }
        lastAttackTick.put(whale.getUUID(), (int) now);
        // 发射音效（原版 launch 播放 WHALE_RUN）
        level.playSound(null, whale.getX(), whale.getY(), whale.getZ(),
                DeepSeekSounds.WHALE_RUN.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
        hitTarget(whale, primary, level);
    }

    /** 命中主目标并连锁：供 FightTargetGoal / 自动索敌共用。 */
    public static void hitTarget(WhalePetEntity whale, LivingEntity primary, ServerLevel level) {
        List<LivingEntity> excluded = new ArrayList<>();
        excluded.add(primary);
        applyHit(whale, primary, level);

        LivingEntity cursor = primary;
        // 射线轨迹撒原版标记粒子（原版 drawHead 的 KILL_TEXT）
        for (int i = 0; i < MAX_CHAIN_TARGETS; i++) {
            Vec3 from = cursor.position();
            LivingEntity next = findNearestHostileAround(level, cursor, CHAIN_RANGE, whale, excluded);
            if (next == null) {
                break;
            }
            excluded.add(next);
            Vec3 to = next.position();
            spawnKillTextAlong(level, from, to);
            applyHit(whale, next, level);
            cursor = next;
        }
    }

    /** 命中一个目标：3 点伤害 + 缓速 III + 代码流 + 原版冲击波特效。 */
    private static void applyHit(WhalePetEntity whale, LivingEntity target, ServerLevel level) {
        if (!target.isAlive()) {
            return;
        }
        target.hurt(target.level().damageSources().mobAttack(whale), DAMAGE);
        if (target.isAlive()) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOWNESS_TICKS, SLOWNESS_AMPLIFIER), whale);
        }
        spawnCodeRain(level, target);
        // 原版 land：kill_impact + 爆炸粒子 + 爆炸音效
        Vec3 p = target.position();
        level.sendParticles(DeepSeekParticles.KILL_IMPACT.get(),
                p.x, p.y + target.getBbHeight() * 0.5D, p.z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.EXPLOSION,
                p.x, p.y + target.getBbHeight() * 0.5D, p.z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.playSound(null, p.x, p.y, p.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 0.7F, 1.8F);
        level.playSound(null, p.x, p.y, p.z,
                DeepSeekSounds.WHALE_ACK.get(), SoundSource.HOSTILE, 0.9F, 1.0F);
    }

    /** 红色代码流特效：命中位置喷出一簇红色字符粒子。 */
    private static void spawnCodeRain(ServerLevel level, LivingEntity target) {
        Vec3 p = target.position();
        level.sendParticles(DeepseekWhaleMod.CODE_RAIN.get(),
                p.x, p.y + target.getBbHeight() * 0.8D, p.z,
                18, 0.4D, 0.8D, 0.4D, 0.35D);
    }

    /** 沿连锁射线方向撒原版 kill_text 标记粒子。 */
    private static void spawnKillTextAlong(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 dir = to.subtract(from);
        double len = dir.length();
        if (len < 0.01D) {
            return;
        }
        Vec3 step = dir.scale(1.0D / Math.max(1.0D, len * 2.0D));
        for (double t = 0.5D; t < len; t += 0.5D) {
            Vec3 at = from.add(step.scale(t * 2.0D));
            level.sendParticles(DeepSeekParticles.KILL_TEXT.get(),
                    at.x, at.y + 0.5D, at.z,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    /** 以中心实体为圆心，在半径 range 内找最近的敌对生物。 */
    private static LivingEntity findNearestHostileAround(ServerLevel level, LivingEntity center,
                                                         double range, WhalePetEntity whale,
                                                         List<LivingEntity> exclude) {
        AABB box = center.getBoundingBox().inflate(range);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != null
                        && e.isAlive()
                        && e != whale
                        && e != center
                        && e.getType().getCategory() == MobCategory.MONSTER
                        && (exclude == null || !exclude.contains(e))
                        && e.distanceToSqr(center) <= range * range);
        if (candidates.isEmpty()) {
            return null;
        }
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        Vec3 centerPos = center.position();
        for (LivingEntity c : candidates) {
            double d = c.distanceToSqr(centerPos);
            if (d < bestDist) {
                bestDist = d;
                best = c;
            }
        }
        return best;
    }
}
