/*
 * MIT License
 *
 * Copyright 2021 vemerion
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package com.github.klikli_dev.occultism.common.entity;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FollowMobGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class DevilFamiliarEntity extends FamiliarEntity {

    private static final EntityDataAccessor<Boolean> LOLLIPOP = SynchedEntityData.defineId(DevilFamiliarEntity.class,
            EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> NOSE = SynchedEntityData.defineId(DevilFamiliarEntity.class,
            EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> EARS = SynchedEntityData.defineId(DevilFamiliarEntity.class,
            EntityDataSerializers.BOOLEAN);

    private final float heightOffset;

    public DevilFamiliarEntity(EntityType<? extends DevilFamiliarEntity> type, Level level) {
        super(type, level);
        this.heightOffset = this.getRandom().nextFloat() * 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return FamiliarEntity.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 1f);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {
        this.setLollipop(this.getRandom().nextDouble() < 0.1);
        this.setNose(this.getRandom().nextDouble() < 0.5);
        this.setEars(this.getRandom().nextDouble() < 0.5);
        return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new SitGoal(this));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8));
        this.goalSelector.addGoal(3, new FollowOwnerGoal(this, 1, 3, 1));
        this.goalSelector.addGoal(4, new FireBreathGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new FollowMobGoal(this, 1, 3, 7));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level.isClientSide && this.swinging) {
            Vec3 direction = Vec3.directionFromRotation(this.getRotationVector()).scale(0.6);
            for (int i = 0; i < 5; i++) {
                Vec3 pos = this.position().add(direction.x + (this.getRandom().nextFloat() - 0.5f) * 0.7,
                        1.5 + (this.getRandom().nextFloat() - 0.5f) * 0.7, direction.z + (this.getRandom().nextFloat() - 0.5f) * 0.7);
                this.level.addParticle(ParticleTypes.FLAME, pos.x, pos.y, pos.z, direction.x * 0.25, 0, direction.z * 0.25);
            }
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(LOLLIPOP, false);
        this.entityData.define(NOSE, false);
        this.entityData.define(EARS, false);
    }

    public float getAnimationHeight(float partialTicks) {
        return Mth.cos((this.tickCount + this.heightOffset + partialTicks) / 3.5f);
    }

    public boolean hasLollipop() {
        return this.entityData.get(LOLLIPOP);
    }

    private void setLollipop(boolean b) {
        this.entityData.set(LOLLIPOP, b);
    }

    public boolean hasNose() {
        return this.entityData.get(NOSE);
    }

    private void setNose(boolean b) {
        this.entityData.set(NOSE, b);
    }

    public boolean hasEars() {
        return this.entityData.get(EARS);
    }

    private void setEars(boolean b) {
        this.entityData.set(EARS, b);
    }


    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.setLollipop(compound.getBoolean("hasLollipop"));
        this.setNose(compound.getBoolean("hasNose"));
        this.setEars(compound.getBoolean("hasEars"));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("hasLollipop", this.hasLollipop());
        compound.putBoolean("hasNose", this.hasNose());
        compound.putBoolean("hasEars", this.hasEars());
    }

    @Override
    public Iterable<MobEffectInstance> getFamiliarEffects() {
        if (this.isEffectEnabled()) {
            return ImmutableList.of(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 300, 0, false, false));
        }
        return ImmutableList.of();
    }

    private static class FireBreathGoal extends Goal {

        private static final int MAX_COOLDOWN = 20 * 5;

        private final DevilFamiliarEntity devil;
        private int cooldown = MAX_COOLDOWN;

        public FireBreathGoal(DevilFamiliarEntity devil) {
            this.devil = devil;
        }

        @Override
        public boolean canUse() {
            return this.cooldown-- < 0 && this.devil.getFamiliarOwner() instanceof Player && !this.getNearbyEnemies().isEmpty();
        }

        private List<Entity> getNearbyEnemies() {
            LivingEntity owner = this.devil.getFamiliarOwner();
            LivingEntity revenge = owner.getLastHurtByMob();
            LivingEntity target = owner.getLastHurtMob();
            List<Entity> enemies = new ArrayList<>();
            if (this.isClose(revenge))
                enemies.add(revenge);
            if (this.isClose(target))
                enemies.add(target);
            return enemies;
        }

        private boolean isClose(LivingEntity e) {
            return e != null && e.distanceToSqr(this.devil) < 5;
        }

        @Override
        public void start() {
            for (Entity e : this.getNearbyEnemies()) {
                e.hurt(DamageSource.playerAttack((Player) this.devil.getFamiliarOwner()), 4);
            }
            this.cooldown = MAX_COOLDOWN;
            this.devil.swing(InteractionHand.MAIN_HAND.MAIN_HAND);
        }

        @Override
        public void stop() {
            this.cooldown = MAX_COOLDOWN;
        }
    }
}