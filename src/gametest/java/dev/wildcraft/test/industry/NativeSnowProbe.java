package dev.wildcraft.test.industry;

import dev.wildcraft.client.input.ActionInput;
import dev.wildcraft.cooking.CookingContent;
import dev.wildcraft.cooking.CookingEffects;
import dev.wildcraft.player.PlayerStamina;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;

/** Real walking/jumping in native powder snow; all frozen-counter observations are read-only. */
public final class NativeSnowProbe {
    public static final boolean REQUESTED=Boolean.getBoolean("wildcraft.c7.industrySnow");
    private static final List<Map<String,Object>> SAMPLES=new ArrayList<>(),APPLICATIONS=new ArrayList<>();
    private static volatile boolean active;
    private static volatile Throwable failure;
    private static int phase,logged=-1,expectedWarmth,beforeFrozen,beforePlayerTick,fromCount,windowApplications,windowGrowth,applied,skipped;
    private static long fromTick;
    private static Map<String,Object> windowStart,windowEnd;
    private static CompletableFuture<Map<String,Object>> pending;
    private static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
    private static long n(Map<String,Object> d,String k){return ((Number)d.get(k)).longValue();}
    private static ServerPlayer player(MinecraftServer s){return s.getPlayerList().getPlayers().getFirst();}
    private static Map<String,Object> inspect(MinecraftServer s){
        var p=player(s);var d=new LinkedHashMap<String,Object>();d.put("worldTick",s.overworld().getGameTime());d.put("playerTick",p.tickCount);d.put("x",p.getX());d.put("y",p.getY());d.put("z",p.getZ());d.put("ground",p.onGround());d.put("inPowderSnow",p.isInPowderSnow);d.put("frozen",p.getTicksFrozen());d.put("freezeLimit",p.getTicksRequiredToFreeze());d.put("canFreeze",p.canFreeze());d.put("warmth",CookingEffects.get(p).warmth().strength());d.put("warmthMillis",CookingEffects.get(p).warmth().millis());d.put("health",p.getHealth());d.put("stamina",PlayerStamina.get(p).stamina());d.put("effectApplications",APPLICATIONS.size());return d;
    }
    private static CompletableFuture<Map<String,Object>> inspectAsync(MinecraftServer s){var f=new CompletableFuture<Map<String,Object>>();s.execute(()->{try{f.complete(inspect(s));}catch(Throwable e){f.completeExceptionally(e);}});return f;}
    public static void initialize(){if(!REQUESTED)return;ServerTickEvents.END_SERVER_TICK.register(s->{if(!active)return;try{var p=player(s);check(s.tickRateManager().tickrate()==20&&!p.isNoGravity(),"Native snow uses original20TPS and gravity");check(p.getTicksFrozen()<p.getTicksRequiredToFreeze(),"Bounded snow fixture must exit before native damage cap; no damage immunity is introduced");check(p.getHealth()==20&&PlayerStamina.get(p).stamina()==100,"No additional cold damage or stamina fee below native freeze cap");SAMPLES.add(inspect(s));}catch(RuntimeException|AssertionError e){failure=e;}});}
    public static void prepare(MinecraftServer s){if(!REQUESTED)return;
        for(int x=5;x<=11;x++)for(int z=-4;z<=5;z++)for(int y=118;y<=124;y++)s.overworld().setBlockAndUpdate(new BlockPos(x,y,z),(y==119?Blocks.STONE:Blocks.AIR).defaultBlockState());
        // A level western exit avoids assuming vanilla unarmored jumping out of a deep snow rim.
        for(int x=4;x<=5;x++)for(int z=-1;z<=1;z++){s.overworld().setBlockAndUpdate(new BlockPos(x,118,z),Blocks.STONE.defaultBlockState());s.overworld().setBlockAndUpdate(new BlockPos(x,119,z),Blocks.AIR.defaultBlockState());}
        for(int x=6;x<=8;x++)for(int z=-1;z<=1;z++){s.overworld().setBlockAndUpdate(new BlockPos(x,118,z),Blocks.STONE.defaultBlockState());for(int y=119;y<=120;y++)s.overworld().setBlockAndUpdate(new BlockPos(x,y,z),Blocks.POWDER_SNOW.defaultBlockState());}
    }
    private static CompletableFuture<Map<String,Object>> inspectWindowAsync(MinecraftServer s){var f=new CompletableFuture<Map<String,Object>>();s.execute(()->{try{var d=inspect(s);d.put("applicationsSnapshot",List.copyOf(APPLICATIONS));f.complete(d);}catch(Throwable e){f.completeExceptionally(e);}});return f;}
    public static void beforeEffect(Entity entity){if(active&&entity instanceof ServerPlayer p){beforeFrozen=p.getTicksFrozen();beforePlayerTick=p.tickCount;}}
    public static void afterEffect(Entity entity){if(!active||!(entity instanceof ServerPlayer p))return;try{
        int strength=CookingEffects.get(p).warmth().strength(),delta=p.getTicksFrozen()-beforeFrozen;int expected=p.canFreeze()&&Math.floorMod(beforePlayerTick,strength+1)==0?Math.min(p.getTicksRequiredToFreeze(),beforeFrozen+1)-beforeFrozen:0;
        var d=new LinkedHashMap<String,Object>();d.put("worldTick",p.level().getGameTime());d.put("playerTick",beforePlayerTick);d.put("warmth",strength);d.put("before",beforeFrozen);d.put("after",p.getTicksFrozen());d.put("delta",delta);d.put("expectedDelta",expected);APPLICATIONS.add(d);check(strength==expectedWarmth&&delta==expected,"Actual native freeze application respects only original warmth stride");
    }catch(RuntimeException|AssertionError e){failure=e;}}
    public static void begin(Minecraft c,int warmth){expectedWarmth=warmth;active=true;pending=inspectAsync(c.getSingleplayerServer());}
    private static void shot(Minecraft c,String name){Screenshot.grab(c.gameDirectory,"c7-native-snow-"+name+".png",c.gameRenderer.mainRenderTarget(),1,m->System.out.println(m.getString()));}
    public static void release(Minecraft c){c.options.keyUp.setDown(false);c.options.keyJump.setDown(false);}
    /** Returns true only after original physical exit and native thaw are actually observed. */
    public static boolean frame(Minecraft c){
        check(failure==null,"Native snow observation failed: "+failure);var s=c.getSingleplayerServer();if(logged!=phase){logged=phase;System.out.println("C7 NATIVE SNOW phase="+phase);}
        if(phase==0&&pending.isDone()){
            var st=pending.join();if(c.player.isUsingItem()||!ActionInput.tickFrame().gameplay()||!Boolean.TRUE.equals(st.get("ground"))){pending=inspectAsync(s);return false;}check(n(st,"frozen")==0&&n(st,"warmth")==expectedWarmth&&Boolean.TRUE.equals(st.get("canFreeze")),"Actual freshly cooked warmth with no prefilled freeze counter");c.player.setYRot(-90);c.player.setXRot(0);c.options.keyUp.setDown(true);pending=inspectAsync(s);phase=1;
        }else if(phase==1&&pending.isDone()){
            var st=pending.join();if(((Number)st.get("x")).doubleValue()<6.45||!Boolean.TRUE.equals(st.get("inPowderSnow"))){pending=inspectAsync(s);return false;}c.options.keyUp.setDown(false);pending=inspectAsync(s);phase=2;
        }else if(phase==2&&pending.isDone()){
            var st=pending.join();if(!Boolean.TRUE.equals(st.get("ground"))||((Number)st.get("y")).doubleValue()>119.01){pending=inspectAsync(s);return false;}check(Boolean.TRUE.equals(st.get("inPowderSnow")),"Native settled powder snow contact");windowStart=st;fromTick=n(st,"worldTick");fromCount=(int)n(st,"effectApplications");shot(c,"settled-before-measurement");pending=inspectWindowAsync(s);phase=3;
        }else if(phase==3&&pending.isDone()){
            var st=pending.join();check(Boolean.TRUE.equals(st.get("inPowderSnow"))&&n(st,"warmth")==expectedWarmth,"Actual steady native contact and effect duration");if(n(st,"worldTick")-fromTick<30){pending=inspectWindowAsync(s);return false;}windowEnd=st;int until=(int)n(st,"effectApplications");windowApplications=until-fromCount;check(windowApplications>=10,"Actual native freeze applications observed");for(var row:((List<Map<String,Object>>)st.get("applicationsSnapshot")).subList(fromCount,until)){windowGrowth+=(int)n(row,"delta");if(n(row,"delta")==0)skipped++;else applied++;}check(n(st,"frozen")-n(windowStart,"frozen")==windowGrowth&&windowGrowth>0,"Native frozen growth equals actual gated applications, with no counter writes");check(expectedWarmth==0?skipped==0:skipped>0,"Vegetable keeps baseline; warming skips native steps");shot(c,"actual-warmth-freeze-window");c.player.setYRot(90);c.options.keyUp.setDown(true);c.options.keyJump.setDown(false);pending=inspectAsync(s);phase=4;
        }else if(phase==4&&pending.isDone()){
            var st=pending.join();if(Boolean.TRUE.equals(st.get("inPowderSnow"))||((Number)st.get("x")).doubleValue()>=5.7){pending=inspectAsync(s);return false;}release(c);shot(c,"native-exit-and-thaw");pending=inspectAsync(s);phase=5;
        }else if(phase==5&&pending.isDone()){
            var st=pending.join();if(n(st,"frozen")!=0||!Boolean.TRUE.equals(st.get("ground"))){pending=inspectAsync(s);return false;}check(!Boolean.TRUE.equals(st.get("inPowderSnow"))&&n(st,"health")==20&&n(st,"stamina")==100,"Native exit/thaw without new penalties");shot(c,"native-thawed-original-player");active=false;return true;
        }
        return false;
    }
    public static Map<String,Object> summary(){var d=new LinkedHashMap<String,Object>();d.put("requested",REQUESTED);d.put("phase",phase);d.put("expectedWarmth",expectedWarmth);d.put("windowStart",windowStart);d.put("windowEnd",windowEnd);d.put("windowApplications",windowApplications);d.put("windowGrowth",windowGrowth);d.put("applied",applied);d.put("skipped",skipped);d.put("frozenCounterWrites",false);d.put("samples",new ArrayList<>(SAMPLES));d.put("actualNativeApplications",new ArrayList<>(APPLICATIONS));return d;}
}
