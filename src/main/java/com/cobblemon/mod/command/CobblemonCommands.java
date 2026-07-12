package com.cobblemon.mod.command;

import com.cobblemon.mod.battle.BattleManager;
import com.cobblemon.mod.battle.BattleSession;
import com.cobblemon.mod.battle.MonMove;
import com.cobblemon.mod.entity.ModEntities;
import com.cobblemon.mod.entity.TrainerNpcEntity;
import com.cobblemon.mod.entity.WildMonEntity;
import com.cobblemon.mod.network.OpenPcPayload;
import com.cobblemon.mod.network.OpenStarterPayload;
import com.cobblemon.mod.party.PartyHelper;
import com.cobblemon.mod.party.PlayerParty;
import com.cobblemon.mod.species.DisabledSpecies;
import com.cobblemon.mod.species.MonSpecies;
import com.cobblemon.mod.species.OwnedMon;
import com.cobblemon.mod.species.SpeciesRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Top-level commands matching official Cobblemon (porter {@code command} package).
 * No {@code /cobblemon ...} namespace — only porter names / aliases.
 * <p>
 * Party slots are <b>1-based</b> (1 = lead).
 */
public final class CobblemonCommands {
    private CobblemonCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // levelup
        registerOp(dispatcher, "levelup", b -> b
                .executes(ctx -> levelUp(ctx, self(ctx), 1))
                .then(Commands.argument("slot", IntegerArgumentType.integer(1, 6))
                        .executes(ctx -> levelUp(ctx, self(ctx), IntegerArgumentType.getInteger(ctx, "slot"))))
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("slot", IntegerArgumentType.integer(1, 6))
                                .executes(ctx -> levelUp(ctx, EntityArgument.getPlayer(ctx, "player"),
                                        IntegerArgumentType.getInteger(ctx, "slot"))))));

        // healpokemon + pokeheal
        registerOp(dispatcher, "healpokemon", CobblemonCommands::attachHeal);
        registerOp(dispatcher, "pokeheal", CobblemonCommands::attachHeal);

        // givepokemon / pokegive
        registerOp(dispatcher, "givepokemon", CobblemonCommands::attachGive);
        registerOp(dispatcher, "pokegive", CobblemonCommands::attachGive);
        registerOp(dispatcher, "givepokemonother", CobblemonCommands::attachGiveOther);
        registerOp(dispatcher, "pokegiveother", CobblemonCommands::attachGiveOther);

        // spawnpokemon / pokespawn
        registerOp(dispatcher, "spawnpokemon", CobblemonCommands::attachSpawn);
        registerOp(dispatcher, "pokespawn", CobblemonCommands::attachSpawn);

        // takepokemon
        registerOp(dispatcher, "takepokemon", b -> b
                .then(Commands.argument("slot", IntegerArgumentType.integer(1, 6))
                        .executes(ctx -> takePokemon(ctx, self(ctx), IntegerArgumentType.getInteger(ctx, "slot"))))
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("slot", IntegerArgumentType.integer(1, 6))
                                .executes(ctx -> takePokemon(ctx, EntityArgument.getPlayer(ctx, "player"),
                                        IntegerArgumentType.getInteger(ctx, "slot"))))));

        // clearparty / clearpc
        registerOp(dispatcher, "clearparty", b -> b
                .executes(ctx -> clearParty(ctx, self(ctx)))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> clearParty(ctx, EntityArgument.getPlayer(ctx, "player")))));
        registerOp(dispatcher, "clearpc", b -> b
                .executes(ctx -> clearPc(ctx, self(ctx)))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> clearPc(ctx, EntityArgument.getPlayer(ctx, "player")))));

        // openstarterscreen / pc / pokedex / trade (player UI — no op required)
        dispatcher.register(Commands.literal("openstarterscreen").executes(CobblemonCommands::openStarter));
        dispatcher.register(Commands.literal("pc").executes(ctx -> openPc(ctx, self(ctx))));
        dispatcher.register(Commands.literal("pokedex").executes(CobblemonCommands::pokedex));
        dispatcher.register(Commands.literal("trade").executes(CobblemonCommands::openTrade));

        // spawntrainer — op scaffold for N1; optional badge for N3 gym leaders
        registerOp(dispatcher, "spawntrainer", b -> b
                .executes(ctx -> spawnTrainer(ctx, "Trainer", ""))
                .then(Commands.argument("name", StringArgumentType.string())
                        .executes(ctx -> spawnTrainer(ctx, StringArgumentType.getString(ctx, "name"), ""))
                        .then(Commands.argument("badge", StringArgumentType.word())
                                .executes(ctx -> spawnTrainer(ctx,
                                        StringArgumentType.getString(ctx, "name"),
                                        StringArgumentType.getString(ctx, "badge"))))));

        // badges / givebadge (N3)
        dispatcher.register(Commands.literal("badges").executes(CobblemonCommands::listBadges));
        registerOp(dispatcher, "givebadge", b -> b
                .then(Commands.argument("badge", StringArgumentType.word())
                        .executes(ctx -> giveBadge(ctx, self(ctx), StringArgumentType.getString(ctx, "badge")))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> giveBadge(ctx,
                                        EntityArgument.getPlayer(ctx, "player"),
                                        StringArgumentType.getString(ctx, "badge"))))));

        // doubleduel — B3 test: start doubles with two nearest wilds
        registerOp(dispatcher, "doubleduel", b -> b.executes(CobblemonCommands::doubleDuel));

        // teach
        registerOp(dispatcher, "teach", b -> b
                .then(Commands.argument("slot", IntegerArgumentType.integer(1, 6))
                        .then(Commands.argument("move", StringArgumentType.word())
                                .executes(ctx -> teach(ctx, self(ctx),
                                        IntegerArgumentType.getInteger(ctx, "slot"),
                                        StringArgumentType.getString(ctx, "move"))))));

        // pokemonedit / pokeedit
        registerOp(dispatcher, "pokemonedit", CobblemonCommands::attachPokeEdit);
        registerOp(dispatcher, "pokeedit", CobblemonCommands::attachPokeEdit);
        registerOp(dispatcher, "pokemoneditother", CobblemonCommands::attachPokeEditOther);
        registerOp(dispatcher, "pokeeditother", CobblemonCommands::attachPokeEditOther);

        // stopbattle
        registerOp(dispatcher, "stopbattle", b -> b.executes(CobblemonCommands::stopBattle));

        // giveegg <species> [hatchTicks] — test egg hatch path
        registerOp(dispatcher, "giveegg", b -> b
                .then(Commands.argument("species", StringArgumentType.word())
                        .executes(ctx -> giveEgg(ctx, StringArgumentType.getString(ctx, "species"), 20 * 10))
                        .then(Commands.argument("ticks", IntegerArgumentType.integer(1, 20 * 60 * 60))
                                .executes(ctx -> giveEgg(ctx, StringArgumentType.getString(ctx, "species"),
                                        IntegerArgumentType.getInteger(ctx, "ticks"))))));

        // makeshiny [slot] — force party mon shiny (test C1)
        registerOp(dispatcher, "makeshiny", b -> b
                .executes(ctx -> makeShiny(ctx, 1))
                .then(Commands.argument("slot", IntegerArgumentType.integer(1, 6))
                        .executes(ctx -> makeShiny(ctx, IntegerArgumentType.getInteger(ctx, "slot")))));

        // helditem
        registerOp(dispatcher, "helditem", b -> b
                .then(Commands.argument("slot", IntegerArgumentType.integer(1, 6))
                        .then(Commands.argument("item", StringArgumentType.word())
                                .executes(ctx -> heldItem(ctx, self(ctx),
                                        IntegerArgumentType.getInteger(ctx, "slot"),
                                        StringArgumentType.getString(ctx, "item"))))
                        .then(Commands.literal("clear")
                                .executes(ctx -> heldItem(ctx, self(ctx),
                                        IntegerArgumentType.getInteger(ctx, "slot"), "")))));

        // pokemonrestart / pokerestart
        registerOp(dispatcher, "pokemonrestart", CobblemonCommands::attachRestart);
        registerOp(dispatcher, "pokerestart", CobblemonCommands::attachRestart);
        registerOp(dispatcher, "pokemonrestartother", CobblemonCommands::attachRestartOther);
        registerOp(dispatcher, "pokerestartother", CobblemonCommands::attachRestartOther);

        // freezepokemon — no-op friendly message if not looking at mon
        registerOp(dispatcher, "freezepokemon", b -> b.executes(ctx -> {
            ctx.getSource().sendFailure(Component.literal(
                    "freezepokemon requires a look-targeted Pokémon entity (debug). Use stopbattle for battles."));
            return 0;
        }));
    }

    // ---- registration helpers ----

    @FunctionalInterface
    private interface NodeBuilder {
        LiteralArgumentBuilder<CommandSourceStack> build(LiteralArgumentBuilder<CommandSourceStack> root);
    }

    private static void registerOp(
            CommandDispatcher<CommandSourceStack> dispatcher,
            String name,
            NodeBuilder builder
    ) {
        dispatcher.register(builder.build(
                Commands.literal(name).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
        ));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> attachHeal(LiteralArgumentBuilder<CommandSourceStack> b) {
        return b.executes(ctx -> heal(ctx, self(ctx)))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> heal(ctx, EntityArgument.getPlayer(ctx, "player"))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> attachGive(LiteralArgumentBuilder<CommandSourceStack> b) {
        return b.then(Commands.argument("species", StringArgumentType.word())
                .executes(ctx -> give(ctx, self(ctx), 5))
                .then(Commands.argument("level", IntegerArgumentType.integer(1, 100))
                        .executes(ctx -> give(ctx, self(ctx), IntegerArgumentType.getInteger(ctx, "level")))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> attachGiveOther(LiteralArgumentBuilder<CommandSourceStack> b) {
        return b.then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("species", StringArgumentType.word())
                        .executes(ctx -> give(ctx, EntityArgument.getPlayer(ctx, "player"), 5))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 100))
                                .executes(ctx -> give(ctx, EntityArgument.getPlayer(ctx, "player"),
                                        IntegerArgumentType.getInteger(ctx, "level"))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> attachSpawn(LiteralArgumentBuilder<CommandSourceStack> b) {
        return b.then(Commands.argument("species", StringArgumentType.word())
                .executes(ctx -> spawn(ctx, 10))
                .then(Commands.argument("level", IntegerArgumentType.integer(1, 100))
                        .executes(ctx -> spawn(ctx, IntegerArgumentType.getInteger(ctx, "level")))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> attachPokeEdit(LiteralArgumentBuilder<CommandSourceStack> b) {
        return b.then(Commands.argument("slot", IntegerArgumentType.integer(1, 6))
                .then(Commands.literal("level")
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 100))
                                .executes(ctx -> editLevel(ctx, self(ctx),
                                        IntegerArgumentType.getInteger(ctx, "slot"),
                                        IntegerArgumentType.getInteger(ctx, "level")))))
                .then(Commands.literal("nickname")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> editNickname(ctx, self(ctx),
                                        IntegerArgumentType.getInteger(ctx, "slot"),
                                        StringArgumentType.getString(ctx, "name"))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> attachPokeEditOther(LiteralArgumentBuilder<CommandSourceStack> b) {
        return b.then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("slot", IntegerArgumentType.integer(1, 6))
                        .then(Commands.literal("level")
                                .then(Commands.argument("level", IntegerArgumentType.integer(1, 100))
                                        .executes(ctx -> editLevel(ctx, EntityArgument.getPlayer(ctx, "player"),
                                                IntegerArgumentType.getInteger(ctx, "slot"),
                                                IntegerArgumentType.getInteger(ctx, "level")))))
                        .then(Commands.literal("nickname")
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .executes(ctx -> editNickname(ctx, EntityArgument.getPlayer(ctx, "player"),
                                                IntegerArgumentType.getInteger(ctx, "slot"),
                                                StringArgumentType.getString(ctx, "name")))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> attachRestart(LiteralArgumentBuilder<CommandSourceStack> b) {
        return b.executes(ctx -> restart(ctx, self(ctx), true))
                .then(Commands.literal("partyonly")
                        .executes(ctx -> restart(ctx, self(ctx), false)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> attachRestartOther(LiteralArgumentBuilder<CommandSourceStack> b) {
        return b.then(Commands.argument("player", EntityArgument.player())
                .executes(ctx -> restart(ctx, EntityArgument.getPlayer(ctx, "player"), true))
                .then(Commands.literal("partyonly")
                        .executes(ctx -> restart(ctx, EntityArgument.getPlayer(ctx, "player"), false))));
    }

    // ---- helpers ----

    private static ServerPlayer self(CommandContext<CommandSourceStack> ctx) {
        return ctx.getSource().getPlayer();
    }

    private static int toIndex(int slot1Based) {
        return slot1Based - 1;
    }

    // ---- command bodies ----

    private static int levelUp(CommandContext<CommandSourceStack> ctx, ServerPlayer player, int slot1) {
        if (player == null) {
            return 0;
        }
        int idx = toIndex(slot1);
        PlayerParty party = PartyHelper.get(player);
        if (party.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("Party is empty."));
            return 0;
        }
        if (idx < 0 || idx >= party.size()) {
            ctx.getSource().sendFailure(Component.literal(
                    "Your party only has " + party.size() + " slot(s). Use 1–" + party.size() + "."));
            return 0;
        }
        OwnedMon mon = party.get(idx).orElseThrow();
        if (mon.level() >= OwnedMon.MAX_LEVEL) {
            ctx.getSource().sendFailure(Component.literal(
                    mon.displayName().getString() + " is already Lv." + OwnedMon.MAX_LEVEL + "."));
            return 0;
        }
        int before = mon.level();
        if (!PartyHelper.levelUpOne(player, idx)) {
            ctx.getSource().sendFailure(Component.literal("Could not level up."));
            return 0;
        }
        OwnedMon after = PartyHelper.get(player).get(idx).orElse(mon);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§aLeveled up " + after.displayName().getString()
                        + " §7Lv." + before + " → §aLv." + after.level()
        ), true);
        return 1;
    }

    private static int heal(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        PartyHelper.healAll(player);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§aHealed " + player.getScoreboardName() + "'s party."
        ), true);
        return 1;
    }

    private static int give(CommandContext<CommandSourceStack> ctx, ServerPlayer player, int level) {
        if (player == null) {
            return 0;
        }
        String id = StringArgumentType.getString(ctx, "species").toLowerCase();
        if (SpeciesRegistry.get(id).isEmpty() && MonSpecies.byId(id).isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("Unknown species: " + id));
            return 0;
        }
        if (DisabledSpecies.isDisabled(id)) {
            ctx.getSource().sendFailure(Component.literal(id + " is disabled (no model)."));
            return 0;
        }
        if (PartyHelper.get(player).isFull()) {
            ctx.getSource().sendFailure(Component.literal("Party is full."));
            return 0;
        }
        OwnedMon mon = OwnedMon.createWild(id, level, player.getRandom());
        PartyHelper.addMon(player, mon);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Gave " + mon.displayName().getString() + " Lv." + mon.level()
                        + " (" + mon.speciesId() + ")"
                        + (mon.isShiny() ? " §e★Shiny" : "")
                        + " to " + player.getScoreboardName() + "."
        ), true);
        return 1;
    }

    private static int giveEgg(CommandContext<CommandSourceStack> ctx, String species, int hatchTicks) {
        ServerPlayer player = self(ctx);
        if (player == null) {
            return 0;
        }
        String id = species.toLowerCase();
        if (SpeciesRegistry.get(id).isEmpty() && MonSpecies.byId(id).isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("Unknown species: " + id));
            return 0;
        }
        net.minecraft.world.item.ItemStack egg = new net.minecraft.world.item.ItemStack(
                com.cobblemon.mod.item.ModItems.POKEMON_EGG.get());
        com.cobblemon.mod.item.PokemonEggItem.writeEggData(
                egg,
                id,
                hatchTicks,
                com.cobblemon.mod.species.StatBlock.rollIvs(player.getRandom()),
                java.util.List.of(),
                com.cobblemon.mod.species.MonForm.NORMAL.id(),
                false
        );
        if (!player.getInventory().add(egg)) {
            player.drop(egg, false);
        }
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Gave " + id + " egg (" + hatchTicks + " ticks) to " + player.getScoreboardName() + "."
        ), true);
        return 1;
    }

    private static int makeShiny(CommandContext<CommandSourceStack> ctx, int slot1) {
        ServerPlayer player = self(ctx);
        if (player == null) {
            return 0;
        }
        int idx = slot1 - 1;
        PlayerParty party = PartyHelper.get(player);
        OwnedMon mon = party.get(idx).orElse(null);
        if (mon == null) {
            ctx.getSource().sendFailure(Component.literal("Empty party slot " + slot1 + "."));
            return 0;
        }
        OwnedMon shiny = mon.withShiny(true);
        PartyHelper.setSlot(player, idx, shiny);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§e★ " + shiny.displayName().getString() + " is now shiny (form=" + shiny.form().id() + ")."
        ), true);
        return 1;
    }

    private static int spawn(CommandContext<CommandSourceStack> ctx, int level) {
        ServerPlayer player = self(ctx);
        if (player == null) {
            return 0;
        }
        String id = StringArgumentType.getString(ctx, "species").toLowerCase();
        if (SpeciesRegistry.get(id).isEmpty() && MonSpecies.byId(id).isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("Unknown species: " + id));
            return 0;
        }
        if (DisabledSpecies.isDisabled(id)) {
            ctx.getSource().sendFailure(Component.literal(id + " is disabled (no model)."));
            return 0;
        }
        ServerLevel world = player.level();
        WildMonEntity mon = ModEntities.WILD_MON.get().create(world, EntitySpawnReason.COMMAND);
        if (mon == null) {
            ctx.getSource().sendFailure(Component.literal("Failed to create entity."));
            return 0;
        }
        OwnedMon identity = OwnedMon.createWild(id, level, player.getRandom());
        Vec3 look = player.getLookAngle();
        Vec3 pos = player.position().add(look.x * 3.0, 0.0, look.z * 3.0);
        mon.setPos(pos.x, player.getY(), pos.z);
        mon.applyIdentity(identity);
        mon.setYRot(player.getYRot());
        world.addFreshEntity(mon);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Spawned " + identity.displayName().getString() + " Lv." + identity.level()
                        + " (" + identity.speciesId() + ")."
        ), true);
        return 1;
    }

    private static int takePokemon(CommandContext<CommandSourceStack> ctx, ServerPlayer player, int slot1) {
        if (player == null) {
            return 0;
        }
        var removed = PartyHelper.release(player, toIndex(slot1));
        if (removed.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("No Pokémon in slot " + slot1 + "."));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§cTook " + removed.get().displayName().getString() + " from party."
        ), true);
        return 1;
    }

    private static int clearParty(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        PartyHelper.clearParty(player);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§7Cleared party for " + player.getScoreboardName()
        ), true);
        return 1;
    }

    private static int clearPc(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        PartyHelper.clearPc(player);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§7Cleared PC for " + player.getScoreboardName()
        ), true);
        return 1;
    }

    private static int restart(CommandContext<CommandSourceStack> ctx, ServerPlayer player, boolean clearPcToo) {
        if (player == null) {
            return 0;
        }
        PartyHelper.clearParty(player);
        if (clearPcToo) {
            PartyHelper.clearPc(player);
        }
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§7Restarted " + player.getScoreboardName()
                        + (clearPcToo ? " (party + PC)." : " (party only).")
        ), true);
        return 1;
    }

    private static int teach(CommandContext<CommandSourceStack> ctx, ServerPlayer player, int slot1, String moveId) {
        if (player == null) {
            return 0;
        }
        int idx = toIndex(slot1);
        PlayerParty party = PartyHelper.get(player);
        if (idx < 0 || idx >= party.size()) {
            ctx.getSource().sendFailure(Component.literal("No Pokémon in slot " + slot1 + "."));
            return 0;
        }
        String key = moveId.toLowerCase().replace('-', '_');
        MonMove move = null;
        for (MonMove m : MonMove.values()) {
            if (m.id().equalsIgnoreCase(key) || m.id().equalsIgnoreCase(moveId)) {
                move = m;
                break;
            }
        }
        if (move == null) {
            ctx.getSource().sendFailure(Component.literal(
                    "Unknown move: " + moveId + " (try tackle, ember_snap, vine_lash, …)"));
            return 0;
        }
        OwnedMon mon = party.get(idx).orElseThrow();
        if (mon.knowsMove(move)) {
            ctx.getSource().sendFailure(Component.literal(
                    mon.displayName().getString() + " already knows " + move.englishName() + "."));
            return 0;
        }
        OwnedMon next = mon.learnMove(move);
        PartyHelper.setSlot(player, idx, next);
        final MonMove taught = move;
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§a" + next.displayName().getString() + " learned " + taught.englishName() + "!"
        ), true);
        return 1;
    }

    private static int editLevel(CommandContext<CommandSourceStack> ctx, ServerPlayer player, int slot1, int level) {
        if (player == null) {
            return 0;
        }
        int idx = toIndex(slot1);
        PlayerParty party = PartyHelper.get(player);
        if (idx < 0 || idx >= party.size()) {
            ctx.getSource().sendFailure(Component.literal("No Pokémon in slot " + slot1 + "."));
            return 0;
        }
        OwnedMon mon = party.get(idx).orElseThrow().withLevel(level).withExp(0);
        PartyHelper.setSlot(player, idx, mon);
        PartyHelper.tryLevelEvolve(player, idx);
        OwnedMon after = PartyHelper.get(player).get(idx).orElse(mon);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§aSet " + after.displayName().getString() + " to Lv." + after.level()
        ), true);
        return 1;
    }

    private static int editNickname(CommandContext<CommandSourceStack> ctx, ServerPlayer player, int slot1, String name) {
        if (player == null) {
            return 0;
        }
        int idx = toIndex(slot1);
        PlayerParty party = PartyHelper.get(player);
        if (idx < 0 || idx >= party.size()) {
            ctx.getSource().sendFailure(Component.literal("No Pokémon in slot " + slot1 + "."));
            return 0;
        }
        String nick = name.length() > 16 ? name.substring(0, 16) : name;
        OwnedMon mon = party.get(idx).orElseThrow().withNickname(nick);
        PartyHelper.setSlot(player, idx, mon);
        ctx.getSource().sendSuccess(() -> Component.literal("§aNickname set to \"" + nick + "\""), true);
        return 1;
    }

    private static int heldItem(CommandContext<CommandSourceStack> ctx, ServerPlayer player, int slot1, String itemId) {
        if (player == null) {
            return 0;
        }
        int idx = toIndex(slot1);
        PlayerParty party = PartyHelper.get(player);
        if (idx < 0 || idx >= party.size()) {
            ctx.getSource().sendFailure(Component.literal("No Pokémon in slot " + slot1 + "."));
            return 0;
        }
        String id = itemId == null ? "" : itemId.toLowerCase();
        if (id.startsWith("cobblemon:")) {
            id = id.substring("cobblemon:".length());
        }
        if (id.startsWith("minecraft:")) {
            id = id.substring("minecraft:".length());
        }
        OwnedMon mon = party.get(idx).orElseThrow().withHeldItem(id);
        PartyHelper.setSlot(player, idx, mon);
        if (id.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "§7Cleared held item on " + mon.displayName().getString()
            ), true);
        } else {
            final String held = id;
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "§a" + mon.displayName().getString() + " now holds " + held
            ), true);
        }
        return 1;
    }

    private static int openPc(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        PacketDistributor.sendToPlayer(player, new OpenPcPayload());
        return 1;
    }

    private static int openStarter(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = self(ctx);
        if (player == null) {
            return 0;
        }
        PacketDistributor.sendToPlayer(player, new OpenStarterPayload());
        return 1;
    }

    private static int pokedex(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = self(ctx);
        if (player == null) {
            return 0;
        }
        var dex = player.getData(com.cobblemon.mod.party.ModAttachments.POKEDEX);
        int total = Math.max(1, SpeciesRegistry.size());
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Pokédex — Seen: " + dex.seenCount() + " / Caught: " + dex.caughtCount()
                        + " (of ~" + total + " species)"
        ), false);
        return 1;
    }

    private static int openTrade(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = self(ctx);
        if (player == null) {
            return 0;
        }
        // Prefer request to nearest player (Cobblemon-style); else debug self-trade
        ServerPlayer nearest = null;
        double best = 12 * 12;
        for (ServerPlayer other : player.level().players()) {
            if (other == player) continue;
            double d = other.distanceToSqr(player);
            if (d < best) {
                best = d;
                nearest = other;
            }
        }
        if (nearest != null) {
            com.cobblemon.mod.trade.TradeManager.requestTrade(player, nearest);
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "§aTrade request sent to nearest player."), false);
        } else {
            com.cobblemon.mod.trade.TradeManager.open(player, null);
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "§aNo players nearby — opened debug self-trade."), false);
        }
        return 1;
    }

    private static int listBadges(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = self(ctx);
        if (player == null) {
            return 0;
        }
        var badges = com.cobblemon.mod.party.PlayerBadges.get(player);
        if (badges.count() == 0) {
            ctx.getSource().sendSuccess(() -> Component.literal("§7No gym badges yet. Defeat a gym leader!"), false);
            return 1;
        }
        StringBuilder sb = new StringBuilder("§eBadges (").append(badges.count()).append("): §f");
        for (String id : badges.ordered()) {
            sb.append(com.cobblemon.mod.party.PlayerBadges.displayName(id)).append("  ");
        }
        ctx.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    private static int giveBadge(CommandContext<CommandSourceStack> ctx, ServerPlayer player, String badge) {
        if (player == null) {
            return 0;
        }
        if (com.cobblemon.mod.party.PlayerBadges.award(player, badge)) {
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "§aAwarded " + com.cobblemon.mod.party.PlayerBadges.displayName(badge)
                            + " to " + player.getScoreboardName()
            ), true);
            return 1;
        }
        ctx.getSource().sendFailure(Component.literal("Already has that badge (or invalid id)."));
        return 0;
    }

    private static int doubleDuel(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = self(ctx);
        if (player == null) {
            return 0;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return 0;
        }
        java.util.List<WildMonEntity> near = level.getEntitiesOfClass(
                WildMonEntity.class,
                player.getBoundingBox().inflate(8),
                e -> !e.isCompanion() && !e.isRemoved()
        );
        if (near.size() < 2) {
            // Spawn two wilds and start
            WildMonEntity a = ModEntities.WILD_MON.get().create(level, EntitySpawnReason.COMMAND);
            WildMonEntity b = ModEntities.WILD_MON.get().create(level, EntitySpawnReason.COMMAND);
            if (a == null || b == null) {
                ctx.getSource().sendFailure(Component.literal("Failed to spawn foes."));
                return 0;
            }
            a.setPos(player.getX() + 1.5, player.getY(), player.getZ());
            b.setPos(player.getX() - 1.5, player.getY(), player.getZ());
            a.applyIdentity(OwnedMon.createWild("rattata", 8, player.getRandom()));
            b.applyIdentity(OwnedMon.createWild("pidgey", 8, player.getRandom()));
            level.addFreshEntity(a);
            level.addFreshEntity(b);
            near = java.util.List.of(a, b);
        }
        WildMonEntity w0 = near.get(0);
        WildMonEntity w1 = near.get(1);
        if (BattleManager.tryStartDoubles(player, w0, w1)) {
            ctx.getSource().sendSuccess(() -> Component.literal("§bDouble battle started!"), true);
            return 1;
        }
        ctx.getSource().sendFailure(Component.literal("Could not start doubles (need 2 healthy party mons)."));
        return 0;
    }

    private static int spawnTrainer(CommandContext<CommandSourceStack> ctx, String name, String badgeId) {
        ServerPlayer player = self(ctx);
        if (player == null) {
            return 0;
        }
        ServerLevel world = player.level();
        TrainerNpcEntity npc = ModEntities.TRAINER_NPC.get().create(world, EntitySpawnReason.COMMAND);
        if (npc == null) {
            ctx.getSource().sendFailure(Component.literal("Failed to create trainer NPC."));
            return 0;
        }
        Vec3 look = player.getLookAngle();
        Vec3 pos = player.position().add(look.x * 3.0, 0.0, look.z * 3.0);
        npc.setPos(pos.x, player.getY(), pos.z);
        npc.setTrainerName(name == null || name.isBlank() ? "Trainer" : name);
        npc.setTrainerTeam(TrainerNpcEntity.defaultTeam());
        if (badgeId != null && !badgeId.isBlank()) {
            npc.setBadgeId(badgeId);
        }
        npc.setYRot(player.getYRot() + 180f);
        world.addFreshEntity(npc);
        String badgeNote = npc.isGymLeader()
                ? " §6[gym: " + npc.getBadgeId() + " badge]"
                : "";
        ctx.getSource().sendSuccess(() -> Component.literal(
                "§aSpawned trainer §f" + npc.getTrainerName()
                        + " §7(team: " + npc.getTrainerTeam().size() + " mons)"
                        + badgeNote + "§7. Right-click to battle."
        ), true);
        return 1;
    }

    private static int stopBattle(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = self(ctx);
        if (player == null) {
            return 0;
        }
        BattleManager.end(player, BattleSession.Phase.RAN);
        BattleManager.forceClear(player.getUUID());
        ctx.getSource().sendSuccess(() -> Component.literal("§7Battle stopped."), true);
        return 1;
    }
}
