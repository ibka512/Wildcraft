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
import net.minecraft.world.item.Items;
import net.minecraft.tags.ItemTags;

/** Renders the current sanitized native item model on the player's animated body. */
public final class BackEquipmentLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    public static final RenderStateDataKey<ItemStackRenderState[]> ITEMS = RenderStateDataKey.create(() -> "wildcraft:back_equipment");
    private record Profile(float x,float y,float z,float angle,float scale) { }
    private static final RenderStateDataKey<Profile[]> PROFILES = RenderStateDataKey.create(() -> "wildcraft:back_profiles");
    public BackEquipmentLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) { super(parent); }
    public static void initialize() {
        BackTransitions.initialize();
        LivingEntityRenderLayerRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
            if (renderer instanceof AvatarRenderer<?> avatar) helper.register(new BackEquipmentLayer(avatar));
        });
    }
    public static void extract(Player player, AvatarRenderState state) {
        Minecraft client = Minecraft.getInstance();
        BackTransitions.extract(player, state);
        // Cape occupies these same anchors. Until a waist layout exists it has display priority.
        if (player == client.player && client.options.getCameraType().isFirstPerson()
                || state.isSpectator || state.isInvisible || state.showCape && state.skin.cape() != null) {
            state.setData(ITEMS, null); return;
        }
        ItemStackRenderState[] items = new ItemStackRenderState[3];
        Profile[] profiles = new Profile[3];
        BackEquipment.View view = BackEquipment.view(player);
        for (int c = 0; c < 3; c++) {
            // The server compares actual owned references, not equal-looking item models.
            // A second identical sword in hand must not hide the recorded sword on the back.
            ItemStack stack = view.item(c);
            profiles[c] = c == 1 ? new Profile(0,6.8F,8.2F,0,1/.55F)
                    : c == 2 ? (stack.is(Items.CROSSBOW) ? new Profile(6.8F,0,5.6F,35,.9F) : new Profile(6.8F,-1,5.6F,-25,.9F))
                    : stack.is(ItemTags.AXES) ? new Profile(-6.5F,5,4.2F,65,.85F) : new Profile(-6.4F,7,4.2F,-90,.85F);
            items[c] = new ItemStackRenderState();
            client.getItemModelResolver().updateForLiving(items[c], stack, ItemDisplayContext.FIXED, player);
        }
        state.setData(ITEMS, items);
        state.setData(PROFILES, profiles);
    }
    @Override public void submit(PoseStack poses, SubmitNodeCollector collector, int light, AvatarRenderState state, float yaw, float pitch) {
        ItemStackRenderState[] items = state.getData(ITEMS);
        if (items == null || state.isInvisible || state.isSpectator) return;
        for (int c = 0; c < 3; c++) {
            if (items[c].isEmpty()) continue;
            poses.pushPose();
            getParentModel().body.translateAndRotate(poses);
            Profile profile = state.getData(PROFILES)[c];
            poses.translate(profile.x/16,profile.y/16,profile.z/16);
            BackTransitions.back(poses,state,c);
            poses.rotateDegrees(Axis.YP, 180);
            poses.rotateDegrees(Axis.ZP, profile.angle);
            poses.scale(profile.scale, profile.scale, profile.scale);
            var center = items[c].getModelBoundingBox().getCenter();
            poses.translate(-center.x,-center.y,-center.z);
            items[c].submit(poses, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poses.popPose();
        }
    }
}
