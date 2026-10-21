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
import net.minecraft.network.chat.TextColor;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

/** Native water courses and rendered HUD observations; no replacement movement or resource writes. */
public final class NativeBuoyancyBaseline {
    private static final String[] NAMES={"one-float-light","one-float-loaded","two-floats-loaded"};
    private record State(boolean riding,boolean enabled,int energy,boolean neutral,double y,double mass) {}
    private static final List<Map<String,Object>> RESULTS=new ArrayList<>(),ROWS=new ArrayList<>();
    private static final Map<String,Integer> VIEWPORT=new LinkedHashMap<>();
    private static final StringBuilder RED_TEXT=new StringBuilder();
    private static CompletableFuture<State> pending;
    private static MachineEntity body;
    private static int phase,index,releaseTick,startTick,lastEnergy,paid,stoppedEnergy,hudWarningFrames;
    private static long boot,deadline,firstAt,lastAt;
    private static boolean sampling;
    private static String requiredViewport;
    private static State preBoard;
    private static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
    private static CompletableFuture<State> submit(MinecraftServer s,Supplier<State> action){var f=new CompletableFuture<State>();s.execute(()->{try{f.complete(action.get());}catch(Throwable e){f.completeExceptionally(e);}});return f;}
    private static State state(MinecraftServer s){var p=s.getPlayerList().getPlayers().getFirst();var i=p.getLastClientInput();return new State(p.getVehicle()==body,body.enabled(),body.energy(),!i.forward()&&!i.backward()&&!i.left()&&!i.right(),body.getY(),body.mass());}
    /** Real font calls, sampled only for the actual mounted fixture; split styled fragments are joined per frame. */
    public static void hudLine(FormattedCharSequence text){
        if(!Boolean.getBoolean("wildcraft.c7.waterBaseline")||!sampling)return;var c=Minecraft.getInstance();if(c.player==null||!(c.player.getVehicle() instanceof MachineEntity m)||body==null||m.getId()!=body.getId())return;
        text.accept((at,style,point)->{if(TextColor.RED.equals(style.getColor()))RED_TEXT.appendCodePoint(point);return true;});
    }
    public static void serverTick(MinecraftServer s){
        if(!sampling||body==null||body.isRemoved())return;check(s.tickRateManager().tickrate()==20,"Original20TPS water measurement");var p=s.getPlayerList().getPlayers().getFirst();int energy=body.energy(),debit=lastEnergy-energy,cost=0,floats=0;
        for(int n=0;n<6;n++){if(body.kind(n)==8)floats++;if(body.active(n))cost+=switch(body.kind(n)){case 1,6->1;case 7->2;default->0;};}
        check(debit==0||debit==cost,"Actual native water debit differs from active parts");paid+=debit;lastEnergy=energy;long now=System.nanoTime();if(firstAt==0)firstAt=now;lastAt=now;
        var row=new LinkedHashMap<String,Object>();row.put("tick",s.overworld().getGameTime());row.put("y",body.getY());row.put("z",body.getZ());row.put("mass",body.mass());row.put("floats",floats);row.put("fluidHeight",body.getFluidHeight(FluidTags.WATER));row.put("ground",body.onGround());row.put("riding",p.getVehicle()==body);row.put("energy",energy);row.put("debit",debit);row.put("activeCost",cost);row.put("enabled",body.enabled());ROWS.add(row);
    }
    private static void key(KeyMapping k){k.setDown(true);KeyMapping.click(InputConstants.getKey(k.saveString()));}
    private static void shot(Minecraft c,String suffix){Screenshot.grab(c.gameDirectory,"c7-water-"+NAMES[index]+"-"+suffix+".png",c.gameRenderer.mainRenderTarget(),1,m->System.out.println(m.getString()));}
    private static void write(Minecraft c,String status,String message){try{Files.writeString(c.gameDirectory.toPath().resolve("c7-native-water.json"),new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("status",status,"message",message,"phase",phase,"providedWaterPoolNotNaturalSurvival",true,"physicsChanged",false,"requireRenderedUI",Boolean.getBoolean("wildcraft.c7.waterUIRequired"),"scenes",RESULTS,"partialRows",new ArrayList<>(ROWS)))+"\n");}catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}}
    private static double value(Map<String,Object> r,String k){return ((Number)r.get(k)).doubleValue();}
    private static State finish(MinecraftServer s){
        sampling=false;var st=state(s);var p=s.getPlayerList().getPlayers().getFirst();check(!st.riding&&!st.enabled&&st.neutral,"Real R/Shift stop and dismount");check(st.energy==stoppedEnergy&&1000-st.energy==paid&&paid>0,"Finite water battery is conserved");check(Batteries.energy(p.getInventory().getItem(9))==617,"Carried617 remains separate");
        var loaded=ROWS.stream().filter(row->Boolean.TRUE.equals(row.get("riding"))).toList();check(loaded.size()>80,"Native loaded water observation is complete");double minY=loaded.stream().mapToDouble(row->value(row,"y")).min().orElseThrow();double lastZ=value(loaded.getLast(),"z");check(lastZ>6,"Original fan produces actual water travel");check(loaded.stream().allMatch(row->value(row,"fluidHeight")>0),"Actual water contact throughout loaded sample");
        if(index==1){check(preBoard.mass<1.75&&preBoard.y>120.2,"Heavy fixture floats before actual passenger boards");check(loaded.stream().allMatch(row->value(row,"mass")>1.75),"Actual passenger overloads single float");check(minY<118.1,"Overloaded native assembly actually sinks to pool floor");if(Boolean.getBoolean("wildcraft.c7.waterUIRequired"))check(hudWarningFrames>2,"Actual overloaded machine HUD lacks rendered buoyancy warning");}
        else {check(loaded.stream().noneMatch(row->Boolean.TRUE.equals(row.get("ground")))&&value(loaded.getLast(),"y")>120,"Adequate original flotation never rests on pool floor and returns to the surface");check(hudWarningFrames==0,"Adequate flotation has no false overload warning");}
        var result=new LinkedHashMap<String,Object>();result.put("scene",NAMES[index]);result.put("preBoardY",preBoard.y);result.put("preBoardMass",preBoard.mass);result.put("minLoadedY",minY);result.put("lastLoadedZ",lastZ);result.put("loadedTicks",loaded.size());result.put("observedSeconds",(lastAt-firstAt)/1e9);result.put("measuredWorldTPS",(ROWS.size()-1)/((lastAt-firstAt)/1e9));result.put("paidEnergy",paid);result.put("remainingEnergy",st.energy);result.put("carriedBattery",617);result.put("renderedOverloadFrames",hudWarningFrames);result.put("rows",new ArrayList<>(ROWS));RESULTS.add(result);return st;
    }
    public static void frame(Minecraft c){
        if(!Boolean.getBoolean("wildcraft.c7.waterBaseline"))return;
        String warning=Component.translatable("machine.wildcraft.buoyancy_low").getString().replaceAll("\\s","");if(RED_TEXT.toString().replaceAll("\\s","").contains(warning))hudWarningFrames++;RED_TEXT.setLength(0);
        long now=System.nanoTime();if(boot==0)boot=now;c.options.pauseOnLostFocus=false;
        try{
            check(System.getProperty("fabric.client.gametest")==null,"Native water uses no accelerated scheduler");check(now-boot<240_000_000_000L,"Water probe timed out in phase "+phase);if(c.player==null||c.level==null||c.getSingleplayerServer()==null||c.gui.screen()!=null||c.gui.overlay()!=null||c.isPaused())return;var s=c.getSingleplayerServer();
            if(phase==0){
                c.options.keyUp.setDown(false);c.options.keyShift.setDown(false);c.options.keyUse.setDown(false);MachinePresentation.TOGGLE.setDown(false);c.options.enableVsync().set(false);c.options.inactivityFpsLimit().set(InactivityFpsLimit.MINIMIZED);c.options.renderDistance().set(4);c.options.framerateLimit().set(Integer.getInteger("wildcraft.c7.frameLimit",120));c.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                pending=submit(s,()->{
                    sampling=false;var p=s.getPlayerList().getPlayers().getFirst();p.stopRiding();if(body!=null)body.discard();body=null;
                    for(int x=-11;x<=11;x++)for(int z=-9;z<=71;z++)for(int y=117;y<=122;y++){
                        boolean edge=x==-11||x==11||z==-9||z==71;var block=y==117||edge&&y<=121?Blocks.STONE:!edge&&y<=120?Blocks.WATER:Blocks.AIR;s.overworld().setBlockAndUpdate(new BlockPos(x,y,z),block.defaultBlockState());
                    }
                    for(int x=-1;x<=1;x++)for(int z=-2;z<=0;z++)for(int y=118;y<=120;y++)s.overworld().setBlockAndUpdate(new BlockPos(x,y,z),Blocks.STONE.defaultBlockState());
                    p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();p.teleportTo(s.overworld(),.5,121,.5,Set.of(),0,0,true);p.setDeltaMovement(Vec3.ZERO);var carried=new ItemStack(EnergyContent.BATTERY);Batteries.charge(carried,617);p.getInventory().setItem(9,carried);
                    body=MechanicsContent.MACHINE.create(s.overworld(),EntitySpawnReason.COMMAND);check(body!=null,"Water body created");body.setPos(.5,120,2.5);body.setYRot(0);body.setOwner(p.getUUID());s.overworld().addFreshEntity(body);
                    var battery=new ItemStack(EnergyContent.BATTERY);Batteries.charge(battery,1000);p.setItemInHand(InteractionHand.MAIN_HAND,battery);check(body.install(p,0,InteractionHand.MAIN_HAND),"Provided battery installed once");
                    int[] nodes=index==0?new int[]{1,2,4}:new int[]{1,2,3,4,5};for(int node:nodes){var item=node==1||index==2&&node==4?MechanicsContent.BUOYANCY:node==2?MechanicsContent.FAN:node==3?MechanicsContent.STABILIZER:MechanicsContent.WHEEL;p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item));check(body.install(p,node,InteractionHand.MAIN_HAND),"Provided water part at node"+node);}
                    p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.inventoryMenu.broadcastChanges();ROWS.clear();paid=0;lastEnergy=1000;firstAt=lastAt=0;return state(s);
                });phase=1;
            }else if(phase==1&&pending.isDone()){pending.join();deadline=now+5_000_000_000L;phase=2;
            }else if(phase==2&&now>=deadline){pending=submit(s,()->{sampling=true;return state(s);});phase=21;
            }else if(phase==21&&pending.isDone()){
                preBoard=pending.join();var target=c.level.getEntity(body.getId());if(!(target instanceof MachineEntity))return;var d=target.position().add(MachineNodes.point(2)).subtract(c.player.getEyePosition());c.player.setYRot((float)Math.toDegrees(Math.atan2(-d.x,d.z)));c.player.setXRot((float)-Math.toDegrees(Math.atan2(d.y,Math.sqrt(d.horizontalDistanceSqr()))));releaseTick=c.player.tickCount+5;phase=3;
            }else if(phase==3&&c.player.tickCount>=releaseTick&&c.hitResult instanceof EntityHitResult hit&&hit.getEntity().getId()==body.getId()){key(c.options.keyUse);releaseTick=c.player.tickCount+2;phase=4;
            }else if(phase==4&&c.player.tickCount>=releaseTick){c.options.keyUse.setDown(false);if(!(c.player.getVehicle() instanceof MachineEntity))return;releaseTick=c.player.tickCount+4;phase=41;
            }else if(phase==41&&c.player.tickCount>=releaseTick&&c.isWindowActive()){
                c.player.setYRot(0);c.player.setXRot(15);c.options.keyUp.setDown(true);key(MachinePresentation.TOGGLE);releaseTick=c.player.tickCount+2;startTick=c.player.tickCount;hudWarningFrames=0;VIEWPORT.clear();if(requiredViewport==null)requiredViewport=c.getWindow().getWidth()+"x"+c.getWindow().getHeight();shot(c,"native-mounted");phase=5;
            }else if(phase==5){
                check(c.isWindowActive()&&c.options.keyUp.isDown(),"Native water input interrupted");check(c.player.getVehicle() instanceof MachineEntity,"Native passenger left before measurement ended");check(c.getFramerateLimitTracker().getThrottleReason().name().equals("NONE"),"Water sample cannot be idle-throttled");VIEWPORT.merge(c.getWindow().getWidth()+"x"+c.getWindow().getHeight(),1,Integer::sum);check(VIEWPORT.keySet().equals(Set.of(requiredViewport)),"Water viewport changed");if(c.player.tickCount>=releaseTick)MachinePresentation.TOGGLE.setDown(false);
                if(c.player.tickCount-startTick>=120){c.options.keyUp.setDown(false);pending=submit(s,()->state(s));phase=6;}
            }else if(phase==6&&pending.isDone()){var st=pending.join();if(!st.neutral){pending=submit(s,()->state(s));return;}shot(c,"native-water-result");key(MachinePresentation.TOGGLE);releaseTick=c.player.tickCount+2;phase=7;
            }else if(phase==7&&c.player.tickCount>=releaseTick){MachinePresentation.TOGGLE.setDown(false);pending=submit(s,()->state(s));phase=71;
            }else if(phase==71&&pending.isDone()){var st=pending.join();if(st.enabled){pending=submit(s,()->state(s));return;}stoppedEnergy=st.energy;key(c.options.keyShift);releaseTick=c.player.tickCount+3;phase=8;
            }else if(phase==8&&c.player.tickCount>=releaseTick){c.options.keyShift.setDown(false);if(c.player.getVehicle()!=null)return;releaseTick=c.player.tickCount+10;phase=9;
            }else if(phase==9&&c.player.tickCount>=releaseTick){pending=submit(s,()->finish(s));phase=10;
            }else if(phase==10&&pending.isDone()){
                pending.join();RESULTS.getLast().put("viewportFrames",new LinkedHashMap<>(VIEWPORT));RESULTS.getLast().put("configuredFrameLimit",c.options.framerateLimit().get());RESULTS.getLast().put("reportedFPS",c.getFps());RESULTS.getLast().put("guiWidth",c.getWindow().getGuiScaledWidth());RESULTS.getLast().put("guiHeight",c.getWindow().getGuiScaledHeight());shot(c,"native-stopped-dismounted");System.out.println("C7 WATER PASS "+NAMES[index]+" paid="+paid+" hud="+hudWarningFrames);
                if(++index==NAMES.length){write(c,"complete","Native provided-water observation; not manual survival or long-term performance");phase=11;c.stop();}else phase=0;
            }
        }catch(RuntimeException|AssertionError e){c.options.keyUp.setDown(false);c.options.keyShift.setDown(false);c.options.keyUse.setDown(false);MachinePresentation.TOGGLE.setDown(false);shot(c,"failure");write(c,"failed",e.toString());throw e;}
    }
}
