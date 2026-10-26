package com.longju.xiantu.trade;

import com.longju.xiantu.registry.ModEntities;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Structure integration entry point. A consumed marker remains consumed even after NPC death. */
@Mod.EventBusSubscriber(modid="xiantu")
public final class TownMerchantPlacement {
    private static final List<com.longju.xiantu.worldgen.town.TownLayout.Merchant> MARKERS=com.longju.xiantu.worldgen.town.TownLayout.MERCHANTS;
    private TownMerchantPlacement() {}
    private static String markerKey(int x,int z,String profession){return x+","+z+"/"+profession;}
    public static void markNatural(ServerLevel level,int x,int z,String profession){
        State state=level.getDataStorage().computeIfAbsent(State::load,State::new,"xiantu_town_merchants");
        if(state.markers.add(markerKey(x,z,profession)))state.setDirty();
    }
    public static int place(ServerLevel level,BlockPos origin) {
        State state=level.getDataStorage().computeIfAbsent(State::load,State::new,"xiantu_town_merchants");
        int placed=0;
        for(var marker:MARKERS) {
            BlockPos pos=origin.offset(marker.x(),marker.y(),marker.z());
            String key=markerKey(origin.getX(),origin.getZ(),marker.profession());
            if(state.markers.contains(key)||state.markers.contains(origin.asLong()+"/"+marker.profession())||!level.hasChunkAt(pos))continue;
            var npc=ModEntities.MERCHANTS.get(marker.profession()).get().create(level);
            if(npc==null)continue;
            npc.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,marker.yaw(),0);
            if(!level.noCollision(npc))continue;
            if(level.addFreshEntity(npc)){state.markers.add(key);state.setDirty();placed++;}
        }
        return placed;
    }
    @SubscribeEvent public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("xiantumerchants").requires(s->s.hasPermission(2))
            .then(Commands.argument("x",IntegerArgumentType.integer()).then(Commands.argument("y",IntegerArgumentType.integer())
            .then(Commands.argument("z",IntegerArgumentType.integer()).executes(c->{
                int n=place(c.getSource().getLevel(),new BlockPos(IntegerArgumentType.getInteger(c,"x"),IntegerArgumentType.getInteger(c,"y"),IntegerArgumentType.getInteger(c,"z")));
                c.getSource().sendSuccess(()->Component.literal("已接入商人："+n+"（已使用、未加载或被占用的站位跳过）"),false);return n;
            })))));
    }
    private static final class State extends SavedData {
        final Set<String> markers=new HashSet<>();
        static State load(CompoundTag tag){State state=new State();for(var value:tag.getList("markers",8))state.markers.add(value.getAsString());return state;}
        @Override public CompoundTag save(CompoundTag tag){var list=new net.minecraft.nbt.ListTag();markers.stream().sorted().forEach(s->list.add(net.minecraft.nbt.StringTag.valueOf(s)));tag.put("markers",list);return tag;}
    }
}
