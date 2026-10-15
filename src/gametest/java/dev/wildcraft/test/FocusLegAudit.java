package dev.wildcraft.test;
import dev.wildcraft.client.animation.rig.*;
import java.util.*;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.joml.*;
/** Independent scope/segment/upper-body and GPU observations on the actual current bow frame. */
public final class FocusLegAudit {
    public static boolean enabled;public static volatile Runnable partialAction;public static volatile boolean partialExecuted;public static float partialActionWeight;public static Matrix4f[] before;public static int frames,gpu,partial,full;public static float maxDelta,minimumExit=1;public static boolean sawExit;
    public static void begin(){enabled=true;before=null;frames=gpu=partial=full=0;maxDelta=0;minimumExit=1;sawExit=false;}
    public static void capture(Matrix4f[] g){before=Arrays.stream(g).map(Matrix4f::new).toArray(Matrix4f[]::new);}
    public static boolean leg(String name){return name.contains("thigh")||name.contains("shin")||name.contains("foot")||name.contains("knee")||name.contains("ankle");}
    public static void check(CombatRig.Data data,Matrix4f[] g,AvatarRenderState state){var pulse=state.getData(FocusLegRig.PULSE);if(pulse==null||before==null)return;var names=Arrays.asList(data.bones());float delta=0;
        for(int i=0;i<g.length;i++){float[] a=before[i].get(new float[16]),b=g[i].get(new float[16]);for(int k=0;k<16;k++){float d=java.lang.Math.abs(a[k]-b[k]);if(!leg(names.get(i))&&d>1e-6)throw new AssertionError("Focus leg layer changed protected current "+names.get(i));delta=java.lang.Math.max(delta,d);}}
        for(String side:new String[]{"left","right"})for(String[] pair:new String[][]{{"thigh","shin"},{"shin","foot"}}){int a=names.indexOf(side+"_"+pair[0]),b=names.indexOf(side+"_"+pair[1]);float old=before[a].getTranslation(new Vector3f()).distance(before[b].getTranslation(new Vector3f())),next=g[a].getTranslation(new Vector3f()).distance(g[b].getTranslation(new Vector3f()));if(java.lang.Math.abs(old-next)>.00002)throw new AssertionError("Focus layer changed actual source leg length");}
        var axis=before[names.indexOf("pelvis")].transformDirection(new Vector3f(1,0,0)).normalize();
        for(String side:new String[]{"left","right"})for(String bone:new String[]{"thigh","shin","foot"}){int i=names.indexOf(side+"_"+bone);var old=before[i].getUnnormalizedRotation(new Quaternionf()).normalize();var next=g[i].getUnnormalizedRotation(new Quaternionf()).normalize();var difference=new Quaternionf(next).mul(new Quaternionf(old).invert()).normalize();float degrees=bone.equals("thigh")?3:5;var expected=new Quaternionf().fromAxisAngleRad(axis,(float)java.lang.Math.toRadians(degrees*pulse.weight()));if(java.lang.Math.abs(difference.dot(expected))<.999999)throw new AssertionError("Focus leg angle/axis differs from adopted layer "+bone);}
        if(partialAction!=null&&pulse.active()&&pulse.weight()>.25&&pulse.weight()<.75){var action=partialAction;partialAction=null;partialActionWeight=pulse.weight();partialExecuted=true;action.run();}
        maxDelta=java.lang.Math.max(maxDelta,delta);frames++;if(pulse.weight()>0&&pulse.weight()<.999)partial++;if(pulse.weight()>.999)full++;if(!pulse.active()){sawExit=true;minimumExit=java.lang.Math.min(minimumExit,pulse.weight());}
    }
    public static void gpu(AvatarRenderState s){if(enabled&&s.getData(FocusLegRig.PULSE)!=null&&s.getData(CombatRigLayer.FRAME)!=null)gpu++;}
}
