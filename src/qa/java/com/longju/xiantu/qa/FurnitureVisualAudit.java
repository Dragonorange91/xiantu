package com.longju.xiantu.qa;

import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.nio.file.Files;
import java.util.*;

/** Reads actual baked sprites and every multipart orientation after resource loading. */
@Mod.EventBusSubscriber(modid="xiantu_qa", value=Dist.CLIENT)
public final class FurnitureVisualAudit {
    @SubscribeEvent public static void commands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("xiantufurnitureaudit").executes(c->audit()));
    }
    private static void require(boolean value,String message) {
        if(!value)throw new IllegalStateException(message);
    }
    static int audit() {
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null)return 0;
        List<Map<String,Object>> rows=new ArrayList<>();List<String> failures=new ArrayList<>();
        for(var entry:ForgeRegistries.BLOCKS.getEntries()) {
            var id=entry.getKey().location();var path=id.getPath();
            if(!id.getNamespace().equals("xiantu")||!(path.endsWith("_alchemy_furnace")||path.endsWith("_refining_furnace")||path.endsWith("_broken_pillar")||path.startsWith("ruin_")))continue;
            var block=entry.getValue();Map<String,Object> row=new LinkedHashMap<>();row.put("id",id.toString());
            try {
                int states=0,quads=0;
                List<Direction> sides=new ArrayList<>(Arrays.asList(Direction.values()));sides.add(null);
                for(var state:block.getStateDefinition().getPossibleStates()) {
                    var model=mc.getBlockRenderer().getBlockModel(state);
                    require(model!=mc.getModelManager().getMissingModel(),"missing block model "+state);
                    for(var side:sides)for(var quad:model.getQuads(state,side,RandomSource.create(42))) {
                        require(!quad.getSprite().contents().name().getPath().equals("missingno"),"missing block sprite "+state);quads++;
                    }
                    states++;
                }
                require(quads>0,"no block geometry");
                var stack=new ItemStack(block);var item=mc.getItemRenderer().getModel(stack,mc.level,mc.player,0);
                require(item!=mc.getModelManager().getMissingModel(),"missing item model");
                for(var context:List.of(ItemDisplayContext.GUI,ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)) {
                    int count=0;var model=item.applyTransform(context,new PoseStack(),false);
                    for(var side:sides)for(var quad:model.getQuads(null,side,RandomSource.create(42))) {
                        require(!quad.getSprite().contents().name().getPath().equals("missingno"),"missing item sprite "+context);count++;
                    }
                    require(count>0,"empty item "+context);
                }
                row.put("states",states);row.put("blockQuads",quads);row.put("passed",true);
            }catch(Exception ex){row.put("passed",false);row.put("error",ex.toString());failures.add(id+": "+ex);}
            rows.add(row);
        }
        Map<String,Object> result=new LinkedHashMap<>();result.put("models",rows);result.put("count",rows.size());result.put("failures",failures);
        try {Files.writeString(mc.gameDirectory.toPath().resolve("furniture-audit.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result));}
        catch(Exception ex){LogUtils.getLogger().error("Cannot save furniture QA",ex);return 0;}
        mc.player.displayClientMessage(Component.literal("Furniture resources: "+rows.size()+" models, "+failures.size()+" failures"),false);
        LogUtils.getLogger().info("[Furniture QA] {} models, {} failures",rows.size(),failures.size());
        return failures.isEmpty()&&rows.size()>=24?1:0;
    }
    private FurnitureVisualAudit(){}
}
