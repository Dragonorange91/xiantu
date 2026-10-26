"""22 hand-composed cinnabar glyphs, no shared skewed template.
Polyline order is the same immutable data used by client and server.
"""
from build_paths import review_font
from pathlib import Path
import json,re
from PIL import Image,ImageDraw,ImageFont
P=Path(__file__).resolve().parents[1]
# Each slash starts a new pen stroke, each comma a turning point. Coordinates percent.
G={
'fireball':'28 20,50 10,72 20/50 26,35 43,48 55,32 75,50 90,70 74,56 55,66 42,50 26/25 61,18 78,29 89,39 83',
'thunder':'22 15,78 15,70 31,30 31/60 26,39 48,64 48,39 77,57 77,43 91/19 40,28 47,20 55/77 39,69 47,79 56/29 89,70 89',
'sword_qi':'50 10,42 34,50 76,58 34,50 10/26 39,40 45,60 45,75 38/30 64,21 78,50 92,80 78,69 64',
'explosion':'27 12,37 28,23 35,39 43/72 12,63 28,79 35,62 43/50 27,37 53,50 72,65 53,50 27/22 58,36 64,24 84,42 78,50 93/79 58,66 64,78 84,59 78,50 93',
'diamond':'50 10,20 34,50 59,80 34,50 10/30 38,30 70,50 89,70 70,70 38/39 66,50 75,61 66',
'ward':'24 15,24 38,30 67,50 91,70 67,76 38,76 15/32 24,68 24,50 45,34 57,67 57/38 69,50 80,62 69',
'reflection':'20 17,36 17,36 35,22 45/80 17,64 17,64 35,78 45/50 21,50 78/21 59,39 48,39 73,22 83/79 59,61 48,61 73,78 83',
'lightbody':'23 24,44 12,39 40,21 53,40 50/77 24,56 12,61 40,79 53,60 50/50 38,41 63,58 69,39 86,65 84',
'speed':'20 14,66 14,42 33,75 33/27 44,65 44,35 63,70 63/49 55,37 76,24 86,44 83,69 91,80 82',
'invisibility':'25 19,39 10,50 19,61 10,75 19/26 38,41 31,50 38,59 31,74 38/32 52,44 45,54 54,42 64/68 52,57 67,68 79,56 87/26 78,40 89,50 81,64 92,77 83',
'concealment':'22 19,37 30,50 17,63 30,78 19/29 44,45 44,41 60,58 60,65 74/23 77,36 87,50 75,64 87,78 77',
'healing':'50 12,34 31,50 42,66 31,50 12/24 49,40 43,50 54,60 43,76 49/50 42,50 75,32 68,26 81,50 92,74 81,68 68,50 75',
'detox':'26 15,50 30,75 15/35 31,26 48,40 62,59 62,74 48,64 31/27 76,42 70,50 83,58 70,73 76,50 92',
'cleanse':'23 22,42 14,50 23,58 14,77 22/50 27,36 46,50 57,64 46,50 27/19 44,27 58,20 70/81 44,73 58,80 70/30 76,40 87,50 77,60 87,70 76',
'soul_seal':'28 13,72 13,65 29,35 29,28 13/50 20,50 70/23 44,36 36,36 64,23 57/77 44,64 36,64 64,77 57/27 78,39 71,50 87,61 71,73 78,50 94',
'spirit_lock':'34 29,34 16,50 10,66 16,66 29/25 33,75 33,75 69,25 69,25 33/50 44,44 50,50 56,56 50,50 44,50 64/33 76,43 86,50 79/67 76,57 86,50 79,50 94',
'demon_seal':'21 12,39 26,50 12,61 26,79 12/29 36,71 36,66 64,34 64,29 36/50 27,50 77/20 43,13 58,24 71,32 66/80 43,87 58,76 71,68 66/25 82,41 73,50 88/75 82,59 73,50 88,50 95',
'trap':'22 15,78 15,78 78,22 78,22 15/32 28,68 28,68 66,32 66,32 28/42 40,58 40,58 54,42 54,42 40/17 47,32 47,22 58/83 47,68 47,78 58',
'earth_escape':'24 16,50 28,76 16/50 20,50 63/23 43,50 39,77 43/32 57,21 75,38 69,50 91/68 57,79 75,62 69,50 91',
'blink':'23 15,42 27,25 40/77 15,58 27,75 40/35 40,50 32,65 40,50 55,35 40/22 61,37 53,31 74,47 69/78 61,63 53,69 74,53 69,50 92',
'long_return':'19 14,39 14,31 33,17 39/81 14,61 14,69 33,83 39/45 14,50 29,55 14/25 49,50 34,75 49,50 63,25 49/18 60,34 67,24 82,40 77/82 60,66 67,76 82,60 77/37 87,50 95,63 87',
'rebirth':'28 17,45 10,61 14,75 28,64 35/73 46,79 64,69 81,53 91,42 78/30 84,18 69,19 51,30 38,39 48/50 31,37 51,50 70,63 51,50 31/35 57,50 48,65 57'
}
out=P/'asset_sources/ruin-interiors-20261025';out.mkdir(parents=True,exist_ok=True)
parsed={k:[[[int(n)/100 for n in p.split()]for p in stroke.split(',')]for stroke in s.split('/')]for k,s in G.items()}
java='''package com.longju.xiantu.talisman;
import java.util.List;
/** Distinct authored glyph per talisman; client guide and authoritative tracing share it. */
public final class TalismanGlyphs {
 public static List<TalismanDrawing.Point> stroke(String id,int index){
  String encoded=switch(id){
'''
for k,s in G.items():java+=f'   case "{k}" -> "{s}";\n'
java+='''   default -> throw new IllegalArgumentException("Unknown talisman glyph: "+id);
  };
  var strokes=encoded.split("/");
  if(index<0||index>=strokes.length)throw new IllegalArgumentException("Invalid glyph stroke");
  return java.util.Arrays.stream(strokes[index].split(",")).map(p->{var xy=p.split(" ");return new TalismanDrawing.Point(Integer.parseInt(xy[0])/100.0,Integer.parseInt(xy[1])/100.0);}).toList();
 }
 private TalismanGlyphs(){}
}
'''
(P/'src/main/java/com/longju/xiantu/talisman/TalismanGlyphs.java').write_text(java,encoding='utf8')
p=P/'src/main/java/com/longju/xiantu/talisman/TalismanDrawing.java';s=p.read_text(encoding='utf8');start=s.index('  int variant=');end=s.index('\n public int stroke',start);s=s[:start]+'  return TalismanGlyphs.stroke(id,stroke);\n }'+s[end:];p.write_text(s,encoding='utf8')
catalog=(P/'src/main/java/com/longju/xiantu/talisman/TalismanCatalog.java').read_text(encoding='utf8');defs=re.findall(r'new Definition\("(.*?)","(.*?)",".*?",\d,(\d)',catalog)
im=Image.new('RGB',(1100,6*250),'#17383a');d=ImageDraw.Draw(im);font=review_font(18)
for i,(id,name,c)in enumerate(defs):
 strokes=parsed[id];assert len(strokes)==3+int(c)*2,(id,len(strokes))
 ox=i%4*275;oy=i//4*250;d.rectangle((ox+12,oy+32,ox+262,oy+244),fill='#f1e3b5');d.text((ox+16,oy+7),name,fill='#f1d9a3',font=font)
 for stroke in strokes:d.line([(ox+25+x*225,oy+35+y*200)for x,y in stroke],fill='#bf291d',width=3)
assert len({json.dumps(v)for v in parsed.values()})==22
im.save(out/'talisman-glyphs.png');(out/'talisman-glyphs.json').write_text(json.dumps(parsed,ensure_ascii=False,indent=2),encoding='utf8')
print('22 unique authored glyphs; stroke counts match catalog')
