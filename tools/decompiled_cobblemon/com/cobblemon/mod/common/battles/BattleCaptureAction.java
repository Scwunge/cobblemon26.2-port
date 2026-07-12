/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  kotlin.Metadata
 *  kotlin.Unit
 *  kotlin.jvm.functions.Function0
 *  kotlin.jvm.functions.Function1
 *  kotlin.jvm.internal.Intrinsics
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.network.syncher.EntityDataAccessor
 *  org.jetbrains.annotations.NotNull
 */
package com.cobblemon.mod.common.battles;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.reactive.Observable;
import com.cobblemon.mod.common.api.text.TextKt;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.entity.pokeball.EmptyPokeBallEntity;
import com.cobblemon.mod.common.net.messages.client.battle.BattleCaptureEndPacket;
import com.cobblemon.mod.common.net.messages.client.battle.BattleCaptureShakePacket;
import com.cobblemon.mod.common.net.messages.client.battle.BattleCaptureStartPacket;
import com.cobblemon.mod.common.util.LocalizationUtilsKt;
import kotlin.Metadata;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={2, 2, 0}, k=1, xi=48, d1={"\u0000,\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u000b\n\u0002\u0018\u0002\n\u0002\b\u0005\u0018\u00002\u00020\u0001B\u001f\u0012\u0006\u0010\u0003\u001a\u00020\u0002\u0012\u0006\u0010\u0005\u001a\u00020\u0004\u0012\u0006\u0010\u0007\u001a\u00020\u0006\u00a2\u0006\u0004\b\b\u0010\tJ\r\u0010\u000b\u001a\u00020\n\u00a2\u0006\u0004\b\u000b\u0010\fR\u0017\u0010\u0003\u001a\u00020\u00028\u0006\u00a2\u0006\f\n\u0004\b\u0003\u0010\r\u001a\u0004\b\u000e\u0010\u000fR\u0017\u0010\u0005\u001a\u00020\u00048\u0006\u00a2\u0006\f\n\u0004\b\u0005\u0010\u0010\u001a\u0004\b\u0011\u0010\u0012R\u0017\u0010\u0007\u001a\u00020\u00068\u0006\u00a2\u0006\f\n\u0004\b\u0007\u0010\u0013\u001a\u0004\b\u0014\u0010\u0015R\u0017\u0010\u0017\u001a\u00020\u00168\u0006\u00a2\u0006\f\n\u0004\b\u0017\u0010\u0018\u001a\u0004\b\u0019\u0010\u001a\u00a8\u0006\u001b"}, d2={"Lcom/cobblemon/mod/common/battles/BattleCaptureAction;", "", "Lcom/cobblemon/mod/common/api/battles/model/PokemonBattle;", "battle", "Lcom/cobblemon/mod/common/battles/ActiveBattlePokemon;", "targetPokemon", "Lcom/cobblemon/mod/common/entity/pokeball/EmptyPokeBallEntity;", "pokeBallEntity", "<init>", "(Lcom/cobblemon/mod/common/api/battles/model/PokemonBattle;Lcom/cobblemon/mod/common/battles/ActiveBattlePokemon;Lcom/cobblemon/mod/common/entity/pokeball/EmptyPokeBallEntity;)V", "", "attach", "()V", "Lcom/cobblemon/mod/common/api/battles/model/PokemonBattle;", "getBattle", "()Lcom/cobblemon/mod/common/api/battles/model/PokemonBattle;", "Lcom/cobblemon/mod/common/battles/ActiveBattlePokemon;", "getTargetPokemon", "()Lcom/cobblemon/mod/common/battles/ActiveBattlePokemon;", "Lcom/cobblemon/mod/common/entity/pokeball/EmptyPokeBallEntity;", "getPokeBallEntity", "()Lcom/cobblemon/mod/common/entity/pokeball/EmptyPokeBallEntity;", "Lnet/minecraft/network/chat/MutableComponent;", "pokemonName", "Lnet/minecraft/network/chat/MutableComponent;", "getPokemonName", "()Lnet/minecraft/network/chat/MutableComponent;", "common"})
public final class BattleCaptureAction {
    @NotNull
    private final PokemonBattle battle;
    @NotNull
    private final ActiveBattlePokemon targetPokemon;
    @NotNull
    private final EmptyPokeBallEntity pokeBallEntity;
    @NotNull
    private final MutableComponent pokemonName;

    public BattleCaptureAction(@NotNull PokemonBattle battle, @NotNull ActiveBattlePokemon targetPokemon, @NotNull EmptyPokeBallEntity pokeBallEntity) {
        Intrinsics.checkNotNullParameter((Object)battle, (String)"battle");
        Intrinsics.checkNotNullParameter((Object)targetPokemon, (String)"targetPokemon");
        Intrinsics.checkNotNullParameter((Object)pokeBallEntity, (String)"pokeBallEntity");
        this.battle = battle;
        this.targetPokemon = targetPokemon;
        this.pokeBallEntity = pokeBallEntity;
        BattlePokemon battlePokemon = this.targetPokemon.getBattlePokemon();
        if (battlePokemon == null || (battlePokemon = battlePokemon.getName()) == null) {
            battlePokemon = TextKt.red("error");
        }
        this.pokemonName = battlePokemon;
    }

