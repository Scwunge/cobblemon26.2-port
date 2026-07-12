package com.cobblemon.mod.network;

import com.cobblemon.mod.Cobblemon;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = Cobblemon.MOD_ID)
public final class ModNetwork {
    private ModNetwork() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(PartyActionPayload.TYPE, PartyActionPayload.STREAM_CODEC, PartyActionPayload::handle);
        registrar.playToServer(ChooseStarterPayload.TYPE, ChooseStarterPayload.STREAM_CODEC, ChooseStarterPayload::handle);
        registrar.playToServer(SendOutPayload.TYPE, SendOutPayload.STREAM_CODEC, SendOutPayload::handle);
        registrar.playToServer(RidePayload.TYPE, RidePayload.STREAM_CODEC, RidePayload::handle);
        registrar.playToServer(PcActionPayload.TYPE, PcActionPayload.STREAM_CODEC, PcActionPayload::handle);
        registrar.playToServer(BattleActionPayload.TYPE, BattleActionPayload.STREAM_CODEC, BattleActionPayload::handle);
        registrar.playToServer(UseItemOnMonPayload.TYPE, UseItemOnMonPayload.STREAM_CODEC, UseItemOnMonPayload::handle);
        registrar.playToClient(OpenPcPayload.TYPE, OpenPcPayload.STREAM_CODEC, OpenPcPayload::handle);
        registrar.playToClient(OpenStarterPayload.TYPE, OpenStarterPayload.STREAM_CODEC, OpenStarterPayload::handle);
        registrar.playToClient(OpenMonSelectPayload.TYPE, OpenMonSelectPayload.STREAM_CODEC, OpenMonSelectPayload::handle);
        registrar.playToClient(BattleUpdatePayload.TYPE, BattleUpdatePayload.STREAM_CODEC, BattleUpdatePayload::handle);
    }
}
