package com.cobblemon.mod.party;

import java.util.function.Supplier;

import com.cobblemon.mod.Cobblemon;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Cobblemon.MOD_ID);

    /**
     * Party data on players. Persisted, copied on death, and synced only to the owning player.
     */
    public static final Supplier<AttachmentType<PlayerParty>> PARTY = ATTACHMENT_TYPES.register(
            "party",
            () -> AttachmentType.builder(PlayerParty::empty)
                    .serialize(PlayerParty.CODEC.fieldOf("party"))
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to, PlayerParty.STREAM_CODEC)
                    .build()
    );

    /**
     * PC storage — boxes of mons beyond the party of 6. Synced to owner for UI.
     */
    public static final Supplier<AttachmentType<PlayerPc>> PC = ATTACHMENT_TYPES.register(
            "pc",
            () -> AttachmentType.builder(PlayerPc::empty)
                    .serialize(PlayerPc.CODEC.fieldOf("pc"))
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to, PlayerPc.STREAM_CODEC)
                    .build()
    );

    /** Seen/caught species for Pokédex lite. */
    public static final Supplier<AttachmentType<PlayerPokedex>> POKEDEX = ATTACHMENT_TYPES.register(
            "pokedex",
            () -> AttachmentType.builder(PlayerPokedex::empty)
                    .serialize(PlayerPokedex.CODEC.fieldOf("pokedex"))
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to, PlayerPokedex.STREAM_CODEC)
                    .build()
    );

    /** N3 — gym badges / story progress. */
    public static final Supplier<AttachmentType<PlayerBadges>> BADGES = ATTACHMENT_TYPES.register(
            "badges",
            () -> AttachmentType.builder(PlayerBadges::empty)
                    .serialize(PlayerBadges.CODEC.fieldOf("badges"))
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to, PlayerBadges.STREAM_CODEC)
                    .build()
    );

    private ModAttachments() {}
}
