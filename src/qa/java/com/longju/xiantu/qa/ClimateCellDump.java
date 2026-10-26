package com.longju.xiantu.qa;
import java.util.*;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class ClimateCellDump {
    @SubscribeEvent public static void commands(RegisterCommandsEvent e){
        e.getDispatcher().register(Commands.literal("xiantuclimatecells").requires(s->s.hasPermission(2)).executes(c->{
            try {
                var cells=new ArrayList<Map<String,Object>>();
                for(var pair:new terrablender.api.ModifiedVanillaOverworldBuilder().build()){
                    var p=pair.getFirst();var id=pair.getSecond().location().getPath();
                    if(p.depth().min()!=0||p.depth().max()!=0||!Set.of("plains","sunflower_plains","savanna","forest","birch_forest").contains(id))continue;
                    cells.add(Map.of("biome",id,"temperature",p.temperature(),"humidity",p.humidity(),
                            "continentalness",p.continentalness(),"erosion",p.erosion(),"weirdness",p.weirdness()));
                }
                java.nio.file.Files.writeString(FMLPaths.GAMEDIR.get().resolve("xiantu-climate-cells.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(cells));
                c.getSource().sendSuccess(()->Component.literal("Exported "+cells.size()+" actual vanilla surface cells"),false);return cells.size();
            }catch(Exception ex){c.getSource().sendFailure(Component.literal(ex.toString()));return 0;}
        }));
    }
}
