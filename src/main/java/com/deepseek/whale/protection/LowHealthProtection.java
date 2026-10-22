package com.deepseek.whale.protection;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 低血保护系统（需求 3）。
 * 触发条件：玩家生命值低于 10 点。
 * 效果：以玩家为中心 10 格内所有敌对生物被击退 III（明显位移 + 向上分量），
 *       并给玩家 12 点（6 心）伤害吸收（金心）。
 * 冷却：每次触发后 5 分钟（6000 tick），随玩家存档持久化。
 */
public final class LowHealthProtection {

    public static final double TRIGGER_HEALTH = 10.0D;      // 生命 < 10 触发
    public static final double KNOCKBACK_RANGE = 10.0D;     // 10 格范围
    public static final double KNOCKBACK_STRENGTH = 2.2D;   // 明显位移
    public static final double KNOCKBACK_UP = 0.6D;         // 向上分量
    public static final float ABSORPTION_HEARTS = 12.0F;    // 6 心 = 12 点吸收
    public static final int COOLDOWN_TICKS = 6000;          // 5 分钟

    private static final String CD_TAG = "deepseek_whale_protect_cd_until";

    private LowHealthProtection() {
    }

    public static void init() {
        // 由 DeepseekWhaleMod 注册监听
    }

    /** 事件入口（注册于 NeoForge.EVENT_BUS，服务端逻辑在内部判定）。 */
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        if (!(player instanceof ServerPlayer sp)) {
            return;
        }
        if (sp.getHealth() >= TRIGGER_HEALTH) {
            return;
        }
        Level level = sp.level();
        long now = level.getGameTime();
        long cdUntil = sp.getPersistentData().getLong(CD_TAG);
        if (now < cdUntil) {
            return;
        }
        sp.getPersistentData().putLong(CD_TAG, now + COOLDOWN_TICKS);

        knockbackHostiles(sp, level);
        sp.addAbsorptionHealth(ABSORPTION_HEARTS);
    }

    /** 击退玩家 10 格内所有敌对生物，带向上分量。 */
    private static void knockbackHostiles(ServerPlayer player, Level level) {
        AABB box = player.getBoundingBox().inflate(KNOCKBACK_RANGE);
        List<LivingEntity> hostiles = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != null
                        && e.isAlive()
                        && e != player
                        && e.getType().getCategory() == MobCategory.MONSTER
                        && e.distanceToSqr(player) <= KNOCKBACK_RANGE * KNOCKBACK_RANGE);
        for (LivingEntity hostile : hostiles) {
            Vec3 to = hostile.position().subtract(player.position());
            if (to.lengthSqr() < 1.0E-4D) {
                to = new Vec3(level.random.nextDouble() - 0.5D, 0.0D, level.random.nextDouble() - 0.5D);
            }
            Vec3 dir = to.normalize();
            // 击退 III 效果：强水平击退 + 明显向上分量
            hostile.knockback(KNOCKBACK_STRENGTH, dir.x, dir.z);
            hostile.setDeltaMovement(hostile.getDeltaMovement().add(0.0D, KNOCKBACK_UP, 0.0D));
            // 短暂减速，让"保护波"有停顿感
            hostile.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
        }
    }
}
