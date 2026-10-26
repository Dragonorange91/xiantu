package com.longju.xiantu.client.equipment;

import com.google.gson.JsonParser;
import com.longju.xiantu.refining.WorkstationLayout;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import static com.longju.xiantu.client.equipment.WeaponAttackMotion.*;

/** Continuous attack/interrupt boundaries, real held-model grip and shared inventory geometry. */
public final class WeaponAttackRegression {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static double distance(Pose a,Pose b){
        return Math.abs(a.x()-b.x())+Math.abs(a.y()-b.y())+Math.abs(a.z()-b.z())
            +Math.abs(a.pitch()-b.pitch())+Math.abs(a.yaw()-b.yaw())+Math.abs(a.roll()-b.roll())
            +Math.abs(a.armPitch()-b.armPitch())+Math.abs(a.armYaw()-b.armYaw())+Math.abs(a.armRoll()-b.armRoll());
    }
    public static void main(String[] args)throws Exception{
        var signatures=new HashSet<Pose>();
        for(var kind:Kind.values()){
            for(float edge:new float[]{-1,0,1,2,Float.NaN,Float.POSITIVE_INFINITY})check(sample(kind,edge).equals(sample(Kind.NONE,0)),"invalid/stopped animation returns rest");
            Pose previous=sample(kind,0);
            for(int i=1;i<=10000;i++){
                var pose=sample(kind,i/10000f);
                check(Float.isFinite(pose.pitch())&&Float.isFinite(pose.armYaw())&&Math.abs(pose.x())<=.35&&Math.abs(pose.y())<=.3&&Math.abs(pose.z())<=.8,"finite bounded hand motion");
                check(distance(previous,pose)<1,"no single-frame discontinuity");
                check(Math.abs(pose.bodyYaw())<=25.001&&Math.abs(pose.armPitch())<=148.001,"bounded body and shoulder angles");
                check(pose.mirrored().mirrored().equals(pose),"both handedness settings preserve curve");
                previous=pose;
            }
            if(kind!=Kind.NONE){
                check(distance(sample(kind,.00001f),sample(kind,0))<.001&&distance(sample(kind,.99999f),sample(kind,1))<.001,"swing start/release joins resting transform");
                signatures.add(sample(kind,.43f));
            }
        }
        check(signatures.size()==4,"four independent attack trajectories");
        check(sample(Kind.SPEAR,.05f).z()-sample(Kind.SPEAR,.20f).z()>.4,"vanilla jab extends over .05-.20 instead of using a generic swing");
        check(Math.abs(VanillaSpearMotion.handPitch(REST)+10)<.01,"idle spear tip turns down ten degrees without translating the grip");
        check(Math.abs(-55+VanillaSpearMotion.handPitch(sample(Kind.SPEAR,.20f))+72)<.01,"combined display and hand transforms raise the previous -82 degree strike by ten degrees");
        for(int i=1;i<1000;i++){
            var p=sample(Kind.SPEAR,i/1000f);
            check(p.z()<=.121f,"long shaft pullback stays clear of the camera near plane");
            if(p.weight()>.95){
                check(p.y()>.26f&&p.x()<-.18f,"horizontal grip stays visible near screen centre");
                check(p.armPitch()<0,"third-person spear arm never drops forward-down during preparation");
            }
        }
        Path models=Path.of("src/main/resources/assets/xiantu/models/item");
        for(int id=11;id<=20;id++){
            var display=JsonParser.parseString(Files.readString(models.resolve("preview_equipment_%03d_held.json".formatted(id)))).getAsJsonObject().getAsJsonObject("display");
            for(String side:List.of("right","left")){
                var r=display.getAsJsonObject("thirdperson_"+side+"hand").getAsJsonArray("rotation");
                float rad=(float)Math.PI/180;
                float mirror=side.equals("right")?1:-1;
                var edge=new Matrix4f().rotateX(-90*rad).rotateY(180*rad).rotateXYZ(r.get(0).getAsFloat()*rad,r.get(1).getAsFloat()*rad*mirror,r.get(2).getAsFloat()*rad*mirror).transformDirection(new Vector3f(1,0,0));
                check(edge.z()<-.99,"axe blade faces forwards after actual vanilla hand-layer and left-hand mirroring");
                var first=display.getAsJsonObject("firstperson_"+side+"hand").getAsJsonArray("rotation");
                var firstEdge=new Matrix4f().rotateXYZ(first.get(0).getAsFloat()*rad,first.get(1).getAsFloat()*rad*mirror,first.get(2).getAsFloat()*rad*mirror).transformDirection(new Vector3f(1,0,0));
                check(firstEdge.z()<-.9,"first-person axe edge points forwards rather than left");
            }
        }
        for(var k:Kind.values())check(custom(k)==(k==Kind.SPEAR),"only spears override vanilla attack transforms");
        // Repeated real swing resets must not reset the visual state or its velocity.
        for(var k:List.of(Kind.SWORD,Kind.AXE,Kind.SPEAR,Kind.STAFF)){
            for(int gap:new int[]{3,4,6,10,24}){
                var clock=new WeaponAttackTimeline(k);Pose previous=clock.pose(0);
                for(int tick=0;tick<240;tick++){
                    if(tick<180&&tick%gap==0){var before=clock.pose(1);clock.attack();check(distance(before,clock.pose(1))==0,"retrigger preserves current pose");}
                    var old=clock.pose(1);clock.tick();check(distance(old,clock.pose(0))<.0001,"tick interpolation joins previous endpoint");
                    for(int f=0;f<12;f++){
                        var pose=clock.pose(f/12f);check(distance(previous,pose)<23,"rapid swings have bounded 240fps frame motion");
                        check(Math.abs(pose.pitch())<45&&Math.abs(pose.armPitch())<105,"no full wrist turns or overhead shoulder spikes");previous=pose;
                    }
                }
                check(distance(clock.pose(1),REST)<.0001,"release settles to idle without queued attacks");
            }
        }
        var fireX=WorkstationLayout.FIRE_X;var fireY=WorkstationLayout.FIRE_Y;
        check(fireX+8==WorkstationLayout.BAGUA_X&&fireY+8==WorkstationLayout.BAGUA_Y,"16px spirit-fire slot is centred on the bagua artwork");
        check(WorkstationLayout.BAGUA_X-WorkstationLayout.BAGUA_SIZE/2>=WorkstationLayout.PANEL_X+10&&WorkstationLayout.BAGUA_Y-WorkstationLayout.BAGUA_SIZE/2+WorkstationLayout.BAGUA_ART_Y_OFFSET>=WorkstationLayout.PANEL_Y+10,"complete artwork clears both top and left panel borders");
        check(WorkstationLayout.BAGUA_Y+WorkstationLayout.BAGUA_SIZE/2+WorkstationLayout.BAGUA_ART_Y_OFFSET<=WorkstationLayout.PANEL_Y+WorkstationLayout.PANEL_HEIGHT-10,"complete artwork clears bottom border");
        check(WorkstationLayout.REQUIREMENT_Y>WorkstationLayout.BAGUA_Y+WorkstationLayout.BAGUA_SIZE/2+20,"recipe examples stay outside material grid");
        var slots=new ArrayList<int[]>();slots.add(new int[]{fireX,fireY});
        for(int i=0;i<8;i++)slots.add(new int[]{WorkstationLayout.materialX(i),WorkstationLayout.materialY(i)});
        for(var a:slots){check(a[0]>=WorkstationLayout.PANEL_X+10&&a[0]+16<=342&&a[1]>=WorkstationLayout.PANEL_Y+10&&a[1]+16<=210,"expanded ring remains inside panel");for(var b:slots)if(a!=b)check(Math.abs(a[0]-b[0])>=18||Math.abs(a[1]-b[1])>=18,"all real item hit areas remain distinct");}
        var recipes=JsonParser.parseString(Files.readString(Path.of("src/main/resources/data/xiantu/refining/catalog.json"))).getAsJsonObject().getAsJsonArray("recipes");
        var weapons=new HashSet<String>();
        for(var row:recipes){var r=row.getAsJsonObject();String type=r.get("kind").getAsString();if(Set.of("sword","axe","spear","staff").contains(type)){check(kind(type)!=Kind.NONE,"every weapon kind has animation");weapons.add(r.get("output").getAsString());}}
        check(weapons.size()==31,"all thirty-one weapon models covered");
        System.out.println("Weapon animation and bagua regression PASS: "+checks+" checks, "+weapons.size()+" weapons, four motion profiles");
    }
}
