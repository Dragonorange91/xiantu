package com.longju.xiantu.worldgen;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.*;
import com.longju.xiantu.worldgen.town.TownWorldgen;
import java.util.*;

public final class RuinStructure extends Structure {
    public static final Codec<RuinStructure> CODEC=RecordCodecBuilder.create(i->i.group(settingsCodec(i),Codec.STRING.fieldOf("blueprint").forGetter(r->r.blueprint)).apply(i,RuinStructure::new));
    private final String blueprint;
    public RuinStructure(StructureSettings settings,String blueprint){super(settings);this.blueprint=blueprint;}
    @Override public StructureType<?>type(){return TownWorldgen.RUIN.get();}
    @Override protected Optional<GenerationStub> findGenerationPoint(GenerationContext c){
        var server=net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();if(server==null)return Optional.empty();
        var spec=RuinSpecs.load(server,blueprint);var size=spec.getAsJsonArray("size");int w=size.get(0).getAsInt(),h=size.get(1).getAsInt(),d=size.get(2).getAsInt();String mode=spec.getAsJsonObject("placement").get("mode").getAsString();
        int ox=c.chunkPos().getMinBlockX()-w/2,oz=c.chunkPos().getMinBlockZ()-d/2;var gen=c.chunkGenerator();var heights=new ArrayList<Integer>();int submerged=0;
        for(int x=0;x<w;x+=16)for(int z=0;z<d;z+=16){int y=gen.getBaseHeight(ox+x,oz+z,Heightmap.Types.OCEAN_FLOOR_WG,c.heightAccessor(),c.randomState())-1;heights.add(y);if(y<gen.getSeaLevel()-2)submerged++;}
        Collections.sort(heights);int low=heights.get(0),high=heights.get(heights.size()-1),median=heights.get(heights.size()/2);
        boolean underwater=mode.equals("underwater");
        if(underwater){
            // The entire dome footprint must be in deep water; no riverbank or land variant.
            if(submerged!=heights.size()||high>gen.getSeaLevel()-(h-5)-4||high-low>12)return Optional.empty();
        }else if(!mode.equals("floating")&&!mode.equals("shore")&&submerged>0||mode.equals("shore")&&low<gen.getSeaLevel()-8)return Optional.empty();
        int relief=high-low;if(mode.equals("surface")&&relief>8||mode.equals("shore")&&relief>10||mode.equals("mountain")&&relief>28)return Optional.empty();
        int entryY=spec.getAsJsonArray("entry").get(1).getAsInt();
        int oy=switch(mode){case "underground"->Math.min(-12,low-h-8);case "underwater"->median-4;case "floating"->Math.min(c.heightAccessor().getMaxBuildHeight()-h-8,Math.max(blueprint.equals("ninefold_star_observatory")?240:blueprint.equals("cloudsea_palace")?180:150,high+32));default->median-entryY+1;};
        if(oy<c.heightAccessor().getMinBuildHeight()+8||oy+h>c.heightAccessor().getMaxBuildHeight()-8)return Optional.empty();
        // Underground rock coverage is sampled over the entire footprint, not just the centre.
        if(mode.equals("underground")&&low<oy+h+6)return Optional.empty();
        var entry=RuinSpecs.offset(new BlockPos(ox,oy,oz),spec.getAsJsonArray("entry"));
        if(!c.validBiome().test(c.biomeSource().getNoiseBiome(entry.getX()>>2,entry.getY()>>2,entry.getZ()>>2,c.randomState().sampler())))return Optional.empty();
        // Reject cliffs at the approach; the ten-block apron must remain walkable.
        if(!mode.equals("floating")&&!mode.equals("underground")&&!underwater){
            int exit=gen.getBaseHeight(entry.getX(),entry.getZ()+10,Heightmap.Types.OCEAN_FLOOR_WG,c.heightAccessor(),c.randomState())-1;
            if(Math.abs(exit-(entry.getY()-1))>8)return Optional.empty();
        }
        if(RuinSiteExclusions.blocked(c,blueprint))return Optional.empty();
        final int ground=gen.getBaseHeight(ox+spec.getAsJsonArray("entry").get(0).getAsInt(),oz+spec.getAsJsonArray("entry").get(2).getAsInt()+4,Heightmap.Types.OCEAN_FLOOR_WG,c.heightAccessor(),c.randomState())-1;var origin=new BlockPos(ox,oy,oz);int top=oy+h+8;
        return Optional.of(new GenerationStub(RuinSpecs.offset(origin,spec.getAsJsonArray("entry")),builder->{
            for(int x=-8;x<w+12;x+=16)for(int z=-8;z<d+12;z+=16)builder.addPiece(new RuinStructurePiece(blueprint,origin,x,z,Math.min(16,w+12-x),Math.min(16,d+12-z),oy-16,top,ground));
        }));
    }
}
