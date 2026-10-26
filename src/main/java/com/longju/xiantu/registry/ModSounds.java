package com.longju.xiantu.registry;

import com.longju.xiantu.XiantuMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, XiantuMod.MOD_ID);
    public static final RegistryObject<SoundEvent> THUNDER_WASTES_ENTRY = SOUNDS.register(
            "terrain.thunder_wastes_entry",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(XiantuMod.MOD_ID, "terrain.thunder_wastes_entry")));
    private ModSounds() {}
}