    @NotNull
    public final PokemonBattle getBattle() {
        return this.battle;
    }

    @NotNull
    public final ActiveBattlePokemon getTargetPokemon() {
        return this.targetPokemon;
    }

    @NotNull
    public final EmptyPokeBallEntity getPokeBallEntity() {
        return this.pokeBallEntity;
    }

    @NotNull
    public final MutableComponent getPokemonName() {
        return this.pokemonName;
    }

    public final void attach() {
        this.battle.sendUpdate(new BattleCaptureStartPacket(this.pokeBallEntity.getPokeBall().getName(), this.pokeBallEntity.getAspects(), this.targetPokemon.getPNX()));
        Observable.subscribe$default(this.pokeBallEntity.getDataTrackerEmitter().pipe(Observable.Companion.filter(BattleCaptureAction::attach$lambda$0), Observable.Companion.emitWhile(arg_0 -> BattleCaptureAction.attach$lambda$1(this, arg_0))), null, arg_0 -> BattleCaptureAction.attach$lambda$2(this, arg_0), 1, null);
        this.pokeBallEntity.getCaptureFuture().thenAccept(arg_0 -> BattleCaptureAction.attach$lambda$4(arg_0 -> BattleCaptureAction.attach$lambda$3(this, arg_0), arg_0));
    }

    private static final boolean attach$lambda$0(EntityDataAccessor it) {
        Intrinsics.checkNotNullParameter((Object)it, (String)"it");
        return Intrinsics.areEqual((Object)it, EmptyPokeBallEntity.Companion.getSHAKE());
    }

    private static final boolean attach$lambda$1(BattleCaptureAction this$0, EntityDataAccessor it) {
        Intrinsics.checkNotNullParameter((Object)it, (String)"it");
        return this$0.pokeBallEntity.isAlive() && this$0.battle.getCaptureActions().contains(this$0);
    }

    private static final Unit attach$lambda$2(BattleCaptureAction this$0, EntityDataAccessor it) {
        Intrinsics.checkNotNullParameter((Object)it, (String)"it");
        PokemonBattle pokemonBattle = this$0.battle;
        String string = this$0.targetPokemon.getPNX();
        Object object = this$0.pokeBallEntity.getEntityData().get(EmptyPokeBallEntity.Companion.getSHAKE());
        Intrinsics.checkNotNullExpressionValue((Object)object, (String)"get(...)");
        pokemonBattle.sendUpdate(new BattleCaptureShakePacket(string, (Boolean)object));
        return Unit.INSTANCE;
    }

    private static final Unit attach$lambda$3(BattleCaptureAction this$0, Boolean successful) {
        if (successful.booleanValue()) {
            BattlePokemon battlePokemon = this$0.targetPokemon.getBattlePokemon();
            if (battlePokemon != null) {
                battlePokemon.setGone(true);
            }
            this$0.battle.dispatchWaiting(2.0f, (Function0<Unit>)((Function0)() -> BattleCaptureAction.attach$lambda$3$0(this$0)));
            String[] stringArray = new String[]{">capture " + this$0.targetPokemon.getPNX()};
            this$0.battle.writeShowdownAction(stringArray);
        } else {
            this$0.battle.dispatchWaiting(2.0f, (Function0<Unit>)((Function0)() -> BattleCaptureAction.attach$lambda$3$1(this$0)));
        }
        PokemonBattle pokemonBattle = this$0.battle;
        String string = this$0.targetPokemon.getPNX();
        Intrinsics.checkNotNull((Object)successful);
        pokemonBattle.sendUpdate(new BattleCaptureEndPacket(string, successful));
        this$0.battle.finishCaptureAction(this$0);
        return Unit.INSTANCE;
    }

    private static final Unit attach$lambda$3$0(BattleCaptureAction this$0) {
        PokemonBattle pokemonBattle = this$0.battle;
        Object[] objectArray = new Object[]{this$0.pokemonName};
        MutableComponent mutableComponent = LocalizationUtilsKt.lang("capture.succeeded", objectArray);
        Intrinsics.checkNotNullExpressionValue((Object)mutableComponent, (String)"lang(...)");
        pokemonBattle.broadcastChatMessage((Component)TextKt.green(mutableComponent));
        return Unit.INSTANCE;
    }

    private static final Unit attach$lambda$3$1(BattleCaptureAction this$0) {
        PokemonBattle pokemonBattle = this$0.battle;
        Object[] objectArray = new Object[]{this$0.pokemonName};
        MutableComponent mutableComponent = LocalizationUtilsKt.lang("capture.broke_free", objectArray);
        Intrinsics.checkNotNullExpressionValue((Object)mutableComponent, (String)"lang(...)");
        pokemonBattle.broadcastChatMessage((Component)TextKt.red(mutableComponent));
        return Unit.INSTANCE;
    }

    private static final void attach$lambda$4(Function1 $tmp0, Object p0) {
        $tmp0.invoke(p0);
    }
}
