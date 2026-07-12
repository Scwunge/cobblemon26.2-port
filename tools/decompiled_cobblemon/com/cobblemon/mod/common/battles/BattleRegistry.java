/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  kotlin.Metadata
 *  kotlin.Unit
 *  kotlin.collections.CollectionsKt
 *  kotlin.collections.MapsKt
 *  kotlin.collections.SetsKt
 *  kotlin.comparisons.ComparisonsKt
 *  kotlin.jvm.JvmStatic
 *  kotlin.jvm.functions.Function0
 *  kotlin.jvm.functions.Function1
 *  kotlin.jvm.internal.Intrinsics
 *  kotlin.jvm.internal.SourceDebugExtension
 *  kotlin.text.StringsKt
 *  net.minecraft.server.level.ServerPlayer
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package com.cobblemon.mod.common.battles;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.Cancelable;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleStartedEvent;
import com.cobblemon.mod.common.api.moves.HiddenPowerUtil;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.pokemon.helditem.HeldItemProvider;
import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.api.pokemon.status.Statuses;
import com.cobblemon.mod.common.api.reactive.CancelableObservable;
import com.cobblemon.mod.common.api.reactive.EventObservable;
import com.cobblemon.mod.common.battles.ActiveBattlePokemon;
import com.cobblemon.mod.common.battles.BattleFormat;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.battles.BattleStartError;
import com.cobblemon.mod.common.battles.BattleStartResult;
import com.cobblemon.mod.common.battles.ErroredBattleStart;
import com.cobblemon.mod.common.battles.ShowdownMoveset;
import com.cobblemon.mod.common.battles.ShowdownMovesetAdapter;
import com.cobblemon.mod.common.battles.SuccessfulBattleStart;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.battles.runner.ShowdownService;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Nature;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.status.PersistentStatus;
import com.cobblemon.mod.common.pokemon.status.PersistentStatusContainer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import kotlin.Metadata;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.collections.MapsKt;
import kotlin.collections.SetsKt;
import kotlin.comparisons.ComparisonsKt;
import kotlin.jvm.JvmStatic;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.SourceDebugExtension;
import kotlin.text.StringsKt;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Metadata(mv={2, 2, 0}, k=1, xi=48, d1={"\u0000f\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\t\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003J\r\u0010\u0005\u001a\u00020\u0004\u00a2\u0006\u0004\b\u0005\u0010\u0003J\u0015\u0010\b\u001a\u00020\u00042\u0006\u0010\u0007\u001a\u00020\u0006\u00a2\u0006\u0004\b\b\u0010\tJ\u0017\u0010\r\u001a\u00020\f*\b\u0012\u0004\u0012\u00020\u000b0\n\u00a2\u0006\u0004\b\r\u0010\u000eJ\u0017\u0010\u0011\u001a\u00020\u00042\u0006\u0010\u0010\u001a\u00020\u000fH\u0002\u00a2\u0006\u0004\b\u0011\u0010\u0012J1\u0010\u001b\u001a\u00020\u001a2\u0006\u0010\u0014\u001a\u00020\u00132\u0006\u0010\u0016\u001a\u00020\u00152\u0006\u0010\u0017\u001a\u00020\u00152\b\b\u0002\u0010\u0019\u001a\u00020\u0018H\u0007\u00a2\u0006\u0004\b\u001b\u0010\u001cJ\u0017\u0010\u001d\u001a\u00020\u00042\u0006\u0010\u0010\u001a\u00020\u000fH\u0007\u00a2\u0006\u0004\b\u001d\u0010\u0012J\u0019\u0010 \u001a\u0004\u0018\u00010\u000f2\u0006\u0010\u001f\u001a\u00020\u001eH\u0007\u00a2\u0006\u0004\b \u0010!J\u0019\u0010#\u001a\u0004\u0018\u00010\u000f2\u0006\u0010\"\u001a\u00020\u0006H\u0007\u00a2\u0006\u0004\b#\u0010$J\u0019\u0010&\u001a\u0004\u0018\u00010\u000f2\u0006\u0010%\u001a\u00020\u001eH\u0007\u00a2\u0006\u0004\b&\u0010!J\r\u0010'\u001a\u00020\u0004\u00a2\u0006\u0004\b'\u0010\u0003R\u001f\u0010*\u001a\n )*\u0004\u0018\u00010(0(8\u0006\u00a2\u0006\f\n\u0004\b*\u0010+\u001a\u0004\b,\u0010-R \u0010/\u001a\u000e\u0012\u0004\u0012\u00020\u001e\u0012\u0004\u0012\u00020\u000f0.8\u0002X\u0082\u0004\u00a2\u0006\u0006\n\u0004\b/\u00100\u00a8\u00061"}, d2={"Lcom/cobblemon/mod/common/battles/BattleRegistry;", "", "<init>", "()V", "", "onServerStarted", "Lnet/minecraft/server/level/ServerPlayer;", "player", "onPlayerDisconnect", "(Lnet/minecraft/server/level/ServerPlayer;)V", "", "Lcom/cobblemon/mod/common/battles/pokemon/BattlePokemon;", "", "packTeam", "(Ljava/util/List;)Ljava/lang/String;", "Lcom/cobblemon/mod/common/api/battles/model/PokemonBattle;", "battle", "startShowdown", "(Lcom/cobblemon/mod/common/api/battles/model/PokemonBattle;)V", "Lcom/cobblemon/mod/common/battles/BattleFormat;", "battleFormat", "Lcom/cobblemon/mod/common/battles/BattleSide;", "side1", "side2", "", "canPreempt", "Lcom/cobblemon/mod/common/battles/BattleStartResult;", "startBattle", "(Lcom/cobblemon/mod/common/battles/BattleFormat;Lcom/cobblemon/mod/common/battles/BattleSide;Lcom/cobblemon/mod/common/battles/BattleSide;Z)Lcom/cobblemon/mod/common/battles/BattleStartResult;", "closeBattle", "Ljava/util/UUID;", "id", "getBattle", "(Ljava/util/UUID;)Lcom/cobblemon/mod/common/api/battles/model/PokemonBattle;", "serverPlayer", "getBattleByParticipatingPlayer", "(Lnet/minecraft/server/level/ServerPlayer;)Lcom/cobblemon/mod/common/api/battles/model/PokemonBattle;", "playerId", "getBattleByParticipatingPlayerId", "tick", "Lcom/google/gson/Gson;", "kotlin.jvm.PlatformType", "gson", "Lcom/google/gson/Gson;", "getGson", "()Lcom/google/gson/Gson;", "Ljava/util/concurrent/ConcurrentHashMap;", "battleMap", "Ljava/util/concurrent/ConcurrentHashMap;", "common"})
@SourceDebugExtension(value={"SMAP\nBattleRegistry.kt\nKotlin\n*S Kotlin\n*F\n+ 1 BattleRegistry.kt\ncom/cobblemon/mod/common/battles/BattleRegistry\n+ 2 _Collections.kt\nkotlin/collections/CollectionsKt___CollectionsKt\n+ 3 fake.kt\nkotlin/jvm/internal/FakeKt\n+ 4 ArraysJVM.kt\nkotlin/collections/ArraysKt__ArraysJVMKt\n+ 5 EventObservables.kt\ncom/cobblemon/mod/common/api/reactive/CancelableObservable\n+ 6 EventObservables.kt\ncom/cobblemon/mod/common/api/reactive/EventObservable\n+ 7 _Arrays.kt\nkotlin/collections/ArraysKt___ArraysKt\n+ 8 EventObservables.kt\ncom/cobblemon/mod/common/api/reactive/CancelableObservable$postThen$1\n+ 9 EventObservables.kt\ncom/cobblemon/mod/common/api/reactive/EventObservable$post$1\n*L\n1#1,245:1\n1563#2:246\n1634#2,3:247\n1563#2:250\n1634#2,3:251\n1617#2,9:254\n1869#2:263\n1870#2:265\n1626#2:266\n1869#2,2:267\n1056#2:269\n1056#2:270\n1869#2,2:296\n1#3:264\n1#3:273\n37#4,2:271\n40#5,2:274\n42#5,2:279\n45#5:282\n47#5:292\n48#5:295\n18#6,2:276\n15#6,5:283\n20#6:291\n20#6:294\n13805#7:278\n13805#7:288\n13806#7:290\n13806#7:293\n40#8:281\n15#9:289\n*S KotlinDebug\n*F\n+ 1 BattleRegistry.kt\ncom/cobblemon/mod/common/battles/BattleRegistry\n*L\n94#1:246\n94#1:247,3\n99#1:250\n99#1:251,3\n166#1:254,9\n166#1:263\n166#1:265\n166#1:266\n167#1:267,2\n171#1:269\n176#1:270\n222#1:296,2\n166#1:264\n181#1:271,2\n212#1:274,2\n212#1:279,2\n212#1:282\n212#1:292\n212#1:295\n212#1:276,2\n214#1:283,5\n214#1:291\n212#1:294\n212#1:278\n214#1:288\n214#1:290\n212#1:293\n212#1:281\n214#1:289\n*E\n"})
public final class BattleRegistry {
    @NotNull
    public static final BattleRegistry INSTANCE = new BattleRegistry();
    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().registerTypeAdapter((Type)((Object)ShowdownMoveset.class), (Object)ShowdownMovesetAdapter.INSTANCE).create();
    @NotNull
    private static final ConcurrentHashMap<UUID, PokemonBattle> battleMap = new ConcurrentHashMap();

