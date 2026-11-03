// SPDX-FileCopyrightText: HbmMods (The Bobcat), contributors to Hbm's Nuclear Tech Mod, 2026 MC Warfare
// SPDX-License-Identifier: LGPL-3.0-only

package net.mcwarfare.ntmextras.lambda;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.IFluidHandlerMK2;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.api.redstoneoverradio.RORFunctionException;
import com.hbm.blockentity.BlockEntityMachineBase;
import com.hbm.inventory.IGUIProvider;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.ISatChip;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemCassette;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.particle.CoolingTowerParticleOptions;
import com.hbm.saveddata.satellites.SatelliteType;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.ModSounds;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class BlockEntityLaunchpadLambda extends BlockEntityMachineBase
        implements FluidTankAccess,
                IEnergyHandlerMK2,
                IFluidHandlerMK2,
                IGUIProvider,
                IControlReceiver,
                SyncUnitSchema,
                IRORValueProvider,
                IRORInteractive {

    public static final int SLOT_COUNT = 7;
    public static final int SLOT_ROCKET = 0;
    public static final int SLOT_SATELLITE = 1;
    public static final int SLOT_FUEL_IN = 2;
    public static final int SLOT_FUEL_OUT = 3;
    public static final int SLOT_OXIDIZER_IN = 4;
    public static final int SLOT_OXIDIZER_OUT = 5;
    public static final int SLOT_BATTERY = 6;

    public static final int INDEX_DOORS = 0;
    public static final int INDEX_ERECTOR = 1;
    public static final int INDEX_ROTOR = 2;
    public static final int INDEX_CLAMPS = 3;
    public static final int INDEX_PISTONS = 4;

    public static final long MAX_POWER = 1_000_000L;
    public static final long CONSUMPTION = 10_000L;
    public static final int TANK_CAPACITY = 64_000;
    public static final int LAUNCH_FUEL = 24_000;
    public static final int COUNTDOWN_DURATION = 200;

    private static final int[] ACCESSIBLE = {SLOT_ROCKET, SLOT_SATELLITE};
    private static final long UNITS = 0x1fL;

    public long power;
    public final FluidTankNTM fuelTank = new FluidTankNTM(TANK_CAPACITY);
    public final FluidTankNTM oxidizerTank = new FluidTankNTM(TANK_CAPACITY);
    private final FluidTankNTM[] tanks = {fuelTank, oxidizerTank};

    public final float[] positions = new float[5];
    public final float[] prevPositions = new float[5];
    public final float[] speed = new float[5];
    public final float[] target = new float[5];
    private final float[] syncPositions = new float[5];
    private int turnProgress;

    /** True if the rocket has been placed on the silo door, detaches the rendering from the erector */
    public boolean erected;

    /** True if the erector is moving up, placing the rocket on the silo door */
    public boolean erecting;

    public int animationDelay;
    public boolean autolaunch;
    public int countdown;

    private final AudioWrapper[] audios = new AudioWrapper[4];

    public BlockEntityLaunchpadLambda(BlockPos pos, BlockState state) {
        super(LambdaContent.LAUNCHPAD_LAMBDA_BE.get(), pos, state, SLOT_COUNT);
        fuelTank.setTankType(NTMFluids.GASOLINE_LEADED);
        oxidizerTank.setTankType(NTMFluids.PEROXIDE);
    }

    public static boolean needsOrbiter(ItemStack stack) {
        return stack.is(ModItems.SAT_GERALD.get())
                || SatelliteType.fromStack(stack) == SatelliteType.LUNAR_MINER;
    }

    @Override
    public void tickServer() {
        power += ItemEnergyTransfer.extract(this, SLOT_BATTERY, MAX_POWER - power, false);
        boolean filled = fuelTank.loadTank(SLOT_FUEL_IN, SLOT_FUEL_OUT, inventory);
        filled |= oxidizerTank.loadTank(SLOT_OXIDIZER_IN, SLOT_OXIDIZER_OUT, inventory);

        if (!hasRocketLoaded()) {
            erected = false;
            erecting = false;
            countdown = 0;
        }

        if (power >= CONSUMPTION) {
            updateStates();
            move();
            power -= CONSUMPTION;
            filled = true;

            if (autolaunch && erected && canLaunch() && countdown <= 0)
                countdown = COUNTDOWN_DURATION;
        }

        if (filled) setChanged();
        syncUnitsChanged(1, UNITS);
        networkPackNT(300);
    }

    @Override
    public void tickClient() {
        for (int i = 0; i < positions.length; i++) {
            prevPositions[i] = positions[i];
            if (turnProgress > 0) positions[i] += (syncPositions[i] - positions[i]) / turnProgress;
            else positions[i] = syncPositions[i];
        }
        if (turnProgress > 0) turnProgress--;

        if (erected && hasOxidizer()) {
            for (int i = 0; i < 3; i++) {
                CoolingTowerParticleOptions smoke =
                        new CoolingTowerParticleOptions.Builder()
                                .setLift(0F)
                                .setBaseScale(0.5F)
                                .setMaxScale(2F)
                                .setLife(70 + level.getRandom().nextInt(30))
                                .noWind()
                                .alphaMod(2F)
                                .setStrafe(0.075F)
                                .build();
                level.addParticle(
                        smoke,
                        worldPosition.getX() + 0.5D + level.getRandom().nextGaussian() * 0.25D,
                        worldPosition.getY() + 2D,
                        worldPosition.getZ() + 0.5D + level.getRandom().nextGaussian() * 0.25D,
                        0D,
                        0D,
                        0D);
            }
        }

        handleSound(
                0, syncPositions[INDEX_DOORS] > 0F && syncPositions[INDEX_DOORS] < 3F);
        handleSound(1, !finishedMoving(INDEX_ERECTOR) && wasMoving(INDEX_ERECTOR));
        handleSound(2, syncPositions[INDEX_ROTOR] > 0F && syncPositions[INDEX_ROTOR] < 180F);
        handleSound(3, countdown > 0);

        if (wasMoving(INDEX_CLAMPS) && syncPositions[INDEX_CLAMPS] == 90F)
            playClient(ModSounds.DOOR_SLIDING_SEAL_STOP.get(), 12D, 25F, 0.75F);
    }

    private void handleSound(int index, boolean running) {
        if (running) {
            if (audios[index] == null || !audios[index].isPlaying()) {
                if (audios[index] != null) audios[index].stopSound();
                audios[index] = createSound(index);
                if (audios[index] == null) return;
                audios[index].startSound(worldPosition);
            }
            audios[index].keepAlive();
        } else if (audios[index] != null) {
            audios[index].stopSound();
            audios[index] = null;

            if (Math.abs(positions[INDEX_ERECTOR] - prevPositions[INDEX_ERECTOR]) > 5F) return;

            if (index == 0) playClient(ModSounds.DOOR_GARAGE_STOP.get(), 3D, 35F, 1F);
            if (index == 1 && target[INDEX_ERECTOR] == 25F)
                playClient(ModSounds.DOOR_WGH_BIG_STOP.get(), 3D, 25F, 0.5F);
            if (index == 1 && target[INDEX_ERECTOR] != 25F)
                playClient(ModSounds.DOOR_WGH_STOP.get(), 3D, 25F, 1F);
            if (index == 2) playClient(ModSounds.DOOR_WGH_BIG_STOP.get(), 15D, 25F, 0.75F);
        }
    }

    private AudioWrapper createSound(int index) {
        float x = worldPosition.getX() + 0.5F;
        float y = worldPosition.getY() + (index == 2 ? 15F : 3F);
        float z = worldPosition.getZ() + 0.5F;
        return switch (index) {
            case 0 ->
                    AudioSystem.getLoopedSound(
                            ModSounds.DOOR_GARAGE_MOVE.get(), SoundSource.BLOCKS, x, y, z, 2F, 35F,
                            1F, 10);
            case 1 ->
                    AudioSystem.getLoopedSound(
                            ModSounds.DOOR_WGH_START.get(), SoundSource.BLOCKS, x, y, z, 2F, 35F,
                            1F, 10);
            case 2 ->
                    AudioSystem.getLoopedSound(
                            ModSounds.DOOR_WGH_BIG_START.get(), SoundSource.BLOCKS, x, y, z, 2F,
                            35F, 0.75F, 10);
            default ->
                    AudioSystem.getLoopedSound(
                            ModSounds.SIREN_TRACK[ItemCassette.TrackType.SIREN.ordinal()].get(),
                            SoundSource.BLOCKS,
                            x,
                            y,
                            z,
                            10F,
                            50F,
                            1F,
                            20);
        };
    }

    private void playClient(SoundEvent sound, double up, float volume, float pitch) {
        level.playLocalSound(
                worldPosition.getX() + 0.5D,
                worldPosition.getY() + up,
                worldPosition.getZ() + 0.5D,
                sound,
                SoundSource.BLOCKS,
                volume,
                pitch,
                false);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        for (int i = 0; i < audios.length; i++) {
            if (audios[i] != null) {
                audios[i].stopSound();
                audios[i] = null;
            }
        }
    }

    private void updateStates() {
        // rocket is being erected
        if (finishedAllMoving() && erecting && !erected) {
            if (erectorDown() && doorsClosed()) {
                setTarget(INDEX_DOORS, 3F, 3F, 60);
                setTarget(INDEX_PISTONS, 0F, 1F, 1);
                setTarget(INDEX_CLAMPS, 0F, 1F, 1);
                setTarget(INDEX_ROTOR, 180F, 180F, 1);
            } else if (erectorDown() && doorsOpen()) {
                animationDelay = 20;
                setTarget(INDEX_ERECTOR, 27F, 27F, 100);
            } else if (erectorUp() && rotorDown()) {
                animationDelay = 20;
                setTarget(INDEX_ROTOR, 0F, 180F, 60);
                setTarget(INDEX_DOORS, 0F, 3F, 60);
            } else if (erectorUp() && rotorUp() && doorsClosed()) {
                animationDelay = 20;
                setTarget(INDEX_ERECTOR, 25F, 2F, 60);
            } else if (erectorCenter() && rotorUp() && doorsClosed()) {
                erected = true;
            } else {
                // off-script, run everything back
                erecting = false;
            }
        }

        // return erector when countdown hits T-1
        if (erected && countdown <= 20 && countdown > 0) erecting = false;

        // return erector post launch
        if (finishedAllMoving() && !erecting) {
            setTarget(INDEX_DOORS, 0F, 3F, 60);

            if (clampsOn()) {
                setTarget(INDEX_PISTONS, 0.75F, 0.75F, 20);
            } else if (clampsOff() && clampsDown()) {
                animationDelay = 10;
                setTarget(INDEX_CLAMPS, 90F, 90F, 40);
            } else if (clampsUp() && !erectorDown()) {
                animationDelay = 20;
                setTarget(INDEX_ERECTOR, 0F, 25F, 100);
            } else if (erectorDown() && doorsClosed()) {
                if (hasRocketLoaded() && !erected) erecting = true;
            }
        }

        if (erected && countdown > 0) {
            countdown--;
            if (countdown <= 0) {
                if (canLaunch()) liftOff();
                // payload or fuel went missing during the countdown, roll the rocket back
                else erected = false;
            }
        }
    }

    private void liftOff() {
        EntityRocketLambda rocket =
                new EntityRocketLambda(LambdaContent.ROCKET_LAMBDA.get(), level);
        rocket.snapTo(
                worldPosition.getX() + 0.5D,
                worldPosition.getY() + 2D,
                worldPosition.getZ() + 0.5D,
                0F,
                0F);
        rocket.setSat(getItem(SLOT_SATELLITE).copy());
        level.addFreshEntity(rocket);
        level.playSound(
                null, worldPosition, ModSounds.SOYUZ_TAKEOFF.get(), SoundSource.BLOCKS, 100F, 1.1F);

        fuelTank.setFill(fuelTank.getFill() - LAUNCH_FUEL);
        oxidizerTank.setFill(oxidizerTank.getFill() - LAUNCH_FUEL);

        setItem(SLOT_ROCKET, ItemStack.EMPTY);
        setItem(SLOT_SATELLITE, ItemStack.EMPTY);
        erected = false;
        setChanged();
    }

    private void setTarget(int index, float value, float span, int duration) {
        target[index] = value;
        speed[index] = (span <= 0F ? 1F : span) / duration;
    }

    private void move() {
        if (animationDelay > 0) animationDelay--;
        for (int i = 0; i < positions.length; i++) {
            prevPositions[i] = positions[i];
            if (animationDelay > 0) continue;
            if (Math.abs(positions[i] - target[i]) <= speed[i]) positions[i] = target[i];
            else if (positions[i] < target[i]) positions[i] += speed[i];
            else positions[i] -= speed[i];
        }
    }

    public boolean doorsClosed() {
        return positions[INDEX_DOORS] == 0F;
    }

    public boolean doorsOpen() {
        return positions[INDEX_DOORS] == 3F;
    }

    public boolean erectorDown() {
        return positions[INDEX_ERECTOR] == 0F;
    }

    public boolean erectorCenter() {
        return positions[INDEX_ERECTOR] == 25F;
    }

    public boolean erectorUp() {
        return positions[INDEX_ERECTOR] == 27F;
    }

    public boolean rotorUp() {
        return positions[INDEX_ROTOR] == 0F;
    }

    public boolean rotorDown() {
        return positions[INDEX_ROTOR] == 180F;
    }

    public boolean clampsOn() {
        return positions[INDEX_PISTONS] == 0F;
    }

    public boolean clampsOff() {
        return positions[INDEX_PISTONS] == 0.75F;
    }

    public boolean clampsUp() {
        return positions[INDEX_CLAMPS] == 90F;
    }

    public boolean clampsDown() {
        return positions[INDEX_CLAMPS] == 0F;
    }

    public boolean canLaunch() {
        if (!hasRocketLoaded() || !hasAllFuel() || power < CONSUMPTION) return false;
        ItemStack satellite = getItem(SLOT_SATELLITE);
        return !satellite.isEmpty() && canPlaceItem(SLOT_SATELLITE, satellite);
    }

    public boolean hasRocketLoaded() {
        return getItem(SLOT_ROCKET).is(LambdaContent.MISSILE_LAMBDA.get());
    }

    public boolean finishedMoving(int index) {
        return positions[index] == target[index];
    }

    public boolean wasMoving(int index) {
        return positions[index] != prevPositions[index];
    }

    public boolean finishedAllMoving() {
        for (int i = 0; i < positions.length; i++) if (!finishedMoving(i)) return false;
        return true;
    }

    public float getInterpPos(int index, float partialTick) {
        return prevPositions[index] + (positions[index] - prevPositions[index]) * partialTick;
    }

    public boolean hasAllFuel() {
        return hasJetFuel() && hasOxidizer();
    }

    public boolean hasJetFuel() {
        return fuelTank.getFill() >= LAUNCH_FUEL;
    }

    public boolean hasOxidizer() {
        return oxidizerTank.getFill() >= LAUNCH_FUEL;
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[] {
            PREFIX_VALUE + "countdown",
            PREFIX_VALUE + "power",
            PREFIX_VALUE + "fuel",
            PREFIX_VALUE + "oxidizer",
            PREFIX_VALUE + "isauto",
            PREFIX_VALUE + "canlaunch",
            PREFIX_FUNCTION + "launch",
            PREFIX_FUNCTION + "setauto" + NAME_SEPARATOR + "on/off"
        };
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "countdown").equals(name)) return "" + countdown;
        if ((PREFIX_VALUE + "power").equals(name)) return "" + power;
        if ((PREFIX_VALUE + "fuel").equals(name)) return "" + fuelTank.getFill();
        if ((PREFIX_VALUE + "oxidizer").equals(name)) return "" + oxidizerTank.getFill();
        if ((PREFIX_VALUE + "isauto").equals(name)) return autolaunch ? "1" : "0";
        if ((PREFIX_VALUE + "canlaunch").equals(name)) return canLaunch() ? "1" : "0";
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setauto").equals(name) && params.length == 1) {
            autolaunch = parseBool(params[0]);
            setChanged();
        } else if ((PREFIX_FUNCTION + "launch").equals(name)) {
            if (canLaunch()) countdown = COUNTDOWN_DURATION;
        }
        return null;
    }

    private static boolean parseBool(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "1", "true", "on" -> true;
            case "0", "false", "off" -> false;
            default -> throw new RORFunctionException(EX_FORMAT);
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESSIBLE;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_ROCKET -> stack.is(LambdaContent.MISSILE_LAMBDA.get());
            case SLOT_SATELLITE -> stack.getItem() instanceof ISatChip && !needsOrbiter(stack);
            case SLOT_FUEL_IN, SLOT_OXIDIZER_IN -> FluidTankNTM.isFluidContainer(stack);
            case SLOT_BATTERY -> IBatteryItem.isBattery(stack);
            default -> false;
        };
    }

    @Override
    protected double interactionRangeSq() {
        return 24D * 24D;
    }

    @Override
    public boolean hasPermission(Player player) {
        return stillValid(player);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("auto")) {
            autolaunch = data.getBooleanOr("auto", autolaunch);
            setChanged();
        }
        if (data.contains("launch") && canLaunch()) {
            countdown = COUNTDOWN_DURATION;
            setChanged();
        }
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long value) {
        if (power != value) setChanged();
        power = value;
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public int tankCount() {
        return tanks.length;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return tanks[index];
    }

    @Override
    public boolean canReceiveFluid(Fluid type) {
        for (FluidTankNTM tank : tanks) {
            if (tank.joins(type)) return true;
        }
        return false;
    }

    @Override
    public void forEachTank(Consumer<FluidTankNTM> visitor) {
        for (FluidTankNTM tank : tanks) visitor.accept(tank);
    }

    @Override
    public long getDemand(Fluid type, int pressure) {
        if (pressure != 0) return 0;
        long demand = 0;
        for (FluidTankNTM tank : tanks)
            if (tank.accepts(type)) demand += tank.getMaxFill() - tank.getFill();
        return demand;
    }

    @Override
    public long transferFluid(Fluid type, int pressure, long amount) {
        if (pressure != 0) return amount;
        long left = amount;
        for (FluidTankNTM tank : tanks) {
            if (left <= 0) break;
            if (tank.accepts(type))
                left -= tank.fill(type, (int) Math.min(left, Integer.MAX_VALUE), true);
        }
        if (left != amount) setChanged();
        return left;
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | UNITS;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(power);
            case 1 -> fuelTank.packetSerialize(output);
            case 2 -> oxidizerTank.packetSerialize(output);
            case 3 -> {
                output.writeBoolean(erecting);
                output.writeBoolean(erected);
                output.writeBoolean(autolaunch);
                output.writeInt(countdown);
            }
            case 4 -> {
                for (float value : target) output.writeFloat(value);
                for (float position : positions) output.writeFloat(position);
            }
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> power = input.readLong();
            case 1 -> fuelTank.packetDeserialize(input);
            case 2 -> oxidizerTank.packetDeserialize(input);
            case 3 -> {
                erecting = input.readBoolean();
                erected = input.readBoolean();
                autolaunch = input.readBoolean();
                countdown = input.readInt();
            }
            case 4 -> {
                for (int i = 0; i < target.length; i++) target[i] = input.readFloat();
                for (int i = 0; i < syncPositions.length; i++) {
                    float value = input.readFloat();
                    if (syncPositions[i] != value) {
                        syncPositions[i] = value;
                        turnProgress = 3;
                    }
                }
            }
            default -> super.readSyncUnit(unit, input);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        power = input.getLongOr("power", 0L);
        input.child("tank0").ifPresent(fuelTank::deserialize);
        input.child("tank1").ifPresent(oxidizerTank::deserialize);
        loadFloats(input, "positions", positions);
        loadFloats(input, "speed", speed);
        loadFloats(input, "target", target);
        erected = input.getBooleanOr("erected", false);
        erecting = input.getBooleanOr("erecting", false);
        animationDelay = input.getIntOr("animationDelay", 0);
        autolaunch = input.getBooleanOr("autolaunch", false);
        countdown = input.getIntOr("countdown", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        fuelTank.serialize(output.child("tank0"));
        oxidizerTank.serialize(output.child("tank1"));
        saveFloats(output, "positions", positions);
        saveFloats(output, "speed", speed);
        saveFloats(output, "target", target);
        output.putBoolean("erected", erected);
        output.putBoolean("erecting", erecting);
        output.putInt("animationDelay", animationDelay);
        output.putBoolean("autolaunch", autolaunch);
        output.putInt("countdown", countdown);
    }

    private static void loadFloats(ValueInput input, String key, float[] values) {
        input.read(key, Codec.FLOAT.listOf())
                .ifPresent(
                        saved -> {
                            for (int i = 0; i < values.length && i < saved.size(); i++)
                                values[i] = saved.get(i);
                        });
    }

    private static void saveFloats(ValueOutput output, String key, float[] values) {
        List<Float> saved = new ArrayList<>(values.length);
        for (float value : values) saved.add(value);
        output.store(key, Codec.FLOAT.listOf(), saved);
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MenuLaunchpadLambda(containerId, inventory, this);
    }
}
