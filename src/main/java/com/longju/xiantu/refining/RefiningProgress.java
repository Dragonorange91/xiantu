package com.longju.xiantu.refining;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
/** Profession and learned diagrams survive ordinary deaths and cultivation resets. */
@Mod.EventBusSubscriber(modid="xiantu")
public final class RefiningProgress {
    public static final String KEY="xiantu_refining";
    public static CompoundTag data(Player p){return p.getPersistentData().getCompound(KEY);}
    public static int xp(Player p){return Math.max(0,data(p).getInt("Experience"));}
    public static int level(Player p){return RefiningRules.level(xp(p));}
    public static String blueprintKey(String id){
        return id.replaceFirst("_tier_[1-7]$", "");
    }
    public static boolean knows(Player p,String id){return data(p).getCompound("Blueprints").getBoolean(blueprintKey(id));}
    public static void learn(Player p,String id){var n=data(p);var b=n.getCompound("Blueprints");b.putBoolean(blueprintKey(id),true);n.put("Blueprints",b);p.getPersistentData().put(KEY,n);}
    public static void award(Player p,int tier){var n=data(p);n.putInt("Experience",Math.min(36100,xp(p)+RefiningRules.award(level(p),tier)));p.getPersistentData().put(KEY,n);}
    @SubscribeEvent public static void clone(PlayerEvent.Clone e){if(e.getOriginal().getPersistentData().contains(KEY))e.getEntity().getPersistentData().put(KEY,data(e.getOriginal()).copy());}
    private RefiningProgress(){}
}
