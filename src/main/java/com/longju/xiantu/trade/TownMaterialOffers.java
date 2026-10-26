package com.longju.xiantu.trade;

import com.longju.xiantu.mining.MetalMaterialRegistry;
import com.longju.xiantu.medicine.MedicineCatalog;
import com.longju.xiantu.wood.SpiritWoodType;
import java.util.List;

/** Whitelisted ordinary resources: boss relics, demon cores and equipment are not scrap. */
final class TownMaterialOffers {
    static void append(List<TownTradeService.Offer> out){
        for(var m:MetalMaterialRegistry.ALL){
            if(m.tier()<=3)buy(out,"smith_merchant",m.id()+"_ingot",m.tier(),2,4);
            else {recycle(out,m.id()+"_ingot",m.tier(),4);recycle(out,m.id()+"_raw",m.tier(),8);}
        }
        for(var herb:MedicineCatalog.HERBS){
            if(herb.tier()<=3)buy(out,"alchemy_merchant",herb.registryId(),herb.tier(),2,4);
            else recycle(out,herb.registryId(),herb.tier(),8);
        }
        for(var wood:SpiritWoodType.values()){
            if(wood.rank()<=3){
                buy(out,"smith_merchant",wood.id+"_planks",wood.rank(),1,8);
                buy(out,"smith_merchant",wood.id+"_long_shaft",wood.rank(),1,4);
                buy(out,"talisman_merchant",wood.id+"_fiber",wood.rank(),1,8);
            }else{
                recycle(out,wood.id+"_log",wood.rank(),4);recycle(out,wood.id+"_stripped_log",wood.rank(),4);
                recycle(out,wood.id+"_planks",wood.rank(),16);recycle(out,wood.id+"_fiber",wood.rank(),16);
            }
        }
        buy(out,"talisman_merchant","qing_wen_elm_stick",1,1,4);
        buy(out,"talisman_merchant","beast_silk",1,2,4);
        for(String key:List.of("purple_pattern_crystal","flame_heart_stone")){
            buy(out,"talisman_merchant",key,2,2,2);
            buy(out,"smith_merchant",key,2,2,2);
        }
        out.add(new TownTradeService.Offer("material/beast_hide/basic","smith_merchant","material_hide","beast_hide",1,2,0,4));
        out.add(new TownTradeService.Offer("recycle/beast_hide/intermediate","wanbao_merchant","recycle_hide","beast_hide",4,1,0,8));
        out.add(new TownTradeService.Offer("recycle/beast_hide/advanced","wanbao_merchant","recycle_hide","beast_hide",6,1,0,8));
        for(int tier=1;tier<=3;tier++)for(String key:List.of("beast_bone","beast_sinew"))
            out.add(new TownTradeService.Offer("material/"+key+"/"+tier,"smith_merchant","beast_material",key,tier,2,0,4));
        for(int tier=4;tier<=8;tier++)for(String key:List.of("beast_bone","beast_fang","beast_claw","beast_sinew","beast_scale","beast_horn","beast_venom_sac","beast_silk","beast_feather","beast_blood_sac","beast_carapace"))
            out.add(new TownTradeService.Offer("recycle/"+key+"/"+tier,"wanbao_merchant","recycle_beast",key,tier,1,0,4));
    }
    private static void buy(List<TownTradeService.Offer> out,String shop,String key,int tier,int price,int count){out.add(new TownTradeService.Offer("material/"+key,shop,"material",key,tier,price,0,count));}
    private static void recycle(List<TownTradeService.Offer> out,String key,int tier,int count){out.add(new TownTradeService.Offer("recycle/"+key,"wanbao_merchant","recycle",key,tier,1,0,count));}
    private TownMaterialOffers(){}
}
