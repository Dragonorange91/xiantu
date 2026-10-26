package com.longju.xiantu.alchemy;

import com.longju.xiantu.cultivation.*;
import com.longju.xiantu.item.PillItem;
import com.longju.xiantu.registry.ModMenus;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkHooks;
import java.util.*;

/** Read-only choices. Buttons carry only a server-issued index, never item NBT/effect values. */
public final class PillChoiceMenu extends AbstractContainerMenu {
    public final int purpose;
    public final List<String> labels;
    public final String summary;
    private final List<ItemStack> snapshots;
    private final List<Integer> slots;
    public PillChoiceMenu(int id,Inventory inv,FriendlyByteBuf buf){
        super(ModMenus.PILL_CHOICE.get(),id);purpose=buf.readVarInt();summary=buf.readUtf();
        int n=buf.readVarInt();if(n<0||n>128)throw new IllegalArgumentException("Invalid pill choices");
        labels=new ArrayList<>();for(int i=0;i<n;i++)labels.add(buf.readUtf());slots=List.of();snapshots=List.of();
    }
    private PillChoiceMenu(int id,Inventory inv,int purpose){
        super(ModMenus.PILL_CHOICE.get(),id);this.purpose=purpose;labels=new ArrayList<>();snapshots=new ArrayList<>();slots=new ArrayList<>();
        ServerPlayer p=(ServerPlayer)inv.player;var m=MedicineService.data(p).medicine();
        summary=purpose==0?selectionText(p):"当前体质："+MedicineService.data(p).physique().displayName;
        if(purpose==1&&m.state().getIntArray("physique_candidates").length>0){
            for(int i:m.state().getIntArray("physique_candidates"))labels.add("选择 "+PhysiqueType.values()[i].displayName);
        }else for(int i=0;i<inv.getContainerSize();i++){
            ItemStack s=inv.getItem(i);if(!(s.getItem() instanceof PillItem pill))continue;
            if(purpose==0&&!pill.definition().breakthrough()||purpose==1&&!pill.definition().kind().equals("physique"))continue;
            slots.add(i);snapshots.add(s.copyWithCount(1));labels.add((purpose==1?"确认消耗：":"选用：")+s.getHoverName().getString()+" ×"+s.getCount());
        }
    }
    public static void open(ServerPlayer p,int purpose){
        NetworkHooks.openScreen(p,new SimpleMenuProvider((id,inv,player)->new PillChoiceMenu(id,inv,purpose),
            Component.literal(purpose==0?"突破用丹":"洗髓择体")),buf->{
                PillChoiceMenu menu=new PillChoiceMenu(0,p.getInventory(),purpose);buf.writeVarInt(purpose);buf.writeUtf(menu.summary);
                buf.writeVarInt(menu.labels.size());for(String label:menu.labels)buf.writeUtf(label);
            });
    }
    public static String selectionText(ServerPlayer p){var s=MedicineService.data(p).medicine().state();return "主丹："+selectedName(s.getString("main_pill"),s.getInt("main_quality"))+" / 辅丹："+selectedName(s.getString("aux_pill"),s.getInt("aux_quality"));}
    private static String selectedName(String id,int q){return id.isEmpty()?"无":PillCatalog.get(id).name()+"·"+PillCatalog.QUALITIES[Math.max(1,Math.min(5,q))-1];}
    @Override public boolean clickMenuButton(Player player,int button){
        if(!(player instanceof ServerPlayer p)||p.containerMenu!=this)return false;
        var c=MedicineService.data(p);var m=c.medicine();var tag=m.state();
        if(button==100){
            if(purpose==0){tag.remove("main_pill");tag.remove("aux_pill");}
            else tag.remove("physique_candidates");open(p,purpose);return true;
        }
        if(button<0||button>=labels.size())return false;
        int[] candidates=tag.getIntArray("physique_candidates");
        if(purpose==1&&candidates.length>0){
            if(button>=candidates.length)return false;
            if(c instanceof PlayerCultivationData mutable){mutable.setPhysique(PhysiqueType.values()[candidates[button]]);tag.remove("physique_candidates");
                CultivationService.applyRealmAttributes(p,c);com.longju.xiantu.gongfa.GongfaService.refresh(p);CultivationEvents.sync(p);p.closeContainer();return true;}
            return false;
        }
        if(button>=slots.size())return false;ItemStack stack=p.getInventory().getItem(slots.get(button));
        if(!ItemStack.isSameItemSameTags(stack,snapshots.get(button)))return false;
        var d=((PillItem)stack.getItem()).definition();
        if(purpose==0){
            if(!eligible(d,c)){p.sendSystemMessage(Component.literal("这枚丹药不适用于当前突破"));return false;}
            String key=d.kind().equals("auxiliary")?"aux":"main";tag.putString(key+"_pill",d.id());tag.putInt(key+"_quality",PillCatalog.quality(stack));
        }else{
            String reason=MedicineService.denial(p,stack);if(!reason.isEmpty()){p.sendSystemMessage(Component.literal(reason));return false;}
            int n=(int)d.value(PillCatalog.quality(stack));double[] weights=PhysiqueService.weights();List<Integer> choices=new ArrayList<>();
            for(int k=0;k<n;k++){var type=PhysiqueType.roll(p.getRandom().nextDouble(),weights);choices.add(type.ordinal());weights[type.ordinal()]=0;}
            MedicineService.commitDose(p,stack,d,PillCatalog.quality(stack));tag.putIntArray("physique_candidates",choices.stream().mapToInt(Integer::intValue).toArray());CultivationEvents.sync(p);
        }
        open(p,purpose);return true;
    }
    public static boolean eligible(PillCatalog.Definition d,CultivationData c){
        if(d.kind().equals("auxiliary"))return true;
        boolean major=BreakthroughRules.isMajorBreakthrough(c.majorRealm(),c.stage());
        return switch(d.id()){
            case "pill_01"->c.majorRealm()==0&&!major;
            case "pill_02"->major&&c.majorRealm()==0;case "pill_06"->major&&c.majorRealm()==1;
            case "pill_15"->major&&c.majorRealm()==2;case "pill_19"->major&&c.majorRealm()==3;
            case "pill_26"->major&&c.majorRealm()==4;case "pill_30"->major&&(c.majorRealm()==5||c.majorRealm()==6);
            case "pill_34"->major&&c.majorRealm()==7;default->false;};
    }
    private static ItemStack selected(ServerPlayer p,String key){
        var c=MedicineService.data(p);var tag=c.medicine().state();String id=tag.getString(key+"_pill");int q=tag.getInt(key+"_quality");
        if(id.isEmpty())return ItemStack.EMPTY;
        for(int i=0;i<p.getInventory().getContainerSize();i++){var s=p.getInventory().getItem(i);
            if(s.getItem() instanceof PillItem pill&&pill.definition().id().equals(id)&&PillCatalog.quality(s)==q&&eligible(pill.definition(),c))return s;}
        return ItemStack.EMPTY;
    }
    public static double bonus(ServerPlayer p,boolean consume){
        double amount=0;
        for(String key:List.of("main","aux")){ItemStack s=selected(p,key);if(s.isEmpty())continue;var d=((PillItem)s.getItem()).definition();int q=PillCatalog.quality(s);
            amount+=d.value(q)/100;if(consume)MedicineService.commitDose(p,s,d,q);}
        return amount;
    }
    @Override public ItemStack quickMoveStack(Player p,int slot){return ItemStack.EMPTY;}
    @Override public boolean stillValid(Player p){return p.isAlive();}
}
