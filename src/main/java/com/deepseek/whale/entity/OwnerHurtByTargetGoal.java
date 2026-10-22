package com.deepseek.whale.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

/**
 * 狼式目标选择：当主人被攻击时，把攻击者设为伙伴目标。
 * 等价于原版驯服动物的 OwnerHurtByTargetGoal。
 * 过滤：只反击敌对怪物（MONSTER 类别），不误伤女仆等友好/中立生物。
 */
public class OwnerHurtByTargetGoal extends Goal {

    private final WhalePetEntity pet;
    private LivingEntity owner;
    private LivingEntity attacker;
    private int timestamp;

    public OwnerHurtByTargetGoal(WhalePetEntity pet) {
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
        LivingEntity mob = p.getLastHurtByMob();
        if (mob == null || !mob.isAlive()) {
            return false;
        }
        if (mob == this.pet || mob == this.owner) {
            return false;
        }
        // 只反击敌对怪物；女仆/村民/动物等友好生物不参与
        if (mob.getType().getCategory() != MobCategory.MONSTER) {
            return false;
        }
        this.attacker = mob;
        this.timestamp = p.getLastHurtByMobTimestamp();
        return true;
    }

    @Override
    public void start() {
        this.pet.setTarget(this.attacker);
    }

    @Override
    public boolean canContinueToUse() {
        return this.attacker != null
                && this.attacker.isAlive()
                && this.owner != null
                && this.owner.isAlive()
                && this.owner.getLastHurtByMobTimestamp() == this.timestamp
                && this.pet.getTarget() == this.attacker;
    }

    @Override
    public void stop() {
        this.owner = null;
        this.attacker = null;
    }
}