    private BattleRegistry() {
    }

    public final Gson getGson() {
        return gson;
    }

    public final void onServerStarted() {
        battleMap.clear();
    }

    public final void onPlayerDisconnect(@NotNull ServerPlayer player) {
        block0: {
            Intrinsics.checkNotNullParameter((Object)player, (String)"player");
            PokemonBattle pokemonBattle = BattleRegistry.getBattleByParticipatingPlayer(player);
            if (pokemonBattle == null) break block0;
            pokemonBattle.stop();
        }
    }

    /*
     * WARNING - void declaration
     */
    @NotNull
    public final String packTeam(@NotNull List<? extends BattlePokemon> $this$packTeam) {
        Intrinsics.checkNotNullParameter($this$packTeam, (String)"<this>");
        List team = new ArrayList();
        for (BattlePokemon battlePokemon : $this$packTeam) {
            void $this$mapTo$iv$iv;
            Collection collection;
            void $this$mapTo$iv$iv2;
            String string;
            Pokemon pk = battlePokemon.getEffectedPokemon();
            StringBuilder packedTeamBuilder = new StringBuilder();
            packedTeamBuilder.append(pk.showdownId() + "|");
            packedTeamBuilder.append("|");
            packedTeamBuilder.append(pk.getUuid() + "|");
            packedTeamBuilder.append(pk.getCurrentHealth() + "|");
            if (pk.getStatus() != null) {
                PersistentStatusContainer persistentStatusContainer = pk.getStatus();
                Intrinsics.checkNotNull((Object)persistentStatusContainer);
                string = persistentStatusContainer.getStatus().getShowdownName();
            } else {
                string = "";
            }
            String showdownStatus = string;
            packedTeamBuilder.append(showdownStatus + "|");
            Object[] objectArray = new PersistentStatus[]{Statuses.SLEEP, Statuses.FROZEN};
            PersistentStatusContainer persistentStatusContainer = pk.getStatus();
            StringBuilder stringBuilder = CollectionsKt.contains((Iterable)CollectionsKt.listOf((Object[])objectArray), (Object)(persistentStatusContainer != null ? persistentStatusContainer.getStatus() : null)) ? packedTeamBuilder.append("2|") : packedTeamBuilder.append("-1|");
            String string2 = HeldItemProvider.provideShowdownId(battlePokemon);
            if (string2 == null) {
                string2 = "";
            }
            String heldItemID = string2;
            packedTeamBuilder.append(heldItemID + "|");
            packedTeamBuilder.append(StringsKt.replace$default((String)pk.getAbility().getName(), (String)"_", (String)"", (boolean)false, (int)4, null) + "|");
            packedTeamBuilder.append(CollectionsKt.joinToString$default((Iterable)pk.getMoveSet().getMoves(), (CharSequence)",", null, null, (int)0, null, BattleRegistry::packTeam$lambda$0, (int)30, null) + "|");
            packedTeamBuilder.append(CollectionsKt.joinToString$default((Iterable)pk.getMoveSet().getMoves(), (CharSequence)",", null, null, (int)0, null, BattleRegistry::packTeam$lambda$1, (int)30, null) + "|");
            Nature battleNature = pk.getEffectiveNature();
            packedTeamBuilder.append(battleNature.getName().getPath() + "|");
            Iterable $this$map$iv = Stats.Companion.getPERMANENT();
            boolean $i$f$map = false;
            Iterable iterable = $this$map$iv;
            Iterable destination$iv$iv = new ArrayList(CollectionsKt.collectionSizeOrDefault((Iterable)$this$map$iv, (int)10));
            boolean $i$f$mapTo = false;
            for (Object item$iv$iv : $this$mapTo$iv$iv2) {
                void it;
                Stat stat = (Stat)item$iv$iv;
                collection = destination$iv$iv;
                boolean bl = false;
                collection.add(pk.getEvs().getOrDefault((Stat)it));
            }
            String evsInOrder = CollectionsKt.joinToString$default((Iterable)((List)destination$iv$iv), (CharSequence)",", null, null, (int)0, null, null, (int)62, null);
            packedTeamBuilder.append(evsInOrder + "|");
            packedTeamBuilder.append(pk.getGender().getShowdownName() + "|");
            Iterable $this$map$iv2 = Stats.Companion.getPERMANENT();
            boolean $i$f$map2 = false;
            destination$iv$iv = $this$map$iv2;
            Collection destination$iv$iv2 = new ArrayList(CollectionsKt.collectionSizeOrDefault((Iterable)$this$map$iv2, (int)10));
            boolean $i$f$mapTo2 = false;
            for (Object item$iv$iv : $this$mapTo$iv$iv) {
                void it;
                Stat bl = (Stat)item$iv$iv;
                collection = destination$iv$iv2;
                boolean bl2 = false;
                collection.add(pk.getIvs().getEffectiveBattleIV((Stat)it));
            }
            String ivsInOrder = CollectionsKt.joinToString$default((Iterable)((List)destination$iv$iv2), (CharSequence)",", null, null, (int)0, null, null, (int)62, null);
            packedTeamBuilder.append(ivsInOrder + "|");
            packedTeamBuilder.append((pk.getShiny() ? "S" : "") + "|");
            packedTeamBuilder.append(pk.getLevel() + "|");
            packedTeamBuilder.append(pk.getFriendship() + ",");
            String string3 = battlePokemon.getEffectedPokemon().getCaughtBall().getName().getPath();
            Intrinsics.checkNotNullExpressionValue((Object)string3, (String)"getPath(...)");
            String pokeball = StringsKt.replace$default((String)string3, (String)"_", (String)"", (boolean)false, (int)4, null);
            packedTeamBuilder.append(pokeball + ",");
            String hiddenPowerType = MapsKt.any(pk.getIvs().getHyperTrainedIVs()) ? HiddenPowerUtil.getHiddenPowerType(pk).getName() : "";
            packedTeamBuilder.append(hiddenPowerType + ",");
            packedTeamBuilder.append((pk.getGmaxFactor() ? "G" : "") + ",");
            packedTeamBuilder.append((pk.getDmaxLevel() < 10 ? Integer.valueOf(pk.getDmaxLevel()) : "") + ",");
            packedTeamBuilder.append(battlePokemon.getEffectedPokemon().getTeraType().getName() + ",");
            String string4 = packedTeamBuilder.toString();
            Intrinsics.checkNotNullExpressionValue((Object)string4, (String)"toString(...)");
            team.add(string4);
        }
        return CollectionsKt.joinToString$default((Iterable)team, (CharSequence)"]", null, null, (int)0, null, null, (int)62, null);
    }

