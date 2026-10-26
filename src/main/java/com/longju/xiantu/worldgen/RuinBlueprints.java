package com.longju.xiantu.worldgen;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import net.minecraftforge.registries.*;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid="xiantu")
public final class RuinBlueprints {
    public static final DeferredRegister<StructureProcessorType<?>> TYPES=DeferredRegister.create(Registries.STRUCTURE_PROCESSOR,"xiantu");
    public static final RegistryObject<StructureProcessorType<RuinHerbProcessor>> HERBS=TYPES.register("ruin_herbs",()->()->RuinHerbProcessor.CODEC);
    @SubscribeEvent public static void commands(RegisterCommandsEvent e){
        e.getDispatcher().register(Commands.literal("xianturuin").requires(s->s.hasPermission(2))
            .then(Commands.argument("blueprint",StringArgumentType.word())
            .then(Commands.argument("position",BlockPosArgument.blockPos()).executes(c->{
                String name=StringArgumentType.getString(c,"blueprint");
                if(!name.matches("[a-z0-9_]+"))return 0;
                var source=c.getSource();var level=source.getLevel();
                var id=new ResourceLocation("xiantu","ruins/"+name);
                var template=level.getStructureManager().get(id);
                if(template.isEmpty()){source.sendFailure(Component.literal("没有找到遗迹蓝图："+name));return 0;}
                var pos=BlockPosArgument.getLoadedBlockPos(c,"position");var size=template.get().getSize();
                if(pos.getY()<level.getMinBuildHeight() || pos.getY()+size.getY()>level.getMaxBuildHeight()){
                    source.sendFailure(Component.literal("蓝图超出世界高度"));return 0;
                }
                String instance=RuinEncounterService.key(level,name,pos);
                if(RuinInstanceData.get(level).placed(instance)){source.sendFailure(Component.literal("此位置的遗迹实例已放置，不能重放刷新奖励或守护者"));return 0;}
                com.google.gson.JsonObject spec;
                try(var reader=level.getServer().getResourceManager().getResource(new ResourceLocation("xiantu","ruin_blueprints/"+name+".json")).orElseThrow().openAsReader()){
                    spec=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
                }catch(java.io.IOException ex){throw new java.io.UncheckedIOException(ex);}
                try{RuinEncounterService.validate(spec);}catch(IllegalArgumentException ex){source.sendFailure(Component.literal("遗迹配置无效，未放置："+ex.getMessage()));return 0;}
                boolean ok=RuinPlacementQueue.enqueue(source,pos,spec,template.get(),instance);
                if(ok)source.sendSuccess(()->Component.literal("已开始分批放置遗迹，完成后会通知"),false);
                return ok?1:0;
            }))));
    }
}
