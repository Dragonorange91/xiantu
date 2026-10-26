package com.longju.xiantu.instance;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/** A damaged manifest stays blocked, rather than silently allocating over an unknown old world. */
public final class InstanceSavedData extends SavedData {
    private static final Logger LOGGER=LogUtils.getLogger();
    private InstanceLedger ledger=new InstanceLedger();
    private String rejected="";
    private boolean blocked;
    private CompoundTag runtime=new CompoundTag();
    public InstanceSavedData(){setDirty();}
    public static InstanceSavedData get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(InstanceSavedData::load,InstanceSavedData::new,"xiantu_instances");}
    private static InstanceSavedData load(CompoundTag n){
        var data=new InstanceSavedData();String json=n.getString("Manifest");
        try{data.ledger=InstanceLedger.load(json);data.runtime=n.getCompound("Runtime").copy();
            for(var s:data.ledger.slots())if(s.state()!=InstanceLedger.State.FREE){
                if(!data.runtime.contains(s.instance().toString(),10))throw new IllegalStateException("Missing instance session");
                var session=data.runtime.getCompound(s.instance().toString());
                if(s.kind()==InstanceLedger.Kind.CAVE&&s.state()==InstanceLedger.State.ACTIVE&&(!data.runtime.getString("Secret").matches("[0-9a-f]{64}")||session.getInt("MasterVersion")<1))throw new IllegalStateException("Invalid cave authority");
                if(s.kind()==InstanceLedger.Kind.REALM){InstanceRules.Abundance.valueOf(session.getString("Abundance"));if(session.getInt("Diameter")<16||session.getInt("Diameter")>192||session.getLong("EndAt")<=0)throw new IllegalStateException("Invalid realm parameters");}
            }
        }catch(RuntimeException failure){data.blocked=true;data.rejected=json;data.runtime=n.getCompound("Runtime").copy();LOGGER.error("仙途实例清单异常，禁止分配或覆盖任何槽位",failure);}
        return data;
    }
    public boolean blocked(){return blocked;}
    public java.util.List<InstanceLedger.Slot> slots(){return ledger.slots();}
    public InstanceLedger ledger(){if(blocked)throw new IllegalStateException("Instance manifest blocked");return ledger;}
    public CompoundTag runtime(){return runtime;}
    public CompoundTag session(InstanceLedger.Slot s){return runtime.getCompound(s.instance().toString());}
    public void session(InstanceLedger.Slot s,CompoundTag n){runtime.put(s.instance().toString(),n);setDirty();}
    String secret(){if(!runtime.contains("Secret")){byte[] b=new byte[32];new java.security.SecureRandom().nextBytes(b);runtime.putString("Secret",java.util.HexFormat.of().formatHex(b));setDirty();}return runtime.getString("Secret");}
    @Override public CompoundTag save(CompoundTag n){n.putString("Manifest",blocked?rejected:ledger.save());n.put("Runtime",runtime.copy());return n;}
}
