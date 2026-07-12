/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  kotlin.Metadata
 *  kotlin.Pair
 *  kotlin.TuplesKt
 *  kotlin.Unit
 *  kotlin.collections.CollectionsKt
 *  kotlin.collections.SetsKt
 *  kotlin.comparisons.ComparisonsKt
 *  kotlin.jvm.JvmOverloads
 *  kotlin.jvm.functions.Function1
 *  kotlin.jvm.internal.Intrinsics
 *  kotlin.jvm.internal.SourceDebugExtension
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.syncher.EntityDataAccessor
 *  net.minecraft.network.syncher.SynchedEntityData
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package com.cobblemon.mod.common.battles;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.storage.party.NPCPartyStore;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.battles.BattleFormat;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.battles.BattleStartError;
import com.cobblemon.mod.common.battles.BattleStartResult;
import com.cobblemon.mod.common.battles.ErroredBattleStart;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.actor.PokemonBattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.entity.npc.NPCBattleActor;
import com.cobblemon.mod.common.entity.npc.NPCEntity;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.util.CollectionUtilsKt;
import com.cobblemon.mod.common.util.EntityExtensionsKt;
import com.cobblemon.mod.common.util.PlayerExtensionsKt;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import kotlin.Metadata;
import kotlin.Pair;
import kotlin.TuplesKt;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.collections.SetsKt;
import kotlin.comparisons.ComparisonsKt;
import kotlin.jvm.JvmOverloads;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.SourceDebugExtension;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Metadata(mv={2, 2, 0}, k=1, xi=48, d1={"\u0000Z\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0007\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0004\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003Jk\u0010\u0013\u001a\u00020\u00122\u0006\u0010\u0005\u001a\u00020\u00042\u0006\u0010\u0006\u001a\u00020\u00042\n\b\u0002\u0010\b\u001a\u0004\u0018\u00010\u00072\n\b\u0002\u0010\t\u001a\u0004\u0018\u00010\u00072\b\b\u0002\u0010\u000b\u001a\u00020\n2\b\b\u0002\u0010\r\u001a\u00020\f2\b\b\u0002\u0010\u000e\u001a\u00020\f2\u0014\b\u0002\u0010\u0011\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00100\u000fH\u0007\u00a2\u0006\u0004\b\u0013\u0010\u0014Jc\u0010\u0018\u001a\u00020\u00122\u000e\b\u0002\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u00040\u00152\u000e\b\u0002\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\u00070\u00152\b\b\u0002\u0010\u000b\u001a\u00020\n2\b\b\u0002\u0010\r\u001a\u00020\f2\b\b\u0002\u0010\u000e\u001a\u00020\f2\u0014\b\u0002\u0010\u0011\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00100\u000fH\u0007\u00a2\u0006\u0004\b\u0018\u0010\u0019J]\u0010 \u001a\u00020\u00122\u0006\u0010\u001a\u001a\u00020\u00042\u0006\u0010\u001c\u001a\u00020\u001b2\n\b\u0002\u0010\u0017\u001a\u0004\u0018\u00010\u00072\b\b\u0002\u0010\u000b\u001a\u00020\n2\b\b\u0002\u0010\r\u001a\u00020\f2\b\b\u0002\u0010\u000e\u001a\u00020\f2\b\b\u0002\u0010\u001e\u001a\u00020\u001d2\b\b\u0002\u0010\u001f\u001a\u00020\u0010H\u0007\u00a2\u0006\u0004\b \u0010!JS\u0010$\u001a\u00020\u00122\u0006\u0010\u001a\u001a\u00020\u00042\u0006\u0010#\u001a\u00020\"2\n\b\u0002\u0010\u0017\u001a\u0004\u0018\u00010\u00072\b\b\u0002\u0010\u000b\u001a\u00020\n2\b\b\u0002\u0010\r\u001a\u00020\f2\b\b\u0002\u0010\u000e\u001a\u00020\f2\b\b\u0002\u0010\u001f\u001a\u00020\u0010H\u0007\u00a2\u0006\u0004\b$\u0010%\u00a8\u0006&"}, d2={"Lcom/cobblemon/mod/common/battles/BattleBuilder;", "", "<init>", "()V", "Lnet/minecraft/server/level/ServerPlayer;", "player1", "player2", "Ljava/util/UUID;", "leadingPokemonPlayer1", "leadingPokemonPlayer2", "Lcom/cobblemon/mod/common/battles/BattleFormat;", "battleFormat", "", "cloneParties", "healFirst", "Lkotlin/Function1;", "Lcom/cobblemon/mod/common/api/storage/party/PartyStore;", "partyAccessor", "Lcom/cobblemon/mod/common/battles/BattleStartResult;", "pvp1v1", "(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/level/ServerPlayer;Ljava/util/UUID;Ljava/util/UUID;Lcom/cobblemon/mod/common/battles/BattleFormat;ZZLkotlin/jvm/functions/Function1;)Lcom/cobblemon/mod/common/battles/BattleStartResult;", "", "players", "leadingPokemon", "pvp2v2", "(Ljava/util/List;Ljava/util/List;Lcom/cobblemon/mod/common/battles/BattleFormat;ZZLkotlin/jvm/functions/Function1;)Lcom/cobblemon/mod/common/battles/BattleStartResult;", "player", "Lcom/cobblemon/mod/common/entity/pokemon/PokemonEntity;", "pokemonEntity", "", "fleeDistance", "party", "pve", "(Lnet/minecraft/server/level/ServerPlayer;Lcom/cobblemon/mod/common/entity/pokemon/PokemonEntity;Ljava/util/UUID;Lcom/cobblemon/mod/common/battles/BattleFormat;ZZFLcom/cobblemon/mod/common/api/storage/party/PartyStore;)Lcom/cobblemon/mod/common/battles/BattleStartResult;", "Lcom/cobblemon/mod/common/entity/npc/NPCEntity;", "npcEntity", "pvn", "(Lnet/minecraft/server/level/ServerPlayer;Lcom/cobblemon/mod/common/entity/npc/NPCEntity;Ljava/util/UUID;Lcom/cobblemon/mod/common/battles/BattleFormat;ZZLcom/cobblemon/mod/common/api/storage/party/PartyStore;)Lcom/cobblemon/mod/common/battles/BattleStartResult;", "common"})
@SourceDebugExtension(value={"SMAP\nBattleBuilder.kt\nKotlin\n*S Kotlin\n*F\n+ 1 BattleBuilder.kt\ncom/cobblemon/mod/common/battles/BattleBuilder\n+ 2 _Collections.kt\nkotlin/collections/CollectionsKt___CollectionsKt\n*L\n1#1,561:1\n1878#2,3:562\n1878#2,3:565\n774#2:568\n865#2,2:569\n774#2:571\n865#2,2:572\n1761#2,3:574\n1573#2:577\n1604#2,3:578\n1056#2:581\n1607#2:582\n1573#2:583\n1604#2,4:584\n1878#2,2:588\n1878#2,3:590\n1880#2:593\n1869#2,2:594\n1761#2,3:596\n1563#2:599\n1634#2,3:600\n1803#2,3:603\n1803#2,3:606\n1869#2,2:609\n1869#2,2:611\n1056#2:613\n1761#2,3:614\n1878#2,3:617\n1878#2,3:620\n774#2:623\n865#2,2:624\n1761#2,3:626\n*S KotlinDebug\n*F\n+ 1 BattleBuilder.kt\ncom/cobblemon/mod/common/battles/BattleBuilder\n*L\n73#1:562,3\n81#1:565,3\n95#1:568\n95#1:569,2\n99#1:571\n99#1:572,2\n103#1:574,3\n137#1:577\n137#1:578,3\n142#1:581\n137#1:582\n144#1:583\n144#1:584,4\n149#1:588,2\n151#1:590,3\n149#1:593\n164#1:594,2\n181#1:596,3\n191#1:599\n191#1:600,3\n192#1:603,3\n193#1:606,3\n210#1:609,2\n211#1:611,2\n251#1:613\n272#1:614,3\n332#1:617,3\n340#1:620,3\n370#1:623\n370#1:624,2\n378#1:626,3\n*E\n"})
public final class BattleBuilder {
    @NotNull
    public static final BattleBuilder INSTANCE = new BattleBuilder();

