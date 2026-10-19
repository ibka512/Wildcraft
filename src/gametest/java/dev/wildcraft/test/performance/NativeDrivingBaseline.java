package dev.wildcraft.test.performance;

import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import dev.wildcraft.client.mechanics.MachinePresentation;
import dev.wildcraft.energy.*;
import dev.wildcraft.mechanics.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

/** Normal 20 TPS, provided assemblies, actual client riding/toggle/forward/dismount inputs. No manual balance claim. */
public final class NativeDrivingBaseline {
    private record State(boolean riding, boolean enabled, boolean working, int energy, long ticks, boolean forward, double z) { }
    private static final List<Map<String,Object>> RESULTS=new ArrayList<>();
    private static final List<Long> FRAME_INTERVALS=new ArrayList<>();
    private static final Map<String,Integer> VIEWPORT_FRAMES=new LinkedHashMap<>();
    private static String requiredViewport;
    private static boolean resizeProbeTriggered;
    private static CompletableFuture<State> pending;
    private static MachineEntity body;
    private static int phase,index,releaseTick,paidTicks,lastEnergy,paidEnergy;
    private static final Map<Integer,Integer> DEBITS=new LinkedHashMap<>();
    private static long boot,deadline,lastFrame,firstPaidAt,lastPaidAt,firstPaidTick,lastPaidTick,driveStarted,nextPoll,nextProgress;
    private static double path,pathAtLastPaid;
    private static Vec3 origin,previousPosition;
    private static Map<String,Object> at50,at100;
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static CompletableFuture<State> submit(MinecraftServer server,Supplier<State> action){
        var future=new CompletableFuture<State>();server.execute(()->{try{future.complete(action.get());}catch(Throwable e){future.completeExceptionally(e);}});return future;
    }
    private static State state(MinecraftServer s){return new State(s.getPlayerList().getPlayers().getFirst().getVehicle()==body,body.enabled(),body.working(),body.energy(),s.overworld().getGameTime(),s.getPlayerList().getPlayers().getFirst().getLastClientInput().forward(),body.getZ());}
    private static Map<String,Object> milestone(long now){return Map.of("elapsedSeconds",(now-firstPaidAt)/1e9,"worldTicks",body.level().getGameTime()-firstPaidTick,"energy",body.energy(),"pathBlocks",path,"straightBlocks",Math.sqrt(body.position().subtract(origin).horizontalDistanceSqr()));}
    /** Read-only observer installed only in this isolated probe's metadata. */
    public static void serverTick(MinecraftServer s){
        if(!Boolean.getBoolean("wildcraft.c7.driveBaseline")||body==null||body.isRemoved()||previousPosition==null)return;
        long now=System.nanoTime();int energy=body.energy(),cost=0;
        for(int n=0;n<6;n++)if(body.active(n))cost+=switch(body.kind(n)){case 1,6->1;case 7->2;default->0;};
        path+=Math.sqrt(body.position().subtract(previousPosition).horizontalDistanceSqr());previousPosition=body.position();
        int debit=lastEnergy-energy;
        check(debit==0||debit==cost,"Each ordinary machine tick pays its original exact cost, never a test refill: "+debit);
        if(debit>0){paidEnergy+=debit;DEBITS.merge(debit,1,Integer::sum);if(paidTicks++==0){firstPaidAt=now;firstPaidTick=s.overworld().getGameTime();}lastPaidAt=now;lastPaidTick=s.overworld().getGameTime();pathAtLastPaid=path;}
        lastEnergy=energy;
        if(firstPaidAt!=0&&path>=50&&at50==null)at50=milestone(now);
        if(firstPaidAt!=0&&path>=100&&at100==null)at100=milestone(now);
    }
    private static void key(KeyMapping binding){binding.setDown(true);KeyMapping.click(InputConstants.getKey(binding.saveString()));}
    private static void shot(Minecraft c,String suffix){Screenshot.grab(c.gameDirectory,"c7-native-drive-"+index+"-"+suffix+".png",c.gameRenderer.mainRenderTarget(),1,m->System.out.println(m.getString()));}
    private static void write(Minecraft c,String status,String message){try{Files.writeString(c.gameDirectory.toPath().resolve("c7-native-driving-baseline.json"),new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("status",status,"message",message,"acceleratedGameTest",false,"providedAssemblyFixture",true,"manualBalanceVerified",false,"scenes",RESULTS,"requiredViewport",String.valueOf(requiredViewport),"viewportFrames",new LinkedHashMap<>(VIEWPORT_FRAMES)))+"\n");}catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}}
    public static void frame(Minecraft c){
        if(!Boolean.getBoolean("wildcraft.c7.driveBaseline"))return;
        long now=System.nanoTime();if(boot==0)boot=now;
        c.options.pauseOnLostFocus=false;
        try{
            check(System.getProperty("fabric.client.gametest")==null,"Native driver must not use client-gametest scheduling");
            check(now-boot<240_000_000_000L,"Normal driving probe timed out at phase "+phase+" screen="+(c.gui.screen()==null?"none":c.gui.screen().getClass().getSimpleName())+" active="+c.isWindowActive());
            if(c.player==null||c.level==null||c.getSingleplayerServer()==null||c.gui.screen()!=null||c.gui.overlay()!=null||c.isPaused())return;
            var server=c.getSingleplayerServer();
            if(phase==0){
                c.options.enableVsync().set(false);c.options.inactivityFpsLimit().set(InactivityFpsLimit.MINIMIZED);c.options.renderDistance().set(4);c.options.framerateLimit().set(Integer.getInteger("wildcraft.c7.frameLimit",120));c.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                c.options.keyUp.setDown(false);c.options.keyShift.setDown(false);MachinePresentation.TOGGLE.setDown(false);
                pending=submit(server,()->{
                    check(server.tickRateManager().tickrate()==20&&!server.isPaused(),"Original normal 20 TPS");
                    var p=server.getPlayerList().getPlayers().getFirst();p.stopRiding();if(body!=null)body.discard();body=null;
                    if(index==0)for(int x=-4;x<=4;x++)for(int z=-5;z<=265;z++)server.overworld().setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());
                    p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();p.teleportTo(server.overworld(),.5,120,.5,Set.of(),0,0,true);p.setDeltaMovement(Vec3.ZERO);
                    var carried=new ItemStack(EnergyContent.BATTERY);Batteries.charge(carried,617);p.getInventory().setItem(9,carried);
                    body=MechanicsContent.MACHINE.create(server.overworld(),EntitySpawnReason.COMMAND);check(body!=null,"Body created");body.setPos(.5,120,2.5);body.setYRot(0);body.setOwner(p.getUUID());server.overworld().addFreshEntity(body);
                    var battery=new ItemStack(EnergyContent.BATTERY);Batteries.charge(battery,1000);p.setItemInHand(InteractionHand.MAIN_HAND,battery);check(body.install(p,0,InteractionHand.MAIN_HAND),"Provided full battery transferred once");
                    for(int node:new int[]{4,5}){p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(MechanicsContent.WHEEL));check(body.install(p,node,InteractionHand.MAIN_HAND),"Provided real wheel installed");}
                    if(index==1){p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(MechanicsContent.FAN));check(body.install(p,2,InteractionHand.MAIN_HAND),"Provided fan");p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(MechanicsContent.STABILIZER));check(body.install(p,1,InteractionHand.MAIN_HAND),"Provided stabilizer");}
                    p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.inventoryMenu.broadcastChanges();
                    paidTicks=paidEnergy=0;DEBITS.clear();lastEnergy=1000;path=pathAtLastPaid=0;firstPaidAt=lastPaidAt=0;at50=at100=null;origin=previousPosition=body.position();return state(server);
                });phase=1;
            }else if(phase==1&&pending.isDone()){
                pending.join();deadline=now+5_000_000_000L;phase=2;
            }else if(phase==2&&now>=deadline){
                var target=c.level.getEntity(body.getId());if(!(target instanceof MachineEntity))return;
                var delta=target.position().add(MachineNodes.point(2)).subtract(c.player.getEyePosition());c.player.setYRot((float)Math.toDegrees(Math.atan2(-delta.x,delta.z)));c.player.setXRot((float)-Math.toDegrees(Math.atan2(delta.y,Math.sqrt(delta.horizontalDistanceSqr()))));
                releaseTick=c.player.tickCount+5;phase=3;
            }else if(phase==3&&c.player.tickCount>=releaseTick&&c.hitResult instanceof EntityHitResult hit&&hit.getEntity().getId()==body.getId()){
                key(c.options.keyUse);releaseTick=c.player.tickCount+2;phase=4;
            }else if(phase==4&&c.player.tickCount>=releaseTick){c.options.keyUse.setDown(false);if(!(c.player.getVehicle() instanceof MachineEntity))return;
                releaseTick=c.player.tickCount+4;phase=41;
            }else if(phase==41&&c.player.tickCount>=releaseTick&&c.isWindowActive()){
                c.player.setYRot(0);c.player.setXRot(15);c.options.keyUp.setDown(true);key(MachinePresentation.TOGGLE);releaseTick=c.player.tickCount+2;FRAME_INTERVALS.clear();VIEWPORT_FRAMES.clear();if(requiredViewport==null)requiredViewport=c.getWindow().getWidth()+"x"+c.getWindow().getHeight();lastFrame=now;driveStarted=now;nextPoll=nextProgress=0;pending=submit(server,()->state(server));shot(c,"native-mounted");phase=5;
            }else if(phase==5){
                check(c.isWindowActive()&&c.options.keyUp.isDown(),"Native driving input interrupted by focus/key release; no steady-input result inferred");
                VIEWPORT_FRAMES.merge(c.getWindow().getWidth()+"x"+c.getWindow().getHeight(),1,Integer::sum);
                check(VIEWPORT_FRAMES.keySet().equals(Set.of(requiredViewport)),"Viewport changed during native driving; incomparable FPS: required="+requiredViewport+", observed="+VIEWPORT_FRAMES);
                if(Boolean.getBoolean("wildcraft.c7.viewportChangeProbe")&&!resizeProbeTriggered&&now-driveStarted>1_000_000_000L){resizeProbeTriggered=true;c.getWindow().setWindowed(900,600);}
                FRAME_INTERVALS.add(now-lastFrame);lastFrame=now;if(c.player.tickCount>=releaseTick)MachinePresentation.TOGGLE.setDown(false);
                check(c.getFramerateLimitTracker().getThrottleReason().name().equals("NONE"),"Driving sampling is never AFK/window-throttled");
                // Only a completed server snapshot determines depletion.
                if(pending.isDone()&&now>=nextPoll){var observed=pending.join();nextPoll=now+250_000_000L;
                if(now>=nextProgress){System.out.println("C7 NATIVE DRIVE STATE "+index+" "+observed+" active="+c.isWindowActive()+" forwardKey="+c.options.keyUp.isDown()+" context="+dev.wildcraft.client.input.ActionInput.tickFrame().context());nextProgress=now+2_000_000_000L;}
                check(now-driveStarted<10_000_000_000L||observed.energy()<1000,"Native inputs did not start paid movement: "+observed);
                if(observed.enabled()&&observed.forward()&&!observed.working()&&observed.energy()<(index==0?2:5)&&observed.riding()){c.options.keyUp.setDown(false);pending=submit(server,()->{
                    int cost=index==0?2:5;check(paidTicks>0&&paidEnergy==1000-body.energy()&&body.energy()<cost&&!body.working(),"Original finite battery conserves every actual debit, including a possible sub-cost remainder");
                    if(index==0)check(paidTicks==500&&body.energy()==0,"Uninterrupted two-wheel input pays exactly500 native two-energy ticks");
                    var p=server.getPlayerList().getPlayers().getFirst();check(p.getVehicle()==body&&Batteries.energy(p.getInventory().getItem(9))==617,"Native rider and untouched separate carried battery");
                    var r=new LinkedHashMap<String,Object>();r.put("assembly",index==0?"two wheels + battery":"two wheels + fan + stabilizer + battery");r.put("steadyForwardCostPerWorldTick",cost);r.put("actualDebitDistribution",new LinkedHashMap<>(DEBITS));r.put("paidEnergy",paidEnergy);r.put("paidWorldTicks",paidTicks);r.put("firstToLastPaidWorldTicks",lastPaidTick-firstPaidTick);r.put("firstToLastPaidSeconds",(lastPaidAt-firstPaidAt)/1e9);r.put("measuredPaidWorldTPS",(lastPaidTick-firstPaidTick)/((lastPaidAt-firstPaidAt)/1e9));r.put("pathBlocksAtLastPaidTick",pathAtLastPaid);r.put("pathBlocksAtDepletionObservation",path);r.put("unpoweredCoastBlocksBeforeObservation",path-pathAtLastPaid);r.put("straightBlocksAtDepletion",Math.sqrt(body.position().subtract(origin).horizontalDistanceSqr()));r.put("energyAtDepletion",body.energy());r.put("carriedBattery",617);r.put("at50Blocks",at50==null?Map.of("reached",false):at50);r.put("at100Blocks",at100==null?Map.of("reached",false):at100);RESULTS.add(r);return state(server);
                });phase=6;}else pending=submit(server,()->state(server));}
            }else if(phase==6&&pending.isDone()){
                var s=pending.join();var sorted=FRAME_INTERVALS.stream().mapToLong(Long::longValue).sorted().toArray();var r=RESULTS.getLast();check(VIEWPORT_FRAMES.values().stream().mapToInt(Integer::intValue).sum()==FRAME_INTERVALS.size(),"Every driving sample has a true viewport observation");r.put("viewportFrames",new LinkedHashMap<>(VIEWPORT_FRAMES));r.put("measuredFrames",FRAME_INTERVALS.size());r.put("cycleMedianMs",sorted[sorted.length/2]/1e6);r.put("cycleP95Ms",sorted[(int)((sorted.length-1)*.95)]/1e6);r.put("reportedFPSAtDepletion",c.getFps());r.put("configuredFrameLimit",c.options.framerateLimit().get());r.put("actualRenderDistance",c.options.renderDistance().get());r.put("pixelWidth",c.getWindow().getWidth());r.put("pixelHeight",c.getWindow().getHeight());shot(c,"finite-depleted");
                key(MachinePresentation.TOGGLE);releaseTick=c.player.tickCount+2;deadline=now+500_000_000L;phase=7;
            }else if(phase==7&&c.player.tickCount>=releaseTick&&now>=deadline){MachinePresentation.TOGGLE.setDown(false);key(c.options.keyShift);releaseTick=c.player.tickCount+3;phase=8;
            }else if(phase==8&&c.player.tickCount>=releaseTick){c.options.keyShift.setDown(false);if(c.player.getVehicle()!=null)return;
                pending=submit(server,()->{check(!body.enabled()&&!body.working(),"Actual native R disables depleted assembly");check(Batteries.energy(server.getPlayerList().getPlayers().getFirst().getInventory().getItem(9))==617,"Stop/dismount cannot debit carried battery");return state(server);});phase=9;
            }else if(phase==9&&pending.isDone()){
                pending.join();System.out.println("C7 NATIVE DRIVE "+RESULTS.getLast());shot(c,"native-stopped-dismounted");
                if(++index==2){write(c,"complete","Two native real-time finite-assembly runs; API-provided installations, not manual survival or long-term performance");phase=10;c.stop();}else phase=0;
            }
        }catch(RuntimeException|AssertionError e){c.options.keyUp.setDown(false);c.options.keyUse.setDown(false);c.options.keyShift.setDown(false);MachinePresentation.TOGGLE.setDown(false);shot(c,"failure-phase-"+phase);write(c,"failed",e.toString());throw e;}
    }
}
