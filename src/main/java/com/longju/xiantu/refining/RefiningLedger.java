package com.longju.xiantu.refining;

import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.item.ItemStack;
import java.util.UUID;

/** Portable blanks are authenticated once across dimensions, and cannot be forged twice. */
public final class RefiningLedger extends SavedData {
    private CompoundTag jobs=new CompoundTag();
    public static RefiningLedger get(ServerLevel l){return l.getServer().overworld().getDataStorage().computeIfAbsent(RefiningLedger::load,RefiningLedger::new,"xiantu_refining_blanks");}
    public static RefiningLedger load(CompoundTag n){var d=new RefiningLedger();d.jobs=n.getCompound("Jobs").copy();return d;}
    @Override public CompoundTag save(CompoundTag n){n.put("Jobs",jobs.copy());return n;}
    public ItemStack issue(String recipe,int material,int furnace,int furnaceQuality){return issue(recipe,material,furnace,furnaceQuality,0);}
    public ItemStack issue(String recipe,int material,int furnace,int furnaceQuality,int fireBonus){
        var s=new ItemStack(RefiningItems.BLANK.get());String id=UUID.randomUUID().toString();var data=new CompoundTag();
        data.putInt("FireBonus",Math.max(0,Math.min(900,fireBonus)));data.putString("Recipe",recipe);data.putInt("Material",material);data.putInt("Furnace",furnace);data.putInt("FurnaceQuality",furnaceQuality);
        jobs.put(id,data.copy());s.setTag(data);s.getOrCreateTag().putString("Serial",id);setDirty();return s;
    }
    public CompoundTag inspect(ItemStack s){
        if(!s.is(RefiningItems.BLANK.get())||!s.hasTag())return null;String id=s.getTag().getString("Serial");
        if(!jobs.contains(id,Tag.TAG_COMPOUND))return null;var n=jobs.getCompound(id);
        return n.getInt("State")==0?n.copy():null;
    }
    public boolean reserve(ItemStack s){if(inspect(s)==null)return false;jobs.getCompound(s.getTag().getString("Serial")).putInt("State",1);setDirty();return true;}
    public boolean finish(String id){if(!jobs.contains(id)||jobs.getCompound(id).getInt("State")!=1)return false;jobs.getCompound(id).putInt("State",2);setDirty();return true;}
}
