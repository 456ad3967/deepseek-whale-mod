package com.deepseek.whale.spawn;

import com.deepseek.whale.DeepseekWhaleMod;
import com.deepseek.whale.entity.WhalePetEntity;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 伙伴生成与归属系统（需求 4）。
 * 规则：
 * 1. 首次进入某世界时，自动在该玩家附近生成一条胖虎鲸；
 * 2. 玩家每次登录、复活、跨维度后，确保伙伴存在；
 * 3. 伙伴已存在则复用（同维度不动，其他维度/过远则传送到身边），不重复生成（依赖 PetData 存档）；
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

    /**
     * 去重兜底：同一主人名下同时出现多条鲸鱼时（旧存档残留/未加载区块导致），
     * 只保留离主人最近的一条，其余立即移除。防止"旧实体 + 新实体"并存。
     */
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof WhalePetEntity pet)) {
            return;
        }
        Level level = pet.level();
        if (level.isClientSide) {
            return;
        }
        UUID owner = pet.getOwnerId();
        if (owner == null) {
            return;
        }
        WorldBorder border = level.getWorldBorder();
        AABB area = new AABB(border.getMinX(), level.getMinBuildHeight(), border.getMinZ(),
                border.getMaxX(), level.getMaxBuildHeight(), border.getMaxZ());
        List<WhalePetEntity> same = level.getEntitiesOfClass(WhalePetEntity.class, area,
                p -> p.isAlive() && owner.equals(p.getOwnerId()));
        if (same.size() <= 1) {
            return;
        }
        // 找到主人（可能不在本维度）
        Player ownerPlayer = null;
        if (level instanceof ServerLevel sl && sl.getServer() != null) {
            ownerPlayer = sl.getServer().getPlayerList().getPlayer(owner);
        }
        // 保留离主人最近的一条
        WhalePetEntity keep = null;
        double best = Double.MAX_VALUE;
        for (WhalePetEntity e : same) {
            double d = (ownerPlayer != null) ? e.distanceToSqr(ownerPlayer) : 0.0D;
            if (keep == null || d < best) {
                keep = e;
                best = d;
            }
        }
        for (WhalePetEntity e : same) {
            if (e != keep) {
                e.remove(Entity.RemovalReason.DISCARDED);
            }
        }
    }

    private static void ensurePet(net.minecraft.world.entity.player.Player rawPlayer) {
        if (!(rawPlayer instanceof ServerPlayer player)) {
            return;
        }
        ServerLevel current = player.serverLevel();
        UUID ownerId = player.getUUID();
        PetData data = PetData.get(current);

        // 1) 优先查持久化存档：有记录 → 在已加载实体中找；找不到（区块未加载）也绝不新生成，
        //    旧伙伴会随区块加载，并由实体的"主人过远自动传送"逻辑回到身边，避免重进世界重复生成。
        UUID recorded = data.petFor(ownerId);
        if (recorded != null) {
            WhalePetEntity existing = findPetByUuid(player, recorded);
            if (existing != null && existing.isAlive()) {
                if (existing.level() != current
                        || existing.distanceToSqr(player) > TELEPORT_DISTANCE_SQ) {
                    teleportToPlayer(existing, player);
                }
            }
            return;
        }

        // 2) 无存档记录（旧档/异常残留）：全维度扫描存量伙伴，认领最近一条并写入存档，删除多余者。
        List<WhalePetEntity> owned = findAllOwnedPets(player);
        if (!owned.isEmpty()) {
            WhalePetEntity keep = nearestTo(owned, player);
            data.bind(ownerId, keep.getUUID());
            for (WhalePetEntity e : owned) {
                if (e != keep) {
                    e.remove(Entity.RemovalReason.DISCARDED);
                }
            }
            return;
        }

        // 3) 真的不存在（首次进入/伙伴彻底消失）→ 生成并记录存档
        WhalePetEntity pet = spawnPetNear(player, current);
        if (pet != null) {
            data.bind(ownerId, pet.getUUID());
        }
    }

    /** 在所有维度已加载实体中按 UUID 找伙伴。 */
    private static WhalePetEntity findPetByUuid(ServerPlayer player, UUID petUuid) {
        for (ServerLevel level : player.getServer().getAllLevels()) {
            WorldBorder border = level.getWorldBorder();
            AABB area = new AABB(border.getMinX(), level.getMinBuildHeight(), border.getMinZ(),
                    border.getMaxX(), level.getMaxBuildHeight(), border.getMaxZ());
            List<WhalePetEntity> pets = level.getEntitiesOfClass(WhalePetEntity.class, area,
                    pet -> pet.isAlive() && petUuid.equals(pet.getUUID()));
            if (!pets.isEmpty()) {
                return pets.get(0);
            }
        }
        return null;
    }

    /** 在所有维度已加载实体中查找归属该玩家的全部伙伴。 */
    private static List<WhalePetEntity> findAllOwnedPets(ServerPlayer player) {
        List<WhalePetEntity> result = new java.util.ArrayList<>();
        for (ServerLevel level : player.getServer().getAllLevels()) {
            WorldBorder border = level.getWorldBorder();
            AABB area = new AABB(border.getMinX(), level.getMinBuildHeight(), border.getMinZ(),
                    border.getMaxX(), level.getMaxBuildHeight(), border.getMaxZ());
            result.addAll(level.getEntitiesOfClass(WhalePetEntity.class, area,
                    pet -> pet.isAlive() && player.getUUID().equals(pet.getOwnerId())));
        }
        return result;
    }

    private static WhalePetEntity nearestTo(List<WhalePetEntity> pets, ServerPlayer player) {
        WhalePetEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (WhalePetEntity e : pets) {
            double d = e.distanceToSqr(player);
            if (best == null || d < bestDist) {
                best = e;
                bestDist = d;
            }
        }
        return best;
    }

    private static WhalePetEntity spawnPetNear(ServerPlayer player, ServerLevel level) {
        Vec3 look = player.getLookAngle();
        if (look.lengthSqr() < 1.0E-4D) {
            look = new Vec3(1.0D, 0.0D, 0.0D);
        }
        Vec3 pos = player.position().add(look.scale(SPAWN_DISTANCE)).add(0.0D, 1.0D, 0.0D);

        WhalePetEntity pet = DeepseekWhaleMod.WHALE_PET.get().create(level);
        if (pet == null) {
            return null;
        }
        pet.setPos(pos.x, pos.y, pos.z);
        pet.setOwnerId(player.getUUID());
        pet.setCustomName(Component.literal(PET_NAME));
        level.addFreshEntity(pet);
        return pet;
    }

    private static void teleportToPlayer(WhalePetEntity pet, ServerPlayer player) {
        ServerLevel destination = player.serverLevel();
        Vec3 look = player.getLookAngle();
        Vec3 pos = player.position().add(look.scale(SPAWN_DISTANCE)).add(0.0D, 1.0D, 0.0D);
        BlockPos safe = BlockPos.containing(pos);
        // 悬浮生物不需要落地判定，直接传送到目标点
        pet.teleportTo(destination, safe.getX() + 0.5D, safe.getY() + 0.1D, safe.getZ() + 0.5D,
                Set.of(), player.getYRot(), player.getXRot());
    }
}
