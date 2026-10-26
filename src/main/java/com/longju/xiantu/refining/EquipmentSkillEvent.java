package com.longju.xiantu.refining;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.Event;
import java.util.UUID;
/** Read-only presentation hook. Subscribers must never apply damage, payment or rewards. */
public final class EquipmentSkillEvent extends Event {
    public enum Phase { START, RELEASE, HIT, CANCEL, END }
    public final ServerPlayer owner;public final String skill;public final UUID transaction,target;
    public final Phase phase;public final Vec3 position,direction;
    public EquipmentSkillEvent(ServerPlayer owner,String skill,UUID transaction,UUID target,Phase phase,Vec3 position,Vec3 direction){this.owner=owner;this.skill=skill;this.transaction=transaction;this.target=target;this.phase=phase;this.position=position;this.direction=direction;}
}
