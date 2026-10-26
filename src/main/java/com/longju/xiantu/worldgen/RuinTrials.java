package com.longju.xiantu.worldgen;
import com.longju.xiantu.cultivation.RealmRegistry;
import com.longju.xiantu.item.*;
import com.longju.xiantu.network.*;
import com.longju.xiantu.combat.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.PacketDistributor;
import java.util.*;

/** Three distinct persisted trials. Only server positions/interactions advance their state. */
public final class RuinTrials {
    private static final List<String> ELEMENTS=List.of("metal","wood","water","fire","earth");
    private static String type(RuinAdventureData.Instance i){return i.spec.getAsJsonObject("bossPlan").get("trial").getAsString();}
    public static List<BlockPos> nodes(RuinAdventureData.Instance i){var out=new ArrayList<BlockPos>();for(var v:i.spec.getAsJsonArray("encounters"))out.add(i.absolute(v.getAsJsonObject().getAsJsonArray("position")).below());return out;}
    private static BlockPos core(RuinAdventureData.Instance i){return i.absolute(i.spec.getAsJsonObject("bossPlan").getAsJsonArray("position")).offset(6,0,0);}
    public static String instructions(RuinAdventureData.Instance i){return switch(type(i)){
        case "elements" -> "五行锁妖窟：按金→木→水→火→土清理守护，依次触碰五宫阵眼，再守住封印井30秒。";
        case "stars" -> "星陨祭坛：清理守护后，按西台→东营→书院的次序踏上阵眼；进入主祭坛躲避落星30秒，再右键星核。";
        default -> "水帘洞天：桃根药洞阵眼按金→木→水→火→土投入妖丹；主厅击破三枚棍影核心，再躲避棍阵90秒，右键洞天核心。";
    };}
    public static boolean interact(ServerLevel l,Player player,BlockPos at,InteractionHand hand){
        if(!(player instanceof ServerPlayer p)||hand!=InteractionHand.MAIN_HAND)return false;
        for(var i:RuinAdventureData.get(l).at(at)){
            if(!i.boss()||i.defeated()||!i.bounds.contains(Vec3.atCenterOf(at)))continue;
            var ns=nodes(i);int idx=ns.indexOf(at);boolean isCore=core(i).equals(at);if(idx<0&&!isCore)return false;
            if(!RuinAdventureService.legal(p,i)){p.displayClientMessage(Component.literal("境界不符，无法推进遗迹试炼"),true);return true;}
            var state=i.state;String type=type(i);
            if(state.getBoolean("TrialComplete")){p.displayClientMessage(Component.literal("试炼已完成，进入主厅迎战首领"),true);return true;}
            if(type.equals("elements")){
                if(idx<0)return true;
                if(!i.guardiansDone()){p.displayClientMessage(Component.literal(instructions(i)),true);return true;}
                String theme=i.spec.getAsJsonArray("encounters").get(idx).getAsJsonObject().get("lootTheme").getAsString();int step=state.getInt("RuneStep");
                if(step<5&&theme.equals(ELEMENTS.get(step))){state.putInt("RuneStep",step+1);p.displayClientMessage(Component.literal("阵眼已共鸣 "+(step+1)+"/5"),true);}else if(step<5){state.putInt("RuneStep",0);p.displayClientMessage(Component.literal("共鸣顺序不符：金→木→水→火→土"),true);}
            }else if(type.equals("monkey")&&state.getInt("Offering")<5&&idx==0){
                var stack=p.getItemInHand(hand);String element=stack.getItem()instanceof DemonCoreItem?DemonCoreItem.element(stack):stack.getItem()instanceof BeastMaterialItem?BeastMaterialItem.element(stack):"";int step=state.getInt("Offering");
                if(ELEMENTS.get(step).equals(element)){stack.shrink(1);state.putInt("Offering",step+1);p.displayClientMessage(Component.literal("五行棍势已回应 "+(step+1)+"/5"),true);}
                else{int errors=state.getInt("Errors")+1;state.putInt("Errors",errors%5);if(errors>=5)state.putInt("Offering",0);state.putInt("Penalty",state.getInt("Penalty")+600);p.displayClientMessage(Component.literal("材料未消耗；需要"+new String[]{"金","木","水","火","土"}[step]+"属性妖丹或妖兽材料"),true);}
            }else if(isCore&&state.getBoolean("HazardPassed")){state.putBoolean("TrialComplete",true);p.displayClientMessage(Component.literal("试炼已完成，洞天封印解除"),true);}
            else p.displayClientMessage(Component.literal(instructions(i)),true);
            RuinAdventureData.get(l).setDirty();return true;
        }return false;
    }
    public static void tick(ServerLevel l,RuinAdventureData.Instance i,List<ServerPlayer> party){
        var state=i.state;if(state.getBoolean("TrialComplete"))return;String type=type(i);var visitors=l.players().stream().filter(p->RuinAdventureService.legal(p,i)&&i.bounds.contains(p.position())).toList();
        for(var p:visitors){String discovered="Seen_"+p.getUUID();if(!state.getBoolean(discovered)){state.putBoolean(discovered,true);p.sendSystemMessage(Component.literal(i.spec.get("name").getAsString()+" · "+instructions(i)));}}
        if(!i.guardiansDone())return;
        if(type.equals("stars")&&state.getInt("StarStep")<3){
            int step=state.getInt("StarStep");var ns=nodes(i);var target=ns.get(step);if(visitors.stream().anyMatch(p->p.distanceToSqr(Vec3.atCenterOf(target))<5)){state.putInt("StarStep",step+1);visitors.forEach(p->p.displayClientMessage(Component.literal("星轨点亮 "+(step+1)+"/3"),true));}return;
        }
        if(state.getBoolean("HazardPassed"))return;
        if(type.equals("elements")&&state.getInt("RuneStep")<5||type.equals("monkey")&&state.getInt("Offering")<5)return;
        if(party.isEmpty())return;
        if(type.equals("monkey")&&state.getInt("Penalty")>0){state.putInt("Penalty",Math.max(0,state.getInt("Penalty")-10));return;}
        // Three rune targets move in order around the ring. Strike the exposed core, not a real mob.
        if(type.equals("monkey")&&state.getInt("Phantoms")<3){
            int phase=state.getInt("Phantoms"),time=state.getInt("PhantomTime")+10;state.putInt("PhantomTime",time);var home=i.absolute(i.spec.getAsJsonObject("bossPlan").getAsJsonArray("position"));
            double angle=(time/20.0+phase*2)*.5;Vec3 spot=Vec3.atBottomCenterOf(home).add(Math.sin(angle)*7,1,Math.cos(angle)*7);
            if(time%40==0)effect(l,party,"spirit_ape_king/staff_array/charge",spot,30);
            // Touch-and-strike uses an existing solid rune; the hologram is only the moving warning.
            BlockPos rune=core(i).offset(0,0,(phase-1)*5);if(l.getBlockState(rune).isAir())RuinProtection.internal(()->l.setBlock(rune,Blocks.CHISELED_STONE_BRICKS.defaultBlockState(),3));
            if(time%80==40)hit(l,party,spot,2,i,type);
            return;
        }
        int required=type.equals("monkey")?1800:600;
        if(state.getBoolean("HazardPassed"))return;
        int elapsed=state.getInt("HazardTicks")+10;state.putInt("HazardTicks",elapsed);var center=Vec3.atBottomCenterOf(i.absolute(i.spec.getAsJsonObject("bossPlan").getAsJsonArray("position")));
        if(elapsed%80==10){var target=party.get((elapsed/80)%party.size());var pos=target.position();state.putDouble("HazardX",pos.x);state.putDouble("HazardY",pos.y);state.putDouble("HazardZ",pos.z);effect(l,party,type.equals("stars")?"meteor_general/falling_stars/charge":type.equals("elements")?"five_element_warden/prison_ring/charge":"spirit_ape_king/staff_array/charge",pos,40);}
        if(elapsed%80==50){var pos=new Vec3(state.getDouble("HazardX"),state.getDouble("HazardY"),state.getDouble("HazardZ"));hit(l,party,pos,type.equals("monkey")?2.5:3,i,type);}
        if(elapsed>=required){state.putBoolean("HazardPassed",true);if(type.equals("elements"))state.putBoolean("TrialComplete",true);else RuinProtection.internal(()->l.setBlock(core(i),Blocks.CHISELED_STONE_BRICKS.defaultBlockState(),3));party.forEach(p->p.displayClientMessage(Component.literal(type.equals("elements")?"封印井已稳定，首领即将苏醒":"试炼通过，右键主厅星纹核心解封"),true));}
        if(elapsed%100==0)party.forEach(p->p.displayClientMessage(Component.literal("遗迹试炼 "+elapsed/20+" / "+required/20+" 秒"),true));
    }
    public static boolean strike(ServerLevel l,Player p,BlockPos at){
        for(var i:RuinAdventureData.get(l).at(at))if(i.boss()&&!i.defeated()&&type(i).equals("monkey")&&p instanceof ServerPlayer player&&RuinAdventureService.legal(player,i)&&i.guardiansDone()&&i.state.getInt("PhantomTime")>0&&l.getBlockState(at).is(Blocks.CHISELED_STONE_BRICKS)&&i.state.getInt("Offering")==5&&i.state.getInt("Phantoms")<3){
            int phase=i.state.getInt("Phantoms");if(!at.equals(core(i).offset(0,0,(phase-1)*5)))continue;
            i.state.putInt("Phantoms",phase+1);RuinProtection.internal(()->l.removeBlock(at,false));RuinAdventureData.get(l).setDirty();p.displayClientMessage(Component.literal("棍影核心已破 "+(phase+1)+"/3"),true);return true;
        }return false;
    }
    private static void hit(ServerLevel l,List<ServerPlayer> party,Vec3 pos,double radius,RuinAdventureData.Instance i,String type){
        effect(l,party,type.equals("stars")?"meteor_general/impact":type.equals("elements")?"five_element_warden/impact":"spirit_ape_king/impact",pos,20);
        for(var p:party)if(p.distanceToSqr(pos)<radius*radius&&p.onGround()){
            var element=type.equals("stars")?Element.THUNDER:Element.EARTH;
            if(p.hurt(new ElementalDamageSource(l.damageSources().magic(),element),(float)(RealmRegistry.baseAttack(i.spec.getAsJsonObject("bossPlan").getAsJsonArray("realmRange").get(0).getAsInt(),0)*.2))&&type.equals("monkey")){
                int hits=i.state.getInt("TrialHits")+1;i.state.putInt("TrialHits",hits);if(hits>=3){i.state.putInt("TrialHits",0);i.state.putInt("HazardTicks",0);party.forEach(q->q.displayClientMessage(Component.literal("本轮棍阵失败，已完成步骤保留"),true));}
            }
        }
    }
    private static void effect(ServerLevel l,List<ServerPlayer> players,String path,Vec3 pos,int duration){for(var p:players)XiantuNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new BeastVfxPacket(new net.minecraft.resources.ResourceLocation("xiantu","beasts/"+path),l.dimension().location(),p.getId(),p.getUUID(),pos,0,duration,false));}
    private RuinTrials(){}
}
