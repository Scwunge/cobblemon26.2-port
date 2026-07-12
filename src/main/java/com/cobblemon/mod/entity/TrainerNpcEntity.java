package com.cobblemon.mod.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.cobblemon.mod.battle.BattleManager;
import com.cobblemon.mod.species.OwnedMon;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Minimal trainer NPC scaffold (N1).
 * Holds a display name + team list (species + level). Right-click spawns the lead mon
 * as a temporary wild entity and starts a wild-style battle via {@link BattleManager}.
 */
public class TrainerNpcEntity extends PathfinderMob {
    private static final EntityDataAccessor<String> DATA_TRAINER_NAME =
            SynchedEntityData.defineId(TrainerNpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_TEAM_ENCODED =
            SynchedEntityData.defineId(TrainerNpcEntity.class, EntityDataSerializers.STRING);
    /** N3 — gym badge awarded on win (empty = regular trainer). */
    private static final EntityDataAccessor<String> DATA_BADGE_ID =
            SynchedEntityData.defineId(TrainerNpcEntity.class, EntityDataSerializers.STRING);

    public record TeamMember(String speciesId, int level) {
        public TeamMember {
            speciesId = speciesId == null || speciesId.isBlank() ? "rattata" : speciesId.toLowerCase(Locale.ROOT);
            level = Math.max(1, Math.min(100, level));
        }

        @Override
        public String toString() {
            return speciesId + ":" + level;
        }

        public static TeamMember parse(String s) {
            if (s == null || s.isBlank()) {
                return new TeamMember("rattata", 5);
            }
            String[] parts = s.trim().split(":");
            String id = parts[0].trim().toLowerCase(Locale.ROOT);
            int lvl = 5;
            if (parts.length > 1) {
                try {
                    lvl = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException ignored) {
                }
            }
            return new TeamMember(id, lvl);
        }
    }

    public TrainerNpcEntity(EntityType<? extends TrainerNpcEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 16.0)
                .add(Attributes.ARMOR, 0.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_TRAINER_NAME, "Trainer");
        builder.define(DATA_TEAM_ENCODED, encodeTeam(defaultTeam()));
        builder.define(DATA_BADGE_ID, "");
    }

    public String getBadgeId() {
        return this.entityData.get(DATA_BADGE_ID);
    }

    public void setBadgeId(String badgeId) {
        this.entityData.set(DATA_BADGE_ID, badgeId == null ? "" : badgeId.trim().toLowerCase(Locale.ROOT));
    }

    public boolean isGymLeader() {
        String b = getBadgeId();
        return b != null && !b.isBlank();
    }

    public static List<TeamMember> defaultTeam() {
        List<TeamMember> team = new ArrayList<>(3);
        team.add(new TeamMember("rattata", 5));
        team.add(new TeamMember("pidgey", 5));
        team.add(new TeamMember("caterpie", 4));
        return team;
    }

    public String getTrainerName() {
        return this.entityData.get(DATA_TRAINER_NAME);
    }

    public void setTrainerName(String name) {
        String n = name == null || name.isBlank() ? "Trainer" : name;
        this.entityData.set(DATA_TRAINER_NAME, n);
        setCustomName(Component.literal(n));
        setCustomNameVisible(true);
    }

    /** Trainer mon roster (not Entity.getTeam / scoreboard). */
    public List<TeamMember> getTrainerTeam() {
        return decodeTeam(this.entityData.get(DATA_TEAM_ENCODED));
    }

    public void setTrainerTeam(List<TeamMember> team) {
        List<TeamMember> copy = team == null || team.isEmpty() ? defaultTeam() : new ArrayList<>(team);
        this.entityData.set(DATA_TEAM_ENCODED, encodeTeam(copy));
    }

    public void setTeamFromString(String encoded) {
        setTrainerTeam(decodeTeam(encoded));
    }

    private static String encodeTeam(List<TeamMember> team) {
        if (team == null || team.isEmpty()) {
            return "rattata:5";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < team.size() && i < 6; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(team.get(i).toString());
        }
        return sb.toString();
    }

