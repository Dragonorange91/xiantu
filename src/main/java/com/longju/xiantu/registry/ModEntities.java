package com.longju.xiantu.registry;

import com.longju.xiantu.XiantuMod;
import com.longju.xiantu.entity.InnerDemonEntity;
import com.longju.xiantu.entity.YanweiFoxEntity;
import com.longju.xiantu.entity.CrackedBladeWolfEntity;
import com.longju.xiantu.entity.CragRockSpiderEntity;
import com.longju.xiantu.entity.ColdFangWolfEntity;
import com.longju.xiantu.entity.VineDeerEntity;
import com.longju.xiantu.entity.ThunderDeerEntity;
import com.longju.xiantu.entity.IronToothBadgerEntity;
import com.longju.xiantu.entity.RockBackBoarEntity;
import com.longju.xiantu.entity.MossBackBearEntity;
import com.longju.xiantu.entity.MountainBearEntity;
import com.longju.xiantu.entity.ThickHornRhinoEntity;
import com.longju.xiantu.entity.GoldManeLionEntity;
import com.longju.xiantu.entity.RedSunLionEntity;
import com.longju.xiantu.entity.ThunderFeatherRavenEntity;
import com.longju.xiantu.formation.FormationSeatEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;

@Mod.EventBusSubscriber(modid = XiantuMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, XiantuMod.MOD_ID);
    public static final java.util.Map<String,RegistryObject<EntityType<com.longju.xiantu.trade.TownMerchantEntity>>> MERCHANTS = registerMerchants();
    private static java.util.Map<String,RegistryObject<EntityType<com.longju.xiantu.trade.TownMerchantEntity>>> registerMerchants() {
        var result = new java.util.LinkedHashMap<String,RegistryObject<EntityType<com.longju.xiantu.trade.TownMerchantEntity>>>();
        for (String id : java.util.List.of("wanbao_merchant","smith_merchant","alchemy_merchant","talisman_merchant","town_guide")) {
            result.put(id,ENTITY_TYPES.register(id,()->EntityType.Builder.<com.longju.xiantu.trade.TownMerchantEntity>of(
                com.longju.xiantu.trade.TownMerchantEntity::new,MobCategory.CREATURE).sized(.65F,2.05F).clientTrackingRange(10).build("xiantu:"+id)));
        }
        return result;
    }
    public static final java.util.Map<String, RegistryObject<EntityType<net.minecraft.world.entity.Mob>>> COMPLETION = CompletionEntities.registerTypes();
    public static final RegistryObject<EntityType<com.longju.xiantu.entity.CompletionBolt>> COMPLETION_BOLT = ENTITY_TYPES.register("beast_lightning_bolt", () ->
            EntityType.Builder.<com.longju.xiantu.entity.CompletionBolt>of(com.longju.xiantu.entity.CompletionBolt::new, MobCategory.MISC)
                    .sized(.5F,.5F).clientTrackingRange(8).updateInterval(1).build("xiantu:beast_lightning_bolt"));
    public static final RegistryObject<EntityType<com.longju.xiantu.entity.FoxfireProjectile>> FOXFIRE = ENTITY_TYPES.register("foxfire", () ->
            EntityType.Builder.<com.longju.xiantu.entity.FoxfireProjectile>of(com.longju.xiantu.entity.FoxfireProjectile::new, MobCategory.MISC)
                .sized(.5F,.5F).clientTrackingRange(8).updateInterval(1).build("xiantu:foxfire"));
    public static final RegistryObject<EntityType<YanweiFoxEntity>> YANWEI_FOX = ENTITY_TYPES.register("yanwei_fox", () ->
            EntityType.Builder.<YanweiFoxEntity>of(YanweiFoxEntity::new, MobCategory.CREATURE)
                    .sized(.75F, 1.35F).clientTrackingRange(8).build(XiantuMod.MOD_ID + ":yanwei_fox"));
    public static final RegistryObject<EntityType<CrackedBladeWolfEntity>> CRACKED_BLADE_WOLF = ENTITY_TYPES.register("cracked_blade_wolf", () ->
            EntityType.Builder.<CrackedBladeWolfEntity>of(CrackedBladeWolfEntity::new, MobCategory.MONSTER)
                    .sized(.8F, 1.2F).clientTrackingRange(8).build(XiantuMod.MOD_ID + ":cracked_blade_wolf"));
    public static final RegistryObject<EntityType<CragRockSpiderEntity>> CRAG_ROCK_SPIDER = ENTITY_TYPES.register("crag_rock_spider", () ->
            EntityType.Builder.<CragRockSpiderEntity>of(CragRockSpiderEntity::new, MobCategory.MONSTER)
                    .sized(1.15F, .6F).clientTrackingRange(8).build(XiantuMod.MOD_ID + ":crag_rock_spider"));
    public static final RegistryObject<EntityType<ColdFangWolfEntity>> COLD_FANG_WOLF = ENTITY_TYPES.register("cold_fang_wolf", () ->
            EntityType.Builder.<ColdFangWolfEntity>of(ColdFangWolfEntity::new, MobCategory.MONSTER)
                    .sized(.9F, 1.45F).clientTrackingRange(8).build(XiantuMod.MOD_ID + ":cold_fang_wolf"));
    public static final RegistryObject<EntityType<ThunderFeatherRavenEntity>> THUNDER_FEATHER_RAVEN = ENTITY_TYPES.register("thunder_feather_raven", () ->
            EntityType.Builder.<ThunderFeatherRavenEntity>of(ThunderFeatherRavenEntity::new, MobCategory.CREATURE)
                    .sized(.7F, .95F).clientTrackingRange(8).build(XiantuMod.MOD_ID + ":thunder_feather_raven"));
    public static final RegistryObject<EntityType<VineDeerEntity>> VINE_DEER = ENTITY_TYPES.register("vine_deer", () ->
            EntityType.Builder.<VineDeerEntity>of(VineDeerEntity::new, MobCategory.CREATURE)
                    .sized(1.0F, 1.55F).clientTrackingRange(8).build(XiantuMod.MOD_ID + ":vine_deer"));
    public static final RegistryObject<EntityType<ThunderDeerEntity>> THUNDER_DEER = ENTITY_TYPES.register("thunder_deer", () ->
            EntityType.Builder.<ThunderDeerEntity>of(ThunderDeerEntity::new, MobCategory.CREATURE)
                    .sized(1.0F, 1.65F).clientTrackingRange(8).build(XiantuMod.MOD_ID + ":thunder_deer"));
    public static final RegistryObject<EntityType<RockBackBoarEntity>> ROCK_BACK_BOAR = ENTITY_TYPES.register("rock_back_boar", () ->
            EntityType.Builder.<RockBackBoarEntity>of(RockBackBoarEntity::new, MobCategory.CREATURE)
                    .sized(1.15F, 1.45F).clientTrackingRange(8).build(XiantuMod.MOD_ID + ":rock_back_boar"));
    public static final RegistryObject<EntityType<IronToothBadgerEntity>> IRON_TOOTH_BADGER = ENTITY_TYPES.register("iron_tooth_badger", () ->
            EntityType.Builder.<IronToothBadgerEntity>of(IronToothBadgerEntity::new, MobCategory.CREATURE)
                    .sized(1.1F, 1.2F).clientTrackingRange(8).build(XiantuMod.MOD_ID + ":iron_tooth_badger"));
    public static final RegistryObject<EntityType<MossBackBearEntity>> MOSS_BACK_BEAR = ENTITY_TYPES.register("moss_back_bear", () ->
            EntityType.Builder.<MossBackBearEntity>of(MossBackBearEntity::new, MobCategory.MONSTER)
                    .sized(1.6F, 1.8F).clientTrackingRange(8).build(XiantuMod.MOD_ID + ":moss_back_bear"));
    public static final RegistryObject<EntityType<MountainBearEntity>> MOUNTAIN_BEAR = ENTITY_TYPES.register("mountain_bear", () ->
            EntityType.Builder.<MountainBearEntity>of(MountainBearEntity::new, MobCategory.MONSTER)
                    .sized(1.7F, 2.2F).clientTrackingRange(8).build(XiantuMod.MOD_ID + ":mountain_bear"));
    public static final RegistryObject<EntityType<ThickHornRhinoEntity>> THICK_HORN_RHINO = ENTITY_TYPES.register("thick_horn_rhino", () ->
            EntityType.Builder.<ThickHornRhinoEntity>of(ThickHornRhinoEntity::new, MobCategory.CREATURE)
                    .sized(1.7F, 1.8F).clientTrackingRange(8).build(XiantuMod.MOD_ID + ":thick_horn_rhino"));
    public static final RegistryObject<EntityType<GoldManeLionEntity>> GOLD_MANE_LION = ENTITY_TYPES.register("gold_mane_lion", () ->
            EntityType.Builder.<GoldManeLionEntity>of(GoldManeLionEntity::new, MobCategory.MONSTER)
                    .sized(1.35F, 1.85F).clientTrackingRange(8).build(XiantuMod.MOD_ID + ":gold_mane_lion"));
    public static final RegistryObject<EntityType<RedSunLionEntity>> RED_SUN_LION = ENTITY_TYPES.register("red_sun_lion", () ->
            EntityType.Builder.<RedSunLionEntity>of(RedSunLionEntity::new, MobCategory.MONSTER)
                    .sized(1.35F, 1.85F).clientTrackingRange(8).build(XiantuMod.MOD_ID + ":red_sun_lion"));
    public static final RegistryObject<EntityType<com.longju.xiantu.spell.SpiritSwordEntity>> SPIRIT_SWORD = ENTITY_TYPES.register("spirit_sword", () ->
            EntityType.Builder.<com.longju.xiantu.spell.SpiritSwordEntity>of(com.longju.xiantu.spell.SpiritSwordEntity::new, MobCategory.MISC)
                    .sized(1.15F,.24F).clientTrackingRange(10).updateInterval(1).build("xiantu:spirit_sword"));
    public static final RegistryObject<EntityType<FormationSeatEntity>> FORMATION_SEAT = ENTITY_TYPES.register("formation_seat", () ->
            EntityType.Builder.<FormationSeatEntity>of(FormationSeatEntity::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F).clientTrackingRange(4).updateInterval(20).build(XiantuMod.MOD_ID + ":formation_seat"));
    public static final RegistryObject<EntityType<com.longju.xiantu.alchemy.PillDecoyEntity>> PILL_DECOY = ENTITY_TYPES.register("pill_decoy", () ->
        EntityType.Builder.<com.longju.xiantu.alchemy.PillDecoyEntity>of(com.longju.xiantu.alchemy.PillDecoyEntity::new,MobCategory.MISC).sized(.6F,1.8F).clientTrackingRange(8).build("xiantu:pill_decoy"));
    public static final RegistryObject<EntityType<InnerDemonEntity>> INNER_DEMON = ENTITY_TYPES.register("inner_demon", () ->
            EntityType.Builder.<InnerDemonEntity>of(InnerDemonEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F).clientTrackingRange(8).build(XiantuMod.MOD_ID + ":inner_demon"));

    public static void registerAttributes(net.minecraftforge.event.entity.EntityAttributeCreationEvent event) {
        event.put(PILL_DECOY.get(),net.minecraft.world.entity.monster.Zombie.createAttributes().build());
        CompletionEntities.attributes(event);
        MERCHANTS.values().forEach(type -> event.put(type.get(),net.minecraft.world.entity.Mob.createMobAttributes()
            .add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH,20)
            .add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED,0)
            .add(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE,1).build()));
        event.put(YANWEI_FOX.get(), net.minecraft.world.entity.Mob.createMobAttributes()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 20)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, 4)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE, 16)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, .28).build());
        event.put(CRACKED_BLADE_WOLF.get(), net.minecraft.world.entity.Mob.createMobAttributes()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 36)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, 3)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE, 16)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, .25).build());
        event.put(CRAG_ROCK_SPIDER.get(), net.minecraft.world.entity.Mob.createMobAttributes()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 28)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, 5)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE, 16)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, .27).build());
        event.put(COLD_FANG_WOLF.get(), net.minecraft.world.entity.Mob.createMobAttributes()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 36)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, 3)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE, 16)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, .25).build());
        event.put(THUNDER_FEATHER_RAVEN.get(), net.minecraft.world.entity.Mob.createMobAttributes()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 36)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, 3)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE, 16)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, .25).build());
        event.put(VINE_DEER.get(), net.minecraft.world.entity.Mob.createMobAttributes()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, 3)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 36)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, .25).build());
        event.put(THUNDER_DEER.get(), net.minecraft.world.entity.Mob.createMobAttributes()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, 3)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 36)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, .25).build());
        event.put(ROCK_BACK_BOAR.get(), net.minecraft.world.entity.Mob.createMobAttributes()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 36)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, 3)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE, 24)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, .25).build());
        event.put(IRON_TOOTH_BADGER.get(), net.minecraft.world.entity.Mob.createMobAttributes()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, 3)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 24)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, .23).build());
        event.put(MOSS_BACK_BEAR.get(), net.minecraft.world.entity.Mob.createMobAttributes()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 36)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, 3)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE, 16)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, .25).build());
        event.put(MOUNTAIN_BEAR.get(), net.minecraft.world.entity.Mob.createMobAttributes()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 60)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, 4)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR, 4)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE, .35)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE, 16)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, .23).build());
        event.put(THICK_HORN_RHINO.get(), net.minecraft.world.entity.Mob.createMobAttributes()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 36)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, 3)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR, 3)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE, 24)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, .24).build());
        event.put(GOLD_MANE_LION.get(), net.minecraft.world.entity.Mob.createMobAttributes()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 48)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, 4)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR, 3)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE, 16)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, .27).build());
        event.put(RED_SUN_LION.get(), net.minecraft.world.entity.Mob.createMobAttributes()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 48)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, 4)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR, 3)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE, 16)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, .27).build());
        event.put(INNER_DEMON.get(), net.minecraft.world.entity.monster.Zombie.createAttributes()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 20)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, 3)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, 0.23).build());
    }

    @SubscribeEvent
    public static void registerSpawnPlacements(SpawnPlacementRegisterEvent event) {
    }
    private ModEntities() {}
}