    private BattleBuilder() {
    }

    /*
     * WARNING - void declaration
     */
    @JvmOverloads
    @NotNull
    public final BattleStartResult pvp1v1(@NotNull ServerPlayer player1, @NotNull ServerPlayer player2, @Nullable UUID leadingPokemonPlayer1, @Nullable UUID leadingPokemonPlayer2, @NotNull BattleFormat battleFormat, boolean cloneParties, boolean healFirst, @NotNull Function1<? super ServerPlayer, ? extends PartyStore> partyAccessor) {
        BattleStartResult battleStartResult;
        Intrinsics.checkNotNullParameter((Object)player1, (String)"player1");
        Intrinsics.checkNotNullParameter((Object)player2, (String)"player2");
        Intrinsics.checkNotNullParameter((Object)battleFormat, (String)"battleFormat");
        Intrinsics.checkNotNullParameter(partyAccessor, (String)"partyAccessor");
        int adjustLevel = battleFormat.getAdjustLevel();
        List<BattlePokemon> team1 = ((PartyStore)partyAccessor.invoke((Object)player1)).toBattleTeam(cloneParties || adjustLevel > 0, healFirst, leadingPokemonPlayer1);
        List<BattlePokemon> team2 = ((PartyStore)partyAccessor.invoke((Object)player2)).toBattleTeam(cloneParties || adjustLevel > 0, healFirst, leadingPokemonPlayer2);
        List battlePartyStores = new ArrayList();
        if (adjustLevel > 0) {
            UUID uUID = player1.getUUID();
            Intrinsics.checkNotNullExpressionValue((Object)uUID, (String)"getUUID(...)");
            PlayerPartyStore tempStoreP1 = new PlayerPartyStore(uUID);
            Iterable $this$forEachIndexed$iv = team1;
            boolean $i$f$forEachIndexed = false;
            boolean bl = false;
            for (Object item$iv : $this$forEachIndexed$iv) {
                void it2;
                void objectArray;
                void var19_29;
                if ((var19_29 = ++objectArray) < 0) {
                    CollectionsKt.throwIndexOverflow();
                }
                BattlePokemon battlePokemon = (BattlePokemon)item$iv;
                void index = var19_29;
                boolean bl2 = false;
                it2.getEffectedPokemon().setLevel(adjustLevel);
                it2.getEffectedPokemon().heal();
                tempStoreP1.set((int)index, it2.getEffectedPokemon());
            }
            battlePartyStores.add(tempStoreP1);
            UUID uUID2 = player2.getUUID();
            Intrinsics.checkNotNullExpressionValue((Object)uUID2, (String)"getUUID(...)");
            PlayerPartyStore tempStoreP2 = new PlayerPartyStore(uUID2);
            Iterable $this$forEachIndexed$iv2 = team2;
            boolean bl3 = false;
            int index$iv2 = 0;
            for (Object item$iv : $this$forEachIndexed$iv2) {
                void it;
                int it2;
                if ((it2 = index$iv2++) < 0) {
                    CollectionsKt.throwIndexOverflow();
                }
                BattlePokemon index = (BattlePokemon)item$iv;
                int index2 = it2;
                boolean bl4 = false;
                it.getEffectedPokemon().setLevel(adjustLevel);
                it.getEffectedPokemon().heal();
                tempStoreP2.set(index2, it.getEffectedPokemon());
            }
            battlePartyStores.add(tempStoreP2);
        }
        UUID uUID = player1.getUUID();
        Intrinsics.checkNotNullExpressionValue((Object)uUID, (String)"getUUID(...)");
        PlayerBattleActor player1Actor = new PlayerBattleActor(uUID, (List<? extends BattlePokemon>)team1);
        UUID uUID3 = player2.getUUID();
        Intrinsics.checkNotNullExpressionValue((Object)uUID3, (String)"getUUID(...)");
        PlayerBattleActor player2Actor = new PlayerBattleActor(uUID3, (List<? extends BattlePokemon>)team2);
        ErroredBattleStart errors = new ErroredBattleStart(null, null, 3, null);
        Pair[] pairArray = new Pair[]{TuplesKt.to((Object)player1, (Object)player1Actor), TuplesKt.to((Object)player2, (Object)player2Actor)};
        for (Pair pair : pairArray) {
            boolean bl;
            PlayerBattleActor actor;
            ServerPlayer player;
            block16: {
                void $this$filterTo$iv$iv;
                player = (ServerPlayer)pair.component1();
                actor = (PlayerBattleActor)pair.component2();
                Iterable $this$filter$iv = actor.getPokemonList();
                boolean $i$f$filter22 = false;
                Iterable iterable = $this$filter$iv;
                Collection destination$iv$iv = new ArrayList();
                boolean $i$f$filterTo = false;
                for (Object element$iv$iv : $this$filterTo$iv$iv) {
                    BattlePokemon it = (BattlePokemon)element$iv$iv;
                    boolean bl2 = false;
                    if (!(it.getHealth() > 0)) continue;
                    destination$iv$iv.add(element$iv$iv);
                }
                if (((List)destination$iv$iv).size() < battleFormat.getBattleType().getSlotsPerActor()) {
                    void $this$filterTo$iv$iv2;
                    void $this$filter$iv2;
                    $this$filter$iv = (Collection)errors.getParticipantErrors().get((Object)actor);
                    Iterable $i$f$filter22 = actor.getPokemonList();
                    int n = battleFormat.getBattleType().getSlotsPerActor();
                    Entity entity = (Entity)player;
                    BattleStartError.Companion companion = BattleStartError.Companion;
                    boolean $i$f$filter3 = false;
                    destination$iv$iv = $this$filter$iv2;
                    Collection destination$iv$iv2 = new ArrayList();
                    boolean $i$f$filterTo2 = false;
                    for (Object element$iv$iv : $this$filterTo$iv$iv2) {
                        BattlePokemon it = (BattlePokemon)element$iv$iv;
                        boolean bl3 = false;
                        if (!(it.getHealth() > 0)) continue;
                        destination$iv$iv2.add(element$iv$iv);
                    }
                    List list = (List)destination$iv$iv2;
                    $this$filter$iv.add(companion.insufficientPokemon(entity, n, list.size()));
                }
                Iterable $this$any$iv = actor.getPokemonList();
                boolean $i$f$any = false;
                if ($this$any$iv instanceof Collection && ((Collection)$this$any$iv).isEmpty()) {
                    bl = false;
                } else {
                    for (Object element$iv : $this$any$iv) {
                        BattlePokemon it = (BattlePokemon)element$iv;
                        boolean bl4 = false;
                        PokemonEntity pokemonEntity = it.getEntity();
                        boolean bl5 = pokemonEntity != null ? pokemonEntity.isBusy() : false;
                        if (!bl5) continue;
                        bl = true;
                        break block16;
                    }
                    bl = false;
                }
            }
            if (bl) {
                Collection collection = (Collection)errors.getParticipantErrors().get((Object)actor);
                Component component = player.getDisplayName();
                if (component == null) {
                    component = player.getName();
                }
                Component component2 = component;
                Intrinsics.checkNotNull((Object)component2);
                collection.add(BattleStartError.Companion.targetIsBusy(component2));
            }
            if (BattleRegistry.getBattleByParticipatingPlayer(player) == null) continue;
            ((Collection)errors.getParticipantErrors().get((Object)actor)).add(BattleStartError.Companion.alreadyInBattle(player));
        }
        player1Actor.setBattleTheme(PlayerExtensionsKt.getBattleTheme(player2));
        player2Actor.setBattleTheme(PlayerExtensionsKt.getBattleTheme(player1));
        if (errors.isEmpty()) {
            BattleActor[] battleActorArray2 = new BattleActor[]{player1Actor};
            battleActorArray2 = new BattleActor[]{player2Actor};
            battleStartResult = BattleRegistry.startBattle$default(battleFormat, new BattleSide(battleActorArray), new BattleSide(battleActorArray2), false, 8, null).ifSuccessful((Function1<? super PokemonBattle, Unit>)((Function1)arg_0 -> BattleBuilder.pvp1v1$lambda$6(battlePartyStores, arg_0)));
        } else {
            battleStartResult = errors;
        }
        return battleStartResult;
    }

