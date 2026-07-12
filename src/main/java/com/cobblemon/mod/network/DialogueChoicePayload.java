package com.cobblemon.mod.network;

import com.cobblemon.mod.Cobblemon;
import com.cobblemon.mod.dialogue.DialogueManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client → server: player picked a dialogue option. */
public record DialogueChoicePayload(
        String dialogueId,
        String pageId,
        int optionIndex,
        int speakerEntityId
) implements CustomPacketPayload {
    public static final Type<DialogueChoicePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Cobblemon.MOD_ID, "dialogue_choice"));

    public static final StreamCodec<ByteBuf, DialogueChoicePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, DialogueChoicePayload::dialogueId,
            ByteBufCodecs.STRING_UTF8, DialogueChoicePayload::pageId,
            ByteBufCodecs.VAR_INT, DialogueChoicePayload::optionIndex,
            ByteBufCodecs.VAR_INT, DialogueChoicePayload::speakerEntityId,
            DialogueChoicePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DialogueChoicePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                DialogueManager.choose(
                        player,
                        payload.dialogueId(),
                        payload.pageId(),
                        payload.optionIndex(),
                        payload.speakerEntityId()
                );
            }
        });
    }
}
