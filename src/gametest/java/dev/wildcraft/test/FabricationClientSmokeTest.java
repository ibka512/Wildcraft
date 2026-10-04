package dev.wildcraft.test;

import dev.wildcraft.fabrication.*;
import dev.wildcraft.client.fabrication.FabricatorScreen;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

/** Real container/button/mining/placement packets and actual disk worlds. */
public final class FabricationClientSmokeTest implements FabricClientGameTest {
    private static final BlockPos POS=new BlockPos(0,120,2);
    @Override public void runTest(ClientGameTestContext c){
        TestWorldSave save;ItemStack[] expected={ItemStack.EMPTY};int[] progress={0};
        String lang=c.computeOnClient(client -> client.options.languageCode);
        var reload=c.computeOnClient(client -> {client.options.languageCode="zh_cn";client.getLanguageManager().setSelected("zh_cn");return client.reloadResourcePacks();});c.waitFor(client -> reload.isDone());reload.join();c.waitFor(client -> client.gui.overlay()==null);
        try{
            try(TestSingleplayerContext world=c.worldBuilder().create()){
                world.getConnection().waitForChunksRender();world.getServer().runCommand("gamemode survival @p");
                world.getServer().runOnServer(server -> {var level=server.overworld();var p=server.getPlayerList().getPlayers().getFirst();for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)level.setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());p.teleportTo(level,.5,120,.5,Set.of(),0,30,false);p.getInventory().clearContent();p.getInventory().setItem(0,new ItemStack(FabricationContent.ITEM));p.getInventory().setItem(1,new ItemStack(Items.COPPER_INGOT,8));p.getInventory().setItem(2,new ItemStack(Items.REDSTONE,4));p.getInventory().setItem(3,new ItemStack(Items.IRON_PICKAXE));p.getInventory().setSelectedSlot(0);p.inventoryMenu.broadcastChanges();});
                c.runOnClient(client -> client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));c.waitFor(client -> client.player.getMainHandItem().is(FabricationContent.ITEM));place(c);
                c.waitFor(client -> client.level.getBlockState(POS).is(FabricationContent.BLOCK)&&client.player.getMainHandItem().isEmpty());open(c);c.takeScreenshot("p8-empty-fabricator");
                shift(c,31);shift(c,32);c.waitFor(client -> ((FabricatorMenu)client.player.containerMenu).status()==0);
                c.runOnClient(client -> {var screen=(FabricatorScreen)client.gui.screen();screen.children().stream().filter(w -> w instanceof net.minecraft.client.gui.components.Button).map(w -> (net.minecraft.client.gui.components.Button)w).findFirst().orElseThrow().onPress(new net.minecraft.client.input.KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN,0,0));});
                c.waitFor(client -> ((FabricatorMenu)client.player.containerMenu).status()==2);
                c.runOnClient(client -> {client.gameMode.handleInventoryButtonClick(client.player.containerMenu.containerId,0);client.gameMode.handleInventoryButtonClick(client.player.containerMenu.containerId,0);});c.waitTicks(3);
                world.getServer().runOnServer(server -> {var b=(FabricatorEntity)server.overworld().getBlockEntity(POS);expected[0]=b.pendingResult();progress[0]=b.progress();check(b.hasJob()&&b.getItem(0).getCount()==4&&b.getItem(1).getCount()==2,"Real button replay commits exactly once");});c.takeScreenshot("p8-paid-job-menu");
                c.runOnClient(client -> client.player.closeContainer());c.waitFor(client -> client.gui.screen()==null);save=world.getWorldSave();
            }
            try(TestSingleplayerContext world=save.open()){
                world.getConnection().waitForChunksRender();world.getServer().runOnServer(server -> {var b=(FabricatorEntity)server.overworld().getBlockEntity(POS);check(b.hasJob()&&ItemStack.matches(expected[0],b.pendingResult())&&b.progress()>=progress[0]&&b.progress()<100,"Real disk reload preserves same paid task without offline progress or reroll");});
                c.runOnClient(client -> client.player.getInventory().setSelectedSlot(3));c.waitTicks(3);aim(c);
                c.getInput().holdKey(o -> o.keyAttack);c.waitFor(client -> client.level.getBlockState(POS).isAir(),80);c.getInput().releaseKey(o -> o.keyAttack);
                c.waitFor(client -> client.level.getEntitiesOfClass(ItemEntity.class,new AABB(POS).inflate(2)).stream().anyMatch(e -> e.getItem().is(FabricationContent.ITEM)));
                world.getServer().runOnServer(server -> {var level=server.overworld();var drops=level.getEntitiesOfClass(ItemEntity.class,new AABB(POS).inflate(2));check(drops.size()==1&&drops.getFirst().getItem().is(FabricationContent.ITEM),"Actual mining drops one carried machine, no scattered ingredients");var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(level,.5,120,2.5,Set.of(),0,30,false);});
                c.waitTicks(20);
                c.waitFor(client -> java.util.stream.IntStream.range(0,36).anyMatch(i -> client.player.getInventory().getItem(i).is(FabricationContent.ITEM)));
                world.getServer().runOnServer(server -> {var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(server.overworld(),.5,120,.5,Set.of(),0,30,false);int slot=java.util.stream.IntStream.range(0,36).filter(i -> p.getInventory().getItem(i).is(FabricationContent.ITEM)).findFirst().orElseThrow();p.getInventory().setItem(0,p.getInventory().removeItemNoUpdate(slot));p.getInventory().setSelectedSlot(0);p.inventoryMenu.broadcastChanges();});c.runOnClient(client -> client.player.getInventory().setSelectedSlot(0));c.waitTicks(3);c.waitFor(client -> client.player.getMainHandItem().is(FabricationContent.ITEM));place(c);
                c.waitFor(client -> client.level.getBlockState(POS).is(FabricationContent.BLOCK)&&client.player.getMainHandItem().isEmpty());open(c);c.takeScreenshot("p8-carried-job-resumed");
                c.waitFor(client -> client.player.containerMenu.getSlot(2).hasItem(),150);
                check(c.computeOnClient(client -> ItemStack.matches(expected[0],client.player.containerMenu.getSlot(2).getItem())),"Same paid result finishes after actual mining and native placement");c.takeScreenshot("p8-completed-output");
                world.getServer().runOnServer(server -> {var p=server.getPlayerList().getPlayers().getFirst();for(int n=0;n<36;n++)p.getInventory().setItem(n,new ItemStack(Items.STONE,64));p.containerMenu.broadcastChanges();});c.waitTicks(5);shift(c,2);c.waitTicks(5);
                check(c.computeOnClient(client -> client.player.containerMenu.getSlot(2).hasItem()),"Actual full player inventory retains output in machine");
                c.runOnClient(client -> client.gameMode.handleInventoryButtonClick(client.player.containerMenu.containerId,0));c.waitTicks(5);world.getServer().runOnServer(server -> {var b=(FabricatorEntity)server.overworld().getBlockEntity(POS);check(!b.hasJob()&&b.getItem(0).getCount()==4&&b.getItem(1).getCount()==2,"Uncollected output refuses a second debit");var p=server.getPlayerList().getPlayers().getFirst();p.getInventory().setItem(0,ItemStack.EMPTY);p.containerMenu.broadcastChanges();});c.waitTicks(5);shift(c,2);
                // Native container clicks may be fully predicted and send no changed state id.
                // A later command on the same connection acknowledges authoritative receipt.
                c.runOnClient(client -> client.getConnection().sendCommand("p8collected"));
                c.waitFor(client -> client.player.getAttachedOrElse(dev.wildcraft.test.fabrication.FabricationMultiplayerProbe.ACK,-1)==99);
                check(c.computeOnClient(client -> !client.player.containerMenu.getSlot(2).hasItem() && ItemStack.matches(expected[0],client.player.getInventory().getItem(0))),"Actual native collection matches server acknowledgement");world.getServer().runOnServer(server -> {var p=server.getPlayerList().getPlayers().getFirst();check(ItemStack.matches(expected[0],p.getInventory().getItem(0)),"One real output reaches one real slot");});c.takeScreenshot("p8-output-collected-once");
                var english=c.computeOnClient(client -> {client.options.languageCode="en_us";client.getLanguageManager().setSelected("en_us");return client.reloadResourcePacks();});c.waitFor(client -> english.isDone());english.join();c.waitFor(client -> client.gui.overlay()==null);c.takeScreenshot("p8-english-menu");
            }
        }finally{var reset=c.computeOnClient(client -> {client.options.languageCode=lang;client.getLanguageManager().setSelected(lang);return client.reloadResourcePacks();});c.waitFor(client -> reset.isDone());reset.join();}
        System.out.println("WILDCRAFT P8 native placement/input/button replay, real disk paid job, actual mining/single carried loot/replacement, same output, full inventory and single collection passed");
    }
    private static void place(ClientGameTestContext c){c.runOnClient(client -> client.gameMode.useItemOn(client.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atBottomCenterOf(POS),Direction.UP,POS.below(),false)));}
    private static void open(ClientGameTestContext c){c.runOnClient(client -> client.gameMode.useItemOn(client.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(POS),Direction.NORTH,POS,false)));c.waitFor(client -> client.gui.screen() instanceof FabricatorScreen);}
    private static void shift(ClientGameTestContext c,int slot){c.runOnClient(client -> client.gameMode.handleContainerInput(client.player.containerMenu.containerId,slot,0,ContainerInput.QUICK_MOVE,client.player));}
    private static void aim(ClientGameTestContext c){c.runOnClient(client -> {var d=Vec3.atCenterOf(POS).subtract(client.player.getEyePosition());client.player.setYRot((float)Math.toDegrees(Math.atan2(-d.x,d.z)));client.player.setXRot((float)-Math.toDegrees(Math.atan2(d.y,Math.sqrt(d.horizontalDistanceSqr()))));});c.waitTicks(3);c.waitFor(client -> client.hitResult instanceof BlockHitResult hit&&hit.getBlockPos().equals(POS));}
    private static void check(boolean yes,String message){if(!yes)throw new AssertionError(message);}
}
