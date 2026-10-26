package com.longju.xiantu.qa;

import com.longju.xiantu.gongfa.*;
import com.longju.xiantu.cultivation.*;
import com.longju.xiantu.formation.FormationSeatEntity;
import com.longju.xiantu.registry.ModBlocks;
import com.longju.xiantu.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.UUID;

/** Server callback fixtures are distinct from physical client-held right-click evidence. */
public final class GongfaIntegration {
    private static int checks;
    private record Backup(CompoundTag data, CompoundTag persistent, ItemStack hand, float health,
                          int tick, Vec3 position, BlockPos cushion, BlockState block) {}
    private static final java.util.Map<UUID,Backup> GUI_BACKUPS = new java.util.HashMap<>();
    private static boolean qa(ServerPlayer player) { return player.getServer().getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA"); }
    private static void check(boolean value,String label) { if (!value) throw new IllegalStateException("[Gongfa QA FAIL] "+label); checks++; }
    private static ItemStack book(String id) { return new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("xiantu",id))); }
    private static void clearCombat(ServerPlayer player) { player.getPersistentData().remove("xiantu_method_combat_until"); }
    private static Backup save(ServerPlayer player) {
        CompoundTag data = new CompoundTag(); ((PlayerCultivationData)GongfaService.data(player)).save(data);
        BlockPos cushion = player.blockPosition().offset(4,0,4);
        return new Backup(data,player.getPersistentData().copy(),player.getMainHandItem().copy(),player.getHealth(),player.tickCount,
                player.position(),cushion,player.level().getBlockState(cushion));
    }
    private static void sit(ServerPlayer player,BlockPos cushion) {
        player.stopRiding(); player.level().setBlock(cushion,ModBlocks.MEDITATION_CUSHION.get().defaultBlockState(),3);
        FormationSeatEntity seat = ModEntities.FORMATION_SEAT.get().create(player.level());
        seat.setCushionPos(cushion); ((Entity)seat).setPos(cushion.getX()+.5,cushion.getY()+.05,cushion.getZ()+.5);
        player.level().addFreshEntity(seat); ((Entity)player).setPos(cushion.getX()+.5,cushion.getY()+.05,cushion.getZ()+.5);
        player.startRiding(seat,true);
    }
    private static void restore(ServerPlayer player,Backup saved) {
        GongfaService.cancel(player,""); player.closeContainer(); Entity seat = player.getVehicle(); player.stopRiding();
        if (seat instanceof FormationSeatEntity) seat.discard();
        player.level().setBlock(saved.cushion,saved.block,3);
        ((PlayerCultivationData)GongfaService.data(player)).load(saved.data);
        for (String key : java.util.List.copyOf(player.getPersistentData().getAllKeys())) player.getPersistentData().remove(key);
        player.getPersistentData().merge(saved.persistent); player.setItemInHand(InteractionHand.MAIN_HAND,saved.hand);
        ((Entity)player).tickCount = saved.tick; ((Entity)player).setPos(saved.position);
        GongfaService.refresh(player); player.setHealth(Math.min(saved.health,player.getMaxHealth())); CultivationEvents.sync(player);
    }
    private static boolean confirmBook(ServerPlayer player,String id) {
        player.setItemInHand(InteractionHand.MAIN_HAND,book(id));
        player.getMainHandItem().getItem().use(player.level(),player,InteractionHand.MAIN_HAND);
        return player.containerMenu instanceof GongfaMenu menu && ((AbstractContainerMenu)menu).clickMenuButton(player,0);
    }
    private static boolean start(ServerPlayer player,String id) {
        clearCombat(player);
        if (!confirmBook(player,id)) return false;
        player.getMainHandItem().getItem().use(player.level(),player,InteractionHand.MAIN_HAND);
        return GongfaService.remainingTicks(player) > 0;
    }
    private static void advance(ServerPlayer player,int ticks) {
        ItemStack stack = player.getMainHandItem();
        for (int i=0;i<ticks;i++) {
            ((Entity)player).tickCount++;
            stack.getItem().onUseTick(player.level(),player,stack,200-i);
        }
    }
    private static void finish(ServerPlayer player) {
        advance(player,200);
        ItemStack stack = player.getMainHandItem(); stack.getItem().finishUsingItem(stack,player.level(),player); player.stopUsingItem();
    }
    public static int run(ServerPlayer player) {
        if (!qa(player)) return 0;
        Backup saved = save(player); checks = 0;
        var data = (PlayerCultivationData)GongfaService.data(player);
        try {
            data.setPhysique(PhysiqueType.ORDINARY);
            player.stopRiding(); GongfaService.cancel(player,""); data.setCharacterInitialized(true);
            data.gongfa().clear(); data.setCultivationUnlocked(false); data.setRealmStage(0,0); data.setProgress(0);
            GongfaService.refresh(player); clearCombat(player);
            check(!GongfaService.canMeditate(data) && data.powerMax()==0,"Uninitiated player cannot cultivate");
            check(!confirmBook(player,"xuan_shui_jue"),"Main book cannot skip entrance");
            check(confirmBook(player,"xuan_yuan_yang_qi_jue"),"Held entrance book opens confirm menu");
            player.getMainHandItem().getItem().use(player.level(),player,InteractionHand.MAIN_HAND);
            check(!player.isUsingItem() && GongfaService.remainingTicks(player)==0 && !data.cultivationUnlocked(),"Standing confirmation cannot learn");
            sit(player,saved.cushion);
            player.getMainHandItem().getItem().use(player.level(),player,InteractionHand.MAIN_HAND);
            check(GongfaService.remainingTicks(player)==200,"Seated approved use starts");
            advance(player,199);
            check(!data.cultivationUnlocked(),"199 callbacks do not grant qualification");
            player.getMainHandItem().getItem().finishUsingItem(player.getMainHandItem(),player.level(),player);
            check(!data.cultivationUnlocked() && player.getMainHandItem().getCount()==1,"Early finish rejected without consuming book");
            check(start(player,"xuan_yuan_yang_qi_jue"),"Entrance can retry after interrupted attempt"); finish(player);
            check(data.cultivationUnlocked() && data.gongfa().active().isEmpty() && player.getMainHandItem().getCount()==1,"Entrance success preserves reusable book and leaves main slot empty");
            check(!confirmBook(player,"xuan_yuan_yang_qi_jue"),"Repeated entrance cannot reset realm");
            data.setRealmStage(0,8); data.setProgress(123); clearCombat(player);
            player.setItemInHand(InteractionHand.MAIN_HAND,book("xuan_shui_jue"));
            player.getMainHandItem().getItem().use(player.level(),player,InteractionHand.MAIN_HAND);
            check(player.containerMenu instanceof GongfaMenu && !data.gongfa().knows("xuan_shui_jue"),"Right-click alone neither learns nor equips");
            AbstractContainerMenu menu = player.containerMenu;
            check(!menu.clickMenuButton(player,1000),"Old equip packet rejected");
            check(menu.clickMenuButton(player,1) && !data.gongfa().knows("xuan_shui_jue"),"Decline preserves realm and book");
            check(start(player,"xuan_shui_jue"),"First seated main study begins"); finish(player);
            check(data.gongfa().active().equals("xuan_shui_jue") && data.stage()==8 && data.progress()==123,"Completion learns and equips without first-use rollback");
            check(player.getMainHandItem().getCount()==1,"Main book not consumed");
            check(!confirmBook(player,"xuan_shui_jue"),"Current method cannot be relearned to clear layers");
            data.setRealmStage(0,0); data.setPowerMax(100); data.setPower(60); data.setPowerSpeed(1);
            for (int i=0;i<1200;i++) GongfaService.meditationExperience(player);
            check(data.gongfa().activeLevel()==1,"Sixty effective seconds raises one layer");
            GongfaService.addExperience(player,2610); GongfaService.refresh(player); GongfaService.refresh(player);
            check(data.gongfa().activeLevel()==9 && data.powerMax()==110 && Math.abs(data.powerSpeed()-1.02)<.001 && data.power()==60,"Percentage effects do not stack or fill power");
            var methodHealth = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
                    .getModifier(UUID.fromString("cc03473a-885b-49ef-9d51-20d70e991801"));
            check(methodHealth != null && methodHealth.getOperation()==net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION
                            && methodHealth.getAmount()==1,
                    "Active method health bonus is calculated from realm health and rounded to whole points");
            var methodMovement = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)
                    .getModifier(UUID.fromString("cc03473a-885b-49ef-9d51-20d70e991803"));
            check(methodMovement != null && Math.abs(methodMovement.getAmount()-.03)<.000001,
                    "Nine-layer water method applies its movement passive");
            GongfaService.refresh(player); GongfaService.refresh(player);
            check(player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).getModifiers().stream()
                            .filter(m -> m.getId().equals(UUID.fromString("cc03473a-885b-49ef-9d51-20d70e991803"))).count()==1,
                    "Repeated refresh does not stack passive attribute modifiers");
            data.setRealmStage(2,3); data.setProgress(50); clearCombat(player);
            check(start(player,"tai_qing_hun_yuan_jing"),"Replacement confirmation does not require previous learning record");
            advance(player,80); player.getMainHandItem().getItem().releaseUsing(player.getMainHandItem(),player.level(),player,120);
            check(data.majorRealm()==2 && data.gongfa().activeLevel()==9 && !data.gongfa().knows("tai_qing_hun_yuan_jing"),"Release cancels with no learning or rollback");
            check(start(player,"tai_qing_hun_yuan_jing"),"Retry confirmation begins fresh hold");
            for (int i=0;i<20;i++) GongfaService.useTick(player);
            check(GongfaService.remainingTicks(player)==199,"Repeated same-tick callbacks cannot advance time");
            player.stopRiding(); GongfaService.tick(player);
            check(GongfaService.remainingTicks(player)==0 && data.majorRealm()==2,"Leaving cushion cancels");
            sit(player,saved.cushion); check(start(player,"tai_qing_hun_yuan_jing"),"New seated attempt starts");
            player.setItemInHand(InteractionHand.MAIN_HAND,book("jin_guang_jue")); GongfaService.tick(player);
            check(GongfaService.remainingTicks(player)==0 && data.majorRealm()==2,"Swapping book cancels");
            check(start(player,"tai_qing_hun_yuan_jing"),"Interrupted tick sequence fixture begins");
            advance(player,1); ((Entity)player).tickCount+=2;
            check(!GongfaService.useTick(player) && data.majorRealm()==2,"Missing use ticks cannot be treated as a continuous hold");
            check(start(player,"tai_qing_hun_yuan_jing"),"Cushion removal fixture begins");
            player.level().setBlock(saved.cushion,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3); GongfaService.tick(player);
            check(GongfaService.remainingTicks(player)==0 && data.gongfa().activeLevel()==9,"Removing the physical cushion cancels without clearing layers");
            sit(player,saved.cushion);
            check(start(player,"tai_qing_hun_yuan_jing"),"Damage interruption fixture begins");
            MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.living.LivingDamageEvent(player,player.damageSources().magic(),1));
            check(GongfaService.remainingTicks(player)==0 && data.majorRealm()==2,"Damage event cancels without rollback");
            check(!GongfaService.denial(player,"tai_qing_hun_yuan_jing").isEmpty(),"Combat blocks new confirmation");
            check(start(player,"tai_qing_hun_yuan_jing"),"Valid replacement starts"); finish(player);
            check(data.majorRealm()==1 && data.stage()==3 && data.progress()==0 && data.gongfa().activeLevel()==0,"Completed replacement retreats and clears progress");
            check(data.gongfa().progress("xuan_shui_jue").level()==0 && player.getMainHandItem().getCount()==1,"Old and new layers clear while book survives");
            player.getMainHandItem().getItem().finishUsingItem(player.getMainHandItem(),player.level(),player);
            check(data.majorRealm()==1,"Duplicate completion cannot charge rollback twice");
            check(start(player,"xuan_shui_jue"),"Held old book can be reused for returning to old method"); finish(player);
            check(data.majorRealm()==0 && data.stage()==8 && data.gongfa().activeLevel()==0,"Returning also pays rollback and starts at zero");
            data.setRealmStage(2,3); data.setProgress(data.progressMax());
            check(!BreakthroughService.tryBreakthrough(player) && data.progress()==data.progressMax(),"Method cap denies breakthrough without progress debit");
            data.setRealmStage(0,0); clearCombat(player);
            Zombie zombie = new Zombie(EntityType.ZOMBIE,player.serverLevel()); ((Entity)zombie).setPos(player.position().add(8,0,0));
            zombie.getPersistentData().putInt(com.longju.xiantu.entity.BeastResourceDrops.REALM,0);
            zombie.getPersistentData().putInt(com.longju.xiantu.entity.BeastRealmService.STAGE,0);
            zombie.getPersistentData().putBoolean(com.longju.xiantu.entity.BeastRealmService.INITIALIZED,true);
            try {
                player.serverLevel().addFreshEntity(zombie); ((LivingEntity)zombie).setHealth(1);
                ((LivingEntity)zombie).hurt(player.damageSources().playerAttack(player),100);
                check(data.gongfa().activeProgress().experience()==20,"Actual hostile death awards XP");
                MinecraftForge.EVENT_BUS.post(new LivingDeathEvent(zombie,player.damageSources().playerAttack(player)));
                check(data.gongfa().activeProgress().experience()==20,"Duplicate hostile death cannot grant XP twice");
            } finally { ((Entity)zombie).discard(); }
            Cow cow = new Cow(EntityType.COW,player.serverLevel()); MinecraftForge.EVENT_BUS.post(new LivingDeathEvent(cow,player.damageSources().playerAttack(player)));
            check(data.gongfa().activeProgress().experience()==20,"Peaceful animal gives no XP");
            var old = new net.minecraftforge.common.util.FakePlayer(player.serverLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"MethodOldQA"));
            var clone = new net.minecraftforge.common.util.FakePlayer(player.serverLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"MethodNewQA"));
            var oldData = (PlayerCultivationData)GongfaService.data(old);
            oldData.copyFrom(data); oldData.setRealmStage(5,2); oldData.setProgress(77);
            oldData.setPowerMax(900); oldData.setPowerSpeed(12); oldData.setPower(450);
            oldData.setMerits(17); oldData.setComprehension(9); oldData.setInnerDemon(33);
            oldData.resetSpiritRoots(); oldData.addSpiritRoot(SpiritRootType.FIRE);
            oldData.gongfa().learn("tai_qing_hun_yuan_jing");
            oldData.gongfa().commit("tai_qing_hun_yuan_jing",UUID.randomUUID());
            old.getPersistentData().putString("xiantu_active_gongfa","tai_qing_hun_yuan_jing");
            old.getPersistentData().putDouble("xiantu_gongfa_power_max_bonus",80);
            old.getPersistentData().putDouble("xiantu_gongfa_power_speed_bonus",2);
            old.getPersistentData().putDouble("xiantu_physique_power_max_bonus",30);
            CultivationEvents.clone(new PlayerEvent.Clone(clone,old,true));
            var reborn = GongfaService.data(clone);
            check(reborn.cultivationUnlocked() && reborn.majorRealm()==0 && reborn.stage()==0
                    && reborn.progress()==0,"Death resets the qualified character to Qi Refining layer one");
            check(reborn.power()==0 && reborn.powerMax()==RealmRegistry.get(0,0).powerCap()
                    && reborn.powerSpeed()==CultivationConfig.BASE_POWER_SPEED,"Death recomputes base spiritual power");
            check(reborn.gongfa().active().isEmpty() && reborn.gongfa().save().getCompound("learned").isEmpty(),
                    "Death clears all learned methods and layers");
            check(reborn.spiritRootCount()>=1 && reborn.spiritRootCount()<=SpiritRootType.values().length,
                    "Death redraws a valid initial spirit-root set");
            check(reborn.merits()==17 && reborn.comprehension()==9 && reborn.innerDemon()==33,
                    "Death retains merit, comprehension and inner-demon growth");
            var cachedRoots=java.util.Arrays.stream(SpiritRootType.values()).filter(reborn::hasSpiritRoot)
                    .mapToInt(root->1<<root.ordinal()).reduce(0,(a,b)->a|b);
            CultivationEvents.clone(new PlayerEvent.Clone(clone,old,true));
            var repeatedRoots=java.util.Arrays.stream(SpiritRootType.values()).filter(GongfaService.data(clone)::hasSpiritRoot)
                    .mapToInt(root->1<<root.ordinal()).reduce(0,(a,b)->a|b);
            check(repeatedRoots==cachedRoots,"Repeated death Clone reuses the same spirit-root draw");
            var deathTag=clone.getPersistentData();
            check(!deathTag.contains("xiantu_active_gongfa") && deathTag.getDouble("xiantu_gongfa_power_max_bonus")==0
                    && deathTag.getDouble("xiantu_gongfa_power_speed_bonus")==0
                    && deathTag.getDouble("xiantu_physique_power_max_bonus")==0,
                    "Death removes stale active-method and power-bonus tags");
            for (var method : GongfaRegistry.ALL) {
                var item = book(method.id()); check(item.getItem() instanceof GongfaItem,"Registered reusable book "+method.id());
                check(!player.serverLevel().getRecipeManager().getRecipes().stream().anyMatch(r -> r.getResultItem(player.level().registryAccess()).getItem()==item.getItem()),"No free craft "+method.id());
            }
            clearCombat(player); player.setItemInHand(InteractionHand.MAIN_HAND,book("tai_qing_hun_yuan_jing"));
            GongfaMenu stale = new GongfaMenu(97,player.getInventory(),InteractionHand.MAIN_HAND,"tai_qing_hun_yuan_jing"); player.containerMenu = stale;
            player.setItemInHand(InteractionHand.MAIN_HAND,book("qing_mu_jue"));
            check(!((AbstractContainerMenu)stale).clickMenuButton(player,0),"Stale confirm cannot approve a different book");
            var status = new CultivationStatusMenu(98,player.getInventory()); player.containerMenu = status;
            check(!((AbstractContainerMenu)status).clickMenuButton(player,99),"Unknown jade action rejected server-side");
            data.gongfa().clear(); data.gongfa().learn("qing_mu_jue"); data.gongfa().commit("qing_mu_jue",UUID.randomUUID());
            data.gongfa().activeProgress().add(2670); GongfaService.refresh(player);
            var healing = new net.minecraftforge.event.entity.living.LivingHealEvent(player,10);
            MinecraftForge.EVENT_BUS.post(healing);
            check(Math.abs(healing.getAmount()-10.5)<.001,"Nine-layer wood method amplifies received healing");
            check(player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE)
                            .getModifier(UUID.fromString("cc03473a-885b-49ef-9d51-20d70e991805"))==null,
                    "Inactive passive profile leaves no stale knockback modifier");
            player.sendSystemMessage(Component.literal("[Gongfa QA] All "+checks+" assertions passed"));
            org.slf4j.LoggerFactory.getLogger(GongfaIntegration.class).info("[Gongfa QA] All {} assertions passed",checks); return checks;
        } catch (RuntimeException|LinkageError error) {
            org.slf4j.LoggerFactory.getLogger(GongfaIntegration.class).error("[Gongfa QA] FAIL",error); throw error;
        } finally { restore(player,saved); }
    }
    public static int gui(ServerPlayer player) {
        if (!qa(player) || GUI_BACKUPS.containsKey(player.getUUID())) return 0;
        Backup saved = save(player); GUI_BACKUPS.put(player.getUUID(),saved);
        var data = GongfaService.data(player); data.setCultivationUnlocked(true);
        ((PlayerCultivationData)data).setCharacterInitialized(true); data.setRealmStage(2,3); data.setProgress(99);
        data.setPowerMax(1000); data.setPowerSpeed(2.5); data.setPower(80); data.gongfa().clear();
        data.gongfa().learn("xuan_shui_jue"); data.gongfa().commit("xuan_shui_jue",UUID.randomUUID()); data.gongfa().activeProgress().add(300);
        clearCombat(player); sit(player,saved.cushion); GongfaService.refresh(player);
        player.setItemInHand(InteractionHand.MAIN_HAND,book("tai_qing_hun_yuan_jing")); GongfaMenu.open(player,InteractionHand.MAIN_HAND); return 1;
    }
    public static int restoreGui(ServerPlayer player) {
        if (!qa(player)) return 0;
        Backup saved = GUI_BACKUPS.remove(player.getUUID()); if (saved == null) return 0;
        var data = GongfaService.data(player);
        boolean passed = data.majorRealm()==1 && data.stage()==3 && data.progress()==0 && data.gongfa().active().equals("tai_qing_hun_yuan_jing")
                && data.gongfa().activeLevel()==0 && player.getMainHandItem().getItem()==book("tai_qing_hun_yuan_jing").getItem() && player.getMainHandItem().getCount()==1;
        restore(player,saved);
        String message = "[Gongfa GUI QA] "+(passed ? "PASS" : "FAIL")+" actual seated client study; reusable book retained; original player restored";
        player.sendSystemMessage(Component.literal(message)); org.slf4j.LoggerFactory.getLogger(GongfaIntegration.class).info(message); return passed ? 1 : 0;
    }
}
