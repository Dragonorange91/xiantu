package com.longju.xiantu.alchemy;
import com.longju.xiantu.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;
public final class AlchemyItems {
    public static final RegistryObject<Item> FAILED=ModItems.ITEMS.register("failed_pill",()->new Item(new Item.Properties()));
    public static void bootstrap(){}
    private AlchemyItems(){}
}
