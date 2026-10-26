package com.longju.xiantu.client.equipment;

import com.longju.xiantu.refining.RefinedEquipment;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import java.util.function.Consumer;

/** Client presentation only: no attack packets, entity moves, damage calls or additional ticks. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="xiantu",value=net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class WeaponAttackAnimations {
    private static final java.util.Map<Player,State> STATES=new java.util.WeakHashMap<>();
    private static Object world;
    private static final class State {
        final WeaponAttackTimeline timeline;
        final net.minecraft.world.item.Item item;
        final HumanoidArm arm;
        int swing=-1;boolean swinging;
        State(Player p){timeline=new WeaponAttackTimeline(kind(p.getMainHandItem()));item=p.getMainHandItem().getItem();arm=p.getMainArm();}
    }
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent event){
        if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        if(mc.level!=world){STATES.clear();world=mc.level;}
        if(mc.level==null||mc.isPaused())return;
        STATES.keySet().removeIf(p->p.isRemoved()||!mc.level.players().contains(p));
        for(Player p:mc.level.players()){
            if(!allowed(p)||!WeaponAttackMotion.custom(kind(p.getMainHandItem()))){STATES.remove(p);continue;}
            State s=STATES.get(p);
            if(s==null||s.item!=p.getMainHandItem().getItem()||s.arm!=p.getMainArm()){s=new State(p);STATES.put(p,s);}
            if(p.swinging&&(!s.swinging||p.swingTime<s.swing))s.timeline.attack();
            s.swing=p.swingTime;s.swinging=p.swinging;s.timeline.tick();
        }
    }
    private static WeaponAttackMotion.Pose pose(Player player,float partial){
        if(kind(player.getMainHandItem())==WeaponAttackMotion.Kind.SPEAR) return VanillaSpearMotion.sample(player.getAttackAnim(partial));
        State s=STATES.get(player);
        return s==null||s.item!=player.getMainHandItem().getItem()||s.arm!=player.getMainArm()?WeaponAttackMotion.REST:s.timeline.pose(partial);
    }
    public static WeaponAttackMotion.Kind kind(ItemStack stack){
        var recipe=RefinedEquipment.recipe(stack);
        return recipe==null?WeaponAttackMotion.Kind.NONE:WeaponAttackMotion.kind(recipe.kind());
    }
    private static boolean allowed(Player p){
        var mc=Minecraft.getInstance();
        return !p.isUsingItem()&&!p.isSpectator()&&!p.isSwimming()&&!p.isFallFlying()&&!p.isPassenger()
            &&p.swingingArm!=InteractionHand.OFF_HAND
            &&(p!=mc.player||mc.gameMode==null||!mc.gameMode.isDestroying());
    }
    /** Forge's extension replaces just the vanilla first-person attack transform for these weapons. */
    public static void install(Consumer<IClientItemExtensions> consumer){
        consumer.accept(new IClientItemExtensions(){
            @Override public boolean applyForgeHandTransform(PoseStack stack,net.minecraft.client.player.LocalPlayer player,
                    HumanoidArm arm,ItemStack item,float partialTick,float equip,float swing){
                if(arm!=player.getMainArm()||!allowed(player))return false;
                var kind=kind(item);
                if(!WeaponAttackMotion.custom(kind))return false;
                var pose=pose(player,partialTick);
                if(arm==HumanoidArm.LEFT)pose=pose.mirrored();
                int side=arm==HumanoidArm.RIGHT?1:-1;
                stack.translate(side*.56,-.52-.6*equip,-.72);
                stack.translate(pose.x(),pose.y(),pose.z());
                stack.mulPose(Axis.YP.rotationDegrees(pose.yaw()));
                stack.mulPose(Axis.XP.rotationDegrees(kind==WeaponAttackMotion.Kind.SPEAR?VanillaSpearMotion.handPitch(pose):pose.pitch()));
                stack.mulPose(Axis.ZP.rotationDegrees(pose.roll()));
                return true;
            }
        });
    }
    /** Called in place of the humanoid's vanilla attack. Walking/crouching and armor copying stay native. */
    public static boolean thirdPerson(HumanoidModel<?> model,LivingEntity entity){
        if(com.longju.xiantu.client.SkillCastAnimations.thirdPerson(model,entity))return true;
        if(!(entity instanceof Player player)||!allowed(player))return false;
        var kind=kind(player.getMainHandItem());
        if(!WeaponAttackMotion.custom(kind))return false;
        boolean right=player.getMainArm()==HumanoidArm.RIGHT;
        var pose=pose(player,Minecraft.getInstance().getFrameTime());
        if(!right)pose=pose.mirrored();
        float radians=(float)(Math.PI/180),weight=pose.weight();
        ModelPart attack=right?model.rightArm:model.leftArm;
        attack.xRot=attack.xRot*(1-weight)+(float)((-44+Math.toDegrees(model.head.xRot)+pose.armPitch())*radians*weight);
        attack.yRot=attack.yRot*(1-weight)+pose.armYaw()*radians;
        attack.zRot=attack.zRot*(1-weight)+pose.armRoll()*radians;
        model.body.yRot=pose.bodyYaw()*radians;
        // Both vanilla player skins use shoulder pivots at +/-5; never accumulate last frame's cosine.
        float shoulder=5;
        model.rightArm.x=-(float)Math.cos(model.body.yRot)*shoulder;
        model.leftArm.x=(float)Math.cos(model.body.yRot)*shoulder;
        model.rightArm.z=(float)Math.sin(model.body.yRot)*shoulder;
        model.leftArm.z=-(float)Math.sin(model.body.yRot)*shoulder;
        return true;
    }
    private WeaponAttackAnimations(){}
}
