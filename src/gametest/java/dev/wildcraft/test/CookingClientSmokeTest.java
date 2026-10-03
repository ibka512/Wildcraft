package dev.wildcraft.test;

import dev.wildcraft.cooking.*;
import dev.wildcraft.client.cooking.CookingScreen;
import dev.wildcraft.focus.FocusTime;
import dev.wildcraft.player.PlayerStamina;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;

/** Production jar: native menu packets, cooked item consumption and remaining-time lifecycle. */
public final class CookingClientSmokeTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext c) {
        TestWorldSave save;
        String language=c.computeOnClient(client -> client.options.languageCode);
        var potPos=new BlockPos(3,120,0);
        try {
            var reload=c.computeOnClient(client -> {client.options.languageCode="zh_cn";client.getLanguageManager().setSelected("zh_cn");return client.reloadResourcePacks();});
            c.waitFor(client -> reload.isDone());reload.join();c.waitFor(client -> client.gui.overlay()==null);
            try(TestSingleplayerContext world=c.worldBuilder().create()) {
                world.getConnection().waitForChunksRender();world.getServer().runCommand("gamemode survival @p");
                world.getServer().runOnServer(server -> {
                    var p=server.getPlayerList().getPlayers().getFirst();var level=server.overworld();
                    for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)level.setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());
                    p.teleportTo(level,.5,120,.5,Set.of(),0,0,false);p.setNoGravity(true);
                    level.setBlockAndUpdate(potPos,CookingContent.POT.defaultBlockState());
                    level.setBlockAndUpdate(potPos.below(),Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT,false));
                    var pot=(CookingPotEntity)level.getBlockEntity(potPos);
                    pot.setItem(0,new ItemStack(Items.BEETROOT,2));pot.setItem(1,new ItemStack(Items.POTATO,2));pot.setItem(3,new ItemStack(Items.BOWL,2));
                });
                c.waitFor(client -> client.level.getBlockState(potPos).is(CookingContent.POT) && client.player.getY()>119);
                c.runOnClient(client -> client.gameMode.useItemOn(client.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(potPos),Direction.UP,potPos,false)));
                c.waitFor(client -> client.gui.screen() instanceof CookingScreen);
                c.waitFor(client -> ((CookingMenu)client.player.containerMenu).status()==1);
                c.takeScreenshot("p4b-pot-awaiting-heat");
                world.getServer().runOnServer(server -> server.overworld().setBlockAndUpdate(potPos.below(),Blocks.CAMPFIRE.defaultBlockState()));
                c.waitFor(client -> client.player.containerMenu.getSlot(4).hasItem());
                check(c.computeOnClient(client -> client.player.containerMenu.getSlot(4).getItem().get(CookingContent.MEAL_DATA).equals(new MealData(1,MealData.WARMTH,1,120))),"Cooked server component crossed real menu connection");
                c.takeScreenshot("p4b-cooked-meal-menu");
                c.runOnClient(client -> client.gameMode.handleContainerInput(client.player.containerMenu.containerId,4,0,ContainerInput.QUICK_MOVE,client.player));
                c.waitFor(client -> !client.player.containerMenu.getSlot(4).hasItem());c.waitTicks(5);
                world.getServer().runOnServer(server -> {
                    var p=server.getPlayerList().getPlayers().getFirst();
                    int slot=-1;for(int i=0;i<36;i++)if(p.getInventory().getItem(i).is(CookingContent.MEAL)){slot=i;break;}
                    check(slot>=0,"Real cooked meal reaches player inventory");
                    var meal=p.getInventory().removeItemNoUpdate(slot);p.getInventory().setItem(0,meal);p.getInventory().setSelectedSlot(0);
                    p.inventoryMenu.broadcastChanges();
                });
                c.runOnClient(client -> client.player.closeContainer());c.waitFor(client -> client.gui.screen()==null && client.player.getMainHandItem().is(CookingContent.MEAL));
                c.getInput().holdKey(o -> o.keyUse);c.waitFor(client -> client.player.getAttached(CookingEffects.VIEW).warmth()==1);c.getInput().releaseKey(o -> o.keyUse);
                c.waitFor(client -> client.player.getMainHandItem().is(Items.BOWL));
                check(c.computeOnClient(client -> client.player.getFoodData().getFoodLevel()==20),"Full hunger still allows finite-effect meals");
                c.takeScreenshot("p4b-consumed-meal-hud");
                var pausedBefore=new AtomicInteger();
                c.runOnClient(client -> client.gui.setScreen(new PauseScreen(true)));c.waitFor(client -> client.isPaused());
                world.getServer().runOnServer(server -> pausedBefore.set(CookingEffects.get(server.getPlayerList().getPlayers().getFirst()).warmth().millis()));
                long pauseUntil=System.nanoTime()+700_000_000L;c.waitFor(client -> System.nanoTime()>=pauseUntil);
                world.getServer().runOnServer(server -> check(CookingEffects.get(server.getPlayerList().getPlayers().getFirst()).warmth().millis()==pausedBefore.get(),"Paused interval does not decrement saved effect"));
                c.runOnClient(client -> client.gui.setScreen(null));c.waitFor(client -> !client.isPaused());
                // An actual eaten short meal must expire in effective real time, not after 40 accelerated test ticks.
                give(world,new MealData(1,MealData.RECOVERY,1,2));c.waitFor(client -> client.player.getMainHandItem().is(CookingContent.MEAL));
                c.getInput().holdKey(o -> o.keyUse);c.waitFor(client -> client.player.getAttached(CookingEffects.VIEW).recovery()==1);c.getInput().releaseKey(o -> o.keyUse);
                long consumed=System.nanoTime();c.waitFor(client -> client.player.getAttached(CookingEffects.VIEW).recovery()==0);
                double duration=(System.nanoTime()-consumed)/1e9;
                check(duration>1.5 && duration<4,"Two-second consumed recovery expires by real time: "+duration);
                // Actual native powder-snow increments remain finite and leather still works.
                world.getServer().runOnServer(server -> {
                    var p=server.getPlayerList().getPlayers().getFirst();CookingEffects.eat(p,new MealData(1,MealData.WARMTH,2,30));p.setTicksFrozen(0);p.teleportTo(.5,120,.5);p.setDeltaMovement(Vec3.ZERO);
                    for(int x=-1;x<=1;x++)for(int y=120;y<=122;y++)for(int z=-1;z<=1;z++)server.overworld().setBlockAndUpdate(new BlockPos(x,y,z),Blocks.POWDER_SNOW.defaultBlockState());
                });
                c.waitTicks(40);world.getServer().runOnServer(server -> {var p=server.getPlayerList().getPlayers().getFirst();System.out.println("P4B freeze probe pos="+p.position()+" freeze="+p.getTicksFrozen()+" powder="+p.isInPowderSnow+" can="+p.canFreeze()+" warm="+CookingEffects.get(p));});
                c.waitFor(client -> client.player.getTicksFrozen()>10);c.waitFor(client -> client.player.isFullyFrozen(),600);
                c.waitFor(client -> client.player.getHealth()<20,200);
                c.takeScreenshot("p4b-warmth-still-freezes");
                world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst().setItemSlot(EquipmentSlot.FEET,new ItemStack(Items.LEATHER_BOOTS)));
                c.waitFor(client -> client.player.getTicksFrozen()==0);
                world.getServer().runOnServer(server -> {
                    var p=server.getPlayerList().getPlayers().getFirst();p.setItemSlot(EquipmentSlot.FEET,ItemStack.EMPTY);
                    for(int x=-1;x<=1;x++)for(int y=120;y<=122;y++)for(int z=-1;z<=1;z++)server.overworld().setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
                    p.setHealth(20);CookingEffects.eat(p,new MealData(1,MealData.COOLING,1,10));p.igniteForSeconds(3);
                });
                c.waitFor(client -> client.player.getHealth()<20,200);
                check(c.computeOnClient(client -> client.player.getAttached(CookingEffects.VIEW).cooling()==1),"Cooling meal never grants fire immunity");
                // Native focus lowers world rate but the independent food clock keeps running.
                world.getServer().runOnServer(server -> {
                    var p=server.getPlayerList().getPlayers().getFirst();p.clearFire();p.teleportTo(.5,150,.5);p.setNoGravity(true);p.setOnGround(false);
                    p.getInventory().setItem(0,new ItemStack(Items.BOW));p.getInventory().setItem(9,new ItemStack(Items.ARROW,64));p.getInventory().setSelectedSlot(0);p.inventoryMenu.broadcastChanges();PlayerStamina.fill(p);
                    p.setAttached(CookingEffects.DATA,MealEffects.EMPTY.eat(new MealData(1,MealData.COOLING,1,2)));CookingEffects.publish(p);
                });
                c.waitFor(client -> client.player.getMainHandItem().is(Items.BOW) && client.player.getY()>149);
                c.getInput().holdKey(o -> o.keyUse);c.waitFor(client -> FocusTime.active(client.player));
                long focused=System.nanoTime();c.waitFor(client -> client.player.getAttached(CookingEffects.VIEW).cooling()==0);
                check((System.nanoTime()-focused)/1e9<3,"5 TPS focus does not multiply food duration");
                c.getInput().releaseKey(o -> o.keyUse);c.waitFor(client -> !FocusTime.active(client.player));
                world.getServer().runOnServer(server -> {
                    var p=server.getPlayerList().getPlayers().getFirst();CookingEffects.eat(p,new MealData(1,MealData.RECOVERY,2,30));
                    p.teleportTo(server.getLevel(Level.NETHER),.5,120,.5,Set.of(),0,0,false);p.setNoGravity(true);
                });
                c.waitFor(client -> client.level.dimension().equals(Level.NETHER) && client.player.getAttached(CookingEffects.VIEW).recovery()==2);
                save=world.getWorldSave();
            }
            long offlineUntil=System.nanoTime()+500_000_000L;c.waitFor(client -> System.nanoTime()>=offlineUntil);
            try(TestSingleplayerContext world=save.open()) {
                world.getConnection().waitForChunksRender();
                c.waitFor(client -> client.player.getAttached(CookingEffects.VIEW)!=null);
                check(c.computeOnClient(client -> client.player.getAttached(CookingEffects.VIEW).recovery()==2 && client.player.getAttached(CookingEffects.VIEW).recoverySeconds()<=30),"Reload preserves remaining effect without refreshing or losing it");
                c.takeScreenshot("p4b-reloaded-effects");world.getServer().runCommand("kill @p");c.waitFor(client -> !client.player.isAlive());
                c.runOnClient(client -> client.player.respawn());c.waitFor(client -> client.player.isAlive() && client.player.getAttached(CookingEffects.VIEW)!=null);
                check(c.computeOnClient(client -> client.player.getAttached(CookingEffects.VIEW).recovery()==0),"Death clears food effects after native attachment transfer");
            }
        } finally {
            c.getInput().releaseKey(o -> o.keyUse);
            var reload=c.computeOnClient(client -> {client.options.languageCode=language;client.getLanguageManager().setSelected(language);return client.reloadResourcePacks();});c.waitFor(client -> reload.isDone());reload.join();
        }
        System.out.println("WILDCRAFT P4B native pot/menu/consumption/bowl, real expiry, pause, finite freezing/leather/fire, focus, dimension/reload/death passed");
    }
    private static void give(TestSingleplayerContext world,MealData meal){world.getServer().runOnServer(server -> {var p=server.getPlayerList().getPlayers().getFirst();p.getInventory().setItem(0,CookingContent.meal(meal));p.getInventory().setSelectedSlot(0);p.inventoryMenu.broadcastChanges();});}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
