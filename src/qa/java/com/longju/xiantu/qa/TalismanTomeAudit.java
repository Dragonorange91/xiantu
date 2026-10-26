package com.longju.xiantu.qa;

import com.longju.xiantu.talisman.*;
import com.longju.xiantu.trade.TownTradeService;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Live registry checks use a detached fake player; no player inventory or world is modified. */
@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class TalismanTomeAudit {
    private static int checks;
    private static void check(boolean value,String text){if(!value)throw new IllegalStateException(text);checks++;}
    @SubscribeEvent public static void commands(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("xiantutalismanbookaudit").requires(s->s.hasPermission(2)).executes(c->{
            checks=0;
            try{
            var fake=new net.minecraftforge.common.util.FakePlayer(c.getSource().getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"TalismanTomeQA"));
            for(var tome:TalismanTomes.ALL){
                var offers=TownTradeService.OFFERS.stream().filter(o->o.shop().equals("talisman_merchant")&&o.kind().equals("talisman_book")&&o.key().equals(tome.id())).toList();
                check(offers.size()==1,"one merchant offer per tome");
                var book=offers.get(0).stack();
                check(book.getHoverName().getString().equals(tome.name()),"exact localized title");
                var before=fake.getPersistentData().copy();
                try{
                    check(TalismanBlueprints.learn(fake,book).isEmpty()&&book.isEmpty(),"learning consumes one book");
                    var duplicate=TalismanItems.blueprint(tome.id());
                    check(!TalismanBlueprints.learn(fake,duplicate).isEmpty()&&duplicate.getCount()==1,"duplicate learning consumes nothing");
                    for(var recipe:TalismanRecipes.ALL)
                        check(TalismanProgress.knows(fake,recipe.knowledge())==tome.covers(recipe.tier()),"learning grants only the correct tier band");
                }finally{fake.getPersistentData().getAllKeys().clear();fake.getPersistentData().merge(before);}
            }
            check(TownTradeService.OFFERS.stream().filter(o->o.kind().equals("talisman_book")).count()==3,"only three talisman books sold");
            var papers=TalismanRecipes.ALL.stream().filter(r->r.kind().equals("paper")).toList();
            check(papers.size()==8,"exactly eight paper recipes");
            for(var paper:papers){
                String wood=com.longju.xiantu.wood.SpiritWoodType.values()[paper.tier()-1].id;
                check(paper.ingredients().get(0).id().equals("xiantu:"+wood+"_fiber"),"same-tier spirit wood makes paper");
                var output=paper.output(3);
                check(TalismanItems.tier(output)==paper.tier()&&com.longju.xiantu.material.MaterialQuality.get(output)==3,"paper tier and quality preserved");
            }
            var bench=c.getSource().getLevel().getRecipeManager().byKey(new net.minecraft.resources.ResourceLocation("xiantu:talisman_writing_table")).orElseThrow();
            var menu=new net.minecraft.world.inventory.CraftingMenu(0,fake.getInventory());
            var grid=new net.minecraft.world.inventory.TransientCraftingContainer(menu,3,3);
            for(int i=0;i<bench.getIngredients().size();i++)((net.minecraft.world.Container)grid).setItem(i,bench.getIngredients().get(i).getItems()[0].copy());
            check(((net.minecraft.world.item.crafting.Recipe<net.minecraft.world.inventory.CraftingContainer>)bench).matches(grid,c.getSource().getLevel()),"table crafts without blueprint or tome");
            ((net.minecraft.world.Container)grid).setItem(0,net.minecraft.world.item.ItemStack.EMPTY);
            check(!((net.minecraft.world.item.crafting.Recipe<net.minecraft.world.inventory.CraftingContainer>)bench).matches(grid,c.getSource().getLevel()),"table still requires all materials");
            for(int ceiling=1;ceiling<=8;ceiling++)for(boolean rebirth:new boolean[]{false,true}){
                check(!TalismanSources.pool(ceiling,rebirth).isEmpty(),"every tier has finished rewards");
                for(var recipe:TalismanSources.pool(ceiling,rebirth)){
                    var stack=recipe.output(1);
                    check(!stack.isEmpty()&&!stack.is(TalismanItems.BLUEPRINT.get()),"loot is finished talisman");
                    check(TalismanItems.tier(stack)<=ceiling,"loot respects tier ceiling");
                    check(rebirth||!recipe.id().equals("rebirth"),"rebirth eligibility retained");
                }
                var random=new java.util.Random(37);
                var reward=TalismanSources.roll(ceiling,rebirth,random,1);
                check(!reward.isEmpty()&&!reward.is(TalismanItems.BLUEPRINT.get()),"guaranteed roll gives a finished talisman");
                check(TalismanSources.roll(ceiling,rebirth,random,0).isEmpty(),"zero chance gives nothing");
            }
            check(TalismanSources.pool(0,true).isEmpty(),"no below-tier rewards");
            check(TalismanRecipes.ALL.stream().noneMatch(r->r.kind().equals("blueprint")),"no individual-symbol book crafting");
            final int passed=checks;
            c.getSource().sendSuccess(()->Component.literal("Talisman tome audit PASS: "+passed),false);
            com.mojang.logging.LogUtils.getLogger().info("[Talisman tome audit PASS] checks={}",passed);
            return passed;
            }catch(Exception|LinkageError ex){
                com.mojang.logging.LogUtils.getLogger().error("[Talisman tome audit FAIL] checks="+checks,ex);
                c.getSource().sendFailure(Component.literal("Talisman tome audit FAIL at "+checks+": "+ex));return 0;
            }
        }));
    }
}
