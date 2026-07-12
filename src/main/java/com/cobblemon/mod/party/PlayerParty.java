package com.cobblemon.mod.party;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.cobblemon.mod.species.OwnedMon;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Party of up to {@link #MAX_SIZE} owned mons, attached to a player.
 * Slot 0 is the lead mon.
 */
public final class PlayerParty {
    public static final int MAX_SIZE = 6;

    public static final Codec<PlayerParty> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            OwnedMon.CODEC.listOf().fieldOf("mons").forGetter(PlayerParty::mons)
    ).apply(instance, PlayerParty::new));

    public static final StreamCodec<ByteBuf, PlayerParty> STREAM_CODEC = StreamCodec.composite(
            OwnedMon.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_SIZE)),
            party -> new ArrayList<>(party.mons),
            PlayerParty::new
    );

    private final List<OwnedMon> mons;

    public PlayerParty() {
        this.mons = new ArrayList<>();
    }

    public PlayerParty(List<OwnedMon> mons) {
        this.mons = new ArrayList<>();
        if (mons != null) {
            for (OwnedMon mon : mons) {
                if (this.mons.size() >= MAX_SIZE) {
                    break;
                }
                if (mon != null) {
                    this.mons.add(mon);
                }
            }
        }
    }

    public static PlayerParty empty() {
        return new PlayerParty();
    }

    public List<OwnedMon> mons() {
        return Collections.unmodifiableList(mons);
    }

    public int size() {
        return mons.size();
    }

    public boolean isEmpty() {
        return mons.isEmpty();
    }

    public boolean isFull() {
        return mons.size() >= MAX_SIZE;
    }

    public boolean isValidSlot(int index) {
        return index >= 0 && index < mons.size();
    }

    public boolean add(OwnedMon mon) {
        if (isFull() || mon == null) {
            return false;
        }
        mons.add(mon);
        return true;
    }

    public Optional<OwnedMon> get(int index) {
        if (!isValidSlot(index)) {
            return Optional.empty();
        }
        return Optional.of(mons.get(index));
    }

    public Optional<OwnedMon> lead() {
        return get(0);
    }

    /**
     * Swap two occupied slots. Returns false if either index is invalid.
     */
    public boolean swap(int a, int b) {
        if (!isValidSlot(a) || !isValidSlot(b) || a == b) {
            return false;
        }
        Collections.swap(mons, a, b);
        return true;
    }

    /**
     * Move the mon at {@code index} to the lead slot (0).
     */
    public boolean makeLead(int index) {
        if (!isValidSlot(index) || index == 0) {
            return false;
        }
        OwnedMon mon = mons.remove(index);
        mons.add(0, mon);
        return true;
    }

    /**
     * Remove and return the mon at {@code index}.
     */
    public Optional<OwnedMon> release(int index) {
        if (!isValidSlot(index)) {
            return Optional.empty();
        }
        return Optional.of(mons.remove(index));
    }

    /**
     * Fully heal every mon in the party.
     */
    public void healAll() {
        for (int i = 0; i < mons.size(); i++) {
            mons.set(i, mons.get(i).healed());
        }
    }

    /**
     * Replace the mon at {@code index} (must already be occupied).
     */
    public boolean set(int index, OwnedMon mon) {
        if (!isValidSlot(index) || mon == null) {
            return false;
        }
        mons.set(index, mon);
        return true;
    }

    public PlayerParty copy() {
        return new PlayerParty(new ArrayList<>(mons));
    }
}