    /*
     * WARNING - void declaration
     */
    private final void startShowdown(PokemonBattle battle) {
        List messages = new ArrayList();
        messages.add(">start { \"format\": " + battle.getFormat().toFormatJSON() + " }");
        int actorIndex = 1;
        for (BattleActor actor : battle.getSide1().getActors()) {
            actor.setShowdownId("p" + actorIndex);
            actorIndex += 2;
        }
        actorIndex = 2;
        for (BattleActor actor : battle.getSide2().getActors()) {
            actor.setShowdownId("p" + actorIndex);
            actorIndex += 2;
        }
        for (BattleActor actor : battle.getActors()) {
            void $this$mapNotNullTo$iv$iv;
            int n = battle.getFormat().getBattleType().getSlotsPerActor();
            int actor2 = 0;
            while (actor2 < n) {
                int it = actor2++;
                boolean bl = false;
                actor.getActivePokemon().add(new ActiveBattlePokemon(actor, null, 2, null));
            }
            Iterable $this$mapNotNull$iv = actor.getPokemonList();
            boolean $i$f$mapNotNull = false;
            Iterable bl = $this$mapNotNull$iv;
            Collection destination$iv$iv = new ArrayList();
            boolean $i$f$mapNotNullTo = false;
            void $this$forEach$iv$iv$iv = $this$mapNotNullTo$iv$iv;
            boolean $i$f$forEach = false;
            Iterator iterator = $this$forEach$iv$iv$iv.iterator();
            while (iterator.hasNext()) {
                PokemonEntity it$iv$iv;
                Object element$iv$iv$iv;
                Object element$iv$iv = element$iv$iv$iv = iterator.next();
                boolean bl2 = false;
                BattlePokemon it = (BattlePokemon)element$iv$iv;
                boolean bl3 = false;
                if (it.getEntity() == null) continue;
                boolean bl4 = false;
                destination$iv$iv.add(it$iv$iv);
            }
            List entities = (List)destination$iv$iv;
            Iterable $this$forEach$iv = entities;
            boolean $i$f$forEach2 = false;
            for (Object element$iv : $this$forEach$iv) {
                PokemonEntity it = (PokemonEntity)element$iv;
                boolean bl5 = false;
                it.setBattleId(battle.getBattleId());
            }
        }
        Iterable<BattleActor> $this$sortedBy$iv = battle.getActors();
        boolean $i$f$sortedBy = false;
        for (BattleActor actor : CollectionsKt.sortedWith($this$sortedBy$iv, (Comparator)new Comparator(){

            public final int compare(T a, T b) {
                BattleActor it = (BattleActor)a;
                boolean bl = false;
                Comparable comparable = (Comparable)((Object)it.getShowdownId());
                it = (BattleActor)b;
                Comparable comparable2 = comparable;
                bl = false;
                return ComparisonsKt.compareValues((Comparable)comparable2, (Comparable)((Comparable)((Object)it.getShowdownId())));
            }
        })) {
            messages.add(">player " + actor.getShowdownId() + " {\"name\":\"" + actor.getUuid() + "\",\"team\":\"" + this.packTeam(actor.getPokemonList()) + "\"}");
        }
        $this$sortedBy$iv = battle.getActors();
        $i$f$sortedBy = false;
        for (BattleActor actor : CollectionsKt.sortedWith($this$sortedBy$iv, (Comparator)new Comparator(){

            public final int compare(T a, T b) {
                BattleActor it = (BattleActor)a;
                boolean bl = false;
                Comparable comparable = (Comparable)((Object)it.getShowdownId());
                it = (BattleActor)b;
                Comparable comparable2 = comparable;
                bl = false;
                return ComparisonsKt.compareValues((Comparable)comparable2, (Comparable)((Comparable)((Object)it.getShowdownId())));
            }
        })) {
            messages.add(">" + actor.getShowdownId() + " team " + ((Collection)actor.getPokemonList()).size());
        }
        Collection $this$toTypedArray$iv = messages;
        boolean $i$f$toTypedArray = false;
        Collection thisCollection$iv = $this$toTypedArray$iv;
        ShowdownService.Companion.getService().startBattle(battle, thisCollection$iv.toArray(new String[0]));
    }

