/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  kotlin.Metadata
 *  kotlin.jvm.internal.Intrinsics
 *  net.minecraft.network.chat.MutableComponent
 *  org.jetbrains.annotations.NotNull
 */
package com.cobblemon.mod.common.battles;

import com.cobblemon.mod.common.battles.BattleType;
import com.cobblemon.mod.common.util.LocalizationUtilsKt;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={2, 2, 0}, k=1, xi=48, d1={"\u0000(\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u000f\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003J/\u0010\f\u001a\u00020\u000b2\u0006\u0010\u0005\u001a\u00020\u00042\b\b\u0002\u0010\u0007\u001a\u00020\u00062\u0006\u0010\t\u001a\u00020\b2\u0006\u0010\n\u001a\u00020\b\u00a2\u0006\u0004\b\f\u0010\rR\u0017\u0010\u000e\u001a\u00020\u000b8\u0006\u00a2\u0006\f\n\u0004\b\u000e\u0010\u000f\u001a\u0004\b\u0010\u0010\u0011R\u0017\u0010\u0012\u001a\u00020\u000b8\u0006\u00a2\u0006\f\n\u0004\b\u0012\u0010\u000f\u001a\u0004\b\u0013\u0010\u0011R\u0017\u0010\u0014\u001a\u00020\u000b8\u0006\u00a2\u0006\f\n\u0004\b\u0014\u0010\u000f\u001a\u0004\b\u0015\u0010\u0011R\u0017\u0010\u0016\u001a\u00020\u000b8\u0006\u00a2\u0006\f\n\u0004\b\u0016\u0010\u000f\u001a\u0004\b\u0017\u0010\u0011R\u0017\u0010\u0018\u001a\u00020\u000b8\u0006\u00a2\u0006\f\n\u0004\b\u0018\u0010\u000f\u001a\u0004\b\u0019\u0010\u0011\u00a8\u0006\u001a"}, d2={"Lcom/cobblemon/mod/common/battles/BattleTypes;", "", "<init>", "()V", "", "name", "Lnet/minecraft/network/chat/MutableComponent;", "displayName", "", "actorsPerSide", "slotsPerActor", "Lcom/cobblemon/mod/common/battles/BattleType;", "makeBattleType", "(Ljava/lang/String;Lnet/minecraft/network/chat/MutableComponent;II)Lcom/cobblemon/mod/common/battles/BattleType;", "SINGLES", "Lcom/cobblemon/mod/common/battles/BattleType;", "getSINGLES", "()Lcom/cobblemon/mod/common/battles/BattleType;", "DOUBLES", "getDOUBLES", "TRIPLES", "getTRIPLES", "MULTI", "getMULTI", "ROYAL", "getROYAL", "common"})
public final class BattleTypes {
    @NotNull
    public static final BattleTypes INSTANCE = new BattleTypes();
    @NotNull
    private static final BattleType SINGLES = BattleTypes.makeBattleType$default(INSTANCE, "singles", null, 1, 1, 2, null);
    @NotNull
    private static final BattleType DOUBLES = BattleTypes.makeBattleType$default(INSTANCE, "doubles", null, 1, 2, 2, null);
    @NotNull
    private static final BattleType TRIPLES = BattleTypes.makeBattleType$default(INSTANCE, "triples", null, 1, 3, 2, null);
    @NotNull
    private static final BattleType MULTI = BattleTypes.makeBattleType$default(INSTANCE, "multi", null, 2, 1, 2, null);
    @NotNull
    private static final BattleType ROYAL = BattleTypes.makeBattleType$default(INSTANCE, "freeforall", null, 1, 1, 2, null);

    private BattleTypes() {
    }

    @NotNull
    public final BattleType getSINGLES() {
        return SINGLES;
    }

    @NotNull
    public final BattleType getDOUBLES() {
        return DOUBLES;
    }

    @NotNull
    public final BattleType getTRIPLES() {
        return TRIPLES;
    }

    @NotNull
    public final BattleType getMULTI() {
        return MULTI;
    }

    @NotNull
    public final BattleType getROYAL() {
        return ROYAL;
    }

    @NotNull
    public final BattleType makeBattleType(@NotNull String name, @NotNull MutableComponent displayName, int actorsPerSide, int slotsPerActor) {
        Intrinsics.checkNotNullParameter((Object)name, (String)"name");
        Intrinsics.checkNotNullParameter((Object)displayName, (String)"displayName");
        return new BattleType(name, displayName, actorsPerSide, slotsPerActor){
            private final String name;
            private final MutableComponent displayName;
            private final int actorsPerSide;
            private final int slotsPerActor;
            {
                this.name = $name;
                this.displayName = $displayName;
                this.actorsPerSide = $actorsPerSide;
                this.slotsPerActor = $slotsPerActor;
            }

            public String getName() {
                return this.name;
            }

            public MutableComponent getDisplayName() {
                return this.displayName;
            }

            public int getActorsPerSide() {
                return this.actorsPerSide;
            }

            public int getSlotsPerActor() {
                return this.slotsPerActor;
            }
        };
    }

    public static /* synthetic */ BattleType makeBattleType$default(BattleTypes battleTypes, String string, MutableComponent mutableComponent, int n, int n2, int n3, Object object) {
        if ((n3 & 2) != 0) {
            MutableComponent mutableComponent2 = LocalizationUtilsKt.lang("battle.types." + string, new Object[0]);
            Intrinsics.checkNotNullExpressionValue((Object)mutableComponent2, (String)"lang(...)");
            mutableComponent = mutableComponent2;
        }
        return battleTypes.makeBattleType(string, mutableComponent, n, n2);
    }
}
