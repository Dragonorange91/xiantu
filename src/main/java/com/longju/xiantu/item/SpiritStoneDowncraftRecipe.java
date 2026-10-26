package com.longju.xiantu.item;

import com.google.gson.JsonObject;
import com.longju.xiantu.registry.ModItems;
import com.longju.xiantu.registry.ModRecipes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.level.Level;

public final class SpiritStoneDowncraftRecipe extends ShapelessRecipe {
    private final SpiritStoneTier inputTier;

    public SpiritStoneDowncraftRecipe(ResourceLocation id, SpiritStoneTier inputTier) {
        super(id, "", CraftingBookCategory.MISC,
                new ItemStack(ModItems.getSpiritStone(lowerTier(inputTier)), inputTier.downcraftCount()),
                NonNullList.of(Ingredient.EMPTY,
                        Ingredient.of(ModItems.getSpiritStone(inputTier))));
        this.inputTier = inputTier;
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        if (!super.matches(container, level)) return false;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty()) return SpiritStoneItem.isFullyCharged(stack);
        }
        return false;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.SPIRIT_STONE_DOWNCRAFT_SERIALIZER.get();
    }

    @Override
    public net.minecraft.world.item.crafting.RecipeType<?> getType() {
        return net.minecraft.world.item.crafting.RecipeType.CRAFTING;
    }

    public SpiritStoneTier inputTier() {
        return inputTier;
    }

    private static SpiritStoneTier lowerTier(SpiritStoneTier tier) {
        if (tier.rank() <= 1) throw new IllegalArgumentException("First-tier spirit stone cannot be downcrafted");
        return SpiritStoneTier.values()[tier.rank() - 2];
    }

    public static final class Serializer implements RecipeSerializer<SpiritStoneDowncraftRecipe> {
        @Override
        public SpiritStoneDowncraftRecipe fromJson(ResourceLocation id, JsonObject json) {
            SpiritStoneTier tier = byName(GsonHelper.getAsString(json, "input"));
            return new SpiritStoneDowncraftRecipe(id, tier);
        }

        @Override
        public SpiritStoneDowncraftRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new SpiritStoneDowncraftRecipe(id, byName(buffer.readUtf()));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, SpiritStoneDowncraftRecipe recipe) {
            buffer.writeUtf(recipe.inputTier.registryName());
        }

        private static SpiritStoneTier byName(String name) {
            for (SpiritStoneTier tier : SpiritStoneTier.values()) {
                if (tier.registryName().equals(name)) return tier;
            }
            throw new IllegalArgumentException("Unknown spirit-stone tier: " + name);
        }
    }
}
