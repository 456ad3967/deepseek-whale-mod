package com.deepseek.whale.attack;

import com.deepseek.whale.entity.WhalePetEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 胖虎鲸攻击系统（需求 1 + 2）。
 * 触发方式：伙伴自动攻击其周围 12 格内最近的敌对生物，冷却 1.5 秒。
 * 需求1：主目标受 3 点伤害（1.5 心）+ 缓速 III（3 秒）。
 * 需求2：从主目标出发，在 7 格内连锁其他敌对生物，最多额外命中 3 个。
 * 敌对判定：实体生成类别为 MONSTER（覆盖僵尸/骷髅/苦力怕/幻翼等）。
 */
public final class WhaleAttackSystem {

    public static final int ATTACK_INTERVAL_TICKS = 30;      // 1.5 秒冷却
    public static final double ATTACK_RANGE = 12.0D;         // 伙伴索敌半径
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
        Player owner = whale.getOwnerId() == null ? null : level.getPlayerByUUID(whale.getOwnerId());
        if (owner == null) {
            return;
        }

        LivingEntity primary = findNearestHostileAround(level, whale, ATTACK_RANGE, whale, null);
        if (primary == null) {
            return;
        }
        lastAttackTick.put(whale.getUUID(), (int) now);

        List<LivingEntity> excluded = new ArrayList<>();
        excluded.add(primary);
        applyHit(whale, primary);

        LivingEntity cursor = primary;
        for (int i = 0; i < MAX_CHAIN_TARGETS; i++) {
            LivingEntity next = findNearestHostileAround(level, cursor, CHAIN_RANGE, whale, excluded);
            if (next == null) {
                break;
            }
            excluded.add(next);
            applyHit(whale, next);
            cursor = next;
        }
    }

    /** 命中一个目标：3 点伤害 + 缓速 III。 */
    private static void applyHit(WhalePetEntity whale, LivingEntity target) {
        if (!target.isAlive()) {
            return;
        }
        target.hurt(target.level().damageSources().mobAttack(whale), DAMAGE);
        if (target.isAlive()) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOWNESS_TICKS, SLOWNESS_AMPLIFIER), whale);
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
