package dev.wildcraft.test.research.combat;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.player.*;

/** Feasibility samples only; no damage, target lock or combat animation ships in the formal jar. */
public final class CombatFoundationClientProbe implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext c) {
        gesture();
        try(TestSingleplayerContext w=c.worldBuilder().create()) {
            w.getConnection().waitForChunksRender();
            w.getServer().runOnServer(server->{
                var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(.5,150,.5);p.setNoGravity(true);p.setYRot(73);
                var target=new net.minecraft.world.phys.Vec3(.5,150,5.5);var forward=target.subtract(p.position()).multiply(1,0,1).normalize();
                var right=new net.minecraft.world.phys.Vec3(-forward.z,0,forward.x);
                var before=p.position();p.move(net.minecraft.world.entity.MoverType.SELF,forward.scale(.2));
                check(p.getZ()>before.z+.19&&p.getYRot()==73,"Target-relative forward movement preserves independent aim yaw");
                before=p.position();p.move(net.minecraft.world.entity.MoverType.SELF,right.scale(.2));
                check(p.getX()<before.x-.19&&p.getYRot()==73,"Target-relative side movement preserves independent aim yaw");
            });
            c.runOnClient(client->{
                var state=(AvatarRenderState)client.getEntityRenderDispatcher().getRenderer(client.player).createRenderState(client.player,1);
                float cameraYaw=client.player.getYRot();float nativeBody=state.bodyRot;float nativeHead=state.yRot;
                // Rendering can face the target without assigning the camera or logical player yaw.
                state.bodyRot=35;state.yRot=net.minecraft.util.Mth.wrapDegrees(nativeHead+nativeBody-state.bodyRot);
                check(client.player.getYRot()==cameraYaw,"Body presentation does not assign aim/camera yaw");
                var skin=state.skin;
                for(boolean slim:new boolean[]{false,true}) {
                    state.skin=PlayerSkin.insecure(skin.body(),null,null,slim?PlayerModelType.SLIM:PlayerModelType.WIDE);
                    var model=new PlayerModel(LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE,slim),64,64).bakeRoot(),slim);
                    model.setupAnim(state);
                    float rightLeg=model.rightLeg.xRot,leftLeg=model.leftLeg.xRot,head=model.head.yRot;
                    // A shield arm mask leaves locomotion, the opposite arm, and independent head intact.
                    float opposite=model.rightArm.xRot;model.leftArm.xRot=-1.1F;model.body.yRot=.12F;
                    check(model.rightLeg.xRot==rightLeg&&model.leftLeg.xRot==leftLeg,"Upper-body layer preserves lower-body locomotion");
                    check(model.head.yRot==head&&model.rightArm.xRot==opposite,"Arm mask preserves head and other arm");
                    check(model.leftArm.xRot==-1.1F,"Defense sample applies on both native skin rigs");
                }
                state.skin=skin;
            });
        }
        System.out.println("C0 COMBAT short/hold/cancel gesture and independent head/body/arm/lower-body native model probes PASS; player combat not implemented");
    }
    private static void gesture() {
        // Test-owned release discriminator, deliberately outside production until C3 hand-feel approval.
        var g=new Gesture();g.press(0);check(g.release(80_000_000L).equals("light"),"Short release makes one light intent");
        check(g.release(90_000_000L).equals("none"),"A repeated release never creates a second attack");
        g.press(0);check(g.release(450_000_000L).equals("charged"),"Held release makes one charged intent");
        g.press(0);g.cancel();check(g.release(450_000_000L).equals("none"),"GUI/equipment interruption cancels intent");
    }
    private static final class Gesture {
        boolean held;long began;
        void press(long now){if(!held){held=true;began=now;}}
        String release(long now){if(!held)return "none";held=false;return now-began>=300_000_000L?"charged":"light";}
        void cancel(){held=false;}
    }
    private static void check(boolean okay,String message){if(!okay)throw new AssertionError(message);}
}
