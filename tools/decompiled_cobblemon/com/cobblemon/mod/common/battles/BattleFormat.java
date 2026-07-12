/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 *  kotlin.Metadata
 *  kotlin.collections.CollectionsKt
 *  kotlin.collections.SetsKt
 *  kotlin.jvm.internal.DefaultConstructorMarker
 *  kotlin.jvm.internal.Intrinsics
 *  kotlin.jvm.internal.Reflection
 *  kotlin.jvm.internal.SourceDebugExtension
 *  kotlin.reflect.KProperty1
 *  kotlin.text.StringsKt
 *  net.minecraft.network.RegistryFriendlyByteBuf
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package com.cobblemon.mod.common.battles;

import com.cobblemon.mod.common.battles.BattleRules;
import com.cobblemon.mod.common.battles.BattleType;
import com.cobblemon.mod.common.battles.BattleTypes;
import com.cobblemon.mod.common.net.IntSize;
import com.cobblemon.mod.common.util.BufferUtilsKt;
import com.cobblemon.mod.common.util.NetExtensionsKt;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import kotlin.Metadata;
import kotlin.collections.CollectionsKt;
import kotlin.collections.SetsKt;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.Reflection;
import kotlin.jvm.internal.SourceDebugExtension;
import kotlin.reflect.KProperty1;
import kotlin.text.StringsKt;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Metadata(mv={2, 2, 0}, k=1, xi=48, d1={"\u00002\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\"\n\u0000\n\u0002\u0010\b\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0010\n\u0002\u0010\u000b\n\u0002\b\u0013\b\u0086\b\u0018\u0000 02\u00020\u0001:\u00010B?\u0012\b\b\u0002\u0010\u0003\u001a\u00020\u0002\u0012\b\b\u0002\u0010\u0005\u001a\u00020\u0004\u0012\u000e\b\u0002\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00020\u0006\u0012\b\b\u0002\u0010\t\u001a\u00020\b\u0012\b\b\u0002\u0010\n\u001a\u00020\b\u00a2\u0006\u0004\b\u000b\u0010\fJ\u0015\u0010\u000f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\r\u00a2\u0006\u0004\b\u000f\u0010\u0010J\r\u0010\u0011\u001a\u00020\u0002\u00a2\u0006\u0004\b\u0011\u0010\u0012J\u0010\u0010\u0013\u001a\u00020\u0002H\u00c6\u0003\u00a2\u0006\u0004\b\u0013\u0010\u0012J\u0010\u0010\u0014\u001a\u00020\u0004H\u00c6\u0003\u00a2\u0006\u0004\b\u0014\u0010\u0015J\u0016\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u00020\u0006H\u00c6\u0003\u00a2\u0006\u0004\b\u0016\u0010\u0017J\u0010\u0010\u0018\u001a\u00020\bH\u00c6\u0003\u00a2\u0006\u0004\b\u0018\u0010\u0019J\u0010\u0010\u001a\u001a\u00020\bH\u00c6\u0003\u00a2\u0006\u0004\b\u001a\u0010\u0019JH\u0010\u001b\u001a\u00020\u00002\b\b\u0002\u0010\u0003\u001a\u00020\u00022\b\b\u0002\u0010\u0005\u001a\u00020\u00042\u000e\b\u0002\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00020\u00062\b\b\u0002\u0010\t\u001a\u00020\b2\b\b\u0002\u0010\n\u001a\u00020\bH\u00c6\u0001\u00a2\u0006\u0004\b\u001b\u0010\u001cJ\u001a\u0010\u001f\u001a\u00020\u001e2\b\u0010\u001d\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003\u00a2\u0006\u0004\b\u001f\u0010 J\u0010\u0010!\u001a\u00020\bH\u00d6\u0001\u00a2\u0006\u0004\b!\u0010\u0019J\u0010\u0010\"\u001a\u00020\u0002H\u00d6\u0001\u00a2\u0006\u0004\b\"\u0010\u0012R\u0017\u0010\u0003\u001a\u00020\u00028\u0006\u00a2\u0006\f\n\u0004\b\u0003\u0010#\u001a\u0004\b$\u0010\u0012R\u0017\u0010\u0005\u001a\u00020\u00048\u0006\u00a2\u0006\f\n\u0004\b\u0005\u0010%\u001a\u0004\b&\u0010\u0015R(\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00020\u00068\u0006@\u0006X\u0086\u000e\u00a2\u0006\u0012\n\u0004\b\u0007\u0010'\u001a\u0004\b(\u0010\u0017\"\u0004\b)\u0010*R\u0017\u0010\t\u001a\u00020\b8\u0006\u00a2\u0006\f\n\u0004\b\t\u0010+\u001a\u0004\b,\u0010\u0019R\"\u0010\n\u001a\u00020\b8\u0006@\u0006X\u0086\u000e\u00a2\u0006\u0012\n\u0004\b\n\u0010+\u001a\u0004\b-\u0010\u0019\"\u0004\b.\u0010/\u00a8\u00061"}, d2={"Lcom/cobblemon/mod/common/battles/BattleFormat;", "", "", "mod", "Lcom/cobblemon/mod/common/battles/BattleType;", "battleType", "", "ruleSet", "", "gen", "adjustLevel", "<init>", "(Ljava/lang/String;Lcom/cobblemon/mod/common/battles/BattleType;Ljava/util/Set;II)V", "Lnet/minecraft/network/RegistryFriendlyByteBuf;", "buffer", "saveToBuffer", "(Lnet/minecraft/network/RegistryFriendlyByteBuf;)Lnet/minecraft/network/RegistryFriendlyByteBuf;", "toFormatJSON", "()Ljava/lang/String;", "component1", "component2", "()Lcom/cobblemon/mod/common/battles/BattleType;", "component3", "()Ljava/util/Set;", "component4", "()I", "component5", "copy", "(Ljava/lang/String;Lcom/cobblemon/mod/common/battles/BattleType;Ljava/util/Set;II)Lcom/cobblemon/mod/common/battles/BattleFormat;", "other", "", "equals", "(Ljava/lang/Object;)Z", "hashCode", "toString", "Ljava/lang/String;", "getMod", "Lcom/cobblemon/mod/common/battles/BattleType;", "getBattleType", "Ljava/util/Set;", "getRuleSet", "setRuleSet", "(Ljava/util/Set;)V", "I", "getGen", "getAdjustLevel", "setAdjustLevel", "(I)V", "Companion", "common"})
@SourceDebugExtension(value={"SMAP\nBattleFormat.kt\nKotlin\n*S Kotlin\n*F\n+ 1 BattleFormat.kt\ncom/cobblemon/mod/common/battles/BattleFormat\n+ 2 _Collections.kt\nkotlin/collections/CollectionsKt___CollectionsKt\n*L\n1#1,124:1\n1869#2,2:125\n774#2:127\n865#2,2:128\n*S KotlinDebug\n*F\n+ 1 BattleFormat.kt\ncom/cobblemon/mod/common/battles/BattleFormat\n*L\n102#1:125,2\n113#1:127\n113#1:128,2\n*E\n"})
public final class BattleFormat {
    @NotNull
    public static final Companion Companion = new Companion(null);
    @NotNull
    private final String mod;
    @NotNull
    private final BattleType battleType;
    @NotNull
    private Set<String> ruleSet;
    private final int gen;
    private int adjustLevel;
    @NotNull
    private static final BattleFormat GEN_9_SINGLES;
    @NotNull
    private static final BattleFormat GEN_9_DOUBLES;
    @NotNull
    private static final BattleFormat GEN_9_TRIPLES;
    @NotNull
    private static final BattleFormat GEN_9_MULTI;
    @NotNull
    private static final BattleFormat GEN_9_ROYAL;