    public static /* synthetic */ BattleStartResult pvp1v1$default(BattleBuilder battleBuilder, ServerPlayer serverPlayer, ServerPlayer serverPlayer2, UUID uUID, UUID uUID2, BattleFormat battleFormat, boolean bl, boolean bl2, Function1 function1, int n, Object object) {
        if ((n & 4) != 0) {
            uUID = null;
        }
        if ((n & 8) != 0) {
            uUID2 = null;
        }
        if ((n & 0x10) != 0) {
            battleFormat = BattleFormat.Companion.getGEN_9_SINGLES();
        }
        if ((n & 0x20) != 0) {
            bl = false;
        }
        if ((n & 0x40) != 0) {
            bl2 = false;
        }
        if ((n & 0x80) != 0) {
            function1 = BattleBuilder::pvp1v1$lambda$0;
        }
        return battleBuilder.pvp1v1(serverPlayer, serverPlayer2, uUID, uUID2, battleFormat, bl, bl2, (Function1<? super ServerPlayer, ? extends PartyStore>)function1);
    }

    /*
     * WARNING - void declaration
     */
    @JvmOverloads
    @NotNull
    public final BattleStartResult pvp2v2(@NotNull List<? extends ServerPlayer> players, @NotNull List<UUID> leadingPokemon, @NotNull BattleFormat battleFormat, boolean cloneParties, boolean healFirst, @NotNull Function1<? super ServerPlayer, ? extends PartyStore> partyAccessor) {
        BattleStartResult battleStartResult;
        void $this$fold$iv;
        void $this$fold$iv2;
        void $this$mapTo$iv$iv;
        void $this$mapIndexedTo$iv$iv;
        int n;
        Collection collection;
        void $this$mapIndexedTo$iv$iv2;
        Intrinsics.checkNotNullParameter(players, (String)"players");
        Intrinsics.checkNotNullParameter(leadingPokemon, (String)"leadingPokemon");
        Intrinsics.checkNotNullParameter((Object)battleFormat, (String)"battleFormat");
        Intrinsics.checkNotNullParameter(partyAccessor, (String)"partyAccessor");
        int adjustLevel = battleFormat.getAdjustLevel();
        Iterable $this$mapIndexed$iv = players;
        boolean $i$f$mapIndexed = false;
        Iterable iterable = $this$mapIndexed$iv;
        Iterable destination$iv$iv = new ArrayList(CollectionsKt.collectionSizeOrDefault((Iterable)$this$mapIndexed$iv, (int)10));
        boolean $i$f$mapIndexedTo = false;
        int index$iv$iv = 0;
        for (Object item$iv$iv : $this$mapIndexedTo$iv$iv2) {
            void $this$sortedBy$iv;
            void index2;
            void it;
            int n2;
            if ((n2 = index$iv$iv++) < 0) {
                CollectionsKt.throwIndexOverflow();
            }
            ServerPlayer serverPlayer = (ServerPlayer)item$iv$iv;
            int n3 = n2;
            collection = destination$iv$iv;
            n = 0;
            Iterable iterable2 = ((PartyStore)partyAccessor.invoke((Object)it)).toBattleTeam(cloneParties || adjustLevel > 0, healFirst, leadingPokemon.get((int)index2));
            boolean $i$f$sortedBy = false;
            collection.add(CollectionsKt.sortedWith((Iterable)$this$sortedBy$iv, (Comparator)new Comparator(){

                public final int compare(T a, T b) {
                    BattlePokemon it = (BattlePokemon)a;
                    boolean bl = false;
                    boolean bl2 = it.getHealth() <= 0;
                    it = (BattlePokemon)b;
                    Comparable comparable = Boolean.valueOf(bl2);
                    bl = false;
                    return ComparisonsKt.compareValues((Comparable)comparable, (Comparable)Boolean.valueOf(it.getHealth() <= 0));
                }
            }));
        }
        List teams = (List)destination$iv$iv;
        Iterable $this$mapIndexed$iv2 = teams;
        boolean $i$f$mapIndexed2 = false;
        destination$iv$iv = $this$mapIndexed$iv2;
        Collection destination$iv$iv2 = new ArrayList(CollectionsKt.collectionSizeOrDefault((Iterable)$this$mapIndexed$iv2, (int)10));
        boolean $i$f$mapIndexedTo2 = false;
        int index$iv$iv2 = 0;
        for (Object item$iv$iv2 : $this$mapIndexedTo$iv$iv) {
            void team;
            void index;
            int it;
            if ((it = index$iv$iv2++) < 0) {
                CollectionsKt.throwIndexOverflow();
            }
            List index2 = (List)item$iv$iv2;
            n = it;
            collection = destination$iv$iv2;
            boolean bl = false;
            UUID uUID = players.get((int)index).getUUID();
            Intrinsics.checkNotNullExpressionValue((Object)uUID, (String)"getUUID(...)");
            collection.add(new PlayerBattleActor(uUID, (List<? extends BattlePokemon>)team));
        }
        List playerActors = CollectionsKt.toMutableList((Collection)((List)destination$iv$iv2));
        List battlePartyStores = new ArrayList();
        if (adjustLevel > 0) {
            Iterable $this$forEachIndexed$iv = teams;
            boolean $i$f$forEachIndexed = false;
            int index$iv = 0;
            for (Object item$iv : $this$forEachIndexed$iv) {
                void battleTeam;
                int item$iv$iv;
                if ((item$iv$iv = index$iv++) < 0) {
                    CollectionsKt.throwIndexOverflow();
                }
                List item$iv$iv2 = (List)item$iv;
                int playerIndex = item$iv$iv;
                boolean bl = false;
                UUID uUID = players.get(playerIndex).getUUID();
                Intrinsics.checkNotNullExpressionValue((Object)uUID, (String)"getUUID(...)");
                PlayerPartyStore tempStore = new PlayerPartyStore(uUID);
                Iterable $this$forEachIndexed$iv2 = (Iterable)battleTeam;
                boolean $i$f$forEachIndexed2 = false;
                int index$iv2 = 0;
                for (Object item$iv2 : $this$forEachIndexed$iv2) {
                    void battlePokemon;
                    int n4;
                    if ((n4 = index$iv2++) < 0) {
                        CollectionsKt.throwIndexOverflow();
                    }
                    BattlePokemon battlePokemon2 = (BattlePokemon)item$iv2;
                    int pokemonIndex = n4;
                    boolean bl2 = false;
                    battlePokemon.getEffectedPokemon().setLevel(adjustLevel);
                    battlePokemon.getEffectedPokemon().heal();
                    tempStore.set(pokemonIndex, battlePokemon.getEffectedPokemon());
                }
                battlePartyStores.add(tempStore);
            }
        }
        ErroredBattleStart errors = new ErroredBattleStart(null, null, 3, null);
        if (players.size() != 4) {
            Iterable $this$forEach$iv = playerActors;
            boolean $i$f$forEach = false;
            for (Object element$iv : $this$forEach$iv) {
                PlayerBattleActor actor = (PlayerBattleActor)element$iv;
                boolean bl = false;
                ((Collection)errors.getParticipantErrors().get((Object)actor)).add(BattleStartError.Companion.incorrectActorCount(4, players.size()));
            }
        }
        for (Pair $i$f$forEach : CollectionsKt.zip((Iterable)players, (Iterable)playerActors)) {
            boolean bl;
            Iterable $this$any$iv;
            PlayerBattleActor actor;
            ServerPlayer player;
            block27: {
                player = (ServerPlayer)$i$f$forEach.component1();
                actor = (PlayerBattleActor)$i$f$forEach.component2();
                if (actor.getPokemonList().size() < battleFormat.getBattleType().getSlotsPerActor()) {
                    ((Collection)errors.getParticipantErrors().get((Object)actor)).add(BattleStartError.Companion.insufficientPokemon((Entity)player, battleFormat.getBattleType().getSlotsPerActor(), actor.getPokemonList().size()));
                }
                $this$any$iv = actor.getPokemonList();
                boolean $i$f$any = false;
                if ($this$any$iv instanceof Collection && ((Collection)$this$any$iv).isEmpty()) {
                    bl = false;
                } else {
                    for (Object element$iv : $this$any$iv) {
                        BattlePokemon it = (BattlePokemon)element$iv;
                        boolean bl3 = false;
                        PokemonEntity pokemonEntity = it.getEntity();
                        boolean bl4 = pokemonEntity != null ? pokemonEntity.isBusy() : false;
                        if (!bl4) continue;
                        bl = true;
                        break block27;
                    }
                    bl = false;
                }
            }
            if (bl) {
                $this$any$iv = (Collection)errors.getParticipantErrors().get((Object)actor);
                Component component = player.getDisplayName();
                if (component == null) {
                    component = player.getName();
                }
                Component $i$f$any = component;
                Intrinsics.checkNotNull((Object)$i$f$any);
                $this$any$iv.add(BattleStartError.Companion.targetIsBusy($i$f$any));
            }
            if (BattleRegistry.getBattleByParticipatingPlayer(player) == null) continue;
            ((Collection)errors.getParticipantErrors().get((Object)actor)).add(BattleStartError.Companion.alreadyInBattle(player));
        }
        Iterable $this$map$iv = players;
        boolean $i$f$map22 = false;
        Iterable actor = $this$map$iv;
        Collection destination$iv$iv3 = new ArrayList(CollectionsKt.collectionSizeOrDefault((Iterable)$this$map$iv, (int)10));
        boolean $i$f$mapTo = false;
        for (Object item$iv$iv : $this$mapTo$iv$iv) {
            void p;
            ServerPlayer it = (ServerPlayer)item$iv$iv;
            collection = destination$iv$iv3;
            boolean bl = false;
            collection.add(p.position());
        }
        List playersPositions = (List)destination$iv$iv3;
        Iterable $i$f$map22 = playersPositions.subList(0, players.size() / 2);
        Object initial$iv = new Vec3(0.0, 0.0, 0.0);
        boolean $i$f$fold = false;
        Vec3 accumulator$iv = initial$iv;
        for (Object element$iv : $this$fold$iv2) {
            void vec3;
            Vec3 p = (Vec3)element$iv;
            Vec3 acc = accumulator$iv;
            boolean bl = false;
            Intrinsics.checkNotNullExpressionValue((Object)acc.add(vec3.scale(1.0 / (double)(players.size() / 2))), (String)"add(...)");
        }
        Vec3 side1Center = accumulator$iv;
        initial$iv = playersPositions.subList(players.size() / 2, players.size());
        Object[] initial$iv2 = new Vec3(0.0, 0.0, 0.0);
        boolean $i$f$fold22 = false;
        Object[] accumulator$iv2 = initial$iv2;
        for (Object element$iv : $this$fold$iv) {
            void vec3;
            Vec3 acc = (Vec3)element$iv;
            Object[] acc2 = accumulator$iv2;
            boolean bl = false;
            Intrinsics.checkNotNullExpressionValue((Object)acc2.add(vec3.scale(1.0 / (double)(players.size() / 2))), (String)"add(...)");
        }
        Object[] side2Center = accumulator$iv2;
        if ((side2Center.x - side1Center.x) * (((Vec3)playersPositions.get((int)1)).z - side1Center.z) - (side2Center.z - side1Center.z) * (((Vec3)playersPositions.get((int)1)).x - side1Center.x) < 0.0) {
            CollectionUtilsKt.swap(playerActors, 0, 1);
        }
        if ((side1Center.x - side2Center.x) * (((Vec3)playersPositions.get((int)3)).z - side2Center.z) - (side1Center.z - side2Center.z) * (((Vec3)playersPositions.get((int)3)).x - side2Center.x) < 0.0) {
            CollectionUtilsKt.swap(playerActors, 2, 3);
        }
        initial$iv2 = new PlayerBattleActor[]{playerActors.get(0), playerActors.get(1)};
        List side1Actors = CollectionsKt.listOf((Object[])initial$iv2);
        Object[] $i$f$fold22 = new PlayerBattleActor[]{playerActors.get(2), playerActors.get(3)};
        List side2Actors = CollectionsKt.listOf((Object[])$i$f$fold22);
        ResourceLocation side1Theme = PlayerExtensionsKt.getBattleTheme(players.get(0));
        ResourceLocation side2Theme = PlayerExtensionsKt.getBattleTheme(players.get(2));
        Iterable $this$forEach$iv = side1Actors;
        boolean $i$f$forEach = false;
        for (Object element$iv : $this$forEach$iv) {
            PlayerBattleActor it = (PlayerBattleActor)element$iv;
            boolean bl = false;
            it.setBattleTheme(side2Theme);
        }
        $this$forEach$iv = side2Actors;
        $i$f$forEach = false;
        for (Object element$iv : $this$forEach$iv) {
            PlayerBattleActor it = (PlayerBattleActor)element$iv;
            boolean bl = false;
            it.setBattleTheme(side1Theme);
        }
        if (errors.isEmpty()) {
            BattleActor[] battleActorArray = new BattleActor[]{playerActors.get(0), playerActors.get(1)};
            BattleSide battleSide = new BattleSide(battleActorArray);
            battleActorArray = new BattleActor[]{playerActors.get(2), playerActors.get(3)};
            battleStartResult = BattleRegistry.startBattle$default(battleFormat, battleSide, new BattleSide(battleActorArray), false, 8, null).ifSuccessful((Function1<? super PokemonBattle, Unit>)((Function1)arg_0 -> BattleBuilder.pvp2v2$lambda$11(battlePartyStores, arg_0)));
        } else {
            battleStartResult = errors;
        }
        return battleStartResult;
    }

