package dev.wildcraft.client.mechanics;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.wildcraft.Wildcraft;
import dev.wildcraft.mechanics.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Original cuboid model; coloured material uses the authored 8x8 white texture. */
public final class MachineRenderer extends EntityRenderer<MachineEntity, MachineRenderer.State> {
    public static final class State extends EntityRenderState {
        public float yaw; public int fans, battery, energy; public boolean working; public int preview = -1, ghost, ghostEnergy;
    }
    public MachineRenderer(EntityRendererProvider.Context context) { super(context); shadowRadius = .7F; }
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(MachineEntity body, State state, float partial) {
        super.extractRenderState(body, state, partial);
        state.yaw = body.getYRot(); state.fans = body.fanMask(); state.battery = body.batteryNode();
        state.energy = body.energy(); state.working = body.working();
        state.preview = MachinePresentation.selectedNode(body);
        state.ghost = MachinePresentation.canPreview(body, state.preview)
                ? net.minecraft.client.Minecraft.getInstance().player.getMainHandItem().is(dev.wildcraft.energy.EnergyContent.BATTERY) ? 2 : 1 : 0;
        state.ghostEnergy = state.ghost == 2 ? dev.wildcraft.energy.Batteries.energy(net.minecraft.client.Minecraft.getInstance().player.getMainHandItem()) : 0;
    }
    @Override public void submit(State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
        poses.pushPose(); poses.rotate(Axis.YP, (float)Math.toRadians(-state.yaw));
        box(poses, collector, state.lightCoords, -.66F,.05F,-.66F,.66F,.56F,.66F,0xFF475A5B);
        box(poses, collector, state.lightCoords, -.70F,.54F,-.70F,.70F,.65F,.70F,0xFFB99361);
        box(poses, collector, state.lightCoords, -.38F,.652F,.2F,.38F,.67F,.5F,0xFF77BCB4);
        for (int n = 0; n < MachineNodes.COUNT; n++) {
            var at = MachineNodes.point(n); poses.pushPose(); poses.translate(at);
            orient(poses, n);
            boolean ghost = n == state.preview && state.ghost > 0;
            boolean fan = (state.fans & 1 << n) != 0 || ghost && state.ghost == 1, battery = state.battery == n || ghost && state.ghost == 2;
            int colour = n == state.preview ? state.ghost > 0 ? 0xFF93E8C0 : 0xFFDEAB7A : battery || fan ? 0xFFCCAD72 : 0xFF70A5A5;
            box(poses, collector, state.lightCoords, -.13F,-.13F,-.04F,.13F,.13F,.04F,colour);
            if (battery) {
                box(poses, collector, state.lightCoords,-.2F,-.23F,.04F,.2F,.23F,.15F,ghost ? 0x885DD7B7 : 0xFF4C615A);
                box(poses, collector, state.lightCoords,-.14F,-.15F,.151F,.14F,-.15F+.3F*(ghost ? state.ghostEnergy : state.energy)/1000F,.16F,ghost ? 0x885DD7B7 : 0xFF90D3A9);
            }
            if (fan) {
                box(poses, collector, state.lightCoords,-.29F,-.29F,.04F,.29F,.29F,.1F,ghost ? 0x885DD7B7 : 0xFF826D4B);
                poses.translate(0,0,.11);
                poses.rotate(Axis.ZP, state.working && !ghost ? state.ageInTicks * .7F : 0);
                box(poses, collector, state.lightCoords,-.24F,-.045F,0,.24F,.045F,.04F,ghost ? 0x885DD7B7 : 0xFFA5C0B7);
                box(poses, collector, state.lightCoords,-.045F,-.24F,0,.045F,.24F,.04F,ghost ? 0x885DD7B7 : 0xFFA5C0B7);
                box(poses, collector, state.lightCoords,-.07F,-.07F,.04F,.07F,.07F,.09F,ghost ? 0x885DD7B7 : 0xFF69C5BA);
            }
            poses.popPose();
        }
        poses.popPose(); super.submit(state, poses, collector, camera);
    }
    private static void orient(PoseStack p, int node) {
        switch (node) {
            case 0 -> p.rotate(Axis.XP, -(float)Math.PI/2);
            case 1 -> p.rotate(Axis.XP, (float)Math.PI/2);
            case 2 -> p.rotate(Axis.YP, (float)Math.PI);
            case 4 -> p.rotate(Axis.YP, -(float)Math.PI/2);
            case 5 -> p.rotate(Axis.YP, (float)Math.PI/2);
            default -> { }
        }
    }
    private static void box(PoseStack poses, SubmitNodeCollector collector, int light,
                            float x0,float y0,float z0,float x1,float y1,float z1,int colour) {
        float[][] faces = {
            {x0,y0,z0,x0,y1,z0,x1,y1,z0,x1,y0,z0,0,0,-1},
            {x0,y0,z1,x1,y0,z1,x1,y1,z1,x0,y1,z1,0,0,1},
            {x0,y0,z0,x0,y0,z1,x0,y1,z1,x0,y1,z0,-1,0,0},
            {x1,y0,z0,x1,y1,z0,x1,y1,z1,x1,y0,z1,1,0,0},
            {x0,y0,z0,x1,y0,z0,x1,y0,z1,x0,y0,z1,0,-1,0},
            {x0,y1,z0,x0,y1,z1,x1,y1,z1,x1,y1,z0,0,1,0}
        };
        var texture = Wildcraft.id("textures/entity/machine.png");
        collector.submitCustomGeometry(poses, (colour >>> 24) < 255 ? RenderTypes.entityTranslucent(texture) : RenderTypes.entityCutout(texture),(pose,buffer) -> {
            for (var f : faces) for (int v=0;v<4;v++) buffer.addVertex(pose,f[v*3],f[v*3+1],f[v*3+2])
                    .setColor(colour).setUv(v==0||v==3?0:1,v<2?0:1).setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light).setNormal(pose,f[12],f[13],f[14]);
        });
    }
}
