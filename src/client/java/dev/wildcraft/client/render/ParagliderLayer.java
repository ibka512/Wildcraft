package dev.wildcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

public final class ParagliderLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    public static final RenderStateDataKey<Boolean> OPEN = RenderStateDataKey.create(() -> "wildcraft:paraglider_open");
    public ParagliderLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) { super(parent); }
    public static void initialize() {
        LivingEntityRenderLayerRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
            if (renderer instanceof AvatarRenderer<?> avatar) helper.register(new ParagliderLayer(avatar));
        });
    }
    @Override public void submit(PoseStack poses, SubmitNodeCollector collector, int light, AvatarRenderState state, float yaw, float pitch) {
        if (state.getDataOrDefault(OPEN, false) && !state.isInvisible && !state.isSpectator)
            ParagliderMesh.submit(poses, collector, light, false);
    }
}
