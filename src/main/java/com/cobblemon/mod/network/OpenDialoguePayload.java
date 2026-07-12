package com.cobblemon.mod.network;

import java.util.ArrayList;
import java.util.List;

import com.cobblemon.mod.Cobblemon;
import com.cobblemon.mod.client.ClientHooks;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server → client: open dialogue page. */
public record OpenDialoguePayload(
        String dialogueId,
        String pageId,
        List<String> lines,
        List<String> optionLabels,
        List<String> optionActions,
        int speakerEntityId
) implements CustomPacketPayload {
    public static final Type<OpenDialoguePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Cobblemon.MOD_ID, "open_dialogue"));

    public static final StreamCodec<ByteBuf, OpenDialoguePayload> STREAM_CODEC = StreamCodec.of(
            OpenDialoguePayload::encode,
            OpenDialoguePayload::decode
    );

    private static void encode(ByteBuf buf, OpenDialoguePayload p) {
        ByteBufCodecs.STRING_UTF8.encode(buf, p.dialogueId == null ? "" : p.dialogueId);
        ByteBufCodecs.STRING_UTF8.encode(buf, p.pageId == null ? "" : p.pageId);
        writeList(buf, p.lines);
        writeList(buf, p.optionLabels);
        writeList(buf, p.optionActions);
        ByteBufCodecs.VAR_INT.encode(buf, p.speakerEntityId);
    }

    private static OpenDialoguePayload decode(ByteBuf buf) {
        return new OpenDialoguePayload(
                ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf),
                readList(buf),
                readList(buf),
                readList(buf),
                ByteBufCodecs.VAR_INT.decode(buf)
        );
    }

    private static void writeList(ByteBuf buf, List<String> list) {
        ByteBufCodecs.VAR_INT.encode(buf, list == null ? 0 : list.size());
        if (list != null) {
            for (String s : list) {
                ByteBufCodecs.STRING_UTF8.encode(buf, s == null ? "" : s);
            }
        }
    }

    private static List<String> readList(ByteBuf buf) {
        int n = ByteBufCodecs.VAR_INT.decode(buf);
        List<String> out = new ArrayList<>(Math.max(0, n));
        for (int i = 0; i < n; i++) {
            out.add(ByteBufCodecs.STRING_UTF8.decode(buf));
        }
        return out;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenDialoguePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientHooks.openDialogueScreen(payload));
    }
}
