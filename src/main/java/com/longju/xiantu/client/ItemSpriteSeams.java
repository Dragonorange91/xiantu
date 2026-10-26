package com.longju.xiantu.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Keep generated sprite silhouettes aligned with their pixel-extruded side walls. */
@Mod.EventBusSubscriber(modid="xiantu", value=Dist.CLIENT, bus=Mod.EventBusSubscriber.Bus.MOD)
public final class ItemSpriteSeams {
    private static final Map<BakedModel,BakedModel> CACHE = new IdentityHashMap<>();

    @SubscribeEvent public static void baked(ModelEvent.ModifyBakingResult event) {
        CACHE.clear();
        int count=0;
        for (var entry:event.getModels().entrySet()) {
            if (!entry.getKey().getNamespace().equals("xiantu")) continue;
            BakedModel result=wrap(entry.getValue());
            if(result!=entry.getValue()){entry.setValue(result);count++;}
        }
        com.mojang.logging.LogUtils.getLogger().info("[Xiantu item seams] aligned {} generated item models",count);
    }

    private static BakedModel wrap(BakedModel model) {
        if(model instanceof Aligned) return model;
        BakedModel cached=CACHE.get(model); if(cached!=null) return cached;
        // Reject custom renderers, cubes, authored equipment and all other namespaces.
        if(model.isCustomRenderer()||model.isGui3d()){CACHE.put(model,model);return model;}
        var quads=model.getQuads(null,null,RandomSource.create(0));
        boolean generated=quads.stream().anyMatch(ItemSpriteSeams::fullSpriteFace);
        BakedModel result=generated?new Aligned(model,quads):model;
        CACHE.put(model,result);return result;
    }

