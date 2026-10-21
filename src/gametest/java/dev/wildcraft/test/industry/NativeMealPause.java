package dev.wildcraft.test.industry;

import com.google.gson.*;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.serialization.JsonOps;
import dev.wildcraft.client.fabrication.FabricatorScreen;
import dev.wildcraft.client.focus.FocusSettingsScreen;
import dev.wildcraft.client.input.WildcraftBindings;
import dev.wildcraft.cooking.*;
import dev.wildcraft.fabrication.*;
import dev.wildcraft.test.industry.mixin.NativeIndustryConnectionAccess;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

/** Real food use, normal F8 pause and native save. No synthetic effect-clock or world-time writes. */
public final class NativeMealPause {
    private static final BlockPos POS=new BlockPos(1,120,2);
    private static final boolean RESUME=Boolean.getBoolean("wildcraft.c7.industryResume");
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    private static final List<Map<String,Object>> SAMPLES=new ArrayList<>();
    private static CompletableFuture<Map<String,Object>> pending;
    private static ServerPlayer owner;
    private static ItemStack expected=ItemStack.EMPTY;
    private static Path gameRoot;
    private static Map<String,Object> pauseBefore,pauseAfter;
    private static JsonObject prior;
    private static long boot,joinNano,pauseStart,resumeStart;
    private static int phase,logged=-1,openingFrom;
    private static boolean checkpoint;
    public static void initialize(){ServerPlayerEvents.JOIN.register(p->joinNano=System.nanoTime());ServerLifecycleEvents.SERVER_STOPPING.register(NativeMealPause::stopping);}
    private static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
    private static FabricatorEntity factory(MinecraftServer s){return (FabricatorEntity)s.overworld().getBlockEntity(POS);}
    private static int count(ServerPlayer p,Item item){int n=0;for(int i=0;i<36;i++)if(p.getInventory().getItem(i).is(item))n+=p.getInventory().getItem(i).getCount();return n;}
    private static JsonElement encode(MinecraftServer s,ItemStack item){return ItemStack.CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE,s.registryAccess()),item).getOrThrow();}
    private static Map<String,Object> inspect(MinecraftServer s){
        var p=s.getPlayerList().getPlayers().getFirst();var f=factory(s);var row=new LinkedHashMap<String,Object>();row.put("worldTick",s.overworld().getGameTime());row.put("serverPaused",s.isPaused());row.put("mealMillis",CookingEffects.get(p).cooling().millis());row.put("strength",CookingEffects.get(p).cooling().strength());row.put("bowls",count(p,Items.BOWL));row.put("meals",count(p,CookingContent.MEAL));row.put("factoryProgress",f.hasJob()?f.progress():f.getItem(2).isEmpty()?0:100);row.put("hasJob",f.hasJob());row.put("copper",f.getItem(0).getCount());row.put("redstone",f.getItem(1).getCount());row.put("clientLoaded",p.connection.hasClientLoaded());row.put("awaitingTeleport",((NativeIndustryConnectionAccess)p.connection).wildcraft$awaitingTeleport()!=null);row.put("ground",p.onGround());row.put("sessionActiveUpperMillis",(System.nanoTime()-joinNano)/1_000_000.0);return row;
    }
    private static CompletableFuture<Map<String,Object>> submit(MinecraftServer s,Supplier<Map<String,Object>> a){var f=new CompletableFuture<Map<String,Object>>();s.execute(()->{try{f.complete(a.get());}catch(Throwable e){f.completeExceptionally(e);}});return f;}
    private static long n(Map<String,Object> row,String key){return ((Number)row.get(key)).longValue();}
    private static void write(Path path,Object object){try{Files.writeString(path,GSON.toJson(object)+"\n");}catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}}
    private static void report(Minecraft c,String status,String message){var d=new LinkedHashMap<String,Object>();d.put("status",status);d.put("message",message);d.put("resume",RESUME);d.put("phase",phase);d.put("samples",SAMPLES);d.put("prior",prior);d.put("configuredFrameLimit",c.options.framerateLimit().get());d.put("reportedFPS",c.getFps());d.put("providedMealNotRawCooking",true);d.put("manualPlayVerified",false);d.put("foregroundPerformanceQualified",false);write(c.gameDirectory.toPath().resolve("c7-native-meal-pause.json"),d);}
    private static void stopping(MinecraftServer s){
        if(!checkpoint)return;var f=factory(s);var actual=f.hasJob()?f.pendingResult():f.getItem(2);check(ItemStack.matches(expected,actual),"Native stop preserves same paid output");
        var d=new LinkedHashMap<String,Object>();d.put("worldTick",s.overworld().getGameTime());d.put("factoryProgress",f.hasJob()?f.progress():100);d.put("mealMillis",CookingEffects.get(owner).cooling().millis());d.put("mealEffects",MealEffects.CODEC.encodeStart(JsonOps.INSTANCE,CookingEffects.get(owner)).getOrThrow());d.put("result",encode(s,expected));d.put("uuid",owner.getUUID().toString());d.put("stoppedAtUTCmillis",System.currentTimeMillis());d.put("bowls",count(owner,Items.BOWL));write(gameRoot.resolve("c7-meal-pause-stop.json"),d);
    }
    private static Map<String,Object> setup(MinecraftServer s){
        owner=s.getPlayerList().getPlayers().getFirst();if(!owner.connection.hasClientLoaded())return Map.of("ready",false);
        if(RESUME){
            try{prior=JsonParser.parseString(Files.readString(Path.of(System.getProperty("wildcraft.c7.industryCheckpoint")))).getAsJsonObject();}catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}
            check(owner.getUUID().toString().equals(prior.get("uuid").getAsString()),"Actual saved player identity");expected=ItemStack.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE,s.registryAccess()),prior.get("result")).getOrThrow();var f=factory(s);check(ItemStack.matches(expected,f.hasJob()?f.pendingResult():f.getItem(2)),"Offline time cannot reroll paid result");
            var st=inspect(s);long loss=prior.get("mealMillis").getAsLong()-n(st,"mealMillis");double upper=((Number)st.get("sessionActiveUpperMillis")).doubleValue()+250;
            check(loss>=0&&loss<=upper,"Offline wall time must not be billed as meal time: lost="+loss+", activeUpper="+upper);check(n(st,"strength")==1&&n(st,"bowls")==1&&n(st,"meals")==0,"Real consumed meal/bowl/effect persist without duplication");check(n(st,"copper")==4&&n(st,"redstone")==2,"No second manufacturing debit on reopen");st.put("lossSinceSaveMillis",loss);st.put("wallSinceSaveMillis",System.currentTimeMillis()-prior.get("stoppedAtUTCmillis").getAsLong());return st;
        }
        for(int x=-4;x<=4;x++)for(int z=-4;z<=5;z++)for(int y=119;y<=124;y++)s.overworld().setBlockAndUpdate(new BlockPos(x,y,z),(y==119?Blocks.STONE:Blocks.AIR).defaultBlockState());
        s.overworld().setBlockAndUpdate(POS,FabricationContent.BLOCK.defaultBlockState());factory(s).setItem(0,new ItemStack(Items.COPPER_INGOT,8));factory(s).setItem(1,new ItemStack(Items.REDSTONE,4));
        owner.getInventory().clearContent();owner.getInventory().setItem(0,CookingContent.meal(new MealData(1,MealData.COOLING,1,60)));owner.getInventory().setSelectedSlot(0);owner.teleportTo(s.overworld(),1.5,121,.5,Set.of(),90,-60,true);owner.setDeltaMovement(Vec3.ZERO);owner.inventoryMenu.broadcastChanges();return inspect(s);
    }
    private static void key(KeyMapping k){k.setDown(true);KeyMapping.click(InputConstants.getKey(k.saveString()));}
    private static void shot(Minecraft c,String name){Screenshot.grab(c.gameDirectory,"c7-meal-"+(RESUME?"resume-":"prepare-")+name+".png",c.gameRenderer.mainRenderTarget(),1,m->System.out.println(m.getString()));}
    private static boolean factoryMenu(Minecraft c){return c.gui.screen() instanceof FabricatorScreen screen&&screen.getMenu()==c.player.containerMenu&&screen.getMenu().containerId!=openingFrom;}
    private static void press(Minecraft c,String key){var buttons=c.gui.screen().children().stream().filter(e->e instanceof Button).map(e->(Button)e).filter(b->b.active&&b.visible&&b.getMessage().getString().equals(Component.translatable(key).getString())).toList();check(buttons.size()==1,"One actual native button "+key);buttons.getFirst().onPress(new KeyEvent(InputConstants.KEY_RETURN,0,0));}
    public static void frame(Minecraft c){
        long now=System.nanoTime();if(boot==0){boot=now;gameRoot=c.gameDirectory.toPath();}c.options.pauseOnLostFocus=false;
        try{
            check(System.getProperty("fabric.client.gametest")==null,"Native meal flow cannot use accelerated gametest");check(now-boot<150_000_000_000L,"Native meal pause timed out phase"+phase);if(c.player==null||c.level==null||c.getSingleplayerServer()==null||c.gui.overlay()!=null)return;var s=c.getSingleplayerServer();
            if(logged!=phase){logged=phase;System.out.println("C7 MEAL phase="+phase+" clientPaused="+c.isPaused()+" screen="+(c.gui.screen()==null?"none":c.gui.screen().getClass().getSimpleName()));}
            if(phase==0&&c.gui.screen()==null&&c.player.tickCount>=25){c.options.inactivityFpsLimit().set(InactivityFpsLimit.MINIMIZED);pending=submit(s,()->setup(s));phase=1;
            }else if(phase==1&&pending.isDone()){
                var st=pending.join();if(Boolean.FALSE.equals(st.get("ready"))){phase=0;return;}
                if(RESUME){SAMPLES.add(st);shot(c,"actual-offline-remainder");report(c,"complete","Native eat/F8 paused clocks/resume and real offline remainder verified, not raw cooking or manual play");phase=30;c.stop();return;}
                if(!c.player.onGround()||!Boolean.TRUE.equals(st.get("ground"))||Boolean.TRUE.equals(st.get("awaitingTeleport"))||!c.level.getBlockState(POS).is(FabricationContent.BLOCK)){pending=submit(s,()->inspect(s));return;}c.player.setXRot(-60);c.player.setYRot(90);key(c.options.keyUse);pending=submit(s,()->inspect(s));phase=2;
            }else if(phase==2&&pending.isDone()){
                var st=pending.join();if(n(st,"mealMillis")==0||!c.player.getMainHandItem().is(Items.BOWL)){pending=submit(s,()->inspect(s));return;}c.options.keyUse.setDown(false);check(n(st,"bowls")==1&&n(st,"meals")==0,"Actual use converted exactly one meal into bowl");SAMPLES.add(st);shot(c,"actual-eaten-effect");openingFrom=c.player.containerMenu.containerId;c.gameMode.useItemOn(c.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(POS),Direction.UP,POS,false));phase=3;
            }else if(phase==3&&factoryMenu(c)&&((FabricatorMenu)c.player.containerMenu).status()==0&&c.player.containerMenu.getSlot(0).getItem().getCount()==8){press(c,"fabrication.wildcraft.start");pending=submit(s,()->inspect(s));phase=4;
            }else if(phase==4&&pending.isDone()){
                var st=pending.join();if(!Boolean.TRUE.equals(st.get("hasJob"))){pending=submit(s,()->inspect(s));return;}pending=submit(s,()->{expected=factory(s).pendingResult();check(factory(s).getItem(0).getCount()==4&&factory(s).getItem(1).getCount()==2,"Real button paid exactly4/2");return inspect(s);});phase=5;
            }else if(phase==5&&pending.isDone()){SAMPLES.add(pending.join());c.player.closeContainer();phase=6;
            }else if(phase==6&&c.gui.screen()==null){key(WildcraftBindings.SETTINGS);phase=7;
            }else if(phase==7&&c.gui.screen() instanceof FocusSettingsScreen&&c.isPaused()){WildcraftBindings.SETTINGS.setDown(false);pending=submit(s,()->inspect(s));phase=8;
            }else if(phase==8&&pending.isDone()){
                var st=pending.join();if(!Boolean.TRUE.equals(st.get("serverPaused"))){pending=submit(s,()->inspect(s));return;}pauseBefore=st;SAMPLES.add(st);pauseStart=now;shot(c,"native-F8-paused");phase=9;
            }else if(phase==9&&now-pauseStart>=3_000_000_000L){check(c.isPaused()&&c.gui.screen() instanceof FocusSettingsScreen,"Actual pause menu stayed open for three seconds");pending=submit(s,()->inspect(s));phase=10;
            }else if(phase==10&&pending.isDone()){
                pauseAfter=pending.join();for(String k:List.of("worldTick","factoryProgress","mealMillis"))check(Objects.equals(pauseBefore.get(k),pauseAfter.get(k)),"Actual paused state advanced: "+k+" before="+pauseBefore+", after="+pauseAfter);pauseAfter.put("actualPausedSeconds",(now-pauseStart)/1e9);SAMPLES.add(pauseAfter);shot(c,"unchanged-paused-clocks");press(c,"gui.done");phase=11;
            }else if(phase==11&&c.gui.screen()==null&&!c.isPaused()){resumeStart=now;phase=12;
            }else if(phase==12&&now-resumeStart>=1_500_000_000L){pending=submit(s,()->inspect(s));phase=13;
            }else if(phase==13&&pending.isDone()){
                var st=pending.join();long ticks=n(st,"worldTick")-n(pauseAfter,"worldTick"),loss=n(pauseAfter,"mealMillis")-n(st,"mealMillis");check(ticks>=20&&ticks<=40&&loss>=1000&&loss<=2000,"Resume has native ticks and only actual active meal loss: ticks="+ticks+", millis="+loss);check(n(st,"factoryProgress")==Math.min(100,n(pauseAfter,"factoryProgress")+ticks),"Manufacturing resumes on actual world ticks");SAMPLES.add(st);shot(c,"actual-active-resume");checkpoint=true;report(c,"prepared-for-reopen","Native eating, paused world/effect/job and resumed time pass; real offline reopen still required");phase=14;c.stop();
            }
        }catch(RuntimeException|AssertionError e){c.options.keyUse.setDown(false);WildcraftBindings.SETTINGS.setDown(false);shot(c,"failure");report(c,"failed",e.toString());throw e;}
    }
}
