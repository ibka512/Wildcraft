package dev.wildcraft.test.industry;

import com.google.gson.GsonBuilder;
import com.google.gson.Gson;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import com.mojang.blaze3d.platform.InputConstants;
import dev.wildcraft.client.cooking.CookingScreen;
import dev.wildcraft.client.input.WildcraftBindings;
import dev.wildcraft.client.input.ActionInput;
import dev.wildcraft.combat.MeleeActions;
import dev.wildcraft.player.PlayerStamina;
import dev.wildcraft.cooking.*;
import dev.wildcraft.test.industry.mixin.NativeIndustryConnectionAccess;
import java.nio.file.*;
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

/** Provided raw ingredients, real native pot transfers/fire/use; no direct cooked meal or effect grant. */
public final class NativeRawCooking {
    private static final BlockPos FIRE=new BlockPos(1,120,2),POT=FIRE.above();
    private record RecipeCase(String key,List<Item> ingredients,MealData expected) { }
    // Independent expectations transcribed from the accepted finite recipe design, not match(...).
    private static RecipeCase recipeCase(String key){return switch(key){
        case "vegetable" -> new RecipeCase(key,List.of(Items.CARROT,Items.POTATO),new MealData(1,MealData.NONE,0,0));
        case "warmth1" -> new RecipeCase(key,List.of(Items.BEETROOT,Items.POTATO),new MealData(1,MealData.WARMTH,1,120));
        case "warmth2" -> new RecipeCase(key,List.of(Items.BEETROOT,Items.POTATO,Items.CARROT),new MealData(1,MealData.WARMTH,2,180));
        case "cooling1" -> new RecipeCase(key,List.of(Items.APPLE,Items.MELON_SLICE),new MealData(1,MealData.COOLING,1,120));
        case "cooling2" -> new RecipeCase(key,List.of(Items.APPLE,Items.MELON_SLICE,Items.SWEET_BERRIES),new MealData(1,MealData.COOLING,2,180));
        case "recovery1" -> new RecipeCase(key,List.of(Items.RED_MUSHROOM,Items.SUGAR),new MealData(1,MealData.RECOVERY,1,60));
        case "recovery2" -> new RecipeCase(key,List.of(Items.RED_MUSHROOM,Items.BROWN_MUSHROOM,Items.SUGAR),new MealData(1,MealData.RECOVERY,2,90));
        default -> throw new IllegalArgumentException("Unknown native cooking case: "+key);
    };}
    private static final RecipeCase CASE=recipeCase(System.getProperty("wildcraft.c7.industryRecipe","cooling1"));
    private static final MealData EXPECTED=CASE.expected();
    private static final int BOWL_SLOT=CASE.ingredients().size(),FLINT_SLOT=BOWL_SLOT+1,SHOVEL_SLOT=FLINT_SLOT+1;
    private static final List<Map<String,Object>> ROWS=new ArrayList<>();
    private static CompletableFuture<Map<String,Object>> pending;
    private static volatile boolean sampling;
    private static volatile Throwable failure;
    private static int phase,logged=-1,openingFrom,waitTick,lastProgress;
    private static volatile int cookTicks;
    private static final boolean HEAT_INTERRUPT=Boolean.getBoolean("wildcraft.c7.industryHeatInterrupt");
    private static final boolean SAVE_HEAT=Boolean.getBoolean("wildcraft.c7.industryHeatSave"), RESUME=Boolean.getBoolean("wildcraft.c7.industryResume");
    private static final boolean RECOVERY_PROBE=Boolean.getBoolean("wildcraft.c7.industryRecovery");
    private static volatile boolean regenAudit;
    private static boolean completed,heatInterrupted,saveCheckpoint;
    private static double previousStamina=100;
    private static int previousDelay,hopSpends,recoverySteps,countdownSteps,airDelaySteps,releaseTick;
    private static Map<String,Object> prior;
    private static int interruptedProgress;
    private static long interruptedTick;
    private static long boot;
    public static void initialize(){
        check(!(NativeSnowProbe.REQUESTED&&RECOVERY_PROBE),"Select either native snow or hop-recovery tail");NativeSnowProbe.initialize();
        if(RESUME){try{prior=new Gson().fromJson(Files.readString(Path.of(System.getProperty("wildcraft.c7.industryCheckpoint"))),Map.class);}catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}}
        ServerTickEvents.END_SERVER_TICK.register(NativeRawCooking::tick);
        ServerLifecycleEvents.SERVER_STOPPING.register(s->{if(saveCheckpoint){var st=new LinkedHashMap<String,Object>(state(s));st.put("owner",player(s).getStringUUID());st.put("savedAtMillis",System.currentTimeMillis());try{Files.writeString(Minecraft.getInstance().gameDirectory.toPath().resolve("c7-raw-cooking-stop.json"),new GsonBuilder().setPrettyPrinting().create().toJson(st)+"\n");}catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}}});
    }
    private static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
    private static ServerPlayer player(MinecraftServer s){return s.getPlayerList().getPlayers().getFirst();}
    private static CookingPotEntity pot(MinecraftServer s){return (CookingPotEntity)s.overworld().getBlockEntity(POT);}
    private static int count(ServerPlayer p,Item item){int n=0;for(int i=0;i<36;i++)if(p.getInventory().getItem(i).is(item))n+=p.getInventory().getItem(i).getCount();return n;}
    private static Map<String,Object> state(MinecraftServer s){
        var p=player(s);var q=pot(s);var r=new LinkedHashMap<String,Object>();r.put("worldTick",s.overworld().getGameTime());r.put("status",q.status());r.put("progress",q.progress());r.put("ingredientA",q.getItem(0).getCount());r.put("ingredientB",q.getItem(1).getCount());r.put("ingredientC",q.getItem(2).getCount());r.put("rawInventory",CASE.ingredients().stream().mapToInt(i->count(p,i)).sum());r.put("observedCookTicks",cookTicks);r.put("potBowls",q.getItem(3).getCount());r.put("potOutput",q.getItem(4).getCount());r.put("apples",count(p,Items.APPLE));r.put("melons",count(p,Items.MELON_SLICE));r.put("bowls",count(p,Items.BOWL));r.put("meals",count(p,CookingContent.MEAL));r.put("flintWear",p.getInventory().getItem(FLINT_SLOT).getDamageValue());var effects=CookingEffects.get(p);var selected=switch(EXPECTED.kind()){case MealData.WARMTH -> effects.warmth();case MealData.COOLING -> effects.cooling();case MealData.RECOVERY -> effects.recovery();default -> MealEffects.Effect.EMPTY;};r.put("effectMillis",selected.millis());r.put("effectStrength",selected.strength());r.put("warmthStrength",effects.warmth().strength());r.put("coolingStrength",effects.cooling().strength());r.put("recoveryStrength",effects.recovery().strength());r.put("recoveryMultiplier",CookingEffects.recoveryMultiplier(p));var stamina=PlayerStamina.get(p);r.put("stamina",stamina.stamina());r.put("capacity",stamina.capacity());r.put("recoveryDelay",stamina.recoveryDelay());r.put("canRecover",PlayerStamina.canRecover(p));r.put("meleePhase",MeleeActions.view(p).phase());r.put("health",p.getHealth());r.put("hopSpends",hopSpends);r.put("recoverySteps",recoverySteps);r.put("countdownSteps",countdownSteps);r.put("airDelaySteps",airDelaySteps);r.put("regenAudit",regenAudit);r.put("ground",p.onGround());r.put("clientLoaded",p.connection.hasClientLoaded());r.put("awaitingTeleport",((NativeIndustryConnectionAccess)p.connection).wildcraft$awaitingTeleport()!=null);return r;
    }
    private static long n(Map<String,Object> r,String k){return ((Number)r.get(k)).longValue();}
    private static CompletableFuture<Map<String,Object>> submit(MinecraftServer s,Supplier<Map<String,Object>> action){var f=new CompletableFuture<Map<String,Object>>();s.execute(()->{try{f.complete(action.get());}catch(Throwable e){f.completeExceptionally(e);}});return f;}
    private static void tick(MinecraftServer s){
        if(!sampling)return;
        try{check(s.tickRateManager().tickrate()==20,"Raw cooking remains normal20TPS");var q=pot(s);int effective=q.getItem(4).isEmpty()?q.progress():100;if(!completed&&effective>lastProgress){check(effective==lastProgress+1,"Cook advances once per real world tick");cookTicks++;}if(effective==100)completed=true;lastProgress=effective;if(regenAudit)auditRecovery(s);ROWS.add(state(s));}catch(RuntimeException|AssertionError e){failure=e;sampling=false;}
    }
    private static void auditRecovery(MinecraftServer s){
        var p=player(s);var d=PlayerStamina.get(p);double delta=d.stamina()-previousStamina;
        if(delta<-.000001){check(hopSpends==0&&Math.abs(delta+8)<.000001&&MeleeActions.hopping(p),"One native direction hop costs exactly8, no direct consume");hopSpends++;}
        if(hopSpends>0&&delta>.000001){double expected=Math.min(d.capacity()-previousStamina,1+(EXPECTED.kind()==MealData.RECOVERY?EXPECTED.strength()*.5:0));check(previousDelay==0&&PlayerStamina.canRecover(p)&&Math.abs(delta-expected)<.000001,"Original per-world-tick recovery and cap after native delay");recoverySteps++;}
        if(hopSpends>0&&d.stamina()<d.capacity()){
            if(!PlayerStamina.canRecover(p)){check(d.recoveryDelay()==20,"Natural airborne recovery delay stays20");airDelaySteps++;}
            else if(delta==0&&previousDelay>0){check(d.recoveryDelay()==previousDelay-1,"Original grounded delay decrements once per world tick");countdownSteps++;}
        }
        check(p.getHealth()==20,"No damage or environmental interference in recovery comparison");previousStamina=d.stamina();previousDelay=d.recoveryDelay();
    }
    private static Map<String,Object> setup(MinecraftServer s){
        var p=player(s);if(!p.connection.hasClientLoaded())return Map.of("ready",false);
        for(int x=-4;x<=4;x++)for(int z=-4;z<=5;z++)for(int y=119;y<=124;y++)s.overworld().setBlockAndUpdate(new BlockPos(x,y,z),(y==119?Blocks.STONE:Blocks.AIR).defaultBlockState());
        s.overworld().setBlockAndUpdate(FIRE,Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT,false));s.overworld().setBlockAndUpdate(POT,CookingContent.POT.defaultBlockState());NativeSnowProbe.prepare(s);
        p.getInventory().clearContent();for(int i=0;i<CASE.ingredients().size();i++)p.getInventory().setItem(i,new ItemStack(CASE.ingredients().get(i)));p.getInventory().setItem(BOWL_SLOT,new ItemStack(Items.BOWL));p.getInventory().setItem(FLINT_SLOT,new ItemStack(Items.FLINT_AND_STEEL));if(HEAT_INTERRUPT)p.getInventory().setItem(SHOVEL_SLOT,new ItemStack(Items.IRON_SHOVEL));p.getInventory().setSelectedSlot(0);p.teleportTo(s.overworld(),1.5,121,.5,Set.of(),0,20,true);p.setDeltaMovement(Vec3.ZERO);p.inventoryMenu.broadcastChanges();sampling=true;return state(s);
    }
    private static void shot(Minecraft c,String suffix){Screenshot.grab(c.gameDirectory,"c7-raw-cooking-"+suffix+".png",c.gameRenderer.mainRenderTarget(),1,m->System.out.println(m.getString()));}
    private static void report(Minecraft c,String status,String message){
        var d=new LinkedHashMap<String,Object>();d.put("status",status);d.put("message",message);d.put("phase",phase);d.put("recipe",CASE.key());d.put("expected",EXPECTED);d.put("actualCookTicks",cookTicks);d.put("heatInterrupted",heatInterrupted);d.put("nativeRecoveryProbe",RECOVERY_PROBE);d.put("hopSpends",hopSpends);d.put("recoverySteps",recoverySteps);d.put("groundedCountdownSteps",countdownSteps);d.put("airDelaySteps",airDelaySteps);d.put("providedRawIngredientsNotNaturalAcquisition",true);d.put("directMealOrEffectGrant",false);d.put("foregroundPerformanceQualified",false);d.put("configuredMaxFps",c.options.framerateLimit().get());d.put("reportedFPSAtEnd",c.getFps());d.put("rows",new ArrayList<>(ROWS));d.put("nativeSnow",NativeSnowProbe.summary());
        try{Files.writeString(c.gameDirectory.toPath().resolve("c7-native-raw-cooking.json"),new GsonBuilder().setPrettyPrinting().create().toJson(d)+"\n");}catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}
    }
    private static void open(Minecraft c){openingFrom=c.player.containerMenu.containerId;c.gameMode.useItemOn(c.player,InteractionHand.MAIN_HAND,new BlockHitResult(new Vec3(1.5,121.5,2),Direction.NORTH,POT,false));}
    private static boolean menu(Minecraft c){return c.gui.screen() instanceof CookingScreen screen&&screen.getMenu()==c.player.containerMenu&&screen.getMenu().containerId!=openingFrom;}
    private static void transfer(Minecraft c,int slot){c.gameMode.handleContainerInput(c.player.containerMenu.containerId,slot,0,ContainerInput.QUICK_MOVE,c.player);}
    public static void frame(Minecraft c){
        long now=System.nanoTime();if(boot==0)boot=now;c.options.pauseOnLostFocus=false;
        try{
            check(failure==null,"Native cooking tick audit failed: "+failure);check(System.getProperty("fabric.client.gametest")==null,"No accelerated gametest");check(now-boot<150_000_000_000L,"Native raw cooking timed out phase"+phase);if(c.player==null||c.level==null||c.getSingleplayerServer()==null||c.gui.overlay()!=null)return;var s=c.getSingleplayerServer();
            if(phase==80){if(NativeSnowProbe.frame(c)){sampling=false;report(c,"complete","Actual raw cooking/eat, native powder-snow walk, gated freeze applications and original level walking exit/thaw");phase=81;c.stop();}return;}
            if(logged!=phase){logged=phase;System.out.println("C7 RAW COOKING phase="+phase+" screen="+(c.gui.screen()==null?"none":c.gui.screen().getClass().getSimpleName()));}
            if(phase==0&&c.gui.screen()==null&&c.player.tickCount>=25){c.options.inactivityFpsLimit().set(InactivityFpsLimit.MINIMIZED);
                if(RESUME){pending=submit(s,()->{var st=state(s);check(player(s).getStringUUID().equals(prior.get("owner")),"Same actual disk player owner");check(n(st,"progress")==n(prior,"progress")&&n(st,"status")==1&&n(st,"ingredientA")==1&&n(st,"ingredientB")==1&&n(st,"potBowls")==1&&n(st,"potOutput")==0&&n(st,"flintWear")==1,"Actual saved cold batch and original materials resume unchanged");return st;});lastProgress=(int)n(prior,"progress");cookTicks=lastProgress;sampling=true;phase=20;}
                else{pending=submit(s,()->setup(s));phase=1;}
            }else if(phase==1&&pending.isDone()){
                var st=pending.join();if(Boolean.FALSE.equals(st.get("ready"))){phase=0;return;}if(!c.player.onGround()||!Boolean.TRUE.equals(st.get("ground"))||Boolean.TRUE.equals(st.get("awaitingTeleport"))||!c.level.getBlockState(POT).is(CookingContent.POT)){pending=submit(s,()->state(s));return;}open(c);phase=2;
            }else if(phase==2&&menu(c)){
                for(int i=0;i<CASE.ingredients().size();i++)if(!c.player.containerMenu.getSlot(32+i).getItem().is(CASE.ingredients().get(i)))return;
                for(int i=0;i<CASE.ingredients().size();i++)transfer(c,32+i);phase=3;
            }else if(phase==3&&menu(c)&&((CookingMenu)c.player.containerMenu).status()==3){shot(c,"actual-bowl-required");transfer(c,32+BOWL_SLOT);waitTick=c.player.tickCount+5;phase=4;
            }else if(phase==4&&menu(c)&&c.player.tickCount>=waitTick&&((CookingMenu)c.player.containerMenu).status()==1){check(((CookingMenu)c.player.containerMenu).progress()==0,"Unlit campfire cannot cook");shot(c,"actual-heat-required");c.player.closeContainer();c.player.getInventory().setSelectedSlot(FLINT_SLOT);c.gameMode.useItemOn(c.player,InteractionHand.MAIN_HAND,new BlockHitResult(new Vec3(1.5,120.3,2),Direction.NORTH,FIRE,false));pending=submit(s,()->state(s));phase=5;
            }else if(phase==5&&pending.isDone()){
                var st=pending.join();if(n(st,"flintWear")!=1||!c.level.getBlockState(FIRE).getValue(CampfireBlock.LIT)){pending=submit(s,()->state(s));return;}open(c);phase=6;
            }else if(phase==6&&menu(c)&&HEAT_INTERRUPT&&!heatInterrupted){
                if(((CookingMenu)c.player.containerMenu).progress()<20)return;
                c.player.closeContainer();c.player.getInventory().setSelectedSlot(SHOVEL_SLOT);c.gameMode.useItemOn(c.player,InteractionHand.MAIN_HAND,new BlockHitResult(new Vec3(1.5,120.3,2),Direction.NORTH,FIRE,false));pending=submit(s,()->state(s));phase=20;
            }else if(phase==20&&pending.isDone()){
                var st=pending.join();if(n(st,"status")!=1||c.level.getBlockState(FIRE).getValue(CampfireBlock.LIT)){pending=submit(s,()->state(s));return;}
                interruptedProgress=(int)n(st,"progress");interruptedTick=n(st,"worldTick");check(interruptedProgress>0&&interruptedProgress<100,"Native shovel interruption occurs mid-batch");shot(c,"native-heat-interrupted");pending=submit(s,()->state(s));phase=21;
            }else if(phase==21&&pending.isDone()){
                var st=pending.join();check(n(st,"status")==1&&n(st,"progress")==interruptedProgress&&n(st,"ingredientA")==1&&n(st,"ingredientB")==1&&n(st,"potBowls")==1&&n(st,"potOutput")==0,"No heat keeps actual progress and all uncharged inputs");
                if(n(st,"worldTick")-interruptedTick<20){pending=submit(s,()->state(s));return;}
                shot(c,"native-heat-paused-twenty-ticks");
                if(SAVE_HEAT&&!RESUME){saveCheckpoint=true;sampling=false;report(c,"prepared-for-reopen","Cold partial batch only; real separate-process disk resume still required");phase=30;c.stop();return;}
                heatInterrupted=true;c.player.getInventory().setSelectedSlot(FLINT_SLOT);c.gameMode.useItemOn(c.player,InteractionHand.MAIN_HAND,new BlockHitResult(new Vec3(1.5,120.3,2),Direction.NORTH,FIRE,false));pending=submit(s,()->state(s));phase=22;
            }else if(phase==22&&pending.isDone()){
                var st=pending.join();if(n(st,"flintWear")!=2||!c.level.getBlockState(FIRE).getValue(CampfireBlock.LIT)){pending=submit(s,()->state(s));return;}open(c);phase=6;
            }else if(phase==6&&menu(c)&&((CookingMenu)c.player.containerMenu).progress()>0){shot(c,"actual-lit-cooking");pending=submit(s,()->state(s));phase=7;
            }else if(phase==7&&pending.isDone()){
                var st=pending.join();if(n(st,"potOutput")!=1||n(st,"observedCookTicks")!=100){pending=submit(s,()->state(s));return;}check(cookTicks==100,"Exactly100 real cooking ticks");pending=submit(s,()->{var q=pot(s);check(EXPECTED.equals(q.getItem(4).get(CookingContent.MEAL_DATA)),"Actual native recipe matches independent case "+CASE.key());check(q.getItem(0).isEmpty()&&q.getItem(1).isEmpty()&&q.getItem(2).isEmpty()&&q.getItem(3).isEmpty(),"Actual completion debits all occupied ingredient slots and bowl exactly once");return state(s);});phase=8;
            }else if(phase==8&&pending.isDone()&&menu(c)&&c.player.containerMenu.getSlot(4).hasItem()){pending.join();shot(c,"actual-one-cooked-meal");transfer(c,4);pending=submit(s,()->state(s));phase=9;
            }else if(phase==9&&pending.isDone()){
                var st=pending.join();if(n(st,"meals")!=1||n(st,"potOutput")!=0){pending=submit(s,()->state(s));return;}check(n(st,"rawInventory")==0&&n(st,"bowls")==0,"Raw item and bowl accounting after true native collection");c.player.closeContainer();phase=10;
            }else if(phase==10&&c.gui.screen()==null){int slot=-1;for(int i=0;i<9;i++)if(c.player.getInventory().getItem(i).is(CookingContent.MEAL))slot=i;check(slot>=0,"Native collected meal is in real hotbar");c.player.getInventory().setSelectedSlot(slot);c.player.setXRot(-60);c.player.setYRot(90);c.options.keyUse.setDown(true);KeyMapping.click(InputConstants.getKey(c.options.keyUse.saveString()));pending=submit(s,()->state(s));phase=11;
            }else if(phase==11&&pending.isDone()){
                var st=pending.join();if(n(st,"meals")!=0||n(st,"bowls")!=1||(EXPECTED.kind()!=MealData.NONE&&n(st,"effectMillis")==0)){pending=submit(s,()->state(s));return;}c.options.keyUse.setDown(false);check(n(st,"effectStrength")==EXPECTED.strength()&&n(st,"flintWear")== (HEAT_INTERRUPT?2:1),"Actually cooked effect strength and native lighter wear");
                check(EXPECTED.kind()==MealData.NONE?n(st,"effectMillis")==0:n(st,"effectMillis")>EXPECTED.seconds()*1000-5000,"Actual cooked duration or zero-effect vegetable");
                check(n(st,"warmthStrength")== (EXPECTED.kind()==MealData.WARMTH?EXPECTED.strength():0)&&n(st,"coolingStrength")== (EXPECTED.kind()==MealData.COOLING?EXPECTED.strength():0)&&n(st,"recoveryStrength")== (EXPECTED.kind()==MealData.RECOVERY?EXPECTED.strength():0),"No unrelated meal effect is created");
                check(((Number)st.get("recoveryMultiplier")).doubleValue()==1+(EXPECTED.kind()==MealData.RECOVERY?EXPECTED.strength()*.5:0),"Original recovery multiplier, no new stamina rule");shot(c,"native-eaten-and-bowl-returned");
                if(NativeSnowProbe.REQUESTED){NativeSnowProbe.begin(c,EXPECTED.kind()==MealData.WARMTH?EXPECTED.strength():0);phase=80;}
                else if(RECOVERY_PROBE){pending=submit(s,()->state(s));phase=40;}
                else{sampling=false;report(c,"complete","Actual ingredient/bowl transfers, native side lighting,100 cooking ticks, one meal collection/eat/returned bowl");phase=12;c.stop();}
            }else if(phase==40&&pending.isDone()){
                var st=pending.join();if(!c.player.onGround()||!Boolean.TRUE.equals(st.get("ground"))||c.player.isUsingItem()||!ActionInput.tickFrame().gameplay()){pending=submit(s,()->state(s));return;}
                check(((Number)st.get("stamina")).doubleValue()==100&&((Number)st.get("capacity")).doubleValue()==100,"Native pre-hop full zero-peak stamina");regenAudit=true;c.options.keyRight.setDown(true);WildcraftBindings.DODGE.setDown(true);KeyMapping.click(InputConstants.getKey(WildcraftBindings.DODGE.saveString()));releaseTick=c.player.tickCount+2;pending=submit(s,()->state(s));phase=41;
            }else if(phase==41){
                if(c.player.tickCount>=releaseTick){c.options.keyRight.setDown(false);WildcraftBindings.DODGE.setDown(false);}
                if(!pending.isDone())return;var st=pending.join();
                if(n(st,"hopSpends")!=1||((Number)st.get("stamina")).doubleValue()!=100){pending=submit(s,()->state(s));return;}
                check(n(st,"recoverySteps")>=4&&n(st,"countdownSteps")>=19&&n(st,"airDelaySteps")>0,"Real8-point depletion, airborne delay, grounded countdown and original recovery steps observed");regenAudit=false;sampling=false;shot(c,"native-hop-and-original-stamina-recovery");report(c,"complete","Actual cooking/eat followed by one native8-cost direction hop, original20-grounded-tick delay and per-world-tick recovery");phase=42;c.stop();}
        }catch(RuntimeException|AssertionError e){sampling=false;NativeSnowProbe.release(c);c.options.keyUse.setDown(false);c.options.keyRight.setDown(false);WildcraftBindings.DODGE.setDown(false);shot(c,"failure");report(c,"failed",e.toString());throw e;}
    }
}
