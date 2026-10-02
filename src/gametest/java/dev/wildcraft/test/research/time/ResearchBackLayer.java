package dev.wildcraft.test.research.time;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;

/** Only an R1 anchor proof; not a saved equipment system. */
public final class ResearchBackLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    public static volatile boolean enabled;
    public static final Identifier POST_EFFECT = Identifier.fromNamespaceAndPath("wildcraft-test", "focus_probe");
    public static final RenderStateDataKey<ItemStackRenderState[]> ITEMS = RenderStateDataKey.create(() -> "wildcraft-test:anchor");
    public ResearchBackLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) { super(parent); }

    public static void initialize() {
        LivingEntityRenderLayerRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
            if (renderer instanceof AvatarRenderer<?> avatar) helper.register(new ResearchBackLayer(avatar));
        });
    }

    public static void extract(Player player, AvatarRenderState state) {
        if (!enabled) { state.setData(ITEMS, null); return; }
        ItemStackRenderState[] items = new ItemStackRenderState[3];
        for (int i = 0; i < 3; i++) {
            items[i] = new ItemStackRenderState();
            Minecraft.getInstance().getItemModelResolver().updateForLiving(items[i], player.getInventory().getItem(i + 1), ItemDisplayContext.FIXED, player);
        }
        state.setData(ITEMS, items);
    }

    @Override public void submit(PoseStack poses, SubmitNodeCollector collector, int light, AvatarRenderState state, float yaw, float pitch) {
        ItemStackRenderState[] items = state.getData(ITEMS);
        if (items == null || state.isInvisible || state.isSpectator) return;
        for (int i = 0; i < 3; i++) {
            poses.pushPose();
            getParentModel().body.translateAndRotate(poses);
            poses.translate(i == 0 ? -0.08 : i == 2 ? 0.10 : 0, 0.35, i == 1 ? 0.33 : 0.22);
            poses.rotateDegrees(Axis.YP, 180);
            poses.rotateDegrees(Axis.ZP, i == 0 ? -35 : i == 2 ? 35 : 0);
            poses.scale(0.75F, 0.75F, 0.75F);
            items[i].submit(poses, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poses.popPose();
        }
    }
}
