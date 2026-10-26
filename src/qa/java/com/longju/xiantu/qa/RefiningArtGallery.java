package com.longju.xiantu.qa;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.Commands;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.ArrayList;
import java.util.List;

/** Disposable client-only gallery: renders registered items without altering player inventory. */
@Mod.EventBusSubscriber(modid = "xiantu_qa", value = Dist.CLIENT)
public final class RefiningArtGallery {
    private static final List<String> IDS = List.of("refining_scrap", "equipment_blueprint",
        "condensed_spirit_fire", "wondrous_fire_1", "wondrous_fire_2", "wondrous_fire_3",
        "wondrous_fire_4", "wondrous_fire_5", "wondrous_fire_6");

    @SubscribeEvent public static void commands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("xianturefiningart").executes(context -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return 0;
            List<String> failures = new ArrayList<>();
            for (String id : IDS) {
                ItemStack stack = stack(id);
                var model = mc.getItemRenderer().getModel(stack, mc.level, mc.player, 0);
                boolean valid = !stack.isEmpty() && model != mc.getModelManager().getMissingModel();
                int quads = 0;
                var sides = new ArrayList<Direction>(List.of(Direction.values()));
                sides.add(null);
                for (Direction side : sides) {
                    for (var quad : model.getQuads(null, side, RandomSource.create(42))) {
                        quads++;
                        valid &= quad.getSprite().contents().name().equals(new ResourceLocation("xiantu", "item/" + id));
                    }
                }
                if (!valid || quads == 0) failures.add(id);
            }
            org.slf4j.LoggerFactory.getLogger(RefiningArtGallery.class).info(
                "[Refining art QA] {} models, {} failures: {}", IDS.size(), failures.size(), failures);
            mc.execute(() -> mc.setScreen(new Gallery()));
            return failures.isEmpty() ? 1 : 0;
        }));
    }

    private static ItemStack stack(String id) {
        return new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("xiantu", id)));
    }

    private static final class Gallery extends Screen {
        Gallery() { super(Component.literal("炼器素材 · 游戏内模型预览")); }
        @Override public void render(GuiGraphics graphics, int mx, int my, float tick) {
            renderBackground(graphics);
            graphics.drawCenteredString(font, title, width / 2, 8, 0xfff1d9a8);
            int cellWidth = Math.min(138, (width - 12) / 3);
            int cellHeight = Math.min(95, (height - 38) / 3);
            int left = (width - cellWidth * 3) / 2;
            for (int i = 0; i < IDS.size(); i++) {
                int x = left + (i % 3) * cellWidth, y = 30 + (i / 3) * cellHeight;
                ItemStack item = stack(IDS.get(i));
                graphics.drawCenteredString(font, item.getHoverName(), x + cellWidth / 2, y, 0xffe2dcd0);
                graphics.pose().pushPose();
                graphics.pose().translate(x + cellWidth / 2 - 32, y + 13, 0);
                graphics.pose().scale(3, 3, 1);
                graphics.renderItem(item, 0, 0);
                graphics.pose().popPose();
                graphics.renderItem(item, x + cellWidth / 2 + 24, y + 30);
                if (mx >= x && mx < x + cellWidth && my >= y && my < y + cellHeight)
                    graphics.renderTooltip(font, item, mx, my);
            }
            super.render(graphics, mx, my, tick);
        }
        @Override public boolean isPauseScreen() { return false; }
    }
}
