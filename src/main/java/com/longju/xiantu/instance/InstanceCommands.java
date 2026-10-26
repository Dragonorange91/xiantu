package com.longju.xiantu.instance;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="xiantu")
public final class InstanceCommands {
    @SubscribeEvent public static void register(RegisterCommandsEvent e){
        var root=Commands.literal("xiantu").then(Commands.literal("realm").requires(s->s.hasPermission(2))
            .then(Commands.literal("spawn").executes(c->spawn(c.getSource(),null))
                .then(Commands.literal("natural").executes(c->spawn(c.getSource(),true)))
                .then(Commands.literal("ruins").executes(c->spawn(c.getSource(),false))))
            .then(Commands.literal("close").executes(c->{var d=InstanceService.data(c.getSource().getServer());if(d.blocked()||d.ledger().activeRealm()==null)return 0;var slot=d.slots().stream().filter(v->v.instance()!=null&&v.instance().equals(d.ledger().activeRealm())).findFirst().orElseThrow();return schedule(c.getSource(),slot.index());}))
            .then(Commands.literal("quarantine").then(Commands.argument("slot",IntegerArgumentType.integer(0,15)).executes(c->schedule(c.getSource(),IntegerArgumentType.getInteger(c,"slot")))))
            .then(Commands.literal("retry").then(Commands.argument("slot",IntegerArgumentType.integer(0,15)).executes(c->schedule(c.getSource(),IntegerArgumentType.getInteger(c,"slot")))))
            .then(Commands.literal("purge").then(Commands.argument("slot",IntegerArgumentType.integer(0,15)).then(Commands.literal("confirm").executes(c->schedule(c.getSource(),IntegerArgumentType.getInteger(c,"slot")))))));
        e.getDispatcher().register(root);
    }
    private static int spawn(net.minecraft.commands.CommandSourceStack source,Boolean natural)throws com.mojang.brigadier.exceptions.CommandSyntaxException{
        var p=source.getPlayerOrException();if(p.level().dimension()!=net.minecraft.world.level.Level.OVERWORLD){source.sendFailure(Component.literal("请在主世界使用"));return 0;}
        var l=p.serverLevel();net.minecraft.core.BlockPos entry=null;
        // Loaded terrain only, with enough free space. Refuse rather than overwrite a structure or container.
        search:for(int r=4;r<=24;r+=4)for(int dx=-r;dx<=r;dx+=4)for(int dz=-r;dz<=r;dz+=4){
            var at=p.blockPosition().offset(dx,0,dz);if(!l.hasChunkAt(at))continue;
            at=l.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,at);
            if(!RealmPortal.site(l,at)||!InstanceService.safe(l,at)||!l.getBlockState(at).isAir()||!l.getWorldBorder().isWithinBounds(at)||com.longju.xiantu.worldgen.RuinProtection.protectedAt(l,at))continue;
            var site=at;if(l.structureManager().startsForStructure(new net.minecraft.world.level.ChunkPos(at),v->true).stream().anyMatch(v->v.getBoundingBox().isInside(site)))continue;
            entry=at;break search;
        }
        if(entry==null){source.sendFailure(Component.literal("附近没有合适的已加载空地，请到开阔地重试"));return 0;}
        var params=InstanceRules.draw(new java.util.Random());
        String result=InstanceService.createRealm(source.getServer(),entry,params,0,natural);
        source.sendSuccess(()->Component.literal(result),false);return 1;
    }
    private static int schedule(net.minecraft.commands.CommandSourceStack source,int index){
        var d=InstanceService.data(source.getServer());if(d.blocked()){source.sendFailure(Component.literal("实例记录异常，禁止清理"));return 0;}
        var s=d.ledger().find("xiantu:realm_%02d".formatted(index)).orElseThrow();if(s.state()==InstanceLedger.State.FREE){source.sendFailure(Component.literal("该秘境槽位为空"));return 0;}
        InstanceService.close(source.getServer(),s,"管理员关闭");source.sendSuccess(()->Component.literal("秘境已关闭"),false);return 1;
    }
    private InstanceCommands(){}
}
