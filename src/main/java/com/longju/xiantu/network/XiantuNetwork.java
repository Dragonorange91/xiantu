package com.longju.xiantu.network;

import com.longju.xiantu.XiantuMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class XiantuNetwork {
    private static final String PROTOCOL = "33";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(XiantuMod.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);
    private static int nextId;

    private XiantuNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(GreatFireballInputPacket.class,nextId++,net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER).encoder(GreatFireballInputPacket::encode).decoder(GreatFireballInputPacket::decode).consumerMainThread(GreatFireballInputPacket::handle).add();
        CHANNEL.messageBuilder(GreatFireballStatePacket.class,nextId++,net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT).encoder(GreatFireballStatePacket::encode).decoder(GreatFireballStatePacket::decode).consumerMainThread(GreatFireballStatePacket::handle).add();
        CHANNEL.messageBuilder(FormationProjectionPacket.class,nextId++,net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT).encoder(FormationProjectionPacket::encode).decoder(FormationProjectionPacket::decode).consumerMainThread(FormationProjectionPacket::handle).add();
        CHANNEL.messageBuilder(SpiritSwordStatePacket.class,nextId++,net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT).encoder(SpiritSwordStatePacket::encode).decoder(SpiritSwordStatePacket::decode).consumerMainThread(SpiritSwordStatePacket::handle).add();
        CHANNEL.messageBuilder(SpiritSwordInputPacket.class,nextId++,net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER).encoder(SpiritSwordInputPacket::encode).decoder(SpiritSwordInputPacket::decode).consumerMainThread(SpiritSwordInputPacket::handle).add();
        CHANNEL.messageBuilder(TalismanStrokePacket.class,nextId++,net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER).encoder(TalismanStrokePacket::encode).decoder(TalismanStrokePacket::decode).consumerMainThread(TalismanStrokePacket::handle).add();
        CHANNEL.messageBuilder(BossHudPacket.class,nextId++,net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT).encoder(BossHudPacket::encode).decoder(BossHudPacket::decode).consumerMainThread(BossHudPacket::handle).add();
        CHANNEL.messageBuilder(QitianAppearancePacket.class,nextId++,net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT).encoder(QitianAppearancePacket::encode).decoder(QitianAppearancePacket::decode).consumerMainThread(QitianAppearancePacket::handle).add();
        CHANNEL.messageBuilder(SkillVfxPacket.class,nextId++,net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT).encoder(SkillVfxPacket::encode).decoder(SkillVfxPacket::decode).consumerMainThread(SkillVfxPacket::handle).add();
        CHANNEL.messageBuilder(CondenseSpiritFirePacket.class,nextId++,net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER).encoder(CondenseSpiritFirePacket::encode).decoder(CondenseSpiritFirePacket::decode).consumerMainThread(CondenseSpiritFirePacket::handle).add();
        CHANNEL.messageBuilder(EquipmentSkillIntentPacket.class,nextId++,net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER).encoder(EquipmentSkillIntentPacket::encode).decoder(EquipmentSkillIntentPacket::decode).consumerMainThread(EquipmentSkillIntentPacket::handle).add();
        CHANNEL.messageBuilder(EquipmentSkillStatePacket.class,nextId++,net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT).encoder(EquipmentSkillStatePacket::encode).decoder(EquipmentSkillStatePacket::decode).consumerMainThread(EquipmentSkillStatePacket::handle).add();
        CHANNEL.messageBuilder(SpellStatePacket.class, nextId++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT).encoder(SpellStatePacket::encode).decoder(SpellStatePacket::decode).consumerMainThread(SpellStatePacket::handle).add();
        CHANNEL.messageBuilder(SpellIntentPacket.class, nextId++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER).encoder(SpellIntentPacket::encode).decoder(SpellIntentPacket::decode).consumerMainThread(SpellIntentPacket::handle).add();
        CHANNEL.messageBuilder(SpellConfigurePacket.class, nextId++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER).encoder(SpellConfigurePacket::encode).decoder(SpellConfigurePacket::decode).consumerMainThread(SpellConfigurePacket::handle).add();
        CHANNEL.messageBuilder(OpenAccessoriesPacket.class, nextId++, net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
            .encoder(OpenAccessoriesPacket::encode).decoder(OpenAccessoriesPacket::decode).consumerMainThread(OpenAccessoriesPacket::handle).add();
        CHANNEL.messageBuilder(BeastRealmPacket.class, nextId++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                .encoder(BeastRealmPacket::encode).decoder(BeastRealmPacket::decode)
                .consumerMainThread(BeastRealmPacket::handle).add();
        CHANNEL.messageBuilder(BeastVfxPacket.class, nextId++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                .encoder(BeastVfxPacket::encode).decoder(BeastVfxPacket::decode)
                .consumerMainThread(BeastVfxPacket::handle).add();
        CHANNEL.messageBuilder(GongfaStudyPacket.class, nextId++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                .encoder(GongfaStudyPacket::encode).decoder(GongfaStudyPacket::decode)
                .consumerMainThread(GongfaStudyPacket::handle).add();
        CHANNEL.messageBuilder(CultivationSyncPacket.class, nextId++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CultivationSyncPacket::encode)
                .decoder(CultivationSyncPacket::decode)
                .consumerMainThread(CultivationSyncPacket::handle)
                .add();
        CHANNEL.messageBuilder(InnerDemonStatePacket.class, nextId++, net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
                .encoder(InnerDemonStatePacket::encode).decoder(InnerDemonStatePacket::decode)
                .consumerMainThread(InnerDemonStatePacket::handle).add();
    }
}
