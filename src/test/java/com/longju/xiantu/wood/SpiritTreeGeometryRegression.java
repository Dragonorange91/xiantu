package com.longju.xiantu.wood;

import java.nio.file.*;
import java.util.*;
import com.longju.xiantu.wood.SpiritTreeGeometry.Cell;

public final class SpiritTreeGeometryRegression {
    private static int checks;
    private static void check(boolean b,String message){checks++;if(!b)throw new AssertionError(message);}
    public static void main(String[] args) throws Exception {
        StringBuilder preview=new StringBuilder("[");
        for(int rank=1;rank<=8;rank++) {
            Set<Map<Cell,Boolean>> variants=new HashSet<>();
            for(int seed=0;seed<64;seed++) {
                Map<Cell,Boolean> m=SpiritTreeGeometry.layout(rank,seed);variants.add(m);
                check(m.equals(SpiritTreeGeometry.layout(rank,seed)),"Non-deterministic preflight");
                Set<Cell> logs=new HashSet<>();m.forEach((p,wood)->{if(wood)logs.add(p);});
                Set<Cell> seen=new HashSet<>();ArrayDeque<Cell> q=new ArrayDeque<>();q.add(new Cell(0,0,0));
                while(!q.isEmpty()){Cell p=q.removeFirst();if(!logs.contains(p)||!seen.add(p))continue;
                    for(int[] d:new int[][]{{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}})q.add(p.add(d[0],d[1],d[2]));}
                check(seen.equals(logs),"Disconnected wood rank "+rank+" seed "+seed);
                var distances=SpiritTreeGeometry.leafDistances(m);
                for(var e:m.entrySet()) {
                    Cell p=e.getKey();check(p.y()>=0&&p.y()<=13&&Math.abs(p.x())<=6&&Math.abs(p.z())<=6,"Tree exceeded mature bounds");
                    if(!e.getValue())check(distances.containsKey(p)&&distances.get(p)<=6,"Unsupported leaves");
                }
                int height=m.keySet().stream().mapToInt(Cell::y).max().orElseThrow()+1;
                check(height>=7&&height<=14,"Mature height");check(m.size()<1000,"Excessive tree volume");
                check(m.size()-logs.size()>35,"Overpruned canopy");
                if(seed<3) {
                    if(preview.length()>1)preview.append(',');
                    preview.append("{\"rank\":").append(rank).append(",\"seed\":").append(seed).append(",\"height\":").append(height).append(",\"blocks\":[");
                    boolean first=true;for(var e:m.entrySet()) {if(!first)preview.append(',');first=false;Cell p=e.getKey();
                        preview.append('[').append(p.x()).append(',').append(p.y()).append(',').append(p.z()).append(',').append(e.getValue()?1:0).append(']');}
                    preview.append("]}");
                }
            }
            check(variants.size()>=4,"Need positional variants for rank "+rank);
        }
        preview.append(']');Path out=Path.of("build/reports/spirit-tree-layouts.json");Files.createDirectories(out.getParent());Files.writeString(out,preview);
        System.out.println("Spirit tree geometry: "+checks+" passed; 512 layouts; exact preview data: "+out);
    }
}
