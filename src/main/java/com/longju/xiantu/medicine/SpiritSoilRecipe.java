package com.longju.xiantu.medicine;

import com.google.gson.JsonObject;
import com.longju.xiantu.item.SpiritStoneItem;
import com.longju.xiantu.item.SpiritStoneTier;
import com.longju.xiantu.registry.ModItems;
import com.longju.xiantu.registry.ModRecipes;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;

/** Direct workbench recipe for spirit soil; spirit stones must be full. */
public final class SpiritSoilRecipe extends ShapelessRecipe {
    private final int soilTier;

    public SpiritSoilRecipe(ResourceLocation id, int soilTier) {
        super(id, "", CraftingBookCategory.MISC, new ItemStack(MedicineRegistry.SOILS.get(soilTier).get(), 4), ingredients(soilTier));
        if (soilTier < 0 || soilTier > 6) throw new IllegalArgumentException("Spirit soil tier must be 0..6");
        this.soilTier = soilTier;
    }

    private static NonNullList<Ingredient> ingredients(int tier) {
        NonNullList<Ingredient> list = NonNullList.create();
        if (tier == 0) {
            for (int i = 0; i < 4; i++) list.add(Ingredient.of(net.minecraft.world.level.block.Blocks.DIRT));
            list.add(Ingredient.of(net.minecraft.world.item.Items.BONE_MEAL));
            return list;
        }
        for (int i = 0; i < 4; i++) list.add(Ingredient.of(MedicineRegistry.SOIL_ITEMS.get(tier - 1).get()));
        list.add(Ingredient.of(ModItems.getSpiritStone(SpiritStoneTier.values()[tier - 1])));
        return list;
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        if (!super.matches(container, level)) return false;
        if (soilTier == 0) return true;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (SpiritStoneItem.isSpiritStone(stack)) return SpiritStoneItem.isFullyCharged(stack);
        }
        return false;
    }

    public int soilTier() { return soilTier; }

    @Override
    public RecipeSerializer<?> getSerializer() { return ModRecipes.SPIRIT_SOIL_SERIALIZER.get(); }

    @Override
    public net.minecraft.world.item.crafting.RecipeType<?> getType() { return net.minecraft.world.item.crafting.RecipeType.CRAFTING; }

    public static final class Serializer implements RecipeSerializer<SpiritSoilRecipe> {
        @Override
        public SpiritSoilRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new SpiritSoilRecipe(id, GsonHelper.getAsInt(json, "tier"));
        }

        @Override
        public SpiritSoilRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new SpiritSoilRecipe(id, buffer.readVarInt());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, SpiritSoilRecipe recipe) {
            buffer.writeVarInt(recipe.soilTier);
        }
    }
}
