package dev.wildcraft.client.render;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelType;
import dev.wildcraft.network.ClimbVisual;
import dev.wildcraft.traversal.*;
import org.joml.Quaternionf;

/** Per-player local pose samples only. Native setupAnim precedes every bounded visual join. */
public final class CharacterPoses {
    public static final RenderStateDataKey<Pose> POSE=RenderStateDataKey.create(()->"wildcraft:character_pose");
    public record Pose(String clip,float frame,float facing,float join,float exit,float[][] from,Motion motion){public Pose(String clip,float frame,float facing){this(clip,frame,facing,1,0,null,new Motion());}}
    private record Clips(Map<String,float[][][]> standard,Map<String,float[][][]> slim){}
    private static final Clips DATA=load();
    private static final Map<Player,Motion> MOTION=new WeakHashMap<>();
    public static final class Motion {double x,y,z;float distance;long started;Object dimension;String mode;float[][] latest,from;boolean slim;}
    private CharacterPoses(){}
    private static Clips load(){
        var input=CharacterPoses.class.getResourceAsStream("/assets/wildcraft/animations/character-poses.json");if(input==null)throw new IllegalStateException("Missing approved character poses");
        try(var reader=new InputStreamReader(input,StandardCharsets.UTF_8)){return new Gson().fromJson(reader,Clips.class);}catch(java.io.IOException e){throw new IllegalStateException(e);}
    }
    public static void initialize(){ClientPlayConnectionEvents.DISCONNECT.register((h,c)->MOTION.clear());}
    public static void extract(Player p,AvatarRenderState state,float partial){
        state.setData(POSE,null);
        boolean invalid=!p.isAlive()||p.isSpectator()||p.isInvisible()||state.currentSwing!=null||p.isUsingItem();
        if(invalid){MOTION.remove(p);return;}
        boolean slim=state.skin.model()==PlayerModelType.SLIM;
        var m=MOTION.computeIfAbsent(p,ignored->{var a=new Motion();a.x=p.getX();a.y=p.getY();a.z=p.getZ();a.dimension=p.level().dimension();a.slim=slim;return a;});
        if(m.dimension!=p.level().dimension()||m.slim!=slim){MOTION.remove(p);return;}
        if(Minecraft.getInstance().isPaused()){m.started=System.nanoTime();m.from=m.latest;}
        double moved=Math.sqrt(Math.pow(p.getX()-m.x,2)+Math.pow(p.getY()-m.y,2)+Math.pow(p.getZ()-m.z,2));m.x=p.getX();m.y=p.getY();m.z=p.getZ();
        if(moved>4){MOTION.remove(p);return;}if(moved<1)m.distance+=(float)moved;
        var climb=p.getAttached(Climbing.VISUAL);boolean climbing=climb!=null&&climb.active();
        String next=Gliding.active(p)?"glide":climbing?switch(climb.motion()){case 1->"up";case 2->"down";case 3->"left";case 4->"right";case 5->"mantle";default->"hold";}:"empty";
        if(next.equals("empty")&&(!p.getMainHandItem().isEmpty()||!p.getOffhandItem().isEmpty())){MOTION.remove(p);return;}
        long now=System.nanoTime();
        if(!next.equals(m.mode)){m.from=copy(m.latest);m.started=now;m.distance=0;m.mode=next;}
        float elapsed=Math.max(0,(now-m.started)/1_000_000_000F),duration=next.equals("glide")?.3F:next.equals("empty")?.2F:.12F;
        if(next.equals("empty")&&(m.from==null||elapsed>=duration)){m.latest=null;return;}
        float facing=state.bodyRot;
        if(climbing&&!next.equals("glide")){Direction face=Direction.values()[Math.clamp(climb.face(),2,5)];facing=face.toYRot();state.yRot=Math.clamp(net.minecraft.util.Mth.wrapDegrees(state.yRot+state.bodyRot-facing),-70,70);state.bodyRot=facing;}
        float frame=next.equals("glide")?Math.min(9,elapsed*30):next.equals("empty")?elapsed/.2F*18:next.equals("hold")?0:m.distance*18;
        float t=Math.clamp(elapsed/duration,0,1),join=t*t*(3-2*t);
        state.setData(POSE,new Pose(next.equals("empty")?"release":next,frame,facing,join,next.equals("empty")?t:0,copy(m.from),m));
    }
    public static void apply(PlayerModel model,AvatarRenderState state){
        Pose pose=state.getData(POSE);if(pose==null||state.isInvisible||state.isSpectator||state.currentSwing!=null||state.isUsingItem)return;
        var frames=(state.skin.model()==PlayerModelType.SLIM?DATA.slim:DATA.standard).get(pose.clip);if(frames==null)return;
        float f=pose.clip.equals("glide")||pose.clip.equals("release")||pose.clip.equals("mantle")?Math.min(frames.length-1,pose.frame):pose.frame%(frames.length-1);
        int a=(int)f,b=Math.min(a+1,frames.length-1);float t=f-a;
        ModelPart[] limbs={model.rightArm,model.leftArm,model.rightLeg,model.leftLeg};float[][] presented=new float[4][6];
        for(int i=0;i<4;i++){
            var part=limbs[i];float[] nativePose={part.x,part.y,part.z,part.xRot,part.yRot,part.zRot};
            float[] target=blend(frames[a][i],frames[b][i],t);
            // Only local limbs are copied, never shared ModelPart objects or a world/body matrix.
            float[] from=pose.from==null?nativePose:pose.from[i];
            if(pose.clip.equals("release")){
                float first=Math.clamp(pose.exit*2,0,1);target=blend(from,target,first*first*(3-2*first));
                float last=Math.clamp((pose.exit-.5F)*2,0,1);target=blend(target,nativePose,last*last*(3-2*last));
            }else target=blend(from,target,pose.join);
            presented[i]=target;part.x=target[0];part.y=target[1];part.z=target[2];part.xRot=target[3];part.yRot=target[4];part.zRot=target[5];
        }
        pose.motion.latest=copy(presented);
    }
    public static float[] blend(float[] a,float[] b,float t){
        float[] result=new float[6];for(int i=0;i<3;i++)result[i]=a[i]+(b[i]-a[i])*t;
        var q=new Quaternionf().rotationZYX(a[5],a[4],a[3]).slerp(new Quaternionf().rotationZYX(b[5],b[4],b[3]),t);
        var angles=q.getEulerAnglesZYX(new org.joml.Vector3f());result[3]=angles.x;result[4]=angles.y;result[5]=angles.z;return result;
    }
    private static float[][] copy(float[][] value){if(value==null)return null;return Arrays.stream(value).map(float[]::clone).toArray(float[][]::new);}
}
