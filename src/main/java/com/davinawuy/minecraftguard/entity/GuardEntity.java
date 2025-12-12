package com.davinawuy.minecraftguard.entity;

import com.davinawuy.minecraftguard.ModEntities;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.goal.TrackOwnerAttackerGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraft.world.EntityView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.scoreboard.AbstractTeam;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class GuardEntity extends TameableEntity implements RangedAttackMob, CrossbowUser {
    private static final TrackedData<String> ROLE = DataTracker.registerData(GuardEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<String> SKIN_URL = DataTracker.registerData(GuardEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<String> PATROL_MODE = DataTracker.registerData(GuardEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<String> SHIFT = DataTracker.registerData(GuardEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<Integer> SUSPICION = DataTracker.registerData(GuardEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final double DETECTION_RANGE = 16.0D;
    private static final double DETECTION_RANGE_SQ = DETECTION_RANGE * DETECTION_RANGE;
    private static final float ARREST_HEALTH_THRESHOLD = 6.0F; // 3 hearts
    private static final String ENEMY_TAG = "guard_enemy";
    private static final String FRIEND_TAG = "guard_friend";
    private static final int WAYPOINT_COUNT = 4;
    private static final int BASE_SUSPICION_GAIN = 4;
    private static final int MOVEMENT_SUSPICION_BONUS = 4;
    private static final int SNEAK_SUSPICION_BONUS = 2;
    private static final int NIGHT_SUSPICION_BONUS = 3;
    private static final int ESCALATION_THRESHOLD = 60;
    private static final int WARNING_THRESHOLD = 30;
    private static final int BASE_PATROL_COOLDOWN = 80;
    private static final int PATROL_COOLDOWN_VARIANCE = 40;
    private static final double RANGED_SWITCH_RANGE_SQ = 144.0D;
    private static final double MELEE_SWITCH_RANGE_SQ = 16.0D;
    private static final double BACKUP_RANGE = 16.0D;
    private static final double ARREST_RANGE_SQ = 16.0D;
    private static final int MEMORY_TICKS_PRIMARY = 200;
    private static final int MEMORY_TICKS_FALLBACK = 120;
    private static final int DEFAULT_TERRITORY_RADIUS = 16;

    private BlockPos territoryCenter;
    private int territoryRadius = DEFAULT_TERRITORY_RADIUS;
    private BlockPos lastKnownTarget;
    private int memoryTicks;
    private boolean warnedTarget;
    private int patrolIndex;

    public GuardEntity(EntityType<? extends TameableEntity> entityType, World world) {
        super(entityType, world);
        this.setTamed(false);
    }

    public static DefaultAttributeContainer.Builder createGuardAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.35D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 4.0D);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new SitGoal(this));
        this.goalSelector.add(2, new GuardRangedAttackGoal(this, GuardRole.ARCHER, 20, 16.0f));
        this.goalSelector.add(3, new GuardRangedAttackGoal(this, GuardRole.CROSSBOW, 40, 14.0f));
        this.goalSelector.add(4, new MeleeAttackGoal(this, 1.3D, true));
        this.goalSelector.add(5, new FollowOwnerGoal(this, 1.1D, 6.0f, 2.0f, false));
        this.goalSelector.add(6, new GuardPatrolGoal(this));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(8, new LookAroundGoal(this));

        this.targetSelector.add(1, new TrackOwnerAttackerGoal(this));
        this.targetSelector.add(2, new AttackWithOwnerGoal(this));
        this.targetSelector.add(3, new RevengeGoal(this));
        this.targetSelector.add(4, new ActiveTargetGoal<>(this, MobEntity.class, 10, true, false, this::shouldTargetMob));
    }

    private boolean shouldTargetMob(LivingEntity entity) {
        if (this.getOwner() == null) {
            return false;
        }
        if (!this.isOnDuty() || !this.isWithinTerritory(entity)) {
            return false;
        }
        return entity.getAttacker() == this.getOwner() || this.getOwner().getAttacker() == entity;
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(ROLE, GuardRole.SWORD.getId());
        this.dataTracker.startTracking(SKIN_URL, "");
        this.dataTracker.startTracking(PATROL_MODE, PatrolMode.POST.getId());
        this.dataTracker.startTracking(SHIFT, ShiftSchedule.ALWAYS.getId());
        this.dataTracker.startTracking(SUSPICION, 0);
    }

    public GuardRole getGuardRole() {
        return GuardRole.fromId(this.dataTracker.get(ROLE));
    }

    public void setGuardRole(GuardRole role) {
        this.dataTracker.set(ROLE, role.getId());
        this.equipForRole(role);
    }

    public String getSkinUrl() {
        return this.dataTracker.get(SKIN_URL);
    }

    public void setSkinUrl(String url) {
        this.dataTracker.set(SKIN_URL, url == null ? "" : url);
    }

    public PatrolMode getPatrolMode() {
        return PatrolMode.fromId(this.dataTracker.get(PATROL_MODE));
    }

    public void setPatrolMode(PatrolMode mode) {
        this.dataTracker.set(PATROL_MODE, mode.getId());
        this.patrolIndex = 0;
    }

    public ShiftSchedule getShiftSchedule() {
        return ShiftSchedule.fromId(this.dataTracker.get(SHIFT));
    }

    public void setShiftSchedule(ShiftSchedule schedule) {
        this.dataTracker.set(SHIFT, schedule.getId());
    }

    public int getSuspicion() {
        return this.dataTracker.get(SUSPICION);
    }

    private void setSuspicion(int value) {
        this.dataTracker.set(SUSPICION, MathHelper.clamp(value, 0, 100));
        if (this.getSuspicion() < 10) {
            this.warnedTarget = false;
        }
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        Item item = stack.getItem();

        if (!this.isTamed() && item == Items.BREAD) {
            if (!player.getAbilities().creativeMode) {
                stack.decrement(1);
            }
            this.setOwner(player);
            this.navigation.stop();
            this.setTarget(null);
            this.getWorld().sendEntityStatus(this, (byte) 7);
            this.playSound(SoundEvents.ENTITY_VILLAGER_YES, 1.0f, 1.0f);
            return ActionResult.SUCCESS;
        }

        if (this.isOwner(player) && player.shouldCancelInteraction()) {
            this.setSitting(!this.isSitting());
            this.navigation.stop();
            return ActionResult.SUCCESS;
        }

        if (this.isOwner(player)) {
            if (item == Items.PAPER && stack.hasCustomName()) {
                this.setSkinUrl(stack.getName().getString());
                if (!player.getWorld().isClient) {
                    player.sendMessage(Text.literal("Guard skin set to " + this.getSkinUrl()), true);
                }
                if (!player.getAbilities().creativeMode) {
                    stack.decrement(1);
                }
                return ActionResult.SUCCESS;
            }

            if (item == Items.COMPASS) {
                PatrolMode nextMode = switch (this.getPatrolMode()) {
                    case POST -> PatrolMode.WAYPOINT;
                    case WAYPOINT -> PatrolMode.RANDOM;
                    default -> PatrolMode.POST;
                };
                this.setPatrolMode(nextMode);
                this.setTerritoryCenter(this.getBlockPos());
                if (!player.getWorld().isClient) {
                    player.sendMessage(Text.literal("Patrol mode set to " + nextMode.getDisplayName()), true);
                }
                return ActionResult.SUCCESS;
            }

            if (item == Items.CLOCK) {
                ShiftSchedule next = switch (this.getShiftSchedule()) {
                    case DAY -> ShiftSchedule.NIGHT;
                    case NIGHT -> ShiftSchedule.ALWAYS;
                    default -> ShiftSchedule.DAY;
                };
                this.setShiftSchedule(next);
                if (!player.getWorld().isClient) {
                    player.sendMessage(Text.literal("Shift set to " + next.getDisplayName()), true);
                }
                return ActionResult.SUCCESS;
            }

            GuardRole desiredRole = this.roleFromItem(item);
            if (desiredRole != null) {
                this.setGuardRole(desiredRole);
                this.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 0.6f, 1.0f);
                return ActionResult.SUCCESS;
            }
        }

        return super.interactMob(player, hand);
    }

    private GuardRole roleFromItem(Item item) {
        if (item == Items.BOW) {
            return GuardRole.ARCHER;
        }
        if (item == Items.CROSSBOW) {
            return GuardRole.CROSSBOW;
        }
        if (item == Items.IRON_AXE || item == Items.NETHERITE_AXE || item == Items.DIAMOND_AXE) {
            return GuardRole.MACE;
        }
        if (item == Items.IRON_SWORD || item == Items.NETHERITE_SWORD || item == Items.DIAMOND_SWORD) {
            return GuardRole.SWORD;
        }
        return null;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient) {
            this.ensureTerritoryCenter();
            this.updateDutyCycle();
            this.handlePerception();
            this.updateMemoryNavigation();
            ensureEquipment();
        }
    }

    private void ensureEquipment() {
        GuardRole role = this.getGuardRole();
        Item main = this.getMainHandStack().getItem();
        Item expected = getRoleItem(role).getItem();
        if (main != expected) {
            this.equipForRole(role);
        }
    }

    private void ensureTerritoryCenter() {
        if (this.territoryCenter == null) {
            this.territoryCenter = this.getBlockPos();
        }
    }

    private ItemStack getRoleItem(GuardRole role) {
        return switch (role) {
            case ARCHER -> new ItemStack(Items.BOW);
            case CROSSBOW -> new ItemStack(Items.CROSSBOW);
            case MACE -> new ItemStack(Items.IRON_AXE);
            default -> new ItemStack(Items.IRON_SWORD);
        };
    }

    public BlockPos getTerritoryCenter() {
        if (this.territoryCenter == null) {
            this.territoryCenter = this.getBlockPos();
        }
        return this.territoryCenter;
    }

    public void setTerritoryCenter(BlockPos pos) {
        this.territoryCenter = pos;
    }

    private boolean isWithinTerritory(Entity entity) {
        return entity.getBlockPos().isWithinDistance(this.getTerritoryCenter(), this.territoryRadius);
    }

    private void updateDutyCycle() {
        if (!this.isOnDuty()) {
            if (!this.isSitting()) {
                this.setSitting(true);
            }
            this.setTarget(null);
            this.getNavigation().stop();
            if (!this.getBlockPos().isWithinDistance(this.getTerritoryCenter(), 2.5)) {
                this.getNavigation().startMovingTo(this.getTerritoryCenter().getX() + 0.5, this.getTerritoryCenter().getY(), this.getTerritoryCenter().getZ() + 0.5, 1.0D);
            }
            this.decaySuspicion();
            return;
        }

        if (this.isSitting() && this.getTarget() == null) {
            this.setSitting(false);
        }
    }

    private void handlePerception() {
        if (this.getWorld().isClient || this.age % 10 != 0) {
            return;
        }

        if (!this.isOnDuty()) {
            this.decaySuspicion();
            return;
        }

        this.decaySuspicion();

        List<PlayerEntity> players = this.getWorld().getPlayers(player -> player.squaredDistanceTo(this) <= DETECTION_RANGE_SQ);
        for (PlayerEntity player : players) {
            this.evaluatePlayer(player);
        }

        LivingEntity target = this.getTarget();
        if (target != null) {
            this.adjustRoleForTarget(target);
            this.lastKnownTarget = target.getBlockPos();
            this.memoryTicks = MEMORY_TICKS_PRIMARY;
            if (target instanceof PlayerEntity player) {
                this.attemptArrest(player);
            }
        }
    }

    private void evaluatePlayer(PlayerEntity player) {
        if (!player.isAlive() || player.isSpectator() || player.isCreative()) {
            return;
        }
        if (!this.isWithinTerritory(player)) {
            return;
        }
        Set<String> tags = player.getScoreboardTags();
        if (tags.contains(ENEMY_TAG)) {
            this.increaseSuspicion(40, player);
            return;
        }
        if (this.isWhitelisted(player)) {
            return;
        }

        int gain = BASE_SUSPICION_GAIN;
        if (player.isSprinting() || player.isSwimming()) {
            gain += MOVEMENT_SUSPICION_BONUS;
        }
        if (player.isSneaking()) {
            gain += SNEAK_SUSPICION_BONUS;
        }
        if (!this.getWorld().isDay()) {
            gain += NIGHT_SUSPICION_BONUS;
        }
        this.increaseSuspicion(gain, player);
    }

    private void increaseSuspicion(int amount, LivingEntity source) {
        if (source instanceof PlayerEntity player && this.isWhitelisted(player)) {
            return;
        }
        this.setSuspicion(this.getSuspicion() + amount);
        if (source != null) {
            this.lastKnownTarget = source.getBlockPos();
            this.memoryTicks = Math.max(this.memoryTicks, MEMORY_TICKS_FALLBACK);
        }
        if (this.getSuspicion() >= ESCALATION_THRESHOLD && this.getTarget() == null && source != null) {
            this.escalateToTarget(source);
        } else if (this.getSuspicion() >= WARNING_THRESHOLD && !this.warnedTarget && source != null) {
            this.warnEntity(source);
        }
    }

    private void decaySuspicion() {
        if (this.getSuspicion() > 0) {
            this.setSuspicion(this.getSuspicion() - 1);
        }
    }

    private void warnEntity(LivingEntity entity) {
        this.warnedTarget = true;
        this.playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        if (entity instanceof PlayerEntity player && !player.getWorld().isClient) {
            player.sendMessage(Text.literal("Guard: Halt! State your business."), true);
        }
    }

    private void escalateToTarget(LivingEntity source) {
        if (source == null || !source.isAlive()) {
            return;
        }
        this.setTarget(source);
        this.raiseAlarm();
        this.callForBackup(source);
    }

    private void raiseAlarm() {
        this.getWorld().playSound(null, this.getBlockPos(), SoundEvents.BLOCK_BELL_USE, this.getSoundCategory(), 2.0f, 1.0f);
    }

    private void callForBackup(Entity threat) {
        if (!(threat instanceof LivingEntity living)) {
            return;
        }
        List<GuardEntity> allies = this.getWorld().getEntitiesByClass(GuardEntity.class, this.getBoundingBox().expand(BACKUP_RANGE), guard -> guard != this && guard.isOnDuty());
        for (GuardEntity ally : allies) {
            if (ally.getTarget() == null && ally.isWithinTerritory(threat)) {
                ally.setTarget(living);
            }
        }
    }

    private void updateMemoryNavigation() {
        if (this.getTarget() != null || this.lastKnownTarget == null || this.isSitting()) {
            return;
        }

        if (this.memoryTicks > 0) {
            if (!this.getNavigation().isFollowingPath()) {
                this.getNavigation().startMovingTo(this.lastKnownTarget.getX() + 0.5, this.lastKnownTarget.getY(), this.lastKnownTarget.getZ() + 0.5, 1.05D);
            }
            this.memoryTicks--;
        } else {
            this.lastKnownTarget = null;
        }
    }

    private boolean isWhitelisted(PlayerEntity player) {
        if (this.isOwner(player)) {
            return true;
        }
        Set<String> tags = player.getScoreboardTags();
        if (tags.contains(FRIEND_TAG)) {
            return true;
        }
        if (this.getOwner() instanceof PlayerEntity owner) {
            AbstractTeam ownerTeam = owner.getScoreboardTeam();
            AbstractTeam playerTeam = player.getScoreboardTeam();
            if (ownerTeam != null && ownerTeam.equals(playerTeam)) {
                return true;
            }
        }
        return this.isWearingUniform(player);
    }

    private boolean isWearingUniform(PlayerEntity player) {
        return player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.IRON_CHESTPLATE)
                || player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.CHAINMAIL_CHESTPLATE);
    }

    private void attemptArrest(PlayerEntity player) {
        if (this.getWorld().isClient) {
            return;
        }
        if (this.squaredDistanceTo(player) > ARREST_RANGE_SQ) {
            return;
        }
        if (player.getHealth() <= ARREST_HEALTH_THRESHOLD) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, 1));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 120, 0));
            player.sendMessage(Text.literal("You are under arrest. Drop your weapons!"), true);
        }
    }

    private void adjustRoleForTarget(LivingEntity target) {
        if (target == null) {
            return;
        }
        double distanceSq = this.squaredDistanceTo(target);
        GuardRole role = this.getGuardRole();
        if (distanceSq > RANGED_SWITCH_RANGE_SQ && role == GuardRole.SWORD) {
            this.setGuardRole(GuardRole.CROSSBOW);
        } else if (distanceSq < MELEE_SWITCH_RANGE_SQ && (role == GuardRole.ARCHER || role == GuardRole.CROSSBOW)) {
            this.setGuardRole(GuardRole.SWORD);
        }
    }

    private boolean isOnDuty() {
        return this.getShiftSchedule().isOnDuty(this.getWorld());
    }

    private void equipForRole(GuardRole role) {
        this.equipStack(EquipmentSlot.MAINHAND, this.getRoleItem(role));
        ItemStack offhand = switch (role) {
            case ARCHER, CROSSBOW -> new ItemStack(Items.SHIELD);
            default -> ItemStack.EMPTY;
        };
        this.equipStack(EquipmentSlot.OFFHAND, offhand);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        }
        boolean result = super.damage(source, amount);
        if (result && !this.getWorld().isClient) {
            Entity attacker = source.getAttacker();
            if (attacker instanceof LivingEntity living) {
                this.increaseSuspicion(40, living);
                this.setTarget(living);
            }
            if (source.isExplosive()) {
                this.raiseAlarm();
                this.callForBackup(attacker);
            }
        }
        return result;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putString("GuardRole", this.getGuardRole().getId());
        nbt.putString("SkinUrl", this.getSkinUrl());
        nbt.putString("PatrolMode", this.getPatrolMode().getId());
        nbt.putString("Shift", this.getShiftSchedule().getId());
        BlockPos center = this.getTerritoryCenter();
        nbt.putInt("TerritoryX", center.getX());
        nbt.putInt("TerritoryY", center.getY());
        nbt.putInt("TerritoryZ", center.getZ());
        nbt.putBoolean("HasTerritoryCenter", true);
        nbt.putInt("TerritoryRadius", this.territoryRadius);
        nbt.putInt("Suspicion", this.getSuspicion());
    }

    private boolean hasStoredTerritoryCenter(NbtCompound nbt) {
        if (nbt.contains("HasTerritoryCenter")) {
            return nbt.getBoolean("HasTerritoryCenter");
        }
        return nbt.contains("TerritoryX") || nbt.contains("TerritoryY") || nbt.contains("TerritoryZ");
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.setGuardRole(GuardRole.fromId(nbt.getString("GuardRole")));
        this.setSkinUrl(nbt.getString("SkinUrl"));
        this.setPatrolMode(PatrolMode.fromId(nbt.getString("PatrolMode")));
        this.setShiftSchedule(ShiftSchedule.fromId(nbt.getString("Shift")));
        int x = nbt.getInt("TerritoryX");
        int y = nbt.getInt("TerritoryY");
        int z = nbt.getInt("TerritoryZ");
        boolean hasCenter = this.hasStoredTerritoryCenter(nbt);
        if (hasCenter) {
            this.territoryCenter = new BlockPos(x, y, z);
        }
        this.territoryRadius = nbt.contains("TerritoryRadius") ? nbt.getInt("TerritoryRadius") : this.territoryRadius;
        this.setSuspicion(nbt.getInt("Suspicion"));
    }

    @Override
    public boolean canBeLeashedBy(PlayerEntity player) {
        return true;
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public void attack(LivingEntity target, float pullProgress) {
        ItemStack projectileStack = new ItemStack(Items.ARROW);
        ItemStack weapon = this.getMainHandStack();
        PersistentProjectileEntity arrow = ProjectileUtil.createArrowProjectile(this, projectileStack, pullProgress);
        if (arrow == null) {
            return;
        }

        double d = target.getX() - this.getX();
        double e = target.getBodyY(0.3333333333333333D) - arrow.getY();
        double f = target.getZ() - this.getZ();
        double g = Math.sqrt(d * d + f * f);
        arrow.setVelocity(d, e + g * 0.2F, f, 1.6F, 14 - this.getWorld().getDifficulty().getId() * 4);
        this.playSound(SoundEvents.ENTITY_SKELETON_SHOOT, 1.0F, 1.0F);
        this.getWorld().spawnEntity(arrow);
        if (weapon.isOf(Items.CROSSBOW)) {
            weapon.damage(1, this, entity -> entity.sendToolBreakStatus(Hand.MAIN_HAND));
        }
    }

    @Override
    public void shoot(LivingEntity target, ItemStack crossbow, ProjectileEntity projectile, float multiShotSpray) {
        this.shoot(this, target, projectile, multiShotSpray, 1.6f);
    }

    @Override
    public void postShoot() {
    }

    @Override
    public void setCharging(boolean charging) {
    }

    @Override
    public EntityView method_48926() {
        return this.getWorld();
    }

    @Override
    public void setTarget(LivingEntity target) {
        LivingEntity previous = this.getTarget();
        super.setTarget(target);
        if (target != null) {
            this.setSitting(false);
            this.lastKnownTarget = target.getBlockPos();
            this.memoryTicks = MEMORY_TICKS_PRIMARY;
            this.warnedTarget = false;
        } else if (previous != null) {
            this.lastKnownTarget = previous.getBlockPos();
            this.memoryTicks = MEMORY_TICKS_FALLBACK;
        }
    }

    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity mate) {
        GuardEntity child = ModEntities.GUARD.create(world);
        if (child != null && this.getOwnerUuid() != null) {
            child.setOwnerUuid(this.getOwnerUuid());
            child.setTamed(true);
            child.setGuardRole(this.getGuardRole());
        }
        return child;
    }

    private int randomTerritoryOffset() {
        return MathHelper.nextInt(this.getRandom(), -this.territoryRadius, this.territoryRadius);
    }

    private BlockPos getNextPatrolPos() {
        PatrolMode mode = this.getPatrolMode();
        BlockPos center = this.getTerritoryCenter();
        return switch (mode) {
            case POST -> center;
            case RANDOM -> center.add(this.randomTerritoryOffset(), 0, this.randomTerritoryOffset());
            case WAYPOINT -> {
                int index = this.patrolIndex % WAYPOINT_COUNT;
                this.patrolIndex = (this.patrolIndex + 1) % WAYPOINT_COUNT;
                yield switch (index) {
                    case 0 -> center.add(this.territoryRadius / 2, 0, 0);
                    case 1 -> center.add(0, 0, this.territoryRadius / 2);
                    case 2 -> center.add(-this.territoryRadius / 2, 0, 0);
                    default -> center.add(0, 0, -this.territoryRadius / 2);
                };
            }
        };
    }

    static class GuardPatrolGoal extends Goal {
        private final GuardEntity guard;
        private int cooldown;

        GuardPatrolGoal(GuardEntity guard) {
            this.guard = guard;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            return this.guard.getTarget() == null && !this.guard.isSitting() && this.guard.isOnDuty();
        }

        @Override
        public boolean shouldContinue() {
            return this.guard.getTarget() == null && this.guard.isOnDuty();
        }

        @Override
        public void tick() {
            if (this.cooldown > 0) {
                this.cooldown--;
                return;
            }
            BlockPos next = this.guard.getNextPatrolPos();
            if (next != null) {
                this.guard.getNavigation().startMovingTo(next.getX() + 0.5, next.getY(), next.getZ() + 0.5, 1.05D);
                this.guard.getLookControl().lookAt(next.getX() + 0.5, next.getY(), next.getZ() + 0.5);
            }
            this.cooldown = BASE_PATROL_COOLDOWN + this.guard.getRandom().nextInt(PATROL_COOLDOWN_VARIANCE);
        }
    }

    static class GuardRangedAttackGoal extends Goal {
        private final GuardEntity guard;
        private final GuardRole role;
        private final double speed;
        private final float maxRangeSq;
        private final int baseCooldown;
        private int cooldown;

        public GuardRangedAttackGoal(GuardEntity guard, GuardRole role, int baseCooldown, float range) {
            this.guard = guard;
            this.role = role;
            this.baseCooldown = baseCooldown;
            this.speed = 1.2D;
            this.maxRangeSq = range * range;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            LivingEntity target = this.guard.getTarget();
            return this.guard.getGuardRole() == this.role && !this.guard.isSitting() && target != null && target.isAlive() && this.guard.isOnDuty();
        }

        @Override
        public boolean shouldContinue() {
            LivingEntity target = this.guard.getTarget();
            return this.guard.getGuardRole() == this.role && !this.guard.isSitting() && target != null && target.isAlive() && this.guard.isOnDuty();
        }

        @Override
        public void stop() {
            this.cooldown = 0;
        }

        @Override
        public void tick() {
            LivingEntity target = this.guard.getTarget();
            if (target == null) {
                return;
            }

            double distanceSq = this.guard.squaredDistanceTo(target);
            boolean canSee = this.guard.getVisibilityCache().canSee(target);
            this.guard.getLookControl().lookAt(target, 30.0f, 30.0f);

            if (distanceSq > (double) this.maxRangeSq || !canSee) {
                this.guard.getNavigation().startMovingTo(target, this.speed);
            } else {
                this.guard.getNavigation().stop();
            }

            if (cooldown > 0) {
                cooldown--;
            }

            if (canSee && distanceSq <= (double) this.maxRangeSq && cooldown <= 0) {
                this.guard.attack(target, 1.0f);
                this.cooldown = this.baseCooldown;
            }
        }
    }
}
