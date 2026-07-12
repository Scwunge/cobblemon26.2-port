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
 * Player PC storage — boxes of fixed slots (Pokémon-style banking).
 */
public final class PlayerPc {
    /** Cobblemon-like: many boxes; expandable feel. */
    public static final int BOX_COUNT = 30;
    public static final int BOX_SIZE = 30; // 6x5 grid
    public static final int TOTAL_SLOTS = BOX_COUNT * BOX_SIZE;

    private static final String[] DEFAULT_WALLPAPERS = {
            "default", "forest", "cave", "ocean", "nether", "end"
    };

    public static final Codec<PlayerPc> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Slot.CODEC.listOf().fieldOf("slots").forGetter(PlayerPc::slotList),
            Codec.STRING.listOf().optionalFieldOf("boxNames", List.of()).forGetter(PlayerPc::boxNameList),
            Codec.STRING.listOf().optionalFieldOf("wallpapers", List.of()).forGetter(PlayerPc::wallpaperList)
    ).apply(instance, PlayerPc::fromCodec));

    public static final StreamCodec<ByteBuf, PlayerPc> STREAM_CODEC = StreamCodec.composite(
            Slot.STREAM_CODEC.apply(ByteBufCodecs.list(TOTAL_SLOTS)),
            PlayerPc::slotList,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(BOX_COUNT)),
            PlayerPc::boxNameList,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(BOX_COUNT)),
            PlayerPc::wallpaperList,
            PlayerPc::fromStream
    );

    /** Null = empty slot. Length always TOTAL_SLOTS. */
    private final OwnedMon[] slots;
    private final String[] boxNames;
    private final String[] wallpapers;

    public PlayerPc() {
        this.slots = new OwnedMon[TOTAL_SLOTS];
        this.boxNames = defaultNames();
        this.wallpapers = defaultWallpapers();
    }

    private PlayerPc(OwnedMon[] slots, String[] boxNames, String[] wallpapers) {
        this.slots = slots;
        this.boxNames = boxNames != null ? boxNames : defaultNames();
        this.wallpapers = wallpapers != null ? wallpapers : defaultWallpapers();
    }

    public static PlayerPc empty() {
        return new PlayerPc();
    }

    private static String[] defaultNames() {
        String[] n = new String[BOX_COUNT];
        for (int i = 0; i < BOX_COUNT; i++) {
            n[i] = "Box " + (i + 1);
        }
        return n;
    }

    private static String[] defaultWallpapers() {
        String[] w = new String[BOX_COUNT];
        for (int i = 0; i < BOX_COUNT; i++) {
            w[i] = DEFAULT_WALLPAPERS[i % DEFAULT_WALLPAPERS.length];
        }
        return w;
    }

    private static PlayerPc fromCodec(List<Slot> list, List<String> names, List<String> walls) {
        PlayerPc pc = new PlayerPc();
        if (list != null) {
            for (int i = 0; i < Math.min(list.size(), TOTAL_SLOTS); i++) {
                pc.slots[i] = list.get(i).mon().orElse(null);
            }
        }
        applyList(pc.boxNames, names, "Box ");
        applyList(pc.wallpapers, walls, "default");
        return pc;
    }

    private static PlayerPc fromStream(List<Slot> list, List<String> names, List<String> walls) {
        return fromCodec(list, names, walls);
    }

    private static void applyList(String[] target, List<String> src, String prefixOrDefault) {
        if (src == null) {
            return;
        }
        for (int i = 0; i < target.length && i < src.size(); i++) {
            if (src.get(i) != null && !src.get(i).isBlank()) {
                target[i] = src.get(i);
            }
        }
    }

    private List<Slot> slotList() {
        List<Slot> list = new ArrayList<>(TOTAL_SLOTS);
        for (OwnedMon mon : slots) {
            list.add(new Slot(Optional.ofNullable(mon)));
        }
        return list;
    }

    private List<String> boxNameList() {
        return List.of(boxNames);
    }

    private List<String> wallpaperList() {
        return List.of(wallpapers);
    }

    public String boxName(int box) {
        if (box < 0 || box >= BOX_COUNT) {
            return "Box";
        }
        return boxNames[box] != null ? boxNames[box] : ("Box " + (box + 1));
    }

    public String wallpaper(int box) {
        if (box < 0 || box >= BOX_COUNT) {
            return "default";
        }
        return wallpapers[box] != null ? wallpapers[box] : "default";
    }

    public void setBoxName(int box, String name) {
        if (box >= 0 && box < BOX_COUNT && name != null && !name.isBlank()) {
            boxNames[box] = name.length() > 24 ? name.substring(0, 24) : name;
        }
    }

    public void cycleWallpaper(int box) {
        if (box < 0 || box >= BOX_COUNT) {
            return;
        }
        String cur = wallpaper(box);
        int idx = 0;
        for (int i = 0; i < DEFAULT_WALLPAPERS.length; i++) {
            if (DEFAULT_WALLPAPERS[i].equals(cur)) {
                idx = i;
                break;
            }
        }
        wallpapers[box] = DEFAULT_WALLPAPERS[(idx + 1) % DEFAULT_WALLPAPERS.length];
    }

    /** Accent color for wallpaper themes (ARGB). */
    public static int wallpaperColor(String id) {
        if (id == null) {
            return 0xFF3A4A5A;
        }
        return switch (id) {
            case "forest" -> 0xFF2D5A3A;
            case "cave" -> 0xFF3A3540;
            case "ocean" -> 0xFF1E4A6E;
            case "nether" -> 0xFF6B2A2A;
            case "end" -> 0xFF3A2A55;
            default -> 0xFF3A4A5A;
        };
    }

    public Optional<OwnedMon> get(int box, int indexInBox) {
        int i = globalIndex(box, indexInBox);
        if (i < 0) {
            return Optional.empty();
        }
        return Optional.ofNullable(slots[i]);
    }

    public Optional<OwnedMon> getGlobal(int globalIndex) {
        if (globalIndex < 0 || globalIndex >= TOTAL_SLOTS) {
            return Optional.empty();
        }
        return Optional.ofNullable(slots[globalIndex]);
    }

    public boolean setGlobal(int globalIndex, OwnedMon mon) {
        if (globalIndex < 0 || globalIndex >= TOTAL_SLOTS) {
            return false;
        }
        slots[globalIndex] = mon;
        return true;
    }

    public boolean isEmpty(int globalIndex) {
        return getGlobal(globalIndex).isEmpty();
    }

    public int firstEmptyInBox(int box) {
        if (box < 0 || box >= BOX_COUNT) {
            return -1;
        }
        int start = box * BOX_SIZE;
        for (int i = 0; i < BOX_SIZE; i++) {
            if (slots[start + i] == null) {
                return start + i;
            }
        }
        return -1;
    }

    public int firstEmpty() {
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            if (slots[i] == null) {
                return i;
            }
        }
        return -1;
    }

    public boolean deposit(OwnedMon mon) {
        int i = firstEmpty();
        if (i < 0 || mon == null) {
            return false;
        }
        slots[i] = mon;
        return true;
    }

    public boolean depositToBox(int box, OwnedMon mon) {
        int i = firstEmptyInBox(box);
        if (i < 0 || mon == null) {
            return false;
        }
        slots[i] = mon;
        return true;
    }

    public Optional<OwnedMon> withdraw(int globalIndex) {
        if (globalIndex < 0 || globalIndex >= TOTAL_SLOTS || slots[globalIndex] == null) {
            return Optional.empty();
        }
        OwnedMon mon = slots[globalIndex];
        slots[globalIndex] = null;
        return Optional.of(mon);
    }

    public int count() {
        int n = 0;
        for (OwnedMon mon : slots) {
            if (mon != null) {
                n++;
            }
        }
        return n;
    }

    public boolean isFull() {
        return firstEmpty() < 0;
    }

    public static int globalIndex(int box, int indexInBox) {
        if (box < 0 || box >= BOX_COUNT || indexInBox < 0 || indexInBox >= BOX_SIZE) {
            return -1;
        }
        return box * BOX_SIZE + indexInBox;
    }

    public PlayerPc copy() {
        PlayerPc pc = new PlayerPc();
        System.arraycopy(this.slots, 0, pc.slots, 0, TOTAL_SLOTS);
        System.arraycopy(this.boxNames, 0, pc.boxNames, 0, BOX_COUNT);
        System.arraycopy(this.wallpapers, 0, pc.wallpapers, 0, BOX_COUNT);
        return pc;
    }

    public List<OwnedMon> allMons() {
        List<OwnedMon> list = new ArrayList<>();
        for (OwnedMon mon : slots) {
            if (mon != null) {
                list.add(mon);
            }
        }
        return Collections.unmodifiableList(list);
    }

    /** Codec helper for one PC slot. */
    public record Slot(Optional<OwnedMon> mon) {
        public static final Codec<Slot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                OwnedMon.CODEC.optionalFieldOf("mon").forGetter(Slot::mon)
        ).apply(instance, Slot::new));

        public static final StreamCodec<ByteBuf, Slot> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.optional(OwnedMon.STREAM_CODEC),
                Slot::mon,
                Slot::new
        );
    }
}
