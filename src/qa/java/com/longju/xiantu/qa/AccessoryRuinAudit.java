package com.longju.xiantu.qa;

import com.longju.xiantu.equipment.*;
import com.longju.xiantu.worldgen.*;
import com.longju.xiantu.registry.*;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class AccessoryRuinAudit {
    private static int checks;
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);checks++;}
    @SubscribeEvent public static void commands(RegisterCommandsEvent e){
        e.getDispatcher().register(Commands.literal("xiantuaccessoryaudit").requires(s->s.hasPermission(2)).executes(c->{
            var p=c.getSource().getPlayerOrException();
            if(!p.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA"))return 0;
            p.closeContainer();var persistent=p.getPersistentData().copy();var inv=p.getInventory().save(new net.minecraft.nbt.ListTag());
            checks=0;
            try {
                p.getInventory().clearContent();p.getPersistentData().remove(AccessoryInventory.KEY);
                Item ring=ForgeRegistries.ITEMS.getValue(new ResourceLocation("xiantu","preview_equipment_080"));
                Item necklace=ForgeRegistries.ITEMS.getValue(new ResourceLocation("xiantu","preview_equipment_071"));
                require(new ItemStack(ring).is(AccessoryMenu.RINGS),"ring registry tag");
                require(new ItemStack(necklace).is(AccessoryMenu.NECKLACES),"necklace registry tag");
                net.minecraft.world.inventory.AbstractContainerMenu menu=new AccessoryMenu(79,p.getInventory());
                require(menu.slots.size()==39,"3 accessories + 36 inventory slots");
                for(int i=0;i<3;i++){
                    require(menu.getSlot(i).getMaxStackSize()==1,"single accessory");
                    require(!menu.getSlot(i).mayPlace(new ItemStack(Items.DIAMOND)),"reject generic items");
                    require(menu.getSlot(i).mayPlace(new ItemStack(i==2?necklace:ring)),"matching slot");
                    require(!menu.getSlot(i).mayPlace(new ItemStack(i==2?ring:necklace)),"reject wrong accessory");
                }
                p.getInventory().setItem(9,new ItemStack(ring));menu.quickMoveStack(p,3);
                p.getInventory().setItem(10,new ItemStack(ring));menu.quickMoveStack(p,4);
                p.getInventory().setItem(11,new ItemStack(necklace));menu.quickMoveStack(p,5);
                for(int i=0;i<3;i++)require(menu.getSlot(i).hasItem(),"filled slot "+i);
                net.minecraft.world.SimpleContainer loaded=new AccessoryInventory(p);
                for(int i=0;i<3;i++)require(ItemStack.matches(loaded.getItem(i),menu.getSlot(i).getItem()),"persisted slot "+i);
                p.getInventory().setItem(12,new ItemStack(ring));require(menu.quickMoveStack(p,6).isEmpty(),"full rings reject extra");
                require(p.getInventory().getItem(12).getCount()==1,"extra ring retained");
                menu.quickMoveStack(p,0);require(!menu.getSlot(0).hasItem(),"unequip ring");
                require(((net.minecraft.world.SimpleContainer)new AccessoryInventory(p)).getItem(0).isEmpty(),"unequip saved immediately");
                int count=0;for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).is(ring))count+=p.getInventory().getItem(i).getCount();
                require(count==2,"no duplicate or lost ring on quick move");
                int[] tiers=new int[9];for(int roll=0;roll<100;roll++)tiers[RuinHerbProcessor.tier(roll)]++;
                for(int i=1;i<=8;i++)require(tiers[i]==RuinHerbProcessor.WEIGHTS[i-1],"weighted tier "+i);
                // Inspect container persistence without placing or modifying the world.
                var state=ModBlocks.RUIN_REWARD_CHEST.get().defaultBlockState();
                var chest=(ChestBlockEntity)((net.minecraft.world.level.block.EntityBlock)state.getBlock()).newBlockEntity(BlockPos.ZERO,state);
                require(chest.getContainerSize()==27,"reward coffer 27 slots");chest.setItem(0,new ItemStack(Items.GOLD_INGOT,3));
                var tag=chest.saveWithFullMetadata();
                var copy=(ChestBlockEntity)net.minecraft.world.level.block.entity.BlockEntity.loadStatic(BlockPos.ZERO,state,tag);
                require(copy!=null && copy.getItem(0).getCount()==3,"reward coffer native persistence");
                var f=net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get().resolve("xiantu-qa/accessory-ruin.json");java.nio.file.Files.createDirectories(f.getParent());
                java.nio.file.Files.writeString(f,"{\"passed\":true,\"checks\":"+checks+"}");
                int total=checks;c.getSource().sendSuccess(()->Component.literal("Accessories / ruin PASS "+total),false);return total;
            }catch(Throwable ex){
                org.slf4j.LoggerFactory.getLogger(AccessoryRuinAudit.class).error("Accessory/ruin QA failed after {} checks",checks,ex);
                c.getSource().sendFailure(Component.literal("Accessory/ruin QA failed: "+ex));return 0;
            }finally{
                p.getInventory().load(inv);p.getPersistentData().getAllKeys().stream().toList().forEach(p.getPersistentData()::remove);p.getPersistentData().merge(persistent);p.inventoryMenu.broadcastChanges();
            }
        }));
    }
}
