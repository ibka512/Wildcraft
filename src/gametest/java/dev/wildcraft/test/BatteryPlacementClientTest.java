package dev.wildcraft.test;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildcraft.client.art.ArtMesh;
import dev.wildcraft.energy.*;
import dev.wildcraft.mixin.client.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;
/** Native bounds versus unchanged authored geometry, followed by actual deferred GPU transforms. */
public final class BatteryPlacementClientTest implements FabricClientGameTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static PlayerSkin skin(PlayerModelType type){for(int i=0;i<100;i++){var s=DefaultPlayerSkin.get(new UUID(0,i));if(s.model()==type)return s;}throw new AssertionError("Native skin");}
    private static void settle(ClientGameTestContext c){long end=System.nanoTime()+1_500_000_000L;c.waitFor(v->System.nanoTime()>=end&&BatteryPoseAudit.gpu>3);}
    @Override public void runTest(ClientGameTestContext c){
        try(var w=c.worldBuilder().create()){
            w.getConnection().waitForChunksRender();w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);p.teleportTo(.5,120,.5);for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)s.overworld().setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());});
            for(var type:new PlayerModelType[]{PlayerModelType.WIDE,PlayerModelType.SLIM})for(var arm:HumanoidArm.values()){
                w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.getInventory().clearContent();p.setMainArm(arm);var item=new ItemStack(EnergyContent.BATTERY);Batteries.charge(item,1000);p.setItemInHand(InteractionHand.MAIN_HAND,item);p.inventoryMenu.broadcastChanges();});
                c.waitFor(v->v.player.getMainArm()==arm&&v.player.getMainHandItem().is(EnergyContent.BATTERY)&&Batteries.energy(v.player.getMainHandItem())==1000);
                c.runOnClient(v->{
                    RigAudit.fixture=skin(type);RigAudit.enabled=true;v.player.setXRot(0);v.player.setYRot(0);
                    for(var context:new ItemDisplayContext[]{ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,ItemDisplayContext.FIRST_PERSON_LEFT_HAND,ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,ItemDisplayContext.THIRD_PERSON_LEFT_HAND,ItemDisplayContext.GROUND,ItemDisplayContext.FIXED}){
                        var state=new net.minecraft.client.renderer.item.ItemStackRenderState();v.getItemModelResolver().updateForLiving(state,v.player.getMainHandItem(),context,v.player);var layer=(FusionLayerAccessor)((FusionStateAccessor)state).wildcraft$layers()[0];
                        var pose=new PoseStack();layer.wildcraft$transform().apply(context.leftHand(),pose.last());pose.mulPose(layer.wildcraft$local());
                        var low=new Vector3f(Float.POSITIVE_INFINITY);var high=new Vector3f(Float.NEGATIVE_INFINITY);ArtMesh.get("battery_item").extents(raw->{var pt=pose.last().pose().transformPosition(new Vector3f(raw));low.min(pt);high.max(pt);},0);
                        var expected=new AABB(low.x,low.y,low.z,high.x,high.y,high.z);var actual=state.getModelBoundingBox();
                        double error=Math.max(actual.getMinPosition().distanceTo(expected.getMinPosition()),actual.getMaxPosition().distanceTo(expected.getMaxPosition()));
                        check(error<1e-5,"Native battery extents duplicate model centering "+context+" expected="+expected+" actual="+actual+" error="+error);
                    }
                    BatteryPoseAudit.begin();v.options.setCameraType(CameraType.FIRST_PERSON);
                });
                settle(c);c.takeScreenshot("battery-correct-hand-FP-"+type+"-"+arm);c.runOnClient(v->{BatteryPoseAudit.begin();v.options.setCameraType(CameraType.THIRD_PERSON_FRONT);});settle(c);c.takeScreenshot("battery-correct-hand-TP-"+type+"-"+arm);
                c.runOnClient(v->{check(BatteryPoseAudit.gpu>3&&BatteryPoseAudit.maxMatrixError<=2e-5F,"Actual battery GPU placement uses original native parent once");BatteryPoseAudit.enabled=false;});
                w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(p.getMainHandItem().getCount()==1&&Batteries.energy(p.getMainHandItem())==1000,"Native real held battery remains unique/full");});
            }
            System.out.println("C7_BATTERY_PLACEMENT_PASS four native skin-arm variants / six contexts each / actual FP-TP GPU / real battery1000 retained");
        }finally{c.runOnClient(v->{BatteryPoseAudit.enabled=BatteryPoseAudit.inBattery=false;RigAudit.enabled=false;RigAudit.fixture=null;});}
    }
}
