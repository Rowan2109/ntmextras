// SPDX-FileCopyrightText: HbmMods (The Bobcat), contributors to Hbm's Nuclear Tech Mod, 2026 MC Warfare
// SPDX-License-Identifier: LGPL-3.0-only

package net.mcwarfare.ntmextras.lambda.client;

import com.hbm.render.loader.HFRWavefrontObject;
import java.io.IOException;
import java.io.InputStream;
import net.mcwarfare.ntmextras.NtmExtras;
import net.minecraft.resources.Identifier;

final class LambdaModels {

    static final HFRWavefrontObject LAUNCHPAD = load("machines/launchpad_lambda.obj");
    static final HFRWavefrontObject ROCKET = load("lambda_rocket.obj");
    static final Identifier LAUNCHPAD_TEX =
            Identifier.fromNamespaceAndPath(NtmExtras.ID, "textures/models/machines/launchpad_lambda.png");
    static final Identifier ROCKET_TEX =
            Identifier.fromNamespaceAndPath(NtmExtras.ID, "textures/models/lambda_rocket.png");

    private LambdaModels() {}

    // NTM's own mesh cache only reads its baked .hbm_mesh files, a plain OBJ has to come in as a stream
    private static HFRWavefrontObject load(String path) {
        String resource = "/assets/" + NtmExtras.ID + "/models/" + path;
        try (InputStream input = LambdaModels.class.getResourceAsStream(resource)) {
            if (input == null) throw new IllegalStateException("Missing model " + resource);
            return new HFRWavefrontObject(resource, input);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read model " + resource, e);
        }
    }
}
