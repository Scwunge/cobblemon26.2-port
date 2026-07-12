/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  kotlin.Metadata
 *  kotlin.jvm.internal.Intrinsics
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.Entity
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package com.cobblemon.mod.common.battles;

import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.AlreadyInBattleError;
import com.cobblemon.mod.common.battles.BusyError;
import com.cobblemon.mod.common.battles.CanceledError;
import com.cobblemon.mod.common.battles.IncorrectActorCountError;
import com.cobblemon.mod.common.battles.InsufficientPokemonError;
import com.cobblemon.mod.common.battles.NoPartyError;
import com.cobblemon.mod.common.entity.npc.NPCEntity;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.util.EntityExtensionsKt;
import java.util.UUID;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Metadata(mv={2, 2, 0}, k=1, xi=48, d1={"\u0000\u0016\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\bf\u0018\u0000 \u00072\u00020\u0001:\u0001\u0007J\u0017\u0010\u0005\u001a\u00020\u00042\u0006\u0010\u0003\u001a\u00020\u0002H&\u00a2\u0006\u0004\b\u0005\u0010\u0006\u00a8\u0006\b\u00c0\u0006\u0003"}, d2={"Lcom/cobblemon/mod/common/battles/BattleStartError;", "", "Lnet/minecraft/world/entity/Entity;", "entity", "Lnet/minecraft/network/chat/MutableComponent;", "getMessageFor", "(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/network/chat/MutableComponent;", "Companion", "common"})
public interface BattleStartError {
    @NotNull
    public static final Companion Companion = com.cobblemon.mod.common.battles.BattleStartError$Companion.$$INSTANCE;

    @NotNull
    public MutableComponent getMessageFor(@NotNull Entity var1);

    @Metadata(mv={2, 2, 0}, k=1, xi=48, d1={"\u0000r\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u0086\u0003\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003J\u0015\u0010\u0007\u001a\u00020\u00062\u0006\u0010\u0005\u001a\u00020\u0004\u00a2\u0006\u0004\b\u0007\u0010\bJ\u0015\u0010\u0007\u001a\u00020\u00062\u0006\u0010\n\u001a\u00020\t\u00a2\u0006\u0004\b\u0007\u0010\u000bJ\u0015\u0010\u0007\u001a\u00020\u00062\u0006\u0010\r\u001a\u00020\f\u00a2\u0006\u0004\b\u0007\u0010\u000eJ\u0015\u0010\u0012\u001a\u00020\u00112\u0006\u0010\u0010\u001a\u00020\u000f\u00a2\u0006\u0004\b\u0012\u0010\u0013J\u0015\u0010\u0017\u001a\u00020\u00162\u0006\u0010\u0015\u001a\u00020\u0014\u00a2\u0006\u0004\b\u0017\u0010\u0018J%\u0010\u001f\u001a\u00020\u001e2\u0006\u0010\u001a\u001a\u00020\u00192\u0006\u0010\u001c\u001a\u00020\u001b2\u0006\u0010\u001d\u001a\u00020\u001b\u00a2\u0006\u0004\b\u001f\u0010 J\u001d\u0010\"\u001a\u00020!2\u0006\u0010\u001c\u001a\u00020\u001b2\u0006\u0010\u001d\u001a\u00020\u001b\u00a2\u0006\u0004\b\"\u0010#J\u0017\u0010'\u001a\u00020&2\b\u0010%\u001a\u0004\u0018\u00010$\u00a2\u0006\u0004\b'\u0010(\u00a8\u0006)"}, d2={"Lcom/cobblemon/mod/common/battles/BattleStartError$Companion;", "", "<init>", "()V", "Lnet/minecraft/server/level/ServerPlayer;", "player", "Lcom/cobblemon/mod/common/battles/AlreadyInBattleError;", "alreadyInBattle", "(Lnet/minecraft/server/level/ServerPlayer;)Lcom/cobblemon/mod/common/battles/AlreadyInBattleError;", "Lcom/cobblemon/mod/common/entity/pokemon/PokemonEntity;", "pokemonEntity", "(Lcom/cobblemon/mod/common/entity/pokemon/PokemonEntity;)Lcom/cobblemon/mod/common/battles/AlreadyInBattleError;", "Lcom/cobblemon/mod/common/api/battles/model/actor/BattleActor;", "actor", "(Lcom/cobblemon/mod/common/api/battles/model/actor/BattleActor;)Lcom/cobblemon/mod/common/battles/AlreadyInBattleError;", "Lcom/cobblemon/mod/common/entity/npc/NPCEntity;", "npcEntity", "Lcom/cobblemon/mod/common/battles/NoPartyError;", "noParty", "(Lcom/cobblemon/mod/common/entity/npc/NPCEntity;)Lcom/cobblemon/mod/common/battles/NoPartyError;", "Lnet/minecraft/network/chat/Component;", "targetName", "Lcom/cobblemon/mod/common/battles/BusyError;", "targetIsBusy", "(Lnet/minecraft/network/chat/Component;)Lcom/cobblemon/mod/common/battles/BusyError;", "Lnet/minecraft/world/entity/Entity;", "actorEntity", "", "requiredCount", "hadCount", "Lcom/cobblemon/mod/common/battles/InsufficientPokemonError;", "insufficientPokemon", "(Lnet/minecraft/world/entity/Entity;II)Lcom/cobblemon/mod/common/battles/InsufficientPokemonError;", "Lcom/cobblemon/mod/common/battles/IncorrectActorCountError;", "incorrectActorCount", "(II)Lcom/cobblemon/mod/common/battles/IncorrectActorCountError;", "Lnet/minecraft/network/chat/MutableComponent;", "reason", "Lcom/cobblemon/mod/common/battles/CanceledError;", "canceledByEvent", "(Lnet/minecraft/network/chat/MutableComponent;)Lcom/cobblemon/mod/common/battles/CanceledError;", "common"})
    public static final class Companion {
        static final /* synthetic */ Companion $$INSTANCE;

