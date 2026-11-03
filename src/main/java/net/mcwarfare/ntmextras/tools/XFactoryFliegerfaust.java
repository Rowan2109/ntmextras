// SPDX-FileCopyrightText: HbmMods (The Bobcat), contributors to Hbm's Nuclear Tech Mod, 2026 MC Warfare
// SPDX-License-Identifier: LGPL-3.0-only

package net.mcwarfare.ntmextras.tools;

import com.hbm.client.ClientPlayerAccess;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.Crosshair;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.GunState;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.factory.Lego;
import com.hbm.items.weapon.sedna.factory.XFactoryRocket;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
import com.hbm.packet.PacketWire;
import com.hbm.packet.toclient.MuzzleFlashPayload;
import com.hbm.render.anim.AnimationEnums.GunAnimation;
import com.hbm.render.anim.BusAnimation;
import com.hbm.render.anim.BusAnimationKeyframe.IType;
import com.hbm.render.anim.BusAnimationSequence;
import com.hbm.sound.EntitySounds;
import com.hbm.sound.ModSounds;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** The nine-barrel micro rocket launcher from XFactoryRocket of the 1.7.10 mod. */
public final class XFactoryFliegerfaust {

    public static final BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_MICRO_EXPLODE =
            (bullet, mop) -> {
                if (mop instanceof EntityHitResult entityHit
                        && bullet.tickCount < 3
                        && entityHit.getEntity() == bullet.getThrower()) return;
                Lego.tinyExplode(bullet, mop, 1.5F);
                bullet.discard();
            };

    public static BulletConfig rocket_ff;


    public static final BiConsumer<ItemStack, LambdaContext> LAMBDA_FLIEGERFAUST_SECONDARY =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                Player player = ctx.getPlayer();
                Receiver rec = ctx.config().getReceivers(stack)[0];
                int index = ctx.configIndex();
                GunState state = ItemGunBaseNT.getState(stack, index);

                if (state == GunState.IDLE) {

                    boolean soundPlayed = false;

                    for (int i = 0; i < 9; i++) {
                        if (rec.getCanFire(stack).apply(stack, ctx)) {
                            rec.getOnFire(stack).accept(stack, ctx);

                            if (rec.getFireSound(stack) != null && !soundPlayed) {
                                EntitySounds.play(
                                        entity,
                                        entity.getX(),
                                        entity.getY(),
                                        entity.getZ(),
                                        rec.getFireSound(stack).get(),
                                        SoundSource.PLAYERS,
                                        rec.getFireVolume(stack),
                                        rec.getFirePitch(stack));
                                soundPlayed = true;
                            }
                        } else {

                            if (rec.getDoesDryFire(stack)) {
                                ItemGunBaseNT.playAnimation(
                                        player, stack, GunAnimation.CYCLE_DRY, index);
                                ItemGunBaseNT.setState(
                                        stack,
                                        index,
                                        rec.getRefireAfterDry(stack)
                                                ? GunState.COOLDOWN
                                                : GunState.DRAWING);
                                ItemGunBaseNT.setTimer(
                                        stack, index, rec.getDelayAfterDryFire(stack));
                            }
                        }
                    }

                    if (soundPlayed) {
                        ItemGunBaseNT.setState(stack, index, GunState.COOLDOWN);
                        ItemGunBaseNT.setTimer(stack, index, rec.getDelayAfterFire(stack));
                    }
                }

