package dev.wildcraft.test;
import dev.wildcraft.energy.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
/** Settled real-render captures; accelerated world ticks alone do not settle native equip interpolation. */
public final class BatteryViewClientProbe implements FabricClientGameTest {
    private static void settle(ClientGameTestContext c){long until=System.nanoTime()+1_500_000_000L;c.waitFor(v->System.nanoTime()>=until);}
    @Override public void runTest(ClientGameTestContext c){
        try(var w=c.worldBuilder().create()){
            w.getConnection().waitForChunksRender();w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();p.teleportTo(.5,120,.5);for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)s.overworld().setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());var battery=new ItemStack(EnergyContent.BATTERY);Batteries.charge(battery,1000);p.setItemInHand(InteractionHand.MAIN_HAND,battery);p.inventoryMenu.broadcastChanges();});
            c.waitFor(v->v.player.getMainHandItem().is(EnergyContent.BATTERY)&&Batteries.energy(v.player.getMainHandItem())==1000);
            c.runOnClient(v->{v.options.setCameraType(CameraType.FIRST_PERSON);v.player.setXRot(0);});settle(c);c.takeScreenshot("battery-view-settled-first-person");
            c.runOnClient(v->v.options.setCameraType(CameraType.THIRD_PERSON_FRONT));settle(c);c.takeScreenshot("battery-view-settled-third-person");
            c.runOnClient(v->v.gui.setScreen(new InventoryScreen(v.player)));settle(c);c.takeScreenshot("battery-view-settled-inventory");c.runOnClient(v->v.gui.setScreen(null));
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();if(p.getMainHandItem().getCount()!=1||Batteries.energy(p.getMainHandItem())!=1000)throw new AssertionError("Visual observation changed native battery ownership or energy");});
            System.out.println("C7_BATTERY_VIEW_PROBE_COMPLETE actual settled captures; inspect PNG before visual claims");
        }
    }
}
