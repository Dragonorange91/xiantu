package com.longju.xiantu.qa;

import com.longju.xiantu.combat.*;
import com.longju.xiantu.cultivation.*;
import com.longju.xiantu.registry.ModEntities;
import com.longju.xiantu.registry.ModItems;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraft.world.InteractionHand;

/** Real hurt events on unspawned fixtures; never change the host player's character or inventory. */
public final class ElementalIntegration {
    private static int checks;
    private static void check(boolean ok, String label) {
        if (!ok) throw new IllegalStateException("[Elemental QA FAIL] " + label);
        checks++;
    }
    private static void near(double actual, double expected, String label) {
        check(Math.abs(actual - expected) < .003, label + ": " + actual + " vs " + expected);
    }
    private static void reset(LivingEntity target) {
        target.invulnerableTime = 0; target.hurtTime = 0;
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
        target.getAttribute(Attributes.ARMOR).setBaseValue(0);
        target.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(0);
        target.setHealth(200);
    }
    private static double hit(LivingEntity target, DamageSource source, float amount) {
        reset(target); check(target.hurt(source, amount), "Real hurt accepted");
        return 200 - target.getHealth();
    }
    public static int run(ServerPlayer host) {
        if (!host.getServer().getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA")) {
            host.sendSystemMessage(Component.literal("属性QA仅允许Xiantu-QA开头的隔离世界")); return 0;
        }
        checks = 0;
        check(ElementalConfig.ENABLED.get(), "Elemental combat enabled");
        List<LivingEntity> fixtures = new ArrayList<>();
        var player = new FakePlayer(host.serverLevel(), new GameProfile(UUID.randomUUID(), "ElementQA"));
        fixtures.add(player);
        try {
            var data = (PlayerCultivationData)player.getCapability(CultivationCapability.DATA).orElseThrow(IllegalStateException::new);
            data.setPhysique(PhysiqueType.ORDINARY); data.gongfa().clear();
            var wood = ModEntities.VINE_DEER.get().create(host.serverLevel()); fixtures.add(wood);
            var metal = ModEntities.IRON_TOOTH_BADGER.get().create(host.serverLevel()); fixtures.add(metal);
            var fire = ModEntities.YANWEI_FOX.get().create(host.serverLevel()); fixtures.add(fire);
            var water = ModEntities.COLD_FANG_WOLF.get().create(host.serverLevel()); fixtures.add(water);
            var thunder = ModEntities.THUNDER_DEER.get().create(host.serverLevel()); fixtures.add(thunder);
            var cow = EntityType.COW.create(host.serverLevel()); fixtures.add(cow);
            check(fixtures.stream().allMatch(e -> e != null), "Fixtures created");
            for (LivingEntity fixture : fixtures) for (Element e : Element.values()) if (e.elemental()) {
                check(fixture.getAttribute(ElementalAttributes.RESISTANCE.get(e).get()) != null, "Resistance registered " + e);
                check(fixture.getAttribute(ElementalAttributes.PENETRATION.get(e).get()) != null, "Penetration registered " + e);
            }
            double adv = ElementalConfig.ADVANTAGE.get(), reverse = ElementalConfig.REVERSE.get();
            near(hit(wood, metal.damageSources().mobAttack(metal), 10), 10 * adv, "Metal beast beats wood");
            near(hit(metal, wood.damageSources().mobAttack(wood), 10), 10 * reverse, "Reverse matchup");
            reset(wood); wood.getAttribute(Attributes.ARMOR).setBaseValue(10);
            check(wood.hurt(metal.damageSources().mobAttack(metal), 10), "Armored real hit accepted");
            near(200 - wood.getHealth(), net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb((float)(10 * adv), 10, 0),
                    "Vanilla armor applies exactly once after elemental adjustment");
            near(hit(wood, wood.damageSources().mobAttack(wood), 10), 10, "Same element neutral");
            near(hit(water, thunder.damageSources().mobAttack(thunder), 10), 10, "Thunder attacks water neutrally");
            near(hit(thunder, water.damageSources().mobAttack(water), 10), 10, "Water attacks thunder neutrally");
            near(hit(cow, fire.damageSources().mobAttack(fire), 10), 10, "Unaffiliated vanilla target");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
            near(hit(wood, player.damageSources().playerAttack(player), 10), 10 * adv, "Tagged player weapon");
            near(hit(wood, player.damageSources().thorns(player), 10), 10, "Thorns do not inherit weapon element");
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            near(hit(wood, player.damageSources().playerAttack(player), 10), 10, "Physical player attack");
            wood.getAttribute(ElementalAttributes.RESISTANCE.get(Element.FIRE).get()).setBaseValue(.2);
            fire.getAttribute(ElementalAttributes.PENETRATION.get(Element.FIRE).get()).setBaseValue(.1);
            near(hit(wood, fire.damageSources().mobAttack(fire), 10), 9, "Correct elemental resistance and penetration");
            near(hit(wood, player.damageSources().playerAttack(player), 10), 10, "Resistance never reduces physical");
            var source = new ElementalDamageSource(player.damageSources().indirectMagic(player, player), Element.METAL);
            check(source.getEntity() == player && source.getDirectEntity() == player, "Explicit source preserves attribution");
            near(hit(wood, source, 10), 10 * adv, "Explicit future spell source");
            near(hit(wood, new ElementalDamageSource(player.damageSources().indirectMagic(player, player), Element.METAL, .25), 10),
                    10 * (.75 + .25 * adv), "Mixed hit transforms only the elemental part");
            near(hit(wood, new ElementalDamageSource(metal.damageSources().mobAttack(metal), Element.PHYSICAL), 10), 10, "Explicit physical suppresses innate conversion");
            data.load(new net.minecraft.nbt.CompoundTag()); data.setPhysique(PhysiqueType.ORDINARY);
            data.addSpiritRoot(SpiritRootType.WOOD); data.addSpiritRoot(SpiritRootType.FIRE);
            near(hit(player, new ElementalDamageSource(player.damageSources().indirectMagic(metal, metal), Element.METAL), 10),
                    10 * (adv + reverse) / 2, "Player mixed roots average once; difficulty-independent source");
            var projectile = ModEntities.FOXFIRE.get().create(host.serverLevel()); projectile.setOwner(fire);
            near(hit(water, fire.damageSources().mobProjectile(projectile, fire), 10), 10 * reverse, "Indirect owner determines elemental attack");
            projectile.discard();
            host.sendSystemMessage(Component.literal("[Elemental QA PASS] " + checks + " runtime assertions"));
            return 1;
        } finally {
            for (LivingEntity fixture : fixtures) if (fixture != null) fixture.discard();
        }
    }
    private ElementalIntegration() {}
}
