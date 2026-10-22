package com.deepseek.whale;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 原版音效注册（照抄 deepseek_whale-0.1.0 的 DeepSeekSounds，ogg 资源同原版）。 */
public final class DeepSeekSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, DeepseekWhaleMod.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> WHALE_IDLE = register("whale_idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> WHALE_IDLE2 = register("whale_idle2");
    public static final DeferredHolder<SoundEvent, SoundEvent> WHALE_ACK = register("whale_ack");
    public static final DeferredHolder<SoundEvent, SoundEvent> WHALE_SPEAK = register("whale_speak");
    public static final DeferredHolder<SoundEvent, SoundEvent> WHALE_RUN = register("whale_run");
    public static final DeferredHolder<SoundEvent, SoundEvent> WHALE_ARRIVE = register("whale_arrive");

    private DeepSeekSounds() {
    }

    public static void register(IEventBus bus) {
        SOUNDS.register(bus);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name,
                () -> SoundEvent.createVariableRangeEvent(
                        ResourceLocation.fromNamespaceAndPath(DeepseekWhaleMod.MODID, name)));
    }
}
