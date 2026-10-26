from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def edit(name, pairs):
 p=ROOT/'src/main/java/com/longju/xiantu'/name
 s=p.read_text(encoding='utf-8')
 for old,new in pairs:
  if old not in s: raise AssertionError((name,old[:100]))
  s=s.replace(old,new)
 p.write_text(s,encoding='utf-8')
edit('item/ReincarnationStoneItem.java',[
 ('UUID instance = ensureInstance(stack);','UUID instance = ensureInstance(stack);\n        var ledger=ReincarnationLedger.get(serverPlayer.serverLevel());\n        if(ledger.spent(instance)){serverPlayer.displayClientMessage(Component.literal("这件轮回道具的记录已兑现，不能重复使用。"),true);return InteractionResultHolder.fail(stack);}'),
 ('restore(data, restoreSource, deathId,talisman);','if(!ledger.claim(instance,deathId,serverPlayer.getUUID())){serverPlayer.displayClientMessage(Component.literal("本次死亡记录已恢复，不能重复兑现。"),true);return;}\n                restore(data, restoreSource, deathId,talisman);'),
 ('if(talisman)pending.remove(pendingIndex);else removeDeath(pending, deathId);','removeDeath(pending, deathId);'),
 ('CompoundTag entry = new CompoundTag();','if(findPending(pending,snapshot.getUUID(INSTANCE_ID),owner)>=0)continue;\n            CompoundTag entry = new CompoundTag();')])
edit('talisman/TalismanTableEntity.java',[(
 'int count=TalismanCatalog.find(job.id()).strokes(),start=total()*15/100,span=total()*70/100;int windowStart=start+span*index/count,windowEnd=start+span*(index+1)/count;',
 'int count=TalismanCatalog.find(job.id()).strokes(),windowStart=TalismanControlRules.strokeStart(total(),count,index),windowEnd=TalismanControlRules.strokeEnd(total(),count,index);')])
edit('refining/QitianSkills.java',[
 ('||PENDING.containsKey(p.getUUID())||SpellService.casting(p)||p.isUsingItem()||p.containerMenu!=p.inventoryMenu','||com.longju.xiantu.talisman.TalismanEffects.casting(p)||PENDING.containsKey(p.getUUID())||SpellService.casting(p)||p.isUsingItem()||p.containerMenu!=p.inventoryMenu'),
 ('private static void pay(ServerPlayer p,String id,int cost,int cooldown){','private static void pay(ServerPlayer p,String id,int cost,int cooldown){com.longju.xiantu.talisman.TalismanEffects.breakInvisibility(p);'),
 ('@SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e)', '@SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p){QitianShapeAbilities.clear(p);state(p).remove("disguise_until");syncAppearance(p);}}\n    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e)')])
edit('refining/WeaponActives.java',[
 ('p.isUsingItem()||SpellService.casting(p)', 'p.isUsingItem()||com.longju.xiantu.talisman.TalismanEffects.casting(p)||SpellService.casting(p)')])
edit('spell/SpellService.java',[(
 'REQUESTS.put(p.getUUID(),now+4);GongfaService.cancel',
 'com.longju.xiantu.talisman.TalismanEffects.breakInvisibility(p);REQUESTS.put(p.getUUID(),now+4);GongfaService.cancel')])
edit('entity/BossCombatGoal.java',[
 ('public boolean weaponDelay(){','public boolean talismanInterrupt(){if(!weaponInterruptible())return false;stop();nextSpecial=mob.level().getGameTime()+40;return true;}\n    public boolean weaponDelay(){'),
 ('LivingEntity target=mob.getTarget();if(target==null)return;', 'if(com.longju.xiantu.talisman.TalismanEffects.locked(mob)){stop();return;}\n        LivingEntity target=mob.getTarget();if(target==null)return;')])
edit('entity/CompletionLandEntity.java',[(
 'public boolean weaponInterrupt(){',
 'public boolean talismanInterrupt(){return boss()?bossCombat!=null&&bossCombat.talismanInterrupt():weaponInterrupt();}\n    public boolean weaponInterrupt(){')])
edit('talisman/TalismanEffects.java',[
 ('public static boolean casting(ServerPlayer p){return CASTS.containsKey(p.getUUID());}',
  'public static boolean casting(ServerPlayer p){return CASTS.containsKey(p.getUUID());}\n public static void breakInvisibility(ServerPlayer p){state(p).remove("invisibility_until");}'),
 ('GongfaEvents.target(target).realmRank()-(GongfaService.data(p).majorRealm()+1)>=2',
  '!TalismanControlRules.allowed(GongfaEvents.target(target).realmRank(),tier,controlCount(target))'),
 ('if(CONTROL.contains(id)&&t!=null&&(controlCount(t)>=2||GongfaEvents.target(t).realmRank()-(GongfaService.data(p).majorRealm()+1)>=2))return;',
  'if(CONTROL.contains(id)&&t!=null&&(!TalismanControlRules.allowed(GongfaEvents.target(t).realmRank(),c.tier,controlCount(t))||GongfaEvents.target(t).boss()&&(!(t instanceof CompletionLandEntity boss)||!boss.weaponInterruptible())))return;'),
 ('if(t instanceof CompletionLandEntity boss)boss.weaponInterrupt();return;',
  'if(t instanceof CompletionLandEntity boss)boss.talismanInterrupt();return;'),
 ('double factor=(info.realmRank()>GongfaService.data(p).majorRealm()+1?.5:1)*(info.elite()?.5:1)*(count==1?.5:1);var n=state(t);n.putLong("control_window",now()+200);n.putInt("control_count",count+1);',
  'double factor=TalismanControlRules.duration(info.realmRank(),c.tier,info.elite(),count);if(factor<=0)return;var n=state(t);n.putInt("control_count",count+1);'),
 ('if(c.def.id().equals("demon_seal")&&t instanceof CompletionLandEntity beast)beast.weaponInterrupt();}',
  'if(t instanceof Mob mob)mob.getNavigation().stop();if(c.def.id().equals("demon_seal")&&t instanceof CompletionLandEntity beast)beast.weaponInterrupt();}\n  n.putLong("control_window",Math.max(n.getLong("soul_seal_until"),n.getLong("lock_until"))+200);'),
 ('Math.min(.25,(.12+.02*(c.tier-1))*q),120)',
  'Math.min(.25,(.12+.02*(c.tier-1))*q),121)'),
 ('e.getSource().getDirectEntity()!=attacker&&effect(attacker,"soul_seal")>0',
  '(e.getSource().getDirectEntity()!=attacker||e.getSource() instanceof ElementalDamageSource)&&effect(attacker,"soul_seal")>0')])
print('Patched reincarnation anti-replay, symbol-tier control, cast exclusivity and flight reconnect cleanup')
