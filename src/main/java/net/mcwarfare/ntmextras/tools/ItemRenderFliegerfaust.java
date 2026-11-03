// SPDX-FileCopyrightText: HbmMods (The Bobcat), contributors to Hbm's Nuclear Tech Mod, 2026 MC Warfare
// SPDX-License-Identifier: LGPL-3.0-only

package net.mcwarfare.ntmextras.tools;

import com.hbm.client.model.Meshes;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.render.anim.HbmAnimations;
import com.hbm.render.item.weapon.sedna.ItemRenderWeaponBase;
import com.hbm.render.item.weapon.sedna.RestBody;
import com.hbm.render.loader.HFRWavefrontObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.mcwarfare.ntmextras.NtmExtras;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

public class ItemRenderFliegerfaust extends ItemRenderWeaponBase {

    private static final Identifier MODEL =
            Identifier.fromNamespaceAndPath(NtmExtras.ID, "models/weapons/fliegerfaust.obj");
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(NtmExtras.ID, "textures/models/weapons/fliegerfaust.png");

    private final HFRWavefrontObject model = Meshes.load(MODEL);
    private final RenderType body = RenderTypes.entityCutout(TEXTURE);
    private final int launcher = model.partId("Launcher");
    private final int drum = model.partId("Drum");
    private final int[] rockets =
            model.partIds(
                    "Rocket1", "Rocket2", "Rocket3", "Rocket4", "Rocket5", "Rocket6", "Rocket7",
                    "Rocket8", "Rocket9");

    @Override
    public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);

        float offset = 0.8F;
        standardAimingTransform(
                stack,
                pose,
                -2.75F * offset,
                -2F * offset,
                -1.5F * offset,
                -0.9375,
                -7.75 / 8D,
                -2.5);
    }

    @Override
    public void renderFirstPerson(
            ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light) {

        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        float aimingProgress =
                ItemGunBaseNT.prevAimingProgress
                        + (ItemGunBaseNT.aimingProgress - ItemGunBaseNT.prevAimingProgress)
                                * interp;
        float scale = 1.25F;
        pose.scale(scale, scale, scale);

        double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
        double[] reload = HbmAnimations.getRelevantTransformation("RELOAD");
        double[] drumMove = HbmAnimations.getRelevantTransformation("DRUM");
        double[] drumRot = HbmAnimations.getRelevantTransformation("DRUMROT");
        double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL");
        double[] unloading = HbmAnimations.getRelevantTransformation("UNLOADING");

        int rocketNum =
                unloading[0] > 0
                        ? gun.getConfig(stack, 0)
                                .getReceivers(stack)[0]
                                .getMagazine(stack)
                                .getAmount(stack, null)
                        : rockets.length;

        pose.mulPose(Axis.ZP.rotationDegrees(aimingProgress * -45F));

        pose.translate(0, 0, 1);
        pose.mulPose(Axis.XP.rotationDegrees((float) equip[0]));
        pose.translate(0, 0, -1);

        pose.translate(0, -6, -1);
        pose.mulPose(Axis.XP.rotationDegrees((float) reload[0]));
        pose.translate(0, 6, 1);

        pose.translate(0, 0, recoil[2]);

        submitPart(collector, pose, body, model, launcher, light);

        pose.pushPose();
        pose.translate(0, 0, 2);
        pose.translate(drumMove[0], drumMove[1], drumMove[2]);
        pose.translate(0, 0, -8.5);
        pose.mulPose(Axis.XP.rotationDegrees((float) drumRot[0]));
        pose.translate(0, 0, 8.5);
        submitPart(collector, pose, body, model, drum, light);
        for (int i = 0; i < rocketNum && i < rockets.length; i++) {
            submitPart(collector, pose, body, model, rockets[i], light);
        }
        pose.popPose();

        pose.pushPose();
        pose.translate(0, 0, 8);
        pose.mulPose(Axis.YP.rotationDegrees(90));
        pose.mulPose(Axis.XP.rotationDegrees((float) (90 * gun.shotRand)));
        pose.scale(0.75F, 0.75F, 0.75F);
        renderMuzzleFlash(collector, pose, gun.lastShot[0], 75, 5);
        pose.popPose();
    }

    @Override
    public void restBody(ItemStack stack, RestBody restBody) {
        float offset = 0.8F;
        Matrix4f m = restSetup(0.875F, -2.75F * offset, -2F * offset, -1.5F * offset).scale(1.25F);
        restBody.part(m, model, launcher, TEXTURE);
        Matrix4f d = new Matrix4f(m).translate(0F, 0F, 2F);
        restBody.part(d, model, drum, TEXTURE);
        for (int rocket : rockets) restBody.part(d, model, rocket, TEXTURE);
    }
}
