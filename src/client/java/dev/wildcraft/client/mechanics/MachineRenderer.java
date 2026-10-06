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

/** Adopted second-batch geometry; gameplay and node collision remain server-owned. */
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
        state.roll=stabilized||body.onGround()?0:(float)Math.clamp(MachineNodes.rotate(body.getDeltaMovement(),-body.getYRot()).x*18,-6,6);
        state.kinds = body.kinds(); state.yaw = body.getYRot(); state.fans = body.fanMask(); state.battery = body.batteryNode();
        state.energy = body.energy(); state.working = body.working();
        state.preview = MachinePresentation.selectedNode(body);
        state.ghost = MachinePresentation.canPreview(body, state.preview)
                ? MechanicsContent.kind(net.minecraft.client.Minecraft.getInstance().player.getMainHandItem()) : 0;
        state.ghostEnergy = state.ghost == 2 ? dev.wildcraft.energy.Batteries.energy(net.minecraft.client.Minecraft.getInstance().player.getMainHandItem()) : 0;
    }
    @Override public void submit(State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
        poses.pushPose(); poses.rotate(Axis.YP, (float)Math.toRadians(-state.yaw));poses.rotate(Axis.ZP,(float)Math.toRadians(state.roll));
        dev.wildcraft.client.art.ArtMesh.get("machine").submit(poses,collector,state.lightCoords,false,0,false,false,0);
        for(int n=0;n<MachineNodes.COUNT;n++){
            int kind=state.kinds >>> (n*4)&15;
            boolean ghost=n==state.preview&&state.ghost>0;
            if(ghost)kind=state.ghost;if(kind<1||kind>8)continue;
            boolean active=!ghost&&(state.active&1<<n)!=0;int fuel=(int)(state.fuel >>> (7*n)&127),cooldown=(int)(state.cooldown >>> (6*n)&63);
            String mesh=switch(kind){case 1->"fan";case 2->"battery";case 3->"wing";case 4->!ghost&&fuel==0?"spent_rocket":"rocket";case 5->"spring";case 6->"wheel";case 7->"stabilizer";default->"buoyancy";};
            poses.pushPose();poses.translate(partPoint(n,kind));orient(poses,n);
            float angle=active?(kind==1?state.ageInTicks*.7F:kind==6?state.ageInTicks*.4F:0):0;
            dev.wildcraft.client.art.ArtMesh.get(mesh).submit(poses,collector,state.lightCoords,ghost,angle,!ghost&&cooldown>30,active,ghost?state.ghostEnergy:state.energy);
            if(kind==4&&active&&fuel>0)box(poses,collector,state.lightCoords,-.08F,-.08F,.5F,.08F,.08F,.86F,0xDDFFD58B);
            poses.popPose();
        }
        poses.popPose(); super.submit(state, poses, collector, camera);
    }
    /** A visual mounting adapter only; picking, saved nodes and native collision stay unchanged. */
    public static net.minecraft.world.phys.Vec3 partPoint(int node,int kind) {
        return MachineNodes.point(node).add(0,kind==6&&node>=2?-.075:0,0);
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
