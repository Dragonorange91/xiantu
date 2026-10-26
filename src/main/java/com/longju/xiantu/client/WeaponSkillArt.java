package com.longju.xiantu.client;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import java.util.*;

/** Oriented translucent surfaces made from the user's supplied combat texture sequences.
 * World-space geometry is depth tested; only impact flashes face the viewer.
 * Animation is evaluated from server-confirmed cast age, never advanced per render pass. */
final class WeaponSkillArt {
    private static final Vec3 UP=new Vec3(0,1,0);
    private static final Map<String,Integer> FRAMES=Map.of("crescent",11,"blade",4,"wind",7,"impact",8,"flame",11,"shock",4,"vortex",1,"spear",1,"spark",1);
    private static final Map<String,RenderType> TYPES=new HashMap<>();
    static { for(String id:FRAMES.keySet())TYPES.put(id,Types.make(id)); }

    static void draw(MultiBufferSource buffers,Matrix4f m,Vec3 camera,String shape,Vec3 o,Vec3 f,
                     Vec3 side,Vec3 up,double age,double reach,int color){
        double p=Math.max(0,Math.min(1,age/24)),travel=Math.min(1,age/9);
        float fade=(float)Math.min(1,Math.min((age+1)/2,(24-age)/9));
        if(shape.equals("charge"))fade=1;
        if(fade<=0)return;
        switch(shape){
            case "charge" -> {
                double a=age*.11;Vec3 s=side.scale(Math.cos(a)).add(up.scale(Math.sin(a)));
                Vec3 u=up.scale(Math.cos(a)).subtract(side.scale(Math.sin(a)));
                sprite(buffers,m,camera,"wind",(age%14)/14,o.add(f.scale(.65)),s,u,.85,.85,color,.55f);
                sprite(buffers,m,camera,"spark",0,o.add(f.scale(.65)),side,up,.27,.27,color,.75f);
            }
            case "spear","helix","sun" -> {
                Vec3 tip=o.add(f.scale(reach*travel));
                double length=2.3+reach*.28;
                // The two crossed lance planes retain a solid silhouette from side and above.
                for(int i=0;i<3;i++){
                    double a=i*Math.PI/3+age*.035;
                    Vec3 radial=side.scale(Math.cos(a)).add(up.scale(Math.sin(a)));
                    sprite(buffers,m,camera,"spear",0,tip.subtract(f.scale(length*.36)),radial,f,.55,length,color,fade*.8f);
                }
                sprite(buffers,m,camera,"blade",p,tip.subtract(f.scale(length*.65)),f,up,length*1.6,.55,color,fade*.6f);
                sprite(buffers,m,camera,"impact",p,tip,side,up,.7,.7,color,fade);
            }
            case "triple" -> {
                for(int i=-1;i<=1;i++){
                    double q=Math.max(0,Math.min(1,(age-Math.abs(i)*2)/10));
                    Vec3 pos=o.add(f.scale(reach*q)).add(side.scale(i*.7));
                    double a=i*.35;Vec3 s=side.scale(Math.cos(a)).add(up.scale(Math.sin(a)));
                    sprite(buffers,m,camera,"crescent",p,pos,s,up,1.6,2.4,color,fade*(i==0?1:.6f));
                }
            }
            case "fan","cleave" -> {
                boolean vertical=shape.equals("cleave");
                double a=-1.25+travel*2.5,r=reach*.65;
                Vec3 across=vertical?up:side;
                Vec3 tip=o.add(f.scale(Math.cos(a)*r)).add(across.scale(Math.sin(a)*r));
                sprite(buffers,m,camera,vertical?"flame":"crescent",p,o.add(f.scale(r*.5)),f,across,r*2,r*2,color,fade);
                sprite(buffers,m,camera,"spark",0,tip,side,up,.65,.65,color,fade);
            }
            case "lotus","cross","hexagon" -> {
                Vec3 at=o.add(f.scale(.65));double r=reach*(.7+.3*Math.min(1,age/4));
                // Curved shell: twelve tangent panels, no opaque screen-filling disk.
                for(int i=0;i<12;i++){
                    double a=i*Math.PI/6;Vec3 radial=side.scale(Math.cos(a)).add(up.scale(Math.sin(a)));
                    Vec3 tangent=up.scale(Math.cos(a)).subtract(side.scale(Math.sin(a)));
                    sprite(buffers,m,camera,"blade",p,at.add(radial.scale(r)),tangent,f,r*.65,.42,color,fade*.48f);
                }
                sprite(buffers,m,camera,"wind",p,at,side,up,r*2.15,r*2.15,color,fade*.75f);
            }
            case "vortex" -> {
                double a=age*.10,r=reach*(.4+.6*travel);
                Vec3 x=new Vec3(Math.cos(a),0,Math.sin(a)),z=new Vec3(-Math.sin(a),0,Math.cos(a));
                sprite(buffers,m,camera,"vortex",0,o.add(0,-.7,0),x,z,r*2,r*2,color,fade*.75f);
                for(int i=0;i<3;i++)sprite(buffers,m,camera,"wind",p,o.add(0,-.7+i*.45,0),x,z,r*(1.9-i*.3),r*(1.9-i*.3),color,fade*(.55f-i*.1f));
            }
            case "shock" -> {
                double r=reach*travel;Vec3 ground=o.add(0,-.95,0);
                sprite(buffers,m,camera,"shock",p,ground,new Vec3(1,0,0),new Vec3(0,0,1),r*2,r*2,color,fade);
                for(int i=0;i<8;i++){
                    double a=i*Math.PI/4;Vec3 radial=new Vec3(Math.cos(a),0,Math.sin(a));
                    sprite(buffers,m,camera,"flame",p,ground.add(radial.scale(r*.8)).add(0,.4,0),radial,UP,.9,1.4,color,fade*.75f);
                }
            }
            case "contact","impact" -> {
                Vec3 view=camera.subtract(o).normalize(),right=UP.cross(view).normalize();
                if(right.lengthSqr()<1e-6)right=side;Vec3 vertical=view.cross(right).normalize();
                double size=shape.equals("contact")?.7:1.7;
                sprite(buffers,m,camera,"impact",p,o,right,vertical,size,size,color,fade);
                sprite(buffers,m,camera,"shock",p,o,right,vertical,size*(.5+travel),size*(.5+travel),color,fade*.5f);
            }
            default -> { }
        }
    }
    private static void sprite(MultiBufferSource b,Matrix4f m,Vec3 cam,String id,double phase,
                               Vec3 center,Vec3 right,Vec3 up,double width,double height,int c,float alpha){
        int frame=Math.max(0,Math.min(FRAMES.get(id)-1,(int)(phase*FRAMES.get(id))));
        // Sample inside each atlas cell so linear filtering cannot bleed adjacent frames.
        float inset=.5f/1024,u=(frame%4)/4f+inset,v=(frame/4)/4f+inset,span=.25f-2*inset;
        Vec3 x=right.scale(width*.5),y=up.scale(height*.5),o=center.subtract(cam);
        var buffer=b.getBuffer(TYPES.get(id));
        vertex(buffer,m,o.subtract(x).subtract(y),u,v+span,c,alpha);
        vertex(buffer,m,o.add(x).subtract(y),u+span,v+span,c,alpha);
        vertex(buffer,m,o.add(x).add(y),u+span,v,c,alpha);
        vertex(buffer,m,o.subtract(x).add(y),u,v,c,alpha);
    }
    private static void vertex(VertexConsumer v,Matrix4f m,Vec3 p,float u,float w,int c,float a){
        v.vertex(m,(float)p.x,(float)p.y,(float)p.z).uv(u,w).color(((c>>16)&255)/255f,((c>>8)&255)/255f,(c&255)/255f,a).endVertex();
    }
    private static final class Types extends RenderType{
        static RenderType make(String id){return create("xiantu_weapon_art_"+id,DefaultVertexFormat.POSITION_TEX_COLOR,VertexFormat.Mode.QUADS,4096,false,true,
            CompositeState.builder().setShaderState(new ShaderStateShard(GameRenderer::getPositionTexColorShader))
                .setTextureState(new TextureStateShard(new ResourceLocation("xiantu","textures/vfx/weapon_art/"+id+".png"),true,false))
                .setTransparencyState(ADDITIVE_TRANSPARENCY).setDepthTestState(LEQUAL_DEPTH_TEST)
                .setWriteMaskState(COLOR_WRITE).setCullState(NO_CULL).setOutputState(PARTICLES_TARGET).createCompositeState(false));}
        private Types(){super("xiantu_weapon_art",DefaultVertexFormat.POSITION_TEX_COLOR,VertexFormat.Mode.QUADS,256,false,false,()->{},()->{});}
    }
    private WeaponSkillArt(){}
}
