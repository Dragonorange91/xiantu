package com.longju.xiantu.config;
import net.minecraftforge.common.ForgeConfigSpec;
public final class XiantuClientConfig {
 public enum BossStyle { ORNATE, VANILLA, DISABLED_CUSTOM }
 public static final ForgeConfigSpec SPEC;
 public static final ForgeConfigSpec.EnumValue<BossStyle> BOSS_STYLE;
 public static final ForgeConfigSpec.BooleanValue BOSS_NUMBERS,BOSS_ANIMATION;
 static {var b=new ForgeConfigSpec.Builder();b.push("bossHud");BOSS_STYLE=b.defineEnum("style",BossStyle.ORNATE);BOSS_NUMBERS=b.define("numbers",true);BOSS_ANIMATION=b.define("animation",true);b.pop();SPEC=b.build();}
 private XiantuClientConfig(){}
}
