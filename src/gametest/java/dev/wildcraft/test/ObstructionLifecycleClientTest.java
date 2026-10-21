package dev.wildcraft.test;

import com.google.gson.GsonBuilder;
import dev.wildcraft.client.mechanics.MachinePresentation;
import dev.wildcraft.energy.*;
import dev.wildcraft.mechanics.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

/** Native reversal/toggle/idle tracking boundaries with provided parts; no replacement collision or energy. */
public final class ObstructionLifecycleClientTest implements FabricClientGameTest {
    private final AtomicReference<MachineEntity> serverBody=new AtomicReference<>();
    private final List<Map<String,Object>> rows=new ArrayList<>();
    private volatile boolean sampling;
    private volatile String phase="setup";
    private int lastEnergy=1000,paid;
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private MachineEntity clientBody(Minecraft c){var b=serverBody.get();return b==null?null:(MachineEntity)c.level.getEntity(b.getId());}
    private void report(ClientGameTestContext c,Map<String,Object> result){c.runOnClient(v->{try{Files.writeString(v.gameDirectory.toPath().resolve("c7-obstruction-lifecycle.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result)+"\n");}catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}});}
    @Override public void runTest(ClientGameTestContext c){
        var result=new LinkedHashMap<String,Object>();TestWorldSave save;int remaining;
        ServerTickEvents.END_SERVER_TICK.register(s->{var b=serverBody.get();if(!sampling||b==null||b.isRemoved())return;int e=b.energy(),debit=lastEnergy-e,cost=(b.active(4)?1:0)+(b.active(5)?1:0);check(debit==0||debit==cost,"Actual native two-wheel debit");lastEnergy=e;paid+=debit;var q=new LinkedHashMap<String,Object>();q.put("tick",s.overworld().getGameTime());q.put("phase",phase);q.put("z",b.getZ());q.put("horizontalCollision",b.horizontalCollision);q.put("obstructed",b.obstructed());q.put("enabled",b.enabled());q.put("working",b.working());q.put("energy",e);q.put("debit",debit);rows.add(q);});
        try{
            try(TestSingleplayerContext w=c.worldBuilder().create()){
                w.getConnection().waitForChunksRender();w.getServer().runOnServer(s->{
                    var p=s.getPlayerList().getPlayers().getFirst();var level=s.overworld();for(int x=-4;x<=4;x++)for(int z=-5;z<=12;z++)level.setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());for(int x=-4;x<=4;x++)for(int y=120;y<=122;y++)level.setBlockAndUpdate(new BlockPos(x,y,6),Blocks.STONE.defaultBlockState());
                    p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();p.teleportTo(level,.5,120,.5,Set.of(),0,0,true);var carried=new ItemStack(EnergyContent.BATTERY);Batteries.charge(carried,617);p.getInventory().setItem(9,carried);
                    var b=MechanicsContent.MACHINE.create(level,EntitySpawnReason.COMMAND);check(b!=null,"Provided body");b.setPos(.5,120,2.5);b.setOwner(p.getUUID());level.addFreshEntity(b);var battery=new ItemStack(EnergyContent.BATTERY);Batteries.charge(battery,1000);p.setItemInHand(InteractionHand.MAIN_HAND,battery);check(b.install(p,0,InteractionHand.MAIN_HAND),"Provided1000 installed");for(int n:new int[]{4,5}){p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(MechanicsContent.WHEEL));check(b.install(p,n,InteractionHand.MAIN_HAND),"Provided real wheels");}p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.inventoryMenu.broadcastChanges();serverBody.set(b);sampling=true;
                });
                c.waitFor(v->clientBody(v)!=null&&v.player.onGround());c.waitTicks(4);c.runOnClient(v->{var d=clientBody(v).position().add(MachineNodes.point(2)).subtract(v.player.getEyePosition());v.player.setYRot((float)Math.toDegrees(Math.atan2(-d.x,d.z)));v.player.setXRot((float)-Math.toDegrees(Math.atan2(d.y,Math.sqrt(d.horizontalDistanceSqr()))));});c.waitFor(v->v.hitResult instanceof EntityHitResult h&&h.getEntity().getId()==serverBody.get().getId());c.getInput().pressKey(v->v.keyUse);c.waitFor(v->v.player.getVehicle() instanceof MachineEntity);c.getInput().pressKey(MachinePresentation.TOGGLE);c.waitFor(v->clientBody(v).enabled());
                phase="forward-blocked";c.getInput().holdKey(v->v.keyUp);c.waitFor(v->clientBody(v).obstructed());w.getServer().runOnServer(s->check(serverBody.get().horizontalCollision&&serverBody.get().working(),"Actual server paid wall contact"));double blockedZ=c.computeOnClient(v->clientBody(v).getZ());c.takeScreenshot("c7-obstruction-native-forward");
                phase="reverse-recovery";c.getInput().releaseKey(v->v.keyUp);c.getInput().holdKey(v->v.keyDown);c.waitFor(v->!clientBody(v).obstructed()&&clientBody(v).getZ()<blockedZ-1);check(c.computeOnClient(v->clientBody(v).enabled()),"Actual reverse clears feedback without switching off");c.takeScreenshot("c7-obstruction-native-reverse-clear");c.getInput().releaseKey(v->v.keyDown);w.getServer().waitFor(s->!s.getPlayerList().getPlayers().getFirst().getLastClientInput().backward());c.waitTicks(5);
                phase="idle-on";int idle=w.getServer().computeOnServer(s->serverBody.get().energy());c.waitTicks(10);w.getServer().runOnServer(s->check(serverBody.get().energy()==idle&&!serverBody.get().working()&&!serverBody.get().obstructed(),"Enabled neutral wheel assembly has no false obstruction or debit"));
                phase="blocked-again";c.getInput().holdKey(v->v.keyUp);c.waitFor(v->clientBody(v).obstructed());c.getInput().pressKey(MachinePresentation.TOGGLE);c.waitFor(v->!clientBody(v).enabled()&&!clientBody(v).obstructed());c.getInput().releaseKey(v->v.keyUp);phase="stopped";c.waitTicks(4);remaining=w.getServer().computeOnServer(s->serverBody.get().energy());c.waitTicks(10);w.getServer().runOnServer(s->check(serverBody.get().energy()==remaining&&!serverBody.get().working()&&!serverBody.get().obstructed()&&remaining>0,"R stop clears actual feedback with charge remaining"));c.takeScreenshot("c7-obstruction-native-stop-clear");
                var perspectives=new ArrayList<String>();for(int n=0;n<3;n++){c.getInput().pressKey(v->v.keyTogglePerspective);c.waitTicks(2);perspectives.add(c.computeOnClient(v->v.options.getCameraType().name()));check(c.computeOnClient(v->v.player.getVehicle() instanceof MachineEntity&&!clientBody(v).obstructed()),"Native F5 retains passenger and stopped state");c.takeScreenshot("c7-obstruction-native-camera-"+n);}check(new HashSet<>(perspectives).size()==3,"Real F5 visits all three native perspectives");result.put("nativePerspectives",perspectives);
                phase="dismount";try{c.getInput().holdKey(v->v.keyShift);c.waitFor(v->v.player.getVehicle()==null);}finally{c.getInput().releaseKey(v->v.keyShift);}w.getServer().runOnServer(s->{check(serverBody.get().energy()==remaining&&paid==1000-remaining,"Finite energy accounts every actual input tick");check(Batteries.energy(s.getPlayerList().getPlayers().getFirst().getInventory().getItem(9))==617,"Separate carried617 unchanged");sampling=false;});save=w.getWorldSave();
            }
            phase="reopen";try(TestSingleplayerContext w=save.open()){
                w.getConnection().waitForChunksRender();w.getServer().runOnServer(s->{var b=(MachineEntity)s.overworld().getEntity(serverBody.get().getUUID());check(b!=null&&b.energy()==remaining&&!b.enabled()&&!b.obstructed()&&b.kind(0)==2&&b.kind(4)==6&&b.kind(5)==6,"Native disk reopen retains battery and parts without stale transient obstruction");});c.takeScreenshot("c7-obstruction-native-reopened");
            }
            result.put("complete",true);result.put("remainingEnergy",remaining);result.put("paidEnergy",paid);result.put("carriedBattery",617);result.put("providedParts",true);result.put("manualSurvivalVerified",false);result.put("rows",new ArrayList<>(rows));report(c,result);System.out.println("WILDCRAFT C7 obstruction real reverse recovery/R-stop/idle/F5/dismount/disk reopen passed");
        }catch(RuntimeException|AssertionError e){result.put("complete",false);result.put("phase",phase);result.put("error",e.toString());result.put("partialRows",new ArrayList<>(rows));report(c,result);throw e;}
        finally{sampling=false;c.getInput().releaseKey(v->v.keyUp);c.getInput().releaseKey(v->v.keyDown);c.getInput().releaseKey(v->v.keyUse);c.getInput().releaseKey(v->v.keyShift);c.getInput().releaseKey(MachinePresentation.TOGGLE);}
    }
}
