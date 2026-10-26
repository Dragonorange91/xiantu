package com.longju.xiantu.qa;

import com.google.gson.*;
import com.longju.xiantu.mining.*;
import com.longju.xiantu.refining.*;
import com.longju.xiantu.registry.ModItems;
import com.longju.xiantu.terrain.NaturalResourceOreBlock;
import com.longju.xiantu.worldgen.*;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class RefiningMaterialsAudit {
    private static int checks;
    private static void check(boolean c,String text){checks++;if(!c)throw new IllegalStateException(text);}
    @SubscribeEvent public static void register(RegisterCommandsEvent e){e.getDispatcher().register(Commands.literal("xiantumaterialaudit").requires(s->s.hasPermission(2)).executes(ctx->{
        var source=ctx.getSource();var player=source.getPlayerOrException();var level=player.serverLevel();
        var world=player.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).normalize().getFileName().toString();
        org.slf4j.LoggerFactory.getLogger(RefiningMaterialsAudit.class).info("Material audit entered in {}",world);
        if(!world.startsWith("Xiantu-QA")){source.sendFailure(Component.literal("材料测试仅允许 Xiantu-QA 存档："+world));return 0;}
        checks=0;var report=new JsonObject();
        try{
            for(var mineral:AdvancedMinerals.NATURAL){
                var block=AdvancedMinerals.ORES.get(mineral.id()).get();var natural=block.defaultBlockState();if(natural.hasProperty(NaturalResourceOreBlock.NATURAL_NODE))natural=natural.setValue(NaturalResourceOreBlock.NATURAL_NODE,true);
                var tool=new ItemStack(ModItems.HUNDUN_PICKAXE.get());tool.enchant(Enchantments.BLOCK_FORTUNE,3);
                check(tool.isCorrectToolForDrops(natural),mineral.id()+" high tool");
                check(!new ItemStack(Items.NETHERITE_PICKAXE).isCorrectToolForDrops(natural),mineral.id()+" vanilla insufficient");
                check(new ItemStack(ModItems.HANXING_PICKAXE.get()).isCorrectToolForDrops(natural)==mineral.id().equals("reincarnation_crystal"),mineral.id()+" tier threshold");
                for(var pick:List.of(tool,new ItemStack(ModItems.JIUYOU_PICKAXE.get()))){
                    var drops=Block.getDrops(natural,level,player.blockPosition(),null,player,pick);
                    check(drops.size()==1&&drops.get(0).getCount()==1&&drops.get(0).is(AdvancedMinerals.MATERIALS.get(mineral.id()).get()),mineral.id()+" fixed drop");
                }
                tool.enchant(Enchantments.SILK_TOUCH,1);
                check(Block.getDrops(natural,level,player.blockPosition(),null,player,tool).get(0).is(AdvancedMinerals.MATERIALS.get(mineral.id()).get()),"silk does not duplicate ore");
                if(mineral.id().equals("causality_stone")){check(!natural.hasBlockEntity(),"causality is ordinary ore, no node entity");check(Block.getDrops(block.defaultBlockState(),level,player.blockPosition(),null,player,tool).get(0).is(AdvancedMinerals.MATERIALS.get("causality_stone").get()),"ordinary placed causality ore also drops stone");}
                else check(Block.getDrops(block.defaultBlockState(),level,player.blockPosition(),null,player,tool).isEmpty(),"other placed special ore has no resource");
            }
            var state=AdvancedMinerals.ORES.get("causality_stone").get().defaultBlockState();
            check(!new ItemStack(ModItems.JIUYOU_PICKAXE.get()).isCorrectToolForDrops(state),"causality harvest tier");
            check(new ItemStack(ModItems.HUNDUN_PICKAXE.get()).isCorrectToolForDrops(state),"causality correct tool");
            for(var recipe:RefiningRecipes.ALL){
                check(ForgeRegistries.ITEMS.containsKey(new ResourceLocation(recipe.output())),recipe.id()+" output registered");
                var inputs=new ArrayList<ItemStack>();for(var input:recipe.inputs()){
                    check(ForgeRegistries.ITEMS.containsKey(new ResourceLocation(input.item())),recipe.id()+" ingredient registered");
                    var stack=input.example();check(input.matches(stack),recipe.id()+" specimen matches "+input.label());inputs.add(stack);
                }
                var before=inputs.stream().map(ItemStack::copy).toList();check(RefiningRecipes.allocate(recipe,inputs)!=null,recipe.id()+" full material allocation");
                for(int i=0;i<inputs.size();i++)check(ItemStack.matches(before.get(i),inputs.get(i)),"preflight cannot consume");
                inputs.get(0).shrink(1);check(RefiningRecipes.allocate(recipe,inputs)==null,recipe.id()+" insufficient rejected");
                check(RefiningRules.blueprintRequired(recipe.tier())==(recipe.tier()>=4),"four tier blueprint threshold");
            }
            for(int l=0;l<=9;l++)for(int f=1;f<=6;f++)for(int q=0;q<=5;q++){
                var weights=RefiningRules.qualityWeights(l,f,q,q);check(Arrays.stream(weights).sum()==10000&&Arrays.stream(weights).allMatch(n->n>=0),"quality weights normalized");
            }
            report.addProperty("status","PASS");report.addProperty("recipes",RefiningRecipes.ALL.size());source.sendSuccess(()->Component.literal("炼器材料审计通过："+checks+" 项"),false);
        }catch(Throwable failure){report.addProperty("status","FAIL");report.addProperty("error",failure.toString());org.slf4j.LoggerFactory.getLogger(RefiningMaterialsAudit.class).error("Material audit failed",failure);source.sendFailure(Component.literal(failure.toString()));}
        report.addProperty("checks",checks);
        try{var path=player.server.getServerDirectory().toPath().resolve("xiantu-qa/refining-materials.json");java.nio.file.Files.createDirectories(path.getParent());java.nio.file.Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(report));}catch(Exception failure){throw new RuntimeException(failure);}
        return report.get("status").getAsString().equals("PASS")?1:0;
    }));}
}
