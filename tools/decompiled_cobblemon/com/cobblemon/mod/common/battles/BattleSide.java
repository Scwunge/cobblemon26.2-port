/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  kotlin.Metadata
 *  kotlin.collections.CollectionsKt
 *  kotlin.jvm.internal.Intrinsics
 *  kotlin.jvm.internal.SourceDebugExtension
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceKey
 *  org.jetbrains.annotations.NotNull
 */
package com.cobblemon.mod.common.battles;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.net.NetworkPacket;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.interpreter.ContextManager;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.net.messages.client.effect.SpawnSnowstormEntityParticlePacket;
import com.cobblemon.mod.common.util.MiscUtilsKt;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import kotlin.Metadata;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.SourceDebugExtension;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={2, 2, 0}, k=1, xi=48, d1={"\u0000F\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\u0010\u0011\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0007\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0005\u0018\u00002\u00020\u0001B\u001b\u0012\u0012\u0010\u0004\u001a\n\u0012\u0006\b\u0001\u0012\u00020\u00030\u0002\"\u00020\u0003\u00a2\u0006\u0004\b\u0005\u0010\u0006J\r\u0010\u0007\u001a\u00020\u0000\u00a2\u0006\u0004\b\u0007\u0010\bJ\u0015\u0010\f\u001a\u00020\u000b2\u0006\u0010\n\u001a\u00020\t\u00a2\u0006\u0004\b\f\u0010\rJ\r\u0010\u000f\u001a\u00020\u000e\u00a2\u0006\u0004\b\u000f\u0010\u0010J\r\u0010\u0011\u001a\u00020\u000b\u00a2\u0006\u0004\b\u0011\u0010\u0012R\u001f\u0010\u0004\u001a\n\u0012\u0006\b\u0001\u0012\u00020\u00030\u00028\u0006\u00a2\u0006\f\n\u0004\b\u0004\u0010\u0013\u001a\u0004\b\u0014\u0010\u0015R\u0017\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\u00170\u00168F\u00a2\u0006\u0006\u001a\u0004\b\u0018\u0010\u0019R\"\u0010\u001c\u001a\u00020\u001b8\u0006@\u0006X\u0086.\u00a2\u0006\u0012\n\u0004\b\u001c\u0010\u001d\u001a\u0004\b\u001e\u0010\u001f\"\u0004\b \u0010!R\u0017\u0010#\u001a\u00020\"8\u0006\u00a2\u0006\f\n\u0004\b#\u0010$\u001a\u0004\b%\u0010&\u00a8\u0006'"}, d2={"Lcom/cobblemon/mod/common/battles/BattleSide;", "", "", "Lcom/cobblemon/mod/common/api/battles/model/actor/BattleActor;", "actors", "<init>", "([Lcom/cobblemon/mod/common/api/battles/model/actor/BattleActor;)V", "getOppositeSide", "()Lcom/cobblemon/mod/common/battles/BattleSide;", "Lnet/minecraft/network/chat/Component;", "component", "", "broadcastChatMessage", "(Lnet/minecraft/network/chat/Component;)V", "", "stillSendingOut", "()Z", "playCries", "()V", "[Lcom/cobblemon/mod/common/api/battles/model/actor/BattleActor;", "getActors", "()[Lcom/cobblemon/mod/common/api/battles/model/actor/BattleActor;", "", "Lcom/cobblemon/mod/common/battles/ActiveBattlePokemon;", "getActivePokemon", "()Ljava/util/List;", "activePokemon", "Lcom/cobblemon/mod/common/api/battles/model/PokemonBattle;", "battle", "Lcom/cobblemon/mod/common/api/battles/model/PokemonBattle;", "getBattle", "()Lcom/cobblemon/mod/common/api/battles/model/PokemonBattle;", "setBattle", "(Lcom/cobblemon/mod/common/api/battles/model/PokemonBattle;)V", "Lcom/cobblemon/mod/common/battles/interpreter/ContextManager;", "contextManager", "Lcom/cobblemon/mod/common/battles/interpreter/ContextManager;", "getContextManager", "()Lcom/cobblemon/mod/common/battles/interpreter/ContextManager;", "common"})
@SourceDebugExtension(value={"SMAP\nBattleSide.kt\nKotlin\n*S Kotlin\n*F\n+ 1 BattleSide.kt\ncom/cobblemon/mod/common/battles/BattleSide\n+ 2 _Arrays.kt\nkotlin/collections/ArraysKt___ArraysKt\n+ 3 _Collections.kt\nkotlin/collections/CollectionsKt___CollectionsKt\n*L\n1#1,49:1\n10468#2:50\n10890#2,5:51\n13805#2,2:56\n12970#2,2:58\n1869#3,2:60\n*S KotlinDebug\n*F\n+ 1 BattleSide.kt\ncom/cobblemon/mod/common/battles/BattleSide\n*L\n27#1:50\n27#1:51,5\n34#1:56,2\n37#1:58,2\n40#1:60,2\n*E\n"})
public final class BattleSide {
    @NotNull
    private final BattleActor[] actors;
    public PokemonBattle battle;
    @NotNull
    private final ContextManager contextManager;

