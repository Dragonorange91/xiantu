package com.longju.xiantu.talisman;

import com.longju.xiantu.entity.BeastRealmService;
import com.longju.xiantu.worldgen.RuinAdventureData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Invoked inside the existing one-time chest transaction; elite deaths have a separate once flag. */
@Mod.EventBusSubscriber(modid="xiantu")
public final class TalismanSources {
    public static List<TalismanRecipes.Recipe> pool(int ceiling,boolean rebirth){
        return TalismanRecipes.ALL.stream().filter(r->r.kind().equals("symbol")
            &&r.tier()<=Math.min(8,ceiling)&&(!r.id().equals("rebirth")||rebirth)).toList();
    }
    public static ItemStack roll(int ceiling,boolean rebirth,Random random,double chance){
        var pool=pool(ceiling,rebirth);if(pool.isEmpty()||random.nextDouble()>=chance)return ItemStack.EMPTY;
        return pool.get(random.nextInt(pool.size())).output(1);
    }
    public static ItemStack chest(RuinAdventureData.Instance i,String chest,String grade){
        String loot="";
        for(var value:i.spec.getAsJsonArray("markers")){var m=value.getAsJsonObject();if(m.has("id")&&m.get("id").getAsString().equals(chest)){loot=m.has("loot")?m.get("loot").getAsString():"";break;}}
        var matcher=java.util.regex.Pattern.compile("tier_(\\d)").matcher(loot);int ceiling=matcher.find()?Integer.parseInt(matcher.group(1)):1;
        if(i.blueprint.equals("water_curtain_caverns_grand"))ceiling=6;
        return roll(ceiling,ceiling>=3&&ceiling<=5,new Random(Objects.hash(i.id,chest,"talisman_v1")),grade.equals("core")?.18:.08);
    }
    @SubscribeEvent public static void elite(LivingDeathEvent event){
        if(event.isCanceled()||!(event.getSource().getEntity() instanceof ServerPlayer p)||p.isCreative()||p.isSpectator()||p instanceof net.minecraftforge.common.util.FakePlayer)return;
        var target=event.getEntity();var n=target.getPersistentData();
        if(!BeastRealmService.isBeast(target)||!BeastRealmService.valid(target)||!BeastRealmService.elite(target)||n.getBoolean("xiantu_talisman_reward_rolled"))return;
        n.putBoolean("xiantu_talisman_reward_rolled",true);
        var reward=roll(Math.min(8,BeastRealmService.realm(target)+1),true,new Random(target.getUUID().getMostSignificantBits()^target.getUUID().getLeastSignificantBits()),.025);
        if(!reward.isEmpty())target.spawnAtLocation(reward);
    }
    private TalismanSources(){}
}
