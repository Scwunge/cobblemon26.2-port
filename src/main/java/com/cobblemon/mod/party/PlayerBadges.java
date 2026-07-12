package com.cobblemon.mod.party;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;

/**
 * N3 — gym / story badge progress stored on the player.
 * Badge ids are short keys (e.g. {@code boulder}, {@code cascade}).
 */
public final class PlayerBadges {
    public static final Codec<PlayerBadges> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.listOf().optionalFieldOf("badges", List.of()).forGetter(b -> new ArrayList<>(b.ids))
    ).apply(i, PlayerBadges::new));

    public static final StreamCodec<ByteBuf, PlayerBadges> STREAM_CODEC = StreamCodec.of(
            (buf, b) -> {
                ByteBufCodecs.VAR_INT.encode(buf, b.ids.size());
                for (String id : b.ids) {
                    ByteBufCodecs.STRING_UTF8.encode(buf, id);
                }
            },
            buf -> {
                int n = ByteBufCodecs.VAR_INT.decode(buf);
                List<String> list = new ArrayList<>(n);
                for (int i = 0; i < n; i++) {
                    list.add(ByteBufCodecs.STRING_UTF8.decode(buf));
                }
                return new PlayerBadges(list);
            }
    );

    /** Official-style Kanto badge order (display). */
    public static final List<String> KANTO_ORDER = List.of(
            "boulder", "cascade", "thunder", "rainbow",
            "soul", "marsh", "volcano", "earth"
    );

    private final Set<String> ids;

    public PlayerBadges() {
        this.ids = new LinkedHashSet<>();
    }

    public PlayerBadges(List<String> list) {
        this.ids = new LinkedHashSet<>();
        if (list != null) {
            for (String s : list) {
                if (s != null && !s.isBlank()) {
                    this.ids.add(normalize(s));
                }
            }
        }
    }

    public static PlayerBadges empty() {
        return new PlayerBadges();
    }

    public static String normalize(String id) {
        if (id == null) {
            return "";
        }
        return id.toLowerCase(Locale.ROOT).trim().replace(' ', '_');
    }

    public static String displayName(String id) {
        String n = normalize(id);
        if (n.isEmpty()) {
            return "Badge";
        }
        return Character.toUpperCase(n.charAt(0)) + n.substring(1) + " Badge";
    }

    public static PlayerBadges get(ServerPlayer player) {
        return player.getData(ModAttachments.BADGES);
    }

    public static void set(ServerPlayer player, PlayerBadges badges) {
        player.setData(ModAttachments.BADGES, badges == null ? empty() : badges);
    }

    /**
     * @return true if newly awarded (false if already owned)
     */
    public static boolean award(ServerPlayer player, String badgeId) {
        String id = normalize(badgeId);
        if (id.isEmpty()) {
            return false;
        }
        PlayerBadges cur = get(player);
        if (cur.has(id)) {
            return false;
        }
        PlayerBadges next = cur.with(id);
        set(player, next);
        return true;
    }

    public boolean has(String badgeId) {
        return ids.contains(normalize(badgeId));
    }

    public PlayerBadges with(String badgeId) {
        String id = normalize(badgeId);
        if (id.isEmpty() || ids.contains(id)) {
            return this;
        }
        List<String> next = new ArrayList<>(ids);
        next.add(id);
        return new PlayerBadges(next);
    }

    public Set<String> ids() {
        return Collections.unmodifiableSet(ids);
    }

    public int count() {
        return ids.size();
    }

    public List<String> ordered() {
        List<String> out = new ArrayList<>();
        for (String k : KANTO_ORDER) {
            if (ids.contains(k)) {
                out.add(k);
            }
        }
        for (String id : ids) {
            if (!out.contains(id)) {
                out.add(id);
            }
        }
        return out;
    }
}