    public BattleSide(BattleActor ... actors) {
        Intrinsics.checkNotNullParameter((Object)actors, (String)"actors");
        this.actors = actors;
        this.contextManager = new ContextManager();
    }

    @NotNull
    public final BattleActor[] getActors() {
        return this.actors;
    }

    /*
     * WARNING - void declaration
     */
    @NotNull
    public final List<ActiveBattlePokemon> getActivePokemon() {
        void $this$flatMapTo$iv$iv;
        BattleActor[] $this$flatMap$iv = this.actors;
        boolean $i$f$flatMap = false;
        BattleActor[] battleActorArray = $this$flatMap$iv;
        Collection destination$iv$iv = new ArrayList();
        boolean $i$f$flatMapTo = false;
        int n = ((void)$this$flatMapTo$iv$iv).length;
        for (int i = 0; i < n; ++i) {
            void element$iv$iv;
            void it = element$iv$iv = $this$flatMapTo$iv$iv[i];
            boolean bl = false;
            Iterable list$iv$iv = it.getActivePokemon();
            CollectionsKt.addAll((Collection)destination$iv$iv, (Iterable)list$iv$iv);
        }
        return (List)destination$iv$iv;
    }

    @NotNull
    public final PokemonBattle getBattle() {
        PokemonBattle pokemonBattle = this.battle;
        if (pokemonBattle != null) {
            return pokemonBattle;
        }
        Intrinsics.throwUninitializedPropertyAccessException((String)"battle");
        return null;
    }

    public final void setBattle(@NotNull PokemonBattle pokemonBattle) {
        Intrinsics.checkNotNullParameter((Object)pokemonBattle, (String)"<set-?>");
        this.battle = pokemonBattle;
    }

    @NotNull
    public final ContextManager getContextManager() {
        return this.contextManager;
    }

    @NotNull
    public final BattleSide getOppositeSide() {
        return Intrinsics.areEqual((Object)this, (Object)this.getBattle().getSide1()) ? this.getBattle().getSide2() : this.getBattle().getSide1();
    }

    public final void broadcastChatMessage(@NotNull Component component) {
        Intrinsics.checkNotNullParameter((Object)component, (String)"component");
        BattleActor[] $this$forEach$iv = this.actors;
        boolean $i$f$forEach = false;
        int n = $this$forEach$iv.length;
        for (int i = 0; i < n; ++i) {
            BattleActor element$iv;
            BattleActor it = element$iv = $this$forEach$iv[i];
            boolean bl = false;
            it.sendMessage(component);
        }
    }

    public final boolean stillSendingOut() {
        boolean bl;
        block1: {
            BattleActor[] $this$any$iv = this.actors;
            boolean $i$f$any = false;
            int n = $this$any$iv.length;
            for (int i = 0; i < n; ++i) {
                BattleActor element$iv;
                BattleActor it = element$iv = $this$any$iv[i];
                boolean bl2 = false;
                if (!(it.getStillSendingOutCount() > 0)) continue;
                bl = true;
                break block1;
            }
            bl = false;
        }
        return bl;
    }

    public final void playCries() {
        Iterable $this$forEach$iv = this.getActivePokemon();
        boolean $i$f$forEach = false;
        for (Object element$iv : $this$forEach$iv) {
            ActiveBattlePokemon it = (ActiveBattlePokemon)element$iv;
            boolean bl = false;
            Object object = it.getBattlePokemon();
            if (object == null || (object = ((BattlePokemon)object).getEntity()) == null) continue;
            Object entity = object;
            ((PokemonEntity)entity).cry();
            if (!((PokemonEntity)entity).getPokemon().getShiny() || !((PokemonEntity)entity).getPokemon().isWild()) continue;
            Object[] objectArray = new String[]{"shiny_particles", "middle"};
            NetworkPacket networkPacket = new SpawnSnowstormEntityParticlePacket(MiscUtilsKt.cobblemonResource("shiny_ring"), entity.getId(), CollectionsKt.listOf((Object[])objectArray), null, null, 24, null);
            double d = entity.getX();
            double d2 = entity.getY();
            double d3 = entity.getZ();
            ResourceKey resourceKey = entity.level().dimension();
            Intrinsics.checkNotNullExpressionValue((Object)resourceKey, (String)"dimension(...)");
            NetworkPacket.sendToPlayersAround$default(networkPacket, d, d2, d3, 64.0, resourceKey, null, 32, null);
        }
    }
}
