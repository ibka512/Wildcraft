package dev.wildcraft.test.industry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import dev.wildcraft.client.input.*;
import dev.wildcraft.client.render.GliderHands;
import dev.wildcraft.player.*;
import dev.wildcraft.registry.WildcraftItems;
import dev.wildcraft.traversal.*;
import dev.wildcraft.test.industry.mixin.NativeIndustryConnectionAccess;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Native inventory and traversal input in a generated world, with read-only post-production hand observations. */
public final class NativeTraversalProbe {
    private static final boolean RESUME=Boolean.getBoolean("wildcraft.c7.industryResume"),HOLD_TORCH=Boolean.getBoolean("wildcraft.c7.traversalHolding");
    public static final boolean AIR_FOCUS=Boolean.getBoolean("wildcraft.c7.traversalAirFocus");
    private static final boolean GLIDE_ONLY=Boolean.getBoolean("wildcraft.c7.traversalGlideOnly");
    private static final boolean MANUAL_CLOSE=Boolean.getBoolean("wildcraft.c7.glideManualClose");
    private static final String LANDING_SURFACE=System.getProperty("wildcraft.c7.glideLandingSurface","stone");
    private static final double LANDING_TOP=switch(LANDING_SURFACE){case "stone"->120;case "slab"->119.5;case "carpet"->120.0625;default->throw new IllegalArgumentException("Unknown provided landing surface: "+LANDING_SURFACE);};
    private static final String GLIDER_NAME="C7 Native Paraglider";
    private static final List<Map<String,Object>> ROWS=new ArrayList<>();
    private static Map<String,Object> prior;
    private static ServerPlayer observedPlayer;
    private static volatile Throwable failure;
    private static volatile boolean sampling,observing;
    private static int phase,logged=-1,releaseAt,hiddenFrames,torchHiddenFrames,restoredStickFrames,climbTicks,glideTicks;
    private static long boot,glideFrom,closedAtTick;
    private static double closedAtY,closedAtStamina;
    private static boolean saveCheckpoint;
    private static CompletableFuture<Map<String,Object>> pending;
    private static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
    private static ServerPlayer player(MinecraftServer s){
        // Native disconnect may remove the sole player before SERVER_STOPPING; retain only a read reference.
        var online=s.getPlayerList().getPlayers();if(!online.isEmpty())observedPlayer=online.getFirst();
        if(observedPlayer==null)throw new IllegalStateException("Native traversal owner was never observed");
        return observedPlayer;
    }
    private static int count(ServerPlayer p,Item item){int n=0;for(int i=0;i<36;i++)if(p.getInventory().getItem(i).is(item))n+=p.getInventory().getItem(i).getCount();return n;}
    private static int total(ServerPlayer p,Item item){return count(p,item)+(p.inventoryMenu.getCarried().is(item)?p.inventoryMenu.getCarried().getCount():0)+(GliderEquipment.get(p).is(item)?GliderEquipment.get(p).getCount():0);}
    private static long n(Map<String,Object> d,String k){return ((Number)d.get(k)).longValue();}
    private static Map<String,Object> state(MinecraftServer s){
        var p=player(s);var e=GliderEquipment.get(p);var a=PlayerStamina.get(p);var d=new LinkedHashMap<String,Object>();d.put("worldTick",s.overworld().getGameTime());d.put("owner",p.getStringUUID());d.put("x",p.getX());d.put("y",p.getY());d.put("z",p.getZ());d.put("ground",p.onGround());d.put("collisionBelow001",!p.level().noCollision(p,p.getBoundingBox().move(0,-.01,0)));d.put("supportBlock",p.level().getBlockState(BlockPos.containing(p.getX(),p.getY()-.01,p.getZ())).toString());d.put("health",p.getHealth());d.put("nativeFallDamage",p.getLastDamageSource()!=null&&p.getLastDamageSource().is(net.minecraft.world.damagesource.DamageTypes.FALL));d.put("climbing",Climbing.active(p));d.put("gliding",Gliding.active(p));d.put("climbVertical",Climbing.intent(p).vertical());d.put("stamina",a.stamina());d.put("capacity",a.capacity());d.put("peak",a.highestLevel());d.put("recoveryDelay",a.recoveryDelay());d.put("gliderInventory",count(p,WildcraftItems.PARAGLIDER));d.put("gliderEquipped",e.is(WildcraftItems.PARAGLIDER)?e.getCount():0);d.put("gliderName",e.isEmpty()?"":e.getHoverName().getString());d.put("torch",count(p,Items.TORCH));d.put("stick",count(p,Items.STICK));d.put("stone",count(p,Items.STONE));d.put("selected",p.getInventory().getSelectedSlot());d.put("mainItem",BuiltInRegistries.ITEM.getKey(p.getMainHandItem().getItem()).toString());d.put("mainCount",p.getMainHandItem().getCount());d.put("cursorEmpty",p.inventoryMenu.getCarried().isEmpty());if(AIR_FOCUS)d.putAll(NativeAirFocusProbe.state(p));d.put("clientLoaded",p.connection.hasClientLoaded());d.put("awaitingTeleport",((NativeIndustryConnectionAccess)p.connection).wildcraft$awaitingTeleport()!=null);return d;
    }
    private static CompletableFuture<Map<String,Object>> submit(MinecraftServer s,Supplier<Map<String,Object>> action){var f=new CompletableFuture<Map<String,Object>>();s.execute(()->{try{f.complete(action.get());}catch(Throwable e){f.completeExceptionally(e);}});return f;}
    public static void initialize(){
        observing=true;if(AIR_FOCUS)NativeAirFocusProbe.initialize();
        if(RESUME){try{prior=new Gson().fromJson(Files.readString(Path.of(System.getProperty("wildcraft.c7.industryCheckpoint"))),Map.class);}catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}}
        ServerTickEvents.END_SERVER_TICK.register(s->{if(!sampling)return;try{
            var p=player(s);check(s.tickRateManager().tickrate()==(AIR_FOCUS&&dev.wildcraft.focus.FocusTime.active(s)?5:20),"Only genuine airborne bow may lease5TPS; otherwise20");check(!(Climbing.active(p)&&Gliding.active(p)),"Climb/glide are mutually exclusive");check(p.isAlive()&&!p.isNoGravity(),"Original live gravity remains");
            for(Item item:List.of(WildcraftItems.PARAGLIDER,Items.TORCH,Items.STICK,Items.STONE))check(total(p,item)==1,"Native inventory/cursor/equipment conservation for "+item);
            if(Climbing.active(p))climbTicks++;if(Gliding.active(p))glideTicks++;ROWS.add(state(s));
        }catch(RuntimeException|AssertionError e){failure=e;sampling=false;}});
        ServerLifecycleEvents.SERVER_STOPPING.register(s->{if(saveCheckpoint){var d=new LinkedHashMap<String,Object>(state(s));d.put("savedAtMillis",System.currentTimeMillis());d.put("onlinePlayersAtStop",s.getPlayerList().getPlayerCount());d.put("cachedDisconnectedOwnerRead",s.getPlayerList().getPlayers().isEmpty());write(Minecraft.getInstance().gameDirectory.toPath().resolve("c7-native-traversal-stop.json"),d);}});
    }
    /** Called after the production extraction injections; this method never modifies the render state. */
    public static void observeHands(LocalPlayer p,FirstPersonHandsAndItemsRenderState state){
        if(!observing)return;
        try{
            if(Gliding.active(p)){
                check(Boolean.TRUE.equals(state.getData(GliderHands.OPEN))&&state.mainHandItem.isEmpty()&&state.offHandItem.isEmpty()&&state.handRenderSelection==null&&state.mainHandRenderState.isEmpty()&&state.offHandRenderState.isEmpty(),"Final native first-person ordinary items are hidden while gliding");hiddenFrames++;
                if(p.getMainHandItem().is(Items.TORCH))torchHiddenFrames++;
            }else if(p.getMainHandItem().is(Items.STICK)&&state.mainHandItem.is(Items.STICK)&&!state.mainHandRenderState.isEmpty())restoredStickFrames++;
        }catch(RuntimeException|AssertionError e){failure=e;}
    }
    private static Map<String,Object> setup(MinecraftServer s){
        var p=player(s);if(!p.connection.hasClientLoaded())return Map.of("ready",false);check(GliderEquipment.get(p).isEmpty(),"Only a fresh generated fixture may be populated");
        for(int x=-12;x<=12;x++)for(int z=(AIR_FOCUS?-40:-16);z<=6;z++)for(int y=119;y<=(AIR_FOCUS&&Integer.getInteger("wildcraft.c7.airFocusMode",0)>0?138:134);y++)s.overworld().setBlockAndUpdate(new BlockPos(x,y,z),(y==119?Blocks.STONE:Blocks.AIR).defaultBlockState());
        check(!MANUAL_CLOSE||GLIDE_ONLY,"Manual close is restricted to minimal glide fixture");
        check(GLIDE_ONLY||LANDING_SURFACE.equals("stone"),"Non-full landing variants are restricted to the minimal glide fixture");
        if(!LANDING_SURFACE.equals("stone"))for(int x=-12;x<=12;x++)for(int z=-40;z<=6;z++){
            if(LANDING_SURFACE.equals("slab"))s.overworld().setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE_SLAB.defaultBlockState().setValue(net.minecraft.world.level.block.SlabBlock.TYPE,net.minecraft.world.level.block.state.properties.SlabType.BOTTOM));
            else s.overworld().setBlockAndUpdate(new BlockPos(x,120,z),Blocks.CARPET.white().defaultBlockState());
        }
        if(!GLIDE_ONLY)for(int x=-2;x<=4;x++)for(int y=120;y<=(AIR_FOCUS&&Integer.getInteger("wildcraft.c7.airFocusMode",0)>0?136:132);y++)s.overworld().setBlockAndUpdate(new BlockPos(x,y,2),Blocks.STONE.defaultBlockState());
        if(GLIDE_ONLY)for(int x=1;x<=2;x++)for(int z=0;z<=1;z++)s.overworld().setBlockAndUpdate(new BlockPos(x,127,z),Blocks.STONE.defaultBlockState());
        var glider=new ItemStack(WildcraftItems.PARAGLIDER);glider.set(DataComponents.CUSTOM_NAME,Component.literal(GLIDER_NAME));p.getInventory().clearContent();p.getInventory().setItem(0,glider);p.getInventory().setItem(1,new ItemStack(Items.TORCH));p.getInventory().setItem(2,new ItemStack(Items.STICK));p.getInventory().setItem(3,new ItemStack(Items.STONE));if(AIR_FOCUS&&!GLIDE_ONLY){p.getInventory().setItem(4,new ItemStack(Items.BOW));p.getInventory().setItem(5,new ItemStack(Items.ARROW,2));}p.getInventory().setSelectedSlot(0);p.teleportTo(s.overworld(),1.5,GLIDE_ONLY?128:120,GLIDE_ONLY?.5:1.55,Set.of(),GLIDE_ONLY?180:0,0,true);p.setDeltaMovement(Vec3.ZERO);p.inventoryMenu.broadcastChanges();if(AIR_FOCUS)NativeGroundAudit.begin(p);sampling=true;return state(s);
    }
    private static void key(KeyMapping key){key.setDown(true);KeyMapping.click(InputConstants.getKey(key.saveString()));}
    private static void release(Minecraft c){WildcraftBindings.CLIMB.setDown(false);c.options.keyUp.setDown(false);c.options.keyJump.setDown(false);c.options.keyInventory.setDown(false);c.options.keyHotbarSlots[2].setDown(false);if(AIR_FOCUS)NativeAirFocusProbe.release(c);}
    private static void transfer(Minecraft c,int slot,ContainerInput input){c.gameMode.handleContainerInput(c.player.inventoryMenu.containerId,slot,0,input,c.player);}
    private static void shot(Minecraft c,String name){Screenshot.grab(c.gameDirectory,"c7-native-traversal-"+name+".png",c.gameRenderer.mainRenderTarget(),1,m->System.out.println(m.getString()));}
    private static void write(Path path,Object value){try{Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(value)+"\n");}catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}}
    private static void report(Minecraft c,String status,String message){var d=new LinkedHashMap<String,Object>();d.put("status",status);d.put("message",message);d.put("phase",phase);d.put("resume",RESUME);d.put("holdingTorchProfile",HOLD_TORCH);d.put("nativeGlideOnlyProfile",GLIDE_ONLY);d.put("manualCloseProfile",MANUAL_CLOSE);d.put("manualClosedAtY",closedAtY);d.put("manualClosedAtTick",closedAtTick);d.put("manualClosedAtStamina",closedAtStamina);d.put("providedLandingSurface",LANDING_SURFACE);d.put("providedLandingTopY",LANDING_TOP);if(AIR_FOCUS)d.put("nativeAirFocus",NativeAirFocusProbe.summary());d.put("climbTicks",climbTicks);d.put("glideTicks",glideTicks);d.put("hiddenOrdinaryItemFrames",hiddenFrames);d.put("originalTorchHiddenFrames",torchHiddenFrames);d.put("restoredStickFrames",restoredStickFrames);d.put("configuredMaxFps",c.options.framerateLimit().get());d.put("reportedFPSAtEnd",c.getFps());d.put("providedWorldAndItems",true);d.put("directEquipOrTraversalIntentWrites",false);d.put("manualOrForegroundPerformanceQualified",false);d.put("fullAnimationMatrixQualified",false);d.put("rows",new ArrayList<>(ROWS));write(c.gameDirectory.toPath().resolve("c7-native-traversal.json"),d);}
    public static void frame(Minecraft c){
        long now=System.nanoTime();if(boot==0)boot=now;c.options.pauseOnLostFocus=false;
        try{
            check(failure==null,"Native traversal audit: "+failure);check(System.getProperty("fabric.client.gametest")==null,"No accelerated gametest");check(now-boot<150_000_000_000L,"Native traversal timeout phase"+phase);
            if(c.player==null||c.level==null||c.getSingleplayerServer()==null||c.gui.overlay()!=null)return;var s=c.getSingleplayerServer();
            if(logged!=phase){logged=phase;System.out.println("C7 NATIVE TRAVERSAL phase="+phase);}
            if(AIR_FOCUS&&phase==5&&!NativeAirFocusProbe.configureMode(c))return;
            if(phase==0&&c.gui.screen()==null&&c.player.tickCount>=25){c.options.inactivityFpsLimit().set(InactivityFpsLimit.MINIMIZED);if(RESUME){sampling=true;pending=submit(s,()->state(s));phase=50;}else{pending=submit(s,()->setup(s));phase=1;}
            }else if(phase==1&&pending.isDone()){
                var st=pending.join();if(Boolean.FALSE.equals(st.get("ready"))){phase=0;return;}if(!c.player.onGround()||!Boolean.TRUE.equals(st.get("ground"))||Boolean.TRUE.equals(st.get("awaitingTeleport"))||!GLIDE_ONLY&&!c.level.getBlockState(new BlockPos(1,120,2)).is(Blocks.STONE)){pending=submit(s,()->state(s));return;}key(c.options.keyInventory);phase=2;
            }else if(phase==2&&c.gui.screen() instanceof InventoryScreen){
                c.options.keyInventory.setDown(false);check(c.player.inventoryMenu.slots.size()==47&&GliderEquipment.slot(c.player.inventoryMenu).index==46,"Native E has exactly one extra real equipment slot");shot(c,"real-empty-equipment-slot");transfer(c,39,ContainerInput.PICKUP);transfer(c,46,ContainerInput.PICKUP);transfer(c,39,ContainerInput.PICKUP);pending=submit(s,()->state(s));phase=3;
            }else if(phase==3&&pending.isDone()){
                var st=pending.join();if(!Boolean.TRUE.equals(st.get("cursorEmpty"))||n(st,"stone")!=1){pending=submit(s,()->state(s));return;}check(n(st,"gliderEquipped")==0&&n(st,"gliderInventory")==1,"Native stone placement rejected without moving glider");shot(c,"native-stone-rejected");transfer(c,36,ContainerInput.QUICK_MOVE);pending=submit(s,()->state(s));phase=4;
            }else if(phase==4&&pending.isDone()){
                var st=pending.join();if(n(st,"gliderEquipped")!=1||!GliderEquipment.equipped(c.player)){pending=submit(s,()->state(s));return;}check(n(st,"gliderInventory")==0&&GLIDER_NAME.equals(st.get("gliderName")),"Actual shift move equips same named glider");shot(c,"actual-named-glider-equipped");c.gui.screen().onClose();c.player.getInventory().setSelectedSlot(HOLD_TORCH?1:0);c.options.setCameraType(CameraType.THIRD_PERSON_BACK);pending=submit(s,()->state(s));phase=5;
            }else if(phase==5&&pending.isDone()&&c.gui.screen()==null){
                var st=pending.join();if(!ActionInput.tickFrame().gameplay()||n(st,"selected")!=(HOLD_TORCH?1:0)||!Boolean.TRUE.equals(st.get("ground"))){pending=submit(s,()->state(s));return;}c.options.keyUp.setDown(true);if(!GLIDE_ONLY)key(WildcraftBindings.CLIMB);pending=submit(s,()->state(s));phase=6;
            }else if(phase==6&&pending.isDone()){
                var st=pending.join();if(GLIDE_ONLY){
                    if(Boolean.TRUE.equals(st.get("ground"))||c.player.onGround()||((Number)st.get("y")).doubleValue()>127.9){pending=submit(s,()->state(s));return;}c.options.keyUp.setDown(false);c.options.setCameraType(CameraType.FIRST_PERSON);key(c.options.keyJump);releaseAt=c.player.tickCount+2;pending=submit(s,()->state(s));phase=7;return;
                }if(!Boolean.TRUE.equals(st.get("climbing"))||((Number)st.get("y")).doubleValue()<(AIR_FOCUS?(Integer.getInteger("wildcraft.c7.airFocusMode",0)>0?132.2:128.2):124.2)){pending=submit(s,()->state(s));return;}check(((Number)st.get("stamina")).doubleValue()<100&&n(st,"gliderEquipped")==1,"Actual ascent spends stamina with equipped glider");shot(c,"actual-G-ascent-before-top");WildcraftBindings.CLIMB.setDown(false);c.options.keyUp.setDown(false);c.player.setYRot(180);c.options.setCameraType(CameraType.FIRST_PERSON);key(c.options.keyJump);releaseAt=c.player.tickCount+2;pending=submit(s,()->state(s));phase=7;
            }else if(phase==7){
                if(c.player.tickCount>=releaseAt)c.options.keyJump.setDown(false);if(!pending.isDone())return;var st=pending.join();if(!Boolean.TRUE.equals(st.get("gliding"))){pending=submit(s,()->state(s));return;}check(!Boolean.TRUE.equals(st.get("climbing"))&&n(st,"gliderEquipped")==1,"Airborne jump releases climbing and authorizes real glide");glideFrom=n(st,"worldTick");pending=submit(s,()->state(s));phase=8;
            }else if(phase==8&&pending.isDone()){
                var st=pending.join();if(GLIDE_ONLY){
                    c.options.keyJump.setDown(false);if(MANUAL_CLOSE&&n(st,"worldTick")-glideFrom>=12){check(Boolean.TRUE.equals(st.get("gliding"))&&!Boolean.TRUE.equals(st.get("ground"))&&((Number)st.get("health")).doubleValue()==20,"Manual close begins during a healthy real glide");key(c.options.keyJump);releaseAt=c.player.tickCount+2;pending=submit(s,()->state(s));phase=80;return;}boolean landed=Boolean.TRUE.equals(st.get("ground"))&&c.player.onGround()&&Math.abs(((Number)st.get("y")).doubleValue()-LANDING_TOP)<.001&&Math.abs(c.player.getY()-LANDING_TOP)<.001;
                    if(!landed){check(((Number)st.get("stamina")).doubleValue()>0,"Minimal glide fixture must reach its platform before exhaustion");check(Boolean.TRUE.equals(st.get("gliding"))||Boolean.TRUE.equals(st.get("ground"))&&Math.abs(((Number)st.get("y")).doubleValue()-LANDING_TOP)<.001||c.player.onGround()&&Math.abs(c.player.getY()-LANDING_TOP)<.001,"Glider closed in unsupported air before actual client/server touchdown");pending=submit(s,()->state(s));return;}
                    if(Boolean.TRUE.equals(st.get("gliding"))){pending=submit(s,()->state(s));return;}check(Boolean.TRUE.equals(st.get("collisionBelow001")),"Actual native collision supports the final feet");check(((Number)st.get("health")).doubleValue()==20&&glideTicks>20&&hiddenFrames>0,"Native glide reaches actual platform without damage, with hidden ordinary hands");shot(c,"native-uninterrupted-glide-touchdown");key(c.options.keyHotbarSlots[2]);releaseAt=c.player.tickCount+2;pending=submit(s,()->state(s));phase=9;return;
                }check(Boolean.TRUE.equals(st.get("gliding"))&&!Boolean.TRUE.equals(st.get("climbing")),"Sustained native glide remains mutually exclusive");if(n(st,"worldTick")-glideFrom<12||hiddenFrames<3){pending=submit(s,()->state(s));return;}check(!HOLD_TORCH||torchHiddenFrames>0,"Actual held torch remains inventory-owned but is hidden in final first-person state");shot(c,"actual-open-glider-hidden-ordinary-items");if(AIR_FOCUS){NativeAirFocusProbe.begin(c);phase=20;return;}key(c.options.keyHotbarSlots[2]);releaseAt=c.player.tickCount+2;pending=submit(s,()->state(s));phase=9;
            }else if(phase==80){
                if(c.player.tickCount>=releaseAt)c.options.keyJump.setDown(false);if(!pending.isDone())return;var st=pending.join();if(Boolean.TRUE.equals(st.get("gliding"))||Gliding.active(c.player)){pending=submit(s,()->state(s));return;}check(!Boolean.TRUE.equals(st.get("ground"))&&!c.player.onGround(),"Fresh jump closes canopy before any floor contact");closedAtTick=n(st,"worldTick");closedAtY=((Number)st.get("y")).doubleValue();closedAtStamina=((Number)st.get("stamina")).doubleValue();check(closedAtY>LANDING_TOP+5,"Native fall has enough height for real fall damage");shot(c,"manual-jump-closes-in-air");pending=submit(s,()->state(s));phase=81;
            }else if(phase==81&&pending.isDone()){
                c.options.keyJump.setDown(false);var st=pending.join();check(!Boolean.TRUE.equals(st.get("gliding"))&&!Gliding.active(c.player),"Canopy stays closed after fresh manual input");check(((Number)st.get("stamina")).doubleValue()+1e-6>=closedAtStamina,"Closed canopy charges no extra glide fee");boolean landed=Boolean.TRUE.equals(st.get("ground"))&&c.player.onGround()&&Math.abs(((Number)st.get("y")).doubleValue()-LANDING_TOP)<.001&&Math.abs(c.player.getY()-LANDING_TOP)<.001;
                if(!landed){if(n(st,"worldTick")-closedAtTick>=5)check(((Number)st.get("y")).doubleValue()<closedAtY-.6,"Original gravity resumes after manual close");pending=submit(s,()->state(s));return;}check(Boolean.TRUE.equals(st.get("nativeFallDamage"))&&((Number)st.get("health")).doubleValue()>0&&((Number)st.get("health")).doubleValue()<20,"Actual native fall damage remains after airborne manual close");check(Boolean.TRUE.equals(st.get("collisionBelow001")),"Native manual-close fall reaches supported landing");shot(c,"manual-close-native-fall-damage");key(c.options.keyHotbarSlots[2]);releaseAt=c.player.tickCount+2;pending=submit(s,()->state(s));phase=9;
            }else if(phase==20){
                if(!NativeAirFocusProbe.frame(c))return;key(c.options.keyHotbarSlots[2]);releaseAt=c.player.tickCount+2;pending=submit(s,()->state(s));phase=9;
            }else if(phase==9){
                if(c.player.tickCount>=releaseAt)c.options.keyHotbarSlots[2].setDown(false);if(!pending.isDone())return;var st=pending.join();if(Boolean.TRUE.equals(st.get("gliding"))||n(st,"selected")!=2||!"minecraft:stick".equals(st.get("mainItem"))||restoredStickFrames==0){pending=submit(s,()->state(s));return;}shot(c,"native-item-selection-closes-glider");pending=submit(s,()->state(s));phase=10;
            }else if(phase==10&&pending.isDone()){
                var st=pending.join();if(!Boolean.TRUE.equals(st.get("ground"))||!c.player.onGround()){pending=submit(s,()->state(s));return;}check(n(st,"gliderEquipped")==1&&n(st,"gliderInventory")==0&&n(st,"torch")==1&&n(st,"stick")==1&&n(st,"stone")==1,"Native landing conserves complete provided inventory");key(c.options.keyInventory);phase=11;
            }else if(phase==11&&c.gui.screen() instanceof InventoryScreen){
                c.options.keyInventory.setDown(false);check(GliderEquipment.equipped(c.player),"Real equipment stays equipped after landing");shot(c,"landed-inventory-before-save");c.gui.screen().onClose();release(c);sampling=false;saveCheckpoint=true;report(c,"complete-awaiting-disk-reopen",MANUAL_CLOSE?"Fresh airborne jump closes canopy; original gravity and native fall damage; provided equipment conserved; disk reopen not claimed":GLIDE_ONLY?"Native E equip and uninterrupted glide to actual "+LANDING_SURFACE+" collision touchdown; no G or bow sequence; disk reopen not claimed":"Native E rejection/equip, G ascent, jump glide, first-person ordinary-item hiding, selected item closes and native landing; disk reopen still required");phase=12;c.stop();
            }else if(phase==50&&pending.isDone()){
                var st=pending.join();if(AIR_FOCUS&&Integer.getInteger("wildcraft.c7.airFocusMode",0)>0){if(n(st,"bowMode")!=Integer.getInteger("wildcraft.c7.airFocusMode",0)){pending=submit(s,()->state(s));return;}NativeAirFocusProbe.verifyRestoredMode(c);}for(String key:List.of("owner","gliderName","mainItem"))check(Objects.equals(prior.get(key),st.get(key)),"Actual saved traversal identity/stack: "+key);for(String key:List.of("gliderInventory","gliderEquipped","torch","stick","stone","selected","peak","capacity"))check(n(prior,key)==n(st,key),"Actual equipment/inventory survives disk reopen: "+key);check(!Boolean.TRUE.equals(st.get("climbing"))&&!Boolean.TRUE.equals(st.get("gliding")),"Transient traversal is off after genuine JOIN");if(AIR_FOCUS){for(String k:List.of("bowCount","bowWear","arrowInventory","bowUses"))check(n(prior,k)==n(st,k),"Actual saved bow/arrow once accounting: "+k);check(!dev.wildcraft.focus.FocusTime.active(s)&&s.tickRateManager().tickrate()==20,"No focus lease after real disk JOIN");}key(c.options.keyInventory);phase=51;
            }else if(phase==51&&c.gui.screen() instanceof InventoryScreen){
                c.options.keyInventory.setDown(false);check(c.player.inventoryMenu.slots.size()==47&&GliderEquipment.equipped(c.player),"Actual reopened E shows original glider slot");shot(c,"actual-reopened-equipped-slot");transfer(c,46,ContainerInput.QUICK_MOVE);pending=submit(s,()->state(s));phase=52;
            }else if(phase==52&&pending.isDone()){
                var st=pending.join();if(n(st,"gliderEquipped")!=0||n(st,"gliderInventory")!=1||GliderEquipment.equipped(c.player)){pending=submit(s,()->state(s));return;}shot(c,"native-unequip-no-duplicate");int slot=-1;for(int i=9;i<45;i++)if(c.player.inventoryMenu.getSlot(i).getItem().is(WildcraftItems.PARAGLIDER))slot=i;if(slot<0)return;transfer(c,slot,ContainerInput.QUICK_MOVE);pending=submit(s,()->state(s));phase=53;
            }else if(phase==53&&pending.isDone()){
                var st=pending.join();if(n(st,"gliderEquipped")!=1||!GliderEquipment.equipped(c.player)){pending=submit(s,()->state(s));return;}check(GLIDER_NAME.equals(st.get("gliderName"))&&n(st,"gliderInventory")==0&&n(st,"torch")==1&&n(st,"stick")==1&&n(st,"stone")==1,"Same native named glider re-equips with no extra item");shot(c,"native-reequip-retains-name-and-inventory");c.gui.screen().onClose();sampling=false;observing=false;report(c,"complete","Separate-process actual equipment/inventory restore and native E unequip/re-equip pass");phase=54;c.stop();}
        }catch(RuntimeException|AssertionError e){sampling=false;release(c);shot(c,"failure");report(c,"failed",e.toString());throw e;}
    }
}
