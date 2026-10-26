package com.longju.xiantu.registry;

import com.longju.xiantu.XiantuMod;
import com.longju.xiantu.item.SpiritStoneDowncraftRecipe;
import com.longju.xiantu.medicine.SpiritSoilRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, XiantuMod.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, XiantuMod.MOD_ID);
    public static final RegistryObject<RecipeSerializer<com.longju.xiantu.refining.FacilityCraftingRecipe>> FACILITY_CRAFTING = SERIALIZERS.register("facility_crafting",com.longju.xiantu.refining.FacilityCraftingRecipe.Serializer::new);
    public static final RegistryObject<RecipeType<SpiritStoneDowncraftRecipe>> SPIRIT_STONE_DOWNCRAFT = TYPES.register("spirit_stone_downcraft", () -> new RecipeType<>() {
        @Override public String toString() { return "xiantu:spirit_stone_downcraft"; }
    });
    public static final RegistryObject<RecipeSerializer<SpiritStoneDowncraftRecipe>> SPIRIT_STONE_DOWNCRAFT_SERIALIZER = SERIALIZERS.register(
            "spirit_stone_downcraft", SpiritStoneDowncraftRecipe.Serializer::new);
    public static final RegistryObject<RecipeType<SpiritSoilRecipe>> SPIRIT_SOIL = TYPES.register("spirit_soil", () -> new RecipeType<>() {
        @Override public String toString() { return "xiantu:spirit_soil"; }
    });
    public static final RegistryObject<RecipeSerializer<SpiritSoilRecipe>> SPIRIT_SOIL_SERIALIZER = SERIALIZERS.register(
            "spirit_soil", SpiritSoilRecipe.Serializer::new);

    private ModRecipes() {}
}
