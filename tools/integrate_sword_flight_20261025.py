from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]
def edit(file,old,new):
 p=ROOT/file;s=p.read_text('utf8');assert old in s,file;p.write_text(s.replace(old,new),'utf8')
edit('src/main/java/com/longju/xiantu/spell/SpellService.java','private static void leave(Player p){if(p instanceof ServerPlayer s){','private static void leave(Player p){if(p instanceof ServerPlayer s){SpiritSwordEntity.dismiss(s);')
edit('src/main/java/com/longju/xiantu/client/SpellIcons.java','textures/gui/spells/thunder_strike.png"));','textures/gui/spells/thunder_strike.png"),\n        "sword_flight",new ResourceLocation("xiantu","textures/gui/spells/sword_flight.png"));')
edit('src/main/java/com/longju/xiantu/client/XiantuClient.java','event.registerEntityRenderer(ModEntities.FORMATION_SEAT.get(), FormationSeatRenderer::new);','event.registerEntityRenderer(ModEntities.FORMATION_SEAT.get(), FormationSeatRenderer::new);\n        event.registerEntityRenderer(ModEntities.SPIRIT_SWORD.get(), SpiritSwordRenderer::new);')
edit('src/main/java/com/longju/xiantu/client/XiantuClient.java','public static void registerAdditionalModels(net.minecraftforge.client.event.ModelEvent.RegisterAdditional event) {','public static void registerAdditionalModels(net.minecraftforge.client.event.ModelEvent.RegisterAdditional event) {\n        event.register(new net.minecraft.resources.ResourceLocation("xiantu","item/spirit_flying_sword"));')
a=ROOT/'src/main/resources/assets/xiantu'
(a/'models/item/spirit_flying_sword.json').write_text(json.dumps({'parent':'xiantu:item/preview_qingshuang_sword_held'}),'utf8')
(a/'models/item/spell_scroll_sword_flight.json').write_text((a/'models/item/spell_scroll_fire_bolt.json').read_text('utf8'),'utf8')
for loc,name in [('zh_cn','御剑术卷轴'),('en_us','Sword Flight Scroll')]:
 p=a/f'lang/{loc}.json';d=json.loads(p.read_text('utf8'));d['item.xiantu.spell_scroll_sword_flight']=name;d['entity.xiantu.spirit_sword']='灵力飞剑' if loc=='zh_cn' else 'Spirit Flying Sword';p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n','utf8')
for file in ['src/qa/java/com/longju/xiantu/qa/SpellAudit.java','src/qa/java/com/longju/xiantu/qa/ManufacturingRevisionAudit.java']:
 p=ROOT/file;s=p.read_text('utf8').replace('SpellCatalog.ALL.size()==2','SpellCatalog.ALL.size()==3').replace('SpellScrolls.ITEMS.size()==2','SpellScrolls.ITEMS.size()==3').replace('Set.of("fire_bolt","thunder_strike")','Set.of("fire_bolt","thunder_strike","sword_flight")').replace('only fireball and thunder remain','fireball thunder and sword flight are registered').replace('only two independent spells/scrolls registered','three independent spells/scrolls registered')
 if file.endswith('/SpellAudit.java'):s=s.replace('d.spells().configure(d.spells().revision(),List.of("fire_bolt","thunder_strike","","","",""))','d.spells().configure(d.spells().revision(),List.of("fire_bolt","thunder_strike","sword_flight","","",""))')
 p.write_text(s,'utf8')
v=[list(map(float,line.split()[1:])) for line in (a/'models/item/qingshuang_sword.obj').read_text('utf8').splitlines() if line.startswith('v ')]
print('Accepted Blockbench mesh bounds',[[min(x[i] for x in v),max(x[i] for x in v)] for i in range(3)])
