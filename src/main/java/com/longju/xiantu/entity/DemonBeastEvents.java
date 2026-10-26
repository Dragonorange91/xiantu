package com.longju.xiantu.entity;

import com.longju.xiantu.XiantuMod;
import com.longju.xiantu.item.ArtifactItem;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = XiantuMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DemonBeastEvents {
    private DemonBeastEvents() {}
    /** Combat bonuses use vanilla attributes; this hook only wears protective artifacts. */
    @SubscribeEvent public static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide
                || event.getAmount() <= 0 || event.getSource().is(DamageTypeTags.BYPASSES_ARMOR)) return;
        wear(player, player.getMainHandItem(), EquipmentSlot.MAINHAND);
        wear(player, player.getOffhandItem(), EquipmentSlot.OFFHAND);
    }
    private static void wear(Player player, ItemStack stack, EquipmentSlot slot) {
        if (stack.getItem() instanceof ArtifactItem)
            stack.hurtAndBreak(1, player, entity -> entity.broadcastBreakEvent(slot));
    }
}
