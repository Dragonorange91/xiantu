package com.longju.xiantu.qa;
import com.longju.xiantu.talisman.*;
import net.minecraft.commands.Commands;
import net.minecraft.world.item.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
/** Temporary held-item visual inspection, restores the exact original main-hand stack. */
@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class HeldItemVisualAudit {
 private static ItemStack original;private static java.util.UUID owner;private static int slot;
 @SubscribeEvent public static void commands(RegisterCommandsEvent e){e.getDispatcher().register(Commands.literal("xiantuheldaudit").requires(s->s.hasPermission(2)).then(Commands.argument("index",com.mojang.brigadier.arguments.IntegerArgumentType.integer(-1,10)).executes(c->{
  var p=c.getSource().getPlayerOrException();if(!p.server.getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().equals("Xiantu-QA-BeastFlat-20261017"))return 0;
  int index=com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c,"index");
  if(index<0){if(original!=null&&owner.equals(p.getUUID())){p.getInventory().setItem(slot,original);p.getInventory().selected=slot;original=null;owner=null;p.inventoryMenu.broadcastChanges();}return 1;}
  if(original==null){original=p.getMainHandItem().copy();owner=p.getUUID();slot=p.getInventory().selected;}
  if(!owner.equals(p.getUUID())||p.getInventory().selected!=slot)return 0;
  ItemStack stack;
  if(index<3)stack=TalismanItems.blueprint(TalismanTomes.ALL.get(index).id());
  else{String id=new String[]{"equipment_blueprint","refining_scrap","failed_pill","hanxing_iron_ingot","causality_stone","beast_hide","condensed_spirit_fire","herb_0007_seed"}[index-3];var item=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation("xiantu",id));if(item==null||item==Items.AIR)throw new IllegalStateException(id);stack=new ItemStack(item);}
  p.setItemInHand(InteractionHand.MAIN_HAND,stack);p.inventoryMenu.broadcastChanges();com.mojang.logging.LogUtils.getLogger().info("[Held item visual QA] {}",stack.getHoverName().getString());return 1;
 })));}
}
