package com.longju.xiantu.talisman;
import java.util.*;
/** Exercises the real authoritative tracer against every distinct authored stroke. */
public final class TalismanGlyphRegression {
 public static void main(String[] args){
  var counts=Map.ofEntries(Map.entry("fireball",3),Map.entry("thunder",5),Map.entry("sword_qi",3),Map.entry("explosion",5),Map.entry("diamond",3),Map.entry("ward",3),Map.entry("reflection",5),Map.entry("lightbody",3),Map.entry("speed",3),Map.entry("invisibility",5),Map.entry("concealment",3),Map.entry("healing",3),Map.entry("detox",3),Map.entry("cleanse",5),Map.entry("soul_seal",5),Map.entry("spirit_lock",5),Map.entry("demon_seal",7),Map.entry("trap",5),Map.entry("earth_escape",5),Map.entry("blink",5),Map.entry("long_return",7),Map.entry("rebirth",5));
  var signatures=new HashSet<String>();int checks=0;
  for(var def:counts.entrySet()){
   var drawing=new TalismanDrawing();StringBuilder signature=new StringBuilder();long tick=0;
   for(int stroke=0;stroke<def.getValue();stroke++){
    var authored=TalismanDrawing.template(def.getKey(),stroke);signature.append(authored);var path=TalismanDrawing.resample(authored);
    for(int j=0;j<path.size();j++){
     var p=path.get(j);if(p.x()<.05||p.x()>.95||p.y()<.05||p.y()>.95)throw new AssertionError("Canvas edge: "+def.getKey());
     drawing.sample(def.getKey(),p.x(),p.y(),tick++,j==path.size()-1,12);checks++;
    }
    if(drawing.stroke!=stroke+1||drawing.mistakes!=0)throw new AssertionError("Cannot trace "+def.getKey()+" stroke "+stroke+" progress="+drawing.progress+" mistakes="+drawing.mistakes);
   }
   if(!signatures.add(signature.toString()))throw new AssertionError("Duplicate glyph "+def.getKey());
   if(drawing.precise!=def.getValue())throw new AssertionError("Exact route rejected: "+def.getKey());
  }
  var wrong=new TalismanDrawing();wrong.sample("fireball",.9,.9,0,false,12);
  if(wrong.started()||wrong.mistakes!=1)throw new AssertionError("Wrong start accepted");
  System.out.println("22 unique glyphs, "+checks+" authoritative trace samples PASS");
 }
}