    public BattleFormat(@NotNull String mod, @NotNull BattleType battleType, @NotNull Set<String> ruleSet, int gen, int adjustLevel) {
        Intrinsics.checkNotNullParameter((Object)mod, (String)"mod");
        Intrinsics.checkNotNullParameter((Object)battleType, (String)"battleType");
        Intrinsics.checkNotNullParameter(ruleSet, (String)"ruleSet");
        this.mod = mod;
        this.battleType = battleType;
        this.ruleSet = ruleSet;
        this.gen = gen;
        this.adjustLevel = adjustLevel;
    }

    public /* synthetic */ BattleFormat(String string, BattleType battleType, Set set, int n, int n2, int n3, DefaultConstructorMarker defaultConstructorMarker) {
        if ((n3 & 1) != 0) {
            string = "cobblemon";
        }
        if ((n3 & 2) != 0) {
            battleType = BattleTypes.INSTANCE.getSINGLES();
        }
        if ((n3 & 4) != 0) {
            set = SetsKt.emptySet();
        }
        if ((n3 & 8) != 0) {
            n = 9;
        }
        if ((n3 & 0x10) != 0) {
            n2 = -1;
        }
        this(string, battleType, set, n, n2);
    }

    @NotNull
    public final String getMod() {
        return this.mod;
    }

