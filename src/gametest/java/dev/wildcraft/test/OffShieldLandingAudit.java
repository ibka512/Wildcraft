package dev.wildcraft.test;

import dev.wildcraft.client.animation.rig.CombatRigLayer;
import dev.wildcraft.client.render.BackTransitions;
import dev.wildcraft.equipment.BackEquipment;
import dev.wildcraft.traversal.Gliding;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import java.util.*;

/** Passive test-only evidence; never creates an ownership or presentation event. */
public final class OffShieldLandingAudit {
    public static boolean enabled;
    public static int groundDrawFrames,stationaryGroundDrawFrames,glidingBackFrames,nativeUseBackFrames,eligibleFrames;
    public static float minEligibleProgress=1,maxEligibleProgress;
    private static String scenario="landing";
    public static long reference;
    private static final List<Map<String,Object>> rows=new ArrayList<>();
    private static String previous="";
    public static int rowCount(){return rows.size();}
    public static void begin(){begin("landing");}
    public static void begin(String name){scenario=name;enabled=true;groundDrawFrames=stationaryGroundDrawFrames=glidingBackFrames=nativeUseBackFrames=eligibleFrames=0;minEligibleProgress=1;maxEligibleProgress=0;reference=0;rows.clear();previous="";}
    public static void observe(Avatar entity,AvatarRenderState state){
        if(!enabled||!(entity instanceof Player p))return;
        var visual=p.getAttached(BackEquipment.VISUAL);if(visual==null)return;var entry=visual.shield();
        if(reference==0&&entry.reference()!=0)reference=entry.reference();
        if(reference!=0&&entry.reference()!=reference)throw new AssertionError("Passive landing probe lost actual shield reference");
        var settles=state.getData(BackTransitions.SETTLES);var event=settles==null?null:settles[BackEquipment.SHIELD];
        if(Gliding.active(p)&&entry.owner()==3)glidingBackFrames++;
        if(p.isUsingItem()&&entry.owner()==3)nativeUseBackFrames++;
        int otherSettles=0;if(settles!=null)for(int i=0;i<settles.length;i++)if(i!=BackEquipment.SHIELD&&settles[i]!=null)otherSettles++;
        if(event!=null&&event.draw()&&event.owner()==2&&event.handOwner()==2&&p.onGround()){
            groundDrawFrames++;if(p.getDeltaMovement().horizontalDistanceSqr()<.0004){stationaryGroundDrawFrames++;if(!p.isUsingItem()&&p.getMainHandItem().isEmpty()&&otherSettles==0){eligibleFrames++;minEligibleProgress=Math.min(minEligibleProgress,event.progress());maxEligibleProgress=Math.max(maxEligibleProgress,event.progress());}}
        }
        var frame=state.getData(CombatRigLayer.FRAME);
        String key=entry.owner()+"/"+p.onGround()+"/"+Gliding.active(p)+"/"+(event==null?"none":event.owner()+":"+event.handOwner())+"/"+(frame==null?"native":frame.clip())+"/"+(p.getDeltaMovement().horizontalDistanceSqr()<.0004);
        if(!key.equals(previous)){
            var row=new LinkedHashMap<String,Object>();row.put("gameTick",p.level().getGameTime());row.put("owner",entry.owner());row.put("reference",entry.reference());row.put("ground",p.onGround());row.put("gliding",Gliding.active(p));row.put("intent",Gliding.intent(p).toString());row.put("glideView",String.valueOf(p.getAttached(Gliding.VIEW)));row.put("horizontalSqr",p.getDeltaMovement().horizontalDistanceSqr());row.put("pose",p.getPose().toString());row.put("using",p.isUsingItem());row.put("mainEmpty",p.getMainHandItem().isEmpty());row.put("mainItem",p.getMainHandItem().getItem().toString());row.put("otherSettles",otherSettles);row.put("source",frame==null?null:frame.clip());row.put("settle",event==null?null:Map.of("owner",event.owner(),"handOwner",event.handOwner(),"progress",event.progress()));rows.add(row);previous=key;
        }
    }
    public static void save(String name){try{var report=new LinkedHashMap<String,Object>();report.put("probeOnly",true);report.put("scenario",scenario);report.put("nativeUseBackFrames",nativeUseBackFrames);report.put("eligibleFrames",eligibleFrames);report.put("minEligibleProgress",minEligibleProgress);report.put("maxEligibleProgress",maxEligibleProgress);report.put("sourceGameplayClaimed",false);report.put("groundDrawFrames",groundDrawFrames);report.put("stationaryGroundDrawFrames",stationaryGroundDrawFrames);report.put("glidingBackFrames",glidingBackFrames);report.put("rows",rows);java.nio.file.Files.writeString(java.nio.file.Path.of("offshield-landing-"+name+".json"),new com.google.gson.GsonBuilder().setPrettyPrinting().serializeNulls().create().toJson(report)+"\n");}catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}}
}
