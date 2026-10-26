from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
p=ROOT/'src/main/java/com/longju/xiantu/client/SpellScreen.java';s=p.read_text('utf8')
old='text(g,"伤害 "+String.format(java.util.Locale.ROOT,"%.1f",SpellClient.DATA.save().getCompound("Damages").getDouble(s.id()))+" · 熟练加成 "+level*10+"%",164,184,color);'
new='if(s.type().equals("mount"))text(g,"右键登剑 · 空格升 · 疾跑降",164,184,color);else '+old
assert old in s;s=s.replace(old,new)
s=s.replace('if(!selected.isEmpty()&&mx>=','if(!selected.isEmpty()&&!selected.equals("sword_flight")&&mx>=')
p.write_text(s,'utf8')
p=ROOT/'src/main/java/com/longju/xiantu/spell/SpellScrolls.java';s=p.read_text('utf8');anchor='  @Override public InteractionResultHolder<ItemStack> use'
assert anchor in s
s=s.replace(anchor,'  @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag){\n   if(spell.id().equals("sword_flight")){lines.add(Component.literal("筑基可学；在面前地面凝聚蓝色灵剑"));lines.add(Component.literal("右键登剑，自动第三人称站立飞行"));lines.add(Component.literal("方向键移动，随视线升降；空格上升，疾跑键下降，潜行下剑"));}\n  }\n'+anchor);p.write_text(s,'utf8')
print('Sword UI and scroll controls updated')
