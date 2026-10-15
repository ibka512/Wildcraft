package dev.wildcraft.test;
import dev.wildcraft.test.research.animation.SkinProbeLayer;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import java.util.Set;

/** First GPU qualification of adopted continuous skin; no production renderer or gameplay changes. */
public final class AnimationSkinClientTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext c){
        try(TestSingleplayerContext w=c.worldBuilder().create()){
            w.getConnection().waitForChunksRender();w.getServer().runCommand("gamemode survival @p");w.getServer().runCommand("time set day");
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++)s.overworld().setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());p.getInventory().clearContent();p.teleportTo(s.overworld(),.5,120,.5,Set.of(),0,0,false);p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);p.inventoryMenu.broadcastChanges();});
            c.waitFor(v->v.player.getY()>119);c.getInput().resizeWindow(1280,960);c.runOnClient(v->{v.options.setCameraType(CameraType.THIRD_PERSON_FRONT);v.player.setYRot(0);v.player.setXRot(0);v.options.guiScale().set(2);});c.waitTicks(3);c.takeScreenshot("rig-native-before");
            c.runOnClient(v->{SkinProbeLayer.enabled=true;SkinProbeLayer.calls=0;SkinProbeLayer.vertices=0;});
            for(String name:new String[]{"idle_R","idle_L","shield_R","A_R","A_L","shield_A_R"})for(float time:new float[]{0,.35F,.6F,1}){
                c.runOnClient(v->{SkinProbeLayer.clip=name;SkinProbeLayer.fraction=time;});c.waitTicks(3);c.takeScreenshot("rig-"+name+"-"+Math.round(time*100));
                if(c.computeOnClient(v->SkinProbeLayer.calls==0||SkinProbeLayer.vertices==0))throw new AssertionError("Actual skin mesh was not submitted");
            }
            c.runOnClient(v->{if(!SkinProbeLayer.skin.equals(v.player.getSkin().body().texturePath().toString()))throw new AssertionError("Probe must bind actual player skin");SkinProbeLayer.enabled=false;});c.waitTicks(3);c.takeScreenshot("rig-native-after");
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.getInventory().setItem(0,new ItemStack(net.minecraft.world.item.Items.IRON_SWORD));p.getInventory().setSelectedSlot(0);p.inventoryMenu.broadcastChanges();});
            c.waitFor(v->v.player.getMainHandItem().is(net.minecraft.world.item.Items.IRON_SWORD));
            c.runOnClient(v->{SkinProbeLayer.enabled=true;SkinProbeLayer.heldCalls=0;});
            for(String name:new String[]{"idle_R","A_R"})for(float time:new float[]{0,.25F,.4F,.6F,.8F,1}){
                c.runOnClient(v->{SkinProbeLayer.clip=name;SkinProbeLayer.fraction=time;});c.waitTicks(3);c.takeScreenshot("rig-native-sword-"+name+"-"+Math.round(time*100));
                if(c.computeOnClient(v->SkinProbeLayer.heldCalls==0))throw new AssertionError("Actual native sword was not projected");
            }
        }finally{c.runOnClient(v->{SkinProbeLayer.enabled=false;v.options.setCameraType(CameraType.FIRST_PERSON);});}
        System.out.println("CONTINUOUS SKIN GPU native before/after and 24 adopted idle/single-sword poses, actual skin bound PASS; armor/held item projection not yet qualified");
    }
}
