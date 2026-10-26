package com.longju.xiantu.instance;

import com.google.gson.*;
import java.util.*;

/** Allocation metadata only. No level/file deletion or gameplay entry until generation is integrated. */
public final class InstanceLedger {
    public enum Kind {REALM,CAVE}
    public enum State {FREE,PREPARING,ACTIVE,CLOSING,QUARANTINED,REPAIR_REQUIRED}
    public record Slot(Kind kind,int index,long generation,UUID instance,UUID owner,State state){
        public String dimension(){return "xiantu:"+(kind==Kind.REALM?String.format(Locale.ROOT,"realm_%02d",index):String.format(Locale.ROOT,"dongfu_%03d",index));}
    }
    private final Map<String,Slot> slots=new LinkedHashMap<>();
    private UUID activeRealm;
    public InstanceLedger(){
        for(var kind:Kind.values())for(int i=0;i<(kind==Kind.REALM?InstanceRules.REALM_SLOTS:InstanceRules.CAVE_SLOTS);i++){
            var s=new Slot(kind,i,0,null,null,State.FREE);slots.put(s.dimension(),s);
        }
    }
    public synchronized List<Slot> slots(){return List.copyOf(slots.values());}
    public synchronized UUID activeRealm(){return activeRealm;}
    public synchronized Optional<Slot> cave(UUID owner){return slots.values().stream().filter(s->s.kind==Kind.CAVE&&owner.equals(s.owner)).findFirst();}
    public synchronized Optional<Slot> reserve(Kind kind,UUID owner){
        if(kind==Kind.REALM&&activeRealm!=null||kind==Kind.CAVE&&(owner==null||cave(owner).isPresent()))return Optional.empty();
        for(var s:slots.values())if(s.kind==kind&&s.state==State.FREE){
            var next=new Slot(kind,s.index,Math.addExact(s.generation,1),UUID.randomUUID(),kind==Kind.CAVE?owner:null,State.PREPARING);
            slots.put(next.dimension(),next);if(kind==Kind.REALM)activeRealm=next.instance;return Optional.of(next);
        }return Optional.empty();
    }
    public synchronized boolean activate(Slot request){return transition(request,State.PREPARING,State.ACTIVE);}
    public synchronized boolean beginClose(Slot request){return request.kind==Kind.REALM&&transition(request,State.ACTIVE,State.CLOSING);}
    public synchronized boolean quarantine(Slot request){
        var current=current(request);if(current==null||current.kind!=Kind.REALM||current.state==State.FREE)return false;
        slots.put(current.dimension(),new Slot(current.kind,current.index,current.generation,current.instance,current.owner,State.QUARANTINED));return true;
    }
    public synchronized Optional<Slot> find(String dimension){return Optional.ofNullable(slots.get(dimension));}
    /** Only lifecycle/maintenance may call this after closing all handles and verifying the archive. */
    public synchronized boolean cleanedAfterArchive(Slot request){
        var s=current(request);if(s==null||s.kind!=Kind.REALM||s.state!=State.QUARANTINED)return false;
        slots.put(s.dimension(),new Slot(s.kind,s.index,s.generation,s.instance,null,State.FREE));
        if(Objects.equals(activeRealm,s.instance))activeRealm=null;return true;
    }
    private Slot current(Slot request){
        var current=slots.get(request.dimension());
        return current!=null&&current.generation==request.generation&&Objects.equals(current.instance,request.instance)?current:null;
    }
    private boolean transition(Slot request,State from,State to){
        var s=current(request);if(s==null||s.state!=from)return false;
        slots.put(s.dimension(),new Slot(s.kind,s.index,s.generation,s.instance,s.owner,to));return true;
    }
    public synchronized String save(){
        var root=new JsonObject();root.addProperty("schema",2);
        if(activeRealm!=null)root.addProperty("activeRealm",activeRealm.toString());
        var list=new JsonArray();for(var s:slots.values())if(s.generation>0){var n=new JsonObject();
            n.addProperty("dimension",s.dimension());n.addProperty("generation",s.generation);n.addProperty("instance",s.instance.toString());
            if(s.owner!=null)n.addProperty("owner",s.owner.toString());n.addProperty("state",s.state.name());list.add(n);
        }root.add("slots",list);return root.toString();
    }
    public static InstanceLedger load(String json){
        if(json.length()>262144)throw new IllegalStateException("Oversized instance manifest");
        var root=JsonParser.parseString(json).getAsJsonObject();int schema=root.get("schema").getAsInt();if(schema!=1&&schema!=2)throw new IllegalStateException("Unknown instance manifest version");
        var result=new InstanceLedger();Set<UUID> instances=new HashSet<>(),owners=new HashSet<>();Set<String> dimensions=new HashSet<>();
        for(var entry:root.getAsJsonArray("slots")){
            var n=entry.getAsJsonObject();String key=n.get("dimension").getAsString();var base=result.slots.get(key);
            if(base==null||!dimensions.add(key))throw new IllegalStateException("Invalid/duplicate instance slot "+key);
            long generation=n.get("generation").getAsLong();var id=UUID.fromString(n.get("instance").getAsString());
            var owner=n.has("owner")?UUID.fromString(n.get("owner").getAsString()):null;var state=State.valueOf(n.get("state").getAsString());
            if(generation<1||!instances.add(id)||schema==1&&state==State.FREE||base.kind==Kind.REALM&&owner!=null||base.kind==Kind.CAVE&&(owner==null||!owners.add(owner)||state==State.FREE))throw new IllegalStateException("Invalid instance identity");
            result.slots.put(key,new Slot(base.kind,base.index,generation,id,owner,state));
        }
        if(root.has("activeRealm"))result.activeRealm=UUID.fromString(root.get("activeRealm").getAsString());
        var realms=result.slots.values().stream().filter(s->s.kind==Kind.REALM&&s.state!=State.FREE).toList();
        if(realms.size()>1||realms.isEmpty()&&result.activeRealm!=null||!realms.isEmpty()&&!realms.get(0).instance.equals(result.activeRealm))throw new IllegalStateException("Inconsistent active realm lock");
        return result;
    }
}
