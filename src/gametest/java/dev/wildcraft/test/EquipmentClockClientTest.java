package dev.wildcraft.test;
import dev.wildcraft.client.render.BackTransitions;
import dev.wildcraft.equipment.BackEquipment;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.CameraType;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
/** Focused actual-time feedback loop; the existing equipment test separately verifies real use registration. */
public final class EquipmentClockClientTest implements FabricClientGameTest {
    private static AvatarRenderState state(net.minecraft.client.Minecraft c){return (AvatarRenderState)c.getEntityRenderDispatcher().getRenderer(c.player).createRenderState(c.player,1);}
    @Override public void runTest(ClientGameTestContext c){
        int old=c.computeOnClient(v->v.options.framerateLimit().get());
        try(TestSingleplayerContext w=c.worldBuilder().create()){
            w.getConnection().waitForChunksRender();w.getServer().runCommand("gamemode survival @p");
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++)s.overworld().setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());p.teleportTo(.5,120,.5);p.getInventory().clearContent();p.getInventory().setItem(0,new ItemStack(Items.IRON_SPEAR));p.getInventory().setSelectedSlot(0);BackEquipment.register(p,p.getMainHandItem());p.inventoryMenu.broadcastChanges();});
            c.waitFor(v->v.player.getY()>119&&v.player.getAttached(BackEquipment.VISUAL)!=null);c.runOnClient(v->v.options.setCameraType(CameraType.FIRST_PERSON));
            w.getServer().runCommand("tick rate 5");
            for(int fps:new int[]{120,10}){
                c.runOnClient(v->{v.options.framerateLimit().set(fps);v.player.getInventory().setSelectedSlot(1);});c.waitFor(v->v.player.getAttached(BackEquipment.VISUAL).melee().owner()==3);c.waitTicks(12);
                long started=System.nanoTime();var timing=new long[]{0,0,0};var firstProgress=new float[]{0};
                c.runOnClient(v->v.player.getInventory().setSelectedSlot(0));
                c.waitFor(v->{long now=System.nanoTime();var owner=v.player.getAttached(BackEquipment.VISUAL).melee().owner();var a=state(v).getData(BackTransitions.SETTLES);if(owner==1&&timing[0]==0)timing[0]=now;if(a!=null&&a[0]!=null&&a[0].draw()&&timing[1]==0){timing[1]=now;firstProgress[0]=a[0].progress();}boolean done=owner==1&&(a==null||a[0]==null);if(done)timing[2]=now;return done;},300);
                double transport=(timing[0]-started)/1e9,visual=(timing[2]-timing[1])/1e9+firstProgress[0]*.34,total=(timing[2]-started)/1e9;
                System.out.println("C6 ACTUAL CLOCK fpsLimit="+fps+" measuredFps="+c.computeOnClient(v->v.getFps())+" nativeOwnership="+transport+" animationDuration="+visual+" total="+total);
                if(timing[1]==0||visual>.55)throw new AssertionError("Actual .34s spear draw must remain <=.55s including 10fps frame quantization; measured "+visual);
            }
            w.getServer().runCommand("tick rate 20");
        }finally{c.runOnClient(v->v.options.framerateLimit().set(old));}
    }
}
