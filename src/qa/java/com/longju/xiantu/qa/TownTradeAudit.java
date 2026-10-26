package com.longju.xiantu.qa;

import com.longju.xiantu.trade.*;
import com.longju.xiantu.gongfa.*;
import com.longju.xiantu.registry.ModItems;
import com.longju.xiantu.item.SpiritStoneItem;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import java.util.*;

/** Read-only to the player's inventory/world: test payment plans against registered live items. */
@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class TownTradeAudit {
    private static int checks;
    private static void check(boolean ok,String message){if(!ok)throw new IllegalStateException(message);checks++;}
    private static List<ItemStack> inventory(){var slots=new ArrayList<ItemStack>();for(int i=0;i<37;i++)slots.add(ItemStack.EMPTY);return slots;}
    @SubscribeEvent public static void commands(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("xiantutradeaudit").requires(s->s.hasPermission(2)).executes(c->{
            if(!c.getSource().getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA"))return 0;
            checks=0;
            try {
                var slots=inventory();slots.set(0,new ItemStack(ModItems.LING_YU.get(),3));
                var plan=TownTradeService.plan(slots,3,TownTradeService.book(0));
                check(plan!=null&&plan.get(0).is(TownTradeService.book(0).getItem()),"Exact payment reuses freed slot");
                check(slots.get(0).getCount()==3,"Plan does not debit original inventory");
                slots.set(0,new ItemStack(ModItems.FAN_LING_SHI.get(),3));
                check(TownTradeService.plan(slots,3,TownTradeService.book(0))==null,"Lower tier cannot replace exact currency");
                slots.set(0,new ItemStack(ModItems.LING_YU.get(),3));
                check(TownTradeService.plan(slots,3,TownTradeService.book(1))!=null,"Tier three purchases elemental book");
                ItemStack partial=new ItemStack(ModItems.LING_YU.get(),3);
                ((SpiritStoneItem)partial.getItem()).setChargeMilli(partial,1);
                slots.set(0,partial);check(TownTradeService.plan(slots,3,TownTradeService.book(1))==null,"Partial stones rejected");
                slots=inventory();slots.set(0,new ItemStack(ModItems.LING_YU.get(),1));slots.set(36,new ItemStack(ModItems.LING_YU.get(),2));
                check(TownTradeService.plan(slots,3,TownTradeService.book(0))!=null,"Main inventory and offhand combine");
                slots=inventory();for(int i=0;i<36;i++)slots.set(i,new ItemStack(Items.STONE,64));slots.set(36,new ItemStack(ModItems.LING_YU.get(),3));
                check(TownTradeService.plan(slots,3,TownTradeService.book(0))==null,"Full inventory cannot drop purchased books or debit offhand");
                for(var method:GongfaRegistry.ALL){
                    if(method.tierIndex()==0)continue;
                    if(method.basicElemental()){
                        check(!method.learnableAt(0,7),"Qi eight blocked "+method.id());
                        check(method.learnableAt(0,8),"Qi nine bridge "+method.id());
                        check(method.learnableAt(1,0),"Foundation learns basic "+method.id());
                    }else if(!method.special())check(!method.learnableAt(0,8),"Other ordinary methods cannot learn in Qi "+method.id());
                }
                check(GongfaRegistry.get("tai_chu_dao_jing").learnableAt(0,0)&&GongfaRegistry.get("tai_chu_dao_jing").usableAt(8),"Special method Qi through highest realm");
                var data=new com.longju.xiantu.cultivation.PlayerCultivationData();data.setCultivationUnlocked(true);data.setRealmStage(0,8);
                check(!GongfaService.breakthroughDenial(data).isEmpty(),"Entrance alone cannot reach Foundation");
                data.gongfa().learn("jin_guang_jue");data.gongfa().commit("jin_guang_jue",UUID.randomUUID());
                check(GongfaService.breakthroughDenial(data).isEmpty(),"First basic method unlocks Foundation path");
                var fake=new net.minecraftforge.common.util.FakePlayer(c.getSource().getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"TownTradeQA"));
                var npc=com.longju.xiantu.registry.ModEntities.MERCHANTS.get("wanbao_merchant").get().create(c.getSource().getLevel());
                var at=c.getSource().getPosition();
                ((net.minecraft.world.entity.Entity)fake).setPos(at);
                ((net.minecraft.world.entity.Entity)npc).setPos(at);
                fake.getInventory().items.set(0,new ItemStack(ModItems.LING_YU.get(),3));
                int bookOffer=java.util.stream.IntStream.range(0,TownTradeService.OFFERS.size()).filter(i->TownTradeService.OFFERS.get(i).id().equals("book/jin_guang_jue")).findFirst().orElseThrow();
                TownTradeService.buy(fake,npc,bookOffer);
                check(fake.getInventory().items.get(0).is(TownTradeService.book(0).getItem()),"Actual transaction charges and delivers");
                check(npc.getPersistentData().getCompound("PlayerPurchases").getInt(fake.getUUID().toString())==1,"Per-NPC per-player progress");
                TownTradeService.buy(fake,npc,bookOffer);
                check(npc.getPersistentData().getCompound("PlayerPurchases").getInt(fake.getUUID().toString())==1,"Repeated unpaid request grants nothing");
                fake.getInventory().items.set(0,new ItemStack(ModItems.LING_YU.get(),3));
                ((net.minecraft.world.entity.Entity)fake).setPos(at.add(10,0,0));TownTradeService.buy(fake,npc,bookOffer);
                check(fake.getInventory().items.get(0).getCount()==3,"Distance invalidation does not debit");
                ((net.minecraft.world.entity.Entity)fake).setPos(at);
                ((net.minecraft.world.entity.Entity)npc).discard();TownTradeService.buy(fake,npc,bookOffer);
                check(fake.getInventory().items.get(0).getCount()==3,"Removed merchant cannot trade");
                var path=FMLPaths.GAMEDIR.get().resolve("xiantu-qa/town-trade.txt");java.nio.file.Files.createDirectories(path.getParent());
                java.nio.file.Files.writeString(path,"PASS "+checks+" checks; "+java.time.Instant.now());
                c.getSource().sendSuccess(()->Component.literal("Town trade audit PASS: "+checks),false);return checks;
            }catch(Exception error){c.getSource().sendFailure(Component.literal("Town trade audit FAIL: "+error));throw new RuntimeException(error);}
        }));
    }
}
