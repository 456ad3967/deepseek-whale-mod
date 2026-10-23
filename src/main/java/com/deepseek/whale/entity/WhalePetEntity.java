package com.deepseek.whale.entity;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 胖虎鲸伙伴实体。
 * 核心不变量：归属玩家（其他玩家无法夺取）；免疫一切伤害；不受重力；不被击退；
 * 不被自然清除；不产生经验；常态悬浮跟随主人。
 */
public class WhalePetEntity extends PathfinderMob {

    private static final String TAG_OWNER = "Owner";
    private static final String TAG_KEEPER = "Keeper";
    private static final String TAG_SITTING = "Sitting";

    @Nullable
    private UUID ownerId;

    /** 待命标记：true 时悬浮原地不跟随主人（右键切换）。 */
    private boolean sitting;

    public WhalePetEntity(EntityType<? extends WhalePetEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setNoGravity(true);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    // ------------------------------------------------------------------
    // 归属
    // ------------------------------------------------------------------

    public void setOwnerId(@Nullable UUID uuid) {
        this.ownerId = uuid;
    }

    @Nullable
    public UUID getOwnerId() {
        return this.ownerId;
    }

    public boolean isOwnedBy(Player player) {
        return this.ownerId != null && this.ownerId.equals(player.getUUID());
    }

    // ------------------------------------------------------------------
    // 需求4：免疫伤害 / 不受击退 / 无经验
    // ------------------------------------------------------------------

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false; // 免疫一切伤害
    }

    @Override
    public void knockback(double strength, double x, double z) {
        // 不受击退
    }

    public int getExperienceReward() {
        return 0; // 不产生经验（1.21.1 无父类同签名，作为自定义方法保留）
    }

    @Override
    public boolean isPushable() {
        return false; // 不被推挤
    }

    @Override
    public void push(net.minecraft.world.entity.Entity entity) {
        // 与任何实体互不推动：鱼移动穿过玩家时不再顶着玩家走
    }

    @Override
    public boolean canBeCollidedWith() {
        return false; // 不阻挡玩家行走（但保留碰撞箱用于点击）
    }

    public boolean canBeLeashed(Player player) {
        return false; // 不可牵绳，防止他人牵走
    }

    // ------------------------------------------------------------------
    // 需求（迭代）：不挡放置方块 + 右键待机
    // ------------------------------------------------------------------

    /**
     * 手持物品时射线穿透（玩家拿方块/工具时不挡放置方块）；
     * 空手时可被射线命中，用于右键切换待机。
     */
    @Override
    public boolean isPickable() {
        if (this.level().isClientSide) {
            Player p = this.level().getNearestPlayer(this, 8.0D);
            return p == null || p.getMainHandItem().isEmpty();
        }
        return true;
    }