    /*
     * WARNING - void declaration
     */
    @JvmStatic
    @NotNull
    public static final BattleStartResult startBattle(@NotNull BattleFormat battleFormat, @NotNull BattleSide side1, @NotNull BattleSide side2, boolean canPreempt) {
        void this_$iv$iv;
        void $this$iv;
        Intrinsics.checkNotNullParameter((Object)battleFormat, (String)"battleFormat");
        Intrinsics.checkNotNullParameter((Object)side1, (String)"side1");
        Intrinsics.checkNotNullParameter((Object)side2, (String)"side2");
        PokemonBattle battle = new PokemonBattle(battleFormat, side1, side2);
        Function0 start = () -> BattleRegistry.startBattle$lambda$0(battle);
        if (!canPreempt) {
            start.invoke();
            Unit it = Unit.INSTANCE;
            boolean bl = false;
            return new SuccessfulBattleStart(battle);
        }
        BattleStartedEvent.Pre preBattleEvent = new BattleStartedEvent.Pre(battle, null, 2, null);
        CancelableObservable<BattleStartedEvent.Pre> it = CobblemonEvents.BATTLE_STARTED_PRE;
        Cancelable event$iv = preBattleEvent;
        boolean $i$f$postThen = false;
        EventObservable eventObservable = (EventObservable)$this$iv;
        Cancelable[] cancelableArray = new Cancelable[]{event$iv};
        Cancelable[] events$iv$iv = cancelableArray;
        boolean $i$f$post = false;
        this_$iv$iv.emit(Arrays.copyOf(events$iv$iv, events$iv$iv.length));
        Cancelable[] $this$forEach$iv$iv$iv = events$iv$iv;
        boolean $i$f$forEach = false;
        int n = $this$forEach$iv$iv$iv.length;
        for (int i = 0; i < n; ++i) {
            Cancelable it2;
            Cancelable element$iv$iv$iv;
            Cancelable it$iv = element$iv$iv$iv = $this$forEach$iv$iv$iv[i];
            boolean bl = false;
            if (!it$iv.isCanceled()) {
                void $this$iv2;
                it2 = (BattleStartedEvent.Pre)it$iv;
                boolean bl2 = false;
                start.invoke();
                EventObservable<BattleStartedEvent.Post> eventObservable2 = CobblemonEvents.BATTLE_STARTED_POST;
                BattleStartedEvent.Post[] postArray = new BattleStartedEvent.Post[]{new BattleStartedEvent.Post(battle)};
                BattleStartedEvent.Post[] events$iv = postArray;
                boolean $i$f$post2 = false;
                $this$iv2.emit(Arrays.copyOf(events$iv, events$iv.length));
                BattleStartedEvent.Post[] $this$forEach$iv$iv = events$iv;
                boolean $i$f$forEach2 = false;
                int n2 = $this$forEach$iv$iv.length;
                for (int j = 0; j < n2; ++j) {
                    BattleStartedEvent.Post element$iv$iv;
                    BattleStartedEvent.Post post2 = element$iv$iv = $this$forEach$iv$iv[j];
                    boolean bl3 = false;
                    BattleStartedEvent.Post it3 = post2;
                }
                return new SuccessfulBattleStart(battle);
            }
            Cancelable cancelable = it$iv;
            boolean bl4 = false;
            it2 = cancelable;
        }
        Object[] objectArray = new BattleStartError[]{BattleStartError.Companion.canceledByEvent(preBattleEvent.getReason())};
        return new ErroredBattleStart(SetsKt.mutableSetOf((Object[])objectArray), null, 2, null);
    }

