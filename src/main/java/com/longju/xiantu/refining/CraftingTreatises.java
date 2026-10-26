package com.longju.xiantu.refining;

import com.longju.xiantu.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.RegistryObject;
import java.util.List;

/** Low-tier profession knowledge is learned once, independently of cultivation and experience. */
public final class CraftingTreatises {
    public static final String ALCHEMY_KEY="treatise/alchemy",REFINING_KEY="treatise/refining";
    public static final RegistryObject<Item> ALCHEMY=register("dandao_zhenjie",ALCHEMY_KEY);
    public static final RegistryObject<Item> REFINING=register("qidao_zhenjie",REFINING_KEY);
    private static RegistryObject<Item> register(String id,String key){
        return ModItems.ITEMS.register(id,()->new Item(new Item.Properties().stacksTo(1)){
            @Override public InteractionResultHolder<ItemStack> use(Level level,Player p,InteractionHand hand){
                if(p instanceof net.minecraft.server.level.ServerPlayer server)BlueprintLearningMenu.open(server,hand);
                return InteractionResultHolder.sidedSuccess(p.getItemInHand(hand),level.isClientSide);
            }
            @Override public void appendHoverText(ItemStack stack,Level level,List<Component> text,TooltipFlag flag){
                text.add(Component.literal(key.equals(ALCHEMY_KEY)?"研习后掌握一至三阶丹药炼制":"研习后掌握一至三阶装备炼制"));
                text.add(Component.literal("仍需满足材料、境界、熟练等级及设备条件"));
            }
        });
    }
    public static String knowledge(ItemStack stack){return stack.is(ALCHEMY.get())?ALCHEMY_KEY:stack.is(REFINING.get())?REFINING_KEY:"";}
    public static boolean isTreatiseName(String name){return name.equals("丹道真解")||name.equals("器道真解");}
    public static void bootstrap(){}
    private CraftingTreatises(){}
}
