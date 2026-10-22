package com.deepseek.whale.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/**
 * 追击攻击：当伙伴有目标时，浮游冲向目标并保持攻击距离。
 * 实际攻击节奏由 WhaleAttackSystem（每 1.5 秒，优先打当前目标）负责。
 */
public class FightTargetGoal extends Goal {

    private final WhalePetEntity pet;
    private LivingEntity target;

    public FightTargetGoal(WhalePetEntity pet) {
        this.pet = pet;
    }

    @Override
    public boolean canUse() {
        if (this.pet.level().isClientSide) {
            return false;
        }
        LivingEntity t = this.pet.getTarget();
        if (t == null || !t.isAlive()) {
            return false;
        }
        this.target = t;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity t = this.pet.getTarget();
        if (t == null || !t.isAlive()) {
            return false;
        }
        // 攻击范围内目标仍存活就继续咬住；追出 48 格放弃（回到跟随）
        return this.pet.distanceToSqr(t) < 48.0D * 48.0D;
    }

    @Override
    public void stop() {
        this.target = null;
    }

    @Override
    public void tick() {
        if (this.target == null || !this.target.isAlive()) {
            return;
        }
        this.pet.getLookControl().setLookAt(this.target, 30.0F, 30.0F);

        Vec3 delta = this.target.position()
                .add(0.0D, this.target.getBbHeight() * 0.5D, 0.0D)
                .subtract(this.pet.position());
        double dist = delta.length();

        double speed = 0.35D;
        Vec3 move = Vec3.ZERO;
        if (dist > 2.2D) {
            Vec3 dir = delta.normalize();
            double dy = delta.y * 0.14D;
            dy = Math.max(-0.45D, Math.min(0.45D, dy));
            move = new Vec3(dir.x * speed, dy, dir.z * speed);
        } else if (dist < 1.0D) {
            // 贴太近就稍微退开，保持输出距离
            Vec3 dir = delta.normalize();
            move = new Vec3(-dir.x * 0.06D, 0.0D, -dir.z * 0.06D);
        }
        this.pet.setDeltaMovement(move);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