    private static List<TeamMember> decodeTeam(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return defaultTeam();
        }
        List<TeamMember> out = new ArrayList<>();
        for (String part : encoded.split(",")) {
            if (!part.isBlank()) {
                out.add(TeamMember.parse(part));
            }
            if (out.size() >= 6) {
                break;
            }
        }
        return out.isEmpty() ? defaultTeam() : out;
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        // Ensure nameplate matches trainer name after load
        String n = getTrainerName();
        if (n != null && !n.isBlank()) {
            setCustomName(Component.literal(n));
            setCustomNameVisible(true);
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (BattleManager.inBattle(serverPlayer)) {
            serverPlayer.sendSystemMessage(Component.literal("§cYou're already in a battle."));
            return InteractionResult.SUCCESS;
        }
        // Prefer dialogue graph when available; falls back to immediate battle
        try {
            com.cobblemon.mod.dialogue.DialogueManager.open(serverPlayer, "npc-example", this);
            return InteractionResult.SUCCESS;
        } catch (Throwable t) {
            List<TeamMember> team = getTrainerTeam();
            if (team.isEmpty()) {
                serverPlayer.sendSystemMessage(Component.literal("§cThis trainer has no team."));
                return InteractionResult.SUCCESS;
            }
            boolean started = tryStartTrainerBattle(serverPlayer, team);
            if (!started) {
                serverPlayer.sendSystemMessage(Component.literal(
                        "§e" + getTrainerName() + " wants to battle, but you can't right now (empty/fainted party?)."));
            }
            return InteractionResult.SUCCESS;
        }
    }

    /** Right-click / dialogue battle entry. */
    public boolean tryStartBattle(ServerPlayer player) {
        return tryStartTrainerBattle(player, getTrainerTeam());
    }

    /**
     * v0 trainer battle: spawn lead mon as a temporary wild foe and use wild battle path.
     */
    public boolean tryStartTrainerBattle(ServerPlayer player, List<TeamMember> team) {
        if (!(this.level() instanceof ServerLevel server)) {
            return false;
        }
        TeamMember lead = team.get(0);
        WildMonEntity foe = ModEntities.WILD_MON.get().create(server, EntitySpawnReason.TRIGGERED);
        if (foe == null) {
            return false;
        }
        // Place near the player so engage range is satisfied
        Vec3 towardPlayer = player.position().subtract(this.position());
        if (towardPlayer.lengthSqr() < 0.01) {
            towardPlayer = player.getLookAngle();
        }
        Vec3 spawnPos = player.position().subtract(towardPlayer.normalize().scale(1.5));
        foe.setPos(spawnPos.x, player.getY(), spawnPos.z);
        OwnedMon identity = OwnedMon.createWild(lead.speciesId(), lead.level(), player.getRandom());
        foe.applyIdentity(identity);
        foe.setCustomName(Component.literal(getTrainerName() + "'s " + identity.displayName().getString()));
        foe.setCustomNameVisible(true);
        server.addFreshEntity(foe);

        player.sendSystemMessage(Component.literal(
                "§6" + getTrainerName() + "§7 challenges you with §f"
                        + identity.displayName().getString() + " Lv." + lead.level() + "§7!"));

        // Wild path handles party checks / presentation
        if (!BattleManager.inEngageRange(player, foe)) {
            // Force closer if spawn drifted
            foe.setPos(player.getX() + 1.2, player.getY(), player.getZ());
        }
        boolean ok = BattleManager.tryStart(player, foe);
        // N3: attach badge reward to the session
        if (ok && isGymLeader()) {
            BattleManager.get(player).ifPresent(session -> session.setRewardBadgeId(getBadgeId()));
            player.sendSystemMessage(Component.literal(
                    "§eGym battle! Win to earn the §6"
                            + com.cobblemon.mod.party.PlayerBadges.displayName(getBadgeId()) + "§e."));
        }
        return ok;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        input.getString("TrainerName").ifPresent(this::setTrainerName);
        input.getString("Team").ifPresent(this::setTeamFromString);
        input.getString("BadgeId").ifPresent(this::setBadgeId);
        if (getTrainerName() == null || getTrainerName().isBlank()) {
            setTrainerName("Trainer");
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("TrainerName", getTrainerName());
        output.putString("Team", encodeTeam(getTrainerTeam()));
        if (getBadgeId() != null && !getBadgeId().isBlank()) {
            output.putString("BadgeId", getBadgeId());
        }
    }

    /** Unmodifiable view for commands / debug. */
    public List<TeamMember> teamView() {
        return Collections.unmodifiableList(getTrainerTeam());
    }
}
