package dev.wildcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.player.PlayerModelType;

/** Separate view models; never mutates the shared third-person renderer or player inventory. */
public final class GliderHands {
    public static final RenderStateDataKey<Boolean> OPEN = RenderStateDataKey.create(() -> "wildcraft:glider_hands");
    private static final PlayerModel STANDARD = model(false), SLIM = model(true);
    private GliderHands() { }
    private static PlayerModel model(boolean slim) {
        return new PlayerModel(LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, slim), 64, 64).bakeRoot(), slim);
    }
    public static void submit(PoseStack poses, SubmitNodeCollector collector, AvatarRenderState state) {
        if (state.isInvisible || state.isSpectator) return;
        boolean slim = state.skin.model() == PlayerModelType.SLIM;
        PlayerModel model = slim ? SLIM : STANDARD;
        model.setupAnim(state); GliderPose.apply(model, slim);
        poses.pushPose();
        // Authored eye Y29 and view-model offset Y-6/Z-5; native camera remains untouched.
        poses.translate(0, -11.0F / 16, -5.0F / 16);
        poses.scale(1, -1, 1);
        var skin = RenderTypes.entityTranslucent(state.skin.body().texturePath());
        collector.submitModelPart(model.leftArm, poses, skin, state.lightCoords, OverlayTexture.NO_OVERLAY, null);
        collector.submitModelPart(model.rightArm, poses, skin, state.lightCoords, OverlayTexture.NO_OVERLAY, null);
        ParagliderMesh.submit(poses, collector, state.lightCoords, true);
        poses.popPose();
    }
}
