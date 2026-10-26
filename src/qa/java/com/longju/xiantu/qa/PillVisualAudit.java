package com.longju.xiantu.qa;
import com.longju.xiantu.alchemy.PillCatalog;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.*;
@Mod.EventBusSubscriber(modid="xiantu_qa",value=Dist.CLIENT)
public final class PillVisualAudit {
    @SubscribeEvent public static void commands(RegisterClientCommandsEvent e){
        e.getDispatcher().register(Commands.literal("xiantupillaudit").executes(c->run()));
        e.getDispatcher().register(Commands.literal("xiantupillgallery").executes(c->{Minecraft.getInstance().execute(()->Minecraft.getInstance().setScreen(new Gallery()));return 1;}));
    }
    private static int run(){
        var mc=Minecraft.getInstance();var failures=new ArrayList<String>();int checked=0;
        for(var d:PillCatalog.ALL)for(int q=1;q<=5;q++){
            checked++;var stack=PillCatalog.stack(d.id(),q);String expected="item/pills/"+d.id()+"_q"+q;
            var model=mc.getItemRenderer().getModel(stack,mc.level,mc.player,0);
            boolean valid=model!=mc.getModelManager().getMissingModel();int quads=0;
            var sides=new ArrayList<Direction>(Arrays.asList(Direction.values()));sides.add(null);
            for(var side:sides)for(var quad:model.getQuads(null,side,RandomSource.create(42))){quads++;valid&=quad.getSprite().contents().name().getPath().equals(expected);}
            if(!valid||quads==0)failures.add(d.id()+" q"+q+" incorrect or missing baked sprite");
        }
        String result="[Pill visual QA] "+checked+" variants, "+failures.size()+" failures";
        org.slf4j.LoggerFactory.getLogger(PillVisualAudit.class).info("{} {}",result,failures);mc.player.displayClientMessage(Component.literal(result),false);
        try{var dir=mc.gameDirectory.toPath().resolve("xiantu-qa-pills");java.nio.file.Files.createDirectories(dir);
            java.nio.file.Files.writeString(dir.resolve("visual-audit.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(Map.of("variants",checked,"failures",failures)));}
        catch(Exception ex){throw new RuntimeException(ex);}return failures.isEmpty()?1:0;
    }
    private static final class Gallery extends net.minecraft.client.gui.screens.Screen {
        private int page;
        Gallery(){super(Component.literal("丹药五档品质预览"));}
        @Override protected void init(){
            addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal("上一页"),b->page=Math.max(0,page-1)).bounds(width/2-150,height-25,90,20).build());
            addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal("下一页"),b->page=Math.min(5,page+1)).bounds(width/2+60,height-25,90,20).build());
        }
        @Override public void render(net.minecraft.client.gui.GuiGraphics g,int mx,int my,float tick){
            renderBackground(g);int x=width/2-155,y=25;g.drawCenteredString(font,title,width/2,8,0xFFF3DCA7);
            for(int q=1;q<=5;q++)g.drawString(font,PillCatalog.QUALITIES[q-1],x+100+(q-1)*42,y,0xFFDDC58E,false);
            for(int r=0;r<6;r++){var d=PillCatalog.ALL.get(page*6+r);int row=y+18+r*26;
                g.drawString(font,d.name(),x,row+5,0xFFE0DCCC,false);
                for(int q=1;q<=5;q++){var stack=PillCatalog.stack(d.id(),q);int col=x+101+(q-1)*42;g.renderItem(stack,col,row);
                    if(mx>=col&&mx<col+18&&my>=row&&my<row+18)g.renderTooltip(font,stack,mx,my);}
            }
            g.drawCenteredString(font,(page+1)+" / 6",width/2,height-19,0xFFDDC58E);super.render(g,mx,my,tick);
        }
        @Override public boolean isPauseScreen(){return false;}
    }
}
