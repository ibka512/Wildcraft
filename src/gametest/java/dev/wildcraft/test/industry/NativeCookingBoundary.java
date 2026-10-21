package dev.wildcraft.test.industry;

import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import dev.wildcraft.client.cooking.CookingScreen;
import dev.wildcraft.cooking.*;
import dev.wildcraft.test.industry.mixin.NativeIndustryConnectionAccess;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.*;
import net.minecraft.core.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;

/** Real menus preserve rejected ingredients and two paid outputs; no direct inventory writes after setup. */
public final class NativeCookingBoundary {
    private static final BlockPos FIRE=new BlockPos(1,120,2),POT=FIRE.above();
    private static final List<Map<String,Object>> ROWS=new ArrayList<>();
    private static volatile boolean sampling;
    private static volatile Throwable failure;
    private static int phase,logged=-1,openingFrom,lastProgress,lastOutput,cookTicks,releaseUntil;
    private static long boot,waitFrom;
    private static CompletableFuture<Map<String,Object>> pending;
    private static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
    private static ServerPlayer player(MinecraftServer s){return s.getPlayerList().getPlayers().getFirst();}
    private static CookingPotEntity pot(MinecraftServer s){return (CookingPotEntity)s.overworld().getBlockEntity(POT);}
    private static int count(ServerPlayer p,Item item){int n=0;for(int i=0;i<36;i++)if(p.getInventory().getItem(i).is(item))n+=p.getInventory().getItem(i).getCount();return n;}
    private static long n(Map<String,Object> d,String k){return ((Number)d.get(k)).longValue();}
    private static Map<String,Object> state(MinecraftServer s){
        var p=player(s);var q=pot(s);var d=new LinkedHashMap<String,Object>();d.put("worldTick",s.overworld().getGameTime());d.put("status",q.status());d.put("progress",q.progress());d.put("cookTicks",cookTicks);
        for(int i=0;i<5;i++)d.put("pot"+i,q.getItem(i).getCount());
        d.put("apples",count(p,Items.APPLE));d.put("melons",count(p,Items.MELON_SLICE));d.put("potatoes",count(p,Items.POTATO));d.put("bowls",count(p,Items.BOWL));d.put("meals",count(p,CookingContent.MEAL));d.put("flintWear",p.getInventory().getItem(4).getDamageValue());
        var e=CookingEffects.get(p);d.put("effectMillis",e.cooling().millis());d.put("effectStrength",e.cooling().strength());d.put("otherStrength",e.warmth().strength()+e.recovery().strength());
        d.put("ground",p.onGround());d.put("clientLoaded",p.connection.hasClientLoaded());d.put("awaitingTeleport",((NativeIndustryConnectionAccess)p.connection).wildcraft$awaitingTeleport()!=null);return d;
    }
    private static CompletableFuture<Map<String,Object>> submit(MinecraftServer s,Supplier<Map<String,Object>> action){var f=new CompletableFuture<Map<String,Object>>();s.execute(()->{try{f.complete(action.get());}catch(Throwable e){f.completeExceptionally(e);}});return f;}
    public static void initialize(){ServerTickEvents.END_SERVER_TICK.register(s->{
        if(!sampling)return;
        try{
            check(s.tickRateManager().tickrate()==20,"Native boundary runs at20TPS");var q=pot(s);int progress=q.progress(),output=q.getItem(4).getCount();
            if(progress>lastProgress){check(progress==lastProgress+1,"One cooking increment per actual world tick");cookTicks++;}
            if(output==1&&lastOutput==0){check(lastProgress==99&&progress==0,"Output is committed at100th actual cooking tick");cookTicks++;check(new MealData(1,MealData.COOLING,1,120).equals(q.getItem(4).get(CookingContent.MEAL_DATA)),"Independent native two-batch recipe outcome");}
            lastProgress=progress;lastOutput=output;ROWS.add(state(s));
        }catch(RuntimeException|AssertionError e){failure=e;sampling=false;}
    });}
    private static Map<String,Object> setup(MinecraftServer s){
        var p=player(s);if(!p.connection.hasClientLoaded())return Map.of("ready",false);
        for(int x=-4;x<=4;x++)for(int z=-4;z<=5;z++)for(int y=119;y<=124;y++)s.overworld().setBlockAndUpdate(new BlockPos(x,y,z),(y==119?Blocks.STONE:Blocks.AIR).defaultBlockState());
        s.overworld().setBlockAndUpdate(FIRE,Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT,false));s.overworld().setBlockAndUpdate(POT,CookingContent.POT.defaultBlockState());
        p.getInventory().clearContent();p.getInventory().setItem(0,new ItemStack(Items.APPLE,2));p.getInventory().setItem(1,new ItemStack(Items.POTATO));p.getInventory().setItem(2,new ItemStack(Items.MELON_SLICE,2));p.getInventory().setItem(3,new ItemStack(Items.BOWL,2));p.getInventory().setItem(4,new ItemStack(Items.FLINT_AND_STEEL));p.getInventory().setSelectedSlot(0);p.teleportTo(s.overworld(),1.5,121,.5,Set.of(),0,20,true);p.setDeltaMovement(Vec3.ZERO);p.inventoryMenu.broadcastChanges();sampling=true;return state(s);
    }
    private static void shot(Minecraft c,String key){Screenshot.grab(c.gameDirectory,"c7-cooking-boundary-"+key+".png",c.gameRenderer.mainRenderTarget(),1,m->System.out.println(m.getString()));}
    private static void report(Minecraft c,String status,String message){
        var d=new LinkedHashMap<String,Object>();d.put("status",status);d.put("message",message);d.put("phase",phase);d.put("actualCookTicks",cookTicks);d.put("providedIngredientsNotNaturalAcquisition",true);d.put("directMealOrEffectGrant",false);d.put("manualOrForegroundPerformanceVerified",false);d.put("configuredMaxFps",c.options.framerateLimit().get());d.put("reportedFPSAtEnd",c.getFps());d.put("rows",new ArrayList<>(ROWS));
        try{Files.writeString(c.gameDirectory.toPath().resolve("c7-native-cooking-boundary.json"),new GsonBuilder().setPrettyPrinting().create().toJson(d)+"\n");}catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}
    }
    private static void open(Minecraft c){openingFrom=c.player.containerMenu.containerId;c.gameMode.useItemOn(c.player,InteractionHand.MAIN_HAND,new BlockHitResult(new Vec3(1.5,121.5,2),Direction.NORTH,POT,false));}
    private static boolean menu(Minecraft c){return c.gui.screen() instanceof CookingScreen screen&&screen.getMenu()==c.player.containerMenu&&screen.getMenu().containerId!=openingFrom;}
    private static void transfer(Minecraft c,int slot){c.gameMode.handleContainerInput(c.player.containerMenu.containerId,slot,0,ContainerInput.QUICK_MOVE,c.player);}
    private static void eat(Minecraft c){int slot=-1;for(int i=0;i<9;i++)if(c.player.getInventory().getItem(i).is(CookingContent.MEAL)){slot=i;break;}check(slot>=0,"Actually collected meal in native hotbar");c.player.getInventory().setSelectedSlot(slot);c.player.setXRot(-60);c.player.setYRot(90);c.options.keyUse.setDown(true);KeyMapping.click(InputConstants.getKey(c.options.keyUse.saveString()));}
    public static void frame(Minecraft c){
        long now=System.nanoTime();if(boot==0)boot=now;c.options.pauseOnLostFocus=false;
        try{
            check(failure==null,"Native boundary tick audit: "+failure);check(System.getProperty("fabric.client.gametest")==null,"No accelerated gametest");check(now-boot<150_000_000_000L,"Native cooking boundary timeout phase"+phase);
            if(c.player==null||c.level==null||c.getSingleplayerServer()==null||c.gui.overlay()!=null)return;var s=c.getSingleplayerServer();
            if(logged!=phase){logged=phase;System.out.println("C7 COOKING BOUNDARY phase="+phase);}
            if(phase==0&&c.gui.screen()==null&&c.player.tickCount>=25){c.options.inactivityFpsLimit().set(InactivityFpsLimit.MINIMIZED);pending=submit(s,()->setup(s));phase=1;
            }else if(phase==1&&pending.isDone()){
                var st=pending.join();if(Boolean.FALSE.equals(st.get("ready"))){phase=0;return;}if(!c.player.onGround()||!Boolean.TRUE.equals(st.get("ground"))||Boolean.TRUE.equals(st.get("awaitingTeleport"))||!c.level.getBlockState(POT).is(CookingContent.POT)){pending=submit(s,()->state(s));return;}open(c);phase=2;
            }else if(phase==2&&menu(c)&&c.player.containerMenu.getSlot(32).getItem().is(Items.APPLE)&&c.player.containerMenu.getSlot(33).getItem().is(Items.POTATO)&&c.player.containerMenu.getSlot(35).getItem().is(Items.BOWL)){
                transfer(c,32);transfer(c,33);transfer(c,35);pending=submit(s,()->state(s));phase=3;
            }else if(phase==3&&pending.isDone()){
                var st=pending.join();if(n(st,"pot0")!=2||n(st,"pot1")!=1||n(st,"pot3")!=2){pending=submit(s,()->state(s));return;}c.player.closeContainer();c.player.getInventory().setSelectedSlot(4);c.gameMode.useItemOn(c.player,InteractionHand.MAIN_HAND,new BlockHitResult(new Vec3(1.5,120.3,2),Direction.NORTH,FIRE,false));pending=submit(s,()->state(s));phase=4;
            }else if(phase==4&&pending.isDone()){
                var st=pending.join();if(n(st,"flintWear")!=1||!c.level.getBlockState(FIRE).getValue(CampfireBlock.LIT)){pending=submit(s,()->state(s));return;}waitFrom=n(st,"worldTick");open(c);pending=submit(s,()->state(s));phase=5;
            }else if(phase==5&&pending.isDone()&&menu(c)){
                var st=pending.join();check(n(st,"status")==0&&n(st,"progress")==0&&n(st,"cookTicks")==0&&n(st,"pot0")==2&&n(st,"pot1")==1&&n(st,"pot3")==2&&n(st,"pot4")==0,"Nonmatching accepted ingredients stay uncharged over native ticks");if(n(st,"worldTick")-waitFrom<30){pending=submit(s,()->state(s));return;}shot(c,"invalid-combination-preserved");transfer(c,1);pending=submit(s,()->state(s));phase=6;
            }else if(phase==6&&pending.isDone()&&menu(c)){
                var st=pending.join();if(n(st,"pot1")!=0||n(st,"potatoes")!=1||c.player.containerMenu.getSlot(1).hasItem()){pending=submit(s,()->state(s));return;}transfer(c,34);pending=submit(s,()->state(s));phase=7;
            }else if(phase==7&&pending.isDone()){
                var st=pending.join();if(n(st,"pot1")!=2||n(st,"progress")==0){pending=submit(s,()->state(s));return;}shot(c,"native-correction-starts-cooking");pending=submit(s,()->state(s));phase=8;
            }else if(phase==8&&pending.isDone()){
                var st=pending.join();if(n(st,"pot4")!=1||n(st,"cookTicks")!=100){pending=submit(s,()->state(s));return;}waitFrom=n(st,"worldTick");pending=submit(s,()->state(s));phase=9;
            }else if(phase==9&&pending.isDone()){
                var st=pending.join();check(n(st,"pot0")==1&&n(st,"pot1")==1&&n(st,"pot3")==1&&n(st,"pot4")==1&&n(st,"progress")==0&&n(st,"cookTicks")==100&&n(st,"meals")==0,"Occupied output preserves second batch without charging");if(n(st,"worldTick")-waitFrom<120){pending=submit(s,()->state(s));return;}check(n(st,"status")==4,"Actual occupied-output status");shot(c,"occupied-output-preserves-second-batch");transfer(c,4);pending=submit(s,()->state(s));phase=10;
            }else if(phase==10&&pending.isDone()){
                var st=pending.join();if(n(st,"meals")!=1||n(st,"pot4")!=0){pending=submit(s,()->state(s));return;}if(c.player.containerMenu.getSlot(4).hasItem()){pending=submit(s,()->state(s));return;}transfer(c,4);waitFrom=n(st,"worldTick");pending=submit(s,()->state(s));phase=11;
            }else if(phase==11&&pending.isDone()){
                var st=pending.join();check(n(st,"meals")==1,"Clicking now-empty output cannot duplicate first meal");if(n(st,"pot4")!=1||n(st,"cookTicks")!=200){pending=submit(s,()->state(s));return;}check(n(st,"pot0")==0&&n(st,"pot1")==0&&n(st,"pot3")==0,"Second batch debits exactly the remaining ingredients and bowl");shot(c,"second-batch-single-output");transfer(c,4);pending=submit(s,()->state(s));phase=12;
            }else if(phase==12&&pending.isDone()){
                var st=pending.join();if(n(st,"meals")!=2||n(st,"pot4")!=0||c.player.containerMenu.getSlot(4).hasItem()){pending=submit(s,()->state(s));return;}transfer(c,4);transfer(c,4);waitFrom=n(st,"worldTick");pending=submit(s,()->state(s));phase=13;
            }else if(phase==13&&pending.isDone()){
                var st=pending.join();check(n(st,"meals")==2&&n(st,"pot4")==0&&n(st,"apples")==0&&n(st,"melons")==0&&n(st,"potatoes")==1&&n(st,"bowls")==0&&n(st,"flintWear")==1,"Repeated empty collection preserves exactly two native meals and rejected potato");if(n(st,"worldTick")-waitFrom<5){pending=submit(s,()->state(s));return;}shot(c,"repeated-empty-clicks-no-extra-meals");c.player.closeContainer();phase=14;
            }else if(phase==14&&c.gui.screen()==null){eat(c);pending=submit(s,()->state(s));phase=15;
            }else if(phase==15&&pending.isDone()){
                var st=pending.join();if(n(st,"meals")!=1||n(st,"bowls")!=1||n(st,"effectMillis")==0){pending=submit(s,()->state(s));return;}c.options.keyUse.setDown(false);shot(c,"first-actual-meal-eaten");releaseUntil=c.player.tickCount+4;phase=16;
            }else if(phase==16&&c.player.tickCount>=releaseUntil){eat(c);pending=submit(s,()->state(s));phase=17;
            }else if(phase==17&&pending.isDone()){
                var st=pending.join();if(n(st,"meals")!=0||n(st,"bowls")!=2){pending=submit(s,()->state(s));return;}c.options.keyUse.setDown(false);check(n(st,"effectStrength")==1&&n(st,"effectMillis")>115000&&n(st,"effectMillis")<=120000&&n(st,"otherStrength")==0,"Second same-level meal refreshes to120s, never adds another120s");check(n(st,"potatoes")==1&&n(st,"flintWear")==1&&n(st,"cookTicks")==200,"Final conservation after two actual meals");sampling=false;shot(c,"second-meal-refresh-and-two-bowls");report(c,"complete","Invalid recipe, occupied output, two100-tick batches, repeated empty collection and native same-level refresh");phase=18;c.stop();}
        }catch(RuntimeException|AssertionError e){sampling=false;c.options.keyUse.setDown(false);shot(c,"failure");report(c,"failed",e.toString());throw e;}
    }
}
