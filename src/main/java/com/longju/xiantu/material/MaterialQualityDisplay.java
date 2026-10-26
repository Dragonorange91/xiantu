package com.longju.xiantu.material;

import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** The same server-authored quality field is shown for every eligible material family. */
@Mod.EventBusSubscriber(modid="xiantu")
public final class MaterialQualityDisplay {
    @SubscribeEvent public static void tooltip(ItemTooltipEvent e){
        var s=e.getItemStack();if(!MaterialQuality.eligible(s))return;
        var lines=e.getToolTip();int quality=MaterialQuality.get(s);
        boolean wood=s.getItem() instanceof com.longju.xiantu.wood.WoodBlockItem||s.getItem() instanceof com.longju.xiantu.wood.WoodMaterialItem;
        if(!wood){
            if(quality>0&&!lines.isEmpty())lines.set(0,lines.get(0).copy().append("·"+MaterialQuality.NAMES[quality]));
            if(quality==0)lines.add(Component.literal("品质不明，需用鉴定灵台探明").withStyle(ChatFormatting.GRAY));
        }
        if(quality>0)lines.add(Component.literal("炼器品质转移加成 +"+MaterialQuality.manufacturingBonus(s)+" 个百分点（混料取最低）").withStyle(ChatFormatting.DARK_AQUA));
    }
    private MaterialQualityDisplay(){}
}