    public static /* synthetic */ BattleStartResult pvp2v2$default(BattleBuilder battleBuilder, List list, List list2, BattleFormat battleFormat, boolean bl, boolean bl2, Function1 function1, int n, Object object) {
        if ((n & 1) != 0) {
            list = CollectionsKt.emptyList();
        }
        if ((n & 2) != 0) {
            list2 = CollectionsKt.emptyList();
        }
        if ((n & 4) != 0) {
            battleFormat = BattleFormat.Companion.getGEN_9_MULTI();
        }
        if ((n & 8) != 0) {
            bl = false;
        }
        if ((n & 0x10) != 0) {
            bl2 = false;
        }
        if ((n & 0x20) != 0) {
            function1 = BattleBuilder::pvp2v2$lambda$0;
        }
        return battleBuilder.pvp2v2(list, list2, battleFormat, bl, bl2, (Function1<? super ServerPlayer, ? extends PartyStore>)function1);
    }

    @JvmOverloads
    @NotNull
    public final BattleStartResult pve(@NotNull ServerPlayer player, @NotNull PokemonEntity pokemonEntity, @Nullable UUID leadingPokemon, @NotNull BattleFormat battleFormat, boolean cloneParties, boolean healFirst, float fleeDistance, @NotNull PartyStore party) {
        BattleStartResult battleStartResult;
        BattleActor[] battleActorArray;
        boolean bl;
        ErroredBattleStart errors;
        PokemonBattleActor wildActor;
        PlayerBattleActor playerActor;
        block11: {
            Intrinsics.checkNotNullParameter((Object)player, (String)"player");
            Intrinsics.checkNotNullParameter((Object)pokemonEntity, (String)"pokemonEntity");
            Intrinsics.checkNotNullParameter((Object)battleFormat, (String)"battleFormat");
            Intrinsics.checkNotNullParameter((Object)party, (String)"party");
            Iterable $this$sortedBy$iv = party.toBattleTeam(cloneParties, healFirst, leadingPokemon);
            boolean $i$f$sortedBy = false;
            List playerTeam = CollectionsKt.sortedWith((Iterable)$this$sortedBy$iv, (Comparator)new Comparator(){

                public final int compare(T a, T b) {
                    BattlePokemon it = (BattlePokemon)a;
                    boolean bl = false;
                    boolean bl2 = it.getHealth() <= 0;
                    it = (BattlePokemon)b;
                    Comparable comparable = Boolean.valueOf(bl2);
                    bl = false;
                    return ComparisonsKt.compareValues((Comparable)comparable, (Comparable)Boolean.valueOf(it.getHealth() <= 0));
                }
            });
            UUID uUID = player.getUUID();
            Intrinsics.checkNotNullExpressionValue((Object)uUID, (String)"getUUID(...)");
            playerActor = new PlayerBattleActor(uUID, playerTeam);
            UUID uUID2 = pokemonEntity.getPokemon().getUuid();
            Intrinsics.checkNotNullExpressionValue((Object)uUID2, (String)"<get-uuid>(...)");
            wildActor = new PokemonBattleActor(uUID2, new BattlePokemon(pokemonEntity.getPokemon(), null, null, null, 14, null), fleeDistance, null, 8, null);
            errors = new ErroredBattleStart(null, null, 3, null);
            if (!((Collection)playerTeam).isEmpty() && ((BattlePokemon)playerTeam.get(0)).getHealth() <= 0) {
                ((Collection)errors.getParticipantErrors().get((Object)playerActor)).add(BattleStartError.Companion.insufficientPokemon((Entity)player, battleFormat.getBattleType().getSlotsPerActor(), playerActor.getPokemonList().size()));
            }
            if (playerActor.getPokemonList().size() < battleFormat.getBattleType().getSlotsPerActor()) {
                ((Collection)errors.getParticipantErrors().get((Object)playerActor)).add(BattleStartError.Companion.insufficientPokemon((Entity)player, battleFormat.getBattleType().getSlotsPerActor(), playerActor.getPokemonList().size()));
            }
            Iterable $this$any$iv = playerActor.getPokemonList();
            boolean $i$f$any = false;
            if ($this$any$iv instanceof Collection && ((Collection)$this$any$iv).isEmpty()) {
                bl = false;
            } else {
                for (Object element$iv : $this$any$iv) {
                    BattlePokemon it = (BattlePokemon)element$iv;
                    boolean bl2 = false;
                    PokemonEntity pokemonEntity2 = it.getEntity();
                    boolean bl3 = pokemonEntity2 != null ? pokemonEntity2.isBusy() : false;
                    if (!bl3) continue;
                    bl = true;
                    break block11;
                }
                bl = false;
            }
        }
        if (bl) {
            battleActorArray = (BattleActor[])errors.getParticipantErrors().get((Object)playerActor);
            Component component = player.getDisplayName();
            if (component == null) {
                component = player.getName();
            }
            Component component2 = component;
            Intrinsics.checkNotNull((Object)component2);
            battleActorArray.add(BattleStartError.Companion.targetIsBusy(component2));
        }
        if (BattleRegistry.getBattleByParticipatingPlayer(player) != null) {
            ((Collection)errors.getParticipantErrors().get((Object)playerActor)).add(BattleStartError.Companion.alreadyInBattle(playerActor));
        }
        if (pokemonEntity.getBattleId() != null) {
            ((Collection)errors.getParticipantErrors().get((Object)wildActor)).add(BattleStartError.Companion.alreadyInBattle(wildActor));
        }
        playerActor.setBattleTheme(pokemonEntity.getBattleTheme());
        if (errors.isEmpty()) {
            battleActorArray = new BattleActor[]{playerActor};
            BattleSide battleSide = new BattleSide(battleActorArray);
            battleActorArray = new BattleActor[]{wildActor};
            battleStartResult = BattleRegistry.startBattle$default(battleFormat, battleSide, new BattleSide(battleActorArray), false, 8, null).ifSuccessful((Function1<? super PokemonBattle, Unit>)((Function1)arg_0 -> BattleBuilder.pve$lambda$2(cloneParties, pokemonEntity, arg_0)));
        } else {
            battleStartResult = errors;
        }
        return battleStartResult;
    }

