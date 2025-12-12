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

import java.util.EnumSet;

public class GuardEntity extends TameableEntity implements RangedAttackMob, CrossbowUser {
    private static final TrackedData<String> ROLE = DataTracker.registerData(GuardEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<String> SKIN_URL = DataTracker.registerData(GuardEntity.class, TrackedDataHandlerRegistry.STRING);

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
        this.goalSelector.add(6, new WanderAroundFarGoal(this, 0.8D));
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
        return entity.getAttacker() == this.getOwner() || this.getOwner().getAttacker() == entity;
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(ROLE, GuardRole.SWORD.getId());
        this.dataTracker.startTracking(SKIN_URL, "");
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

    private ItemStack getRoleItem(GuardRole role) {
        return switch (role) {
            case ARCHER -> new ItemStack(Items.BOW);
            case CROSSBOW -> new ItemStack(Items.CROSSBOW);
            case MACE -> new ItemStack(Items.IRON_AXE);
            default -> new ItemStack(Items.IRON_SWORD);
        };
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
        return super.damage(source, amount);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putString("GuardRole", this.getGuardRole().getId());
        nbt.putString("SkinUrl", this.getSkinUrl());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.setGuardRole(GuardRole.fromId(nbt.getString("GuardRole")));
        this.setSkinUrl(nbt.getString("SkinUrl"));
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
        super.setTarget(target);
        if (target != null) {
            this.setSitting(false);
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
            return this.guard.getGuardRole() == this.role && !this.guard.isSitting() && target != null && target.isAlive();
        }

        @Override
        public boolean shouldContinue() {
            LivingEntity target = this.guard.getTarget();
            return this.guard.getGuardRole() == this.role && !this.guard.isSitting() && target != null && target.isAlive();
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
