// SPDX-FileCopyrightText: HbmMods (The Bobcat), contributors to Hbm's Nuclear Tech Mod, 2026 MC Warfare
// SPDX-License-Identifier: LGPL-3.0-only

package net.mcwarfare.ntmextras.lambda;

import com.hbm.api.environment.Environment;
import com.hbm.client.ClientEffects;
import com.hbm.items.ISatChip;
import com.hbm.saveddata.satellites.Satellite;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class EntityRocketLambda extends Entity {

    private ItemStack payload = ItemStack.EMPTY;
    private double acceleration;

    public EntityRocketLambda(EntityType<? extends EntityRocketLambda> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    public void tick() {
        Vec3 motion = getDeltaMovement();
        if (motion.y < 2.0D) {
            acceleration += 0.00025D;
            motion = new Vec3(motion.x, motion.y + acceleration, motion.z);
            setDeltaMovement(motion);
        }

        xo = xOld = getX();
        yo = yOld = getY();
        zo = zOld = getZ();
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);

        if (level().isClientSide()) spawnExhaust(motion);

        if (getY() > 600 && level() instanceof ServerLevel server) {
            deployPayload(server);
            discard();
        }
    }

    private void spawnExhaust(Vec3 motion) {
        ClientEffects.spawnRocketFlame(
                level(),
                getX(),
                getY() + 1D,
                getZ(),
                1.5F,
                0D,
                -1D + random.nextDouble() * 0.25D,
                0D,
                300 + random.nextInt(50));

        if (tickCount % 10 == 0) {
            float angle = (45 * random.nextInt(8)) * Mth.DEG_TO_RAD;
            double vx = Mth.cos(angle);
            double vz = -Mth.sin(angle);
            ClientEffects.spawnRocketFlameInAir(
                    level(),
                    getX() - vx * 0.5D,
                    getY(),
                    getZ() - vz * 0.5D,
                    1F,
                    vx,
                    motion.y - 0.5D,
                    vz,
                    60 + random.nextInt(20),
                    Environment.particlePressure(level()));
        }
    }

    private void deployPayload(ServerLevel server) {
        if (payload.getItem() instanceof ISatChip) {
            Satellite.orbit(server, payload, ISatChip.getFreqS(payload), getX(), getY(), getZ());
        }
    }

    public void setSat(ItemStack stack) {
        payload = stack == null ? ItemStack.EMPTY : stack;
    }

    public ItemStack getSat() {
        return payload;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 500000;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        acceleration = input.getDoubleOr("acceleration", 0D);
        payload = input.read("payload", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putDouble("acceleration", acceleration);
        output.store("payload", ItemStack.OPTIONAL_CODEC, payload);
    }
}
