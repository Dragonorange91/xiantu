package example.xiantucompat;

import com.longju.xiantu.alchemy.PillCatalog;
import com.longju.xiantu.cultivation.CultivationCapability;
import com.longju.xiantu.cultivation.CultivationEvents;
import com.longju.xiantu.item.DemonCoreItem;
import com.longju.xiantu.refining.EquipmentSkillEvent;
import com.longju.xiantu.refining.RefinedEquipment;
import com.longju.xiantu.refining.RefiningRecipes;
import com.longju.xiantu.registry.ModItems;
import com.longju.xiantu.spell.SpellLifecycleEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Called by the integration mod on the server thread, after authorization. */
public final class XiantuIntegrationExamples {
    public static boolean atLeastRealm(ServerPlayer player, int majorRealm) {
        return player.getCapability(CultivationCapability.DATA)
                .map(data -> data.cultivationUnlocked() && data.majorRealm() >= majorRealm)
                .orElse(false);
    }

    public static boolean trySpendPower(ServerPlayer player, double amount) {
        if (!Double.isFinite(amount) || amount < 0 || !player.isAlive()) return false;
        boolean paid = player.getCapability(CultivationCapability.DATA).map(data -> {
            if (!data.cultivationUnlocked() || data.power() < amount) return false;
            data.setPower(data.power() - amount);
            return true;
        }).orElse(false);
        if (paid) CultivationEvents.sync(player);
        return paid;
    }

    public static double rewardProgress(ServerPlayer player, double amount) {
        if (!Double.isFinite(amount) || amount <= 0 || !player.isAlive()) return 0;
        double gained = player.getCapability(CultivationCapability.DATA).map(data -> {
            if (!data.cultivationUnlocked()) return 0.0;
            double before = data.progress();
            data.addProgress(amount);
            return data.progress() - before;
        }).orElse(0.0);
        if (gained > 0) CultivationEvents.sync(player);
        return gained;
    }

    public static ItemStack pill(String catalogId, int quality) {
        if (PillCatalog.ALL.stream().noneMatch(row -> row.id().equals(catalogId)))
            return ItemStack.EMPTY;
        return PillCatalog.stack(catalogId, quality);
    }

    public static ItemStack equipment(String recipeId, int quality) {
        var recipe = RefiningRecipes.find(recipeId);
        return recipe == null ? ItemStack.EMPTY : RefinedEquipment.finish(recipe, quality);
    }

    public static ItemStack demonCore(int tier, String element) {
        if (tier < 1 || tier > 9 || !DemonCoreItem.ELEMENTS.contains(element))
            return ItemStack.EMPTY;
        var stack = new ItemStack(ModItems.DEMON_CORE.get());
        DemonCoreItem.setData(stack, tier, element);
        return stack;
    }

    // Register an instance on MinecraftForge.EVENT_BUS, only after Xiantu is loaded.
    public static final class PresentationListener {
        @SubscribeEvent public void onSpell(SpellLifecycleEvent event) {
            if (event.phase != SpellLifecycleEvent.Phase.RELEASE) return;
            // Send this integration mod's own presentation packet. Do not deal damage here.
        }

        @SubscribeEvent public void onEquipment(EquipmentSkillEvent event) {
            if (event.phase != EquipmentSkillEvent.Phase.RELEASE) return;
            // event.target may be null; group effects by event.transaction.
        }
    }

    private XiantuIntegrationExamples() {}
}
