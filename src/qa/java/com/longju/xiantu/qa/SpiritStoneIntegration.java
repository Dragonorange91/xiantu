package com.longju.xiantu.qa;

import com.longju.xiantu.cultivation.CultivationCapability;
import com.longju.xiantu.cultivation.PlayerCultivationData;
import com.longju.xiantu.cultivation.CultivationEvents;
import com.longju.xiantu.item.SpiritStoneAbsorption;
import com.longju.xiantu.item.SpiritStoneDowncraftRecipe;
import com.longju.xiantu.item.SpiritStoneItem;
import com.longju.xiantu.item.SpiritStoneTier;
import com.longju.xiantu.registry.ModBlocks;
import com.longju.xiantu.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

@Mod.EventBusSubscriber(modid = "xiantu_qa", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SpiritStoneIntegration {
    private static final String MANUAL_BACKUP = "xiantu_manual_stone_absorption_backup";
    private static final Map<UUID, Integer> DAMAGE_TESTS = new HashMap<>();
    private SpiritStoneIntegration() {}

    public static int prepareManualAbsorption(ServerPlayer player) {
        if (!isQaWorld(player)) return 0;
        if (player.getPersistentData().contains(MANUAL_BACKUP)) {
            player.sendSystemMessage(Component.literal("Manual absorption test already has a saved state"));
            return 0;
        }
        PlayerCultivationData data = (PlayerCultivationData) player.getCapability(CultivationCapability.DATA)
                .orElseThrow(IllegalStateException::new);
        CompoundTag backup = new CompoundTag();
        CompoundTag savedData = new CompoundTag();
        data.save(savedData);
        backup.put("cultivation", savedData);
        backup.put("hand", player.getMainHandItem().save(new CompoundTag()));
        backup.putInt("mode", player.gameMode.getGameModeForPlayer().getId());
        backup.putDouble("x", player.getX());
        backup.putDouble("y", player.getY());
        backup.putDouble("z", player.getZ());
        backup.putFloat("health", player.getHealth());
        backup.putInt("food", player.getFoodData().getFoodLevel());
        backup.putFloat("saturation", player.getFoodData().getSaturationLevel());
        backup.putLong("backlash_pulses", player.getPersistentData().getLong("xiantu_spirit_stone_backlash_pulses"));
        player.getPersistentData().put(MANUAL_BACKUP, backup);

        player.setGameMode(GameType.SURVIVAL);
        data.setRealm(0, 0);
        data.setPowerMax(10_000);
        data.setPower(0);
        ItemStack stone = new ItemStack(ModItems.LING_YU.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stone);
        CultivationEvents.sync(player);
        player.sendSystemMessage(Component.literal(
                "[Manual stone QA] Ready: realm rank 1, power 0, tier-three stone in main hand. Hold right-click for 2 seconds; then run /xiantuqa spirit_stone_restore."));
        return 1;
    }

    public static int restoreManualAbsorption(ServerPlayer player) {
        if (!isQaWorld(player)) return 0;
        CompoundTag root = player.getPersistentData();
        if (!root.contains(MANUAL_BACKUP)) {
            player.sendSystemMessage(Component.literal("No manual absorption backup exists"));
            return 0;
        }
        long currentPower = player.getCapability(CultivationCapability.DATA)
                .map(data -> Math.round(data.power() * 1_000.0D)).orElse(0L);
        ItemStack currentHand = player.getMainHandItem();
        long remaining = SpiritStoneItem.chargeMilli(currentHand, SpiritStoneTier.LING_YU);
        player.sendSystemMessage(Component.literal("[Manual stone QA] Result: player power="
                + currentPower / 1_000.0D + ", tier-three stone remaining=" + remaining / 1_000.0D));

        SpiritStoneAbsorption.cancel(player);
        player.stopUsingItem();
        DAMAGE_TESTS.remove(player.getUUID());
        CompoundTag backup = root.getCompound(MANUAL_BACKUP);
        PlayerCultivationData data = (PlayerCultivationData) player.getCapability(CultivationCapability.DATA)
                .orElseThrow(IllegalStateException::new);
        data.load(backup.getCompound("cultivation"));
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.of(backup.getCompound("hand")));
        player.teleportTo(backup.getDouble("x"), backup.getDouble("y"), backup.getDouble("z"));
        player.setHealth(Math.min(player.getMaxHealth(), backup.getFloat("health")));
        player.getFoodData().setFoodLevel(backup.getInt("food"));
        player.getFoodData().setSaturation(backup.getFloat("saturation"));
        player.getPersistentData().putLong("xiantu_spirit_stone_backlash_pulses", backup.getLong("backlash_pulses"));
        player.setGameMode(GameType.byId(backup.getInt("mode")));
        player.removeEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
        player.removeEffect(net.minecraft.world.effect.MobEffects.WEAKNESS);
        root.remove(MANUAL_BACKUP);
        CultivationEvents.sync(player);
        player.sendSystemMessage(Component.literal("[Manual stone QA] Original player state restored"));
        return 1;
    }

    public static int scheduleManualDamage(ServerPlayer player) {
        if (!isQaWorld(player)) return 0;
        if (!player.getPersistentData().contains(MANUAL_BACKUP)) return 0;
        DAMAGE_TESTS.put(player.getUUID(), player.getServer().getTickCount() + 1200);
        player.sendSystemMessage(Component.literal("[Manual stone QA] Damage test armed for 60 seconds; hold right-click to receive external damage at use tick 5"));
        return 1;
    }

    @SubscribeEvent
    public static void damageTestTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        Integer deadline = DAMAGE_TESTS.get(player.getUUID());
        if (deadline == null) return;
        if (!player.isAlive() || player.getServer().getTickCount() > deadline) {
            DAMAGE_TESTS.remove(player.getUUID());
            player.sendSystemMessage(Component.literal("[Manual stone QA] Damage test expired"));
            return;
        }
        if (!player.isUsingItem() || player.getTicksUsingItem() < 5) return;
        ItemStack stone = player.getUseItem();
        if (!stone.is(ModItems.LING_YU.get())) return;
        DAMAGE_TESTS.remove(player.getUUID());
        long charge = SpiritStoneItem.chargeMilli(stone, SpiritStoneTier.LING_YU);
        player.invulnerableTime = 0;
        boolean hit = ((net.minecraft.world.entity.Entity) player).hurt(player.damageSources().generic(), 1.0F);
        check(player, hit && !player.isUsingItem() && stone.getCount() == 1
                        && charge > 0 && charge < SpiritStoneTier.LING_YU.spiritualPower() * 1000
                        && SpiritStoneItem.chargeMilli(stone, SpiritStoneTier.LING_YU) == charge,
                "External damage cancels real held use without losing the remaining charge: " + charge / 1000.0D);
    }

    private static boolean isQaWorld(ServerPlayer player) {
        boolean qa = player.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                .normalize().getFileName().toString().startsWith("Xiantu-QA");
        if (!qa) player.sendSystemMessage(Component.literal("QA commands require a Xiantu-QA disposable world"));
        return qa;
    }

    public static int runAbsorption(ServerPlayer player) {
        if (!player.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                .normalize().getFileName().toString().startsWith("Xiantu-QA")) {
            player.sendSystemMessage(Component.literal("QA commands require a Xiantu-QA disposable world"));
            return 0;
        }

        PlayerCultivationData data = (PlayerCultivationData) player.getCapability(CultivationCapability.DATA)
                .orElseThrow(IllegalStateException::new);
        CompoundTag savedData = new CompoundTag();
        data.save(savedData);
        ItemStack savedHand = player.getMainHandItem().copy();
        float savedHealth = player.getHealth();
        GameType savedMode = player.gameMode.getGameModeForPlayer();
        BlockPos savedPosition = player.blockPosition();
        long savedBacklashPulses = player.getPersistentData().getLong("xiantu_spirit_stone_backlash_pulses");
        try {
            player.setGameMode(GameType.SURVIVAL);
            data.setRealm(0, 0);
            data.setPowerMax(10_000);
            data.setPower(0);

            ItemStack changingState = new ItemStack(ModItems.FAN_LING_SHI.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, changingState);
            var changingUse = changingState.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
            check(player, changingUse.getResult().consumesAction(), "Same-tier absorption starts before power changes");
            data.setPower(9_950);
            ItemStack changedResult = SpiritStoneAbsorption.complete(player, SpiritStoneTier.FAN_LING_SHI, changingState);
            player.setItemInHand(InteractionHand.MAIN_HAND, changedResult);
            check(player, changingState.getCount() == 1 && data.power() == 10_000
                            && SpiritStoneItem.chargeMilli(changingState, SpiritStoneTier.FAN_LING_SHI) == 50_000,
                    "Absorption that reaches the cap consumes only the fifty power actually restored");
            player.stopUsingItem();
            data.setPower(0);

            ItemStack sameTier = new ItemStack(ModItems.FAN_LING_SHI.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, sameTier);
            var use = sameTier.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
            check(player, use.getResult().consumesAction() && player.isUsingItem(),
                    "Right-click starts the server-authoritative absorption action");
            check(player, sameTier.getItem().getUseDuration(sameTier) == 40,
                    "Absorption action lasts 40 ticks");
            player.sendSystemMessage(Component.literal("[Spirit stone QA] Completing same-tier transaction"));
            ItemStack sameTierResult = SpiritStoneAbsorption.complete(player, SpiritStoneTier.FAN_LING_SHI, sameTier);
            player.setItemInHand(InteractionHand.MAIN_HAND, sameTierResult);
            player.sendSystemMessage(Component.literal("[Spirit stone QA] Same-tier transaction returned"));
            check(player, sameTierResult.isEmpty() && data.power() == 100,
                    "Same-tier completion absorbs the stage-rate amount and preserves exact charge");
            player.stopUsingItem();

            data.setPower(0);
            ItemStack oneAbove = new ItemStack(ModItems.LING_JING.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, oneAbove);
            var higherUse = oneAbove.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
            check(player, higherUse.getResult().consumesAction(), "One-tier-higher stone begins absorption normally");
            ItemStack higherResult = SpiritStoneAbsorption.complete(player, SpiritStoneTier.LING_JING, oneAbove);
            player.setItemInHand(InteractionHand.MAIN_HAND, higherResult);
            check(player, higherResult.getCount() == 1 && data.power() == 100
                            && SpiritStoneItem.chargeMilli(higherResult, SpiritStoneTier.LING_JING) == 210_000,
                    "One tier above absorbs the same stage-rate amount and retains the unused charge");
            player.stopUsingItem();

            data.setPower(0);
            player.setHealth(20.0F);
            ItemStack overRank = new ItemStack(ModItems.LING_YU.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, overRank);
            player.getPersistentData().putLong("xiantu_spirit_stone_backlash_pulses", 0L);
            var overRankUse = overRank.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
            check(player, overRankUse.getResult().consumesAction(),
                    "Two-tier-over stone starts without a crouch confirmation");
            boolean overRankContinued = SpiritStoneAbsorption.tick(player, SpiritStoneTier.LING_YU, overRank, 10);
            check(player, overRankContinued, "Over-rank backlash stays active for the full absorption");
            SpiritStoneAbsorption.cancel(player, overRank);
            player.stopUsingItem();
            check(player, player.isAlive() && player.getHealth() == 8.0F
                            && player.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN)
                            && player.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS),
                    "Quick cancellation cannot erase the next escalating pulse after ten use ticks");

            data.setPower(0);
            data.setPowerMax(5);
            player.setHealth(20.0F);
            player.getPersistentData().putLong("xiantu_spirit_stone_backlash_pulses", 0L);
            ItemStack highestTier = new ItemStack(ModItems.DAO_SHI.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, highestTier);
            var highestTierUse = highestTier.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
            check(player, highestTierUse.getResult().consumesAction(),
                    "Highest-tier over-rank stone starts without overflow crouch confirmation");
            check(player, player.getHealth() == 6.0F
                            && player.getPersistentData().getLong("xiantu_spirit_stone_backlash_pulses") == 1L,
                    "A quick right-click immediately applies the first over-rank backlash pulse");
            SpiritStoneAbsorption.tick(player, SpiritStoneTier.DAO_SHI, highestTier, 1);
            SpiritStoneAbsorption.tick(player, SpiritStoneTier.DAO_SHI, highestTier, 2);
            long expectedRemaining = 299_000_000L - 5_000L;
            check(player, data.power() == data.powerMax()
                            && SpiritStoneItem.chargeMilli(highestTier, SpiritStoneTier.DAO_SHI) == expectedRemaining,
                    "Highest-tier over-rank absorption consumes only the five power needed to reach the cap");
            SpiritStoneAbsorption.cancel(player, highestTier);
            player.stopUsingItem();

            data.setPower(0);
            data.setPowerMax(10_000);
            ItemStack interrupted = new ItemStack(ModItems.FAN_LING_SHI.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, interrupted);
            var interruptedUse = interrupted.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
            long initialCharge = SpiritStoneItem.chargeMilli(interrupted, SpiritStoneTier.FAN_LING_SHI);
            for (int tick = 1; tick <= 20; tick++) {
                SpiritStoneAbsorption.tick(player, SpiritStoneTier.FAN_LING_SHI, interrupted, tick);
            }
            long partialCharge = SpiritStoneItem.chargeMilli(interrupted, SpiritStoneTier.FAN_LING_SHI);
            player.teleportTo(savedPosition.getX() + 2.0D, savedPosition.getY(), savedPosition.getZ());
            boolean continued = SpiritStoneAbsorption.canContinue(player, SpiritStoneTier.FAN_LING_SHI, interrupted);
            check(player, interruptedUse.getResult().consumesAction() && !continued && interrupted.getCount() == 1
                            && partialCharge < initialCharge,
                    "Movement interruption preserves consumed power as a partially charged stone");
            check(player, !SpiritStoneItem.isFullyCharged(interrupted),
                    "Partially charged stone is not eligible for crafting or downcrafting");
            player.stopUsingItem();

            player.sendSystemMessage(Component.literal("[Spirit stone QA] 11 runtime assertions passed"));
            return 1;
        } finally {
            SpiritStoneAbsorption.cancel(player);
            player.stopUsingItem();
            player.teleportTo(savedPosition.getX() + 0.5D, savedPosition.getY(), savedPosition.getZ() + 0.5D);
            data.load(savedData);
            player.setItemInHand(InteractionHand.MAIN_HAND, savedHand);
            player.setHealth(savedHealth);
            player.setGameMode(savedMode);
            player.getPersistentData().putLong("xiantu_spirit_stone_backlash_pulses", savedBacklashPulses);
        }
    }

    private static void check(ServerPlayer player, boolean passed, String label) {
        player.sendSystemMessage(Component.literal("[Spirit stone QA " + (passed ? "PASS" : "FAIL") + "] " + label));
        if (!passed) throw new IllegalStateException(label);
    }

    public static int verifyDowncraftRecipes(ServerPlayer player) {
        if (!player.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                .normalize().getFileName().toString().startsWith("Xiantu-QA")) {
            player.sendSystemMessage(Component.literal("QA commands require a Xiantu-QA disposable world"));
            return 0;
        }
        var tiers = SpiritStoneTier.values();
        for (int i = 1; i < tiers.length; i++) {
            ResourceLocation id = new ResourceLocation("xiantu", "spirit_stone_downcraft_" + tiers[i].registryName());
            player.sendSystemMessage(Component.literal("[Spirit stone QA] Checking downcraft tier " + tiers[i].rank()));
            var recipe = player.server.getRecipeManager().byKey(id).orElse(null);
            check(player, recipe != null, "Recipe is loaded: " + id);
            check(player, recipe instanceof SpiritStoneDowncraftRecipe && recipe instanceof ShapelessRecipe,
                    "Recipe uses charged-stone shapeless crafting: " + id);
            var downcraft = (SpiritStoneDowncraftRecipe) recipe;
            check(player, downcraft.inputTier() == tiers[i], "Recipe input tier is correct: " + id);
            var ingredients = recipe.getIngredients();
            check(player, ingredients.size() == 1
                            && ingredients.get(0).test(new ItemStack(ModItems.getSpiritStone(tiers[i]))),
                    "Recipe consumes exactly the matching upper-tier stone: " + id);
            ItemStack output = recipe.getResultItem(player.server.registryAccess());
            check(player, output.is(ModItems.getSpiritStone(tiers[i - 1])) && output.getCount() == tiers[i].downcraftCount(),
                    "Recipe returns the configured adjacent-tier quantity: " + id);

            var menu = new CraftingMenu(0, player.getInventory());
            var crafting = new TransientCraftingContainer(menu, 3, 3);
            ItemStack full = new ItemStack(ModItems.getSpiritStone(tiers[i]));
            crafting.setItem(0, full);
            player.sendSystemMessage(Component.literal("[Spirit stone QA] Matching full-charge tier " + tiers[i].rank()));
            boolean fullMatches = ((ShapelessRecipe) downcraft).matches(crafting, player.level());
            check(player, fullMatches, "Full-charge stone can be downcrafted: " + id);
            player.sendSystemMessage(Component.literal("[Spirit stone QA] Matching partial-charge tier " + tiers[i].rank()));
            ItemStack partial = new ItemStack(ModItems.getSpiritStone(tiers[i]));
            ((SpiritStoneItem) partial.getItem()).setChargeMilli(partial,
                    ((SpiritStoneItem) partial.getItem()).maxChargeMilli() / 2);
            crafting.setItem(0, partial);
            boolean partialMatches = ((ShapelessRecipe) downcraft).matches(crafting, player.level());
            check(player, !partialMatches, "Partial-charge stone cannot be multiplied: " + id);
            check(player, !ItemStack.isSameItemSameTags(full, partial),
                    "Partial-charge stone cannot stack with its full-charge counterpart: " + id);
        }
        player.sendSystemMessage(Component.literal("[Spirit stone QA] All 56 downcraft recipe assertions passed"));
        return 1;
    }

}