    @NotNull
    public final BattleType getBattleType() {
        return this.battleType;
    }

    @NotNull
    public final Set<String> getRuleSet() {
        return this.ruleSet;
    }

    public final void setRuleSet(@NotNull Set<String> set) {
        Intrinsics.checkNotNullParameter(set, (String)"<set-?>");
        this.ruleSet = set;
    }

    public final int getGen() {
        return this.gen;
    }

    public final int getAdjustLevel() {
        return this.adjustLevel;
    }

    public final void setAdjustLevel(int n) {
        this.adjustLevel = n;
    }

    @NotNull
    public final RegistryFriendlyByteBuf saveToBuffer(@NotNull RegistryFriendlyByteBuf buffer) {
        Intrinsics.checkNotNullParameter((Object)buffer, (String)"buffer");
        BufferUtilsKt.writeString((ByteBuf)buffer, this.mod);
        this.battleType.saveToBuffer(buffer);
        NetExtensionsKt.writeSizedInt((ByteBuf)buffer, IntSize.U_BYTE, this.ruleSet.size());
        Iterable $this$forEach$iv = this.ruleSet;
        boolean $i$f$forEach = false;
        for (Object element$iv : $this$forEach$iv) {
            String p0 = (String)element$iv;
            boolean bl = false;
            BufferUtilsKt.writeString((ByteBuf)buffer, p0);
        }
        NetExtensionsKt.writeSizedInt((ByteBuf)buffer, IntSize.INT, this.adjustLevel);
        return buffer;
    }

    /*
     * WARNING - void declaration
     */
    @NotNull
    public final String toFormatJSON() {
        void $this$filterTo$iv$iv;
        Iterable $this$filter$iv = this.ruleSet;
        boolean $i$f$filter = false;
        Iterable iterable = $this$filter$iv;
        Collection destination$iv$iv = new ArrayList();
        boolean $i$f$filterTo = false;
        for (Object element$iv$iv : $this$filterTo$iv$iv) {
            String it = (String)element$iv$iv;
            boolean bl = false;
            if (!(!Intrinsics.areEqual((Object)it, (Object)"Bag Clause"))) continue;
            destination$iv$iv.add(element$iv$iv);
        }
        List rulesToSendToShowdown = (List)destination$iv$iv;
        return StringsKt.replace$default((String)StringsKt.trimIndent((String)("\n            {\n                \"mod\": \"" + this.mod + "\",\n                \"gameType\": \"" + this.battleType.getName() + "\",\n                \"gen\": " + this.gen + ",\n                \"ruleset\": [" + CollectionsKt.joinToString$default((Iterable)rulesToSendToShowdown, null, null, null, (int)0, null, BattleFormat::toFormatJSON$lambda$1, (int)31, null) + "],\n                \"effectType\": \"Format\"\n            }\n        ")), (String)"\n", (String)"", (boolean)false, (int)4, null);
    }

