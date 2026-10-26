package com.longju.xiantu.refining;

import com.longju.xiantu.cultivation.CultivationCapability;
import com.longju.xiantu.material.MaterialQuality;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.UUID;

/** Finished equipment retains the accepted model and carries independent level/quality data. */
@Mod.EventBusSubscriber(modid="xiantu")
public final class RefinedEquipment {
    public static final String KEY="xiantu_refined";
    private static final double[] MULT={1,1.15,1.35,1.6,2};
    private static final int[] DURABILITY={100,300,1000,3000,10000,30000,100000};
    private static final int[] WEAPON_DURABILITY={2500,5000,10000,20000,40000,80000,160000};
    public static boolean infinite(ItemStack s){
        var id=net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(s.getItem());
        return id!=null&&id.getNamespace().equals("xiantu")&&(id.getPath().equals("preview_jingubang")||java.util.Set.of("preview_qitian_helmet","preview_qitian_chestplate","preview_qitian_leggings","preview_qitian_boots").contains(id.getPath()));
    }
    public static RefiningRecipes.Recipe recipe(ItemStack stack){
        if(!stack.hasTag()||!stack.getTag().contains(KEY))return RefiningRecipes.baseEquipment(String.valueOf(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem())));
        var r=RefiningRecipes.definition(stack.getTag().getCompound(KEY).getString("Recipe"));
        return r!=null&&r.output().equals(String.valueOf(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem())))?r:null;
    }
    public static int quality(ItemStack s){return s.hasTag()?Math.max(1,Math.min(5,s.getTag().getCompound(KEY).getInt("Quality"))):1;}
    public static double multiplier(ItemStack s){return MULT[quality(s)-1];}
    public static int durability(ItemStack s,int fallback){var r=recipe(s);return r==null?fallback:(int)Math.floor((WeaponPassives.weapon(r)?WEAPON_DURABILITY:DURABILITY)[r.tier()-1]*multiplier(s)*(r.theme().equals("混沌斧")?1+.1*WeaponPassives.scale(s):1)+1e-8);}
    public static boolean broken(ItemStack s){return !infinite(s)&&s.isDamageableItem()&&s.getMaxDamage()>0&&s.getDamageValue()>=s.getMaxDamage();}
    public static int minimumRealm(RefiningRecipes.Recipe r){return r.theme().equals("齐天")?3:RefiningBlockEntity.realmNeeded(r.tier());}
    public static boolean usable(Player p,ItemStack s){
        var r=recipe(s);if(r==null)return true;if(broken(s))return false;
        // Local presentation uses the same synchronized data as the cultivation HUD.
        var d=p.level().isClientSide?CultivationCapability.clientData():p.getCapability(CultivationCapability.DATA).orElse(null);
        return d!=null&&d.cultivationUnlocked()&&d.majorRealm()>=minimumRealm(r);
    }
    public static ItemStack finish(RefiningRecipes.Recipe r,int quality){
        var s=r.result();var n=new net.minecraft.nbt.CompoundTag();n.putString("Recipe",r.id());n.putInt("Quality",Math.max(1,Math.min(5,quality)));s.getOrCreateTag().put(KEY,n);return s;
    }
    public static UUID modifierId(String key,EquipmentSlot slot){return UUID.nameUUIDFromBytes(("xiantu.refined."+key+slot).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    private static void add(ItemAttributeModifierEvent e,Attribute attribute,String key,double value){
        // Vanilla recognizes its base-speed UUID and displays base + modifier, rather than a negative delta.
        UUID id=attribute==Attributes.ATTACK_SPEED&&key.equals("speed")
                ?Items.IRON_SWORD.getDefaultAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_SPEED).iterator().next().getId()
                :modifierId(key,e.getSlotType());
        e.addModifier(attribute,new AttributeModifier(id,"炼器属性",value,AttributeModifier.Operation.ADDITION));
    }
    @SubscribeEvent public static void attributes(ItemAttributeModifierEvent e){
        var r=recipe(e.getItemStack());if(r==null)return;int t=r.tier()-1;double q=multiplier(e.getItemStack());
        if(e.getItemStack().getItem() instanceof ArmorItem armor && e.getSlotType()==armor.getEquipmentSlot()){
            e.removeAttribute(Attributes.ARMOR);e.removeAttribute(Attributes.ARMOR_TOUGHNESS);
            add(e,Attributes.ARMOR,"armor",ArmorStats.protection(r.tier(),armor.getType().getName())*q);
            add(e,Attributes.ARMOR_TOUGHNESS,"toughness",ArmorStats.toughness(r.tier())*q);
        }else if(e.getSlotType()==EquipmentSlot.MAINHAND && WeaponPassives.weapon(r)){
            e.removeAttribute(Attributes.ATTACK_DAMAGE);e.removeAttribute(Attributes.ATTACK_SPEED);
            double factor=r.kind().equals("axe")?1.3:r.kind().equals("spear")?.9:1;
            add(e,Attributes.ATTACK_DAMAGE,"damage",Math.round(new int[]{10,16,25,38,56,80,112}[t]*q*factor*100)/100.0+(r.id().equals("jingu_staff")&&q==2?12:0));
            add(e,Attributes.ATTACK_SPEED,"speed",(r.kind().equals("axe")||r.kind().equals("staff")?1.0:r.kind().equals("spear")?1.2:1.6)-4);
            if(r.kind().equals("spear"))add(e,net.minecraftforge.common.ForgeMod.ENTITY_REACH.get(),"reach",1);
            if(r.kind().equals("staff"))add(e,net.minecraftforge.common.ForgeMod.ENTITY_REACH.get(),"reach",1.5);
            WeaponPassives.attributes(e,r);
        }
    }
    @SubscribeEvent public static void attack(AttackEntityEvent e){if(!usable(e.getEntity(),e.getEntity().getMainHandItem())){e.setCanceled(true);e.getEntity().displayClientMessage(Component.literal("境界不足，无法使用这件装备"),true);}}
    /** Native equipment updates only notice stack changes; a realm rollback must also revoke attributes. */
    public static void refreshEligibility(Player player){
        for(var slot:EquipmentSlot.values()){
            var stack=player.getItemBySlot(slot);if(recipe(stack)==null)continue;
            var modifiers=stack.getAttributeModifiers(slot);
            if(!usable(player,stack)){player.getAttributes().removeAttributeModifiers(modifiers);continue;}
            for(var entry:modifiers.entries()){
                var attribute=player.getAttribute(entry.getKey());
                if(attribute!=null&&attribute.getModifier(entry.getValue().getId())==null)attribute.addTransientModifier(entry.getValue());
            }
        }
    }
    @SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.PlayerTickEvent e){if(e.phase==net.minecraftforge.event.TickEvent.Phase.END&&!e.player.level().isClientSide)refreshEligibility(e.player);}
    @SubscribeEvent public static void tooltip(ItemTooltipEvent e){
        var r=recipe(e.getItemStack());if(r==null)return;int q=quality(e.getItemStack());
        e.getToolTip().set(0,e.getToolTip().get(0).copy().append(" · "+r.tier()+"阶 · "+MaterialQuality.NAMES[Math.max(1,Math.min(5,q))]));
        e.getToolTip().add(Component.literal("品质属性倍率 ×"+multiplier(e.getItemStack())).withStyle(net.minecraft.ChatFormatting.AQUA));
        e.getToolTip().add(Component.literal("最低使用境界："+com.longju.xiantu.cultivation.RealmRegistry.MAJOR_REALM_NAMES.get(minimumRealm(r))).withStyle(net.minecraft.ChatFormatting.GRAY));
    }
    public static class Sword extends SwordItem {
        public Sword(Tier tier,int attack,float speed,Properties p){super(tier,attack,speed,p);}
        @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer){com.longju.xiantu.client.equipment.WeaponAttackAnimations.install(consumer);}
        @Override public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level l,Player p,net.minecraft.world.InteractionHand hand){var special=QitianSkills.use(l,p,hand);if(special.getResult()!=net.minecraft.world.InteractionResult.PASS)return special;var result=WeaponConditions.use(l,p,hand);return result.getResult()==net.minecraft.world.InteractionResult.PASS?super.use(l,p,hand):result;}
        @Override public int getUseDuration(ItemStack s){var r=recipe(s);return r!=null&&r.tier()>=5&&(r.theme().equals("开天斧")||r.theme().equals("贯日枪"))?72000:super.getUseDuration(s);}
        @Override public void releaseUsing(ItemStack s,net.minecraft.world.level.Level l,net.minecraft.world.entity.LivingEntity e,int remaining){var r=recipe(s);if(r!=null&&r.id().equals("jingu_staff"))QitianSkills.releaseStaff(s,e,remaining);else WeaponConditions.release(s,l,e,remaining);}

        @Override public int getMaxDamage(ItemStack s){return infinite(s)?0:durability(s,super.getMaxDamage(s));}
        @Override public <T extends net.minecraft.world.entity.LivingEntity> int damageItem(ItemStack s,int amount,T e,java.util.function.Consumer<T> broken){return WeaponPassives.durabilityCost(s,amount,e);}
    }
    public static class Axe extends AxeItem {
        public Axe(Tier tier,float attack,float speed,Properties p){super(tier,attack,speed,p);}
        @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer){com.longju.xiantu.client.equipment.WeaponAttackAnimations.install(consumer);}
        @Override public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level l,Player p,net.minecraft.world.InteractionHand hand){var special=QitianSkills.use(l,p,hand);if(special.getResult()!=net.minecraft.world.InteractionResult.PASS)return special;var result=WeaponConditions.use(l,p,hand);return result.getResult()==net.minecraft.world.InteractionResult.PASS?super.use(l,p,hand):result;}
        @Override public int getUseDuration(ItemStack s){var r=recipe(s);return r!=null&&r.tier()>=5&&(r.theme().equals("开天斧")||r.theme().equals("贯日枪"))?72000:super.getUseDuration(s);}
        @Override public void releaseUsing(ItemStack s,net.minecraft.world.level.Level l,net.minecraft.world.entity.LivingEntity e,int remaining){var r=recipe(s);if(r!=null&&r.id().equals("jingu_staff"))QitianSkills.releaseStaff(s,e,remaining);else WeaponConditions.release(s,l,e,remaining);}

        @Override public int getMaxDamage(ItemStack s){return durability(s,super.getMaxDamage(s));}
        @Override public <T extends net.minecraft.world.entity.LivingEntity> int damageItem(ItemStack s,int amount,T e,java.util.function.Consumer<T> broken){return WeaponPassives.durabilityCost(s,amount,e);}
        @Override public boolean mineBlock(ItemStack s,net.minecraft.world.level.Level l,net.minecraft.world.level.block.state.BlockState state,net.minecraft.core.BlockPos pos,net.minecraft.world.entity.LivingEntity e){
            WeaponPassives.beginMining(state);try{return super.mineBlock(s,l,state,pos,e);}finally{WeaponPassives.endMining();}
        }
    }
    private RefinedEquipment(){}
}