    /** 主人空手右键：切换 待命/跟随。 */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.isOwnedBy(player)) {
            return InteractionResult.PASS; // 非主人不可操作，他人无法夺取
        }
        if (player.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        this.sitting = !this.sitting;
        player.displayClientMessage(
                Component.literal(this.sitting
                        ? "§e[人机肥鱼] 原地待命中~"
                        : "§e[人机肥鱼] 出发！跟上！"),
                true);
        return InteractionResult.SUCCESS;
    }

    public boolean isSitting() {
        return this.sitting;
    }

    // ------------------------------------------------------------------
    // 保存
    // ------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.ownerId != null) {
            tag.putUUID(TAG_OWNER, this.ownerId);
        }
        if (this.getCustomName() != null) {
            tag.putString(TAG_KEEPER, this.getCustomName().getString());
        }
        tag.putBoolean(TAG_SITTING, this.sitting);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID(TAG_OWNER)) {
            this.ownerId = tag.getUUID(TAG_OWNER);
        }
        // keeper 名用于展示，实体命名由生成系统负责，这里只恢复归属
        this.sitting = tag.getBoolean(TAG_SITTING);
    }

    // ------------------------------------------------------------------
    // 行为：悬浮跟随主人
    // ------------------------------------------------------------------

    @Override
    protected void registerGoals() {
        // 狼式目标选择：主人被攻击 → 反击；主人攻击 → 跟随攻击
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        // 有目标时追击，无目标时跟随主人
        this.goalSelector.addGoal(0, new FightTargetGoal(this));
        this.goalSelector.addGoal(1, new FollowOwnerGoal());
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            this.setNoGravity(true);
            this.fallDistance = 0.0F; // 免疫摔落积累
            this.setAirSupply(this.getMaxAirSupply()); // 水下不缺氧
            if (this.sitting && this.getTarget() == null) {
                // 待命且无战斗目标：悬浮原地不动
                this.setDeltaMovement(Vec3.ZERO);
            }
            // 偶尔播放原版闲置音效（约 12 秒一次，二选一），移动时撒蓝色尾迹粒子
            if (this.tickCount % 240 == 0 && this.random.nextInt(3) == 0) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        this.random.nextBoolean()
                                ? com.deepseek.whale.DeepSeekSounds.WHALE_IDLE.get()
                                : com.deepseek.whale.DeepSeekSounds.WHALE_IDLE2.get(),
                        net.minecraft.sounds.SoundSource.NEUTRAL, 0.5F, 1.0F);
            }
            if (this.getDeltaMovement().horizontalDistanceSqr() > 0.0025D && this.random.nextInt(3) == 0) {
                ((ServerLevel) this.level()).sendParticles(com.deepseek.whale.DeepSeekParticles.TOKEN_TRAIL.get(),
                        this.getX() + (this.random.nextDouble() - 0.5D) * 0.16D,
                        this.getY() + 0.15D + this.random.nextDouble() * 0.3D,
                        this.getZ() + (this.random.nextDouble() - 0.5D) * 0.16D,
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            // 原版 aura：身体周围持续环绕白色"10"字符粒子（token_spark / token_orbit 均为"10"贴图）
            if (this.tickCount % 10 == 0) {
                ((ServerLevel) this.level()).sendParticles(com.deepseek.whale.DeepSeekParticles.TOKEN_ORBIT.get(),
                        this.getX(), this.getY() + 0.4D, this.getZ(),
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            if (this.tickCount % 14 == 0) {
                ((ServerLevel) this.level()).sendParticles(com.deepseek.whale.DeepSeekParticles.TOKEN_SPARK.get(),
                        this.getX() + (this.random.nextDouble() - 0.5D) * 1.2D,
                        this.getY() + 0.3D + this.random.nextDouble() * 1.0D,
                        this.getZ() + (this.random.nextDouble() - 0.5D) * 1.2D,
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            com.deepseek.whale.attack.WhaleAttackSystem.tickWhale(this, (ServerLevel) this.level());
        }
    }

    /** 跟随主人：距离 > 2.5 格时游向主人，靠近后悬停在其斜上方。 */
    private class FollowOwnerGoal extends Goal {

        private Player owner;

        FollowOwnerGoal() {
        }

        @Override
        public boolean canUse() {
            if (WhalePetEntity.this.level().isClientSide) {
                return false;
            }
            if (WhalePetEntity.this.sitting) {
                return false; // 待命中不跟随
            }
            if (WhalePetEntity.this.getTarget() != null) {
                return false; // 战斗中不跟随，交给 FightTargetGoal
            }
            if (WhalePetEntity.this.ownerId == null) {
                return false;
            }
            Player p = WhalePetEntity.this.level().getPlayerByUUID(WhalePetEntity.this.ownerId);
            if (p == null || !p.isAlive()) {
                return false;
            }
            this.owner = p;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return this.owner != null && this.owner.isAlive()
                    && WhalePetEntity.this.distanceToSqr(this.owner) < 128.0D * 128.0D;
        }

        @Override
        public void start() {
            // 无额外启动逻辑
        }

        @Override
        public void stop() {
            this.owner = null;
        }

        @Override
        public void tick() {
            if (this.owner == null) {
                return;
            }
            // 跟随锚点：主人身后 2.5 格、头顶偏上，避免贴脸堵路
            Vec3 look = this.owner.getLookAngle();
            Vec3 back = new Vec3(look.x, 0.0D, look.z);
            if (back.lengthSqr() < 1.0E-4D) {
                back = new Vec3(1.0D, 0.0D, 0.0D);
            } else {
                back = back.normalize().scale(-2.5D);
            }
            Vec3 anchor = this.owner.position().add(back.x, 0.9D, back.z);
            Vec3 delta = anchor.subtract(WhalePetEntity.this.position());
            double dist = delta.length();

            WhalePetEntity.this.getLookControl().setLookAt(this.owner, 30.0F, 30.0F);

            // 跑远了（>32 格）直接传送到主人身边，等价于原版 PendingTeleports 兜底
            if (dist > 32.0D) {
                WhalePetEntity.this.teleportTo(
                        (net.minecraft.server.level.ServerLevel) WhalePetEntity.this.level(),
                        this.owner.getX(), this.owner.getY() + 1.0D, this.owner.getZ(),
                        java.util.Set.of(),
                        this.owner.getYRot(), 0.0F);
                WhalePetEntity.this.setDeltaMovement(Vec3.ZERO);
                // 到达音效（原版 WHALE_ARRIVE）
                WhalePetEntity.this.level().playSound(null,
                        this.owner.getX(), this.owner.getY(), this.owner.getZ(),
                        com.deepseek.whale.DeepSeekSounds.WHALE_ARRIVE.get(),
                        net.minecraft.sounds.SoundSource.NEUTRAL, 0.7F, 1.0F);
                return;
            }

            double speed = 0.22D;
            Vec3 move = Vec3.ZERO;
            if (dist > 3.0D) {
                // 离锚点太远：靠近
                Vec3 dir = delta.normalize();
                double dy = delta.y * 0.12D;
                dy = Math.max(-0.4D, Math.min(0.4D, dy));
                move = new Vec3(dir.x * speed, dy, dir.z * speed);
            } else if (dist < 2.0D) {
                // 太近：缓退
                Vec3 dir = delta.normalize();
                move = new Vec3(-dir.x * 0.05D, 0.0D, -dir.z * 0.05D);
            }
            WhalePetEntity.this.setDeltaMovement(move);

            // 没有目标时轻微上下浮动（呼吸感）
            double bob = Math.sin(WhalePetEntity.this.tickCount * 0.08D) * 0.02D;
            WhalePetEntity.this.setPos(
                    WhalePetEntity.this.getX(),
                    WhalePetEntity.this.getY() + bob,
                    WhalePetEntity.this.getZ());
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }
    }
}
