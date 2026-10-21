package dev.wildcraft.test.performance;

import com.google.gson.GsonBuilder;
import dev.wildcraft.mechanics.*;
import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

/** Ordinary renderer and integrated-server scheduling. Six short stationary control scenes, no gameplay or long-run claim. */
public final class NativeRenderBaseline {
    private record Snapshot(long at,long worldTicks,int bodies,float rate,long gcMillis) { }
    private static final int[] SIZES={0,4,16,16,4,0};
    private static final boolean CONTROLLED_ONLY=Boolean.getBoolean("wildcraft.c7.controlledOnly");
    private static final int FIRST_INDEX=CONTROLLED_ONLY?3:0;
    private static final int SAMPLE_SECONDS=Integer.getInteger("wildcraft.c7.sampleSeconds",15);
    private static final int ROUNDS=Integer.getInteger("wildcraft.c7.rounds",1);
    private static final Map<String,Integer> THROTTLE_FRAMES=new LinkedHashMap<>();
    private static final Map<String,Integer> VIEWPORT_FRAMES=new LinkedHashMap<>(),FOCUS_FRAMES=new LinkedHashMap<>();
    private static final Map<String,Integer> NATIVE_FOCUS_FRAMES=new LinkedHashMap<>();
    private static String requiredViewport;
    private static boolean resizeProbeTriggered,focusProbeTriggered;
    private static int actualRenderDistance;
    private static final List<Map<String,Object>> RESULTS=new ArrayList<>();
    private static final List<Long> INTERVALS=new ArrayList<>();
    private static CompletableFuture<Snapshot> pending;
    private static Snapshot before;
    private static int phase,index=FIRST_INDEX,frames,round;
    private static long deadline,previous,bootAt;
    private static boolean bootCaptured;
    private static CompletableFuture<Snapshot> submit(MinecraftServer s,Supplier<Snapshot> action){
        var result=new CompletableFuture<Snapshot>();s.execute(()->{try{result.complete(action.get());}catch(Throwable e){result.completeExceptionally(e);}});return result;
    }
    private static long gcMillis(){return ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(b->Math.max(0,b.getCollectionTime())).sum();}
    private static Snapshot sample(MinecraftServer s){return new Snapshot(System.nanoTime(),s.overworld().getGameTime(),s.overworld().getEntitiesOfClass(MachineEntity.class,new AABB(-30,110,-30,30,130,30)).size(),s.tickRateManager().tickrate(),gcMillis());}
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private static void write(Minecraft c,Map<String,Object> data){try{Files.writeString(c.gameDirectory.toPath().resolve("c7-native-render-baseline.json"),new GsonBuilder().setPrettyPrinting().create().toJson(data)+"\n");}catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}}
    public static void frame(Minecraft c){
        if(!Boolean.getBoolean("wildcraft.c7.renderBaseline"))return;
        long now=System.nanoTime();if(bootAt==0)bootAt=now;
        c.options.pauseOnLostFocus=false;
        try{
            if(!bootCaptured&&now-bootAt>10_000_000_000L){bootCaptured=true;System.out.println("C7 NATIVE BOOT screen="+(c.gui.screen()==null?"none":c.gui.screen().getClass().getName()+" title="+c.gui.screen().getTitle().getString())+" overlay="+(c.gui.overlay()==null?"none":c.gui.overlay().getClass().getName())+" world="+(c.level!=null));Screenshot.grab(c.gameDirectory,"c7-native-boot-state.png",c.gameRenderer.mainRenderTarget(),1,m->System.out.println(m.getString()));}
            check(System.getProperty("fabric.client.gametest")==null,"This baseline must not use the accelerated client-gametest scheduler");
            check(now-bootAt<(180L+(SIZES.length-FIRST_INDEX)*ROUNDS*(SAMPLE_SECONDS+5L))*1_000_000_000L,"Native quickplay/probe timed out; no performance success inferred");
            if(c.player==null||c.level==null||c.getSingleplayerServer()==null||c.gui.screen()!=null||c.gui.overlay()!=null||c.isPaused())return;
            var server=c.getSingleplayerServer();
            if(phase==0){
                c.options.setCameraType(CameraType.THIRD_PERSON_BACK);c.options.framerateLimit().set(120);
                actualRenderDistance=Integer.getInteger("wildcraft.c7.renderDistance",16);
                c.options.renderDistance().set(actualRenderDistance);
                c.options.inactivityFpsLimit().set(index<3?InactivityFpsLimit.AFK:InactivityFpsLimit.MINIMIZED);
                c.player.setYRot(0);c.player.setXRot(0);
                check(c.options.renderDistance().get()==actualRenderDistance,"Actual render distance must match the controlled setting");
                pending=submit(server,()->{var p=server.getPlayerList().getPlayers().getFirst();check(server.tickRateManager().tickrate()==20&&!server.isPaused(),"Native world is actually20 TPS before fixture");
                    server.overworld().getEntitiesOfClass(MachineEntity.class,new AABB(-30,110,-30,30,130,30)).forEach(net.minecraft.world.entity.Entity::discard);
                    for(int x=-16;x<=16;x++)for(int z=-8;z<=24;z++)server.overworld().setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());
                    p.teleportTo(server.overworld(),.5,120,.5,Set.of(),0,0,true);p.setDeltaMovement(Vec3.ZERO);
                    for(int n=0;n<SIZES[index];n++){var body=MechanicsContent.MACHINE.create(server.overworld(),EntitySpawnReason.COMMAND);check(body!=null,"Native body created");body.setPos(-5.5+(n%4)*4,120,5.5+(n/4)*4);body.setYRot(0);body.setOwner(p.getUUID());server.overworld().addFreshEntity(body);check(!body.enabled()&&body.energy()==0,"Stopped empty body fixture, no complex active physics");}return sample(server);});phase=1;
            }else if(phase==1&&pending.isDone()){pending.join();deadline=now+5_000_000_000L;phase=2;
            }else if(phase==2&&now>=deadline){pending=submit(server,()->sample(server));phase=3;
            }else if(phase==3&&pending.isDone()){before=pending.join();check(before.bodies()==SIZES[index]&&before.rate()==20,"Actual measured body count and native rate");deadline=Math.max(now,before.at())+SAMPLE_SECONDS*1_000_000_000L;INTERVALS.clear();THROTTLE_FRAMES.clear();VIEWPORT_FRAMES.clear();FOCUS_FRAMES.clear();NATIVE_FOCUS_FRAMES.clear();if(requiredViewport==null)requiredViewport=c.getWindow().getWidth()+"x"+c.getWindow().getHeight();previous=0;frames=0;phase=4;
            }else if(phase==4){
                VIEWPORT_FRAMES.merge(c.getWindow().getWidth()+"x"+c.getWindow().getHeight(),1,Integer::sum);
                FOCUS_FRAMES.merge(c.isWindowActive()?"active":"inactive",1,Integer::sum);
                if(Boolean.getBoolean("wildcraft.c7.nativeFocusAudit")||Boolean.getBoolean("wildcraft.c7.requireFocus")){
                    long flags=org.lwjgl.sdl.SDLVideo.SDL_GetWindowFlags(c.getWindow().handle());
                    long keyboard=org.lwjgl.sdl.SDLKeyboard.SDL_GetKeyboardFocus();
                    String observation="input="+((flags&org.lwjgl.sdl.SDLVideo.SDL_WINDOW_INPUT_FOCUS)!=0)+", keyboard="+(keyboard==c.getWindow().handle()?"self":keyboard==0?"none":"other")+", hidden="+((flags&org.lwjgl.sdl.SDLVideo.SDL_WINDOW_HIDDEN)!=0)+", minimized="+((flags&org.lwjgl.sdl.SDLVideo.SDL_WINDOW_MINIMIZED)!=0)+", active="+c.isWindowActive()+", focused="+c.getWindow().isFocused()+", iconified="+c.getWindow().isIconified();
                    if(!NATIVE_FOCUS_FRAMES.containsKey(observation))System.out.println("C7 NATIVE FOCUS pid="+ProcessHandle.current().pid()+" observation="+observation);
                    NATIVE_FOCUS_FRAMES.merge(observation,1,Integer::sum);
                    if(Boolean.getBoolean("wildcraft.c7.requireFocus"))check((flags&org.lwjgl.sdl.SDLVideo.SDL_WINDOW_INPUT_FOCUS)!=0&&keyboard==c.getWindow().handle()&&(flags&(org.lwjgl.sdl.SDLVideo.SDL_WINDOW_HIDDEN|org.lwjgl.sdl.SDLVideo.SDL_WINDOW_MINIMIZED))==0,"Independent SDL focus rejected native sample: "+observation);
                }
                if(Boolean.getBoolean("wildcraft.c7.requireFocus"))check(c.isWindowActive()&&c.getWindow().isFocused()&&!c.getWindow().isIconified(),"Focused native sample lost actual focus: active="+c.isWindowActive()+", focused="+c.getWindow().isFocused()+", iconified="+c.getWindow().isIconified());
                if(Boolean.getBoolean("wildcraft.c7.focusLossProbe")&&!focusProbeTriggered&&now-before.at()>1_000_000_000L){focusProbeTriggered=true;check(org.lwjgl.sdl.SDLVideo.SDL_MinimizeWindow(c.getWindow().handle()),"SDL cannot minimize isolated probe window");}
                check(VIEWPORT_FRAMES.keySet().equals(Set.of(requiredViewport)),"Viewport changed during native sample; incomparable FPS: required="+requiredViewport+", observed="+VIEWPORT_FRAMES);
                if(Boolean.getBoolean("wildcraft.c7.viewportChangeProbe")&&!resizeProbeTriggered&&now-before.at()>1_000_000_000L){resizeProbeTriggered=true;c.getWindow().setWindowed(900,600);}
                THROTTLE_FRAMES.merge(c.getFramerateLimitTracker().getThrottleReason().name(),1,Integer::sum);if(previous!=0)INTERVALS.add(now-previous);previous=now;frames++;if(now>=deadline){pending=submit(server,()->sample(server));phase=5;}
            }else if(phase==5&&pending.isDone()){
                var after=pending.join();double seconds=(after.at()-before.at())/1e9;long ticks=after.worldTicks()-before.worldTicks();double tps=ticks/seconds;
                check(after.rate()==20&&after.bodies()==SIZES[index]&&frames>40&&seconds>=SAMPLE_SECONDS,"Ordinary native measurement reaches actual interval and counts: seconds="+seconds+", rate="+after.rate()+", bodies="+after.bodies()+", expected="+SIZES[index]+", frames="+frames);check(tps>15&&tps<23,"Reported world advancement is normal scheduling, not accelerated gametest: "+tps);
                var sorted=INTERVALS.stream().mapToLong(Long::longValue).sorted().toArray();var r=new LinkedHashMap<String,Object>();r.put("requireFocus",Boolean.getBoolean("wildcraft.c7.requireFocus"));r.put("round",round+1);r.put("stoppedEmptyBodies",SIZES[index]);r.put("actualSeconds",seconds);r.put("worldTicks",ticks);r.put("measuredWorldTPS",tps);r.put("nativeTickRate",after.rate());r.put("clientRunTickCycles",frames);r.put("cyclesPerSecond",frames/seconds);r.put("cycleMedianMs",sorted[sorted.length/2]/1e6);r.put("cycleP95Ms",sorted[(int)Math.floor((sorted.length-1)*.95)]/1e6);r.put("reportedFPSAtEnd",c.getFps());r.put("heapUsedBytes",Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());r.put("heapMaxBytes",Runtime.getRuntime().maxMemory());r.put("gcMillis",after.gcMillis()-before.gcMillis());r.put("pixelWidth",c.getWindow().getWidth());r.put("pixelHeight",c.getWindow().getHeight());r.put("renderDistance",c.options.renderDistance().get());
                r.put("inactivityFpsLimit",c.options.inactivityFpsLimit().get().name());
                r.put("throttleReasonFrames",new LinkedHashMap<>(THROTTLE_FRAMES));r.put("viewportFrames",new LinkedHashMap<>(VIEWPORT_FRAMES));r.put("focusFrames",new LinkedHashMap<>(FOCUS_FRAMES));
                if(Boolean.getBoolean("wildcraft.c7.nativeFocusAudit")||Boolean.getBoolean("wildcraft.c7.requireFocus"))r.put("nativeFocusFrames",new LinkedHashMap<>(NATIVE_FOCUS_FRAMES));
                r.put("effectiveFrameLimitAtEnd",c.getFramerateLimitTracker().getFramerateLimit());
                r.put("throttleReasonAtEnd",c.getFramerateLimitTracker().getThrottleReason().name());
                r.put("windowIconified",c.getWindow().isIconified());r.put("windowFocused",c.getWindow().isFocused());
                check(!c.getWindow().isIconified(),"Minimized window invalidates a render baseline");
                if(index>=3)check(THROTTLE_FRAMES.keySet().equals(Set.of("NONE")),"Controlled MINIMIZED-only samples must have no inactive throttle: "+THROTTLE_FRAMES);
                RESULTS.add(r);
                Screenshot.grab(c.gameDirectory,"c7-native-round-"+(round+1)+"-stopped-bodies-"+SIZES[index]+"-"+c.options.inactivityFpsLimit().get().name()+".png",c.gameRenderer.mainRenderTarget(),1,m->System.out.println("C7 native baseline screenshot "+m.getString()));System.out.println("C7 NATIVE BASELINE "+r);
                if(++index==SIZES.length&&++round==ROUNDS){write(c,Map.of("status","complete","nativeScheduler",true,"acceleratedGameTest",false,"fixture","copied generated test world; stationary empty stopped bodies; 5s warmup; sampleSeconds per sample; controlled-only skips AFK; actual render setting pinned","longTermPerformanceVerified",false,"sampleSeconds",SAMPLE_SECONDS,"rounds",ROUNDS,"controlledOnly",CONTROLLED_ONLY,"scenes",RESULTS));phase=6;c.stop();}else{if(index==SIZES.length)index=FIRST_INDEX;phase=0;}
            }
        }catch(RuntimeException|AssertionError e){write(c,Map.of("status","failed","message",e.toString(),"partialScenes",RESULTS,"requiredViewport",String.valueOf(requiredViewport),"viewportFrames",new LinkedHashMap<>(VIEWPORT_FRAMES),"focusFrames",new LinkedHashMap<>(FOCUS_FRAMES),"nativeFocusFrames",new LinkedHashMap<>(NATIVE_FOCUS_FRAMES)));Screenshot.grab(c.gameDirectory,"c7-native-guard-failure.png",c.gameRenderer.mainRenderTarget(),1,m->System.out.println(m.getString()));throw e;}
    }
}
