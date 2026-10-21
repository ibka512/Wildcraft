package dev.wildcraft.test.industry;

import com.google.gson.*;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.serialization.JsonOps;
import dev.wildcraft.client.input.*;
import dev.wildcraft.combat.MeleeActions;
import dev.wildcraft.fuse.*;
import dev.wildcraft.player.PlayerStamina;
import dev.wildcraft.test.industry.mixin.NativeIndustryConnectionAccess;
import java.nio.file.*;
import java.io.*;
import net.minecraft.nbt.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

/** Native F/V/hotbar/attack/sneak inputs only; server intent and damage observations never change their result. */
public final class NativeFuseProbe {
    private static final boolean RESUME=Boolean.getBoolean("wildcraft.c7.industryResume"),CODEC_AUDIT=Boolean.getBoolean("wildcraft.c7.fuseCodecAudit");
    private static final String MATERIAL_NAME="C7 Native Metal";
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    private static final List<Map<String,Object>> ROWS=new ArrayList<>(),INTENTS=new ArrayList<>(),HITS=new ArrayList<>(),CODEC_REPORT=new ArrayList<>();
    private static volatile boolean enabled,sampling;
    private static volatile Throwable failure;
    private static volatile int intentSequence,expectedVisibleSlot=-1;
    private static volatile Map<String,Object> latestIntent=Map.of();
    private static ServerPlayer owner;
    private static UUID targetUUID;
    private static Map<String,Object> prior;
    private static CompletableFuture<Map<String,Object>> pending;
    private static long boot,waitFrom;
    private static int phase,logged=-1,releaseAt,beforeIntent,damageEvents;
    private static boolean saveCheckpoint;
    private static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
    private static long n(Map<String,Object> d,String k){return ((Number)d.get(k)).longValue();}
    private static ServerPlayer player(MinecraftServer s){var ps=s.getPlayerList().getPlayers();if(!ps.isEmpty())owner=ps.getFirst();if(owner==null)throw new IllegalStateException("No native Fuse owner observed");return owner;}
    private static LivingEntity target(MinecraftServer s){return targetUUID==null?null:(LivingEntity)s.overworld().getEntity(targetUUID);}
    private static int uses(ItemStack s){var d=s.get(FusionContent.DATA);return d==null?-1:d.remaining();}
    private static int rawIron(ServerPlayer p){int n=p.getOffhandItem().is(Items.IRON_INGOT)?p.getOffhandItem().getCount():0;for(int i=0;i<36;i++)if(p.getInventory().getItem(i).is(Items.IRON_INGOT))n+=p.getInventory().getItem(i).getCount();return n;}
    private static void materialMetadata(ItemStack m){check(m.is(Items.IRON_INGOT)&&m.getHoverName().getString().equals(MATERIAL_NAME)&&Boolean.TRUE.equals(m.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE)),"Original native material name/glint preserved");}
    private static Map<String,Object> state(MinecraftServer s){
        var p=player(s);var a=p.getInventory().getItem(0);var b=p.getInventory().getItem(1);var off=p.getOffhandItem();var t=target(s);var d=new LinkedHashMap<String,Object>();
        d.put("worldTick",s.overworld().getGameTime());d.put("owner",p.getStringUUID());d.put("selected",p.getInventory().getSelectedSlot());d.put("hostAName",a.getHoverName().getString());d.put("hostBName",b.getHoverName().getString());d.put("Auses",uses(a));d.put("Buses",uses(b));d.put("Awear",a.getDamageValue());d.put("Bwear",b.getDamageValue());d.put("offIron",off.is(Items.IRON_INGOT)?off.getCount():0);d.put("offWear",off.getOrDefault(FusionContent.WEAR,32));d.put("rawIron",rawIron(p));d.put("damageEvents",damageEvents);d.put("targetLoaded",t!=null);d.put("targetId",t==null?-1:t.getId());d.put("targetUUID",targetUUID==null?"":targetUUID.toString());d.put("targetHealth",t==null?-1:t.getHealth());d.put("attackScale",p.getAttackStrengthScale(.5F));d.put("meleePhase",MeleeActions.view(p).phase());d.put("stamina",PlayerStamina.get(p).stamina());d.put("ground",p.onGround());d.put("health",p.getHealth());d.put("cursorEmpty",p.inventoryMenu.getCarried().isEmpty());d.put("clientLoaded",p.connection.hasClientLoaded());d.put("awaitingTeleport",((NativeIndustryConnectionAccess)p.connection).wildcraft$awaitingTeleport()!=null);return d;
    }
    private static String encodeNative(MinecraftServer s,ItemStack stack){
        try{var bytes=new ByteArrayOutputStream();var tag=(CompoundTag)ItemStack.CODEC.encodeStart(RegistryOps.create(NbtOps.INSTANCE,s.registryAccess()),stack).getOrThrow();NbtIo.write(tag,new DataOutputStream(bytes));return Base64.getEncoder().encodeToString(bytes.toByteArray());}catch(IOException e){throw new UncheckedIOException(e);}
    }
    private static ItemStack decodeNative(MinecraftServer s,String encoded){
        try{var bytes=Base64.getDecoder().decode(encoded);check(bytes.length<=1_048_576,"Bounded local native QA checkpoint");var tag=NbtIo.read(new DataInputStream(new ByteArrayInputStream(bytes)),NbtAccounter.create(1_048_576));return ItemStack.CODEC.parse(RegistryOps.create(NbtOps.INSTANCE,s.registryAccess()),tag).getOrThrow();}catch(IOException e){throw new UncheckedIOException(e);}
    }
    private static CompletableFuture<Map<String,Object>> submit(MinecraftServer s,Supplier<Map<String,Object>> action){var f=new CompletableFuture<Map<String,Object>>();s.execute(()->{try{f.complete(action.get());}catch(Throwable e){f.completeExceptionally(e);}});return f;}
    public static void initialize(){
        enabled=true;
        if(RESUME){try{prior=GSON.fromJson(Files.readString(Path.of(System.getProperty("wildcraft.c7.industryCheckpoint"))),Map.class);targetUUID=UUID.fromString((String)prior.get("targetUUID"));}catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}}
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity,source,base,taken,blocked)->{if(enabled&&entity.getUUID().equals(targetUUID)&&source.getDirectEntity() instanceof ServerPlayer&&taken>0){damageEvents++;HITS.add(Map.of("worldTick",entity.level().getGameTime(),"taken",taken,"blocked",blocked,"health",entity.getHealth()));}});
        ServerTickEvents.END_SERVER_TICK.register(s->{if(!sampling)return;try{
            var p=player(s);check(s.tickRateManager().tickrate()==20,"Native Fuse remains20TPS");check(p.getInventory().getItem(0).is(Items.IRON_SWORD)&&p.getInventory().getItem(1).is(Items.STONE_SWORD),"Actual two host slots retained");
            int embedded=0;for(int i=0;i<2;i++){var h=p.getInventory().getItem(i);var f=h.get(FusionContent.DATA);if(f!=null){check(!f.isProjection(),"Server owns real bounded data");materialMetadata(f.material(s.registryAccess()).orElseThrow());embedded++;}}
            if(p.getOffhandItem().is(Items.IRON_INGOT))materialMetadata(p.getOffhandItem());check(rawIron(p)+embedded==2,"Exactly two raw-or-fused material units");check(p.getHealth()==20&&!p.isNoGravity(),"Original live gravity, no incoming target damage");ROWS.add(state(s));
        }catch(RuntimeException|AssertionError e){failure=e;sampling=false;}});
        ServerLifecycleEvents.SERVER_STOPPING.register(s->{if(saveCheckpoint){var d=new LinkedHashMap<String,Object>(state(s));var ops=RegistryOps.create(JsonOps.INSTANCE,s.registryAccess());d.put("hostAStack",ItemStack.CODEC.encodeStart(ops,player(s).getInventory().getItem(0)).getOrThrow());d.put("hostBStack",ItemStack.CODEC.encodeStart(ops,player(s).getInventory().getItem(1)).getOrThrow());d.put("checkpointFormat","native-nbt-base64-v1");for(int i=0;i<2;i++){var host=player(s).getInventory().getItem(i);var lossless=encodeNative(s,host);check(ItemStack.matches(host,decodeNative(s,lossless)),"Native checkpoint self-roundtrip before saving");d.put(i==0?"hostANbt":"hostBNbt",lossless);}d.put("savedAtMillis",System.currentTimeMillis());d.put("onlineAtStop",s.getPlayerList().getPlayerCount());write(Minecraft.getInstance().gameDirectory.toPath().resolve("c7-native-fuse-stop.json"),d);}});
    }
    /** RETURN observer of the real production receive, not a replacement call or packet interceptor. */
    public static void received(ServerPlayer p,FusionIntent input,boolean accepted){if(!enabled)return;var d=new LinkedHashMap<String,Object>();d.put("sequence",++intentSequence);d.put("worldTick",p.level().getGameTime());d.put("expectedVisibleSlotAtPress",expectedVisibleSlot);d.put("actualServerSlot",p.getInventory().getSelectedSlot());d.put("actualMainName",p.getMainHandItem().getHoverName().getString());d.put("split",input.split());d.put("accepted",accepted);d.put("Auses",uses(p.getInventory().getItem(0)));d.put("Buses",uses(p.getInventory().getItem(1)));d.put("offCount",p.getOffhandItem().getCount());latestIntent=d;INTENTS.add(d);}
    private static Map<String,Object> setup(MinecraftServer s){
        var p=player(s);if(!p.connection.hasClientLoaded())return Map.of("ready",false);
        for(int x=-6;x<=6;x++)for(int z=-6;z<=12;z++)for(int y=119;y<=124;y++)s.overworld().setBlockAndUpdate(new BlockPos(x,y,z),(y==119?Blocks.STONE:Blocks.AIR).defaultBlockState());
        var a=new ItemStack(Items.IRON_SWORD);a.set(DataComponents.CUSTOM_NAME,Component.literal("C7 Host A"));var b=new ItemStack(Items.STONE_SWORD);b.set(DataComponents.CUSTOM_NAME,Component.literal("C7 Host B"));var m=new ItemStack(Items.IRON_INGOT,2);m.set(DataComponents.CUSTOM_NAME,Component.literal(MATERIAL_NAME));m.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE,true);
        p.getInventory().clearContent();p.getInventory().setItem(0,a);p.getInventory().setItem(1,b);p.getInventory().setItem(2,m);p.getInventory().setSelectedSlot(0);p.teleportTo(s.overworld(),.5,120,.5,Set.of(),0,0,true);p.setDeltaMovement(Vec3.ZERO);p.inventoryMenu.broadcastChanges();
        var target=EntityTypes.VILLAGER.create(s.overworld(),EntitySpawnReason.COMMAND);check(target!=null,"Native target creation");target.setNoAi(true);target.setPos(.5,120,2.5);s.overworld().addFreshEntity(target);targetUUID=target.getUUID();sampling=true;return state(s);
    }
    private static void key(KeyMapping key){key.setDown(true);KeyMapping.click(InputConstants.getKey(key.saveString()));}
    private static void release(Minecraft c){WildcraftBindings.FUSE.setDown(false);c.options.keyShift.setDown(false);c.options.keySwapOffhand.setDown(false);c.options.keyAttack.setDown(false);c.options.keyUp.setDown(false);c.options.keyInventory.setDown(false);for(int i=0;i<3;i++)c.options.keyHotbarSlots[i].setDown(false);}
    private static void fuse(Minecraft c){expectedVisibleSlot=c.player.getInventory().getSelectedSlot();beforeIntent=intentSequence;key(WildcraftBindings.FUSE);releaseAt=c.player.tickCount+2;}
    private static boolean intent(boolean split,int slot,boolean accepted){if(intentSequence<=beforeIntent)return false;var d=latestIntent;check(Boolean.valueOf(split).equals(d.get("split")),"Actual split modifier");check(n(d,"actualServerSlot")==slot&&Boolean.valueOf(accepted).equals(d.get("accepted")),"Visible slot "+slot+" Fuse ownership: "+d);return true;}
    private static boolean aim(Minecraft c,int id){
        var e=c.level.getEntity(id);if(e==null)return false;var v=e.getBoundingBox().getCenter().subtract(c.player.getEyePosition());double horizontal=Math.hypot(v.x,v.z);c.player.setYRot((float)Math.toDegrees(Math.atan2(-v.x,v.z)));c.player.setXRot((float)-Math.toDegrees(Math.atan2(v.y,horizontal)));
        c.options.keyUp.setDown(horizontal>2.4);return horizontal<=2.4&&c.hitResult instanceof EntityHitResult hit&&hit.getEntity().getId()==id;
    }
    private static void shot(Minecraft c,String name){Screenshot.grab(c.gameDirectory,"c7-native-fuse-"+name+".png",c.gameRenderer.mainRenderTarget(),1,m->System.out.println(m.getString()));}
    private static void write(Path file,Object d){try{Files.writeString(file,GSON.toJson(d)+"\n");}catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}}
    private static void report(Minecraft c,String status,String message){var d=new LinkedHashMap<String,Object>();d.put("status",status);d.put("message",message);d.put("phase",phase);d.put("resume",RESUME);d.put("actualDamageEvents",damageEvents);d.put("configuredMaxFps",c.options.framerateLimit().get());d.put("reportedFPSAtEnd",c.getFps());d.put("providedItemsAndNoAiTarget",true);d.put("directFuseSplitAttackOrHostWritesAfterSetup",false);d.put("networkDelayedOrIntercepted",false);d.put("manualOrForegroundPerformanceQualified",false);d.put("intents",new ArrayList<>(INTENTS));d.put("hits",new ArrayList<>(HITS));d.put("codecAudit",new ArrayList<>(CODEC_REPORT));d.put("rows",new ArrayList<>(ROWS));write(c.gameDirectory.toPath().resolve("c7-native-fuse.json"),d);}
    public static void frame(Minecraft c){
        long now=System.nanoTime();if(boot==0)boot=now;c.options.pauseOnLostFocus=false;
        try{
            check(failure==null,"Native Fuse audit: "+failure);check(System.getProperty("fabric.client.gametest")==null,"No accelerated gametest");check(now-boot<150_000_000_000L,"Native Fuse timeout phase"+phase);
            if(c.player==null||c.level==null||c.getSingleplayerServer()==null||c.gui.overlay()!=null)return;var s=c.getSingleplayerServer();if(logged!=phase){logged=phase;System.out.println("C7 NATIVE FUSE phase="+phase);}
            if(c.player.tickCount>=releaseAt){WildcraftBindings.FUSE.setDown(false);c.options.keySwapOffhand.setDown(false);for(int i=0;i<3;i++)c.options.keyHotbarSlots[i].setDown(false);}
            if(phase==0&&c.gui.screen()==null&&c.player.tickCount>=25){c.options.inactivityFpsLimit().set(InactivityFpsLimit.MINIMIZED);if(RESUME){sampling=true;pending=submit(s,()->state(s));phase=50;}else{pending=submit(s,()->setup(s));phase=1;}
            }else if(phase==1&&pending.isDone()){
                var st=pending.join();if(Boolean.FALSE.equals(st.get("ready"))){phase=0;return;}if(!c.player.onGround()||!Boolean.TRUE.equals(st.get("ground"))||Boolean.TRUE.equals(st.get("awaitingTeleport"))||!Boolean.TRUE.equals(st.get("targetLoaded"))){pending=submit(s,()->state(s));return;}key(c.options.keyHotbarSlots[2]);releaseAt=c.player.tickCount+2;pending=submit(s,()->state(s));phase=2;
            }else if(phase==2&&pending.isDone()){
                var st=pending.join();if(n(st,"selected")!=2||c.player.getInventory().getSelectedSlot()!=2){pending=submit(s,()->state(s));return;}key(c.options.keySwapOffhand);releaseAt=c.player.tickCount+2;pending=submit(s,()->state(s));phase=3;
            }else if(phase==3&&pending.isDone()){
                var st=pending.join();if(n(st,"offIron")!=2){pending=submit(s,()->state(s));return;}shot(c,"actual-F-material-to-offhand");key(c.options.keyHotbarSlots[0]);releaseAt=c.player.tickCount+2;pending=submit(s,()->state(s));phase=4;
            }else if(phase==4&&pending.isDone()){
                var st=pending.join();if(n(st,"selected")!=0||c.player.getInventory().getSelectedSlot()!=0||!ActionInput.tickFrame().gameplay()){pending=submit(s,()->state(s));return;}fuse(c);pending=submit(s,()->state(s));phase=5;
            }else if(phase==5&&pending.isDone()&&intent(false,0,true)){
                var st=pending.join();if(n(st,"Auses")!=32||n(st,"offIron")!=1){pending=submit(s,()->state(s));return;}check(n(st,"Buses")==-1&&n(st,"Awear")==0,"Normal A native Fuse consumes one offhand item");shot(c,"actual-A-fused-once");waitFrom=n(st,"worldTick");pending=submit(s,()->state(s));phase=6;
            }else if(phase==6&&pending.isDone()){
                var st=pending.join();if(n(st,"worldTick")-waitFrom<5){pending=submit(s,()->state(s));return;}fuse(c);pending=submit(s,()->state(s));phase=7;
            }else if(phase==7&&pending.isDone()&&intent(false,0,false)){
                var st=pending.join();check(n(st,"Auses")==32&&n(st,"offIron")==1,"Already fused host does not debit twice");waitFrom=n(st,"worldTick");pending=submit(s,()->state(s));phase=70;
            }else if(phase==70&&pending.isDone()){
                var st=pending.join();if(n(st,"worldTick")-waitFrom<5){pending=submit(s,()->state(s));return;}key(c.options.keyHotbarSlots[1]);releaseAt=c.player.tickCount+2;phase=8;
            }else if(phase==8&&c.player.getInventory().getSelectedSlot()==1&&c.player.getMainHandItem().is(Items.STONE_SWORD)){
                // This is the first observed native frame with B already selected. No manual slot write or sync.
                fuse(c);pending=submit(s,()->state(s));phase=9;
            }else if(phase==9&&pending.isDone()&&intent(false,1,true)){
                var st=pending.join();if(n(st,"Buses")!=32||n(st,"offIron")!=0){pending=submit(s,()->state(s));return;}check(n(st,"Auses")==32,"Quick visible B Fuse never mutates previous host A");shot(c,"native-quick-switch-B-fused");pending=submit(s,()->state(s));phase=10;
            }else if(phase==10&&pending.isDone()){
                var st=pending.join();if(((Number)st.get("attackScale")).doubleValue()<.99||n(st,"meleePhase")!=0||!aim(c,(int)n(st,"targetId"))){pending=submit(s,()->state(s));return;}key(c.options.keyAttack);releaseAt=c.player.tickCount+1;pending=submit(s,()->state(s));phase=11;
            }else if(phase==11){
                if(c.player.tickCount>=releaseAt)c.options.keyAttack.setDown(false);if(!pending.isDone())return;var st=pending.join();if(n(st,"Buses")!=31||n(st,"Bwear")!=1){pending=submit(s,()->state(s));return;}check(n(st,"damageEvents")==1&&n(st,"Auses")==32&&n(st,"Awear")==0&&((Number)st.get("stamina")).doubleValue()==100,"One actual ordinary hit/wear/Fuse use with no extra cost");shot(c,"actual-first-hit-one-wear-one-use");waitFrom=n(st,"worldTick");pending=submit(s,()->state(s));phase=12;
            }else if(phase==12&&pending.isDone()){
                var st=pending.join();if(n(st,"worldTick")-waitFrom<5){pending=submit(s,()->state(s));return;}c.options.keyShift.setDown(true);releaseAt=c.player.tickCount+2;phase=13;
            }else if(phase==13&&c.player.tickCount>=releaseAt){fuse(c);pending=submit(s,()->state(s));phase=14;
            }else if(phase==14){
                if(c.player.tickCount>=releaseAt)c.options.keyShift.setDown(false);if(!pending.isDone()||!intent(true,1,true))return;var st=pending.join();if(n(st,"Buses")!=-1||n(st,"offIron")!=1){pending=submit(s,()->state(s));return;}check(n(st,"offWear")==31&&n(st,"Bwear")==1&&n(st,"Auses")==32,"Actual split returns31-use material without resetting host wear");c.options.keyShift.setDown(false);shot(c,"native-split-retains-material-wear");waitFrom=n(st,"worldTick");pending=submit(s,()->state(s));phase=15;
            }else if(phase==15&&pending.isDone()){
                var st=pending.join();if(n(st,"worldTick")-waitFrom<5){pending=submit(s,()->state(s));return;}fuse(c);pending=submit(s,()->state(s));phase=16;
            }else if(phase==16&&pending.isDone()&&intent(false,1,true)){
                var st=pending.join();if(n(st,"Buses")!=31||n(st,"offIron")!=0){pending=submit(s,()->state(s));return;}shot(c,"native-refuse-does-not-refill-material");waitFrom=n(st,"worldTick");pending=submit(s,()->state(s));phase=17;
            }else if(phase==17&&pending.isDone()){
                var st=pending.join();if(n(st,"worldTick")-waitFrom<25||((Number)st.get("attackScale")).doubleValue()<.99||n(st,"meleePhase")!=0||!aim(c,(int)n(st,"targetId"))){pending=submit(s,()->state(s));return;}key(c.options.keyAttack);releaseAt=c.player.tickCount+1;pending=submit(s,()->state(s));phase=18;
            }else if(phase==18){
                if(c.player.tickCount>=releaseAt)c.options.keyAttack.setDown(false);if(!pending.isDone())return;var st=pending.join();if(n(st,"Buses")!=30||n(st,"Bwear")!=2){pending=submit(s,()->state(s));return;}check(n(st,"damageEvents")==2&&n(st,"Auses")==32&&n(st,"Awear")==0&&n(st,"offIron")==0&&((Number)st.get("stamina")).doubleValue()==100,"Second actual ordinary hit continues material31 to30 exactly once");shot(c,"native-second-hit-and-two-owned-hosts");release(c);sampling=false;saveCheckpoint=true;report(c,"complete-awaiting-disk-reopen","Native F/V/quick slot/actual two hits/split/refuse keep original material and host accounting; disk reopen still required");phase=19;c.stop();
            }else if(phase==50&&pending.isDone()){
                var st=pending.join();if(!Boolean.TRUE.equals(st.get("targetLoaded"))){pending=submit(s,()->state(s));return;}check(Objects.equals(prior.get("owner"),st.get("owner")),"Actual saved player identity");
                if(CODEC_AUDIT){pending=submit(s,()->{var ops=RegistryOps.create(JsonOps.INSTANCE,s.registryAccess());for(int i=0;i<2;i++){var live=player(s).getInventory().getItem(i);var json=ItemStack.CODEC.parse(ops,ItemStack.CODEC.encodeStart(ops,live).getOrThrow()).getOrThrow();var nativeCopy=decodeNative(s,encodeNative(s,live));var a=live.get(FusionContent.DATA);var b=json.get(FusionContent.DATA);var audit=new LinkedHashMap<String,Object>();audit.put("slot",i);audit.put("liveJsonSelfMatches",ItemStack.matches(live,json));audit.put("liveNativeSelfMatches",ItemStack.matches(live,nativeCopy));audit.put("liveRawMaterial",a.rawMaterial().toString());audit.put("jsonRawMaterial",b.rawMaterial().toString());audit.put("liveCountTagType",a.rawMaterial().get("count").getClass().getSimpleName());audit.put("jsonCountTagType",b.rawMaterial().get("count").getClass().getSimpleName());audit.put("liveHash",a.networkHash());audit.put("jsonHash",b.networkHash());CODEC_REPORT.add(audit);check(ItemStack.matches(live,nativeCopy),"Lossless native stack reference roundtrip");}return state(s);});phase=60;return;}
                check("native-nbt-base64-v1".equals(prior.get("checkpointFormat")),"QA reference must preserve native NBT types; old JSON snapshots stay historical");pending=submit(s,()->{var ops=RegistryOps.create(JsonOps.INSTANCE,s.registryAccess());for(int i=0;i<2;i++){var expected=decodeNative(s,(String)prior.get(i==0?"hostANbt":"hostBNbt"));check(ItemStack.matches(expected,player(s).getInventory().getItem(i)),"Exact native saved host components/data/wear");}check(target(s).getHealth()==((Number)prior.get("targetHealth")).floatValue(),"Original native target damage persists");return state(s);});phase=51;
            }else if(phase==51&&pending.isDone()){
                var st=pending.join();check(n(st,"Auses")==32&&n(st,"Buses")==30&&n(st,"Bwear")==2&&n(st,"Awear")==0&&n(st,"offIron")==0&&n(st,"selected")==1,"Actual saved two-host material use/wear");shot(c,"actual-disk-restores-both-hosts");c.options.keyShift.setDown(true);releaseAt=c.player.tickCount+2;phase=52;
            }else if(phase==52&&c.player.tickCount>=releaseAt){fuse(c);pending=submit(s,()->state(s));phase=53;
            }else if(phase==53){
                if(c.player.tickCount>=releaseAt)c.options.keyShift.setDown(false);if(!pending.isDone()||!intent(true,1,true))return;var st=pending.join();if(n(st,"Buses")!=-1||n(st,"offIron")!=1){pending=submit(s,()->state(s));return;}check(n(st,"offWear")==30&&n(st,"Bwear")==2&&n(st,"Auses")==32,"Actual post-reopen split preserves30 uses and all named/glint material");c.options.keyShift.setDown(false);key(c.options.keyInventory);phase=54;
            }else if(phase==60&&pending.isDone()){pending.join();sampling=false;report(c,"codec-diagnostic-complete","Read-only live stack self-roundtrips compare JSON against native typed NBT; not a gameplay or disk qualification");phase=61;c.stop();
            }else if(phase==54&&c.gui.screen() instanceof InventoryScreen){c.options.keyInventory.setDown(false);shot(c,"native-reopened-split-material-and-inventory");c.gui.screen().onClose();release(c);sampling=false;report(c,"complete","Actual two-host disk restore and native split preserve material30/name/glint and original host wear");phase=55;c.stop();}
        }catch(RuntimeException|AssertionError e){sampling=false;release(c);shot(c,"failure");report(c,"failed",e.toString());throw e;}
    }
}
