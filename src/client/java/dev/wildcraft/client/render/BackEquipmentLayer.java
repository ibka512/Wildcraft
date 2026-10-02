package dev.wildcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.wildcraft.equipment.BackEquipment;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Renders the current sanitized native item model on the player's animated body. */
public final class BackEquipmentLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    public static final RenderStateDataKey<ItemStackRenderState[]> ITEMS = RenderStateDataKey.create(() -> "wildcraft:back_equipment");
    public BackEquipmentLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) { super(parent); }
    public static void initialize() {
        LivingEntityRenderLayerRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
            if (renderer instanceof AvatarRenderer<?> avatar) helper.register(new BackEquipmentLayer(avatar));
        });
    }
    public static void extract(Player player, AvatarRenderState state) {
        Minecraft client = Minecraft.getInstance();
        // Cape occupies these same anchors. Until a waist layout exists it has display priority.
        if (player == client.player && client.options.getCameraType().isFirstPerson()
                || state.isSpectator || state.isInvisible || state.showCape && state.skin.cape() != null) {
            state.setData(ITEMS, null); return;
        }
        ItemStackRenderState[] items = new ItemStackRenderState[3];
        BackEquipment.View view = BackEquipment.view(player);
        for (int c = 0; c < 3; c++) {
            // The server compares actual owned references, not equal-looking item models.
            // A second identical sword in hand must not hide the recorded sword on the back.
            ItemStack stack = view.item(c);
            items[c] = new ItemStackRenderState();
            client.getItemModelResolver().updateForLiving(items[c], stack, ItemDisplayContext.FIXED, player);
        }
        state.setData(ITEMS, items);
    }
    @Override public void submit(PoseStack poses, SubmitNodeCollector collector, int light, AvatarRenderState state, float yaw, float pitch) {
        ItemStackRenderState[] items = state.getData(ITEMS);
        if (items == null || state.isInvisible || state.isSpectator) return;
        for (int c = 0; c < 3; c++) {
            if (items[c].isEmpty()) continue;
            poses.pushPose();
            getParentModel().body.translateAndRotate(poses);
            poses.translate(c == 0 ? -0.08 : c == 2 ? 0.10 : 0, 0.35, c == 1 ? 0.33 : 0.22);
            poses.rotateDegrees(Axis.YP, 180);
            poses.rotateDegrees(Axis.ZP, c == 0 ? -35 : c == 2 ? 35 : 0);
            poses.scale(0.75F, 0.75F, 0.75F);
            items[c].submit(poses, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poses.popPose();
        }
    }
}