    private static boolean fullSpriteFace(BakedQuad q) {
        var name=q.getSprite().contents().name();
        if(!name.getNamespace().equals("xiantu")||!name.getPath().startsWith("item/"))return false;
        if(q.getDirection()!=Direction.NORTH&&q.getDirection()!=Direction.SOUTH)return false;
        int[] data=q.getVertices();int stride=data.length/4;
        float minX=1,maxX=0,minY=1,maxY=0;
        for(int i=0;i<4;i++){
            float x=Float.intBitsToFloat(data[i*stride]),y=Float.intBitsToFloat(data[i*stride+1]),z=Float.intBitsToFloat(data[i*stride+2]);
            if(Math.abs(z-(q.getDirection()==Direction.NORTH?7.5f:8.5f)/16f)>0.000001f)return false;
            minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);
        }
        return minX==0&&minY==0&&maxX==1&&maxY==1;
    }

    private record Layer(TextureAtlasSprite sprite,int tint) {}

    private static List<BakedQuad> rebuild(List<BakedQuad> source) {
        Map<Layer,EnumMap<Direction,BakedQuad>> layers=new LinkedHashMap<>();
        for(BakedQuad q:source) if(fullSpriteFace(q)) {
            // Animated outlines can change per frame: retain their vanilla mesh.
            if(q.getSprite().contents().getUniqueFrames().count()!=1) continue;
            layers.computeIfAbsent(new Layer(q.getSprite(),q.getTintIndex()),k->new EnumMap<>(Direction.class));
        }
        for(BakedQuad q:source) {
            var templates=layers.get(new Layer(q.getSprite(),q.getTintIndex()));
            if(templates!=null) templates.putIfAbsent(q.getDirection(),q);
        }
        layers.entrySet().removeIf(e->e.getValue().size()!=6);
        List<BakedQuad> result=new ArrayList<>();
        for(BakedQuad q:source) {
            if(!layers.containsKey(new Layer(q.getSprite(),q.getTintIndex()))) result.add(q);
            else if(!fullSpriteFace(q)&&q.getDirection().getAxis()==Direction.Axis.Z) result.add(q);
        }
        for(var entry:layers.entrySet()) {
            var contents=entry.getKey().sprite().contents();
            int w=contents.width(),h=contents.height(),frame=contents.getUniqueFrames().findFirst().orElse(0);
            // Opaque row runs make the front silhouette geometric too. A full
            // alpha-tested image plane can lose a subpixel next to an edge wall
            // after perspective interpolation, even with matching vertex bounds.
            for(int y=0;y<h;y++) {
                int x=0;
                while(x<w) {
                    if(contents.isTransparent(frame,x,y)){x++;continue;}
                    int start=x++;while(x<w&&!contents.isTransparent(frame,x,y))x++;
                    result.add(front(entry.getValue().get(Direction.NORTH),start,x,y,w,h));
                    result.add(front(entry.getValue().get(Direction.SOUTH),start,x,y,w,h));
                }
            }
            for(int y=0;y<h;y++) for(int x=0;x<w;x++) {
                if(contents.isTransparent(frame,x,y)) continue;
                if(x==0||contents.isTransparent(frame,x-1,y)) result.add(edge(entry.getValue().get(Direction.WEST),x,y,w,h));
                if(x==w-1||contents.isTransparent(frame,x+1,y)) result.add(edge(entry.getValue().get(Direction.EAST),x,y,w,h));
                if(y==0||contents.isTransparent(frame,x,y-1)) result.add(edge(entry.getValue().get(Direction.UP),x,y,w,h));
                if(y==h-1||contents.isTransparent(frame,x,y+1)) result.add(edge(entry.getValue().get(Direction.DOWN),x,y,w,h));
            }
        }
        return List.copyOf(result);
    }

    private static BakedQuad front(BakedQuad template,int start,int end,int y,int w,int h) {
        float x0=(float)start/w,x1=(float)end/w,y0=1f-(float)(y+1)/h,y1=1f-(float)y/h;
        boolean north=template.getDirection()==Direction.NORTH;float z=(north?7.5f:8.5f)/16f;
        float[][] positions=north?new float[][]{{x1,y0,z},{x0,y0,z},{x0,y1,z},{x1,y1,z}}
            :new float[][]{{x0,y0,z},{x1,y0,z},{x1,y1,z},{x0,y1,z}};
        int[] data=template.getVertices().clone();int stride=data.length/4;
        for(int i=0;i<4;i++) {
            for(int a=0;a<3;a++)data[i*stride+a]=Float.floatToRawIntBits(positions[i][a]);
            // Tiny inward U bias stays inside an opaque run; V is the exact row
            // centre. RGB detail and true transparent holes are retained.
            double px=positions[i][0]==x0?start+.001:end-.001;
            data[i*stride+4]=Float.floatToRawIntBits(template.getSprite().getU(px*16/w));
            data[i*stride+5]=Float.floatToRawIntBits(template.getSprite().getV((y+.5)*16/h));
        }
        return new BakedQuad(data,template.getTintIndex(),template.getDirection(),template.getSprite(),template.isShade(),template.hasAmbientOcclusion());
    }

    private static BakedQuad edge(BakedQuad template,int x,int y,int w,int h) {
        float x0=(float)x/w,x1=(float)(x+1)/w,y0=1f-(float)(y+1)/h,y1=1f-(float)y/h;
        float z0=7.5f/16f,z1=8.5f/16f;
        float[][] positions=switch(template.getDirection()) {
            case EAST -> new float[][]{{x1,y0,z0},{x1,y1,z0},{x1,y1,z1},{x1,y0,z1}};
            case WEST -> new float[][]{{x0,y0,z1},{x0,y1,z1},{x0,y1,z0},{x0,y0,z0}};
            case UP -> new float[][]{{x0,y1,z0},{x0,y1,z1},{x1,y1,z1},{x1,y1,z0}};
            case DOWN -> new float[][]{{x0,y0,z1},{x0,y0,z0},{x1,y0,z0},{x1,y0,z1}};
            default -> throw new IllegalArgumentException("Not a sprite edge");
        };
        int[] data=template.getVertices().clone();int stride=data.length/4;
        float u=template.getSprite().getU((x+.5)*16/w),v=template.getSprite().getV((y+.5)*16/h);
        // One exact opaque texel per exposed boundary. No merged span can cross
        // a transparent hole, and no side wall protrudes through the front face.
        for(int i=0;i<4;i++) {
            for(int a=0;a<3;a++) data[i*stride+a]=Float.floatToRawIntBits(positions[i][a]);
            data[i*stride+4]=Float.floatToRawIntBits(u);data[i*stride+5]=Float.floatToRawIntBits(v);
        }
        return new BakedQuad(data,template.getTintIndex(),template.getDirection(),template.getSprite(),template.isShade(),template.hasAmbientOcclusion());
    }

    private static final class Aligned implements BakedModel {
        private final BakedModel original;
        private final List<BakedQuad> quads;
        private final ItemOverrides overrides;
        Aligned(BakedModel original,List<BakedQuad> source){
            this.original=original;this.quads=rebuild(source);
            this.overrides=new ItemOverrides(){
                @Override public BakedModel resolve(BakedModel model,ItemStack stack,ClientLevel level,LivingEntity owner,int seed){
                    BakedModel resolved=original.getOverrides().resolve(original,stack,level,owner,seed);
                    return resolved==null?null:wrap(resolved);
                }
            };
        }
        public List<BakedQuad> getQuads(BlockState state,Direction side,RandomSource random){return side==null?quads:original.getQuads(state,side,random);}
        public boolean useAmbientOcclusion(){return original.useAmbientOcclusion();}
        public boolean isGui3d(){return original.isGui3d();}
        public boolean usesBlockLight(){return original.usesBlockLight();}
        public boolean isCustomRenderer(){return false;}
        public TextureAtlasSprite getParticleIcon(){return original.getParticleIcon();}
        public ItemTransforms getTransforms(){return original.getTransforms();}
        public ItemOverrides getOverrides(){return overrides;}
        public BakedModel applyTransform(ItemDisplayContext context,PoseStack pose,boolean left){original.applyTransform(context,pose,left);return this;}
    }
    private ItemSpriteSeams(){}
}
