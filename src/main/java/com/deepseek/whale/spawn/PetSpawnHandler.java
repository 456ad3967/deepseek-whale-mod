package com.deepseek.whale.spawn;

import com.deepseek.whale.DeepseekWhaleMod;
import com.deepseek.whale.entity.WhalePetEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 伙伴生成与归属系统（需求 4）。
 * 规则：
 * 1. 首次进入某世界时，自动在该玩家附近生成一条胖虎鲸；
 * 2. 玩家每次登录、复活、跨维度后，确保伙伴存在；
 * 3. 伙伴已存在则复用（同维度不动，其他维度/过远则传送到身边），不重复生成；
 * 4. 伙伴归属生成时的玩家，其他玩家无法夺取（由实体归属逻辑保证）。
 */
public final class PetSpawnHandler {

    private static final double SPAWN_DISTANCE = 2.5D;
    private static final double TELEPORT_DISTANCE_SQ = 128.0D * 128.0D;
    private static final String PET_NAME = "人机肥鱼";

    private PetSpawnHandler() {
    }

    public static void init() {
        // 注册动作在 DeepseekWhaleMod 中完成
    }

    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        ensurePet(event.getEntity());
    }

    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        ensurePet(event.getEntity());
    }

    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        ensurePet(event.getEntity());
    }

    private static void ensurePet(net.minecraft.world.entity.player.Player rawPlayer) {
        if (!(rawPlayer instanceof ServerPlayer player)) {
            return;
        }
        ServerLevel current = player.serverLevel();

        // 1) 在所有维度查找归属该玩家的存活伙伴
        WhalePetEntity existing = findOwnedPet(player);

        if (existing != null && existing.isAlive()) {
            // 已存在 → 复用：同维度且近 → 不动；否则传送回身边
            if (existing.level() != current
                    || existing.distanceToSqr(player) > TELEPORT_DISTANCE_SQ) {
                teleportToPlayer(existing, player);
            }
            return;
        }

        // 2) 不存在（首次进入/伙伴消失）→ 在玩家附近生成
        spawnPetNear(player, current);
    }

    private static WhalePetEntity findOwnedPet(ServerPlayer player) {
        for (ServerLevel level : player.server().getAllLevels()) {
            List<WhalePetEntity> pets = level.getEntitiesOfClass(WhalePetEntity.class,
                    level.getWorldBorder().getCollisionBox(),
                    pet -> pet.isAlive() && player.getUUID().equals(pet.getOwnerId()));
            if (!pets.isEmpty()) {
                return pets.get(0);
            }
        }
        return null;
    }

    private static void spawnPetNear(ServerPlayer player, ServerLevel level) {
        Vec3 look = player.getLookAngle();
        if (look.lengthSqr() < 1.0E-4D) {
            look = new Vec3(1.0D, 0.0D, 0.0D);
        }
        Vec3 pos = player.position().add(look.scale(SPAWN_DISTANCE)).add(0.0D, 1.0D, 0.0D);

        WhalePetEntity pet = DeepseekWhaleMod.WHALE_PET.get().create(level);
        if (pet == null) {
            return;
        }
        pet.setPos(pos.x, pos.y, pos.z);
        pet.setOwnerId(player.getUUID());
        pet.setCustomName(Component.literal(PET_NAME));
        level.addFreshEntity(pet);
    }

    private static void teleportToPlayer(WhalePetEntity pet, ServerPlayer player) {
        ServerLevel destination = player.serverLevel();
        Vec3 look = player.getLookAngle();
        Vec3 pos = player.position().add(look.scale(SPAWN_DISTANCE)).add(0.0D, 1.0D, 0.0D);
        BlockPos safe = BlockPos.containing(pos);
        // 悬浮生物不需要落地判定，直接传送到目标点
        pet.teleportTo(destination, safe.getX() + 0.5D, safe.getY() + 0.1D, safe.getZ() + 0.5D,
                player.getYRot(), player.getXRot());
    }
}
