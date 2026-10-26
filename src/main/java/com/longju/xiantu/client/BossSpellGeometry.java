package com.longju.xiantu.client;

import com.longju.xiantu.entity.BossCombatSkills;
import com.longju.xiantu.entity.CompletionLandEntity;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Authored spell subjects, with solid faceted cores and flowing surfaces.
 * No extra entities/GLB instances; server origin/aim/time remain authoritative. */
final class BossSpellGeometry {
    private final VertexConsumer v; private final Matrix4f m; private final Vec3 camera;
    private static final Vec3 UP=new Vec3(0,1,0);
    private BossSpellGeometry(VertexConsumer v,Matrix4f m,Vec3 camera){this.v=v;this.m=m;this.camera=camera;}
    static void draw(VertexConsumer v,Matrix4f m,Vec3 camera,CompletionLandEntity boss,
                     BossCombatSkills.Skill s,CompoundTag n,double age,int color,float alpha){
        new BossSpellGeometry(v,m,camera).spell(boss,s,n,age,color,alpha);
    }
    private static Vec3 pos(CompoundTag n,String p){return new Vec3(n.getDouble(p+"X"),n.getDouble(p+"Y"),n.getDouble(p+"Z"));}
    private static double clamp(double p){return Math.max(0,Math.min(1,p));}
    private static int shade(int c,double f){return ((int)(((c>>16)&255)*f)<<16)|((int)(((c>>8)&255)*f)<<8)|(int)((c&255)*f);}
    static void meteor(VertexConsumer v,Matrix4f m,Vec3 camera,CompletionLandEntity boss,BossCombatSkills.Skill skill,CompoundTag n,double age,float partial){
        var g=new BossSpellGeometry(v,m,camera);String id=skill.id();double t=age-skill.windup();
        Vec3 o=pos(n,"origin"),d=pos(n,"direction").normalize(),side=new Vec3(d.z,0,-d.x),aim=pos(n,"aim");
        int c=0x88CCFF;float fade=(float)clamp((skill.active()-t)/6);
        if((id.equals("meteor_guard")&&t>=0&&t<skill.active())||(id.equals("final_star")&&t<0)){
            Vec3 at=o.add(side.scale(id.equals("meteor_guard")?1.8:2)).add(0,id.equals("meteor_guard")?2.65:1.65,0);
            g.crystal(at,UP,.28,.48,0xD9F5FF,.95f);
            for(int i=0;i<3;i++)g.flow(at.add(0,-.5,0),UP,.42,.95,age*.06+i*2.094,0x6BAFFF,.6f,.035);
        }
        if(!java.util.Set.of("attack","blink_pierce","star_javelin","star_track").contains(id))return;
        if(t<0){
            Vec3 at=boss.getPosition(partial).add(side.scale(.48)).add(0,1.65,0);
            for(int i=0;i<2;i++)g.flow(at,d,.18*(1-clamp(age/skill.windup())*.5),1.6,age*.12+i*Math.PI,c,.45f,.035);
            if(id.equals("blink_pierce")){
                Vec3 end=aim.subtract(d.scale(3.2));g.arc(end.add(0,.06,0),.8,.045,0,Math.PI*2,c,.5f,0);
                g.line(end.add(0,.08,0),end.add(d.scale(skill.reach())).add(0,.08,0),c,.35f,.06);
            }return;
        }
        if(t>skill.active())return;boolean melee=id.equals("attack")||id.equals("blink_pierce");
        Vec3 base=melee?boss.getPosition(partial):o;double p=clamp(t/skill.active());
        double extension=melee?Math.sin(Math.PI*clamp(p/.8))*skill.reach():p*skill.reach();
        Vec3 tip=base.add(d.scale(Math.max(.7,extension))).add(0,1.5,0);
        g.lance(tip,d,melee?2.8:3.4,c,fade);
        for(int i=0;i<2;i++)g.flow(tip.subtract(d.scale(2.6)),d,.12,2.6,age*.2+i*Math.PI,c,fade*.4f,.04);
        if(id.equals("blink_pierce")&&t<8){g.arc(o.add(0,.12,0),.6+t*.12,.12,0,Math.PI*2,c,(float)(1-t/8)*.6f,0);g.fragments(o.add(0,1,0),age,8,c,(float)(1-t/8),.8,false);}
    }
    private void lance(Vec3 tip,Vec3 d,double length,int color,float alpha){
        Vec3 neck=tip.subtract(d.scale(.75));line(tip.subtract(d.scale(length)),neck,0xD4B15F,alpha,.085);crystal(tip.subtract(d.scale(.38)),d,.15,.5,color,alpha);line(tip.subtract(d.scale(length)),tip,color,alpha*.20f,.22);line(neck,tip,0xEEFBFF,alpha,.025);
    }
    static void beast(VertexConsumer v,Matrix4f m,Vec3 camera,String species,String stage,Vec3 at,Vec3 dir,double age,int ticks){
        var g=new BossSpellGeometry(v,m,camera);
        int c=switch(species){
            case "yanwei_fox","red_sun_lion","flint_armored_lizard","fire_beak_falcon","molten_turtle"->0xFF913E;
            case "cold_fang_wolf","jade_scale_crocodile","azure_scale_python","dark_shell_crab","tide_pattern_ray"->0x8BDDEB;
            case "electric_marten","thunder_prison_lion","purple_lightning_bat","thunder_feather_raven","thunder_deer"->0xB4B9FF;
            case "moss_back_bear","jade_leaf_mantis","ancient_branch_python","mushroom_toad","vine_deer"->0x9EDB73;
            case "crag_rock_spider","mountain_bear","rock_back_boar","thick_horn_rhino","sand_armadillo"->0xCFB17B;
            default->0xE9D5A1;
        };
        double p=clamp(age/Math.max(1,ticks));float fade=(float)(1-clamp((age-ticks*.55)/(ticks*.45+12)));
        if(age<0||fade<=0)return;
        Vec3 side=new Vec3(dir.z,0,-dir.x);
        if(stage.equals("flee")){g.fragments(at.add(0,.15,0),age,5,c,fade*.45F,.45,true);return;}
        if(stage.equals("warn")){
            for(int i=0;i<2;i++)g.flow(at.add(0,.45,0),UP,.3*(1-p*.5),.65,age*.08+i*Math.PI,c,fade*(float)(p*.3),.025);
            return;
        }
        boolean hit=stage.equals("impact");Vec3 centre=at.add(0,hit?.25:.75,0);double size=hit?.5+1.1*Math.sin(p*Math.PI*.7):.65;
        switch(species){
            case "yanwei_fox","flint_armored_lizard","red_sun_lion","fire_beak_falcon" -> {
                g.crystal(centre,dir,.20*size,.4*size,0xFFF1A4,fade*.9F);
                for(int i=0;i<3;i++)g.flow(centre.subtract(dir.scale(.9)),dir,.22*size,1.3,age*.21+i*2.094,c,fade*.60F,.12);
                g.fragments(centre,age,12,c,fade,size,true);
            }
            case "electric_marten","thunder_prison_lion","purple_lightning_bat","thunder_feather_raven" -> {
                g.crystal(centre,dir,.14,.35,0xE6EDFF,fade);
                for(int i=0;i<5;i++){double a=i*Math.PI*2/5;Vec3 end=centre.add(Math.sin(a)*size,.3+Math.cos(a)*.25,Math.cos(a)*size);g.bolt(centre,end,age,i,c,fade*.85F);}
            }
            case "crag_rock_spider" -> {
                for(int i=0;i<8;i++){double a=i*Math.PI/4;Vec3 d=new Vec3(Math.sin(a),0,Math.cos(a));g.line(at.add(0,.06,0),at.add(d.scale(size)).add(0,.06,0),c,fade*.65F,.035);g.crystal(at.add(d.scale(size*.7)),UP,.12,.22,c,fade);}
            }
            case "rock_back_boar","thick_horn_rhino","mountain_bear","gold_back_beetle" -> {
                for(int i=-2;i<=2;i++){Vec3 p0=centre.add(side.scale(i*.35)).add(dir.scale(-Math.abs(i)*.20));g.crystal(p0,dir.add(0,.5,0).normalize(),.18,.55,c,fade*.8F);}
                g.fragments(centre,age,12,shade(c,.70),fade,size,true);
            }
            case "mushroom_toad" -> {
                for(int i=0;i<3;i++)g.flow(at.add(0,i*.22,0),UP,.4+i*.2,1,age*.035+i*2,c,fade*.18F,.16);
                g.fragments(centre,age,16,c,fade*.55F,size,false);
            }
            case "ancient_branch_python","azure_scale_python","jade_scale_crocodile","dark_shell_crab" -> {
                for(int i=0;i<2;i++)g.arc(centre,size+i*.17,.16,age*.08+i*Math.PI,age*.08+i*Math.PI+1.8,c,fade*.65F,.18);
                g.fragments(centre,age,10,c,fade*.6F,size*.8,true);
            }
            default -> {
                // Blade wolves, mantis and bears: three unequal curved claw surfaces.
                for(int i=-1;i<=1;i++)g.arc(centre.add(0,i*.19,0),size,.14,age*.11+i*.17,age*.11+i*.17+1.4,c,fade*.72F,.16);
                g.fragments(centre,age,8,c,fade*.55F,size*.7,true);
            }
        }
    }
    private void spell(CompletionLandEntity b,BossCombatSkills.Skill s,CompoundTag n,double age,int c,float alpha){
        Vec3 o=pos(n,"origin"),aim=pos(n,"aim"),d=pos(n,"direction").normalize(),side=new Vec3(d.z,0,-d.x);
        double active=age-s.windup(),p=clamp(active/s.active()),r=s.reach();String id=s.id();
        boolean warning=active<0;
        if(warning){
            double charge=clamp(age/s.windup());Vec3 at=b.position().add(0,b.getBbHeight()*.62,0);
            // Sparse inward streams, always above telegraph outlines; no safe-gap coverage.
            for(int i=0;i<3;i++)flow(at,UP,.65*(1-charge*.45),1.2,age*.09+i*2.094,c,(float)(.22*charge),.035);
            if(id.equals("falling_stars")||id.equals("final_star"))crystal(aim.add(0,7,0),UP,.18+.4*charge,.6+charge,c,(float)(.7*charge));
            if(b.definitionId().equals("xuantians_remnant")){
                for(int sign:new int[]{-1,1}){
                    Vec3 hand=at.add(side.scale(sign*.45)).add(d.scale(.5));
                    flow(hand.subtract(d.scale(.35)),d,.13,.5,age*.10,c,(float)(.42*charge),.045);
                }
            }
            if(id.equals("blade_wheel"))wheel(at,d,1.0*charge,age*.08,c,(float)(.65*charge));
            return;
        }
        switch(id){
            case "star_javelin","star_track","blink_pierce" -> {
                Vec3 tip=o.add(d.scale(r*p)).add(0,1.15,0);
                sword(tip,d,id.equals("star_track")?3.6:id.equals("blink_pierce")?4.4:2.8,c,alpha);
                for(int i=0;i<3;i++)flow(tip.subtract(d.scale(3)),d,.22,3,age*.22+i*2.094,c,alpha*.32F,.055);
                fragments(tip,age,12,c,alpha,.6,false);
            }
            case "falling_stars","final_river","final_elements" -> {
                double a=Math.min(active,s.active()-.001);int wave=Math.min(4,(int)(a/12));double local=clamp((a-wave*12)/12),ang=wave*Math.PI*2/5;
                Vec3 landing=aim.add(Math.sin(ang)*3,0,Math.cos(ang)*3),at=landing.add(0,Math.max(.15,8*(1-local/.8)),0);
                if(id.equals("final_river")){
                    for(int i=0;i<3;i++)flow(at,UP,.35+i*.18,4,age*.12+i*2,c,alpha*.42F,.24);
                }else{
                    int cc=id.equals("final_elements")?new int[]{0xE6C46E,0x80CD78,0x72D9EE,0xFF8240,0xCAAA79}[wave]:c;
                    crystal(at,UP,.55,1.0,shade(cc,.48),alpha);
                    crystal(at,UP,.21,1.22,cc,alpha);
                    for(int i=0;i<3;i++)flow(at,UP,.4,3.2,age*.19+i*2.094,cc,alpha*.5F,.12);
                }
                if(local>=.70)impact(landing,r*clamp((local-.70)/.30),age,c,alpha);
            }
            case "thunder_ring","final_star","staff_array","prison_ring" -> {
                double radius=Math.max(.3,r*p);
                for(int i=0;i<20;i++){
                    double a=i*Math.PI*2/20;Vec3 axis=new Vec3(Math.sin(a),0,Math.cos(a));
                    if(axis.dot(d)>.77)continue;Vec3 at=o.add(axis.scale(radius)).add(0,.18,0);
                    if(id.equals("staff_array"))staff(at.add(0,1.2,0),axis.add(0,.3,0).normalize(),1.8,c,alpha);
                    else if(id.equals("thunder_ring"))bolt(at,at.add(axis.yRot(.4F).scale(.8)).add(0,.5,0),age,i,c,alpha);
                    else crystal(at,axis.add(0,.7,0).normalize(),.10,.55,c,alpha*.8F);
                    arc(o.add(0,.12,0),radius,.10,a,a+.22,c,alpha*.5F,.025);
                }
                if(id.equals("final_star"))fragments(o.add(0,1,0),age,18,c,alpha*.6F,1+p*2,true);
            }
            case "meteor_guard","element_cycle" -> {
                for(int i=0;i<5;i++){
                    double a=age*.045+i*Math.PI*2/5;Vec3 at=b.position().add(Math.sin(a)*1.8,1.6+Math.sin(a*2)*.4,Math.cos(a)*1.8);
                    int cc=id.equals("element_cycle")?new int[]{0xE6C46E,0x80CD78,0x72D9EE,0xFF8240,0xCAAA79}[i]:c;
                    crystal(at,UP,.23,.48,shade(cc,.5),alpha);crystal(at,UP,.07,.55,cc,alpha);
                    glyph(at.add(0,.1,-.25),.3,a,cc,alpha*.7F,i);
                }
            }
            case "earth_split","prison_stomp","wall_fall" -> {
                double radius=Math.max(.1,r*p);
                for(int i=0;i<12;i++){
                    double angle=i*Math.PI/6;Vec3 axis=new Vec3(Math.sin(angle),0,Math.cos(angle));
                    Vec3 at=o.add(axis.scale(radius));
                    crystal(at.add(0,.20*(1-p),0),axis.add(0,.7,0).normalize(),.18,.42*(1-p)+.12,shade(c,.65),alpha*.85F);
                    line(at.subtract(axis.scale(.65)),at,c,alpha*.65F,.04);
                }
                fragments(o,age,16,0xA68A50,alpha*.6F,Math.max(.2,radius),true);
            }
            case "final_staff" -> {
                Vec3 head=o.add(d.scale(r*p));
                for(int i=0;i<9;i++){
                    double q=clamp((p-i/13.0)*4);if(q<=0)continue;
                    Vec3 at=o.add(d.scale(r*i/10.0));double h=Math.sin(q*Math.PI)*.7;
                    crystal(at.add(side.scale((i%2==0?1:-1)*.4)).add(0,h*.4,0),UP,.35,h+.15,0xA68A50,alpha*.8F);
                    arc(at.add(0,.12,0),.7*q,.12,0,Math.PI*2,c,alpha*(float)(1-q),.02);
                }
                if(id.equals("final_staff"))staff(head.add(0,1.2*(1-p)+.3,0),d,5,c,alpha*.9F);
                impact(head,1.1,age,c,alpha*.8F);
            }
            case "blade_wheel" -> {Vec3 at=o.add(0,.6,0);wheel(at,UP,Math.max(.3,r*p),age*.22,c,alpha);fragments(at,age,10,c,alpha,Math.max(.3,r*p),false);}
            case "formation_charge","cloud_assault","attack","armour_counter" -> {
                Vec3 at=b.position().add(0,b.getBbHeight()*.5,0);
                if(id.equals("attack")&&b.definitionId().equals("xuantians_remnant")){
                    fist(at.add(d.scale(.4+Math.sin(p*Math.PI)*1.5)),d,.7,c,alpha);
                    break;
                }
                if(id.equals("armour_counter")){for(int i=-2;i<=2;i++)crystal(at.add(d.yRot(i*.24F).scale(.8)),UP,.22,.6,c,alpha*.7F);}
                else {
                    double sweep=p*Math.PI*1.5;for(int layer=0;layer<3;layer++)arc(at,1.8+layer*.18,.22,-sweep,-sweep+1.4,c,alpha*(.55F-layer*.13F),layer*.22);
                    for(int i=0;i<2;i++)flow(at.subtract(d.scale(2)).add(side.scale(i==0?-.6:.6)),d,.16,2,age*.16+i*3,c,alpha*.35F,.10);
                }
            }
            case "king_roar","five_breath" -> {
                for(int i=0;i<3;i++){
                    double q=clamp(p-i*.10);Vec3 at=o.add(d.scale(r*q)).add(0,1.1,0);double width=.3+q*1.7;
                    for(int j=-3;j<=3;j++){
                        Vec3 x=at.add(side.scale(j*width/3)).add(0,Math.sqrt(Math.max(0,1-j*j/9.0))*width*.5,0);
                        crystal(x,d,.05,.24,c,alpha*(.65F-i*.15F));
                        if(j<3)line(x,at.add(side.scale((j+1)*width/3)).add(0,Math.sqrt(Math.max(0,1-(j+1)*(j+1)/9.0))*width*.5,0),c,alpha*.35F,.08);
                    }
                }
            }
            case "palace_cloud" -> {
                Vec3 at=aim.add(0,Math.max(.12,6*(1-p/.65)),0);
                for(int row=0;row<3;row++)for(int col=-2;col<=2;col++){
                    Vec3 cell=at.add(col*.6+(row%2)*.22,row*.45,0);
                    crystal(cell,UP,.42,.34,id.equals("wall_fall")?0x617875:0xB4DDE9,alpha*.8F);
                    line(cell.add(-.27,-.18,-.3),cell.add(.27,-.18,-.3),c,alpha*.6F,.028);
                }
                if(p>=.6)impact(aim,r*clamp((p-.6)/.4),age,c,alpha);
            }
            case "sword_array" -> {
                double phase=(Math.min(active,s.active()-.001)%10)/10;int punch=(int)(active/10);
                float pulse=(float)Math.sin(phase*Math.PI);Vec3 at=b.position().add(0,b.getBbHeight()*.62,0).add(side.scale(punch%2==0?.36:-.36)).add(d.scale(.6+phase*1.4));
                fist(at,d,.75,c,alpha*pulse);flow(at.subtract(d.scale(1.2)),d,.20,1.2,age*.15,c,alpha*pulse*.4F,.10);
            }
            case "city_seal" -> {
                Vec3 at=b.position().add(0,b.getBbHeight()*.65,0).add(d.scale(.6));
                for(int i=-1;i<=1;i++)crystal(at.add(side.scale(i*.25)),UP,.16,.38,c,alpha*.5F);
                if(n.getBoolean("counterTriggered")){fist(at.add(d.scale(.9)),d,1,c,alpha);fragments(at,age,10,c,alpha,.8,true);}
            }
            case "final_city" -> {
                Vec3 at=b.position().add(0,b.getBbHeight()*.62,0).add(d.scale(.9));fist(at,d,1.15,c,alpha);
                for(int i=-1;i<=1;i+=2)flow(at.subtract(d.scale(2)).add(side.scale(i*.25)),d,.22,2,age*.15+i,c,alpha*.4F,.11);
                fragments(b.position(),age,12,0xBBAD7F,alpha*.6F,1,true);
            }
            case "cloud_swords" -> {
                for(int i=-1;i<=1;i++){Vec3 at=o.add(d.scale(r*p)).add(side.scale(i*2.6)).add(0,1.1,0);sword(at,d,2.6,c,alpha);flow(at.subtract(d.scale(2.4)),d,.25,2.4,age*.12+i,c,alpha*.35F,.18);}
            }
            case "water_pressure" -> {
                double radius=r*(1-.55*p);for(int layer=0;layer<3;layer++){
                    double y=.15+(2-layer*.6)*(1-p);for(int i=0;i<5;i++)arc(aim.add(0,y,0),radius+layer*.10,.24,i*Math.PI*2/5+age*.014,i*Math.PI*2/5+age*.014+.8,c,alpha*.38F,.08);
                }
            }
            case "cloud_locks" -> {
                for(int i=0;i<3;i++){if((n.getInt("brokenLinks")&(1<<i))!=0)continue;double a=i*Math.PI*2/3;Vec3 end=aim.add(Math.sin(a)*3,1.2,Math.cos(a)*3),start=aim.add(0,1,0);
                    for(int j=0;j<12;j++){double q=j/12.;Vec3 at=start.lerp(end,q).add(0,-Math.sin(q*Math.PI)*.35,0);crystal(at,end.subtract(start).normalize(),.075,.14,c,alpha*.75F);}
                    seal(end.add(0,-1.05,0),.32,age*.02,c,alpha);
                }
            }
            case "banner_command","final_formation" -> {
                if(id.equals("banner_command")){
                    Vec3 at=o.add(side.scale(2));line(at,at.add(0,3.2,0),0xB58D48,alpha,.08);
                    for(int i=0;i<10;i++){double x=i*.12;Vec3 a=at.add(side.scale(x)).add(0,2.8,.08*Math.sin(age*.15+i*.4));Vec3 z=at.add(side.scale(x+.12)).add(0,2.8,.08*Math.sin(age*.15+(i+1)*.4));quad(a,z,z.add(0,-.9,0),a.add(0,-.9,0),shade(c,.35),alpha*.8F);}
                    glyph(at.add(side.scale(.55)).add(0,2.3,-.02),.5,0,c,alpha,3);
                }else for(int sign:new int[]{-1,1}){Vec3 dir=d.add(side.scale(sign)).normalize();Vec3 at=o.add(dir.scale((p*2-1)*r)).add(0,.9,0);sword(at,dir,4,c,alpha);flow(at.subtract(dir.scale(4)),dir,.3,4,age*.18,c,alpha*.4F,.18);}
            }
            case "ape_phantoms","soul_phantoms","mirror_water" -> {
                for(int i=0;i<3;i++){double a=i*Math.PI*2/3;Vec3 at=o.add(Math.sin(a)*3,.15,Math.cos(a)*3);for(int j=0;j<2;j++)flow(at.add(0,j*.2,0),UP,.7,2.6,age*.04+i+j*3,c,alpha*.18F,.06);}
            }
            default -> { }
        }
    }
    private void fist(Vec3 at,Vec3 forward,double size,int color,float alpha){
        Vec3 side=new Vec3(forward.z,0,-forward.x);
        crystal(at.subtract(forward.scale(size*.22)),forward,size*.30,size*.38,shade(color,.65),alpha*.8F);
        for(int i=0;i<4;i++){
            Vec3 knuckle=at.add(side.scale((i-1.5)*size*.16)).add(0,size*.12,0);
            crystal(knuckle,forward,size*.115,size*.18,color,alpha);
            line(knuckle.subtract(forward.scale(size*.2)),knuckle.add(forward.scale(size*.16)),0xFFF0C6,alpha*.65F,.025);
        }
        crystal(at.add(side.scale(size*.32)).add(0,-size*.10,0),forward,size*.15,size*.25,color,alpha*.85F);
    }
    private void sword(Vec3 tip,Vec3 dir,double length,int c,float a){
        Vec3 shaft=tip.subtract(dir.scale(length*.48));crystal(shaft,dir,.16,length*.52,c,a*.9F);
        Vec3 cross=dir.cross(UP);if(cross.lengthSqr()<.01)cross=new Vec3(1,0,0);cross=cross.normalize();
        Vec3 hilt=tip.subtract(dir.scale(length*.83));line(hilt.subtract(cross.scale(.34)),hilt.add(cross.scale(.34)),0xFFE3A1,a,.095);
        line(tip.subtract(dir.scale(length)),hilt,shade(c,.55),a,.09);
        line(tip.subtract(dir.scale(length*.72)),tip,0xFFF8DE,a*.85F,.028);
    }
    private void staff(Vec3 centre,Vec3 axis,double length,int c,float a){
        Vec3 start=centre.subtract(axis.scale(length*.5)),end=centre.add(axis.scale(length*.5));line(start,end,shade(c,.36),a,.16);
        for(int sign:new int[]{-1,1})crystal(centre.add(axis.scale(sign*length*.43)),axis,.16,length*.11,c,a);
        line(start,end,c,a*.26F,.25);
    }
    private void wheel(Vec3 centre,Vec3 axis,double radius,double angle,int c,float a){
        Vec3 u=axis.cross(UP).normalize();if(u.lengthSqr()<.01)u=new Vec3(1,0,0);Vec3 w=axis.cross(u).normalize();
        for(int i=0;i<6;i++){double t=angle+i*Math.PI/3;Vec3 radial=u.scale(Math.cos(t)).add(w.scale(Math.sin(t)));Vec3 tangent=u.scale(-Math.sin(t)).add(w.scale(Math.cos(t)));
            crystal(centre.add(radial.scale(radius*.72)),tangent,.12,.64,c,a);line(centre.add(radial.scale(.35)),centre.add(radial.scale(radius*.7)),c,a*.6F,.055);}
        crystal(centre,axis,.22,.18,0xFFEBB0,a);
    }
    private void seal(Vec3 at,double size,double angle,int c,float a){
        Vec3[] p=new Vec3[4];for(int i=0;i<4;i++){double t=angle+Math.PI/4+i*Math.PI/2;p[i]=at.add(Math.sin(t)*size,0,Math.cos(t)*size);}
        for(int i=0;i<4;i++){line(p[i],p[(i+1)%4],c,a,.055);quad(p[i],p[(i+1)%4],p[(i+1)%4].add(0,.2,0),p[i].add(0,.2,0),shade(c,.42),a*.6F);}
        for(int i=0;i<4;i++){Vec3 p0=at.add(Math.sin(angle+i*Math.PI/2)*size*.5,.04,Math.cos(angle+i*Math.PI/2)*size*.5);glyph(p0,size*.35,angle+i*Math.PI/2,c,a,i);}
    }
    private void glyph(Vec3 at,double s,double angle,int c,float alpha,int seed){
        Vec3 u=new Vec3(Math.cos(angle),0,Math.sin(angle)),w=new Vec3(-Math.sin(angle),0,Math.cos(angle));
        for(int i=0;i<4;i++){double x=(i-1.5)*s*.22;line(at.add(u.scale(x)).add(w.scale(-s*.35)),at.add(u.scale(x)).add(w.scale(s*(i%2==0?.35:.18))),c,alpha,.025);}
        line(at.add(u.scale(-s*.4)),at.add(u.scale(s*.4)),c,alpha,.025);
        line(at.add(w.scale(s*.28)).add(u.scale(-s*.35)),at.add(w.scale(s*.28)).add(u.scale(s*.25)),c,alpha,.025);
    }
    private void impact(Vec3 at,double radius,double age,int c,float a){
        arc(at.add(0,.10,0),Math.max(.15,radius),.16,0,Math.PI*2,c,a*.5F,.045);
        fragments(at.add(0,.25,0),age,18,c,a*.8F,Math.max(.2,radius*.65),true);
    }
    private void fragments(Vec3 at,double age,int count,int c,float alpha,double radius,boolean ballistic){
        for(int i=0;i<count;i++){
            double f=((age*.055+i*.6180339887)%1),theta=i*2.39996323;
            double y=ballistic?1.8*f*(1-f):.5*Math.sin(theta+age*.06);
            Vec3 p=at.add(Math.sin(theta)*radius*f,y,Math.cos(theta)*radius*f);
            crystal(p,new Vec3(Math.sin(theta),.5,Math.cos(theta)).normalize(),.035+.045*(1-f),.11*(1-f)+.025,c,alpha*(float)(1-f));
        }
    }
    private void bolt(Vec3 a,Vec3 b,double age,int seed,int c,float alpha){
        Vec3 last=a;for(int i=1;i<=5;i++){double t=i/5.;Vec3 p=a.lerp(b,t).add(i==5?0:Math.sin(seed*4+i*7+Math.floor(age/2))*.13,i==5?0:Math.cos(seed+i*3)*.13,0);line(last,p,c,alpha,.035);line(last,p,c,alpha*.16F,.12);last=p;}
    }
    private void flow(Vec3 start,Vec3 axis,double radius,double length,double phase,int c,float alpha,double width){
        Vec3 u=axis.cross(UP);if(u.lengthSqr()<.01)u=new Vec3(1,0,0);u=u.normalize();Vec3 w=axis.cross(u).normalize();
        for(int i=0;i<24;i++){
            double t=i/24.,q=(i+1)/24.,a=phase+t*Math.PI*2,b=phase+q*Math.PI*2;
            Vec3 x=start.add(axis.scale(t*length)).add(u.scale(Math.cos(a)*radius)).add(w.scale(Math.sin(a)*radius));
            Vec3 y=start.add(axis.scale(q*length)).add(u.scale(Math.cos(b)*radius)).add(w.scale(Math.sin(b)*radius));
            line(x,y,c,alpha*(float)Math.sin(t*Math.PI),width*(.25+Math.sin(t*Math.PI)));
        }
    }
    private void arc(Vec3 at,double r,double width,double start,double end,int c,float alpha,double rise){
        for(int i=0;i<32;i++){
            double a=start+(end-start)*i/32,b=start+(end-start)*(i+1)/32;
            Vec3 x=at.add(Math.sin(a)*r,Math.sin(a)*rise,Math.cos(a)*r),y=at.add(Math.sin(b)*r,Math.sin(b)*rise,Math.cos(b)*r);
            Vec3 ix=at.add(Math.sin(a)*(r-width),Math.sin(a)*rise,Math.cos(a)*(r-width)),iy=at.add(Math.sin(b)*(r-width),Math.sin(b)*rise,Math.cos(b)*(r-width));
            quad(x,y,iy,ix,c,alpha);line(x,y,0xFFF3D8,alpha*.65F,.018);
        }
    }
    private void crystal(Vec3 at,Vec3 axis,double radius,double half,int c,float alpha){
        Vec3 u=axis.cross(UP);if(u.lengthSqr()<.01)u=new Vec3(1,0,0);u=u.normalize();Vec3 w=axis.cross(u).normalize();
        Vec3 top=at.add(axis.scale(half)),bottom=at.subtract(axis.scale(half));
        for(int i=0;i<4;i++){double a=i*Math.PI/2,b=(i+1)*Math.PI/2;Vec3 p=at.add(u.scale(Math.cos(a)*radius)).add(w.scale(Math.sin(a)*radius)),q=at.add(u.scale(Math.cos(b)*radius)).add(w.scale(Math.sin(b)*radius));
            int shade=shade(c,.42+i*.16);quad(top,p,q,q,shade,alpha);quad(bottom,q,p,p,shade,alpha);}
    }
    private void line(Vec3 a,Vec3 b,int c,float alpha,double width){
        Vec3 side=b.subtract(a).cross(camera.subtract(a.add(b).scale(.5))).normalize().scale(width*.5);if(side.lengthSqr()<1e-10)return;
        quad(a.add(side),b.add(side),b.subtract(side),a.subtract(side),c,alpha);
    }
    private void quad(Vec3 a,Vec3 b,Vec3 c,Vec3 d,int color,float alpha){
        if(alpha<=.001)return;int r=(color>>16)&255,g=(color>>8)&255,bl=color&255,al=(int)(Math.min(1,alpha)*255);
        for(Vec3 p:new Vec3[]{a,b,c,d})v.vertex(m,(float)p.x,(float)p.y,(float)p.z).color(r,g,bl,al).endVertex();
    }
}