    public static /* synthetic */ BattleStartResult pve$default(BattleBuilder battleBuilder, ServerPlayer serverPlayer, PokemonEntity pokemonEntity, UUID uUID, BattleFormat battleFormat, boolean bl, boolean bl2, float f, PartyStore partyStore, int n, Object object) {
        if ((n & 4) != 0) {
            uUID = null;
        }
        if ((n & 8) != 0) {
            battleFormat = BattleFormat.Companion.getGEN_9_SINGLES();
        }
        if ((n & 0x10) != 0) {
            bl = false;
        }
        if ((n & 0x20) != 0) {
            bl2 = false;
        }
        if ((n & 0x40) != 0) {
            f = Cobblemon.INSTANCE.getConfig().getDefaultFleeDistance();
        }
        if ((n & 0x80) != 0) {
            partyStore = PlayerExtensionsKt.party(serverPlayer);
        }
        return battleBuilder.pve(serverPlayer, pokemonEntity, uUID, battleFormat, bl, bl2, f, partyStore);
    }

    /*
     * WARNING - void declaration
     */
    @JvmOverloads
    @NotNull
    public final BattleStartResult pvn(@NotNull ServerPlayer player, @NotNull NPCEntity npcEntity, @Nullable UUID leadingPokemon, @NotNull BattleFormat battleFormat, boolean cloneParties, boolean healFirst, @NotNull PartyStore party) {
        BattleStartResult battleStartResult;
        BattleActor[] battleActorArray;
        boolean bl;
        NPCBattleActor npcActor;
        ErroredBattleStart errors;
        PlayerBattleActor playerActor;
        block17: {
            void $this$filterTo$iv$iv;
            Intrinsics.checkNotNullParameter((Object)player, (String)"player");
            Intrinsics.checkNotNullParameter((Object)npcEntity, (String)"npcEntity");
            Intrinsics.checkNotNullParameter((Object)battleFormat, (String)"battleFormat");
            Intrinsics.checkNotNullParameter((Object)party, (String)"party");
            List<BattlePokemon> playerTeam = party.toBattleTeam(cloneParties, healFirst, leadingPokemon);
            UUID uUID = player.getUUID();
            Intrinsics.checkNotNullExpressionValue((Object)uUID, (String)"getUUID(...)");
            playerActor = new PlayerBattleActor(uUID, (List<? extends BattlePokemon>)playerTeam);
            NPCPartyStore npcParty = npcEntity.getPartyForChallenge(CollectionsKt.listOf((Object)player));
            errors = new ErroredBattleStart(null, null, 3, null);
            int adjustLevel = battleFormat.getAdjustLevel();
            List playerPartyStores = new ArrayList();
            List npcPartyStores = new ArrayList();
            if (adjustLevel > 0) {
                UUID uUID2 = player.getUUID();
                Intrinsics.checkNotNullExpressionValue((Object)uUID2, (String)"getUUID(...)");
                PlayerPartyStore tempStorePlayer = new PlayerPartyStore(uUID2);
                Iterable $this$forEachIndexed$iv = playerTeam;
                boolean $i$f$forEachIndexed = false;
                int index$iv = 0;
                for (Object item$iv : $this$forEachIndexed$iv) {
                    void battlePokemon3;
                    int n;
                    if ((n = index$iv++) < 0) {
                        CollectionsKt.throwIndexOverflow();
                    }
                    BattlePokemon battlePokemon2 = (BattlePokemon)item$iv;
                    int index = n;
                    boolean bl2 = false;
                    battlePokemon3.getEffectedPokemon().setLevel(adjustLevel);
                    battlePokemon3.getEffectedPokemon().heal();
                    tempStorePlayer.set(index, battlePokemon3.getEffectedPokemon());
                }
                playerPartyStores.add(tempStorePlayer);
                NPCPartyStore tempStoreNpc = new NPCPartyStore(npcEntity);
                NPCPartyStore nPCPartyStore = npcParty;
                Intrinsics.checkNotNull((Object)nPCPartyStore);
                Iterable $this$forEachIndexed$iv2 = nPCPartyStore;
                boolean $i$f$forEachIndexed2 = false;
                int index$iv2 = 0;
                for (Object item$iv : $this$forEachIndexed$iv2) {
                    void battlePokemon;
                    int battlePokemon3;
                    if ((battlePokemon3 = index$iv2++) < 0) {
                        CollectionsKt.throwIndexOverflow();
                    }
                    Pokemon index = (Pokemon)item$iv;
                    int index2 = battlePokemon3;
                    boolean bl3 = false;
                    battlePokemon.setLevel(adjustLevel);
                    battlePokemon.heal();
                    tempStoreNpc.set(index2, (Pokemon)battlePokemon);
                }
                npcPartyStores.add(npcParty);
            }
            if (playerActor.getPokemonList().size() < battleFormat.getBattleType().getSlotsPerActor()) {
                ((Collection)errors.getParticipantErrors().get((Object)playerActor)).add(BattleStartError.Companion.insufficientPokemon((Entity)player, battleFormat.getBattleType().getSlotsPerActor(), playerActor.getPokemonList().size()));
            }
            if (BattleRegistry.getBattleByParticipatingPlayer(player) != null) {
                ((Collection)errors.getParticipantErrors().get((Object)playerActor)).add(BattleStartError.Companion.alreadyInBattle(playerActor));
            }
            if (npcParty == null) {
                ((Collection)errors.getGeneralErrors()).add(BattleStartError.Companion.noParty(npcEntity));
                return errors;
            }
            Integer n = npcEntity.getSkill();
            npcActor = new NPCBattleActor(npcEntity, npcParty, n != null ? n.intValue() : npcEntity.getNpc().getSkill());
            Iterable $this$filter$iv = npcActor.getPokemonList();
            boolean $i$f$filter = false;
            Iterable $i$f$forEachIndexed2 = $this$filter$iv;
            Collection destination$iv$iv = new ArrayList();
            boolean $i$f$filterTo = false;
            for (Object element$iv$iv : $this$filterTo$iv$iv) {
                BattlePokemon it = (BattlePokemon)element$iv$iv;
                boolean bl4 = false;
                if (!(it.getHealth() > 0)) continue;
                destination$iv$iv.add(element$iv$iv);
            }
            if (((List)destination$iv$iv).size() < battleFormat.getBattleType().getSlotsPerActor()) {
                ((Collection)errors.getParticipantErrors().get((Object)npcActor)).add(BattleStartError.Companion.insufficientPokemon((Entity)npcEntity, battleFormat.getBattleType().getSlotsPerActor(), npcActor.getPokemonList().size()));
            }
            Iterable $this$any$iv = playerActor.getPokemonList();
            boolean $i$f$any = false;
            if ($this$any$iv instanceof Collection && ((Collection)$this$any$iv).isEmpty()) {
                bl = false;
            } else {
                for (Object element$iv : $this$any$iv) {
                    BattlePokemon it = (BattlePokemon)element$iv;
                    boolean bl5 = false;
                    PokemonEntity pokemonEntity = it.getEntity();
                    boolean bl6 = pokemonEntity != null ? pokemonEntity.isBusy() : false;
                    if (!bl6) continue;
                    bl = true;
                    break block17;
                }
                bl = false;
            }
        }
        if (bl) {
            battleActorArray = (BattleActor[])errors.getParticipantErrors().get((Object)playerActor);
            Component component = player.getDisplayName();
            if (component == null) {
                component = player.getName();
            }
            Component component2 = component;
            Intrinsics.checkNotNull((Object)component2);
            battleActorArray.add(BattleStartError.Companion.targetIsBusy(component2));
        }
        playerActor.setBattleTheme(npcEntity.getBattleTheme());
        if (errors.isEmpty()) {
            battleActorArray = new BattleActor[]{playerActor};
            BattleSide battleSide = new BattleSide(battleActorArray);
            battleActorArray = new BattleActor[]{npcActor};
            battleStartResult = BattleRegistry.startBattle$default(battleFormat, battleSide, new BattleSide(battleActorArray), false, 8, null).ifSuccessful((Function1<? super PokemonBattle, Unit>)((Function1)arg_0 -> BattleBuilder.pvn$lambda$4(npcEntity, arg_0)));
        } else {
            battleStartResult = errors;
        }
        return battleStartResult;
    }

