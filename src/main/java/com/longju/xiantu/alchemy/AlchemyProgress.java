package com.longju.xiantu.alchemy;
import com.longju.xiantu.refining.RefiningRules;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid="xiantu")
public final class AlchemyProgress {
    public static final String KEY="xiantu_alchemy";
    public static CompoundTag data(Player p){return p.getPersistentData().getCompound(KEY);}
    public static int xp(Player p){return Math.max(0,data(p).getInt("Experience"));}
    public static int level(Player p){return RefiningRules.level(xp(p));}
    public static void award(Player p,int rank){var n=data(p);n.putInt("Experience",Math.min(36100,xp(p)+RefiningRules.award(level(p),rank)));p.getPersistentData().put(KEY,n);}
    @SubscribeEvent public static void clone(PlayerEvent.Clone e){if(e.getOriginal().getPersistentData().contains(KEY))e.getEntity().getPersistentData().put(KEY,data(e.getOriginal()).copy());}
    private AlchemyProgress(){}
}
