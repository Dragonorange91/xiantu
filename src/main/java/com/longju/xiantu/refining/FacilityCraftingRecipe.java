package com.longju.xiantu.refining;

import com.google.gson.JsonObject;
import com.longju.xiantu.registry.ModRecipes;
import com.longju.xiantu.wood.SpiritWoodProcessingRecipe;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Starter facilities use a workbench. Recipe-book previews do not grant learning. */
public final class FacilityCraftingRecipe extends ShapelessRecipe {
    public final String facility;
    public FacilityCraftingRecipe(ShapelessRecipe base,String facility){
        super(base.getId(),base.getGroup(),base.category(),base.getResultItem(RegistryAccess.EMPTY),base.getIngredients());
        this.facility=facility;
        String expected=switch(facility){case "refining_station_1"->"xiantu:lifeng_refining_furnace";case "smelting_furnace"->"xiantu:smelting_furnace";case "forging_table"->"xiantu:artisan_forging_table";case "talisman_table"->"xiantu:talisman_writing_table";case "jiuyou_pickaxe","hundun_pickaxe"->"xiantu:"+facility;default->throw new IllegalArgumentException("Invalid starter facility");};
        if(!net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(base.getResultItem(RegistryAccess.EMPTY).getItem()).toString().equals(expected))throw new IllegalArgumentException("Mismatched facility output");
    }
    @Override public boolean matches(CraftingContainer grid,Level level){
        if(!super.matches(grid,level))return false;
        if(facility.equals("talisman_table"))return true;
        var p=SpiritWoodProcessingRecipe.owner(grid,level);
        if(p==null||!RefiningProgress.knows(p,"facility/"+facility))return false;
        int realm=facility.equals("jiuyou_pickaxe")?3:facility.equals("hundun_pickaxe")?6:0;
        return realm==0||p.getCapability(com.longju.xiantu.cultivation.CultivationCapability.DATA).map(d->d.cultivationUnlocked()&&d.majorRealm()>=realm).orElse(false);
    }
    @Override public RecipeSerializer<?> getSerializer(){return ModRecipes.FACILITY_CRAFTING.get();}
    public static final class Serializer implements RecipeSerializer<FacilityCraftingRecipe>{
        private final ShapelessRecipe.Serializer base=new ShapelessRecipe.Serializer();
        @Override public FacilityCraftingRecipe fromJson(ResourceLocation id,JsonObject json){return new FacilityCraftingRecipe(base.fromJson(id,json),json.get("facility").getAsString());}
        @Override public FacilityCraftingRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf b){String key=b.readUtf(64);return new FacilityCraftingRecipe(base.fromNetwork(id,b),key);}
        @Override public void toNetwork(FriendlyByteBuf b,FacilityCraftingRecipe r){b.writeUtf(r.facility,64);base.toNetwork(b,r);}
    }
}