    @NotNull
    public final String component1() {
        return this.mod;
    }

    @NotNull
    public final BattleType component2() {
        return this.battleType;
    }

    @NotNull
    public final Set<String> component3() {
        return this.ruleSet;
    }

    public final int component4() {
        return this.gen;
    }

    public final int component5() {
        return this.adjustLevel;
    }

    @NotNull
    public final BattleFormat copy(@NotNull String mod, @NotNull BattleType battleType, @NotNull Set<String> ruleSet, int gen, int adjustLevel) {
        Intrinsics.checkNotNullParameter((Object)mod, (String)"mod");
        Intrinsics.checkNotNullParameter((Object)battleType, (String)"battleType");
        Intrinsics.checkNotNullParameter(ruleSet, (String)"ruleSet");
        return new BattleFormat(mod, battleType, ruleSet, gen, adjustLevel);
    }

    public static /* synthetic */ BattleFormat copy$default(BattleFormat battleFormat, String string, BattleType battleType, Set set, int n, int n2, int n3, Object object) {
        if ((n3 & 1) != 0) {
            string = battleFormat.mod;
        }
        if ((n3 & 2) != 0) {
            battleType = battleFormat.battleType;
        }
        if ((n3 & 4) != 0) {
            set = battleFormat.ruleSet;
        }
        if ((n3 & 8) != 0) {
            n = battleFormat.gen;
        }
        if ((n3 & 0x10) != 0) {
            n2 = battleFormat.adjustLevel;
        }
        return battleFormat.copy(string, battleType, set, n, n2);
    }

    @NotNull
    public String toString() {
        return "BattleFormat(mod=" + this.mod + ", battleType=" + this.battleType + ", ruleSet=" + this.ruleSet + ", gen=" + this.gen + ", adjustLevel=" + this.adjustLevel + ")";
    }

