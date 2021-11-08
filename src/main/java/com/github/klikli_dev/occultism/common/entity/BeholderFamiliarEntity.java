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

import java.util.List;

import com.github.klikli_dev.occultism.common.advancement.FamiliarTrigger;
import com.github.klikli_dev.occultism.registry.OccultismAdvancements;
import com.github.klikli_dev.occultism.util.FamiliarUtil;
import com.google.common.collect.ImmutableList;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ILivingEntityData;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.World;

public class BeholderFamiliarEntity extends FamiliarEntity {

    private static final float DEG_30 = FamiliarUtil.toRads(30);

    private Eye[] eyes = new Eye[] { new Eye(-0.2, 1.3, -0.2), new Eye(0.24, 1.3, -0.23), new Eye(0.28, 1.3, 0.23),
            new Eye(-0.15, 1.3, 0.27) };

    public BeholderFamiliarEntity(EntityType<? extends BeholderFamiliarEntity> type, World worldIn) {
        super(type, worldIn);
    }

    @Override
    public Iterable<EffectInstance> getFamiliarEffects() {
        return ImmutableList.of();
    }

    @Override
    public ILivingEntityData finalizeSpawn(IServerWorld pLevel, DifficultyInstance pDifficulty, SpawnReason pReason,
            ILivingEntityData pSpawnData, CompoundNBT pDataTag) {
        this.setBeard(this.getRandom().nextBoolean());
        this.setSpikes(this.getRandom().nextBoolean());
        this.setTongue(this.getRandom().nextDouble() < 0.1);
        return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
    }

    @Override
    public void setFamiliarOwner(LivingEntity owner) {
        if (this.hasTongue())
            OccultismAdvancements.FAMILIAR.trigger(owner, FamiliarTrigger.Type.RARE_VARIANT);
        super.setFamiliarOwner(owner);
    }

    public boolean hasBeard() {
        return this.hasVariant(0);
    }

    private void setBeard(boolean b) {
        this.setVariant(0, b);
    }

    public boolean hasSpikes() {
        return this.hasVariant(1);
    }

    private void setSpikes(boolean b) {
        this.setVariant(1, b);
    }

    public boolean hasTongue() {
        return this.hasVariant(2);
    }

    private void setTongue(boolean b) {
        this.setVariant(2, b);
    }

    @Override
    public void tick() {
        super.tick();

        if (level.isClientSide) {
            for (Eye eye : eyes)
                eye.tick();
        } else {
            this.yRot = 90;
        }
        this.yBodyRot = this.yRot;
    }

    public Vector2f getEyeRot(float partialTicks, int i) {
        return eyes[i].getEyeRot(partialTicks);
    }

    private Vector3d lerpVec(float value, Vector3d start, Vector3d stop) {
        return new Vector3d(MathHelper.lerp(value, start.x, stop.x), MathHelper.lerp(value, start.y, stop.y),
                MathHelper.lerp(value, start.z, stop.z));
    }

    private class Eye {
        private Vector3d lookPos0, lookPos, pos;
        private EyeTarget eyeTarget;

        private Eye(double x, double y, double z) {
            this.pos = new Vector3d(x, y, z);
            this.init();
        }

        private void init() {
            this.selectEyeTarget();
            Vector3d targetPos = eyeTarget.getEyeTarget();
            if (targetPos != null)
                this.lookPos = targetPos;
            else
                this.lookPos = Vector3d.ZERO;
            this.lookPos0 = this.lookPos;
        }

        private void selectEyeTarget() {
            List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(7),
                    e -> e != BeholderFamiliarEntity.this);
            if (entities.isEmpty()) {
                eyeTarget = new PositionEyeTarget(Vector3d.ZERO.add(5, 0, 0).yRot(getRandom().nextFloat() * 360)
                        .xRot(getRandom().nextFloat() * 360).add(position()));
            } else {
                eyeTarget = new EntityEyeTarget(entities.get(getRandom().nextInt(entities.size())).getId());
            }
        }

        private boolean needNewEyeTarget() {
            Vector3d targetPos = eyeTarget.getEyeTarget();
            return targetPos == null || targetPos.distanceToSqr(position()) > 15 * 15
                    || getRandom().nextDouble() < 0.01;
        }

        private void tick() {
            this.lookPos0 = this.lookPos;
            if (needNewEyeTarget())
                selectEyeTarget();

            Vector3d targetPos = eyeTarget.getEyeTarget();
            if (targetPos != null)
                this.lookPos = lerpVec(0.2f, this.lookPos, targetPos);
        }

        private Vector2f getEyeRot(float partialTicks) {
            float bodyRot = FamiliarUtil.toRads(MathHelper.rotLerp(partialTicks, yBodyRotO, yBodyRot));

            Vector3d direction = position().add(pos.yRot(-bodyRot)).vectorTo(lerpVec(partialTicks, lookPos0, lookPos));
            double yRot = MathHelper.atan2(direction.z, direction.x) + FamiliarUtil.toRads(-90) - bodyRot;
            double xRot = direction.normalize().y;
            return new Vector2f((float) (DEG_30 - DEG_30 * xRot), (float) yRot);
        }

        private abstract class EyeTarget {
            abstract protected Vector3d getEyeTarget();
        }

        private class EntityEyeTarget extends EyeTarget {
            int entityId;

            private EntityEyeTarget(int entityId) {
                this.entityId = entityId;
            }

            @Override
            protected Vector3d getEyeTarget() {
                Entity e = level.getEntity(entityId);
                return e == null ? null : e.getEyePosition(0);
            }
        }

        private class PositionEyeTarget extends EyeTarget {
            Vector3d position;

            private PositionEyeTarget(Vector3d position) {
                this.position = position;
            }

            @Override
            protected Vector3d getEyeTarget() {
                return position;
            }
        }
    }
}
