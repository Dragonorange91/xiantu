package com.longju.xiantu.spell;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.Event;
import java.util.UUID;

/** Read-only server notification for later client presentation. Never authorizes damage/payment. */
public final class SpellLifecycleEvent extends Event {
 public enum Phase{WINDUP,RELEASE,FLIGHT,HIT,END,CANCEL}
 public final ServerPlayer caster;public final String spellId;public final UUID transaction,target;
 public final Phase phase;public final Vec3 position,direction;
 public SpellLifecycleEvent(ServerPlayer caster,String spellId,UUID transaction,UUID target,Phase phase,Vec3 position,Vec3 direction){this.caster=caster;this.spellId=spellId;this.transaction=transaction;this.target=target;this.phase=phase;this.position=position;this.direction=direction;}
}
