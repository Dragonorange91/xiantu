package com.longju.xiantu.qa;

import com.longju.xiantu.refining.*;
import com.longju.xiantu.alchemy.*;
import com.longju.xiantu.material.*;
import com.longju.xiantu.equipment.*;
import com.longju.xiantu.spell.*;
import com.longju.xiantu.registry.ModItems;
import com.longju.xiantu.workstation.WorkstationRegistry;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Actual registered stacks, attribute events and container slots; no world/player edits. */
@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class ManufacturingRevisionAudit {
    private static int checks;
    private static void check(boolean ok,String label){checks++;if(!ok)throw new IllegalStateException(label);}
    private static double attack(ItemStack s){return 1+s.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE).stream().mapToDouble(m->m.getAmount()).sum();}
    private static boolean finalSpeed(Component line){
        if(line.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
                &&t.getKey().equals("attribute.modifier.equals.0")
                &&Arrays.stream(t.getArgs()).anyMatch(arg->arg instanceof Component attr
                    &&attr.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents a
                    &&a.getKey().equals("attribute.name.generic.attack_speed")))return true;
        return line.getSiblings().stream().anyMatch(ManufacturingRevisionAudit::finalSpeed);
    }
    private static void slots(AbstractContainerMenu menu){
        var active=menu.slots.stream().filter(s->s.isActive()).toList();
        for(int i=0;i<active.size();i++)for(int j=i+1;j<active.size();j++){
            var a=active.get(i);var b=active.get(j);check(Math.abs(a.x-b.x)>=18||Math.abs(a.y-b.y)>=18,"active slots overlap "+i+"/"+j);
        }
        for(var s:active)check(s.x>=0&&s.x+16<=com.longju.xiantu.refining.WorkstationLayout.WIDTH&&s.y>=0&&s.y+16<=com.longju.xiantu.refining.WorkstationLayout.HEIGHT,"slot outside GUI");
    }
    @SubscribeEvent public static void commands(RegisterCommandsEvent e){
        e.getDispatcher().register(Commands.literal("xianturevisionaudit").executes(c->{
            var p=c.getSource().getPlayerOrException();
            if(!p.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA")){c.getSource().sendFailure(Component.literal("仅限Xiantu-QA测试存档"));return 0;}
            checks=0;
            try{
                var items=new ArrayList<net.minecraftforge.registries.RegistryObject<Item>>(EquipmentPreview.items());items.addAll(EquipmentModelBatch.items());
                var baseline=new ItemStack(Items.NETHERITE_SWORD);int weapons=0;
                for(var item:items){
                    var raw=new ItemStack(item.get());var rawBefore=raw.save(new CompoundTag());var r=RefinedEquipment.recipe(raw);
                    check(r!=null,"missing base stats "+item.getId());check(RefinedEquipment.quality(raw)==1,"raw defaults to ordinary quality");
                    check(!raw.getHoverName().getString().contains("外观样品"),"sample suffix remains");
                    double previous=0;int durability=0;
                    for(int q=1;q<=5;q++){
                        var s=RefinedEquipment.finish(r,q);check(RefinedEquipment.quality(s)==q,"quality stored");
                        if(WeaponPassives.weapon(r)){
                            var speedModifiers=s.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_SPEED);
                            var vanillaSpeed=baseline.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_SPEED).iterator().next().getId();
                            var baseSpeed=speedModifiers.stream().filter(m->m.getId().equals(vanillaSpeed)).findFirst();
                            check(baseSpeed.isPresent(),"vanilla UUID required for final attack-speed tooltip: "+r.id());
                            double expectedSpeed=switch(r.kind()){case "axe","staff"->1;case "spear"->1.2;default->1.6;};
                            check(Math.abs(4+baseSpeed.orElseThrow().getAmount()-expectedSpeed)<1e-8,"attack speed unchanged by display fix: "+r.id());
                            var tooltip=s.getTooltipLines(p,TooltipFlag.Default.NORMAL);
                            check(tooltip.stream().anyMatch(ManufacturingRevisionAudit::finalSpeed),"native tooltip shows final speed: "+r.id()+" "+tooltip.stream().map(Component.Serializer::toJson).toList());
                            double damage=attack(s);check(damage>attack(baseline),"weapon below netherite "+r.id());
                            check(q==1||damage>previous,"higher quality must increase actual damage");previous=damage;
                            if(!RefinedEquipment.infinite(s)){check(s.getMaxDamage()>baseline.getMaxDamage(),"weapon durability below netherite");check(q==1||s.getMaxDamage()>durability,"higher quality increases durability");durability=s.getMaxDamage();}
                        }else if(s.getItem() instanceof ArmorItem armor){
                            double protection=s.getAttributeModifiers(armor.getEquipmentSlot()).get(Attributes.ARMOR).stream().mapToDouble(m->m.getAmount()).sum();
                            check(protection>previous,"quality changes actual armor defense");previous=protection;
                            double diamond=switch(armor.getType()){case HELMET,BOOTS->3;case CHESTPLATE->8;case LEGGINGS->6;};
                            check(protection>=diamond,"every armor piece at least diamond: "+r.id());
                            double toughness=s.getAttributeModifiers(armor.getEquipmentSlot()).get(Attributes.ARMOR_TOUGHNESS).stream().mapToDouble(m->m.getAmount()).sum();
                            check(Math.abs(toughness-ArmorStats.toughness(r.tier())*RefinedEquipment.multiplier(s))<1e-8,"quality-scaled toughness: "+r.id());
                            if(!r.theme().equals("齐天"))check(!EquipmentTooltips.ordinary(r.theme(),1).isEmpty(),"armor set description: "+r.id());
                        }
                    }
                    if(WeaponPassives.weapon(r)){weapons++;check(Math.abs(attack(raw)-attack(RefinedEquipment.finish(r,1)))<.0001,"raw and crafted ordinary stats differ");}
                    check(rawBefore.equals(raw.save(new CompoundTag())),"attribute query must not mutate stack NBT");
                }
                check(items.size()==93&&weapons==31,"full equipment coverage");
                var r=RefiningRecipes.find("equipment_001_tier_1");check(attack(RefinedEquipment.finish(r,1))==11&&attack(RefinedEquipment.finish(r,5))==21,"first-tier sword damage example");
                var core=new ItemStack(ModItems.DEMON_CORE.get());com.longju.xiantu.item.DemonCoreItem.setData(core,8,"fire");core.getOrCreateTag().putInt(MaterialQuality.KEY,5);
                check(!MaterialQuality.eligible(core)&&MaterialQuality.get(core)==0&&MaterialQuality.manufacturingBonus(core)==0,"cores excluded even with old quality NBT");
                MaterialQuality.set(core,5);check(!core.getTag().contains(MaterialQuality.KEY)&&com.longju.xiantu.item.DemonCoreItem.grade(core)==8,"core quality removed without changing grade");
                var be=new RefiningBlockEntity(p.blockPosition(),WorkstationRegistry.EQUIPMENT.get("qingmu_alchemy_furnace").get().defaultBlockState());
                ((net.minecraft.world.level.block.entity.BlockEntity)be).setLevel(p.serverLevel());
                AbstractContainerMenu alchemy=new AlchemyMenu(601,p.getInventory(),be);slots(alchemy);
                check(be.alchemyAux.getContainerSize()==1&&alchemy.slots.get(12).getMaxStackSize()==8&&alchemy.slots.size()==49,"single auxiliary slot with eight-core limit");
                for(boolean forge:new boolean[]{false,true}){
                    AbstractContainerMenu menu=new RefiningMenu(602,p.getInventory(),new SimpleContainer(12),p.blockPosition(),forge);slots(menu);
                    if(!forge)for(int i=0;i<8;i++){var s=menu.slots.get(4+i);check(s.x==alchemy.slots.get(4+i).x&&s.y==alchemy.slots.get(4+i).y,"both menus use the same bagua");}
                    if(!forge)check(menu.slots.get(3).x+8==256&&menu.slots.get(3).y+8==128,"fire in expanded bagua centre");
                }
                check(SpellCatalog.ALL.size()==5&&SpellScrolls.ITEMS.keySet().equals(Set.of("fire_bolt","thunder_strike","sword_flight","great_fireball","rejuvenation")),"five independent spells/scrolls registered");
                var spells=new SpellData();check(!spells.learn("water_blade")&&!spells.learn("stone_skin"),"removed spells cannot be learned");
                String message="[Manufacturing revision QA] PASS "+checks+" assertions; 93 equipment, 31 weapons, real qualities, neutral cores and bagua slots";
                org.slf4j.LoggerFactory.getLogger(ManufacturingRevisionAudit.class).info(message);p.sendSystemMessage(Component.literal(message));return 1;
            }catch(Exception|LinkageError failure){p.sendSystemMessage(Component.literal("[Manufacturing revision QA] FAIL "+checks+": "+failure));throw failure;}
        }));
    }
    private ManufacturingRevisionAudit(){}
}