        private Companion() {
        }

        @NotNull
        public final AlreadyInBattleError alreadyInBattle(@NotNull ServerPlayer player) {
            Intrinsics.checkNotNullParameter((Object)player, (String)"player");
            UUID uUID = player.getUUID();
            Intrinsics.checkNotNullExpressionValue((Object)uUID, (String)"getUUID(...)");
            Component component = EntityExtensionsKt.effectiveName((Entity)player);
            Intrinsics.checkNotNullExpressionValue((Object)component, (String)"effectiveName(...)");
            return new AlreadyInBattleError(uUID, component);
        }

        @NotNull
        public final AlreadyInBattleError alreadyInBattle(@NotNull PokemonEntity pokemonEntity) {
            Intrinsics.checkNotNullParameter((Object)pokemonEntity, (String)"pokemonEntity");
            UUID uUID = pokemonEntity.getUUID();
            Intrinsics.checkNotNullExpressionValue((Object)uUID, (String)"getUUID(...)");
            Component component = EntityExtensionsKt.effectiveName((Entity)pokemonEntity);
            Intrinsics.checkNotNullExpressionValue((Object)component, (String)"effectiveName(...)");
            return new AlreadyInBattleError(uUID, component);
        }

        @NotNull
        public final AlreadyInBattleError alreadyInBattle(@NotNull BattleActor actor) {
            Intrinsics.checkNotNullParameter((Object)actor, (String)"actor");
            return new AlreadyInBattleError(actor.getUuid(), (Component)actor.getName());
        }

        @NotNull
        public final NoPartyError noParty(@NotNull NPCEntity npcEntity) {
            Intrinsics.checkNotNullParameter((Object)npcEntity, (String)"npcEntity");
            return new NoPartyError(npcEntity);
        }

        @NotNull
        public final BusyError targetIsBusy(@NotNull Component targetName) {
            Intrinsics.checkNotNullParameter((Object)targetName, (String)"targetName");
            return new BusyError(targetName);
        }

        @NotNull
        public final InsufficientPokemonError insufficientPokemon(@NotNull Entity actorEntity, int requiredCount, int hadCount) {
            Intrinsics.checkNotNullParameter((Object)actorEntity, (String)"actorEntity");
            return new InsufficientPokemonError(actorEntity, requiredCount, hadCount);
        }

        @NotNull
        public final IncorrectActorCountError incorrectActorCount(int requiredCount, int hadCount) {
            return new IncorrectActorCountError(requiredCount, hadCount);
        }

        @NotNull
        public final CanceledError canceledByEvent(@Nullable MutableComponent reason) {
            return new CanceledError(reason);
        }

        static {
            $$INSTANCE = new Companion();
        }
    }
}
