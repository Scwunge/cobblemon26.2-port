package com.cobblemon.mod.client;

import com.cobblemon.mod.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * Client battle BGM while a wild battle is open.
 * <p>
 * Plays {@code cobblemon:battle.pvw.default} (classic-style wild battle loop).
 * Resource packs that ship official Pokémon battle music under that event id
 * will automatically replace the built-in chiptune.
 */
public final class BattleMusic {
    private static SoundInstance playing;
    private static int restartCooldown;

    private BattleMusic() {}

    public static void start() {
        stop();
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSoundManager() == null) {
            return;
        }
        try {
            mc.getMusicManager().stopPlaying();
        } catch (Throwable ignored) {
        }
        playing = createLoop();
        mc.getSoundManager().play(playing);
        restartCooldown = 40;
    }

    public static void ensurePlaying() {
        if (playing == null) {
            start();
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSoundManager() == null) {
            return;
        }
        if (restartCooldown > 0) {
            restartCooldown--;
            return;
        }
        if (!mc.getSoundManager().isActive(playing)) {
            playing = createLoop();
            mc.getSoundManager().play(playing);
            restartCooldown = 20;
        }
    }

    public static void stop() {
        Minecraft mc = Minecraft.getInstance();
        if (playing != null && mc.getSoundManager() != null) {
            mc.getSoundManager().stop(playing);
        }
        playing = null;
        restartCooldown = 0;
    }

    private static SoundInstance createLoop() {
        SoundEvent track = pickTrack();
        return new SimpleSoundInstance(
                track.location(),
                SoundSource.MUSIC,
                0.62f,
                1.0f,
                RandomSource.create(),
                true,
                0,
                SoundInstance.Attenuation.NONE,
                0.0,
                0.0,
                0.0,
                true
        );
    }

    private static SoundEvent pickTrack() {
        try {
            return ModSounds.battleWild();
        } catch (Throwable t) {
            try {
                return SoundEvents.MUSIC_DISC_PIGSTEP.value();
            } catch (Throwable t2) {
                return SoundEvents.MUSIC_CREATIVE.value();
            }
        }
    }
}
