package com.longju.xiantu.wood;
import com.google.gson.JsonObject;
import com.longju.xiantu.material.MaterialQuality;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Vanilla workbench pattern, with server owner/realm checks and deterministic materialQuality inheritance. */
public final class SpiritWoodProcessingRecipe extends ShapedRecipe {
    public final SpiritWoodType wood;
    public SpiritWoodProcessingRecipe(ShapedRecipe base,SpiritWoodType t){
        super(base.getId(),base.getGroup(),base.category(),base.getWidth(),base.getHeight(),base.getIngredients(),base.getResultItem(RegistryAccess.EMPTY));wood=t;
    }
    public static Player owner(CraftingContainer grid,Level level){
        for(Player player:level.players())for(var slot:player.containerMenu.slots)if(slot.container==grid)return player;
        return null;
    }
    @Override public boolean matches(CraftingContainer grid,Level level){
        if(!super.matches(grid,level))return false;
        Player p=owner(grid,level);if(p==null||!SpiritWoodRules.realm(p,wood))return false;
        return true;
    }
    @Override public ItemStack assemble(CraftingContainer grid,RegistryAccess registry){
        ItemStack output=super.assemble(grid,registry);
        return MaterialQuality.eligible(output)?MaterialQuality.set(output,MaterialQuality.inherit(grid)):output;
    }
    @Override public RecipeSerializer<?> getSerializer(){return SpiritWoodRegistry.RECIPE.get();}
    public static final class Serializer implements RecipeSerializer<SpiritWoodProcessingRecipe>{
        private final ShapedRecipe.Serializer base=new ShapedRecipe.Serializer();
        @Override public SpiritWoodProcessingRecipe fromJson(ResourceLocation id,JsonObject json){return new SpiritWoodProcessingRecipe(base.fromJson(id,json),SpiritWoodType.valueOf(json.get("wood").getAsString().toUpperCase(java.util.Locale.ROOT)));}
        @Override public SpiritWoodProcessingRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf buf){SpiritWoodType type=buf.readEnum(SpiritWoodType.class);return new SpiritWoodProcessingRecipe(base.fromNetwork(id,buf),type);}
        @Override public void toNetwork(FriendlyByteBuf buf,SpiritWoodProcessingRecipe recipe){buf.writeEnum(recipe.wood);base.toNetwork(buf,recipe);}
    }
}
