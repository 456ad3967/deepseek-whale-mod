package com.deepseek.whale.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

/**
 * 狼式目标选择：主人最近攻击的实体成为伙伴目标。
 * 等价于原版驯服动物的 OwnerHurtTargetGoal。
 */
public class OwnerHurtTargetGoal extends Goal {

    private final WhalePetEntity pet;
    private LivingEntity owner;
    private LivingEntity target;
    private int timestamp;

    public OwnerHurtTargetGoal(WhalePetEntity pet) {
        this.pet = pet;
    }

    @Override
    public boolean canUse() {
        if (this.pet.level().isClientSide) {
            return false;
        }
        if (this.pet.getOwnerId() == null) {
            return false;
        }
        Player p = this.pet.level().getPlayerByUUID(this.pet.getOwnerId());
        if (p == null || !p.isAlive()) {
            return false;
        }
        this.owner = p;
        LivingEntity mob = p.getLastHurtMob();
        if (mob == null || !mob.isAlive()) {
            return false;
        }
        if (mob == this.pet || mob == this.owner) {
            return false;
        }
        this.target = mob;
        this.timestamp = p.getLastHurtMobTimestamp();
        return true;
    }

    @Override
    public void start() {
        this.pet.setTarget(this.target);
    }

    @Override
    public boolean canContinueToUse() {
        return this.target != null
                && this.target.isAlive()
                && this.owner != null
                && this.owner.isAlive()
                && this.owner.getLastHurtMobTimestamp() == this.timestamp
                && this.pet.getTarget() == this.target;
    }

    @Override
    public void stop() {
        this.owner = null;
        this.target = null;
    }
}
