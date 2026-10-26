package com.longju.xiantu.instance;

import com.longju.xiantu.registry.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.*;
import com.mojang.serialization.Codec;
import net.minecraft.world.level.chunk.ChunkGenerator;

public final class InstanceRegistry {
    public static final DeferredRegister<Codec<? extends ChunkGenerator>> GENERATORS=DeferredRegister.create(Registries.CHUNK_GENERATOR,"xiantu");
    public static final RegistryObject<Codec<? extends ChunkGenerator>> LANDSCAPE=GENERATORS.register("instance",()->InstanceChunkGenerator.CODEC);
    public static final RegistryObject<Block> CORE=ModBlocks.BLOCKS.register("instance_core",()->new InstanceCoreBlock(BlockBehaviour.Properties.of().strength(-1,3600000).lightLevel(s->12).noLootTable()));
    public static final RegistryObject<Block> PORTAL=ModBlocks.BLOCKS.register("realm_portal",()->new RealmPortalBlock(true));
    public static final RegistryObject<Block> FIELD=ModBlocks.BLOCKS.register("realm_portal_field",()->new RealmPortalBlock(false));
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<RealmPortalBlockEntity>> PORTAL_ENTITY=ModBlockEntities.BLOCK_ENTITIES.register("realm_portal",()->net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(RealmPortalBlockEntity::new,PORTAL.get()).build(null));
    public static final RegistryObject<Item> REALM_TEST=ModItems.ITEMS.register("realm_test_jade",RealmTestItem::new);
    public static final RegistryObject<Item> TOKEN=ModItems.ITEMS.register("dongfu_token",()->new CaveTokenItem(false));
    public static final RegistryObject<Item> CHILD=ModItems.ITEMS.register("dongfu_child_token",()->new CaveTokenItem(true));
    public static void bootstrap(){InstanceDecor.bootstrap();}
    private InstanceRegistry(){}
}