    public static /* synthetic */ BattleStartResult pvn$default(BattleBuilder battleBuilder, ServerPlayer serverPlayer, NPCEntity nPCEntity, UUID uUID, BattleFormat battleFormat, boolean bl, boolean bl2, PartyStore partyStore, int n, Object object) {
        if ((n & 4) != 0) {
            uUID = null;
        }
        if ((n & 8) != 0) {
            battleFormat = BattleFormat.Companion.getGEN_9_SINGLES();
        }
        if ((n & 0x10) != 0) {
            bl = false;
        }
        if ((n & 0x20) != 0) {
            bl2 = false;
        }
        if ((n & 0x40) != 0) {
            partyStore = PlayerExtensionsKt.party(serverPlayer);
        }
        return battleBuilder.pvn(serverPlayer, nPCEntity, uUID, battleFormat, bl, bl2, partyStore);
    }

    @JvmOverloads
    @NotNull
    public final BattleStartResult pvp1v1(@NotNull ServerPlayer player1, @NotNull ServerPlayer player2, @Nullable UUID leadingPokemonPlayer1, @Nullable UUID leadingPokemonPlayer2, @NotNull BattleFormat battleFormat, boolean cloneParties, boolean healFirst) {
        Intrinsics.checkNotNullParameter((Object)player1, (String)"player1");
        Intrinsics.checkNotNullParameter((Object)player2, (String)"player2");
        Intrinsics.checkNotNullParameter((Object)battleFormat, (String)"battleFormat");
        return BattleBuilder.pvp1v1$default(this, player1, player2, leadingPokemonPlayer1, leadingPokemonPlayer2, battleFormat, cloneParties, healFirst, null, 128, null);
    }

