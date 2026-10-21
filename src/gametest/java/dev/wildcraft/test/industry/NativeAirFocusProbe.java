package dev.wildcraft.test.industry;

import dev.wildcraft.focus.FocusTime;
import dev.wildcraft.player.PlayerStamina;
import dev.wildcraft.traversal.*;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.client.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;

/** Ordinary native inputs; fee, time, inventory, movement and projectile observations are read-only. */
public final class NativeAirFocusProbe {
    private static final List<Map<String,Object>> ROWS=new ArrayList<>();
    private static final List<Map<String,Object>> CLIENT=new ArrayList<>();
    private static final AtomicInteger SHOTS=new AtomicInteger();
    private static CompletableFuture<Map<String,Object>> pending;
    private static Map<String,Object> start,end,unconfirmedStart,unconfirmedEnd,pauseBefore,pauseAfter,resumeObservation;
    private static final boolean PAUSE=Boolean.getBoolean("wildcraft.c7.airFocusPause");
    private static long pauseStarted;
    private static final int MODE=Integer.getInteger("wildcraft.c7.airFocusMode",0);
    private static int settingsPhase,confirmRelease;
    private static boolean confirmed,settingsComplete,restoredMode;
    private static int phase,logged=-1;
    private static boolean nativeReleaseJump;
    private static long started;
    private static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
    private static double n(Map<String,Object>d,String k){return ((Number)d.get(k)).doubleValue();}
    public static void initialize(){NativeGroundAudit.initialize();ServerEntityEvents.ENTITY_LOAD.register((entity,level)->{if(entity instanceof AbstractArrow arrow&&arrow.getOwner() instanceof ServerPlayer)SHOTS.incrementAndGet();});}
    public static Map<String,Object> state(ServerPlayer p){
        var d=new LinkedHashMap<String,Object>();var s=p.level().getServer();var view=p.getAttached(FocusTime.VIEW);int bows=0,arrows=0,wear=0;
        for(int i=0;i<36;i++){var item=p.getInventory().getItem(i);if(item.is(Items.BOW)){bows+=item.getCount();wear+=item.getDamageValue();}if(item.is(Items.ARROW))arrows+=item.getCount();}
        d.put("serverPaused",FocusTime.paused(s));d.put("bowMode",dev.wildcraft.combat.SkillPreferences.bow(p));d.put("worldTick",s.overworld().getGameTime());d.put("observedNano",System.nanoTime());d.put("stamina",PlayerStamina.get(p).stamina());d.put("focusSession",view==null?0:view.session());d.put("focus",FocusTime.active(s));d.put("worldRate",s.tickRateManager().tickrate());d.put("bowSeconds",view==null?0:view.bowSeconds());d.put("nativeUseTicks",p.getTicksUsingItem());d.put("usingBow",p.isUsingItem()&&p.getUseItem().is(Items.BOW));d.put("selected",p.getInventory().getSelectedSlot());d.put("bowCount",bows);d.put("bowWear",wear);d.put("arrowInventory",arrows);d.put("bowUses",p.getStats().getValue(Stats.ITEM_USED.get(Items.BOW)));d.put("actualArrowLoads",SHOTS.get());d.put("ground",p.onGround());d.put("gliding",Gliding.active(p));d.put("climbing",Climbing.active(p));d.put("noGravity",p.isNoGravity());d.put("y",p.getY());d.put("dy",p.getDeltaMovement().y);d.put("health",p.getHealth());return d;
    }
    private static CompletableFuture<Map<String,Object>> inspect(MinecraftServer s){var f=new CompletableFuture<Map<String,Object>>();s.execute(()->{try{var row=state(s.getPlayerList().getPlayers().getFirst());ROWS.add(row);f.complete(row);}catch(Throwable e){f.completeExceptionally(e);}});return f;}
    private static void key(KeyMapping key){key.setDown(true);KeyMapping.click(InputConstants.getKey(key.saveString()));}
    private static void shot(Minecraft c,String name){Screenshot.grab(c.gameDirectory,"c7-native-air-focus-"+name+".png",c.gameRenderer.mainRenderTarget(),1,m->System.out.println(m.getString()));}
    public static void observeClient(Minecraft c,boolean requested,boolean acknowledged,int selected,ItemStack main,ItemStack off){
        if(NativeTraversalProbe.AIR_FOCUS&&c.player!=null)NativeGroundAudit.client(c.player);
        if(!NativeTraversalProbe.AIR_FOCUS||c.player==null||phase<3||phase>5)return;var p=c.player;var view=p.getAttached(Gliding.VIEW);var f=dev.wildcraft.client.input.ActionInput.tickFrame();var d=new LinkedHashMap<String,Object>();
        d.put("phase",phase);d.put("playerTick",p.tickCount);d.put("requested",requested);d.put("acknowledged",acknowledged);d.put("viewActive",view!=null&&view.active());d.put("viewBlocked",view!=null&&view.blocked());d.put("eligible",Gliding.eligible(p));d.put("ground",p.onGround());d.put("y",p.getY());d.put("using",p.isUsingItem());d.put("currentWear",p.getMainHandItem().getDamageValue());d.put("rememberedWear",main.getDamageValue());d.put("itemMatches",ItemStack.matches(main,p.getMainHandItem()));d.put("selected",p.getInventory().getSelectedSlot());d.put("rememberedSelected",selected);d.put("gameplay",f.gameplay());d.put("freshJump",f.fresh(dev.wildcraft.input.Action.JUMP));d.put("triggeredUse",f.triggered(dev.wildcraft.input.Action.USE));CLIENT.add(d);
    }
    private static void buttons(net.minecraft.client.gui.components.events.GuiEventListener e,List<net.minecraft.client.gui.components.Button> out){
        if(e instanceof net.minecraft.client.gui.components.Button b)out.add(b);
        if(e instanceof net.minecraft.client.gui.components.events.ContainerEventHandler container)for(var child:container.children())buttons(child,out);
    }
    private static void press(Minecraft c,net.minecraft.network.chat.Component label){
        var all=new ArrayList<net.minecraft.client.gui.components.Button>();buttons(c.gui.screen(),all);var buttons=all.stream().filter(b->b.active&&b.visible&&b.getMessage().getString().equals(label.getString())).toList();check(buttons.size()==1,"Exactly one native settings button: "+label.getString());buttons.getFirst().onPress(new net.minecraft.client.input.KeyEvent(InputConstants.KEY_RETURN,0,0));
    }
    /** Only native widget activation in the generated run directory; never assigns options/preferences. */
    public static boolean configureMode(Minecraft c){
        if(MODE==0||settingsComplete)return true;check(MODE==1||MODE==2,"Existing bow mode only");
        if(settingsPhase==0&&c.gui.screen()==null&&dev.wildcraft.client.input.ActionInput.tickFrame().gameplay()){key(dev.wildcraft.client.input.WildcraftBindings.SETTINGS);settingsPhase=1;}
        else if(settingsPhase==1&&c.gui.screen() instanceof dev.wildcraft.client.focus.FocusSettingsScreen){dev.wildcraft.client.input.WildcraftBindings.SETTINGS.setDown(false);press(c,net.minecraft.network.chat.Component.translatable("combat.wildcraft.settings"));settingsPhase=2;}
        else if(settingsPhase==2&&c.gui.screen() instanceof dev.wildcraft.client.combat.CombatSettingsScreen){
            var o=dev.wildcraft.client.combat.CombatOptions.get();check(o.bowSkill==0,"Fresh test settings start at auto");
            for(int i=0;i<MODE;i++)press(c,net.minecraft.network.chat.Component.translatable("combat.wildcraft.bow_skill",net.minecraft.network.chat.Component.translatable("combat.wildcraft.skill_mode."+o.bowSkill),dev.wildcraft.client.input.WildcraftBindings.CONFIRM_SKILL.getTranslatedKeyMessage()));
            settingsPhase=20;
        }else if(settingsPhase==20&&c.gui.screen() instanceof dev.wildcraft.client.combat.CombatSettingsScreen){shot(c,"native-F8-mode-"+MODE);press(c,net.minecraft.network.chat.Component.translatable("gui.done"));settingsPhase=3;
        }else if(settingsPhase==3&&c.gui.screen() instanceof dev.wildcraft.client.focus.FocusSettingsScreen){press(c,net.minecraft.network.chat.Component.translatable("gui.done"));settingsPhase=4;}
        else if(settingsPhase==4&&c.gui.screen()==null&&!c.isPaused()){
            if(pending==null)pending=inspect(c.getSingleplayerServer());if(!pending.isDone())return false;var row=pending.join();if(n(row,"bowMode")!=MODE){pending=inspect(c.getSingleplayerServer());return false;}
            try{var saved=com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(c.gameDirectory.toPath().resolve("config/wildcraft-combat.json"))).getAsJsonObject();check(saved.get("bowSkill").getAsInt()==MODE,"Native settings action actually saved generated config");}catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}settingsComplete=true;return true;
        }
        return false;
    }
    public static void verifyRestoredMode(Minecraft c){
        check(MODE>0&&dev.wildcraft.client.combat.CombatOptions.get().bowSkill==MODE,"Real original menu-saved config loaded in separate process");restoredMode=true;
    }
    public static void afterNativeBowRelease(){
        if(!NativeTraversalProbe.AIR_FOCUS||phase!=3||nativeReleaseJump)return;
        var c=Minecraft.getInstance();if(c.player==null||!c.player.getMainHandItem().is(Items.BOW))return;
        // Earliest genuine fresh jump after the native use-release path; never delay inventory packets.
        key(c.options.keyJump);nativeReleaseJump=true;
    }
    public static void release(Minecraft c){c.options.keyUse.setDown(false);dev.wildcraft.client.input.WildcraftBindings.SETTINGS.setDown(false);dev.wildcraft.client.input.WildcraftBindings.CONFIRM_SKILL.setDown(false);c.options.keyJump.setDown(false);c.options.keyHotbarSlots[4].setDown(false);}
    public static void begin(Minecraft c){c.options.keyJump.setDown(false);key(c.options.keyHotbarSlots[4]);pending=inspect(c.getSingleplayerServer());started=System.nanoTime();}
    public static boolean frame(Minecraft c){
        if(confirmed&&phase>=2)dev.wildcraft.client.input.WildcraftBindings.CONFIRM_SKILL.setDown(false);
        check(System.nanoTime()-started<30_000_000_000L,"Native air focus timeout phase"+phase);if(!pending.isDone())return false;var s=c.getSingleplayerServer();var row=pending.join();
        if(logged!=phase){logged=phase;System.out.println("C7 NATIVE AIR FOCUS phase="+phase);}
        check(!Boolean.TRUE.equals(row.get("noGravity"))&&n(row,"health")==20,"Original live gravity/health");
        if(phase==0){
            if(n(row,"selected")!=4||Boolean.TRUE.equals(row.get("gliding"))||!c.player.getMainHandItem().is(Items.BOW)){pending=inspect(s);return false;}
            c.options.keyHotbarSlots[4].setDown(false);check(!Boolean.TRUE.equals(row.get("ground"))&&n(row,"bowCount")==1&&n(row,"arrowInventory")==2&&n(row,"bowWear")==0&&n(row,"actualArrowLoads")==0,"Native take-out closes glider before use; pristine bow and two arrows");shot(c,"native-bow-selection-closes-canopy");c.player.setXRot(-15);key(c.options.keyUse);pending=inspect(s);phase=1;
        }else if(phase==1){
            if(MODE==2&&Boolean.TRUE.equals(row.get("usingBow"))){check(!Boolean.TRUE.equals(row.get("focus"))&&n(row,"worldRate")==20&&n(row,"bowMode")==2,"Disabled stays original time");start=row;pending=inspect(s);phase=7;return false;}
            if(MODE==1&&!confirmed&&Boolean.TRUE.equals(row.get("usingBow"))){
                check(!Boolean.TRUE.equals(row.get("focus"))&&n(row,"worldRate")==20&&n(row,"bowMode")==1,"Manual cannot start before actual H");if(unconfirmedStart==null)unconfirmedStart=row;
                check(Math.abs(n(row,"stamina")-n(unconfirmedStart,"stamina"))<.00001,"No focus fee while manual ready");
                if(n(row,"worldTick")-n(unconfirmedStart,"worldTick")>=4){unconfirmedEnd=row;shot(c,"actual-manual-ready-no-charge");key(dev.wildcraft.client.input.WildcraftBindings.CONFIRM_SKILL);confirmed=true;confirmRelease=c.player.tickCount+2;}
                pending=inspect(s);return false;
            }
            if(!Boolean.TRUE.equals(row.get("focus"))){pending=inspect(s);return false;}
            check(Boolean.TRUE.equals(row.get("usingBow"))&&!Boolean.TRUE.equals(row.get("gliding"))&&!Boolean.TRUE.equals(row.get("climbing"))&&!Boolean.TRUE.equals(row.get("ground"))&&n(row,"worldRate")==5,"Real native bow starts exclusive5TPS focus");start=row;shot(c,"actual-native-five-tps-draw");pending=inspect(s);phase=2;
        }else if(phase==2){
            check(Boolean.TRUE.equals(row.get("focus"))&&n(row,"worldRate")==5&&Boolean.TRUE.equals(row.get("usingBow"))&&!Boolean.TRUE.equals(row.get("ground")),"Native uninterrupted airborne draw");
            check(n(row,"arrowInventory")==2&&n(row,"bowWear")==0&&n(row,"actualArrowLoads")==0,"No shot/ammo/wear before release");
            double seconds=(n(row,"observedNano")-n(start,"observedNano"))/1e9;if(seconds<1.2){pending=inspect(s);return false;}
            double cost=n(start,"stamina")-n(row,"stamina");check(Math.abs(cost-seconds*10)<2.6&&cost>=10,"Actual real-time10 stamina/sec fee");check(n(row,"nativeUseTicks")>=20&&n(row,"worldTick")-n(start,"worldTick")<15,"Native full pull compensated in real seconds, not20 slow world ticks");end=row;shot(c,"actual-paid-full-pull");if(PAUSE){key(dev.wildcraft.client.input.WildcraftBindings.SETTINGS);pending=inspect(s);phase=8;return false;}c.options.keyUse.setDown(false);pending=inspect(s);phase=3;
        }else if(phase==8){
            if(!(c.gui.screen() instanceof dev.wildcraft.client.focus.FocusSettingsScreen)||!c.isPaused()||!Boolean.TRUE.equals(row.get("serverPaused"))||Boolean.TRUE.equals(row.get("focus"))){pending=inspect(s);return false;}
            dev.wildcraft.client.input.WildcraftBindings.SETTINGS.setDown(false);check(n(row,"worldRate")==20,"Actual F8 pause releases original time lease");pauseBefore=row;pauseStarted=System.nanoTime();shot(c,"actual-air-F8-paused");pending=inspect(s);phase=9;
        }else if(phase==9){
            check(c.isPaused()&&c.gui.screen() instanceof dev.wildcraft.client.focus.FocusSettingsScreen,"Native pause remains open");if(System.nanoTime()-pauseStarted<1_000_000_000L){pending=inspect(s);return false;}pending=inspect(s);phase=10;
        }else if(phase==10){
            pauseAfter=row;for(String k:List.of("worldTick","stamina","nativeUseTicks","bowWear","arrowInventory","actualArrowLoads","bowUses"))check(Objects.equals(pauseBefore.get(k),row.get(k)),"Actual paused bow state advanced: "+k);check(!Boolean.TRUE.equals(row.get("focus"))&&Boolean.TRUE.equals(row.get("serverPaused")),"Original skill stays canceled while paused");shot(c,"actual-air-pause-unchanged");press(c,net.minecraft.network.chat.Component.translatable("gui.done"));pending=inspect(s);phase=11;
        }else if(phase==11){
            if(c.gui.screen()!=null||c.isPaused()||Boolean.TRUE.equals(row.get("serverPaused"))){pending=inspect(s);return false;}resumeObservation=row;if(Boolean.TRUE.equals(row.get("usingBow")))check(Boolean.TRUE.equals(row.get("focus"))&&n(row,"worldRate")==5&&n(row,"focusSession")!=n(end,"focusSession"),"Still-using original auto bow acquires fresh session after pause");else check(!Boolean.TRUE.equals(row.get("focus"))&&n(row,"worldRate")==20&&n(row,"actualArrowLoads")==1&&n(row,"bowWear")==1&&n(row,"arrowInventory")==1,"Native GUI already released bow once; do not reenter or replay release");check(n(pauseAfter,"stamina")-n(row,"stamina")>=0&&n(pauseAfter,"stamina")-n(row,"stamina")<2.6,"Resumed first interval does not charge the paused second");c.options.keyUse.setDown(false);pending=inspect(s);phase=3;
        }else if(phase==7){
            check(!Boolean.TRUE.equals(row.get("focus"))&&n(row,"worldRate")==20&&Boolean.TRUE.equals(row.get("usingBow"))&&!Boolean.TRUE.equals(row.get("ground")),"Disabled native draw in original air/time");check(Math.abs(n(row,"stamina")-n(start,"stamina"))<.00001&&n(row,"arrowInventory")==2&&n(row,"bowWear")==0,"Disabled has no focus fee or premature shot");
            if(n(row,"worldTick")-n(start,"worldTick")<6){pending=inspect(s);return false;}end=row;shot(c,"actual-disabled-native-draw-no-charge");c.options.keyUse.setDown(false);pending=inspect(s);phase=3;
        }else if(phase==3){
            if(Boolean.TRUE.equals(row.get("focus"))||n(row,"bowWear")!=1||n(row,"arrowInventory")!=1){pending=inspect(s);return false;}
            check(n(row,"worldRate")==20&&n(row,"actualArrowLoads")==1&&n(row,"bowUses")==1&&!Boolean.TRUE.equals(row.get("usingBow")),"Native release once: one arrow/ammo/wear/stat and20TPS restored");shot(c,"actual-once-shot-native-time-restored");if(!nativeReleaseJump)key(c.options.keyJump);pending=inspect(s);phase=4;
        }else if(phase==4){
            if(!Boolean.TRUE.equals(row.get("gliding"))){pending=inspect(s);return false;}c.options.keyJump.setDown(false);check(!Boolean.TRUE.equals(row.get("focus"))&&n(row,"worldRate")==20,"Fresh native jump reopens equipped glider after release");shot(c,"native-reopen-canopy-after-shot");pending=inspect(s);phase=5;
        }else if(phase==5){
            check(n(row,"bowWear")==1&&n(row,"arrowInventory")==1&&n(row,"actualArrowLoads")==1,"No duplicate shot on transition");if(!Boolean.TRUE.equals(row.get("ground"))||!c.player.onGround()||Math.abs(n(row,"y")-120)>.01||Math.abs(c.player.getY()-120)>.01){pending=inspect(s);return false;}check(!Boolean.TRUE.equals(row.get("focus"))&&!Boolean.TRUE.equals(row.get("gliding"))&&n(row,"worldRate")==20,"Actual glider landing clears transient modes");shot(c,"native-safe-landing-with-bow");release(c);phase=6;return true;
        }
        return false;
    }
    public static Map<String,Object> summary(){var d=new LinkedHashMap<String,Object>();d.put("phase",phase);d.put("mode",MODE);d.put("settingsComplete",settingsComplete);d.put("restoredMode",restoredMode);d.put("pauseRequested",PAUSE);d.put("pauseBefore",pauseBefore);d.put("pauseAfter",pauseAfter);d.put("resumeObservation",resumeObservation);d.put("confirmed",confirmed);d.put("unconfirmedStart",unconfirmedStart);d.put("unconfirmedEnd",unconfirmedEnd);d.put("jumpAfterNativeRelease",nativeReleaseJump);d.put("start",start);d.put("paidHoldEnd",end);d.put("rows",new ArrayList<>(ROWS));d.put("actualArrowLoads",SHOTS.get());d.put("clientControlObservations",new ArrayList<>(CLIENT));d.put("nativeGroundAudit",NativeGroundAudit.summary());d.put("directGravityOrTimeOrFeeWrites",false);return d;}
}
