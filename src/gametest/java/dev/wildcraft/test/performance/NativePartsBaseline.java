package dev.wildcraft.test.performance;

import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import dev.wildcraft.client.mechanics.MachinePresentation;
import dev.wildcraft.energy.*;
import dev.wildcraft.mechanics.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

/** Provided parts, native R/use/Shift and ordinary world ticks. Never a human survival or foreground benchmark. */
public final class NativePartsBaseline {
    private static final String MODE=System.getProperty("wildcraft.c7.partsMode","rocket");
    private static final boolean ROCKET=MODE.startsWith("rocket"),REOPEN=MODE.endsWith("-reopen");
    private record State(boolean riding,boolean enabled,boolean ground,int fuel,int cooldown,int energy,double y,int kind) {}
    private static final List<Map<String,Object>> ROWS=new ArrayList<>();
    private static final StringBuilder HUD=new StringBuilder(),HUD_RED=new StringBuilder();
    private static int fuelHudFrames,burningOffHudFrames,cooldownHudFrames;
    private static boolean burningShot;
    private static CompletableFuture<State> pending;
    private static MachineEntity body;
    private static volatile boolean sampling;
    private static volatile Throwable serverFailure;
    private static int phase,releaseTick,startTick,launches,paid,burnTicks,lastEnergy,lastFuel,checkpointCooldown;
    private static long boot,firstAt,lastAt;
    private static double peakY;
    private static String viewport;
    private static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
    private static int node(){return ROCKET?2:1;}
    private static CompletableFuture<State> submit(MinecraftServer s,Supplier<State> action){var f=new CompletableFuture<State>();s.execute(()->{try{f.complete(action.get());}catch(Throwable e){f.completeExceptionally(e);}});return f;}
    private static ServerPlayer player(MinecraftServer s){return s.getPlayerList().getPlayers().getFirst();}
    private static State state(MinecraftServer s){return new State(player(s).getVehicle()==body,body.enabled(),body.onGround(),body.rocketFuel(2),body.springCooldown(1),body.energy(),body.getY(),body.kind(node()));}
    private static int count(ServerPlayer p,Item item){int n=0;for(int i=0;i<36;i++)if(p.getInventory().getItem(i).is(item))n+=p.getInventory().getItem(i).getCount();return n;}
    private static ItemStack spring(ServerPlayer p){for(int i=0;i<36;i++)if(p.getInventory().getItem(i).is(MechanicsContent.SPRING))return p.getInventory().getItem(i);throw new AssertionError("No recovered spring");}
    private static void install(ServerPlayer p,int n,ItemStack stack){p.setItemInHand(InteractionHand.MAIN_HAND,stack);check(body.install(p,n,InteractionHand.MAIN_HAND),"Provided real stack installs at node"+n);}
    private static void key(KeyMapping k){k.setDown(true);KeyMapping.click(InputConstants.getKey(k.saveString()));}
    private static void shot(Minecraft c,String name){Screenshot.grab(c.gameDirectory,"c7-parts-"+MODE+"-"+name+".png",c.gameRenderer.mainRenderTarget(),1,m->System.out.println(m.getString()));}
    public static void hudLine(net.minecraft.util.FormattedCharSequence text){
        if(!Boolean.getBoolean("wildcraft.c7.partsBaseline"))return;var c=Minecraft.getInstance();
        if(c.player==null||body==null||!(c.player.getVehicle() instanceof MachineEntity m)||m.getId()!=body.getId())return;
        text.accept((at,style,point)->{HUD.appendCodePoint(point);if(net.minecraft.network.chat.TextColor.RED.equals(style.getColor()))HUD_RED.appendCodePoint(point);return true;});
    }
    private static String compact(String s){return s.replaceAll("\\s","");}
    private static void hudFrame(Minecraft c){
        if(c.player!=null&&c.player.getVehicle() instanceof MachineEntity m){
            if(ROCKET&&m.rocketFuel(2)>0){
                if(compact(HUD.toString()).contains(compact(Component.translatable("machine.wildcraft.fuel",m.rocketFuel(2),80).getString())))fuelHudFrames++;
                if(!m.enabled()&&m.active(2)&&compact(HUD_RED.toString()).contains(compact(Component.translatable("machine.wildcraft.rocket_burning").getString())))burningOffHudFrames++;
            }
            if(!ROCKET&&m.springCooldown(1)>0&&compact(HUD.toString()).contains(compact(Component.translatable("machine.wildcraft.cooldown",m.springCooldown(1)).getString())))cooldownHudFrames++;
        }
        HUD.setLength(0);HUD_RED.setLength(0);
    }
    private static void write(Minecraft c,String status,String message){
        var result=new LinkedHashMap<String,Object>();result.put("status",status);result.put("mode",MODE);result.put("phase",phase);result.put("message",message);result.put("nativeWorldRate",20);result.put("acceleratedGameTest",false);result.put("providedFixtureNotManualSurvival",true);result.put("foregroundPerformanceQualified",false);result.put("burnTicks",burnTicks);result.put("springLaunches",launches);result.put("paidEnergy",paid);result.put("carriedBattery",617);result.put("recoveredCooldown",checkpointCooldown);result.put("peakY",peakY);result.put("rows",new ArrayList<>(ROWS));
        result.put("renderedFuelFrames",fuelHudFrames);result.put("renderedBurningOffFrames",burningOffHudFrames);result.put("renderedCooldownFrames",cooldownHudFrames);result.put("uiRequired",Boolean.getBoolean("wildcraft.c7.partsUIRequired"));
        result.put("pixelWidth",c.getWindow().getWidth());result.put("pixelHeight",c.getWindow().getHeight());result.put("guiWidth",c.getWindow().getGuiScaledWidth());result.put("guiHeight",c.getWindow().getGuiScaledHeight());result.put("language",c.options.languageCode);result.put("configuredFrameLimit",c.options.framerateLimit().get());result.put("reportedFPS",c.getFps());
        if(ROWS.size()>1){double seconds=(lastAt-firstAt)/1e9;result.put("observedSeconds",seconds);result.put("measuredWorldTPS",(ROWS.size()-1)/seconds);}
        try{Files.writeString(c.gameDirectory.toPath().resolve("c7-native-parts.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result)+"\n");}catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}
    }
    public static void serverTick(MinecraftServer s){
        if(!sampling||body==null||body.isRemoved())return;
        try{
            check(s.tickRateManager().tickrate()==20&&!s.isPaused(),"Parts observation needs actual native20TPS");
            int energy=body.energy(),fuel=body.rocketFuel(2),debit=lastEnergy-energy;
            check(debit==0||!ROCKET&&debit==20&&body.active(1),"Only original single-spring20 fee may be debited");
            if(debit>0){paid+=debit;launches++;}
            if(ROCKET){int burned=lastFuel-fuel;check(burned==0||burned==1,"Fuel must decrease by one actual world tick");if(burned==1){burnTicks++;check(body.active(2),"Every committed fuel tick has actual rocket activity");}}
            check(Batteries.energy(player(s).getInventory().getItem(9))==617,"Carried battery must stay separate");
            lastEnergy=energy;lastFuel=fuel;peakY=Math.max(peakY,body.getY());long now=System.nanoTime();if(firstAt==0)firstAt=now;lastAt=now;
            var row=new LinkedHashMap<String,Object>();row.put("worldTick",s.overworld().getGameTime());row.put("phase",phase);row.put("enabled",body.enabled());row.put("working",body.working());row.put("ground",body.onGround());row.put("y",body.getY());row.put("z",body.getZ());row.put("energy",energy);row.put("debit",debit);row.put("fuel",fuel);row.put("cooldown",body.springCooldown(1));row.put("active",body.active(node()));row.put("riding",player(s).getVehicle()==body);ROWS.add(row);
        }catch(RuntimeException|AssertionError e){serverFailure=e;sampling=false;}
    }
    private static State setup(MinecraftServer s){
        var p=player(s);check(s.tickRateManager().tickrate()==20,"No accelerated scheduler");
        if(REOPEN){
            var bodies=s.overworld().getEntitiesOfClass(MachineEntity.class,new AABB(-8,118,-8,8,140,10));check(bodies.size()==1,"Real disk-reopened fixture has one original body");body=bodies.getFirst();check(body.owner().equals(p.getUUID())&&!body.enabled()&&!p.isPassenger(),"Disk retains owner and stopped session");
            check(Batteries.energy(p.getInventory().getItem(9))==617,"Disk retains independent carried battery");
            if(ROCKET){check(count(p,MechanicsContent.SPENT_ROCKET)==1&&body.part(2).isEmpty()&&body.energy()==0,"Disk keeps one recovered inert casing and no installed fuel");return state(s);}
            check(body.energy()==60&&count(p,MechanicsContent.SPRING)==1&&body.part(1).isEmpty(),"Disk retains twice-paid battery and one recovered spring");
            var item=spring(p);var data=item.get(MechanicsContent.SPRING_DATA);check(data!=null&&data.cooldown()>0&&!data.armed(),"Inventory save preserves real cooldown/unarmed spring");checkpointCooldown=data.cooldown();
            // Move, never copy, the saved spring into the empty real hand before installing.
            for(int i=0;i<36;i++)if(p.getInventory().getItem(i)==item)p.getInventory().setItem(i,ItemStack.EMPTY);
            check(p.getMainHandItem().isEmpty(),"Reopen hand is empty before original spring transfer");install(p,1,item);check(body.springCooldown(1)==checkpointCooldown,"Reinstallation does not refresh cooldown");
            lastEnergy=60;lastFuel=0;sampling=true;return state(s);
        }
        p.stopRiding();s.overworld().getEntitiesOfClass(MachineEntity.class,new AABB(-20,110,-20,20,145,25)).forEach(net.minecraft.world.entity.Entity::discard);
        for(int x=-6;x<=6;x++)for(int z=-5;z<=12;z++)for(int y=119;y<=133;y++)s.overworld().setBlockAndUpdate(new BlockPos(x,y,z),(y==119||ROCKET&&z==6&&y<125?Blocks.STONE:Blocks.AIR).defaultBlockState());
        p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();p.teleportTo(s.overworld(),.5,120,.5,Set.of(),0,20,true);p.setDeltaMovement(Vec3.ZERO);
        var carried=new ItemStack(EnergyContent.BATTERY);Batteries.charge(carried,617);p.getInventory().setItem(9,carried);
        body=MechanicsContent.MACHINE.create(s.overworld(),EntitySpawnReason.COMMAND);check(body!=null,"Provided native body");body.setOwner(p.getUUID());body.setPos(.5,120,2.5);body.setYRot(0);s.overworld().addFreshEntity(body);
        if(ROCKET){var item=new ItemStack(MechanicsContent.ROCKET);item.set(DataComponents.CUSTOM_NAME,Component.literal("C7 finite rocket"));install(p,2,item);lastEnergy=0;lastFuel=80;}
        else {var battery=new ItemStack(EnergyContent.BATTERY);Batteries.charge(battery,100);install(p,0,battery);install(p,1,new ItemStack(MechanicsContent.SPRING));lastEnergy=100;lastFuel=0;}
        p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.inventoryMenu.broadcastChanges();sampling=true;return state(s);
    }
    public static void frame(Minecraft c){
        if(!Boolean.getBoolean("wildcraft.c7.partsBaseline"))return;
        hudFrame(c);
        long now=System.nanoTime();if(boot==0)boot=now;c.options.pauseOnLostFocus=false;
        try{
            check(serverFailure==null,"Server tick audit failed: "+serverFailure);check(System.getProperty("fabric.client.gametest")==null,"Ordinary parts client must not use gametest acceleration");check(now-boot<180_000_000_000L,"Native parts timed out in phase"+phase);
            if(c.player==null||c.level==null||c.getSingleplayerServer()==null||c.gui.screen()!=null||c.gui.overlay()!=null||c.isPaused())return;var s=c.getSingleplayerServer();
            if(phase==0){
                c.options.keyUse.setDown(false);c.options.keyShift.setDown(false);MachinePresentation.TOGGLE.setDown(false);c.options.setCameraType(CameraType.THIRD_PERSON_BACK);c.options.renderDistance().set(4);c.options.inactivityFpsLimit().set(InactivityFpsLimit.MINIMIZED);c.options.framerateLimit().set(Integer.getInteger("wildcraft.c7.frameLimit",120));pending=submit(s,()->setup(s));phase=1;
            }else if(phase==1&&pending.isDone()){
                pending.join();if(REOPEN&&ROCKET){shot(c,"disk-casing-conserved");write(c,"complete","Actual native disk reopen keeps the recovered named spent casing, no fuel or battery refill");phase=99;c.stop();return;}
                releaseTick=c.player.tickCount+8;phase=2;
            }else if(phase==2&&c.player.tickCount>=releaseTick){
                var target=c.level.getEntity(body.getId());if(!(target instanceof MachineEntity))return;var d=target.position().add(MachineNodes.point(2)).subtract(c.player.getEyePosition());c.player.setYRot((float)Math.toDegrees(Math.atan2(-d.x,d.z)));c.player.setXRot((float)-Math.toDegrees(Math.atan2(d.y,Math.sqrt(d.horizontalDistanceSqr()))));releaseTick=c.player.tickCount+5;phase=3;
            }else if(phase==3&&c.player.tickCount>=releaseTick&&c.hitResult instanceof EntityHitResult hit&&hit.getEntity().getId()==body.getId()){key(c.options.keyUse);releaseTick=c.player.tickCount+2;phase=4;
            }else if(phase==4&&c.player.tickCount>=releaseTick){c.options.keyUse.setDown(false);if(!(c.player.getVehicle() instanceof MachineEntity))return;releaseTick=c.player.tickCount+4;phase=5;
            }else if(phase==5&&c.player.tickCount>=releaseTick){
                c.player.setYRot(0);c.player.setXRot(15);viewport=c.getWindow().getWidth()+"x"+c.getWindow().getHeight();shot(c,"mounted-before-R");key(MachinePresentation.TOGGLE);releaseTick=c.player.tickCount+2;startTick=c.player.tickCount;phase=6;
            }else if(phase==6){
                check(viewport.equals(c.getWindow().getWidth()+"x"+c.getWindow().getHeight()),"Native parts viewport changed");if(c.player.tickCount>=releaseTick)MachinePresentation.TOGGLE.setDown(false);
                if(c.player.tickCount-startTick<(ROCKET?12:8))return;shot(c,"native-first-action");pending=submit(s,()->{var st=state(s);check(st.enabled&&st.riding,"Real native use/R mounted toggle");
                    if(ROCKET)check(st.fuel>0&&st.fuel<80&&st.energy==0,"Battery-free native finite ignition");
                    else if(REOPEN)check(st.energy==60&&!body.part(1).get(MechanicsContent.SPRING_DATA).armed(),"Saved unarmed spring cannot launch on immediate enable");
                    else check(st.energy==80&&st.cooldown>0&&st.y>120.2,"First native ground launch pays20 and actually rises");return st;});phase=7;
            }else if(phase==7&&pending.isDone()){
                pending.join();if(ROCKET){key(MachinePresentation.TOGGLE);releaseTick=c.player.tickCount+2;phase=8;}else{startTick=c.player.tickCount;phase=20;}
            }else if(phase==8&&c.player.tickCount>=releaseTick){
                MachinePresentation.TOGGLE.setDown(false);pending=submit(s,()->{var st=state(s);check(!st.enabled&&st.fuel>0,"Real R off keeps live finite remainder");check(!body.recover(player(s),2),"Burning rocket cannot be removed even after R off");return st;});phase=9;
            }else if(phase==9&&pending.isDone()){pending.join();shot(c,"burning-after-R-off");startTick=c.player.tickCount;phase=10;
            }else if(phase==10&&c.player.tickCount-startTick>=90){
                if(Boolean.getBoolean("wildcraft.c7.partsUIRequired"))check(fuelHudFrames>2&&burningOffHudFrames>2,"Actual riding HUD must show finite fuel and red still-burning feedback after switch-off: fuel="+fuelHudFrames+", burning="+burningOffHudFrames);
                pending=submit(s,()->{var st=state(s);check(!st.enabled&&!body.working()&&body.part(2).is(MechanicsContent.SPENT_ROCKET)&&st.fuel==0&&burnTicks==80,"Exactly80 native fuel debits end in one inert real casing");check(ROWS.stream().anyMatch(row->Boolean.FALSE.equals(row.get("enabled"))&&((Number)row.get("fuel")).intValue()>0),"Observed burning with switch off");return st;});phase=11;
            }else if(phase==10&&!burningShot&&burningOffHudFrames>=3){burningShot=true;shot(c,"actual-still-burning-HUD");
            }else if(phase==11&&pending.isDone()){pending.join();shot(c,"native-spent");key(c.options.keyShift);phase=12;
            }else if(phase==12){if(c.player.getVehicle()!=null)return;c.options.keyShift.setDown(false);pending=submit(s,()->{var p=player(s);check(body.recover(p,2)&&!body.recover(p,2),"Original API recovery returns casing only once");var item=p.getInventory().getItem(0);check(item.is(MechanicsContent.SPENT_ROCKET)&&item.getHoverName().getString().equals("C7 finite rocket"),"Original named stack survives exhaustion/recovery");check(!body.install(p,2,InteractionHand.MAIN_HAND)&&Batteries.charge(item,1000)==0,"Spent stack cannot install or charge as fresh fuel");check(count(p,MechanicsContent.SPENT_ROCKET)==1&&count(p,MechanicsContent.ROCKET)==0,"No duplicate fresh or spent fuel");p.inventoryMenu.broadcastChanges();sampling=false;return state(s);});phase=13;
            }else if(phase==13&&pending.isDone()){pending.join();shot(c,"once-recovered");write(c,"complete","Actual80 world-tick burn; native use/R/Shift; API once recovery; disk reopening is separate");phase=99;c.stop();
            }else if(phase==20&&c.player.tickCount-startTick>=70){
                pending=submit(s,()->{var st=state(s);check(st.ground&&st.cooldown==0&&st.energy==(REOPEN?60:80),"Landing while still enabled cannot repeat unarmed spring");check(!body.part(1).get(MechanicsContent.SPRING_DATA).armed(),"Spring still needs stopped ground rearm");return st;});phase=21;
            }else if(phase==21&&pending.isDone()){pending.join();shot(c,"grounded-no-auto-repeat");key(MachinePresentation.TOGGLE);releaseTick=c.player.tickCount+4;phase=22;
            }else if(phase==22&&c.player.tickCount>=releaseTick){MachinePresentation.TOGGLE.setDown(false);pending=submit(s,()->{check(!body.enabled()&&body.onGround()&&body.part(1).get(MechanicsContent.SPRING_DATA).armed(),"Actual stopped ground tick rearms cooled spring");return state(s);});phase=23;
            }else if(phase==23&&pending.isDone()){pending.join();key(MachinePresentation.TOGGLE);releaseTick=c.player.tickCount+2;startTick=c.player.tickCount;phase=24;
            }else if(phase==24){if(c.player.tickCount>=releaseTick)MachinePresentation.TOGGLE.setDown(false);if(c.player.tickCount-startTick<7)return;pending=submit(s,()->{var st=state(s);check(st.energy==(REOPEN?40:60)&&st.cooldown>0&&st.y>120.2,"Independent rearmed launch pays exactly20 and rises");return st;});phase=25;
            }else if(phase==25&&pending.isDone()){pending.join();if(Boolean.getBoolean("wildcraft.c7.partsUIRequired"))check(cooldownHudFrames>2,"Actual mounted spring HUD must show live cooldown");shot(c,"rearmed-native-launch");key(MachinePresentation.TOGGLE);releaseTick=c.player.tickCount+2;phase=26;
            }else if(phase==26&&c.player.tickCount>=releaseTick){MachinePresentation.TOGGLE.setDown(false);pending=submit(s,()->{var p=player(s);check(!body.enabled()&&body.recover(p,1)&&!body.recover(p,1),"Original API returns spring once while real cooldown is live");var data=spring(p).get(MechanicsContent.SPRING_DATA);check(data.cooldown()>0&&!data.armed(),"Recovered stack retains actual cooldown and unarmed state");checkpointCooldown=data.cooldown();check(body.energy()==(REOPEN?40:60)&&paid==(REOPEN?20:40)&&launches==(REOPEN?1:2),"Exact independent costs and real launch count");check(count(p,MechanicsContent.SPRING)==1&&count(p,EnergyContent.BATTERY)==1,"One recovered spring and independent carried battery; second battery stays installed");p.inventoryMenu.broadcastChanges();sampling=false;return state(s);});phase=27;
            }else if(phase==27&&pending.isDone()){pending.join();key(c.options.keyShift);phase=28;
            }else if(phase==28){if(c.player.getVehicle()!=null)return;c.options.keyShift.setDown(false);shot(c,"recovered-cooldown-stack");write(c,"complete",REOPEN?"Real disk spring retained cooldown/charge; no immediate refill; native rearm third launch pays20":"Native first/second launches; original cooldown recovery; disk reopening is separate");phase=99;c.stop();}
        }catch(RuntimeException|AssertionError e){sampling=false;c.options.keyUse.setDown(false);c.options.keyShift.setDown(false);MachinePresentation.TOGGLE.setDown(false);shot(c,"failure");write(c,"failed",e.toString());throw e;}
    }
}