    @JvmOverloads
    @NotNull
    public final BattleStartResult pvp1v1(@NotNull ServerPlayer player1, @NotNull ServerPlayer player2, @Nullable UUID leadingPokemonPlayer1, @Nullable UUID leadingPokemonPlayer2, @NotNull BattleFormat battleFormat, boolean cloneParties) {
        Intrinsics.checkNotNullParameter((Object)player1, (String)"player1");
        Intrinsics.checkNotNullParameter((Object)player2, (String)"player2");
        Intrinsics.checkNotNullParameter((Object)battleFormat, (String)"battleFormat");
        return BattleBuilder.pvp1v1$default(this, player1, player2, leadingPokemonPlayer1, leadingPokemonPlayer2, battleFormat, cloneParties, false, null, 192, null);
    }

    @JvmOverloads
    @NotNull
    public final BattleStartResult pvp1v1(@NotNull ServerPlayer player1, @NotNull ServerPlayer player2, @Nullable UUID leadingPokemonPlayer1, @Nullable UUID leadingPokemonPlayer2, @NotNull BattleFormat battleFormat) {
        Intrinsics.checkNotNullParameter((Object)player1, (String)"player1");
        Intrinsics.checkNotNullParameter((Object)player2, (String)"player2");
        Intrinsics.checkNotNullParameter((Object)battleFormat, (String)"battleFormat");
        return BattleBuilder.pvp1v1$default(this, player1, player2, leadingPokemonPlayer1, leadingPokemonPlayer2, battleFormat, false, false, null, 224, null);
    }

    @JvmOverloads
    @NotNull
    public final BattleStartResult pvp1v1(@NotNull ServerPlayer player1, @NotNull ServerPlayer player2, @Nullable UUID leadingPokemonPlayer1, @Nullable UUID leadingPokemonPlayer2) {
        Intrinsics.checkNotNullParameter((Object)player1, (String)"player1");
        Intrinsics.checkNotNullParameter((Object)player2, (String)"player2");
        return BattleBuilder.pvp1v1$default(this, player1, player2, leadingPokemonPlayer1, leadingPokemonPlayer2, null, false, false, null, 240, null);
    }

    @JvmOverloads
    @NotNull
    public final BattleStartResult pvp1v1(@NotNull ServerPlayer player1, @NotNull ServerPlayer player2, @Nullable UUID leadingPokemonPlayer1) {
        Intrinsics.checkNotNullParameter((Object)player1, (String)"player1");
        Intrinsics.checkNotNullParameter((Object)player2, (String)"player2");
        return BattleBuilder.pvp1v1$default(this, player1, player2, leadingPokemonPlayer1, null, null, false, false, null, 248, null);
    }

    @JvmOverloads
    @NotNull
    public final BattleStartResult pvp1v1(@NotNull ServerPlayer player1, @NotNull ServerPlayer player2) {
        Intrinsics.checkNotNullParameter((Object)player1, (String)"player1");
        Intrinsics.checkNotNullParameter((Object)player2, (String)"player2");
        return BattleBuilder.pvp1v1$default(this, player1, player2, null, null, null, false, false, null, 252, null);
    }

    @JvmOverloads
    @NotNull
    public final BattleStartResult pvp2v2(@NotNull List<? extends ServerPlayer> players, @NotNull List<UUID> leadingPokemon, @NotNull BattleFormat battleFormat, boolean cloneParties, boolean healFirst) {
        Intrinsics.checkNotNullParameter(players, (String)"players");
        Intrinsics.checkNotNullParameter(leadingPokemon, (String)"leadingPokemon");
        Intrinsics.checkNotNullParameter((Object)battleFormat, (String)"battleFormat");
        return BattleBuilder.pvp2v2$default(this, players, leadingPokemon, battleFormat, cloneParties, healFirst, null, 32, null);
    }

    @JvmOverloads
    @NotNull
    public final BattleStartResult pvp2v2(@NotNull List<? extends ServerPlayer> players, @NotNull List<UUID> leadingPokemon, @NotNull BattleFormat battleFormat, boolean cloneParties) {
        Intrinsics.checkNotNullParameter(players, (String)"players");
        Intrinsics.checkNotNullParameter(leadingPokemon, (String)"leadingPokemon");
        Intrinsics.checkNotNullParameter((Object)battleFormat, (String)"battleFormat");
        return BattleBuilder.pvp2v2$default(this, players, leadingPokemon, battleFormat, cloneParties, false, null, 48, null);
    }

    @JvmOverloads
    @NotNull
    public final BattleStartResult pvp2v2(@NotNull List<? extends ServerPlayer> players, @NotNull List<UUID> leadingPokemon, @NotNull BattleFormat battleFormat) {
        Intrinsics.checkNotNullParameter(players, (String)"players");
        Intrinsics.checkNotNullParameter(leadingPokemon, (String)"leadingPokemon");
        Intrinsics.checkNotNullParameter((Object)battleFormat, (String)"battleFormat");
        return BattleBuilder.pvp2v2$default(this, players, leadingPokemon, battleFormat, false, false, null, 56, null);
    }

    @JvmOverloads
    @NotNull
    public final BattleStartResult pvp2v2(@NotNull List<? extends ServerPlayer> players, @NotNull List<UUID> leadingPokemon) {
        Intrinsics.checkNotNullParameter(players, (String)"players");
        Intrinsics.checkNotNullParameter(leadingPokemon, (String)"leadingPokemon");
        return BattleBuilder.pvp2v2$default(this, players, leadingPokemon, null, false, false, null, 60, null);
    }

    @JvmOverloads
    @NotNull
    public final BattleStartResult pvp2v2(@NotNull List<? extends ServerPlayer> players) {
        Intrinsics.checkNotNullParameter(players, (String)"players");
        return BattleBuilder.pvp2v2$default(this, players, null, null, false, false, null, 62, null);
    }

    @JvmOverloads
    @NotNull
    public final BattleStartResult pvp2v2() {
        return BattleBuilder.pvp2v2$default(this, null, null, null, false, false, null, 63, null);
    }

    @JvmOverloads
    @NotNull
    public final BattleStartResult pve(@NotNull ServerPlayer player, @NotNull PokemonEntity pokemonEntity, @Nullable UUID leadingPokemon, @NotNull BattleFormat battleFormat, boolean cloneParties, boolean healFirst, float fleeDistance) {
        Intrinsics.checkNotNullParameter((Object)player, (String)"player");
        Intrinsics.checkNotNullParameter((Object)pokemonEntity, (String)"pokemonEntity");
        Intrinsics.checkNotNullParameter((Object)battleFormat, (String)"battleFormat");
        return BattleBuilder.pve$default(this, player, pokemonEntity, leadingPokemon, battleFormat, cloneParties, healFirst, fleeDistance, null, 128, null);
    }

    @JvmOverloads
    @NotNull
    public final BattleStartResult pve(@NotNull ServerPlayer player, @NotNull PokemonEntity pokemonEntity, @Nullable UUID leadingPokemon, @NotNull BattleFormat battleFormat, boolean cloneParties, boolean healFirst) {
        Intrinsics.checkNotNullParameter((Object)player, (String)"player");
        Intrinsics.checkNotNullParameter((Object)pokemonEntity, (String)"pokemonEntity");
        Intrinsics.checkNotNullParameter((Object)battleFormat, (String)"battleFormat");
        return BattleBuilder.pve$default(this, player, pokemonEntity, leadingPokemon, battleFormat, cloneParties, healFirst, 0.0f, null, 192, null);
    }

