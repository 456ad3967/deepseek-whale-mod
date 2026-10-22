package com.deepseek.whale.spawn;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * 伙伴持久化存档：记录"主人 UUID → 伙伴 UUID"的绑定关系。
 * 解决重进世界/区块未加载时 findOwnedPet 扫不到旧伙伴而重复生成的问题。
 * 存档位置：世界 data 目录下的 deepseek_whale_pets。
 */
public final class PetData extends SavedData {

    public static final String NAME = "deepseek_whale_pets";
    private static final String KEY_OWNER = "owner";
    private static final String KEY_PET = "pet";

    private final Map<UUID, UUID> ownerToPet = new HashMap<>();

    public PetData() {
    }

    public static PetData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage()
                .computeIfAbsent(new Factory<PetData>(PetData::new, PetData::load, null), NAME);
    }

    /** 该主人的伙伴 UUID；无记录返回 null。 */
    public UUID petFor(UUID owner) {
        return ownerToPet.get(owner);
    }

    /** 绑定/更新 主人→伙伴 关系。 */
    public void bind(UUID owner, UUID pet) {
        ownerToPet.put(owner, pet);
        setDirty();
    }

    public static PetData load(CompoundTag tag, HolderLookup.Provider registries) {
        PetData data = new PetData();
        ListTag list = tag.getList("pets", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            try {
                UUID owner = UUID.fromString(entry.getString(KEY_OWNER));
                UUID pet = UUID.fromString(entry.getString(KEY_PET));
                data.ownerToPet.put(owner, pet);
            } catch (IllegalArgumentException ignored) {
                // 损坏条目跳过
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, UUID> e : ownerToPet.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putString(KEY_OWNER, e.getKey().toString());
            entry.putString(KEY_PET, e.getValue().toString());
            list.add(entry);
        }
        tag.put("pets", list);
        return tag;
    }
}
