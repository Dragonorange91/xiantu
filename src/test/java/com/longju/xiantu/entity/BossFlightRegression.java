package com.longju.xiantu.entity;

import com.longju.xiantu.cultivation.RealmRegistry;
import com.longju.xiantu.spell.SwordFlightControls;
import com.longju.xiantu.client.HudLayout;

public final class BossFlightRegression {
 private static int checks;
 private static void check(boolean value,String reason){checks++;if(!value)throw new AssertionError(reason);}
 public static void main(String[] args){
  for(int realm=3;realm<=8;realm++)for(int stage=0;stage<4;stage++){
   double hp=BossBalance.health(realm,stage),attack=BossBalance.attack(realm,stage);
   check(hp==RealmRegistry.baseHealth(realm,stage)*45.,"boss template follows realm and minor stage");
   check(attack>=RealmRegistry.baseAttack(realm,stage)*1.8,"boss attack remains a threat");
   check(8*RealmRegistry.baseAttack(realm,0)*2.2*1.5<hp*.4,"eight fully boosted critical barehand strikes cannot trivialize same-realm boss");
   check(BossBalance.received((float)(hp*.01),hp)==(float)(hp*.01),"small boss hits keep damage");
   check(Math.abs(BossBalance.received((float)(hp*.2),hp)-hp*(.03+.17*.25))<.001,"one boss cap softens only excess to quarter");
  }
  check(BossBalance.health(5,3)==12375,"Lianxu peak boss has 12375 health, not legacy 896");
  check(HudLayout.bossWidth(3840,2088,600)>1100,"4K boss frame grows above old 560px cap");
  check(HudLayout.bossWidth(854,480,235)<=360,"compact screen keeps top-left clearance");
  int previous=0;for(int width:new int[]{1280,1920,2560,3840}){int actual=HudLayout.bossWidth(width,width*9/16,width/6);check(actual>previous,"boss frame grows with window");previous=actual;}
  var controls=new SwordFlightControls();
  check(!controls.tick(1,true,false,false,false).boost(),"one W press does not sprint");
  controls.tick(2,false,false,false,false);check(controls.tick(5,true,false,false,false).boost(),"double W starts sprint");
  check(controls.tick(20,true,false,false,false).boost(),"sprint holds while W held");
  check(!controls.tick(21,false,false,false,false).boost(),"releasing W resets double-tap sprint");
  check(controls.tick(22,false,false,true,false).boost(),"configured sprint key works independently");
  check(!controls.tick(23,false,true,false,false).dismount(),"single Shift does not dismount");
  check(!controls.tick(24,false,true,false,false).dismount(),"holding Shift does not count as double-tap");
  controls.tick(25,false,false,false,false);check(controls.tick(26,false,true,false,false).dismount(),"double Shift dismounts");
  controls.reset();controls.tick(30,false,true,false,false);controls.tick(31,false,false,false,false);check(!controls.tick(40,false,true,false,false).dismount(),"slow Shift presses do not dismount");
  controls.tick(41,true,true,true,true);check(!controls.tick(42,true,true,false,false).dismount(),"opening GUI resets input edges");
  System.out.println("Boss and sword flight regression PASS "+checks+" assertions");
 }
}