    public static /* synthetic */ BattleStartResult startBattle$default(BattleFormat battleFormat, BattleSide battleSide, BattleSide battleSide2, boolean bl, int n, Object object) {
        if ((n & 8) != 0) {
            bl = true;
        }
        return BattleRegistry.startBattle(battleFormat, battleSide, battleSide2, bl);
    }

    @JvmStatic
    public static final void closeBattle(@NotNull PokemonBattle battle) {
        Intrinsics.checkNotNullParameter((Object)battle, (String)"battle");
        Iterable $this$forEach$iv = battle.getOnEndHandlers();
        boolean $i$f$forEach = false;
        for (Object element$iv : $this$forEach$iv) {
            Function1 it = (Function1)element$iv;
            boolean bl = false;
            it.invoke((Object)battle);
        }
        battleMap.remove(battle.getBattleId());
        ShowdownService.Companion.getService().endBattle(battle);
    }

    @JvmStatic
    @Nullable
    public static final PokemonBattle getBattle(@NotNull UUID id) {
        Intrinsics.checkNotNullParameter((Object)id, (String)"id");
        return battleMap.get(id);
    }

    @JvmStatic
    @Nullable
    public static final PokemonBattle getBattleByParticipatingPlayer(@NotNull ServerPlayer serverPlayer) {
        Object v1;
        block1: {
            Intrinsics.checkNotNullParameter((Object)serverPlayer, (String)"serverPlayer");
            Collection<PokemonBattle> collection = battleMap.values();
            Intrinsics.checkNotNullExpressionValue(collection, (String)"<get-values>(...)");
            Iterable iterable = collection;
            for (Object t : iterable) {
                PokemonBattle it = (PokemonBattle)t;
                boolean bl = false;
                if (!(it.getActor(serverPlayer) != null)) continue;
                v1 = t;
                break block1;
            }
            v1 = null;
        }
        return v1;
    }