    @JvmOverloads
    @NotNull
    public final BattleStartResult pve(@NotNull ServerPlayer player, @NotNull PokemonEntity pokemonEntity, @Nullable UUID leadingPokemon, @NotNull BattleFormat battleFormat, boolean cloneParties) {
        Intrinsics.checkNotNullParameter((Object)player, (String)"player");
        Intrinsics.checkNotNullParameter((Object)pokemonEntity, (String)"pokemonEntity");
        Intrinsics.checkNotNullParameter((Object)battleFormat, (String)"battleFormat");
        return BattleBuilder.pve$default(this, player, pokemonEntity, leadingPokemon, battleFormat, cloneParties, false, 0.0f, null, 224, null);
    }

    @JvmOverloads
    @NotNull
    public final BattleStartResult pve(@NotNull ServerPlayer player, @NotNull PokemonEntity pokemonEntity, @Nullable UUID leadingPokemon, @NotNull BattleFormat battleFormat) {
        Intrinsics.checkNotNullParameter((Object)player, (String)"player");
        Intrinsics.checkNotNullParameter((Object)pokemonEntity, (String)"pokemonEntity");
        Intrinsics.checkNotNullParameter((Object)battleFormat, (String)"battleFormat");
        return BattleBuilder.pve$default(this, player, pokemonEntity, leadingPokemon, battleFormat, false, false, 0.0f, null, 240, null);
    }

    @JvmOverloads
    @NotNull
    public final BattleStartResult pve(@NotNull ServerPlayer player, @NotNull PokemonEntity pokemonEntity, @Nullable UUID leadingPokemon) {
        Intrinsics.checkNotNullParameter((Object)player, (String)"player");
        Intrinsics.checkNotNullParameter((Object)pokemonEntity, (String)"pokemonEntity");
        return BattleBuilder.pve$default(this, player, pokemonEntity, leadingPokemon, null, false, false, 0.0f, null, 248, null);
    }

    @JvmOverloads
    @NotNull
    public final BattleStartResult pve(@NotNull ServerPlayer player, @NotNull PokemonEntity pokemonEntity) {
        Intrinsics.checkNotNullParameter((Object)player, (String)"player");
        Intrinsics.checkNotNullParameter((Object)pokemonEntity, (String)"pokemonEntity");
        return BattleBuilder.pve$default(this, player, pokemonEntity, null, null, false, false, 0.0f, null, 252, null);
    }

    @JvmOverloads
    @NotNull
    public final BattleStartResult pvn(@NotNull ServerPlayer player, @NotNull NPCEntity npcEntity, @Nullable UUID leadingPokemon, @NotNull BattleFormat battleFormat, boolean cloneParties, boolean healFirst) {
        Intrinsics.checkNotNullParameter((Object)player, (String)"player");
        Intrinsics.checkNotNullParameter((Object)npcEntity, (String)"npcEntity");
        Intrinsics.checkNotNullParameter((Object)battleFormat, (String)"battleFormat");
        return BattleBuilder.pvn$default(this, player, npcEntity, leadingPokemon, battleFormat, cloneParties, healFirst, null, 64, null);
    }

    @JvmOverloads
    @NotNull
    public final BattleStartResult pvn(@NotNull ServerPlayer player, @NotNull NPCEntity npcEntity, @Nullable UUID leadingPokemon, @NotNull BattleFormat battleFormat, boolean cloneParties) {
        Intrinsics.checkNotNullParameter((Object)player, (String)"player");
        Intrinsics.checkNotNullParameter((Object)npcEntity, (String)"npcEntity");
        Intrinsics.checkNotNullParameter((Object)battleFormat, (String)"battleFormat");
        return BattleBuilder.pvn$default(this, player, npcEntity, leadingPokemon, battleFormat, cloneParties, false, null, 96, null);
    }

    @JvmOverloads
    @NotNull
    public final BattleStartResult pvn(@NotNull ServerPlayer player, @NotNull NPCEntity npcEntity, @Nullable UUID leadingPokemon, @NotNull BattleFormat battleFormat) {
        Intrinsics.checkNotNullParameter((Object)player, (String)"player");
        Intrinsics.checkNotNullParameter((Object)npcEntity, (String)"npcEntity");
        Intrinsics.checkNotNullParameter((Object)battleFormat, (String)"battleFormat");
        return BattleBuilder.pvn$default(this, player, npcEntity, leadingPokemon, battleFormat, false, false, null, 112, null);
    }

    @JvmOverloads
    @NotNull
    public final BattleStartResult pvn(@NotNull ServerPlayer player, @NotNull NPCEntity npcEntity, @Nullable UUID leadingPokemon) {
        Intrinsics.checkNotNullParameter((Object)player, (String)"player");
        Intrinsics.checkNotNullParameter((Object)npcEntity, (String)"npcEntity");
        return BattleBuilder.pvn$default(this, player, npcEntity, leadingPokemon, null, false, false, null, 120, null);
    }

    @JvmOverloads
    @NotNull
    public final BattleStartResult pvn(@NotNull ServerPlayer player, @NotNull NPCEntity npcEntity) {
        Intrinsics.checkNotNullParameter((Object)player, (String)"player");
        Intrinsics.checkNotNullParameter((Object)npcEntity, (String)"npcEntity");
        return BattleBuilder.pvn$default(this, player, npcEntity, null, null, false, false, null, 124, null);
    }

    private static final PlayerPartyStore pvp1v1$lambda$0(ServerPlayer it) {
        Intrinsics.checkNotNullParameter((Object)it, (String)"it");
        return PlayerExtensionsKt.party(it);
    }

    private static final Unit pvp1v1$lambda$6(List $battlePartyStores, PokemonBattle it) {
        Intrinsics.checkNotNullParameter((Object)it, (String)"it");
        it.getBattlePartyStores().addAll($battlePartyStores);
        return Unit.INSTANCE;
    }

    private static final PlayerPartyStore pvp2v2$lambda$0(ServerPlayer it) {
        Intrinsics.checkNotNullParameter((Object)it, (String)"it");
        return PlayerExtensionsKt.party(it);
    }

    private static final Unit pvp2v2$lambda$11(List $battlePartyStores, PokemonBattle it) {
        Intrinsics.checkNotNullParameter((Object)it, (String)"it");
        it.getBattlePartyStores().addAll($battlePartyStores);
        return Unit.INSTANCE;
    }

    private static final Unit pve$lambda$2(boolean $cloneParties, PokemonEntity $pokemonEntity, PokemonBattle it) {
        Intrinsics.checkNotNullParameter((Object)it, (String)"it");
        if (!$cloneParties) {
            $pokemonEntity.setBattleId(it.getBattleId());
        }
        return Unit.INSTANCE;
    }

    private static final Unit pvn$lambda$4(NPCEntity $npcEntity, PokemonBattle battle) {
        Intrinsics.checkNotNullParameter((Object)battle, (String)"battle");
        SynchedEntityData synchedEntityData = $npcEntity.getEntityData();
        Intrinsics.checkNotNullExpressionValue((Object)synchedEntityData, (String)"getEntityData(...)");
        EntityDataAccessor<Set<UUID>> entityDataAccessor = NPCEntity.Companion.getBATTLE_IDS();
        Intrinsics.checkNotNullExpressionValue(entityDataAccessor, (String)"<get-BATTLE_IDS>(...)");
        EntityExtensionsKt.update(synchedEntityData, entityDataAccessor, arg_0 -> BattleBuilder.pvn$lambda$4$0(battle, arg_0));
        return Unit.INSTANCE;
    }

    private static final Set pvn$lambda$4$0(PokemonBattle $battle, Set it) {
        Intrinsics.checkNotNull((Object)it);
        return SetsKt.plus((Set)it, (Object)$battle.getBattleId());
    }
}
