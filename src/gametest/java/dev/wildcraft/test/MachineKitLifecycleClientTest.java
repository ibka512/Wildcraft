package dev.wildcraft.test;

import dev.wildcraft.cooking.*;
import dev.wildcraft.energy.*;
import dev.wildcraft.fabrication.*;
import dev.wildcraft.fuse.*;
import dev.wildcraft.mechanics.*;
import dev.wildcraft.player.*;
import dev.wildcraft.registry.WildcraftItems;
import dev.wildcraft.traversal.*;
import dev.wildcraft.focus.FocusTime;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Provided player kit and a separately paid machine share native riding, disk and recovery lifecycle. */
public final class MachineKitLifecycleClientTest implements FabricClientGameTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static void kit(net.minecraft.server.level.ServerPlayer p){
        check(PlayerStamina.get(p).highestLevel()==30&&GliderEquipment.get(p).is(WildcraftItems.PARAGLIDER),"Player growth and dedicated glider remain owned while driving");
        check(p.getInventory().getItem(9).is(EnergyContent.BATTERY)&&Batteries.energy(p.getInventory().getItem(9))==617,"Carried battery cannot pay machine cost or refill");
        var sword=p.getInventory().getItem(0);check(sword.is(Items.IRON_SWORD)&&sword.getDamageValue()==7&&sword.get(FusionContent.DATA)!=null&&sword.get(FusionContent.DATA).remaining()==32,"Driving does not attack, wear or consume carried Fuse material");
        check(CookingEffects.get(p).warmth().millis()>0,"Actually eaten meal remains active beside machine");
    }
    private static int count(net.minecraft.world.entity.player.Inventory inventory,Item item){int n=0;for(int i=0;i<36;i++)if(inventory.getItem(i).is(item))n+=inventory.getItem(i).getCount();return n;}
    @Override public void runTest(ClientGameTestContext c){
        TestWorldSave save;int remaining;ItemStack paidOutput;var factoryPos=new BlockPos(3,120,-2);
        try(TestSingleplayerContext w=c.worldBuilder().create()){
            w.getConnection().waitForChunksRender();w.getServer().runCommand("gamemode survival @p");
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.getInventory().clearContent();p.giveExperienceLevels(30);p.giveExperienceLevels(-25);GliderEquipment.set(p,new ItemStack(WildcraftItems.PARAGLIDER));
                for(int x=-8;x<=8;x++)for(int z=-60;z<=60;z++)s.overworld().setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());p.teleportTo(s.overworld(),.5,120,.5,Set.of(),0,20,true);p.setDeltaMovement(Vec3.ZERO);
                var carried=new ItemStack(EnergyContent.BATTERY);Batteries.charge(carried,1000);check(Batteries.consume(carried,383),"Carried provided battery pays383");p.getInventory().setItem(9,carried);
                var sword=new ItemStack(Items.IRON_SWORD);sword.setDamageValue(7);var material=new ItemStack(Items.DIAMOND);check(FusionItems.attach(sword,material,p.registryAccess())&&material.isEmpty(),"Provided material is really consumed");p.getInventory().setItem(0,sword);
                var meal=new ItemStack(CookingContent.MEAL);meal.set(CookingContent.MEAL_DATA,new MealData(1,MealData.WARMTH,1,120));p.getInventory().setItem(1,meal);
                var installed=new ItemStack(EnergyContent.BATTERY);Batteries.charge(installed,1000);p.getInventory().setItem(6,installed);p.getInventory().setItem(7,new ItemStack(MechanicsContent.FAN));
                var body=MechanicsContent.MACHINE.create(s.overworld(),EntitySpawnReason.COMMAND);body.setPos(.5,120,2.5);body.setYRot(0);body.setOwner(p.getUUID());s.overworld().addFreshEntity(body);p.inventoryMenu.broadcastChanges();
            });
            c.waitFor(v->v.gui.screen()==null&&v.gui.overlay()==null&&v.player.getY()>119&&MechanicsClientSmokeTest.find(v.level)!=null);c.runOnClient(v->{v.player.setYRot(180);v.player.setXRot(0);});c.waitTicks(2);c.getInput().pressKey(v->v.keyHotbarSlots[1]);c.waitFor(v->v.player.getMainHandItem().is(CookingContent.MEAL));c.getInput().holdKey(v->v.keyUse);c.waitFor(v->v.player.getAttached(CookingEffects.VIEW)!=null&&v.player.getAttached(CookingEffects.VIEW).warmth()==1);c.getInput().releaseKey(v->v.keyUse);c.waitFor(v->v.player.getMainHandItem().is(Items.BOWL));
            c.getInput().pressKey(v->v.keyHotbarSlots[6]);c.waitFor(v->v.player.getMainHandItem().is(EnergyContent.BATTERY));w.getServer().waitFor(s->s.getPlayerList().getPlayers().getFirst().getMainHandItem().is(EnergyContent.BATTERY));w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(MechanicsClientSmokeTest.find(s.overworld()).install(p,0,InteractionHand.MAIN_HAND),"Real owned second battery transfers to real node");});
            c.getInput().pressKey(v->v.keyHotbarSlots[7]);c.waitFor(v->v.player.getMainHandItem().is(MechanicsContent.FAN));w.getServer().waitFor(s->s.getPlayerList().getPlayers().getFirst().getMainHandItem().is(MechanicsContent.FAN));w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(MechanicsClientSmokeTest.find(s.overworld()).install(p,2,InteractionHand.MAIN_HAND),"Real fan transfers to node");});c.waitFor(v->v.player.getMainHandItem().isEmpty());
            MechanicsClientSmokeTest.aim(c,MachineNodes.point(2));MechanicsClientSmokeTest.click(c);c.waitFor(v->v.player.getVehicle() instanceof MachineEntity);c.getInput().holdKeyFor(dev.wildcraft.client.mechanics.MachinePresentation.TOGGLE,1);c.waitFor(v->MechanicsClientSmokeTest.find(v.level).working());c.getInput().holdKeyFor(v->v.keyRight,8);c.waitTicks(10);w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();kit(p);var body=MechanicsClientSmokeTest.find(s.overworld());check(body.energy()>0&&body.energy()<1000,"Only actually installed battery pays native working ticks");check(Math.abs(body.getYRot())>5&&body.position().distanceToSqr(new Vec3(.5,120,2.5))>.05,"Real rider steering changes yaw and actual machine displacement");check(!Climbing.active(p)&&!Gliding.active(p)&&!FocusTime.active(s),"Actual rider has no illegal traversal or Focus");});c.takeScreenshot("kit-native-riding-separate-finite-batteries");
            c.getInput().holdKeyFor(dev.wildcraft.client.mechanics.MachinePresentation.TOGGLE,1);c.waitFor(v->!MechanicsClientSmokeTest.find(v.level).enabled());c.getInput().holdKeyFor(v->v.keyShift,1);c.waitFor(v->v.player.getVehicle()==null);
            remaining=w.getServer().computeOnServer(s->{var body=MechanicsClientSmokeTest.find(s.overworld());check(!body.enabled()&&!body.working(),"Stopped machine cannot consume while saved or offline");return body.energy();});c.takeScreenshot("kit-stopped-dismounted-before-save");save=w.getWorldSave();
        }
        try(TestSingleplayerContext w=save.open()){
            w.getConnection().waitForChunksRender();c.waitFor(v->MechanicsClientSmokeTest.find(v.level)!=null);w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();kit(p);var body=MechanicsClientSmokeTest.find(s.overworld());check(body.owner().equals(p.getUUID())&&!body.enabled()&&body.energy()==remaining,"Stopped real assembly retains exact owner and separately paid charge");check(!p.isPassenger()&&s.tickRateManager().tickrate()==20,"No old riding/input/lease is replayed");p.teleportTo(s.overworld(),body.getX(),body.getY(),body.getZ()-2,Set.of(),body.getYRot(),20,true);check(body.recover(p,0)&&!body.recover(p,0)&&body.recover(p,2)&&body.recoverBody(p),"Native recovery returns actual battery/fan/body only once");int count=0,total=0,fans=0,bodies=0;for(int i=0;i<36;i++){var stack=p.getInventory().getItem(i);if(stack.is(EnergyContent.BATTERY)){count+=stack.getCount();total+=Batteries.energy(stack);}if(stack.is(MechanicsContent.FAN))fans+=stack.getCount();if(stack.is(MechanicsContent.BODY))bodies+=stack.getCount();}check(count==2&&total==617+remaining&&fans==1&&bodies==1,"Two independent finite batteries and one fan/body conserve exact total charge and count");p.inventoryMenu.broadcastChanges();});c.waitFor(v->MechanicsClientSmokeTest.find(v.level)==null);c.takeScreenshot("kit-native-reopen-once-recovery-conservation");
            paidOutput=w.getServer().computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(s.overworld(),1.5,120,-2.5,Set.of(),0,20,true);s.overworld().setBlockAndUpdate(factoryPos,FabricationContent.BLOCK.defaultBlockState());var factory=(FabricatorEntity)s.overworld().getBlockEntity(factoryPos);factory.setItem(0,new ItemStack(Items.COPPER_INGOT,8));factory.setItem(1,new ItemStack(Items.REDSTONE,4));check(factory.start(p)&&!factory.start(p),"Real provided paid job starts once; replay cannot debit twice");check(factory.getItem(0).getCount()==4&&factory.getItem(1).getCount()==2,"Native job consumes exactly four copper and two redstone");return factory.pendingResult();});save=w.getWorldSave();

        }
        try(TestSingleplayerContext w=save.open()){
            w.getConnection().waitForChunksRender();w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();kit(p);var factory=(FabricatorEntity)s.overworld().getBlockEntity(factoryPos);check(factory!=null&&ItemStack.matches(paidOutput,factory.hasJob()?factory.pendingResult():factory.getItem(2)),"Native reload retains same actual committed result alongside player kit, without reroll");check(factory.getItem(0).getCount()==4&&factory.getItem(1).getCount()==2,"Job never debits provided input again after reopening");});
            c.waitFor(v->v.gui.screen()==null&&v.gui.overlay()==null&&v.player.getY()>119);c.runOnClient(v->v.gameMode.useItemOn(v.player,InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(factoryPos),net.minecraft.core.Direction.NORTH,factoryPos,false)));c.waitFor(v->v.player.containerMenu instanceof FabricatorMenu);c.waitFor(v->v.player.containerMenu.getSlot(2).hasItem(),150);check(c.computeOnClient(v->ItemStack.matches(paidOutput,v.player.containerMenu.getSlot(2).getItem())),"Same authoritative output really finishes in native menu");c.takeScreenshot("kit-paid-factory-reload-same-result");
            int before=c.computeOnClient(v->count(v.player.getInventory(),paidOutput.getItem()));c.runOnClient(v->v.gameMode.handleContainerInput(v.player.containerMenu.containerId,2,0,net.minecraft.world.inventory.ContainerInput.QUICK_MOVE,v.player));c.waitFor(v->!v.player.containerMenu.getSlot(2).hasItem()&&count(v.player.getInventory(),paidOutput.getItem())==before+1);w.getServer().waitFor(s->count(s.getPlayerList().getPlayers().getFirst().getInventory(),paidOutput.getItem())==before+1);w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();kit(p);var factory=(FabricatorEntity)s.overworld().getBlockEntity(factoryPos);check(!factory.hasJob()&&factory.getItem(2).isEmpty()&&factory.getItem(0).getCount()==4&&factory.getItem(1).getCount()==2,"One real collection conserves unpaid remainder without auto-restarting");});c.runOnClient(v->v.player.closeContainer());c.takeScreenshot("kit-paid-factory-once-collection-keeps-kit");
        }
        System.out.println("C7 provided player growth/glider/Fuse/eaten meal and separate batteries native ride-stop-save-recovery plus paid factory same-result-reload/real once collection PASS; API installation/job fixture, not raw crafting or manual performance");
    }
}
