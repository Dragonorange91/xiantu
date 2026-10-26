package com.longju.xiantu.trade;

import com.longju.xiantu.gongfa.GongfaService;
import com.longju.xiantu.item.SpiritStoneItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.ArrayList;
import java.util.List;

public final class TownTradeService {
    private TownTradeService() {}
    public static final List<String> BOOKS=List.of("jin_guang_jue","qing_mu_jue","xuan_shui_jue","chi_yan_jue","yan_tu_jue");
    public record Offer(String id,String shop,String kind,String key,int rank,int price,int purchases,int amount){
        public Offer(String id,String shop,String kind,String key,int rank,int price,int purchases){this(id,shop,kind,key,rank,price,purchases,1);}
        public boolean recycling(){return kind.startsWith("recycle");}
        public boolean exchange(){return kind.equals("exchange");}
        public String category(){return recycling()?"回收":exchange()?"兑换":kind.equals("material")||kind.equals("beast_material")||kind.equals("material_hide")?"材料":"图籍成品";}
        public ItemStack stack(){
            var stack=switch(kind){
                case "spell"->com.longju.xiantu.spell.SpellScrolls.stack(key);
                case "talisman_book"->com.longju.xiantu.talisman.TalismanItems.blueprint(key);
                case "talisman"->com.longju.xiantu.talisman.TalismanItems.symbol(key,key.equals("rebirth")?3:com.longju.xiantu.talisman.TalismanCatalog.find(key).minimum(),key.equals("rebirth")?1:3);
                case "facility"->com.longju.xiantu.refining.RefiningItems.facilityDiagram(key);
                case "alchemy"->com.longju.xiantu.refining.RefiningItems.alchemyDiagram(key);
                case "gear"->{var s=new ItemStack(com.longju.xiantu.refining.RefiningItems.BLUEPRINT.get());s.getOrCreateTag().putString("Recipe",key);s.getOrCreateTag().putInt("BlueprintVersion",1);yield s;}
                default->new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("xiantu",key)));
            };
            stack.setCount(amount);
            if(kind.equals("recycle_beast")||kind.equals("beast_material"))com.longju.xiantu.item.BeastMaterialItem.setData(stack,rank,"");
            if(kind.equals("material_hide")||kind.equals("recycle_hide"))stack.getOrCreateTag().putString("hide_tier",kind.equals("material_hide")?"basic":rank==4?"intermediate":"advanced");
            if(kind.equals("material")&&com.longju.xiantu.material.MaterialQuality.eligible(stack))com.longju.xiantu.material.MaterialQuality.set(stack,1);
            return stack;
        }
        public ItemStack output(){return recycling()?new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("xiantu",com.longju.xiantu.item.SpiritStoneTier.values()[rank-1].registryName())),price):stack();}
    }
    public static final List<Offer> OFFERS=offers();
    private static List<Offer> offers(){
        var out=new ArrayList<Offer>();
        out.add(new Offer("cave/token","wanbao_merchant","token","dongfu_token",6,32,0));
        for(String book:BOOKS)out.add(new Offer("book/"+book,"wanbao_merchant","book",book,3,3,0));
        for(var book:com.longju.xiantu.gongfa.GongfaRegistry.ALL)if(book.tierIndex()==2||book.tierIndex()==3)
            out.add(new Offer("book/"+book.id(),"wanbao_merchant","book",book.id(),book.tierIndex()+1,book.tierIndex()==2?6:8,book.tierIndex()==2?0:20));
        out.add(new Offer("treatise/refining","smith_merchant","treatise","qidao_zhenjie",2,3,0));
        out.add(new Offer("treatise/alchemy","alchemy_merchant","treatise","dandao_zhenjie",2,3,0));
        for(String id:com.longju.xiantu.refining.FacilityRecipes.blueprints()){
            if(id.startsWith("array_")||id.endsWith("_pickaxe"))continue;
            int tier=(id.equals("forging_table")||id.equals("smelting_furnace"))?1:Character.digit(id.charAt(id.length()-1),10);
            String shop=id.startsWith("alchemy_furnace_")?"alchemy_merchant":"smith_merchant";
            out.add(new Offer("facility/"+id,shop,"facility",id,tier,TradeEconomyRules.facilityPrice(tier),0));
        }
        out.add(new Offer("facility/jiuyou_pickaxe","smith_merchant","facility","jiuyou_pickaxe",3,8,20));
        out.add(new Offer("facility/hundun_pickaxe","smith_merchant","facility","hundun_pickaxe",6,8,20));
        for(String part:List.of("eye","base","structure"))out.add(new Offer("facility/array_"+part+"_2","wanbao_merchant","facility","array_"+part+"_2",3,6,20));
        var alchemyKnowledge=new java.util.HashSet<String>();
        for(var r:com.longju.xiantu.alchemy.AlchemyRecipes.ALL)if(r.rank()==4&&alchemyKnowledge.add(r.knowledge()))out.add(new Offer(r.knowledge(),"alchemy_merchant","alchemy",r.id(),3,6,20));
        var themes=new java.util.HashSet<String>();
        for(var r:com.longju.xiantu.refining.RefiningRecipes.ALL)if(r.tier()>=4&&r.tier()<=6&&themes.add(com.longju.xiantu.refining.RefiningProgress.blueprintKey(r.id())))out.add(new Offer("gear/"+r.id(),"smith_merchant","gear",r.id(),r.tier()-1,5,20));
        for(var s:com.longju.xiantu.spell.SpellCatalog.ALL)if(s.tier()<=2)for(String shop:s.id().equals("rejuvenation")?List.of("wanbao_merchant"):List.of("wanbao_merchant","talisman_merchant"))out.add(new Offer("spell/"+s.id(),shop,"spell",s.id(),s.tier(),3,0));
        out.add(new Offer("talisman_book/xuanmen_initial","talisman_merchant","talisman_book","xuanmen_initial",1,3,0));
        out.add(new Offer("talisman_book/xuanmen_true","talisman_merchant","talisman_book","xuanmen_true",3,5,20));
        out.add(new Offer("talisman_book/xuanmen_secret","talisman_merchant","talisman_book","xuanmen_secret",6,8,20));
        out.add(new Offer("talisman/rebirth","talisman_merchant","talisman","rebirth",3,16,0));
        for(String id:java.util.List.of("fireball","diamond","lightbody","healing","detox"))out.add(new Offer("talisman/"+id,"talisman_merchant","talisman",id,1,2,0));
        out.add(new Offer("material/cinnabar","talisman_merchant","material","cinnabar",1,1,0));
        TownMaterialOffers.append(out);
        for(int tier=1;tier<8;tier++)out.add(new Offer("exchange/"+tier,"wanbao_merchant","exchange",com.longju.xiantu.item.SpiritStoneTier.values()[tier].registryName(),tier,TradeEconomyRules.exchangeCost(tier),0));
        return List.copyOf(out);
    }
    public static ItemStack book(int offer) { return new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("xiantu",BOOKS.get(offer)))); }
    public static int stoneRank(int offer) { return OFFERS.get(offer).rank(); }
    public static boolean known(ServerPlayer player,int offer) {
        var o=OFFERS.get(offer);
        if(o.kind().equals("spell"))return com.longju.xiantu.spell.SpellService.data(player).knows(o.key());
        if(o.kind().equals("talisman_book"))return com.longju.xiantu.talisman.TalismanProgress.knows(player,o.key());
        if(o.kind().equals("talisman")||o.category().equals("材料")||o.recycling()||o.exchange())return false;
        return o.kind().equals("book")?GongfaService.data(player).gongfa().knows(o.key()):com.longju.xiantu.refining.RefiningProgress.knows(player,com.longju.xiantu.refining.RefiningItems.knowledge(o.stack()));
    }
    public static int purchases(ServerPlayer player,TownMerchantEntity merchant){return merchant.getPersistentData().getCompound("PlayerPurchases").getInt(player.getUUID().toString());}
    public static int dailyLimit(Offer o){return o.kind().equals("talisman")?(o.key().equals("rebirth")?2:8):Integer.MAX_VALUE;}
    private static net.minecraft.nbt.CompoundTag daily(ServerPlayer p){return TradeDailyLedger.read(p.getPersistentData(),p.server.overworld().getGameTime());}
    public static int remaining(ServerPlayer p,Offer o){return dailyLimit(o)==Integer.MAX_VALUE?-1:Math.max(0,dailyLimit(o)-daily(p).getInt(o.id()));}
    /** Work on copies first: no charge or item mutation if payment/output space is invalid. */
    public static List<ItemStack> plan(List<ItemStack> original,int rank,ItemStack output) {
        return plan(original,rank,3,output);
    }
    public static List<ItemStack> plan(List<ItemStack> original,int rank,int price,ItemStack output) {
        return planExchange(original,s->s.getItem() instanceof SpiritStoneItem stone && stone.tier().rank()==rank && SpiritStoneItem.isFullyCharged(s),price,output);
    }
    public static List<ItemStack> planExchange(List<ItemStack> original,java.util.function.Predicate<ItemStack> accepted,int count,ItemStack output){
        if(original.size()!=37||count<=0||count>64||output.isEmpty())return null;
        var result=new ArrayList<ItemStack>();original.forEach(s->result.add(s.copy()));int remaining=count;
        for(int slot=0;slot<result.size() && remaining>0;slot++) {
            ItemStack stack=result.get(slot);
            if(!stack.isEmpty()&&accepted.test(stack)) {
                int take=Math.min(remaining,stack.getCount());stack.shrink(take);remaining-=take;
            }
        }
        if(remaining>0)return null;
        // Merge only identical item+NBT. The last slot is offhand, never an output destination.
        var delivery=output.copy();
        for(int slot=0;slot<36&&!delivery.isEmpty();slot++){
            var s=result.get(slot);if(!s.isEmpty()&&ItemStack.isSameItemSameTags(s,delivery)){
                int moved=Math.min(delivery.getCount(),Math.max(0,s.getMaxStackSize()-s.getCount()));s.grow(moved);delivery.shrink(moved);
            }
        }
        for(int slot=0;slot<36&&!delivery.isEmpty();slot++)if(result.get(slot).isEmpty()){
            int moved=Math.min(delivery.getCount(),delivery.getMaxStackSize());var part=delivery.copy();part.setCount(moved);result.set(slot,part);delivery.shrink(moved);
        }
        return delivery.isEmpty()?result:null;
    }
    public static String buy(ServerPlayer player,TownMerchantEntity merchant,int offer) {
        if(!merchant.mayTrade(player)||offer<0||offer>=OFFERS.size())return "交易已失效";
        var o=OFFERS.get(offer);if(!merchant.profession().equals(o.shop()))return "此店不出售该物品";
        if(purchases(player,merchant)<o.purchases())return "在本店完成"+o.purchases()+"次有效交易后开放";
        if((o.kind().equals("book")||o.kind().equals("treatise")||o.kind().equals("talisman_book"))&&known(player,offer))return "已经掌握，无需重复购买";
        var day=daily(player);if(day.getInt(o.id())>=dailyLimit(o))return "今日该商品已售罄";
        var inventory=player.getInventory();
        var original=new ArrayList<ItemStack>(inventory.items);original.add(inventory.offhand.get(0));
        var sample=o.stack();
        List<ItemStack> next=o.recycling()?planExchange(original,s->s.is(sample.getItem())&&(!o.kind().equals("recycle_beast")||com.longju.xiantu.item.BeastMaterialItem.grade(s)==o.rank())&&(!o.kind().equals("recycle_hide")||com.longju.xiantu.item.BeastHideItem.tier(s).equals(com.longju.xiantu.item.BeastHideItem.tier(sample))),o.amount(),o.output()):plan(original,o.rank(),o.price(),o.output());
        if(next==null)return o.recycling()?"需要"+o.amount()+"份"+sample.getHoverName().getString()+"，并留出灵石存放位置":"需要"+o.price()+"颗"+o.rank()+"阶完整灵石，并留出存放物品的位置";
        for(int slot=0;slot<36;slot++)inventory.items.set(slot,next.get(slot));
        inventory.offhand.set(0,next.get(36));inventory.setChanged();
        // Progress remains independent per player and NPC instance; no global/shared counter.
        var trades=merchant.getPersistentData().getCompound("PlayerPurchases");
        String key=player.getUUID().toString();if(!o.recycling()&&!o.exchange())trades.putInt(key,Math.min(32767,trades.getInt(key)+1));
        merchant.getPersistentData().put("PlayerPurchases",trades);
        if(dailyLimit(o)!=Integer.MAX_VALUE){day.putInt(o.id(),day.getInt(o.id())+1);TradeDailyLedger.write(player.getPersistentData(),day);}
        player.inventoryMenu.broadcastChanges();player.containerMenu.broadcastChanges();
        return (o.recycling()?"已回收，获得":o.exchange()?"已兑换":"已购得")+o.output().getHoverName().getString()+"×"+o.output().getCount();
    }
}
