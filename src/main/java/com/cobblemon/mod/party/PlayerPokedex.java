package com.cobblemon.mod.party;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Simple seen/caught species tracking (Pokédex lite).
 */
public final class PlayerPokedex {
    public static final Codec<PlayerPokedex> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.listOf().optionalFieldOf("seen", List.of()).forGetter(PlayerPokedex::seenList),
            Codec.STRING.listOf().optionalFieldOf("caught", List.of()).forGetter(PlayerPokedex::caughtList)
    ).apply(i, PlayerPokedex::fromLists));

    public static final StreamCodec<ByteBuf, PlayerPokedex> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
            PlayerPokedex::seenList,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
            PlayerPokedex::caughtList,
            PlayerPokedex::fromLists
    );

    private final Set<String> seen = new HashSet<>();
    private final Set<String> caught = new HashSet<>();

    public PlayerPokedex() {}

    private PlayerPokedex(Set<String> seen, Set<String> caught) {
        this.seen.addAll(seen);
        this.caught.addAll(caught);
    }

    public static PlayerPokedex empty() {
        return new PlayerPokedex();
    }

    private static PlayerPokedex fromLists(List<String> seen, List<String> caught) {
        PlayerPokedex d = new PlayerPokedex();
        if (seen != null) {
            for (String s : seen) {
                d.seen.add(norm(s));
            }
        }
        if (caught != null) {
            for (String s : caught) {
                d.caught.add(norm(s));
                d.seen.add(norm(s));
            }
        }
        return d;
    }

    private List<String> seenList() {
        return List.copyOf(seen);
    }

    private List<String> caughtList() {
        return List.copyOf(caught);
    }

    private static String norm(String id) {
        return id == null ? "" : id.toLowerCase(Locale.ROOT);
    }

    public PlayerPokedex copy() {
        return new PlayerPokedex(new HashSet<>(seen), new HashSet<>(caught));
    }

    public boolean markSeen(String speciesId) {
        return seen.add(norm(speciesId));
    }

    public boolean markCaught(String speciesId) {
        String id = norm(speciesId);
        seen.add(id);
        return caught.add(id);
    }

    public boolean hasSeen(String speciesId) {
        return seen.contains(norm(speciesId));
    }

    public boolean hasCaught(String speciesId) {
        return caught.contains(norm(speciesId));
    }

    public int seenCount() {
        return seen.size();
    }

    public int caughtCount() {
        return caught.size();
    }
}
