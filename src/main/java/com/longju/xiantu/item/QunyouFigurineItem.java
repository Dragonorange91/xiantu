package com.longju.xiantu.item;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

public final class QunyouFigurineItem extends BlockItem {
    public QunyouFigurineItem(Block block,Properties properties){super(block,properties);}
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer){com.longju.xiantu.client.GltfItemRenderer.install(consumer);}
}
