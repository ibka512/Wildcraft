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

/** Isolated provided courses, real native inputs and read-only world-tick measurements. */
public final class NativeTerrainBaseline {
    private static final String[] SCENES={"turn-left-right","reverse","wall","drop-one-block"};
    private record State(long tick,boolean riding,boolean enabled,int energy,boolean neutral) {}
    private static final List<Map<String,Object>> RESULTS=new ArrayList<>();
    private static final List<Map<String,Object>> ROWS=new ArrayList<>();
    private static final Map<String,Integer> VIEWPORT=new LinkedHashMap<>();
    private static final List<Long> INTERVALS=new ArrayList<>();
    private static CompletableFuture<State> pending;
    private static MachineEntity body;
    private static int phase,index,releaseTick,startTick,lastEnergy,paid,stoppedEnergy;
    private static long boot,deadline,lastFrame,firstAt,lastAt;
    private static boolean sampling;
    private static String requiredViewport;
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static CompletableFuture<State> submit(MinecraftServer s,Supplier<State> action){
        var f=new CompletableFuture<State>();s.execute(()->{try{f.complete(action.get());}catch(Throwable e){f.completeExceptionally(e);}});return f;
    }
    private static State state(MinecraftServer s){
        var p=s.getPlayerList().getPlayers().getFirst();var input=p.getLastClientInput();
        return new State(s.overworld().getGameTime(),p.getVehicle()==body,body.enabled(),body.energy(),!input.forward()&&!input.backward()&&!input.left()&&!input.right());
    }
    /** Runs only in this probe metadata; observes original movement and billing without replacing them. */
    public static void serverTick(MinecraftServer s){
        if(!sampling||body==null||body.isRemoved())return;
        check(s.tickRateManager().tickrate()==20,"Terrain measurements require original20TPS");
        var p=s.getPlayerList().getPlayers().getFirst();var input=p.getLastClientInput();
        int energy=body.energy(),debit=lastEnergy-energy,cost=0;
        for(int n=0;n<6;n++)if(body.active(n))cost+=switch(body.kind(n)){case 1,6->1;case 7->2;default->0;};
        check(debit==0||debit==cost,"Native debit differs from actual active parts: "+debit+"/"+cost);
        paid+=debit;lastEnergy=energy;long at=System.nanoTime();if(firstAt==0)firstAt=at;lastAt=at;
        var row=new LinkedHashMap<String,Object>();row.put("tick",s.overworld().getGameTime());row.put("x",body.getX());row.put("y",body.getY());row.put("z",body.getZ());row.put("yaw",body.getYRot());
        row.put("ground",body.onGround());row.put("wall",body.horizontalCollision);row.put("boxMaxZ",body.getBoundingBox().maxZ);row.put("energy",energy);row.put("debit",debit);row.put("activeCost",cost);
        row.put("forward",input.forward());row.put("backward",input.backward());row.put("left",input.left());row.put("right",input.right());row.put("riding",p.getVehicle()==body);row.put("enabled",body.enabled());ROWS.add(row);
    }
    private static void keys(Minecraft c,boolean forward,boolean back,boolean left,boolean right){c.options.keyUp.setDown(forward);c.options.keyDown.setDown(back);c.options.keyLeft.setDown(left);c.options.keyRight.setDown(right);}
    private static void key(KeyMapping k){k.setDown(true);KeyMapping.click(InputConstants.getKey(k.saveString()));}
    private static void shot(Minecraft c,String label){Screenshot.grab(c.gameDirectory,"c7-terrain-"+SCENES[index]+"-"+label+".png",c.gameRenderer.mainRenderTarget(),1,m->System.out.println(m.getString()));}
    private static void write(Minecraft c,String status,String message){
        try{var result=new LinkedHashMap<String,Object>();result.put("status",status);result.put("message",message);result.put("phase",phase);result.put("scene",SCENES[Math.min(index,SCENES.length-1)]);result.put("productionPhysicsChanged",false);result.put("providedCourseNotNaturalSurvival",true);result.put("manualBalanceVerified",false);result.put("acceleratedGameTest",false);result.put("scenes",RESULTS);result.put("currentViewport",new LinkedHashMap<>(VIEWPORT));Files.writeString(c.gameDirectory.toPath().resolve("c7-native-terrain.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result)+"\n");}catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}
    }
    private static double value(Map<String,Object> row,String key){return ((Number)row.get(key)).doubleValue();}
    private static long count(String key){return ROWS.stream().filter(r->Boolean.TRUE.equals(r.get(key))).count();}
    private static State finish(MinecraftServer s){
        sampling=false;var st=state(s);var p=s.getPlayerList().getPlayers().getFirst();
        check(!st.enabled&&!st.riding&&st.neutral,"Actual R stop, neutral inputs and Shift dismount");
        check(st.energy==stoppedEnergy&&1000-st.energy==paid&&paid>0,"Finite battery conserved including stopping");
        check(Batteries.energy(p.getInventory().getItem(9))==617,"Separate carried battery is unchanged");
        check(body.kind(0)==2&&body.kind(4)==6&&body.kind(5)==6,"Installed original parts remain unique");
        check(ROWS.size()>60,"Real per-world-tick observation is sufficiently complete");
        double minY=ROWS.stream().mapToDouble(r->value(r,"y")).min().orElseThrow();double maxZ=ROWS.stream().mapToDouble(r->value(r,"z")).max().orElseThrow();double minZ=ROWS.stream().mapToDouble(r->value(r,"z")).min().orElseThrow();double maxX=ROWS.stream().mapToDouble(r->value(r,"x")).max().orElseThrow();
        if(index==0){check(count("left")>=20&&count("right")>=20,"Actual both steering directions sampled");check(maxX>.5+3&&maxZ>2.5+3,"Actual curved trajectory advances in both axes");}
        if(index==1){check(count("backward")>=40&&minZ<2.5-8,"Native reverse drives backward");}
        if(index==2){check(count("wall")>=10,"Actual collision stalls at the wall");check(ROWS.stream().allMatch(r->value(r,"boxMaxZ")<=12.00001),"Collision box never tunnels through the wall");}
        if(index==3){check(minY<119.05&&maxZ>16,"Native descent and continued lower-ground travel");check(ROWS.stream().anyMatch(r->!Boolean.TRUE.equals(r.get("ground"))&&value(r,"debit")==0),"An actual airborne wheel sample does not pay wheel energy");check(ROWS.stream().filter(r->Boolean.TRUE.equals(r.get("ground"))&&value(r,"y")<119.05).count()>10,"Actually lands on the lower surface");}
        var result=new LinkedHashMap<String,Object>();result.put("scene",SCENES[index]);result.put("worldTicks",ROWS.size());result.put("observedSeconds",(lastAt-firstAt)/1e9);result.put("paidEnergy",paid);result.put("remainingEnergy",st.energy);result.put("carriedBattery",617);result.put("minY",minY);result.put("minZ",minZ);result.put("maxZ",maxZ);result.put("maxX",maxX);result.put("wallTicks",count("wall"));result.put("leftTicks",count("left"));result.put("rightTicks",count("right"));result.put("backwardTicks",count("backward"));result.put("rows",new ArrayList<>(ROWS));RESULTS.add(result);return st;
    }
    public static void frame(Minecraft c){
        if(!Boolean.getBoolean("wildcraft.c7.terrainBaseline"))return;
        long now=System.nanoTime();if(boot==0)boot=now;c.options.pauseOnLostFocus=false;
        try{
            check(System.getProperty("fabric.client.gametest")==null,"No accelerated game-test scheduling");
            check(now-boot<240_000_000_000L,"Bounded terrain probe timed out in phase "+phase);
            if(c.player==null||c.level==null||c.getSingleplayerServer()==null||c.gui.screen()!=null||c.gui.overlay()!=null||c.isPaused())return;
            var s=c.getSingleplayerServer();
            if(phase==0){
                keys(c,false,false,false,false);c.options.keyShift.setDown(false);c.options.keyUse.setDown(false);MachinePresentation.TOGGLE.setDown(false);
                c.options.enableVsync().set(false);c.options.inactivityFpsLimit().set(InactivityFpsLimit.MINIMIZED);c.options.renderDistance().set(4);c.options.framerateLimit().set(Integer.getInteger("wildcraft.c7.frameLimit",120));c.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                pending=submit(s,()->{
                    sampling=false;var p=s.getPlayerList().getPlayers().getFirst();p.stopRiding();if(body!=null)body.discard();body=null;
                    for(int x=-20;x<=20;x++)for(int z=-24;z<=64;z++){
                        s.overworld().setBlockAndUpdate(new BlockPos(x,118,z),Blocks.STONE.defaultBlockState());
                        for(int y=119;y<=123;y++)s.overworld().setBlockAndUpdate(new BlockPos(x,y,z),y==119&&!(index==3&&z>=12)?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
                    }
                    if(index==2)for(int x=-20;x<=20;x++)for(int y=120;y<=123;y++)s.overworld().setBlockAndUpdate(new BlockPos(x,y,12),Blocks.STONE.defaultBlockState());
                    p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();p.teleportTo(s.overworld(),.5,120,.5,Set.of(),0,0,true);p.setDeltaMovement(Vec3.ZERO);
                    var carried=new ItemStack(EnergyContent.BATTERY);Batteries.charge(carried,617);p.getInventory().setItem(9,carried);
                    body=MechanicsContent.MACHINE.create(s.overworld(),EntitySpawnReason.COMMAND);check(body!=null,"Original mechanical body created");body.setPos(.5,120,2.5);body.setYRot(0);body.setOwner(p.getUUID());s.overworld().addFreshEntity(body);
                    var battery=new ItemStack(EnergyContent.BATTERY);Batteries.charge(battery,1000);p.setItemInHand(InteractionHand.MAIN_HAND,battery);check(body.install(p,0,InteractionHand.MAIN_HAND),"Full provided battery installed once");
                    for(int node:new int[]{4,5}){p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(MechanicsContent.WHEEL));check(body.install(p,node,InteractionHand.MAIN_HAND),"Original wheel installed");}
                    p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.inventoryMenu.broadcastChanges();ROWS.clear();paid=0;lastEnergy=1000;firstAt=lastAt=0;return state(s);
                });phase=1;
            }else if(phase==1&&pending.isDone()){pending.join();deadline=now+5_000_000_000L;phase=2;
            }else if(phase==2&&now>=deadline){
                var target=c.level.getEntity(body.getId());if(!(target instanceof MachineEntity))return;var delta=target.position().add(MachineNodes.point(2)).subtract(c.player.getEyePosition());c.player.setYRot((float)Math.toDegrees(Math.atan2(-delta.x,delta.z)));c.player.setXRot((float)-Math.toDegrees(Math.atan2(delta.y,Math.sqrt(delta.horizontalDistanceSqr()))));releaseTick=c.player.tickCount+5;phase=3;
            }else if(phase==3&&c.player.tickCount>=releaseTick&&c.hitResult instanceof EntityHitResult hit&&hit.getEntity().getId()==body.getId()){
                key(c.options.keyUse);releaseTick=c.player.tickCount+2;phase=4;
            }else if(phase==4&&c.player.tickCount>=releaseTick){c.options.keyUse.setDown(false);if(!(c.player.getVehicle() instanceof MachineEntity))return;releaseTick=c.player.tickCount+4;phase=41;
            }else if(phase==41&&c.player.tickCount>=releaseTick&&c.isWindowActive()){
                pending=submit(s,()->{sampling=true;return state(s);});phase=42;
            }else if(phase==42&&pending.isDone()){
                pending.join();c.player.setYRot(0);c.player.setXRot(15);keys(c,index!=1,index==1,index==0,false);key(MachinePresentation.TOGGLE);releaseTick=c.player.tickCount+2;startTick=c.player.tickCount;VIEWPORT.clear();INTERVALS.clear();lastFrame=now;if(requiredViewport==null)requiredViewport=c.getWindow().getWidth()+"x"+c.getWindow().getHeight();shot(c,"native-mounted");phase=5;
            }else if(phase==5){
                check(c.isWindowActive(),"Native input interrupted by focus loss");check(c.getFramerateLimitTracker().getThrottleReason().name().equals("NONE"),"No AFK/window throttle during driving");VIEWPORT.merge(c.getWindow().getWidth()+"x"+c.getWindow().getHeight(),1,Integer::sum);check(VIEWPORT.keySet().equals(Set.of(requiredViewport)),"Viewport changed during terrain measurement");INTERVALS.add(now-lastFrame);lastFrame=now;
                check((index==1?c.options.keyDown:c.options.keyUp).isDown(),"Native drive key released during sample");int ticks=c.player.tickCount-startTick;if(c.player.tickCount>=releaseTick)MachinePresentation.TOGGLE.setDown(false);
                if(index==0){c.options.keyLeft.setDown(ticks<30);c.options.keyRight.setDown(ticks>=30&&ticks<60);}
                if(ticks>=(index==0?90:index==1?65:110)){keys(c,false,false,false,false);pending=submit(s,()->state(s));phase=6;}
            }else if(phase==6&&pending.isDone()){
                var st=pending.join();if(!st.neutral){pending=submit(s,()->state(s));return;}shot(c,"native-course-end");key(MachinePresentation.TOGGLE);releaseTick=c.player.tickCount+2;phase=7;
            }else if(phase==7&&c.player.tickCount>=releaseTick){
                MachinePresentation.TOGGLE.setDown(false);pending=submit(s,()->state(s));phase=71;
            }else if(phase==71&&pending.isDone()){
                var st=pending.join();if(st.enabled){pending=submit(s,()->state(s));return;}stoppedEnergy=st.energy;key(c.options.keyShift);releaseTick=c.player.tickCount+3;phase=8;
            }else if(phase==8&&c.player.tickCount>=releaseTick){c.options.keyShift.setDown(false);if(c.player.getVehicle()!=null)return;releaseTick=c.player.tickCount+10;phase=9;
            }else if(phase==9&&c.player.tickCount>=releaseTick){pending=submit(s,()->finish(s));phase=10;
            }else if(phase==10&&pending.isDone()){
                pending.join();var result=RESULTS.getLast();check(VIEWPORT.values().stream().mapToInt(Integer::intValue).sum()==INTERVALS.size(),"Every frame has a true viewport observation");var sorted=INTERVALS.stream().mapToLong(Long::longValue).sorted().toArray();result.put("viewportFrames",new LinkedHashMap<>(VIEWPORT));result.put("configuredFrameLimit",c.options.framerateLimit().get());result.put("reportedFPS",c.getFps());result.put("cycleP95Ms",sorted[(int)((sorted.length-1)*.95)]/1e6);shot(c,"native-stopped-dismounted");System.out.println("C7 TERRAIN PASS "+SCENES[index]+" paid="+paid+" remaining="+stoppedEnergy);
                if(++index==SCENES.length){write(c,"complete","Four provided controlled courses, not manual natural-terrain or long-term performance verification");phase=11;c.stop();}else phase=0;
            }
        }catch(RuntimeException|AssertionError e){keys(c,false,false,false,false);c.options.keyUse.setDown(false);c.options.keyShift.setDown(false);MachinePresentation.TOGGLE.setDown(false);shot(c,"failure");write(c,"failed",e.toString());throw e;}
    }
}
