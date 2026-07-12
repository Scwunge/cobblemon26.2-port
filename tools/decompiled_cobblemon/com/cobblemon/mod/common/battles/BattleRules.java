/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  kotlin.Metadata
 *  org.jetbrains.annotations.NotNull
 */
package com.cobblemon.mod.common.battles;

import kotlin.Metadata;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={2, 2, 0}, k=1, xi=48, d1={"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u000b\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003R\u0014\u0010\u0005\u001a\u00020\u00048\u0006X\u0086T\u00a2\u0006\u0006\n\u0004\b\u0005\u0010\u0006R\u0014\u0010\u0007\u001a\u00020\u00048\u0006X\u0086T\u00a2\u0006\u0006\n\u0004\b\u0007\u0010\u0006R\u0014\u0010\b\u001a\u00020\u00048\u0006X\u0086T\u00a2\u0006\u0006\n\u0004\b\b\u0010\u0006R\u0014\u0010\t\u001a\u00020\u00048\u0006X\u0086T\u00a2\u0006\u0006\n\u0004\b\t\u0010\u0006R\u0014\u0010\n\u001a\u00020\u00048\u0006X\u0086T\u00a2\u0006\u0006\n\u0004\b\n\u0010\u0006R\u0014\u0010\u000b\u001a\u00020\u00048\u0006X\u0086T\u00a2\u0006\u0006\n\u0004\b\u000b\u0010\u0006R\u0014\u0010\f\u001a\u00020\u00048\u0006X\u0086T\u00a2\u0006\u0006\n\u0004\b\f\u0010\u0006R\u0014\u0010\r\u001a\u00020\u00048\u0006X\u0086T\u00a2\u0006\u0006\n\u0004\b\r\u0010\u0006R\u0014\u0010\u000e\u001a\u00020\u00048\u0006X\u0086T\u00a2\u0006\u0006\n\u0004\b\u000e\u0010\u0006\u00a8\u0006\u000f"}, d2={"Lcom/cobblemon/mod/common/battles/BattleRules;", "", "<init>", "()V", "", "OBTAINABLE", "Ljava/lang/String;", "PAST", "UNOBTAINABLE", "BAG_CLAUSE", "TEAM_PREVIEW", "ENDLESS_BATTLE_CLAUSE", "CANCEL_MOD", "SLEEP_CLAUSE", "HP_PERCENTAGE_MOD", "common"})
public final class BattleRules {
    @NotNull
    public static final BattleRules INSTANCE = new BattleRules();
    @NotNull
    public static final String OBTAINABLE = "Obtainable";
    @NotNull
    public static final String PAST = "+Past";
    @NotNull
    public static final String UNOBTAINABLE = "+Unobtainable";
    @NotNull
    public static final String BAG_CLAUSE = "Bag Clause";
    @NotNull
    public static final String TEAM_PREVIEW = "Team Preview";
    @NotNull
    public static final String ENDLESS_BATTLE_CLAUSE = "Endless Battle Clause";
    @NotNull
    public static final String CANCEL_MOD = "Cancel Mod";
    @NotNull
    public static final String SLEEP_CLAUSE = "Sleep Clause";
    @NotNull
    public static final String HP_PERCENTAGE_MOD = "HP Percentage Mod";

    private BattleRules() {
    }
}
