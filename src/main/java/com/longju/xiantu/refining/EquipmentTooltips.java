package com.longju.xiantu.refining;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.Locale;

/** Explain server-owned equipment effects without inventing persistent potion buffs. */
@Mod.EventBusSubscriber(modid="xiantu")
public final class EquipmentTooltips {
    private static String pct(double n){return String.format(Locale.ROOT,"%.1f%%",n*100);}
    public static String ordinary(String theme,double k){return switch(theme){
        case "沧溟镇岳甲"->"生命上限 +"+pct(.06*k)+"；水抗 +"+pct(.04*k);
        case "归藏曜甲"->"近战反伤 "+pct(.05*k)+"（间隔1秒）；物理减伤 +"+pct(.02*k);
        case "青梧流云甲"->"移速 +"+pct(.05*k)+"；摔落减伤 "+pct(.20*k);
        case "芝华回生甲"->"治疗效果 +"+pct(.08*k)+"；脱战5秒后自然恢复 +"+pct(.04*k);
        case "太初镇脉甲"->"控制抗性 +"+pct(.08*k)+"；金抗 +"+pct(.04*k);
        case "厚载玄衡甲"->"生命上限 +"+pct(.05*k)+"；五行抗性各 +"+pct(.02*k);
        case "离照焚星甲"->"伤害倍率 +"+pct(.05*k)+"；火抗 +"+pct(.04*k);
        case "寒汐锁月甲"->"缓慢与控制时长 -"+pct(.08*k)+"；水抗 +"+pct(.04*k);
        case "霆渊逐光甲"->"法术前摇 -"+pct(.05*k)+"；移速 +"+pct(.03*k);
        case "坤元藏真甲"->"灵力上限 +"+pct(.08*k)+"；灵力自然恢复 +"+pct(.04*k);
        default->"";
    };}
    @SubscribeEvent public static void tooltip(ItemTooltipEvent e){
        var s=e.getItemStack();var r=RefinedEquipment.recipe(s);
        if(r==null)return;
        if(!(s.getItem() instanceof ArmorItem)){
            String line=switch(r.theme()){
                case "定海链"->"生命上限 +5%";case "避水链"->"生命上限 +4%，水下呼吸";
                case "玄灵链"->"灵力上限 +4%";case "仙灵链"->"生命上限 +8%，治疗效果 +6%";
                case "神灵链"->"生命上限 +10%，控制抗性 +6%；致命伤保留1血并免伤1秒，冷却180秒";
                case "道灵链"->"生命上限 +12%，五行抗性 +3%；濒危时短暂减伤";
                case "长生链"->"生命上限 +9%，自然恢复 +4%；脱战后周期回血";
                case "轮回链"->"生命上限 +10%";case "不朽链"->"生命上限 +14%；濒危时短暂减伤，限制穿透";
                case "玄枢戒"->"灵力上限 +5%";case "聚灵戒"->"灵力自然恢复 +5%";
                case "清心戒"->"控制抗性 +5%";case "养魂戒"->"心魔幻影伤害减免5%";
                case "悟道戒"->"修炼速度 +4%，法术经验 +4%";
                case "天枢戒"->"灵力上限 +7%，灵力自然恢复 +3%";
                case "仙枢戒"->"灵力上限 +10%，修炼速度 +5%";
                case "神枢戒"->"灵力上限 +12%，灵力自然恢复 +5%";
                case "道枢戒"->"灵力上限 +15%，修炼速度 +7%";
                default->"";
            };
            if(!line.isEmpty()){
                e.getToolTip().add(Component.literal("饰品基础被动："+line).withStyle(ChatFormatting.GREEN));
                e.getToolTip().add(Component.literal("数值按阶位、品质缩放；同名戒指仅较强者生效").withStyle(ChatFormatting.GRAY));
                e.getToolTip().add(Component.literal("放入对应饰品栏，且境界达标时生效").withStyle(ChatFormatting.AQUA));
            }return;
        }
        e.getToolTip().add(Component.literal("本件灵甲防护：减伤 "+pct(ArmorBalance.protection(r.tier(),RefinedEquipment.quality(s)))+"（穿戴且境界达标生效）").withStyle(ChatFormatting.AQUA));
        if(r.theme().equals("齐天")){
            double k=QitianQuality.scale(RefinedEquipment.quality(s));
            String piece=switch(r.id()){
                case "fengchi_crown"->"火焰、雷击环境减伤 "+pct(Math.min(.85,.5*k))+"";
                case "suozi_chestplate"->"单次受伤超过最大生命20%：霸体1秒，冷却30秒";
                case "liujin_leggings"->"击退与缓慢时长 -"+pct(.2*k);
                case "ousi_boots"->"移速 +"+pct(.25*k)+"；跳高 +"+pct(.2*k)+"；摔落减伤 "+pct(Math.min(.95,.8*k))+"；筋斗步安全向前位移";
                default->"";
            };
            e.getToolTip().add(Component.literal("齐天神通："+piece).withStyle(ChatFormatting.GREEN));
            double set=e.getEntity()!=null&&QitianSkills.pieces(e.getEntity())==4?QitianSkills.setScale(e.getEntity()):k;
            e.getToolTip().add(Component.literal("2件减伤 "+pct(.1*set)+"；3件七十二变（获得物种能力）").withStyle(ChatFormatting.GOLD));
            e.getToolTip().add(Component.literal("4件生命 +"+pct(.35*set)+"、近战 +"+pct(.30*set)+"；大圣法相短时强化伤害、攻击范围").withStyle(ChatFormatting.GOLD));
            e.getToolTip().add(Component.literal("套装按最弱部件品质计算，凡品至极品均可使用神通").withStyle(ChatFormatting.GRAY));
            if(e.getEntity()!=null)e.getToolTip().add(Component.literal("有效齐天套装："+QitianSkills.pieces(e.getEntity())+"/4件").withStyle(ChatFormatting.AQUA));
            return;
        }
        var equipped=e.getEntity()==null?null:EquipmentEffects.read(e.getEntity());
        boolean active=equipped!=null&&equipped.set().equals(r.theme());
        String line=ordinary(r.theme(),active?equipped.scale():WeaponPassives.scale(s));if(line.isEmpty())return;
        e.getToolTip().add(Component.literal("四件套："+r.theme()).withStyle(ChatFormatting.GOLD));
        e.getToolTip().add(Component.literal(line).withStyle(ChatFormatting.GREEN));
        e.getToolTip().add(Component.literal("同主题四件穿齐生效，按整套最弱部件折算").withStyle(ChatFormatting.GRAY));
        if(e.getEntity()!=null){
            e.getToolTip().add(Component.literal(active?"套装已激活":"套装未激活：需同主题四件且境界达标").withStyle(active?ChatFormatting.AQUA:ChatFormatting.GRAY));
        }
    }
    private EquipmentTooltips(){}
}
