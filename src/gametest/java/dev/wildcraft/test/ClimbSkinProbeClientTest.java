package dev.wildcraft.test;
import dev.wildcraft.test.research.animation.SkinProbeLayer;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.*;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.level.block.Blocks;
import java.util.UUID;
/** Source-only wall/skin GPU qualification; no claim that native G or real climb phase has been integrated. */
public final class ClimbSkinProbeClientTest implements FabricClientGameTest {
    private static PlayerSkin skin(PlayerModelType model){for(int i=0;i<100;i++){var s=DefaultPlayerSkin.get(new UUID(0,i));if(s.model()==model)return s;}throw new AssertionError("Native skin fixture");}
    @Override public void runTest(ClientGameTestContext c){
        try(TestSingleplayerContext w=c.worldBuilder().create()){
            w.getConnection().waitForChunksRender();w.getServer().runCommand("gamemode survival @p");w.getServer().runCommand("time set day");
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();for(int x=-5;x<=5;x++)for(int y=119;y<=126;y++)s.overworld().setBlockAndUpdate(new BlockPos(x,y,1),Blocks.STONE.defaultBlockState());p.getInventory().clearContent();p.setNoGravity(true);p.teleportTo(.5,121,.68);p.inventoryMenu.broadcastChanges();});c.waitFor(v->v.player.getY()>120.9);c.getInput().resizeWindow(1280,960);
            c.runOnClient(v->{v.options.setCameraType(CameraType.THIRD_PERSON_BACK);v.options.guiScale().set(2);v.player.setNoGravity(true);v.player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);v.player.setYRot(-35);v.player.setXRot(5);RigAudit.enabled=true;RigAudit.bodyYaw=0F;SkinProbeLayer.enabled=true;SkinProbeLayer.calls=0;});
            for(var model:new PlayerModelType[]{PlayerModelType.WIDE,PlayerModelType.SLIM}){
                c.runOnClient(v->RigAudit.fixture=skin(model));
                for(String clip:new String[]{"climb_enter","climb_up","climb_left","climb_right","climb_idle_up","climb_idle_left","climb_idle_right"})for(float fraction:new float[]{0,.35F,.7F}){
                    c.runOnClient(v->{SkinProbeLayer.clip=clip;SkinProbeLayer.fraction=fraction;});c.waitTicks(2);c.takeScreenshot("climb-source-"+model+"-"+clip+"-"+Math.round(fraction*100));
                    if(c.computeOnClient(v->SkinProbeLayer.calls==0||!SkinProbeLayer.skin.equals(RigAudit.fixture.body().texturePath().toString())))throw new AssertionError("Actual adopted mesh/native skin must reach GPU");
                }
            }
            System.out.println("CLIMB SOURCE GPU 42 actual native skin poses/side observation at wall PASS; real G/displacement/head/blocked/corners NOT integrated or qualified");
        }finally{c.runOnClient(v->{SkinProbeLayer.enabled=false;RigAudit.enabled=false;RigAudit.fixture=null;RigAudit.bodyYaw=null;v.options.setCameraType(CameraType.FIRST_PERSON);});}
    }
}
