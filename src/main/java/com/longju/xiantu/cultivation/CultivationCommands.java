package com.longju.xiantu.cultivation;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class CultivationCommands {
    private CultivationCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("xiantu")
                .then(Commands.literal("realm").requires(source->source.hasPermission(2))
                        .then(Commands.literal("list").executes(context->{
                            var data=com.longju.xiantu.instance.InstanceSavedData.get(context.getSource().getServer());
                            if(data.blocked()){context.getSource().sendFailure(Component.literal("实例记录异常，已禁止分配，请检查服务器日志"));return 0;}
                            long realms=data.slots().stream().filter(s->s.kind()==com.longju.xiantu.instance.InstanceLedger.Kind.REALM&&s.state()!=com.longju.xiantu.instance.InstanceLedger.State.FREE).count();
                            long caves=data.slots().stream().filter(s->s.kind()==com.longju.xiantu.instance.InstanceLedger.Kind.CAVE&&s.state()!=com.longju.xiantu.instance.InstanceLedger.State.FREE).count();
                            context.getSource().sendSuccess(()->Component.literal("秘境实例："+realms+"；永久洞府："+caves+"。秘境结束后间隔现实两小时再开放，洞府由主手使用令牌开辟。"),false);
                            if(data.ledger().activeRealm()==null&&data.runtime().contains("NextRealmAtMillis")){long seconds=Math.max(0,(data.runtime().getLong("NextRealmAtMillis")-System.currentTimeMillis()+999)/1000);context.getSource().sendSuccess(()->Component.literal("下一次秘境："+(seconds==0?"等待玩家附近出现合适空地":(seconds/60)+"分"+(seconds%60)+"秒后")),false);}
                            for(var slot:data.slots())if(slot.state()!=com.longju.xiantu.instance.InstanceLedger.State.FREE)context.getSource().sendSuccess(()->Component.literal(slot.dimension()+" · "+slot.state()+" · 代次"+slot.generation()),false);return 1;
                        })))
                .then(Commands.literal("medicine").executes(context -> medicine(context.getSource())))
                .then(Commands.literal("status").executes(context -> status(context.getSource())))
                .then(Commands.literal("breakthrough").executes(context -> breakthrough(context.getSource())))
                .then(Commands.literal("roots").executes(context -> roots(context.getSource()))));
    }

    private static int status(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) return 0;
        player.getCapability(CultivationCapability.DATA).ifPresent(data -> {
            RealmDefinition realm = RealmRegistry.get(data.majorRealm(), data.stage());
            player.sendSystemMessage(Component.literal(String.format(
                    "境界：%s·%s | 修为：%.1f/%.1f | 灵力：%.1f/%.1f | 灵根：%d | 灵脉：%d级（%d/%d） | 体质：%s",
                    realm.majorName(), realm.minorName(), data.progress(), data.progressMax(),
                    data.power(), data.powerMax(), data.spiritRootCount(),
                    com.longju.xiantu.mining.SpiritVeinProgress.level(player),
                    com.longju.xiantu.mining.SpiritVeinProgress.experience(player),
                    com.longju.xiantu.mining.SpiritVeinProgress.nextThreshold(player),data.physique().displayName)));
        });
        return 1;
    }

    private static int medicine(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) return 0;
        player.getCapability(CultivationCapability.DATA).ifPresent(data -> player.sendSystemMessage(com.longju.xiantu.alchemy.MedicineService.status(data.medicine())));
        return 1;
    }

    private static int roots(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) return 0;
        player.getCapability(CultivationCapability.DATA).ifPresent(data -> {
            StringBuilder roots = new StringBuilder();
            for (SpiritRootType type : SpiritRootType.values()) {
                if (data.hasSpiritRoot(type)) {
                    if (roots.length() > 0) roots.append('、');
                    roots.append(type.displayName());
                }
            }
            player.sendSystemMessage(Component.literal("灵根：" + (roots.length() == 0 ? "无" : roots)));
        });
        return 1;
    }

    private static int breakthrough(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) return 0;
        return BreakthroughService.tryBreakthrough(player) ? 1 : 0;
    }
}
