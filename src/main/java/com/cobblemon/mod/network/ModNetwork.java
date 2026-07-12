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
        registrar.playToServer(PastureActionPayload.TYPE, PastureActionPayload.STREAM_CODEC, PastureActionPayload::handle);
        registrar.playToServer(BattleActionPayload.TYPE, BattleActionPayload.STREAM_CODEC, BattleActionPayload::handle);
        registrar.playToServer(UseItemOnMonPayload.TYPE, UseItemOnMonPayload.STREAM_CODEC, UseItemOnMonPayload::handle);
        registrar.playToServer(TradeOfferPayload.TYPE, TradeOfferPayload.STREAM_CODEC, TradeOfferPayload::handle);
        registrar.playToClient(OpenPcPayload.TYPE, OpenPcPayload.STREAM_CODEC, OpenPcPayload::handle);
        registrar.playToClient(OpenPasturePayload.TYPE, OpenPasturePayload.STREAM_CODEC, OpenPasturePayload::handle);
        registrar.playToClient(OpenStarterPayload.TYPE, OpenStarterPayload.STREAM_CODEC, OpenStarterPayload::handle);
        registrar.playToClient(OpenMonSelectPayload.TYPE, OpenMonSelectPayload.STREAM_CODEC, OpenMonSelectPayload::handle);
        registrar.playToClient(BattleUpdatePayload.TYPE, BattleUpdatePayload.STREAM_CODEC, BattleUpdatePayload::handle);
        registrar.playToClient(OpenTradePayload.TYPE, OpenTradePayload.STREAM_CODEC, OpenTradePayload::handle);
        registrar.playToClient(OpenDialoguePayload.TYPE, OpenDialoguePayload.STREAM_CODEC, OpenDialoguePayload::handle);
        registrar.playToServer(DialogueChoicePayload.TYPE, DialogueChoicePayload.STREAM_CODEC, DialogueChoicePayload::handle);
        registrar.playToServer(TradeRequestPayload.TYPE, TradeRequestPayload.STREAM_CODEC, TradeRequestPayload::handle);
        registrar.playToClient(TradeOfferNotifyPayload.TYPE, TradeOfferNotifyPayload.STREAM_CODEC, TradeOfferNotifyPayload::handle);
        registrar.playToServer(BattleChallengePayload.TYPE, BattleChallengePayload.STREAM_CODEC, BattleChallengePayload::handle);
        registrar.playToClient(BattleChallengeNotifyPayload.TYPE, BattleChallengeNotifyPayload.STREAM_CODEC, BattleChallengeNotifyPayload::handle);
    }
}