    public int hashCode() {
        int result = this.mod.hashCode();
        result = result * 31 + this.battleType.hashCode();
        result = result * 31 + ((Object)this.ruleSet).hashCode();
        result = result * 31 + Integer.hashCode(this.gen);
        result = result * 31 + Integer.hashCode(this.adjustLevel);
        return result;
    }

    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BattleFormat)) {
            return false;
        }
        BattleFormat battleFormat = (BattleFormat)other;
        if (!Intrinsics.areEqual((Object)this.mod, (Object)battleFormat.mod)) {
            return false;
        }
        if (!Intrinsics.areEqual((Object)this.battleType, (Object)battleFormat.battleType)) {
            return false;
        }
        if (!Intrinsics.areEqual(this.ruleSet, battleFormat.ruleSet)) {
            return false;
        }
        if (this.gen != battleFormat.gen) {
            return false;
        }
        return this.adjustLevel == battleFormat.adjustLevel;
    }

    private static final CharSequence toFormatJSON$lambda$1(String it) {
        Intrinsics.checkNotNullParameter((Object)it, (String)"it");
        return "\"" + it + "\"";
    }

    public BattleFormat() {
        this(null, null, null, 0, 0, 31, null);
    }

    static {
        Object[] objectArray = new String[]{"Obtainable", "+Past", "+Unobtainable"};
        GEN_9_SINGLES = new BattleFormat(null, BattleTypes.INSTANCE.getSINGLES(), SetsKt.mutableSetOf((Object[])objectArray), 0, 0, 25, null);
        objectArray = new String[]{"Obtainable", "+Past", "+Unobtainable"};
        GEN_9_DOUBLES = new BattleFormat(null, BattleTypes.INSTANCE.getDOUBLES(), SetsKt.mutableSetOf((Object[])objectArray), 0, 0, 25, null);
        objectArray = new String[]{"Obtainable", "+Past", "+Unobtainable"};
        GEN_9_TRIPLES = new BattleFormat(null, BattleTypes.INSTANCE.getTRIPLES(), SetsKt.mutableSetOf((Object[])objectArray), 0, 0, 25, null);
        objectArray = new String[]{"Obtainable", "+Past", "+Unobtainable"};
        GEN_9_MULTI = new BattleFormat(null, BattleTypes.INSTANCE.getMULTI(), SetsKt.mutableSetOf((Object[])objectArray), 0, 0, 25, null);
        objectArray = new String[]{"Obtainable", "+Past", "+Unobtainable"};
        GEN_9_ROYAL = new BattleFormat(null, BattleTypes.INSTANCE.getROYAL(), SetsKt.mutableSetOf((Object[])objectArray), 0, 0, 25, null);
    }

    @Metadata(mv={2, 2, 0}, k=1, xi=48, d1={"\u0000&\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\"\n\u0002\u0010\u000e\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0010\b\u0086\u0003\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003J'\u0010\t\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00042\u0010\b\u0002\u0010\b\u001a\n\u0012\u0004\u0012\u00020\u0007\u0018\u00010\u0006\u00a2\u0006\u0004\b\t\u0010\nJ\u0015\u0010\f\u001a\u00020\u00042\u0006\u0010\u000b\u001a\u00020\u0007\u00a2\u0006\u0004\b\f\u0010\rJ\u0015\u0010\u0010\u001a\u00020\u00042\u0006\u0010\u000f\u001a\u00020\u000e\u00a2\u0006\u0004\b\u0010\u0010\u0011R\u0017\u0010\u0012\u001a\u00020\u00048\u0006\u00a2\u0006\f\n\u0004\b\u0012\u0010\u0013\u001a\u0004\b\u0014\u0010\u0015R\u0017\u0010\u0016\u001a\u00020\u00048\u0006\u00a2\u0006\f\n\u0004\b\u0016\u0010\u0013\u001a\u0004\b\u0017\u0010\u0015R\u0017\u0010\u0018\u001a\u00020\u00048\u0006\u00a2\u0006\f\n\u0004\b\u0018\u0010\u0013\u001a\u0004\b\u0019\u0010\u0015R\u0017\u0010\u001a\u001a\u00020\u00048\u0006\u00a2\u0006\f\n\u0004\b\u001a\u0010\u0013\u001a\u0004\b\u001b\u0010\u0015R\u0017\u0010\u001c\u001a\u00020\u00048\u0006\u00a2\u0006\f\n\u0004\b\u001c\u0010\u0013\u001a\u0004\b\u001d\u0010\u0015\u00a8\u0006\u001e"}, d2={"Lcom/cobblemon/mod/common/battles/BattleFormat$Companion;", "", "<init>", "()V", "Lcom/cobblemon/mod/common/battles/BattleFormat;", "battleFormat", "", "", "rules", "setBattleRules", "(Lcom/cobblemon/mod/common/battles/BattleFormat;Ljava/util/Set;)Lcom/cobblemon/mod/common/battles/BattleFormat;", "id", "fromFormatIdentifier", "(Ljava/lang/String;)Lcom/cobblemon/mod/common/battles/BattleFormat;", "Lnet/minecraft/network/RegistryFriendlyByteBuf;", "buffer", "loadFromBuffer", "(Lnet/minecraft/network/RegistryFriendlyByteBuf;)Lcom/cobblemon/mod/common/battles/BattleFormat;", "GEN_9_SINGLES", "Lcom/cobblemon/mod/common/battles/BattleFormat;", "getGEN_9_SINGLES", "()Lcom/cobblemon/mod/common/battles/BattleFormat;", "GEN_9_DOUBLES", "getGEN_9_DOUBLES", "GEN_9_TRIPLES", "getGEN_9_TRIPLES", "GEN_9_MULTI", "getGEN_9_MULTI", "GEN_9_ROYAL", "getGEN_9_ROYAL", "common"})
    @SourceDebugExtension(value={"SMAP\nBattleFormat.kt\nKotlin\n*S Kotlin\n*F\n+ 1 BattleFormat.kt\ncom/cobblemon/mod/common/battles/BattleFormat$Companion\n+ 2 _Collections.kt\nkotlin/collections/CollectionsKt___CollectionsKt\n+ 3 fake.kt\nkotlin/jvm/internal/FakeKt\n*L\n1#1,124:1\n774#2:125\n865#2,2:126\n1617#2,9:128\n1869#2:137\n808#2,11:138\n1870#2:151\n1626#2:152\n1#3:149\n1#3:150\n*S KotlinDebug\n*F\n+ 1 BattleFormat.kt\ncom/cobblemon/mod/common/battles/BattleFormat$Companion\n*L\n37#1:125\n37#1:126,2\n38#1:128,9\n38#1:137\n40#1:138,11\n38#1:151\n38#1:152\n38#1:150\n*E\n"})
    public static final class Companion {
        private Companion() {
        }

        /*
         * WARNING - void declaration
         */
        @NotNull
        public final BattleFormat setBattleRules(@NotNull BattleFormat battleFormat, @Nullable Set<String> rules) {
            Set set;
            List ruleValues;
            List list;
            List list2;
            Set filteredRules;
            Set set2;
            Iterable $this$filterTo$iv$iv;
            Collection destination$iv$iv;
            Intrinsics.checkNotNullParameter((Object)battleFormat, (String)"battleFormat");
            Set<String> set3 = rules;
            if (set3 != null) {
                Iterable $this$filter$iv = set3;
                boolean $i$f$filter = false;
                Iterable iterable = $this$filter$iv;
                destination$iv$iv = new ArrayList();
                boolean $i$f$filterTo = false;
                for (Object element$iv$iv : $this$filterTo$iv$iv) {
                    String it = (String)element$iv$iv;
                    boolean bl = false;
                    boolean bl2 = !StringsKt.isBlank((CharSequence)it);
                    if (!bl2) continue;
                    destination$iv$iv.add(element$iv$iv);
                }
                set2 = CollectionsKt.toSet((Iterable)((List)destination$iv$iv));
            } else {
                set2 = null;
            }
            Set set4 = filteredRules = set2;
            if (set4 != null) {
                void $this$mapNotNullTo$iv$iv;
                Iterable $this$mapNotNull$iv = set4;
                boolean $i$f$mapNotNull = false;
                $this$filterTo$iv$iv = $this$mapNotNull$iv;
                destination$iv$iv = new ArrayList();
                boolean $i$f$mapNotNullTo = false;
                void $this$forEach$iv$iv$iv = $this$mapNotNullTo$iv$iv;
                boolean $i$f$forEach = false;
                Iterator iterator = $this$forEach$iv$iv$iv.iterator();
                while (iterator.hasNext()) {
                    String it$iv$iv;
                    Object v4;
                    Iterable iterable;
                    block10: {
                        void $this$filterIsInstanceTo$iv$iv;
                        void $this$filterIsInstance$iv;
                        Object element$iv$iv$iv;
                        Object element$iv$iv = element$iv$iv$iv = iterator.next();
                        boolean bl = false;
                        String ruleName = (String)element$iv$iv;
                        boolean bl3 = false;
                        iterable = Reflection.getOrCreateKotlinClass(BattleRules.class).getMembers();
                        boolean $i$f$filterIsInstance = false;
                        Iterator iterator2 = $this$filterIsInstance$iv;
                        Collection destination$iv$iv2 = new ArrayList();
                        boolean $i$f$filterIsInstanceTo = false;
                        for (Object element$iv$iv2 : $this$filterIsInstanceTo$iv$iv) {
                            if (!(element$iv$iv2 instanceof KProperty1)) continue;
                            destination$iv$iv2.add(element$iv$iv2);
                        }
                        Iterable iterable2 = (List)destination$iv$iv2;
                        for (Object t : iterable2) {
                            KProperty1 it = (KProperty1)t;
                            boolean bl4 = false;
                            if (!Intrinsics.areEqual((Object)it.getName(), (Object)ruleName)) continue;
                            v4 = t;
                            break block10;
                        }
                        v4 = null;
                    }
                    KProperty1 kProperty1 = v4;
                    if ((kProperty1 != null && (iterable = kProperty1.getGetter()) != null ? (String)iterable.call(new Object[0]) : null) == null) continue;
                    it$iv$iv = it$iv$iv;
                    boolean bl = false;
                    destination$iv$iv.add(it$iv$iv);
                }
                list2 = (List)destination$iv$iv;
            } else {
                list2 = list = null;
            }
            if (list2 == null) {
                list = ruleValues = CollectionsKt.emptyList();
            }
            if ((set = filteredRules) == null) {
                set = SetsKt.plus(battleFormat.getRuleSet(), (Iterable)ruleValues);
            }
            return BattleFormat.copy$default(battleFormat, null, null, set, 0, 0, 27, null);
        }

        public static /* synthetic */ BattleFormat setBattleRules$default(Companion companion, BattleFormat battleFormat, Set set, int n, Object object) {
            if ((n & 2) != 0) {
                set = null;
            }
            return companion.setBattleRules(battleFormat, set);
        }

        @NotNull
        public final BattleFormat fromFormatIdentifier(@NotNull String id) {
            Intrinsics.checkNotNullParameter((Object)id, (String)"id");
            return switch (id) {
                case "single", "singles", "single_battle" -> Companion.getGEN_9_SINGLES();
                case "double_battle", "doubles", "double" -> Companion.getGEN_9_DOUBLES();
                case "triples", "triple", "triple_battle" -> Companion.getGEN_9_TRIPLES();
                default -> Companion.getGEN_9_SINGLES();
            };
        }

        @NotNull
        public final BattleFormat getGEN_9_SINGLES() {
            return GEN_9_SINGLES;
        }

        @NotNull
        public final BattleFormat getGEN_9_DOUBLES() {
            return GEN_9_DOUBLES;
        }

        @NotNull
        public final BattleFormat getGEN_9_TRIPLES() {
            return GEN_9_TRIPLES;
        }

        @NotNull
        public final BattleFormat getGEN_9_MULTI() {
            return GEN_9_MULTI;
        }

        @NotNull
        public final BattleFormat getGEN_9_ROYAL() {
            return GEN_9_ROYAL;
        }

        @NotNull
        public final BattleFormat loadFromBuffer(@NotNull RegistryFriendlyByteBuf buffer) {
            Intrinsics.checkNotNullParameter((Object)buffer, (String)"buffer");
            String mod = BufferUtilsKt.readString((ByteBuf)buffer);
            BattleType battleType = BattleType.Companion.loadFromBuffer(buffer);
            Set ruleSet = new LinkedHashSet();
            int n = NetExtensionsKt.readSizedInt((ByteBuf)buffer, IntSize.U_BYTE);
            int n2 = 0;
            while (n2 < n) {
                int it = n2++;
                boolean bl = false;
                ruleSet.add(BufferUtilsKt.readString((ByteBuf)buffer));
            }
            int adjustLevel = NetExtensionsKt.readSizedInt((ByteBuf)buffer, IntSize.INT);
            return new BattleFormat(mod, battleType, ruleSet, 0, adjustLevel, 8, null);
        }

        public /* synthetic */ Companion(DefaultConstructorMarker $constructor_marker) {
            this();
        }
    }
}
