package dev.wildcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildcraft.Wildcraft;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

public final class ParagliderLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    public static final RenderStateDataKey<Boolean> OPEN = RenderStateDataKey.create(() -> "wildcraft:paraglider_open");
    private final CanopyModel canopy = new CanopyModel(geometry().bakeRoot());

    public ParagliderLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer) { super(renderer); }

    public static void initialize() {
        LivingEntityRenderLayerRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
            if (renderer instanceof AvatarRenderer<?> avatar) helper.register(new ParagliderLayer(avatar));
        });
    }

    @Override
    public void submit(PoseStack poses, SubmitNodeCollector collector, int light, AvatarRenderState state, float yaw, float pitch) {
        if (state.getDataOrDefault(OPEN, false) && !state.isInvisible && !state.isSpectator) {
            renderColoredCutoutModel(canopy, Wildcraft.id("textures/entity/paraglider.png"), poses, collector, light, state, -1, 0);
        }
    }

    private static LayerDefinition geometry() {
        MeshDefinition mesh = new MeshDefinition();
        var root = mesh.getRoot();
        root.addOrReplaceChild("canopy", CubeListBuilder.create().texOffs(0, 0).addBox(-8, -23, -10, 16, 1, 20), PartPose.ZERO);
        root.addOrReplaceChild("left", CubeListBuilder.create().texOffs(0, 0).addBox(-12, 0, -10, 12, 1, 20), PartPose.offsetAndRotation(-8, -23, 0, 0, 0, -0.16F));
        root.addOrReplaceChild("right", CubeListBuilder.create().texOffs(0, 0).addBox(0, 0, -10, 12, 1, 20), PartPose.offsetAndRotation(8, -23, 0, 0, 0, 0.16F));
        root.addOrReplaceChild("frame", CubeListBuilder.create().texOffs(0, 32)
                .addBox(-8, -22, -7, 1, 16, 1).addBox(7, -22, -7, 1, 16, 1)
                .addBox(-8, -7, -7, 16, 1, 1), PartPose.ZERO);
        return LayerDefinition.create(mesh, 128, 64);
    }

    private static final class CanopyModel extends EntityModel<AvatarRenderState> {
        CanopyModel(ModelPart root) { super(root); }
    }
}
