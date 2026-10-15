package dev.wildcraft.client.animation.rig;
import com.google.gson.Gson;
import dev.wildcraft.focus.FocusTime;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.player.Player;
import org.joml.*;
/** Adopted ten-bone settling over the current bow pose. Native time, charge and upper body are never owned here. */
public final class FocusLegRig {
    public record Pulse(float weight,boolean active,float age){}
    public static final RenderStateDataKey<Pulse> PULSE=RenderStateDataKey.create(()->"wildcraft:focus_leg_layer");
    private record Config(int version,float enterSeconds,float exitSeconds,float thighBackDegrees,float shinExtraBackDegrees,String[] mask){}
    private static final Map<Player,Motion> MOTION=new WeakHashMap<>();
    private static final class Motion {long stamp;float weight,from,age;boolean active;}
    private static Config config;private static boolean loaded;
    private FocusLegRig(){}
    static void clear(){MOTION.clear();}static void cancel(Player p){MOTION.remove(p);}
    private static Config config(){if(!loaded){loaded=true;try(var in=FocusLegRig.class.getResourceAsStream("/assets/wildcraft/animation/focus-leg-layer.json")){
        if(in==null)return null;config=new Gson().fromJson(new InputStreamReader(in,StandardCharsets.UTF_8),Config.class);
        var mask=new HashSet<String>();for(String side:new String[]{"left","right"})for(String bone:new String[]{"thigh","shin","foot","knee_deform","ankle_deform"})mask.add(side+"_"+bone);
        if(config.version!=1||!mask.equals(Set.of(config.mask))||config.enterSeconds!=.16F||config.exitSeconds!=.2F||config.thighBackDegrees!=3||config.shinExtraBackDegrees!=2)throw new IllegalStateException("Unsupported adopted focus layer contract");
    }catch(IOException e){throw new UncheckedIOException(e);}}return config;}
    static void apply(Player p,AvatarRenderState state,CombatRig.Data data,Matrix4f[] global){
        var cfg=config();if(cfg==null)return;var client=Minecraft.getInstance();boolean active=FocusTime.active(p);var m=MOTION.get(p);
        if(m==null){if(!active)return;m=new Motion();MOTION.put(p,m);}
        boolean paused=client.isPaused()||client.gui.screen()!=null&&client.gui.screen().isPauseScreen();long now=System.nanoTime();float dt=m.stamp==0||paused?0:java.lang.Math.max(0,(now-m.stamp)/1_000_000_000F);m.stamp=now;
        if(!paused&&active!=m.active){m.active=active;m.from=m.weight;m.age=0;}
        if(!paused){m.age+=dt;float t=java.lang.Math.clamp(m.age/(m.active?cfg.enterSeconds:cfg.exitSeconds),0,1);t=t*t*(3-2*t);m.weight=m.active?m.from+(1-m.from)*t:m.from*(1-t);}
        if(!m.active&&m.weight<=0){cancel(p);return;}
        state.setData(PULSE,new Pulse(m.weight,m.active,m.age));if(m.weight<=0)return;
        apply(data,global,m.weight,cfg.thighBackDegrees,cfg.shinExtraBackDegrees);
    }
    /** Snapshot current leg positions first; every application starts from an unmodified current animation frame. */
    private static void apply(CombatRig.Data data,Matrix4f[] g,float weight,float thighDegrees,float shinDegrees){
        var names=Arrays.asList(data.bones());var axis=g[names.indexOf("pelvis")].transformDirection(new Vector3f(1,0,0)).normalize();
        var tilt=new Quaternionf().fromAxisAngleRad(axis,(float)java.lang.Math.toRadians(thighDegrees*weight));var calf=new Quaternionf().fromAxisAngleRad(axis,(float)java.lang.Math.toRadians(shinDegrees*weight));var lower=new Quaternionf(calf).mul(tilt);
        for(String side:new String[]{"left","right"}){
            int thigh=names.indexOf(side+"_thigh"),shin=names.indexOf(side+"_shin"),foot=names.indexOf(side+"_foot"),knee=names.indexOf(side+"_knee_deform"),ankle=names.indexOf(side+"_ankle_deform");
            var hip=g[thigh].getTranslation(new Vector3f());var oldKnee=g[shin].getTranslation(new Vector3f());var oldAnkle=g[foot].getTranslation(new Vector3f());
            var nextKnee=new Vector3f(oldKnee).sub(hip).rotate(tilt).add(hip);var nextAnkle=new Vector3f(oldAnkle).sub(oldKnee).rotate(lower).add(nextKnee);
            var q1=new Quaternionf(tilt).mul(g[thigh].getUnnormalizedRotation(new Quaternionf()).normalize());var q2=new Quaternionf(lower).mul(g[shin].getUnnormalizedRotation(new Quaternionf()).normalize());var q3=new Quaternionf(lower).mul(g[foot].getUnnormalizedRotation(new Quaternionf()).normalize());
            pose(g[thigh],hip,q1);pose(g[shin],nextKnee,q2);pose(g[foot],nextAnkle,q3);pose(g[knee],nextKnee,new Quaternionf(q1).slerp(q2,.5F));pose(g[ankle],nextAnkle,new Quaternionf(q2).slerp(q3,.5F));
        }
    }
    private static void pose(Matrix4f matrix,Vector3f position,Quaternionf rotation){var scale=matrix.getScale(new Vector3f());matrix.translationRotateScale(position,rotation.normalize(),scale);}
}