                if (state == GunState.RELOADING) {
                    ItemGunBaseNT.setReloadCancel(stack, true);
                }
            };

    public static final BiConsumer<ItemStack, LambdaContext> ORCHESTRA_FLIEGERFAUST =
            (stack, ctx) -> {
                LivingEntity entity = ctx.entity();
                if (entity.level().isClientSide()) return;
                GunAnimation type = ItemGunBaseNT.getLastAnim(stack, ctx.configIndex());
                int timer = ItemGunBaseNT.getAnimTimer(stack, ctx.configIndex());

                if (type == GunAnimation.CYCLE) {
                    if (timer == 0) {
                        PacketWire.sendToAllAround(
                                new MuzzleFlashPayload(entity),
                                new TargetPoint(
                                        (ServerLevel) entity.level(),
                                        entity.getX(),
                                        entity.getY(),
                                        entity.getZ(),
                                        100D));
                    }
                }
                if (type == GunAnimation.RELOAD) {
                    if (timer == 20) play(entity, ModSounds.GUN_BOLT_OPEN.get(), 1F, 0.85F);
                    if (timer == 62) play(entity, ModSounds.GUN_CANISTER_INSERT.get(), 1F, 1F);
                    if (timer == 64) play(entity, ModSounds.GUN_BOLT_CLOSE.get(), 1F, 0.85F);
                }
                if (type == GunAnimation.JAMMED) {
                    if (timer == 25) play(entity, ModSounds.GUN_BOLT_OPEN.get(), 1F, 0.85F);
                    if (timer == 32) play(entity, ModSounds.GUN_BOLT_CLOSE.get(), 1F, 0.85F);
                }
            };

    @SuppressWarnings("incomplete-switch")
    public static final BiFunction<ItemStack, GunAnimation, BusAnimation>
            LAMBDA_FLIEGERFAUST_ANIMS =
                    (stack, type) -> {
                        boolean empty =
                                ((ItemGunBaseNT) stack.getItem())
                                                .getConfig(stack, 0)
                                                .getReceivers(stack)[0]
                                                .getMagazine(stack)
                                                .getAmount(
                                                        stack,
                                                        ClientPlayerAccess.player().getInventory())
                                        <= 0;
                        switch (type) {
                            case EQUIP:
                                return new BusAnimation()
                                        .addBus(
                                                "EQUIP",
                                                new BusAnimationSequence()
                                                        .addPos(60, 0, 0, 0)
                                                        .addPos(0, 0, 0, 500, IType.SIN_DOWN));
                            case CYCLE:
                                return new BusAnimation()
                                        .addBus(
                                                "RECOIL",
                                                new BusAnimationSequence()
                                                        .addPos(0, 0, -0.125, 50, IType.SIN_DOWN)
                                                        .addPos(0, 0, 0, 150, IType.SIN_FULL));
                            case RELOAD:
                                return new BusAnimation()
                                        .addBus(
                                                "RELOAD",
                                                new BusAnimationSequence()
                                                        .addPos(75, 0, 0, 750, IType.SIN_FULL)
                                                        .hold(2750)
                                                        .addPos(0, 0, 0, 750, IType.SIN_FULL))
                                        .addBus(
                                                "DRUM",
                                                new BusAnimationSequence()
                                                        .hold(1000)
                                                        .addPos(0, 0, -1, 500, IType.SIN_FULL)
                                                        .addPos(0, -4, -1, 500, IType.SIN_UP)
                                                        .hold(500)
                                                        .addPos(0, 0, -1, 500, IType.SIN_DOWN)
                                                        .addPos(0, 0, 0, 250, IType.SIN_UP))
                                        .addBus(
                                                "DRUMROT",
                                                new BusAnimationSequence()
                                                        .hold(1500)
                                                        .addPos(-90, 0, 0, 500, IType.SIN_UP)
                                                        .hold(500)
                                                        .addPos(0, 0, 0, 500, IType.SIN_DOWN))
                                        .addBus(
                                                "RECOIL",
                                                new BusAnimationSequence()
                                                        .hold(3250)
                                                        .addPos(0, 0, 0.25, 150, IType.SIN_DOWN)
                                                        .addPos(0, 0, 0, 100, IType.SIN_FULL))
                                        .addBus(
                                                "UNLOADING",
                                                new BusAnimationSequence()
                                                        .setPos(1, 0, 0)
                                                        .hold(2500)
                                                        .setPos(0, 0, 0));
                            case JAMMED:
                                return new BusAnimation()
                                        .addBus(
                                                "RELOAD",
                                                new BusAnimationSequence()
                                                        .hold(500)
                                                        .addPos(75, 0, 0, 500, IType.SIN_FULL)
                                                        .hold(1500)
                                                        .addPos(0, 0, 0, 500, IType.SIN_FULL))
                                        .addBus(
                                                "DRUM",
                                                new BusAnimationSequence()
                                                        .hold(1250)
                                                        .addPos(0, 0, -1, 250, IType.SIN_FULL)
                                                        .addPos(0, 0, 0, 250, IType.SIN_UP))
                                        .addBus(
                                                "RECOIL",
                                                new BusAnimationSequence()
                                                        .hold(1750)
                                                        .addPos(0, 0, 0.25, 150, IType.SIN_DOWN)
                                                        .addPos(0, 0, 0, 100, IType.SIN_FULL))
                                        .addBus(
                                                "UNLOADING",
                                                new BusAnimationSequence()
                                                        .setPos(1, 0, 0)
                                                        .hold(1750)
                                                        .setPos(0, 0, 0));
                            case INSPECT:
                                return new BusAnimation()
                                        .addBus(
                                                "RELOAD",
                                                new BusAnimationSequence()
                                                        .addPos(75, 0, 0, 750, IType.SIN_FULL)
                                                        .hold(500)
                                                        .addPos(0, 0, 0, 750, IType.SIN_FULL))
                                        .addBus(
                                                "ROCKET",
                                                new BusAnimationSequence()
                                                        .addPos(0, empty ? -3 : 0, 0, 0));
                        }
                        return null;
                    };

    private XFactoryFliegerfaust() {}

    /** The bullet's id goes over the network, so it is taken at a fixed point of mod loading. */
    static void init() {
        rocket_ff =
                new BulletConfig()
                        .setItem(ToolsContent.AMMO_ROCKET_MICRO::get)
                        .setLife(300)
                        .setSelfDamageDelay(10)
                        .setVel(7F)
                        .setGrav(0D)
                        .setOnEntityHit(null)
                        .setOnRicochet(null)
                        .setOnImpact(LAMBDA_MICRO_EXPLODE);
    }

    static ItemGunBaseNT create(Item.Properties props) {
        return new ItemGunBaseNT(
                WeaponQuality.SPECIAL,
                props,
                new GunConfig()
                        .dura(9 * 25)
                        .draw(7)
                        .inspect(40)
                        .crosshair(Crosshair.L_CIRCUMFLEX)
                        .rec(
                                new Receiver(0)
                                        .dmg(15F)
                                        .spread(0.015F)
                                        .spreadHipfire(0.015F)
                                        .auto(true)
                                        .delay(4)
                                        .reload(85)
                                        .jam(60)
                                        .sound(() -> ModSounds.WEAPON_ROCKET_FLAME.get(), 1.0F, 1.0F)
                                        .mag(new MagazineFullReload(0, 9).addConfigs(rocket_ff))
                                        .offset(1, -0.0625 * 1.5, -0.1875D)
                                        .offsetScoped(1, -0.0625 * 1.5, -0.125D)
                                        .setupStandardFire()
                                        .recoil(XFactoryRocket.LAMBDA_RECOIL_ROCKET))
                        .setupStandardConfiguration()
                        .ps(LAMBDA_FLIEGERFAUST_SECONDARY)
                        .anim(LAMBDA_FLIEGERFAUST_ANIMS)
                        .orchestra(ORCHESTRA_FLIEGERFAUST));
    }

    private static void play(LivingEntity entity, SoundEvent sound, float volume, float pitch) {
        EntitySounds.play(
                entity,
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                sound,
                SoundSource.PLAYERS,
                volume,
                pitch);
    }
}