    @JvmStatic
    @Nullable
    public static final PokemonBattle getBattleByParticipatingPlayerId(@NotNull UUID playerId) {
        Object v1;
        block1: {
            Intrinsics.checkNotNullParameter((Object)playerId, (String)"playerId");
            Collection<PokemonBattle> collection = battleMap.values();
            Intrinsics.checkNotNullExpressionValue(collection, (String)"<get-values>(...)");
            Iterable iterable = collection;
            for (Object t : iterable) {
                PokemonBattle it = (PokemonBattle)t;
                boolean bl = false;
                if (!CollectionsKt.contains(it.getPlayerUUIDs(), (Object)playerId)) continue;
                v1 = t;
                break block1;
            }
            v1 = null;
        }
        return v1;
    }

    public final void tick() {
        battleMap.forEachValue(Long.MAX_VALUE, arg_0 -> BattleRegistry.tick$lambda$1(BattleRegistry::tick$lambda$0, arg_0));
    }

    private static final CharSequence packTeam$lambda$0(Move move) {
        Intrinsics.checkNotNullParameter((Object)move, (String)"move");
        return StringsKt.replace$default((String)move.getName(), (String)"_", (String)"", (boolean)false, (int)4, null);
    }

    private static final CharSequence packTeam$lambda$1(Move move) {
        Intrinsics.checkNotNullParameter((Object)move, (String)"move");
        return move.getCurrentPp() + "/" + move.getMaxPp();
    }

    private static final Unit startBattle$lambda$0(PokemonBattle $battle) {
        ((Map)battleMap).put($battle.getBattleId(), $battle);
        INSTANCE.startShowdown($battle);
        return Unit.INSTANCE;
    }

    private static final Unit tick$lambda$0(PokemonBattle it) {
        it.tick();
        return Unit.INSTANCE;
    }

    private static final void tick$lambda$1(Function1 $tmp0, Object p0) {
        $tmp0.invoke(p0);
    }
}
