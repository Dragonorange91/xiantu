package com.longju.xiantu.registry;

import com.longju.xiantu.XiantuMod;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, XiantuMod.MOD_ID);
    public static final net.minecraftforge.registries.RegistryObject<MobEffect> BEAST_SNARE = EFFECTS.register("beast_snare", com.longju.xiantu.effect.BeastSnareEffect::new);
    private ModEffects() {}
}
