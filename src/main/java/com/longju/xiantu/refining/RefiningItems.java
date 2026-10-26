package com.longju.xiantu.refining;

import com.longju.xiantu.registry.ModItems;
import net.minecraft.world.item.*;
import net.minecraft.world.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraftforge.registries.RegistryObject;
import java.util.List;

public final class RefiningItems {
    public static final RegistryObject<Item> BLANK=ModItems.ITEMS.register("equipment_blank",()->new Item(new Item.Properties().stacksTo(1)){
        @Override public Component getName(ItemStack s){var r=recipe(s);return r==null?super.getName(s):Component.literal(r.name()+"·"+r.tier()+"阶器胚");}
        @Override public void appendHoverText(ItemStack s,Level l,List<Component> text,TooltipFlag f){text.add(Component.literal("在锻打台完成成型；材料品质与炉具加成已保留"));}
    });
    public static final RegistryObject<Item> SCRAP=ModItems.ITEMS.register("refining_scrap",()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> BLUEPRINT=ModItems.ITEMS.register("equipment_blueprint",()->new Item(new Item.Properties()){
        @Override public Component getName(ItemStack s){String label=diagramName(s);return label.isEmpty()?super.getName(s):Component.literal(label);}
        @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand hand){
            var s=p.getItemInHand(hand);String key=knowledge(s);if(key.isEmpty())return InteractionResultHolder.fail(s);
            if(p instanceof net.minecraft.server.level.ServerPlayer server)BlueprintLearningMenu.open(server,hand);
            return InteractionResultHolder.sidedSuccess(s,l.isClientSide);
        }
    });
    public static String knowledge(ItemStack s){
        String talisman=com.longju.xiantu.talisman.TalismanBlueprints.key(s);if(!talisman.isEmpty())return "talisman/"+talisman;
        String treatise=CraftingTreatises.knowledge(s);if(!treatise.isEmpty())return treatise;
        if(!s.is(BLUEPRINT.get())||!s.hasTag()||s.getTag().contains("BlueprintVersion")&&s.getTag().getInt("BlueprintVersion")!=1)return "";
        int types=(s.getTag().contains("Facility")?1:0)+(s.getTag().contains("Alchemy")?1:0)+(s.getTag().contains("Recipe")?1:0);
        if(types!=1)return "";
        if(s.getTag().contains("Facility")){String id=s.getTag().getString("Facility");return FacilityRecipes.label(id).isEmpty()?"":"facility/"+id;}
        if(s.getTag().contains("Alchemy")){var r=com.longju.xiantu.alchemy.AlchemyRecipes.find(s.getTag().getString("Alchemy"));return r==null||r.rank()<=3?"":r.knowledge();}
        var r=RefiningRecipes.diagram(s.getTag().getString("Recipe"));return r==null?"":r.id();
    }
    public static String diagramName(ItemStack s){
        String talisman=com.longju.xiantu.talisman.TalismanBlueprints.key(s);if(!talisman.isEmpty())return com.longju.xiantu.talisman.TalismanBlueprints.name(talisman);
        String treatise=CraftingTreatises.knowledge(s);if(!treatise.isEmpty())return treatise.equals(CraftingTreatises.ALCHEMY_KEY)?"丹道真解":"器道真解";
        if(!s.hasTag())return "";
        if(s.getTag().contains("Facility"))return FacilityRecipes.label(s.getTag().getString("Facility"));
        if(s.getTag().contains("Alchemy")){var r=com.longju.xiantu.alchemy.AlchemyRecipes.find(s.getTag().getString("Alchemy"));return r==null||r.rank()<=3?"":r.name()+"丹方";}
        var r=RefiningRecipes.diagram(s.getTag().getString("Recipe"));return r==null?"":r.name()+"器方";
    }
    public static ItemStack facilityDiagram(String id){
        if(FacilityRecipes.label(id).isEmpty())throw new IllegalArgumentException("Unknown facility diagram");
        var s=new ItemStack(BLUEPRINT.get());s.getOrCreateTag().putString("Facility",id);s.getOrCreateTag().putInt("BlueprintVersion",1);return s;
    }
    public static ItemStack alchemyDiagram(String id){
        var recipe=com.longju.xiantu.alchemy.AlchemyRecipes.find(id);if(recipe==null||recipe.rank()<=3)throw new IllegalArgumentException("No individual low-tier pill diagrams");
        var s=new ItemStack(BLUEPRINT.get());s.getOrCreateTag().putString("Alchemy",id);s.getOrCreateTag().putInt("BlueprintVersion",1);return s;
    }
    public static RefiningRecipes.Recipe recipe(ItemStack s){return s.hasTag()?RefiningRecipes.find(s.getTag().getString("Recipe")):null;}
    public static void bootstrap(){}
    private RefiningItems(){}
}
