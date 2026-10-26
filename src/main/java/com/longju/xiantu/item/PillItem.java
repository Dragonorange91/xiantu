package com.longju.xiantu.item;
import com.longju.xiantu.alchemy.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import java.util.List;
public final class PillItem extends Item {
    private final PillCatalog.Definition definition;
    public PillItem(PillCatalog.Definition definition){super(new Properties().stacksTo(64));this.definition=definition;}
    public PillCatalog.Definition definition(){return definition;}
    @Override public Component getName(ItemStack stack){return Component.literal("一二三四五六七八".charAt(definition.rank()-1)+"阶·"+PillCatalog.QUALITIES[PillCatalog.quality(stack)-1]+"·"+definition.name());}
    @Override public int getUseDuration(ItemStack stack){return 32;}
    @Override public UseAnim getUseAnimation(ItemStack stack){return UseAnim.EAT;}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(player instanceof ServerPlayer p){
            if(definition.breakthrough()){PillChoiceMenu.open(p,0);return InteractionResultHolder.success(stack);}
            if(definition.kind().equals("physique")){PillChoiceMenu.open(p,1);return InteractionResultHolder.success(stack);}
            if(definition.kind().equals("cross_dimension")){MedicineService.beginTeleport(p,hand);return InteractionResultHolder.success(stack);}
            String denial=MedicineService.denial(p,stack);
            if(!denial.isEmpty()){p.displayClientMessage(Component.literal(denial),true);return InteractionResultHolder.fail(stack);}
        }
        player.startUsingItem(hand);return InteractionResultHolder.consume(stack);
    }
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity entity){if(entity instanceof ServerPlayer p)MedicineService.consume(p,stack);return stack;}
    @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag){
        lines.add(Component.literal(definition.description()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal(MedicineService.effectText(definition,PillCatalog.quality(stack))).withStyle(ChatFormatting.AQUA));
        if(definition.cooldown()>0)lines.add(Component.literal("同组冷却 "+definition.cooldown()+" 秒").withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal(String.format(java.util.Locale.ROOT,"丹毒 +%.2f · 停药后可打坐调息排毒",MedicineService.doseToxicity(definition,PillCatalog.quality(stack)))).withStyle(ChatFormatting.DARK_PURPLE));
    }
}
