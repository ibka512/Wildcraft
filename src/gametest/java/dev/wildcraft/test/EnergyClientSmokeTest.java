package dev.wildcraft.test;

import dev.wildcraft.energy.*;
import dev.wildcraft.client.energy.ChargerScreen;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

public final class EnergyClientSmokeTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext c){
        TestWorldSave save;var pos=new BlockPos(3,120,0);
        String language=c.computeOnClient(client -> client.options.languageCode);
        try {
            var reload=c.computeOnClient(client -> {client.options.languageCode="zh_cn";client.getLanguageManager().setSelected("zh_cn");return client.reloadResourcePacks();});c.waitFor(client -> reload.isDone());reload.join();c.waitFor(client -> client.gui.overlay()==null);
            try(TestSingleplayerContext world=c.worldBuilder().create()){
                world.getConnection().waitForChunksRender();world.getServer().runCommand("gamemode survival @p");
                world.getServer().runOnServer(server -> {
                    var p=server.getPlayerList().getPlayers().getFirst();var level=server.overworld();
                    for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)level.setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());
                    p.teleportTo(level,.5,120,.5,Set.of(),0,0,false);p.setNoGravity(true);p.getInventory().setItem(0,new ItemStack(EnergyContent.BATTERY));p.getInventory().setItem(1,new ItemStack(Items.REDSTONE_BLOCK));p.inventoryMenu.broadcastChanges();
                    level.setBlockAndUpdate(pos,EnergyContent.CHARGER.defaultBlockState());level.setBlockAndUpdate(pos.east(),Blocks.REDSTONE_TORCH.defaultBlockState());
                });
                c.waitFor(client -> client.player.getY()>119 && client.player.getMainHandItem().is(EnergyContent.BATTERY) && client.level.getBlockState(pos).is(EnergyContent.CHARGER));
                open(c,pos);c.waitFor(client -> client.gui.screen() instanceof ChargerScreen);
                c.runOnClient(client -> client.gameMode.handleContainerInput(client.player.containerMenu.containerId,28,0,ContainerInput.QUICK_MOVE,client.player));
                c.waitFor(client -> client.player.containerMenu.getSlot(0).getItem().is(EnergyContent.BATTERY));c.waitTicks(5);
                c.waitFor(client -> ((ChargerMenu)client.player.containerMenu).status()==1);
                check(c.computeOnClient(client -> ((ChargerMenu)client.player.containerMenu).energy()==0),"Carried redstone and actual adjacent redstone signal do not charge");c.takeScreenshot("p5-signal-without-energy");
                world.getServer().runOnServer(server -> server.overworld().setBlockAndUpdate(pos.east(),Blocks.REDSTONE_BLOCK.defaultBlockState()));
                c.waitFor(client -> ((ChargerMenu)client.player.containerMenu).energy()>100);
                c.takeScreenshot("p5-charging");c.waitFor(client -> ((ChargerMenu)client.player.containerMenu).energy()==1000);
                c.waitFor(client -> ((ChargerMenu)client.player.containerMenu).status()==3);c.takeScreenshot("p5-full-battery");
                c.runOnClient(client -> client.gameMode.handleContainerInput(client.player.containerMenu.containerId,0,0,ContainerInput.QUICK_MOVE,client.player));c.waitTicks(5);
                c.runOnClient(client -> client.player.closeContainer());
                world.getServer().runOnServer(server -> {
                    var p=server.getPlayerList().getPlayers().getFirst();int slot=-1;for(int i=0;i<36;i++)if(p.getInventory().getItem(i).is(EnergyContent.BATTERY)){slot=i;break;}
                    check(slot>=0,"Charged stack reaches actual server inventory");var battery=p.getInventory().removeItemNoUpdate(slot);
                    check(Batteries.energy(battery)==1000 && Batteries.consume(battery,627) && Batteries.energy(battery)==373,"Actual extracted stack pays a finite cost");
                    p.getInventory().setItem(0,battery);p.getInventory().setSelectedSlot(0);p.inventoryMenu.broadcastChanges();
                    server.overworld().setBlockAndUpdate(pos.east(),Blocks.REDSTONE_TORCH.defaultBlockState());
                });
                c.waitFor(client -> client.gui.screen()==null && Batteries.energy(client.player.getMainHandItem())==373);
                c.takeScreenshot("p5-partly-used-battery");open(c,pos);c.waitFor(client -> client.gui.screen() instanceof ChargerScreen);
                c.runOnClient(client -> client.gameMode.handleContainerInput(client.player.containerMenu.containerId,28,0,ContainerInput.QUICK_MOVE,client.player));c.waitTicks(5);
                c.waitFor(client -> ((ChargerMenu)client.player.containerMenu).energy()==373 && ((ChargerMenu)client.player.containerMenu).status()==1);
                c.runOnClient(client -> client.player.closeContainer());save=world.getWorldSave();
            }
            try(TestSingleplayerContext world=save.open()){
                world.getConnection().waitForChunksRender();open(c,pos);c.waitFor(client -> client.gui.screen() instanceof ChargerScreen);
                c.waitFor(client -> ((ChargerMenu)client.player.containerMenu).status()==1);
                check(c.computeOnClient(client -> ((ChargerMenu)client.player.containerMenu).energy()==373),"Reload and inventory transfer never refill a battery");
                c.takeScreenshot("p5-reloaded-charge");c.runOnClient(client -> client.player.closeContainer());
            }
        } finally {
            var reload=c.computeOnClient(client -> {client.options.languageCode=language;client.getLanguageManager().setSelected(language);return client.reloadResourcePacks();});c.waitFor(client -> reload.isDone());reload.join();
        }
        System.out.println("WILDCRAFT P5 native signal/carried-source rejection, charging/full UI, extracted finite cost, transfer/save/reload passed");
    }
    private static void open(ClientGameTestContext c,BlockPos pos){c.runOnClient(client -> client.gameMode.useItemOn(client.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false)));}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
