package com.longju.xiantu.qa;

import com.longju.xiantu.trade.*;
import com.longju.xiantu.item.*;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** Uses registered live items and the production transaction, without changing the real player. */
@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class TownEconomyAudit {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new IllegalStateException(message);}
    private static ItemStack stone(int tier,int count){return new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("xiantu",SpiritStoneTier.values()[tier-1].registryName())),count);}
    private static List<ItemStack> empty(){var slots=new ArrayList<ItemStack>();for(int i=0;i<37;i++)slots.add(ItemStack.EMPTY);return slots;}
    private static int index(String id,String shop){for(int i=0;i<TownTradeService.OFFERS.size();i++){var o=TownTradeService.OFFERS.get(i);if(o.id().equals(id)&&o.shop().equals(shop))return i;}throw new IllegalStateException(id);}
    private static int count(List<ItemStack> slots,Item item){return slots.stream().filter(s->s.is(item)).mapToInt(ItemStack::getCount).sum();}
    @SubscribeEvent public static void commands(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("xiantueconomyaudit").executes(c->{
            if(!c.getSource().getServer().getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA"))return 0;
            checks=0;
            try{
                var ids=new HashSet<String>();int exchanges=0;
                for(var o:TownTradeService.OFFERS){
                    check(ids.add(o.shop()+"/"+o.id()),"unique offer "+o.id());
                    check(!o.stack().isEmpty()&&!o.output().isEmpty(),"registered output "+o.id());
                    check(o.amount()>=1&&o.amount()<=64&&o.price()>=1&&o.price()<=64,"bounded trade "+o.id());
                    if(o.kind().equals("facility")){
                        if(o.key().startsWith("array_")){
                            check(Set.of("array_eye_2","array_base_2","array_structure_2").contains(o.key())&&o.shop().equals("wanbao_merchant"),"only second formation plans sold at Wanbao");
                            check(o.rank()==3&&o.price()==6&&o.purchases()==20,"second formation plan price and relationship gate");
                        }else if(o.key().endsWith("_pickaxe")){
                            check(Set.of("jiuyou_pickaxe","hundun_pickaxe").contains(o.key())&&o.shop().equals("smith_merchant"),"advanced pick plans use smith");
                            check(o.rank()==(o.key().equals("jiuyou_pickaxe")?3:6)&&o.price()==8&&o.purchases()==20,"advanced pick price and relationship gate");
                        }else{
                            check(o.shop().equals(o.key().startsWith("alchemy_furnace_")?"alchemy_merchant":"smith_merchant"),"facility correct shop");
                            check(o.purchases()==0&&o.price()==TradeEconomyRules.facilityPrice(o.rank()),"facility immediate availability and price");
                        }
                    }
                    if(o.recycling())check(o.rank()>=4&&o.rank()<=8,"only middle/high ordinary materials recycled");
                    if(o.exchange()){
                        exchanges++;var slots=empty();slots.set(0,stone(o.rank(),4));
                        var result=TownTradeService.plan(slots,o.rank(),o.price(),o.output());
                        check(result!=null&&count(result,o.output().getItem())==1,"adjacent exchange gives one high stone");
                        check(((SpiritStoneItem)o.output().getItem()).tier().rank()==o.rank()+1,"no skip tiers");
                        check(slots.get(0).getCount()==4,"planner immutable");
                        slots.set(0,stone(o.rank(),3));check(TownTradeService.plan(slots,o.rank(),4,o.output())==null,"three insufficient");
                        var partial=stone(o.rank(),4);((SpiritStoneItem)partial.getItem()).setChargeMilli(partial,1);slots.set(0,partial);
                        check(TownTradeService.plan(slots,o.rank(),4,o.output())==null,"partially drained payment rejected");
                    }
                }
                check(exchanges==7,"all seven adjacent exchange pairs");
                for(String key:com.longju.xiantu.refining.FacilityRecipes.blueprints()){
                    long expected=key.startsWith("array_")&&key.endsWith("_3")?0:1;
                    check(TownTradeService.OFFERS.stream().filter(o->o.kind().equals("facility")&&o.key().equals(key)).count()==expected,"facility source separation: "+key);
                }
                var slots=empty();slots.set(0,stone(1,2));slots.set(36,stone(1,2));slots.set(1,stone(2,63));
                var merged=TownTradeService.plan(slots,1,4,stone(2,1));check(merged!=null&&merged.get(1).getCount()==64&&merged.get(36).isEmpty(),"offhand plus main payment, identical output merge");
                slots=empty();for(int i=0;i<36;i++)slots.set(i,new ItemStack(Items.STONE,64));slots.set(36,stone(1,4));
                check(TownTradeService.plan(slots,1,4,stone(2,1))==null&&slots.get(36).getCount()==4,"full inventory atomically rejects");
                var recycling=TownTradeService.OFFERS.stream().filter(o->o.kind().equals("recycle_beast")&&o.rank()==4).findFirst().orElseThrow();
                slots=empty();slots.set(0,BeastMaterialItem.setData(new ItemStack(recycling.stack().getItem(),4),3,""));
                check(TownTradeService.planExchange(slots,s->s.is(recycling.stack().getItem())&&BeastMaterialItem.grade(s)==4,4,recycling.output())==null,"low grade cannot satisfy higher beast salvage");
                var level=c.getSource().getLevel();var profile=new com.mojang.authlib.GameProfile(UUID.randomUUID(),"EconomyQA");var player=new FakePlayer(level,profile);
                var merchant=com.longju.xiantu.registry.ModEntities.MERCHANTS.get("talisman_merchant").get().create(level);
                var other=com.longju.xiantu.registry.ModEntities.MERCHANTS.get("talisman_merchant").get().create(level);
                var pos=c.getSource().getPosition();((Entity)player).setPos(pos);((Entity)merchant).setPos(pos);((Entity)other).setPos(pos);
                int rebirth=index("talisman/rebirth","talisman_merchant");var offer=TownTradeService.OFFERS.get(rebirth);player.getInventory().items.set(0,stone(3,64));
                check(TownTradeService.remaining(player,offer)==2,"fresh player gets two");
                TownTradeService.buy(player,merchant,rebirth);TownTradeService.buy(player,other,rebirth);
                check(TownTradeService.remaining(player,offer)==0,"two purchases across NPCs exhaust shared stock");
                int paid=player.getInventory().items.get(0).getCount();TownTradeService.buy(player,merchant,rebirth);
                check(player.getInventory().items.get(0).getCount()==paid&&paid==32,"third request no debit");
                var saved=player.getPersistentData().copy();var relog=new FakePlayer(level,profile);relog.getPersistentData().merge(saved);
                check(TownTradeService.remaining(relog,offer)==0,"serialized player stock survives relog");
                var clone=new FakePlayer(level,profile);TradeDailyLedger.clone(new net.minecraftforge.event.entity.player.PlayerEvent.Clone(clone,player,true));
                check(TownTradeService.remaining(clone,offer)==0,"death clone preserves stock");
                long time=level.getServer().overworld().getGameTime();var today=TradeDailyLedger.read(saved,time);
                check(today.getInt(offer.id())==2,"read-only ledger saved");
                check(TradeDailyLedger.read(saved,time+24000).getInt(offer.id())==0,"next game day resets");
                check(TradeDailyLedger.read(new CompoundTag(),time).getInt(offer.id())==0,"player quota independent");
                var wanbao=com.longju.xiantu.registry.ModEntities.MERCHANTS.get("wanbao_merchant").get().create(level);((Entity)wanbao).setPos(pos);
                player.getInventory().clearContent();int salvage=TownTradeService.OFFERS.indexOf(recycling);
                player.getInventory().items.set(0,BeastMaterialItem.setData(new ItemStack(recycling.stack().getItem(),4),3,""));
                TownTradeService.buy(player,wanbao,salvage);check(player.getInventory().items.get(0).getCount()==4,"production salvage rejects wrong grade");
                player.getInventory().items.set(0,recycling.stack());TownTradeService.buy(player,wanbao,salvage);
                check(count(player.getInventory().items,recycling.output().getItem())==1&&count(player.getInventory().items,recycling.stack().getItem())==0,"production salvage consumes material and pays stone");
                int exchange=index("exchange/1","wanbao_merchant");player.getInventory().clearContent();player.getInventory().items.set(0,stone(1,8));
                TownTradeService.buy(player,merchant,exchange);check(player.getInventory().items.get(0).getCount()==8,"wrong merchant rejects exchange");
                TownTradeService.buy(player,wanbao,exchange);check(count(player.getInventory().items,stone(1,1).getItem())==4&&count(player.getInventory().items,stone(2,1).getItem())==1,"production exchange consumes four and delivers one");
                ((Entity)wanbao).discard();
                ((Entity)merchant).discard();((Entity)other).discard();
                var path=level.getServer().getWorldPath(LevelResource.ROOT).resolve("town-economy-audit.txt");java.nio.file.Files.writeString(path,"PASS "+checks+" checks, "+TownTradeService.OFFERS.size()+" offers; "+java.time.Instant.now());
                c.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal("Town economy PASS "+checks),false);return 1;
            }catch(Exception|LinkageError ex){org.slf4j.LoggerFactory.getLogger(TownEconomyAudit.class).error("Town economy QA failed",ex);c.getSource().sendFailure(net.minecraft.network.chat.Component.literal("Town economy FAIL "+ex));return 0;}
        }));
    }
    private TownEconomyAudit(){}
}
