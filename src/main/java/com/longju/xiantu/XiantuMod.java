package com.longju.xiantu;

import com.longju.xiantu.network.XiantuNetwork;
import com.longju.xiantu.registry.ModBlocks;
import com.longju.xiantu.registry.ModBlockItems;
import com.longju.xiantu.registry.ModEffects;
import com.longju.xiantu.registry.ModEntities;
import com.longju.xiantu.registry.ModItems;
import com.longju.xiantu.registry.ModCreativeTabs;
import com.longju.xiantu.registry.ModMenus;
import com.longju.xiantu.registry.ModBlockEntities;
import com.longju.xiantu.registry.ModParticles;
import com.longju.xiantu.registry.ModSounds;
import com.longju.xiantu.registry.ModWorldgen;
import com.longju.xiantu.config.XiantuConfig;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.common.MinecraftForge;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

@Mod(XiantuMod.MOD_ID)
public final class XiantuMod {
    public static final String MOD_ID = "xiantu";
    private static final Logger LOGGER = LogUtils.getLogger();

    static {
        configureWindowsSelectorDirectory();
    }

    /**
     * Java 17 and 21 on some Windows installations create NIO selector pipes
     * below the system temporary directory.  That location can reject the
     * loopback Unix-domain socket used by the JDK, which then makes an
     * integrated server crash when a world is opened.  The property is read
     * by the JDK at runtime, so the mod can provide a writable game-local
     * directory before Minecraft creates its first selector.
     */
    private static void configureWindowsSelectorDirectory() {
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
            return;
        }
        if (System.getProperty("jdk.net.unixdomain.tmpdir") != null) {
            return;
        }
        Exception lastFailure = null;
        try {
            Path gameDirectory = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
            Path homeDirectory = Paths.get(System.getProperty("user.home", ".")).toAbsolutePath().normalize();
            Path[] candidates = {
                    gameDirectory.resolve("xiantu-runtime").resolve("selector"),
                    homeDirectory.resolve(".xiantu-runtime").resolve("selector")
            };
            for (Path selectorDirectory : candidates) {
                // Windows Unix-domain socket names have a finite path length.
                if (selectorDirectory.toString().length() > 100) {
                    continue;
                }
                try {
                    Files.createDirectories(selectorDirectory);
                    if (Files.isDirectory(selectorDirectory) && Files.isWritable(selectorDirectory)) {
                        System.setProperty("jdk.net.unixdomain.tmpdir", selectorDirectory.toString());
                        return;
                    }
                } catch (Exception exception) {
                    lastFailure = exception;
                }
            }
        } catch (Exception exception) {
            lastFailure = exception;
        }
        LOGGER.warn("无法为 Java NIO 选择器准备仙途运行目录，继续使用 Java 默认临时目录", lastFailure);
    }

    public XiantuMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        com.longju.xiantu.mining.MiningTiers.bootstrap();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, XiantuConfig.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, com.longju.xiantu.config.XiantuClientConfig.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER,
                com.longju.xiantu.combat.ElementalConfig.SPEC, "xiantu-elements.toml");
        com.longju.xiantu.combat.ElementalAttributes.REGISTRY.register(bus);
        LOGGER.info("仙途模组已加载，版本 {}", "0.1.0");
        com.longju.xiantu.wood.SpiritWoodRegistry.register(bus);
        com.longju.xiantu.medicine.MedicineRegistry.register(bus);
        com.longju.xiantu.workstation.WorkstationRegistry.register(bus);
        com.longju.xiantu.terrain.TerrainBlockRegistry.register(bus);
        com.longju.xiantu.terrain.TerrainPlantRegistry.register(bus);
        ModItems.ITEMS.register(bus);
        com.longju.xiantu.instance.InstanceRegistry.bootstrap();
        com.longju.xiantu.instance.InstanceRegistry.GENERATORS.register(bus);
        ModCreativeTabs.TABS.register(bus);
        ModBlockItems.ITEMS.register(bus);
        ModBlocks.BLOCKS.register(bus);
        ModBlockEntities.BLOCK_ENTITIES.register(bus);
        ModEffects.EFFECTS.register(bus);
        ModEntities.ENTITY_TYPES.register(bus);
        ModMenus.MENUS.register(bus);
        ModParticles.PARTICLES.register(bus);
        ModSounds.SOUNDS.register(bus);
        ModWorldgen.FEATURES.register(bus);
        com.longju.xiantu.worldgen.RuinBlueprints.TYPES.register(bus);
        com.longju.xiantu.worldgen.town.TownWorldgen.STRUCTURES.register(bus);
        com.longju.xiantu.worldgen.town.TownWorldgen.PIECES.register(bus);
        com.longju.xiantu.registry.ModRecipes.TYPES.register(bus);
        com.longju.xiantu.registry.ModRecipes.SERIALIZERS.register(bus);
        bus.addListener(ModEntities::registerAttributes);
        bus.addListener(com.longju.xiantu.entity.BeastRealmService::setup);
        bus.addListener(com.longju.xiantu.terrain.TerrainWorldgenRegistration::commonSetup);
        bus.addListener(com.longju.xiantu.cultivation.CultivationCapability::register);
        // Register gameplay events explicitly so capability attachment and
        // player ticks are reliable in both the client and dedicated server.
        MinecraftForge.EVENT_BUS.register(com.longju.xiantu.cultivation.CultivationEvents.class);
        MinecraftForge.EVENT_BUS.register(com.longju.xiantu.mining.SpiritOreEvents.class);
        MinecraftForge.EVENT_BUS.register(com.longju.xiantu.item.SpiritStoneAbsorption.class);
        MinecraftForge.EVENT_BUS.register(com.longju.xiantu.terrain.TerrainLightningEvents.class);
        XiantuNetwork.register();
    }
}






