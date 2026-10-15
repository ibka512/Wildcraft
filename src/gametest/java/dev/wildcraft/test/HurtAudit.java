package dev.wildcraft.test;
import dev.wildcraft.client.animation.rig.*;
import dev.wildcraft.combat.MeleeActions;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
/** Observes actual before/after native layer and real GPU; damage is generated only by native server calls. */
public final class HurtAudit {
    public static boolean enabled;
    public static int applied,gpu,nonzero;
    public static float maxDelta;
    public static String clip;
    public static Matrix4f[] before;
    public static void begin(){enabled=true;applied=gpu=nonzero=0;maxDelta=0;clip=null;before=null;}
    public static void before(CombatRig.Data data,Matrix4f[] global){if(enabled)before=Arrays.stream(global).map(Matrix4f::new).toArray(Matrix4f[]::new);}
    public static void after(CombatRig.Data data,Matrix4f[] global,Matrix4f world){
        if(!enabled||before==null)return;applied++;var names=Arrays.asList(data.bones());
        for(int i=0;i<global.length;i++){
            String n=names.get(i);boolean upper=n.equals("body")||n.equals("head")||n.contains("arm")||n.contains("elbow")||n.contains("wrist")||n.contains("palm")||n.contains("reference")||n.contains("mount");
            if(!upper&&!global[i].equals(before[i],1e-6F))throw new AssertionError("Hurt source changed protected native root/pelvis/leg "+n);
            if(n.equals("head")){var a=before[i].getUnnormalizedRotation(new Quaternionf()).normalize();var b=global[i].getUnnormalizedRotation(new Quaternionf()).normalize();if(Math.abs(a.dot(b))<.999999)throw new AssertionError("Hurt changed current native head aim");}
        }
        for(String side:new String[]{"left","right"})for(String[] pair:new String[][]{{"upper_arm","elbow_deform"},{"elbow_deform","palm_grip"}}){int a=names.indexOf(side+"_"+pair[0]),b=names.indexOf(side+"_"+pair[1]);float old=before[a].getTranslation(new Vector3f()).distance(before[b].getTranslation(new Vector3f())),now=global[a].getTranslation(new Vector3f()).distance(global[b].getTranslation(new Vector3f()));if(Math.abs(old-now)>2e-5F)throw new AssertionError("Hurt stretched native arm segments");}
        float diff=0;float[] a=world.get(new float[16]),identity=new Matrix4f().get(new float[16]);for(int i=0;i<16;i++)diff=Math.max(diff,Math.abs(a[i]-identity[i]));maxDelta=Math.max(maxDelta,diff);if(diff>.001F)nonzero++;
    }
    public static void gpu(AvatarRenderState state){var f=state.getData(CombatRigLayer.FRAME);if(!enabled||f==null||!f.clip().startsWith("hurt_"))return;gpu++;clip=f.clip();
        var c=Minecraft.getInstance();int head=Arrays.asList(f.data().bones()).indexOf("head");var shift=f.global()[head].getTranslation(new Vector3f()).sub(before[head].getTranslation(new Vector3f()));
        float angle=(float)Math.toRadians(MeleeActions.view(c.player).hurtYaw()-state.bodyRot);var incoming=new Vector3f(-(float)Math.sin(angle),0,(float)Math.cos(angle));
        if(shift.lengthSquared()>1e-6F&&shift.dot(incoming)>1e-5F)throw new AssertionError("Actual recoil leans toward incoming native damage: "+shift+" "+incoming+" "+f.clip());
    }
}
