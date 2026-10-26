package com.longju.xiantu.entity;

import com.longju.xiantu.cultivation.RealmRegistry;
import com.longju.xiantu.spell.SpellRules;

/** World/event boss template from outline 17.2; includes the actual minor stage. */
public final class BossBalance {
 public static double health(int realm,int stage){return RealmRegistry.baseHealth(realm,stage)*45.;}
 public static double attack(int realm,int stage){return RealmRegistry.baseAttack(realm,stage)*2.2;}
 public static float received(float damage,double maximum){return (float)SpellRules.softCap(damage,maximum*.03);}
 private BossBalance(){}
}
