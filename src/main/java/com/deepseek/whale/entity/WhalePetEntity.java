package com.deepseek.whale.entity;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
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

    @Nullable
    private UUID ownerId;

    public WhalePetEntity(EntityType<? extends WhalePetEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired(true);
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

    @Override
    public int getExperienceReward() {
        return 0; // 不产生经验
    }

    @Override
    public boolean isPushable() {
        return false; // 不被推挤
    }

    @Override
    public boolean canBeCollidedWith() {
        return false; // 不阻挡玩家行走（但保留碰撞箱用于点击）
    }

    @Override
    public boolean canBeLeashed(Player player) {
        return false; // 不可牵绳，防止他人牵走
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
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID(TAG_OWNER)) {
            this.ownerId = tag.getUUID(TAG_OWNER);
        }
        // keeper 名用于展示，实体命名由生成系统负责，这里只恢复归属
    }

    // ------------------------------------------------------------------
    // 行为：悬浮跟随主人
    // ------------------------------------------------------------------

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FollowOwnerGoal());
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            this.setNoGravity(true);
            this.fallDistance = 0.0F; // 免疫摔落积累
            this.setAirSupply(this.getMaxAirSupply()); // 水下不缺氧
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
                    && WhalePetEntity.this.distanceToSqr(this.owner) < 64.0D * 64.0D;
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
            Vec3 delta = this.owner.position()
                    .add(0.0D, 0.8D, 0.0D)
                    .subtract(WhalePetEntity.this.position());
            double dist = delta.length();

            WhalePetEntity.this.getLookControl().setLookAt(this.owner, 30.0F, 30.0F);

            double speed = 0.22D;
            Vec3 move = Vec3.ZERO;
            if (dist > 2.5D) {
                // 向主人游去：水平按方向，垂直按高度差平滑逼近
                Vec3 dir = delta.normalize();
                double dy = delta.y * 0.12D;
                dy = Math.max(-0.4D, Math.min(0.4D, dy));
                move = new Vec3(dir.x * speed, dy, dir.z * speed);
            } else if (dist < 1.4D) {
                // 太近就缓退一点
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
