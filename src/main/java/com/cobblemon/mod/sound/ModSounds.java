package com.cobblemon.mod.sound;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import com.cobblemon.mod.Cobblemon;
import com.cobblemon.mod.species.MonSpecies;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Pokémon cry SoundEvents (from Cobblemon originals, remapped to {@code Cobblemon} namespace).
 */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, Cobblemon.MOD_ID);

    /** Wild battle BGM (Cobblemon-compatible event id; pack can override). */
    public static final DeferredHolder<SoundEvent, SoundEvent> BATTLE_PVW = SOUND_EVENTS.register(
            "battle.pvw.default",
            () -> SoundEvent.createVariableRangeEvent(
                    Identifier.fromNamespaceAndPath(Cobblemon.MOD_ID, "battle.pvw.default")
            )
    );
    public static final DeferredHolder<SoundEvent, SoundEvent> BATTLE_PVP = SOUND_EVENTS.register(
            "battle.pvp.default",
            () -> SoundEvent.createVariableRangeEvent(
                    Identifier.fromNamespaceAndPath(Cobblemon.MOD_ID, "battle.pvp.default")
            )
    );
    public static final DeferredHolder<SoundEvent, SoundEvent> POKE_BALL_SEND_OUT = SOUND_EVENTS.register(
            "poke_ball.send_out",
            () -> SoundEvent.createVariableRangeEvent(
                    Identifier.fromNamespaceAndPath(Cobblemon.MOD_ID, "poke_ball.send_out")
            )
    );

    private static final Map<MonSpecies, DeferredHolder<SoundEvent, SoundEvent>> CRIES =
            new EnumMap<>(MonSpecies.class);

    static {
        for (MonSpecies species : MonSpecies.values()) {
            String folder = soundFolder(species);
            String eventPath = "pokemon." + folder + ".cry";
            DeferredHolder<SoundEvent, SoundEvent> holder = SOUND_EVENTS.register(
                    eventPath,
                    () -> SoundEvent.createVariableRangeEvent(
                            Identifier.fromNamespaceAndPath(Cobblemon.MOD_ID, eventPath)
                    )
            );
            CRIES.put(species, holder);
        }
    }

    private ModSounds() {}

    public static void register(IEventBus bus) {
        SOUND_EVENTS.register(bus);
    }

    public static SoundEvent battleWild() {
        return BATTLE_PVW.get();
    }

    public static SoundEvent pokeBallSendOut() {
        return POKE_BALL_SEND_OUT.get();
    }

    public static SoundEvent cry(MonSpecies species) {
        DeferredHolder<SoundEvent, SoundEvent> h = CRIES.get(species);
        return h != null ? h.get() : SoundEvent.createVariableRangeEvent(
                Identifier.fromNamespaceAndPath(Cobblemon.MOD_ID, "pokemon.rattata.cry")
        );
    }

    /** Folder under sounds/pokemon/ (nidoran_f → nidoranf, etc.). */
    public static String soundFolder(MonSpecies species) {
        String id = species.id().toLowerCase(Locale.ROOT);
        return switch (id) {
            case "nidoran_f" -> "nidoranf";
            case "nidoran_m" -> "nidoranm";
            case "mr_mime" -> "mrmime";
            case "farfetchd" -> "farfetchd";
            default -> id;
        };
    }
}
