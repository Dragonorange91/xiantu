package com.longju.xiantu.qa;

import com.google.gson.GsonBuilder;
import com.longju.xiantu.equipment.EquipmentModelBatch;
import com.longju.xiantu.equipment.EquipmentPreview;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.commands.Commands;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.nio.file.Files;
import java.util.*;

/** QA-only actual client resources and render gallery. Never changes player gear or world blocks. */
@Mod.EventBusSubscriber(modid="xiantu_qa", value=Dist.CLIENT)
public final class EquipmentVisualAudit {
    static List<ItemStack> samples() {
        List<ItemStack> list=new ArrayList<>();
        EquipmentPreview.items().forEach(i->list.add(new ItemStack(i.get())));
        EquipmentModelBatch.items().forEach(i->list.add(new ItemStack(i.get())));
        return list;
    }
    @SubscribeEvent public static void commands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("xiantuequipmentaudit").executes(c->audit()));
        event.getDispatcher().register(Commands.literal("xiantuequipmentgallery").executes(c->{
            Minecraft.getInstance().execute(()->Minecraft.getInstance().setScreen(new Gallery()));return 1;
        }));
    }
    private static void require(boolean value,String message) {
        if(!value)throw new IllegalStateException(message);
    }
    static int audit() {
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null)return 0;
        List<Map<String,Object>> rows=new ArrayList<>();List<String> failures=new ArrayList<>();
        for(ItemStack stack:samples()) {
            String id=ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();Map<String,Object> row=new LinkedHashMap<>();row.put("id",id);
            try {
                var baked=mc.getItemRenderer().getModel(stack,mc.level,mc.player,0);
                require(baked!=mc.getModelManager().getMissingModel(),id+" missing item model");
                Map<String,Integer> quads=new LinkedHashMap<>();
                for(var context:List.of(ItemDisplayContext.GUI,ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)) {
                    var posed=baked.applyTransform(context,new PoseStack(),false);int count=0;
                    List<Direction> sides=new ArrayList<>(Arrays.asList(Direction.values()));sides.add(null);
                    for(Direction side:sides)for(var quad:posed.getQuads(null,side,RandomSource.create(42))) {
                        require(!quad.getSprite().contents().name().getPath().equals("missingno"),id+" missing sprite in "+context);
                        count++;
                    }
                    require(count>0,id+" empty baked model in "+context);quads.put(context.toString(),count);
                }
                row.put("bakedQuads",quads);
                if(stack.getItem() instanceof ArmorItem armor) {
                    EquipmentSlot slot=armor.getEquipmentSlot();
                    require(mc.player.inventoryMenu.slots.get(8-slot.getIndex()).mayPlace(stack),id+" actual client armor slot rejected");
                    require(!mc.player.inventoryMenu.slots.get(slot==EquipmentSlot.HEAD?6:5).mayPlace(stack),id+" wrong client armor slot accepted");
                    HumanoidModel<LivingEntity> original=new HumanoidModel<>(LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE,0),64,64).bakeRoot());
                    var actual=IClientItemExtensions.of(stack).getHumanoidArmorModel(mc.player,stack,slot,original);
                    require(actual!=null&&actual!=original,id+" native armor model fell back to vanilla");
                    String texture=stack.getItem().getArmorTexture(stack,mc.player,slot,null);
                    require(texture!=null&&mc.getResourceManager().getResource(new ResourceLocation(texture)).isPresent(),id+" missing armor texture");
                    row.put("armorModel",actual.getClass().getName());row.put("armorTexture",texture);
                }
                row.put("status","passed");
            } catch(Exception ex) {row.put("status","failed");row.put("error",ex.toString());failures.add(id+": "+ex);}
            rows.add(row);
        }
        Map<String,Object> result=new LinkedHashMap<>();result.put("items",rows);result.put("count",rows.size());result.put("failures",failures);
        result.put("limitation","Loaded client models/sprites/armor resources only; visual quality and frame time require separate inspection.");
        try {var dir=mc.gameDirectory.toPath().resolve("xiantu-qa-equipment");Files.createDirectories(dir);Files.writeString(dir.resolve("runtime-resources.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result));}
        catch(Exception ex){LogUtils.getLogger().error("Cannot save equipment QA",ex);return 0;}
        mc.player.displayClientMessage(Component.literal("Equipment client resources: "+rows.size()+" items, "+failures.size()+" failures"),false);
        LogUtils.getLogger().info("[Xiantu equipment audit] {} items, {} failures",rows.size(),failures.size());
        return failures.isEmpty()&&rows.size()==93?1:0;
    }
    private static final class Gallery extends Screen {
        final List<ItemStack> items=samples();final List<List<ItemStack>> sets=new ArrayList<>();
        int page=0,set=0;ArmorStand display;
        Gallery(){super(Component.literal("装备模型检查（QA）"));
            sets.add(items.subList(1,5));sets.add(items.subList(6,10));
            for(int n=35;n<=67;n+=4){List<ItemStack> suit=new ArrayList<>();for(int i=n;i<n+4;i++)suit.add(new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("xiantu",String.format(Locale.ROOT,"preview_equipment_%03d",i)))));sets.add(suit);}
        }
        @Override protected void init(){
            display=new ArmorStand(minecraft.level,0,0,0);display.setNoGravity(true);wear();
            addRenderableWidget(Button.builder(Component.literal("上一页"),b->page=Math.floorMod(page-1,(items.size()+19)/20)).bounds(12,height-27,70,20).build());
            addRenderableWidget(Button.builder(Component.literal("下一页"),b->page=(page+1)%((items.size()+19)/20)).bounds(86,height-27,70,20).build());
            addRenderableWidget(Button.builder(Component.literal("上一套"),b->{set=Math.floorMod(set-1,sets.size());wear();}).bounds(width-154,height-27,70,20).build());
            addRenderableWidget(Button.builder(Component.literal("下一套"),b->{set=(set+1)%sets.size();wear();}).bounds(width-80,height-27,70,20).build());
        }
        void wear(){if(display==null)return;for(ItemStack item:sets.get(set)){ArmorItem armor=(ArmorItem)item.getItem();display.setItemSlot(armor.getEquipmentSlot(),item.copy());}}
        @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partial){
            graphics.fill(0,0,width,height,0xff15202c);graphics.drawString(font,title,12,10,0xffffff);
            graphics.drawString(font,"模型图标 "+(page+1)+"/"+((items.size()+19)/20),12,25,0xbed7e8);
            int area=Math.max(200,width-170),cell=area/5,step=Math.max(38,(height-70)/4);
            for(int k=0;k<20;k++){int i=page*20+k;if(i>=items.size())break;int x=12+(k%5)*cell,y=44+(k/5)*step;ItemStack stack=items.get(i);
                graphics.renderItem(stack,x+Math.max(0,(cell-32)/2),y);
                String name=stack.getHoverName().getString().replace("（外观样品）","");graphics.drawString(font,font.plainSubstrByWidth(name,cell-5),x,y+20,0xffffff);
                if(mouseX>=x&&mouseX<x+cell&&mouseY>=y&&mouseY<y+step)graphics.renderTooltip(font,stack,mouseX,mouseY);
            }
            if(display!=null){int x=width-85,y=height-47,scale=Math.max(22,Math.min(55,(height-78)/3));
                InventoryScreen.renderEntityInInventoryFollowsMouse(graphics,x,y,scale,x-mouseX,y-65-mouseY,display);
                graphics.drawString(font,"护甲 "+(set+1)+"/"+sets.size(),width-140,25,0xbed7e8);
            }
            super.render(graphics,mouseX,mouseY,partial);
        }
        @Override public boolean isPauseScreen(){return false;}
    }
    private EquipmentVisualAudit(){}
}
