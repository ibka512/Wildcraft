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
        public float yaw, roll; public int fans, battery, energy, kinds, active; public long fuel, cooldown; public boolean working; public int preview = -1, ghost, ghostEnergy;
    }
    public MachineRenderer(EntityRendererProvider.Context context) { super(context); shadowRadius = .7F; }
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(MachineEntity body, State state, float partial) {
        super.extractRenderState(body, state, partial);
        state.active=body.activeMask();state.fuel=0;state.cooldown=0;boolean stabilized=false;
        for(int n=0;n<6;n++){state.fuel|=(long)body.rocketFuel(n)<<(7*n);state.cooldown|=(long)body.springCooldown(n)<<(6*n);if(body.kind(n)==7&&body.active(n))stabilized=true;}
        state.roll=stabilized?0:(float)Math.clamp(MachineNodes.rotate(body.getDeltaMovement(),-body.getYRot()).x*18,-6,6);
        state.kinds = body.kinds(); state.yaw = body.getYRot(); state.fans = body.fanMask(); state.battery = body.batteryNode();
        state.energy = body.energy(); state.working = body.working();
        state.preview = MachinePresentation.selectedNode(body);
        state.ghost = MachinePresentation.canPreview(body, state.preview)
                ? MechanicsContent.kind(net.minecraft.client.Minecraft.getInstance().player.getMainHandItem()) : 0;
        state.ghostEnergy = state.ghost == 2 ? dev.wildcraft.energy.Batteries.energy(net.minecraft.client.Minecraft.getInstance().player.getMainHandItem()) : 0;
    }
    @Override public void submit(State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
        poses.pushPose(); poses.rotate(Axis.YP, (float)Math.toRadians(-state.yaw));poses.rotate(Axis.ZP,(float)Math.toRadians(state.roll));
        box(poses, collector, state.lightCoords, -.66F,.05F,-.66F,.66F,.56F,.66F,0xFF475A5B);
        box(poses, collector, state.lightCoords, -.70F,.54F,-.70F,.70F,.65F,.70F,0xFFB99361);
        box(poses, collector, state.lightCoords, -.38F,.652F,.2F,.38F,.67F,.5F,0xFF77BCB4);
        for (int n = 0; n < MachineNodes.COUNT; n++) {
            var at = MachineNodes.point(n); poses.pushPose(); poses.translate(at);
            orient(poses, n);
            boolean ghost = n == state.preview && state.ghost > 0;
            boolean fan = (state.fans & 1 << n) != 0 || ghost && state.ghost == 1, battery = state.battery == n || ghost && state.ghost == 2;
            int kind = ghost ? state.ghost : state.kinds >>> (n * 4) & 15;
            int colour = n == state.preview ? state.ghost > 0 ? 0xFF93E8C0 : 0xFFDEAB7A : battery || fan ? 0xFFCCAD72 : 0xFF70A5A5;
            box(poses, collector, state.lightCoords, -.13F,-.13F,-.04F,.13F,.13F,.04F,colour);
            if (battery) {
                box(poses, collector, state.lightCoords,-.2F,-.23F,.04F,.2F,.23F,.15F,ghost ? 0x885DD7B7 : 0xFF4C615A);
                box(poses, collector, state.lightCoords,-.14F,-.15F,.151F,.14F,-.15F+.3F*(ghost ? state.ghostEnergy : state.energy)/1000F,.16F,ghost ? 0x885DD7B7 : 0xFF90D3A9);
            }
            if (kind == 3) box(poses, collector, state.lightCoords,-.6F,-.08F,.04F,.6F,.08F,.55F,ghost ? 0x885DD7B7 : 0xFFCBB98E);
            if (kind == 6) {
                box(poses, collector, state.lightCoords,-.25F,-.25F,.04F,.25F,.25F,.24F,ghost ? 0x885DD7B7 : 0xFF354547);
                poses.pushPose();poses.translate(0,0,.25);poses.rotate(Axis.ZP,(state.active & 1<<n)!=0&&!ghost?state.ageInTicks*.4F:0);
                box(poses, collector, state.lightCoords,-.2F,-.025F,0,.2F,.025F,.02F,ghost ? 0x885DD7B7 : 0xFFBB9561);poses.popPose();
            }
            int material=ghost?0x885DD7B7:0xFFB99867;
            if(kind==4){box(poses,collector,state.lightCoords,-.13F,-.13F,.03F,.13F,.13F,.5F,material);
                box(poses,collector,state.lightCoords,-.09F,-.09F,.01F,.09F,.09F,.12F,ghost?material:0xFF6B918C);
                if(!ghost && (state.active & 1<<n)!=0 && (state.fuel >>> (7*n)&127)>0)box(poses,collector,state.lightCoords,-.08F,-.08F,.5F,.08F,.08F,.86F,0xDDFFD58B);}
            if(kind==5){float stretch=!ghost && (state.cooldown >>> (6*n)&63)>30?.32F:.12F;
                box(poses,collector,state.lightCoords,-.22F,-.22F,.04F,.22F,.22F,.1F,material);
                for(int i=0;i<3;i++)box(poses,collector,state.lightCoords,-.14F,-.14F,.12F+i*stretch/3,.14F,.14F,.16F+i*stretch/3,ghost?material:0xFF758D8A);
                box(poses,collector,state.lightCoords,-.22F,-.22F,.18F+stretch,.22F,.22F,.24F+stretch,material);}
            if(kind==7){box(poses,collector,state.lightCoords,-.18F,-.26F,.04F,.18F,.26F,.22F,material);
                box(poses,collector,state.lightCoords,-.08F,-.08F,.23F,.08F,.08F,.25F,ghost?material:(state.active & 1<<n)!=0?0xFF8DD5B7:0xFF607775);}
            if(kind==8){box(poses,collector,state.lightCoords,-.32F,-.18F,.04F,.32F,.18F,.38F,ghost?material:0xFFD3C59D);
                box(poses,collector,state.lightCoords,-.04F,-.2F,.02F,.04F,.2F,.4F,material);}
            if (fan) {
                box(poses, collector, state.lightCoords,-.29F,-.29F,.04F,.29F,.29F,.1F,ghost ? 0x885DD7B7 : 0xFF826D4B);
                poses.translate(0,0,.11);
                poses.rotate(Axis.ZP, (state.active & 1<<n)!=0 && !ghost ? state.ageInTicks * .7F : 0);
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
