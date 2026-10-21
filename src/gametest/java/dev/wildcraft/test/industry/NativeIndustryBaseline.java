package dev.wildcraft.test.industry;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import dev.wildcraft.client.energy.ChargerScreen;
import dev.wildcraft.client.fabrication.FabricatorScreen;
import dev.wildcraft.energy.*;
import dev.wildcraft.fabrication.*;
import dev.wildcraft.test.industry.mixin.NativeIndustryConnectionAccess;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

/** Ordinary native block menus and finite resources. Initial provided fixtures, no replacement physics or time. */
public final class NativeIndustryBaseline implements ModInitializer {
    private static final BlockPos A=new BlockPos(0,120,2),B=new BlockPos(2,120,2),SOURCE=new BlockPos(1,120,2),FACTORY=new BlockPos(1,120,-2);
    private static final boolean RESUME=Boolean.getBoolean("wildcraft.c7.industryResume");
    private static final List<Map<String,Object>> ROWS=new ArrayList<>();
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    private static CompletableFuture<Map<String,Object>> pending;
    private static ItemStack expected=ItemStack.EMPTY;
    private static Path gameRoot;
    private static volatile boolean sampling,checkpointOnStop;
    private static volatile Throwable serverFailure;
    private static volatile int phase;
    private static int tickTarget,lastTotal,charged,lastProgress=-1,loggedPhase=-1;
    private static long boot,firstAt,lastAt,lastOpenAt,failureAt;
    private static String diagnosticFailure;
    private static int openingFromId;
    private static Map<String,Object> initialResume;
    private static String ownerId;
    @Override public void onInitialize(){if(Boolean.getBoolean("wildcraft.c7.industryFuse")){NativeFuseProbe.initialize();return;}if(Boolean.getBoolean("wildcraft.c7.industryTraversal")){NativeTraversalProbe.initialize();return;}if(Boolean.getBoolean("wildcraft.c7.industryBoundary")){NativeCookingBoundary.initialize();return;}if(Boolean.getBoolean("wildcraft.c7.industryRawCooking")){NativeRawCooking.initialize();return;}if(Boolean.getBoolean("wildcraft.c7.industryMealPause")){NativeMealPause.initialize();return;}ServerTickEvents.END_SERVER_TICK.register(NativeIndustryBaseline::serverTick);ServerLifecycleEvents.SERVER_STOPPING.register(NativeIndustryBaseline::stopping);}
    private static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
    private static ServerPlayer player(MinecraftServer s){return s.getPlayerList().getPlayers().getFirst();}
    private static ChargerEntity charger(MinecraftServer s,BlockPos pos){return (ChargerEntity)s.overworld().getBlockEntity(pos);}
    private static FabricatorEntity factory(MinecraftServer s){return (FabricatorEntity)s.overworld().getBlockEntity(FACTORY);}
    private static int inventoryEnergy(ServerPlayer p){int n=0;for(int i=0;i<36;i++)n+=Batteries.energy(p.getInventory().getItem(i));return n;}
    private static int total(MinecraftServer s){return charger(s,A).artEnergy()+charger(s,B).artEnergy()+inventoryEnergy(player(s));}
    private static JsonElement encode(MinecraftServer s,ItemStack stack){return ItemStack.CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE,s.registryAccess()),stack).getOrThrow();}
    private static Map<String,Object> state(MinecraftServer s){
        var f=factory(s);var row=new LinkedHashMap<String,Object>();row.put("worldTick",s.overworld().getGameTime());row.put("serverPosition",player(s).position().toString());row.put("serverOnGround",player(s).onGround());row.put("serverFloor",s.overworld().getBlockState(new BlockPos(1,119,0)).toString());row.put("energyA",charger(s,A).artEnergy());row.put("energyB",charger(s,B).artEnergy());row.put("inventoryEnergy",inventoryEnergy(player(s)));row.put("totalEnergy",total(s));row.put("chargedEnergy",charged);row.put("clientLoaded",player(s).connection.hasClientLoaded());row.put("awaitingTeleport",((NativeIndustryConnectionAccess)player(s).connection).wildcraft$awaitingTeleport()!=null);row.put("serverMenu",player(s).containerMenu.getClass().getSimpleName());row.put("copper",f.getItem(0).getCount());row.put("redstone",f.getItem(1).getCount());row.put("hasJob",f.hasJob());row.put("progress",f.progress());row.put("output",f.getItem(2).isEmpty()?"empty":BuiltInRegistries.ITEM.getKey(f.getItem(2).getItem()).toString());return row;
    }
    private static CompletableFuture<Map<String,Object>> submit(MinecraftServer s,Supplier<Map<String,Object>> a){var f=new CompletableFuture<Map<String,Object>>();s.execute(()->{try{f.complete(a.get());}catch(Throwable e){f.completeExceptionally(e);}});return f;}
    private static void write(Path path,Object value){try{Files.writeString(path,GSON.toJson(value)+"\n");}catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}}
    private static void report(Minecraft c,String status,String message){
        var data=new LinkedHashMap<String,Object>();data.put("status",status);data.put("message",message);data.put("resume",RESUME);data.put("phase",phase);data.put("chargedEnergy",charged);data.put("providedFixtureNotNaturalAcquisition",true);data.put("acceleratedGameTest",false);data.put("researchRegistryLoaded",false);data.put("foregroundPerformanceQualified",false);data.put("initialResume",initialResume);data.put("rows",new ArrayList<>(ROWS));
        if(ROWS.size()>1){double seconds=(lastAt-firstAt)/1e9;data.put("observedSeconds",seconds);data.put("measuredWorldTPS",(ROWS.size()-1)/seconds);}write(c.gameDirectory.toPath().resolve("c7-native-industry.json"),data);
    }
    private static void stopping(MinecraftServer s){
        if(!checkpointOnStop)return;sampling=false;var f=factory(s);check(f.hasJob()&&ItemStack.matches(expected,f.pendingResult()),"Stopping still retains exact originally paid result");
        // Native disconnect may already have removed the player list; only the committed block state is read here.
        var data=new LinkedHashMap<String,Object>();data.put("worldTick",s.overworld().getGameTime());data.put("progress",f.progress());data.put("copper",f.getItem(0).getCount());data.put("redstone",f.getItem(1).getCount());data.put("result",encode(s,expected));data.put("playerUUID",ownerId);data.put("checkpointAt", "native SERVER_STOPPING, before world persistence, no extra world tick");write(gameRoot.resolve("c7-industry-stop.json"),data);
    }
    private static void serverTick(MinecraftServer s){
        if(!sampling)return;
        try{
            check(s.tickRateManager().tickrate()==20&&!s.isPaused(),"Industry audit needs ordinary unpaused20TPS");check(Batteries.energy(player(s).getInventory().getItem(9))==617,"Independent carried battery remains617");
            int nowTotal=total(s),delta=nowTotal-lastTotal;check(delta==0||delta==FixedEnergy.OUTPUT_PER_TICK,"One fixed source has one total20 budget per world tick, not one per charger: "+delta);charged+=delta;lastTotal=nowTotal;
            var a=charger(s,A);var b=charger(s,B);if(a.getItem(0).is(EnergyContent.BATTERY)&&b.getItem(0).is(EnergyContent.BATTERY))check(Math.abs(a.artEnergy()-b.artEnergy())<=20,"Shared-source native alternation is fair");
            if(!expected.isEmpty()){
                var f=factory(s);check(f.getItem(0).getCount()==4&&f.getItem(1).getCount()==2,"Committed factory job cannot debit provided input again");
                int progress=f.hasJob()?f.progress():f.getItem(2).isEmpty()?0:100;
                if(f.hasJob())check(ItemStack.matches(expected,f.pendingResult()),"Pending result cannot reroll");else if(!f.getItem(2).isEmpty())check(ItemStack.matches(expected,f.getItem(2)),"Completed actual stack matches original full components");
                if(lastProgress>=0&&progress>0)check(progress==lastProgress||progress==lastProgress+1,"Factory advances at most one per actual native world tick");lastProgress=progress;
            }
            long now=System.nanoTime();if(firstAt==0)firstAt=now;lastAt=now;var row=new LinkedHashMap<String,Object>(state(s));row.put("debitOrCharge",delta);row.put("phase",phase);ROWS.add(row);
        }catch(RuntimeException|AssertionError e){serverFailure=e;sampling=false;}
    }
    private static Map<String,Object> setup(MinecraftServer s){
        var p=player(s);if(!p.connection.hasClientLoaded())return Map.of("ready",false);ownerId=p.getUUID().toString();
        if(RESUME){
            try{
                var prior=JsonParser.parseString(Files.readString(Path.of(System.getProperty("wildcraft.c7.industryCheckpoint")))).getAsJsonObject();expected=ItemStack.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE,s.registryAccess()),prior.get("result")).getOrThrow();
                check(p.getUUID().toString().equals(prior.get("playerUUID").getAsString()),"Same actual player reopens original provided world");check(total(s)==2617&&charger(s,A).getItem(0).isEmpty()&&charger(s,B).getItem(0).isEmpty(),"Disk keeps both charged batteries and carried617 separately");
                var f=factory(s);long elapsed=s.overworld().getGameTime()-prior.get("worldTick").getAsLong();int effective=f.hasJob()?f.progress():f.getItem(2).isEmpty()?0:100;
                check(elapsed>=0&&effective>=prior.get("progress").getAsInt()&&effective<=Math.min(100,prior.get("progress").getAsInt()+elapsed),"Offline reload cannot add progress beyond actual world ticks");
                check(ItemStack.matches(expected,f.hasJob()?f.pendingResult():f.getItem(2)),"Full originally paid result survives real disk reload");check(f.getItem(0).getCount()==4&&f.getItem(1).getCount()==2,"Saved inputs are not debited on reopen");
                initialResume=new LinkedHashMap<String,Object>(state(s));initialResume.put("savedCheckpoint",prior);lastTotal=2617;sampling=true;return state(s);
            }catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}
        }
        for(int x=-5;x<=5;x++)for(int z=-5;z<=6;z++)for(int y=119;y<=123;y++)s.overworld().setBlockAndUpdate(new BlockPos(x,y,z),(y==119?Blocks.STONE:Blocks.AIR).defaultBlockState());
        p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();p.teleportTo(s.overworld(),1.5,120,.5,Set.of(),0,20,true);p.setDeltaMovement(Vec3.ZERO);
        s.overworld().setBlockAndUpdate(A,EnergyContent.CHARGER.defaultBlockState());s.overworld().setBlockAndUpdate(B,EnergyContent.CHARGER.defaultBlockState());s.overworld().setBlockAndUpdate(A.south(),Blocks.REDSTONE_TORCH.defaultBlockState());s.overworld().setBlockAndUpdate(FACTORY,FabricationContent.BLOCK.defaultBlockState());
        p.getInventory().setItem(0,new ItemStack(EnergyContent.BATTERY));p.getInventory().setItem(1,new ItemStack(EnergyContent.BATTERY));p.getInventory().setItem(2,new ItemStack(Items.REDSTONE_BLOCK));p.getInventory().setItem(3,new ItemStack(Items.COPPER_INGOT,8));p.getInventory().setItem(4,new ItemStack(Items.REDSTONE,4));
        var carried=new ItemStack(EnergyContent.BATTERY);Batteries.charge(carried,617);p.getInventory().setItem(9,carried);p.getInventory().setSelectedSlot(0);p.inventoryMenu.broadcastChanges();lastTotal=617;sampling=true;return state(s);
    }
    private static void shot(Minecraft c,String suffix){Screenshot.grab(c.gameDirectory,"c7-industry-"+(RESUME?"resume-":"prepare-")+suffix+".png",c.gameRenderer.mainRenderTarget(),1,m->System.out.println(m.getString()));}
    private static void open(Minecraft c,BlockPos pos){lastOpenAt=System.nanoTime();openingFromId=c.player.containerMenu.containerId;var result=c.gameMode.useItemOn(c.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));System.out.println("C7 INDUSTRY native use "+pos+" result="+result+" clientTick="+c.player.tickCount);}
    private static void move(Minecraft c,int slot){c.gameMode.handleContainerInput(c.player.containerMenu.containerId,slot,0,ContainerInput.QUICK_MOVE,c.player);}
    private static boolean menu(Minecraft c,Class<?> type){return c.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen&&type.isInstance(screen)&&screen.getMenu()==c.player.containerMenu&&screen.getMenu().containerId!=openingFromId;}
    private static int value(Map<String,Object> d,String key){return ((Number)d.get(key)).intValue();}
    private static Map<String,Object> collection(MinecraftServer s){
        int count=0;for(int i=0;i<36;i++)if(ItemStack.isSameItemSameComponents(expected,player(s).getInventory().getItem(i)))count+=player(s).getInventory().getItem(i).getCount();
        check(count<=1,"Native result cannot be duplicated");var st=new LinkedHashMap<String,Object>(state(s));st.put("matchingOutputs",count);
        if(count==1&&factory(s).getItem(2).isEmpty()&&!factory(s).hasJob()){
            check(total(s)==2617&&s.overworld().getBlockState(SOURCE).is(Blocks.REDSTONE_BLOCK),"Source and all finite energy survive actual acknowledged output collection");sampling=false;
        }
        return st;
    }
    public static void frame(Minecraft c){
        if(Boolean.getBoolean("wildcraft.c7.industryFuse")){NativeFuseProbe.frame(c);return;}
        if(Boolean.getBoolean("wildcraft.c7.industryTraversal")){NativeTraversalProbe.frame(c);return;}
        if(Boolean.getBoolean("wildcraft.c7.industryBoundary")){NativeCookingBoundary.frame(c);return;}
        if(Boolean.getBoolean("wildcraft.c7.industryRawCooking")){NativeRawCooking.frame(c);return;}
        if(Boolean.getBoolean("wildcraft.c7.industryMealPause")){NativeMealPause.frame(c);return;}
        long now=System.nanoTime();if(boot==0){boot=now;gameRoot=c.gameDirectory.toPath();}c.options.pauseOnLostFocus=false;
        try{
            check(serverFailure==null,"Industry native audit failed: "+serverFailure);check(System.getProperty("fabric.client.gametest")==null,"No accelerated scheduler");check(now-boot<180_000_000_000L,"Native industry timed out in phase"+phase);
            check(!FabricLoader.getInstance().isModLoaded("wildcraft-test")&&!FabricLoader.getInstance().isModLoaded("wildcraft-c7-performance"),"No legacy research test Mod");
            if(c.player==null||c.level==null||c.getSingleplayerServer()==null||c.gui.overlay()!=null)return;var s=c.getSingleplayerServer();
            if(loggedPhase!=phase){loggedPhase=phase;System.out.println("C7 INDUSTRY phase="+phase+" clientTick="+c.player.tickCount+" y="+c.player.getY()+" screen="+(c.gui.screen()==null?"none":c.gui.screen().getClass().getSimpleName()));}
            if(phase==0&&c.gui.screen()==null&&c.player.tickCount>=25){c.options.renderDistance().set(4);c.options.inactivityFpsLimit().set(InactivityFpsLimit.MINIMIZED);pending=submit(s,()->setup(s));phase=1;
            }else if(phase==1&&pending.isDone()){var st=pending.join();if(Boolean.FALSE.equals(st.get("ready"))){phase=0;return;}if(c.player.getY()<119||!c.level.getBlockState(A).is(EnergyContent.CHARGER))return;if(!Boolean.TRUE.equals(st.get("clientLoaded"))||Boolean.TRUE.equals(st.get("awaitingTeleport"))||!c.player.onGround()||!Boolean.TRUE.equals(st.get("serverOnGround"))){pending=submit(s,()->state(s));return;}System.out.println("C7 INDUSTRY actual ready="+st);if(RESUME){open(c,FACTORY);phase=20;}else{open(c,A);phase=2;}
            }else if(phase==2&&menu(c,ChargerScreen.class)&&c.player.containerMenu.getSlot(28).getItem().is(EnergyContent.BATTERY)){move(c,28);tickTarget=c.player.tickCount+5;phase=3;
            }else if(phase==2&&now-lastOpenAt>3_000_000_000L){pending=submit(s,()->state(s));phase=100;
            }else if(phase==100&&pending.isDone()){var st=pending.join();diagnosticFailure="Native menu not opened; server="+st+", clientPos="+c.player.position()+", clientFloor="+c.level.getBlockState(new BlockPos(1,119,0))+", screen="+c.gui.screen();System.out.println("C7 INDUSTRY DIAGNOSTIC "+diagnosticFailure);shot(c,"menu-not-open-diagnostic");report(c,"failed",diagnosticFailure);failureAt=now;phase=101;
            }else if(phase==101&&now-failureAt>500_000_000L){throw new AssertionError(diagnosticFailure);
            }else if(phase==3&&c.player.tickCount>=tickTarget&&menu(c,ChargerScreen.class)&&c.player.containerMenu.getSlot(0).hasItem()){
                check(((ChargerMenu)c.player.containerMenu).energy()==0&&((ChargerMenu)c.player.containerMenu).status()==1,"Actual redstone signal/carried block cannot charge");shot(c,"signal-no-energy");c.player.closeContainer();open(c,B);phase=4;
            }else if(phase==4&&menu(c,ChargerScreen.class)&&c.player.containerMenu.getSlot(29).getItem().is(EnergyContent.BATTERY)){move(c,29);tickTarget=c.player.tickCount+5;phase=5;
            }else if(phase==5&&c.player.tickCount>=tickTarget&&menu(c,ChargerScreen.class)&&c.player.containerMenu.getSlot(0).hasItem()){
                check(((ChargerMenu)c.player.containerMenu).energy()==0,"Second charger cannot consume carried redstone");shot(c,"two-empty-batteries");c.player.closeContainer();c.player.getInventory().setSelectedSlot(2);c.gameMode.useItemOn(c.player,InteractionHand.MAIN_HAND,new BlockHitResult(new Vec3(1.5,120,2.5),Direction.UP,SOURCE.below(),false));open(c,A);phase=6;
            }else if(phase==6&&menu(c,ChargerScreen.class)&&((ChargerMenu)c.player.containerMenu).energy()>0){check(((ChargerMenu)c.player.containerMenu).energy()<1000,"Capture real partially charged native state");shot(c,"actual-shared-charge");pending=submit(s,()->state(s));phase=7;
            }else if(phase==7&&pending.isDone()){var st=pending.join();if(value(st,"energyA")!=1000||value(st,"energyB")!=1000||value(st,"chargedEnergy")<2000){pending=submit(s,()->state(s));return;}check(value(st,"chargedEnergy")==2000,"Shared fixed source exactly supplies2000 total");shot(c,"actual-full");move(c,0);phase=8;
            }else if(phase==8&&menu(c,ChargerScreen.class)&&!c.player.containerMenu.getSlot(0).hasItem()){c.player.closeContainer();open(c,B);phase=9;
            }else if(phase==9&&menu(c,ChargerScreen.class)&&c.player.containerMenu.getSlot(0).getItem().is(EnergyContent.BATTERY)&&((ChargerMenu)c.player.containerMenu).energy()==1000){move(c,0);phase=10;
            }else if(phase==10&&menu(c,ChargerScreen.class)&&!c.player.containerMenu.getSlot(0).hasItem()){
                c.player.closeContainer();pending=submit(s,()->state(s));phase=11;
            }else if(phase==11&&pending.isDone()){var st=pending.join();if(value(st,"energyA")!=0||value(st,"energyB")!=0||value(st,"inventoryEnergy")!=2617){pending=submit(s,()->state(s));return;}check(value(st,"totalEnergy")==2617,"Native extraction keeps two full batteries and carried617");pending=submit(s,()->{check(s.overworld().getBlockState(SOURCE).is(Blocks.REDSTONE_BLOCK)&&player(s).getInventory().getItem(2).isEmpty(),"Native placement consumes one source item while fixed block remains infinite");return state(s);});shot(c,"native-batteries-extracted");open(c,FACTORY);phase=12;
            }else if(phase==12&&menu(c,FabricatorScreen.class)&&c.player.containerMenu.getSlot(33).getItem().getCount()==8&&c.player.containerMenu.getSlot(34).getItem().getCount()==4){pending.join();move(c,33);move(c,34);phase=13;
            }else if(phase==13&&menu(c,FabricatorScreen.class)&&((FabricatorMenu)c.player.containerMenu).status()==0){
                check(c.player.containerMenu.getSlot(0).getItem().getCount()==8&&c.player.containerMenu.getSlot(1).getItem().getCount()==4,"Native transfers provide exact real factory inputs");shot(c,"factory-before-pay");var buttons=c.gui.screen().children().stream().filter(e->e instanceof Button).map(e->(Button)e).filter(b->b.active&&b.getMessage().getString().equals(Component.translatable("fabrication.wildcraft.start").getString())).toList();check(buttons.size()==1,"One actual native manufacture button");buttons.getFirst().onPress(new KeyEvent(257,0,0));c.gameMode.handleInventoryButtonClick(c.player.containerMenu.containerId,0);tickTarget=c.player.tickCount+5;phase=14;
            }else if(phase==14&&c.player.tickCount>=tickTarget){
                pending=submit(s,()->{var f=factory(s);check(f.hasJob()&&f.getItem(0).getCount()==4&&f.getItem(1).getCount()==2,"Actual repeated start commits only one4/2 payment");expected=f.pendingResult();lastProgress=f.progress();return state(s);});phase=15;
            }else if(phase==15&&pending.isDone()){pending.join();shot(c,"factory-paid-once");tickTarget=c.player.tickCount+20;phase=16;
            }else if(phase==16&&c.player.tickCount>=tickTarget){
                pending=submit(s,()->{check(factory(s).hasJob()&&factory(s).progress()>20&&factory(s).progress()<80,"Ordinary paid job remains incomplete before native save");return state(s);});phase=17;
            }else if(phase==17&&pending.isDone()){pending.join();shot(c,"factory-before-native-close");c.player.closeContainer();checkpointOnStop=true;report(c,"prepared-for-reopen","Native charge and paid job verified; SERVER_STOPPING writes actual complete checkpoint; separate disk reopen still required");phase=18;c.stop();
            }else if(phase==20&&menu(c,FabricatorScreen.class)){shot(c,"same-paid-job");pending=submit(s,()->state(s));phase=21;
            }else if(phase==21&&pending.isDone()){
                var st=pending.join();if(!Boolean.FALSE.equals(st.get("hasJob"))||"empty".equals(st.get("output"))){pending=submit(s,()->state(s));return;}shot(c,"same-completed-output");c.gameMode.handleInventoryButtonClick(c.player.containerMenu.containerId,0);tickTarget=c.player.tickCount+4;phase=22;
            }else if(phase==22&&c.player.tickCount>=tickTarget){
                pending=submit(s,()->{var f=factory(s);check(!f.hasJob()&&ItemStack.matches(expected,f.getItem(2))&&f.getItem(0).getCount()==4&&f.getItem(1).getCount()==2,"Uncollected output blocks a second actual debit");return state(s);});phase=23;
            }else if(phase==23&&pending.isDone()){pending.join();move(c,2);phase=24;
            }else if(phase==24&&menu(c,FabricatorScreen.class)&&!c.player.containerMenu.getSlot(2).hasItem()){
                pending=submit(s,()->collection(s));phase=25;
            }else if(phase==25&&pending.isDone()){var st=pending.join();if(value(st,"matchingOutputs")!=1||!"empty".equals(st.get("output"))||Boolean.TRUE.equals(st.get("hasJob"))){pending=submit(s,()->collection(s));return;}shot(c,"native-once-collected");c.player.closeContainer();report(c,"complete","Ordinary fixed-source shared20/tick budget and native paid100tick job, real disk same-result reopen and once collection");phase=26;c.stop();}
        }catch(RuntimeException|AssertionError e){sampling=false;shot(c,"failure");report(c,"failed",e.toString());throw e;}
    }
}
