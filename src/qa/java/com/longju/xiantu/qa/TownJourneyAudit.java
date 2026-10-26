package com.longju.xiantu.qa;

import com.longju.xiantu.trade.*;
import com.longju.xiantu.registry.*;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import java.util.*;

/** Isolated real-player transactions, always restoring inventory, introduction progress and advancement. */
@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class TownJourneyAudit {
    private static int checks;
    private static void check(boolean value,String message){if(!value)throw new IllegalStateException(message);checks++;}
    private static int stones(net.minecraft.world.entity.player.Player p){return p.getInventory().countItem(ModItems.GUIDING_STONE.get());}
    @SubscribeEvent public static void commands(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("xiantujourneyaudit").requires(s->s.hasPermission(2)).executes(c->{
            var p=c.getSource().getPlayerOrException();
            if(!p.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA"))return 0;
            p.closeContainer();var inv=p.getInventory().save(new ListTag());var persistent=p.getPersistentData().copy();
            var advancement=p.server.getAdvancements().getAdvancement(TownJourney.FIRST_STEP);
            check(advancement!=null,"First step advancement registered");
            List<String> awarded=new ArrayList<>();p.getAdvancements().getOrStartProgress(advancement).getCompletedCriteria().forEach(awarded::add);
            var oldMode=p.gameMode.getGameModeForPlayer();
            var guide=ModEntities.MERCHANTS.get("town_guide").get().create(p.serverLevel());
            var guide2=ModEntities.MERCHANTS.get("town_guide").get().create(p.serverLevel());
            checks=0;
            try{
                p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                ((net.minecraft.world.entity.Entity)guide).setPos(p.position());((net.minecraft.world.entity.Entity)guide2).setPos(p.position());
                for(String criterion:List.copyOf(awarded))p.getAdvancements().revoke(advancement,criterion);
                p.getInventory().clearContent();TownJourney.state(p).getAllKeys().stream().toList().forEach(TownJourney.state(p)::remove);
                // Avoid searching/generating terrain in the transaction audit; locate is a separate real-world test.
                TownJourney.state(p).putString("SearchState","unavailable");
                TownJourney.prepare(p);check(stones(p)==1,"Opening grants exactly one guiding stone");
                TownJourney.prepare(p);check(stones(p)==1,"Repeated login/prepare does not duplicate stone");
                check(TownJourney.state(p).getBoolean("StoneIssued"),"Issue persisted");
                check(TownTradeService.BOOKS.size()==5&&!TownTradeService.BOOKS.contains("xuan_yuan_yang_qi_jue"),"Wanbao removes entrance book");
                for(String key:TownTradeService.BOOKS)check(TownTradeService.OFFERS.stream().filter(o->o.shop().equals("wanbao_merchant")&&o.kind().equals("book")&&o.key().equals(key)&&o.rank()==3).count()==1,"All elemental methods cost tier3 stones");
                check(TownJourney.grantBook(p,guide),"First conversation grants book");
                var book=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation("xiantu","xuan_yuan_yang_qi_jue"));
                check(p.getInventory().countItem(book)==1,"Book delivered to real inventory");
                check(!TownJourney.grantBook(p,guide)&&!TownJourney.grantBook(p,guide2),"Repeated/sibling town guide cannot grant again");
                check(p.getInventory().countItem(book)==1,"No duplicate book");
                TownJourney.state(p).putBoolean("BookClaimed",false);for(int i=0;i<36;i++)p.getInventory().items.set(i,new ItemStack(Items.STONE,64));
                check(!TownJourney.grantBook(p,guide)&&!TownJourney.state(p).getBoolean("BookClaimed"),"Full inventory preserves claim");
                p.getInventory().items.set(0,ItemStack.EMPTY);check(TownJourney.grantBook(p,guide),"Retry after freeing slot succeeds");
                p.getInventory().items.set(1,new ItemStack(ModItems.GUIDING_STONE.get()));p.getInventory().offhand.set(0,new ItemStack(ModItems.GUIDING_STONE.get()));
                TownJourney.finish(p);check(stones(p)==0,"Arrival clears main and offhand stones");
                check(p.getAdvancements().getOrStartProgress(advancement).isDone(),"Arrival awards First step");
                TownJourney.prepare(p);check(stones(p)==0,"Completed journey never reissues stone");
                p.getInventory().items.set(1,new ItemStack(ModItems.GUIDING_STONE.get()));TownJourney.prepare(p);check(stones(p)==0,"Late picked stone removed after completion");
                var saved=TownJourney.state(p).copy();check(saved.getBoolean("Arrived")&&saved.getBoolean("BookClaimed"),"Arrival and claim persist independently of cultivation");
                ((net.minecraft.world.entity.Entity)guide).discard();TownJourney.state(p).putBoolean("BookClaimed",false);check(!TownJourney.grantBook(p,guide),"Removed guide cannot grant");
                var file=FMLPaths.GAMEDIR.get().resolve("xiantu-qa/town-journey.json");java.nio.file.Files.createDirectories(file.getParent());
                java.nio.file.Files.writeString(file,"{\"passed\":true,\"checks\":"+checks+",\"at\":\""+java.time.Instant.now()+"\"}");
                c.getSource().sendSuccess(()->Component.literal("Town journey PASS: "+checks+"; player restored"),false);return checks;
            }catch(java.io.IOException error){throw new java.io.UncheckedIOException(error);}finally{
                TownLocator.cancel(p.getUUID());
                p.getInventory().load(inv);p.getPersistentData().getAllKeys().stream().toList().forEach(p.getPersistentData()::remove);p.getPersistentData().merge(persistent);
                var current=new ArrayList<String>();p.getAdvancements().getOrStartProgress(advancement).getCompletedCriteria().forEach(current::add);
                for(String criterion:current)if(!awarded.contains(criterion))p.getAdvancements().revoke(advancement,criterion);
                for(String criterion:awarded)p.getAdvancements().award(advancement,criterion);
                p.setGameMode(oldMode);p.inventoryMenu.broadcastChanges();
                ((net.minecraft.world.entity.Entity)guide).discard();((net.minecraft.world.entity.Entity)guide2).discard();
            }
        }));
    }
}
