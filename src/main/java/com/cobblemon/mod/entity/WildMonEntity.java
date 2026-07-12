package com.cobblemon.mod.entity;

import java.util.UUID;

import com.cobblemon.mod.battle.BattleManager;
import com.cobblemon.mod.entity.ai.FleeWhenHurtGoal;
import com.cobblemon.mod.entity.ai.WildDespawnGoal;
import com.cobblemon.mod.sound.ModSounds;
import com.cobblemon.mod.species.MonForm;
import com.cobblemon.mod.species.MonGender;
import com.cobblemon.mod.species.MonSpecies;
import com.cobblemon.mod.species.OwnedMon;
import com.cobblemon.mod.species.SpawnRules;
import com.cobblemon.mod.species.StatBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Wild or sent-out companion Cobblemon — carries species + identity for creature system.
 */
public class WildMonEntity extends PathfinderMob {
    private static final EntityDataAccessor<String> DATA_SPECIES =
            SynchedEntityData.defineId(WildMonEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_LEVEL =
            SynchedEntityData.defineId(WildMonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_COMPANION =
            SynchedEntityData.defineId(WildMonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> DATA_FORM =
            SynchedEntityData.defineId(WildMonEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_GENDER =
            SynchedEntityData.defineId(WildMonEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> DATA_SIZE =
            SynchedEntityData.defineId(WildMonEntity.class, EntityDataSerializers.FLOAT);

    private boolean configured;
    private @Nullable UUID companionOwner;
    /** Full identity when converted from / to party. */
    private @Nullable OwnedMon identity;

    public WildMonEntity(EntityType<? extends WildMonEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                // Shorter notice range so mons don't "lock on" from far away
                .add(Attributes.FOLLOW_RANGE, 12.0)
                .add(Attributes.ARMOR, 0.0)
                // Scales hitbox via LivingEntity.getScale() → getDimensions
                .add(Attributes.SCALE, 1.0);
    }

    public static MobCategory spawnCategory() {
        return MobCategory.MISC;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.cry(getSpecies());
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return ModSounds.cry(getSpecies());
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.cry(getSpecies());
    }

    @Override
    public float getVoicePitch() {
        // Slight size-based pitch variance
        float s = getSizeScale();
        return Mth.clamp(1.15f - (s - 1f) * 0.2f, 0.75f, 1.35f);
    }

    @Override
    protected float getSoundVolume() {
        return 0.85f;
    }

    /** Play this mon's cry (send-out, battle start, etc.). */
    public void playCry() {
        SoundEvent cry = ModSounds.cry(getSpecies());
        this.level().playSound(null, getX(), getY(), getZ(), cry, SoundSource.NEUTRAL, getSoundVolume(), getVoicePitch());
    }

    /** Always hittable by thrown cube balls (and other projectiles). */
    @Override
    public boolean canBeHitByProjectile() {
        return !this.isRemoved() && !this.isCompanion();
    }

    @Override
    public boolean isPickable() {
        return !this.isCompanion() && super.isPickable();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new FollowCompanionOwnerGoal(this, 1.15, 4.0f, 2.0f));
        this.goalSelector.addGoal(2, new FleeWhenHurtGoal(this, 1.35));
        // Type-aware roaming: water types will stroll through water; others avoid it.
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0) {
            @Override
            public boolean canUse() {
                if (WildMonEntity.this.isCompanion()) {
                    return false;
                }
                // Water / dual-water mons may path into shallows instead of only dry land
                if (WildMonEntity.this.prefersWater()) {
                    return false; // RandomStroll in water handled below
                }
                return super.canUse();
            }
        });
        this.goalSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.RandomStrollGoal(this, 1.0) {
            @Override
            public boolean canUse() {
                return !WildMonEntity.this.isCompanion()
                        && WildMonEntity.this.prefersWater()
                        && super.canUse();
            }
        });
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 5.0f));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(6, new WildDespawnGoal(this));
    }

    /** Water-primary (or dual) mons roam freely including through water. */
    public boolean prefersWater() {
        var handle = com.cobblemon.mod.species.SpeciesHandle.of(getSpeciesId());
        if (handle.primaryType() == com.cobblemon.mod.species.MonElement.WATER) {
            return true;
        }
        return handle.secondaryType().orElse(null) == com.cobblemon.mod.species.MonElement.WATER;
    }

    /** Flying / ghost types are slightly faster roamers. */
    public boolean isSwiftRoamer() {
        var handle = com.cobblemon.mod.species.SpeciesHandle.of(getSpeciesId());
        var t = handle.primaryType();
        return t == com.cobblemon.mod.species.MonElement.FLYING
                || t == com.cobblemon.mod.species.MonElement.GHOST
                || t == com.cobblemon.mod.species.MonElement.ELECTRIC;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SPECIES, MonSpecies.RATTATA.id());
        builder.define(DATA_LEVEL, 5);
        builder.define(DATA_COMPANION, false);
        builder.define(DATA_FORM, MonForm.NORMAL.id());
        builder.define(DATA_GENDER, MonGender.GENDERLESS.id());
        builder.define(DATA_SIZE, 1.0f);
    }

    public MonSpecies getSpecies() {
        return com.cobblemon.mod.species.SpeciesHandle.of(getSpeciesId()).asEnumOrFallback();
    }

    public String getSpeciesId() {
        return this.entityData.get(DATA_SPECIES);
    }

    public void setSpecies(MonSpecies species) {
        setSpeciesId(species == null ? MonSpecies.RATTATA.id() : species.id());
    }

    public void setSpeciesId(String speciesId) {
        this.configured = true;
        this.entityData.set(DATA_SPECIES, speciesId == null ? MonSpecies.RATTATA.id() : speciesId);
        refreshStats();
        updateDisplayName();
        // Not always-on (no wall spam); look-at shows name via renderer crosshair pick
        setCustomNameVisible(false);
        applyHitboxScale();
    }

    public int getMonLevel() {
        return this.entityData.get(DATA_LEVEL);
    }

    public void setMonLevel(int level) {
        this.configured = true;
        this.entityData.set(DATA_LEVEL, Mth.clamp(level, 1, OwnedMon.MAX_LEVEL));
        refreshStats();
        updateDisplayName();
    }

    private void updateDisplayName() {
        setCustomName(Component.empty()
                .append(com.cobblemon.mod.species.SpeciesHandle.of(getSpeciesId()).displayName())
                .append(Component.literal(" Lv." + getMonLevel())));
    }

    public MonForm getForm() {
        return MonForm.byId(this.entityData.get(DATA_FORM));
    }

    public void setForm(MonForm form) {
        this.entityData.set(DATA_FORM, form.id());
        refreshStats();
    }

    public MonGender getGender() {
        return MonGender.byId(this.entityData.get(DATA_GENDER));
    }

    public void setGender(MonGender gender) {
        this.entityData.set(DATA_GENDER, gender.id());
    }

    public float getSizeScale() {
        return this.entityData.get(DATA_SIZE);
    }

    public void setSizeScale(float scale) {
        this.entityData.set(DATA_SIZE, Mth.clamp(scale, 0.5f, 2.5f));
        refreshStats();
        applyHitboxScale();
    }

    /**
     * Match collision/pick box to species size (base entity is 0.75×0.95).
     * Uses Attributes.SCALE so LivingEntity.getDimensions scales correctly.
     */
    private void applyHitboxScale() {
        var scaleAttr = getAttribute(Attributes.SCALE);
        if (scaleAttr == null) {
            return;
        }
        var handle = com.cobblemon.mod.species.SpeciesHandle.of(getSpeciesId());
        // Stage + species size → hitbox. Keep small stage-1 mons easy to hit, finals bulkier.
        float s = handle.sizeScale() * getSizeScale();
        if (handle.stage() >= 3) {
            s *= 1.15f;
        } else if (handle.stage() <= 1) {
            s *= 0.95f;
        }
        // Tighter clamp — huge boxes made "I'm not even close" battles too common
        s = Mth.clamp(s, 0.7f, 1.35f);
        scaleAttr.setBaseValue(s);
        // Safe after configure — avoid during pure worldgen chunk packing
        if (this.configured && !this.level().isClientSide()) {
            this.refreshDimensions();
        }
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        // Base chibi box; LivingEntity.getDimensions applies Attributes.SCALE once more
        return EntityDimensions.scalable(0.72f, 1.05f).withEyeHeight(0.88f).scale(this.getAgeScale());
    }

    public void setCompanionOwner(@Nullable UUID owner) {
        this.configured = true;
        this.companionOwner = owner;
        this.entityData.set(DATA_COMPANION, owner != null);
    }

    public @Nullable UUID getCompanionOwner() {
        return this.companionOwner;
    }

    public boolean isCompanion() {
        return this.entityData.get(DATA_COMPANION) || this.companionOwner != null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        // Only the companion owner may ride their mon — and only if the species is rideable
        if (!isCompanion() || companionOwner == null) {
            return false;
        }
        if (!this.getPassengers().isEmpty()) {
            return false;
        }
        if (!(passenger instanceof Player p) || !companionOwner.equals(p.getUUID())) {
            return false;
        }
        // Block small mons (Charmander etc.) — matches official Cobblemon ride tables
        return com.cobblemon.mod.species.SpeciesHandle.of(getSpeciesId()).isRideable();
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        Entity first = this.getFirstPassenger();
        if (first instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    @Override
    protected void tickRidden(Player player, net.minecraft.world.phys.Vec3 travelVector) {
        this.setRot(player.getYRot(), player.getXRot() * 0.5f);
        this.yRotO = this.yBodyRot = this.yHeadRot = this.getYRot();
        super.tickRidden(player, travelVector);
    }

    @Override
    protected net.minecraft.world.phys.Vec3 getRiddenInput(Player player, net.minecraft.world.phys.Vec3 travelVector) {
        float f = player.xxa * 0.5f;
        float f1 = player.zza;
        if (f1 <= 0.0f) {
            f1 *= 0.25f;
        }
        return new net.minecraft.world.phys.Vec3(f, 0.0, f1);
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        return (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.9f;
    }

    public void applyIdentity(OwnedMon mon) {
        this.identity = mon;
        setSpeciesId(mon.speciesId());
        setMonLevel(mon.level());
        setForm(mon.form());
        setGender(mon.gender());
        setSizeScale(mon.sizeScale());
        setHealth(Math.min(getMaxHealth(), mon.hp()));
    }

    public void randomize(RandomSource random) {
        SpawnRules.Pick pick = SpawnRules.pick(this.level(), this.blockPosition(), random);
        OwnedMon mon = OwnedMon.createWild(pick.speciesId(), pick.level(), random);
        applyIdentity(mon);
    }

    private void refreshStats() {
        int monLevel = getMonLevel();
        StatBlock stats = StatBlock.compute(
                com.cobblemon.mod.species.SpeciesHandle.of(getSpeciesId()),
                monLevel,
                com.cobblemon.mod.species.Nature.HARDY,
                getForm()
        );
        double maxHp = stats.hp();
        // Size slightly affects HP display bulk
        maxHp *= 0.9 + 0.1 * getSizeScale();
        var maxHealth = getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(maxHp);
        }
        var speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            float spe = stats.speed() / 100f;
            double base = 0.22 + spe * 0.12;
            if (isSwiftRoamer()) {
                base *= 1.12;
            }
            if (prefersWater() && this.isInWater()) {
                base *= 1.15;
            }
            speed.setBaseValue(base);
        }
        float hp = (float) maxHp;
        if (getHealth() <= 0 || getHealth() > hp) {
            setHealth(hp);
        } else {
            setHealth(Math.min(getHealth(), hp));
        }
        applyHitboxScale();
    }

    public OwnedMon toOwnedMon() {
        if (identity != null && identity.speciesId().equals(getSpeciesId())) {
            return identity.withHp((int) getHealth()).withLevel(getMonLevel());
        }
        // createWild preserves full-dex speciesId (never Gen1 type stand-in)
        OwnedMon mon = OwnedMon.createWild(getSpeciesId(), getMonLevel(), this.getRandom());
        return mon.withHp((int) getHealth())
                .withLevel(getMonLevel());
    }

    @Override
    public Component getDisplayName() {
        Component name = Component.empty()
                .append(com.cobblemon.mod.species.SpeciesHandle.of(getSpeciesId()).displayName())
                .append(Component.literal(" " + getGender().symbol()))
                .append(Component.literal(" Lv." + getMonLevel()));
        if (getForm() != MonForm.NORMAL) {
            name = Component.empty().append(Component.literal("[" + getForm().id() + "] ")).append(name);
        }
        return name;
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            EntitySpawnReason spawnReason,
            @Nullable SpawnGroupData spawnData
    ) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnReason, spawnData);
        // Never run heavy species rolls during chunk gen / natural packing.
        if (spawnReason == EntitySpawnReason.CHUNK_GENERATION
                || spawnReason == EntitySpawnReason.NATURAL
                || spawnReason == EntitySpawnReason.STRUCTURE) {
            return data;
        }
        if (!this.configured && !isCompanion()) {
            try {
                randomize(level.getRandom());
            } catch (Exception e) {
                applyIdentity(OwnedMon.createWild(MonSpecies.RATTATA, 5, level.getRandom()));
            }
        }
        return data;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount) {
        if (!isCompanion() && damageSource.getEntity() instanceof ServerPlayer player) {
            // Only start a fight if they're actually next to the mon
            if (!BattleManager.inBattle(player) && BattleManager.inEngageRange(player, this)) {
                BattleManager.tryStart(player, this);
            }
            // Never take vanilla damage from players (battle system handles fights)
            return false;
        }
        return super.hurtServer(level, damageSource, amount);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (isCompanion()) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            if (!BattleManager.inEngageRange(serverPlayer, this)) {
                return InteractionResult.PASS;
            }
            if (BattleManager.tryStart(serverPlayer, this)) {
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        input.getString("Species").ifPresent(id -> {
            this.entityData.set(DATA_SPECIES, id);
            this.configured = true;
        });
        this.entityData.set(DATA_LEVEL, input.getIntOr("MonLevel", 5));
        input.getString("Form").ifPresent(f -> this.entityData.set(DATA_FORM, f));
        input.getString("Gender").ifPresent(g -> this.entityData.set(DATA_GENDER, g));
        this.entityData.set(DATA_SIZE, input.getFloatOr("SizeScale", 1.0f));
        input.read("Owner", net.minecraft.core.UUIDUtil.CODEC).ifPresent(uuid -> {
            this.companionOwner = uuid;
            this.entityData.set(DATA_COMPANION, true);
        });
        this.configured = true;
        refreshStats();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        // Full-dex id — never Gen1 type stand-in (solrock must not become geodude)
        output.putString("Species", getSpeciesId());
        output.putInt("MonLevel", getMonLevel());
        output.putString("Form", getForm().id());
        output.putString("Gender", getGender().id());
        output.putFloat("SizeScale", getSizeScale());
        if (this.companionOwner != null) {
            output.store("Owner", net.minecraft.core.UUIDUtil.CODEC, this.companionOwner);
        }
    }
}
