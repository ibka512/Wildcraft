package dev.wildcraft.test;
import dev.wildcraft.client.animation.rig.*;
import java.util.*;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Matrix4f;
/** Test-owned observation around the actual renderer's arm overlay, not a shadow game rule. */
public final class GuardRigAudit {
    public static boolean enabled;public static int frames;public static float activeDeviation;private static Matrix4f[] before;
    public static void begin(){enabled=true;frames=0;activeDeviation=0;before=null;}
    public static void before(AvatarRenderState s,Matrix4f[] global){before=enabled&&s.getData(GuardRig.PULSE)!=null?Arrays.stream(global).map(Matrix4f::new).toArray(Matrix4f[]::new):null;}
    public static void after(Player p,AvatarRenderState s,CombatRig.Data d,Matrix4f[] global){
        if(before==null)return;String active=p.getMainArm()==HumanoidArm.RIGHT?"left_":"right_";
        for(int i=0;i<global.length;i++){if(!global[i].isFinite())throw new AssertionError("Actual guard matrix nonfinite");String n=d.bones()[i];boolean arm=n.startsWith(active)&&!(n.contains("thigh")||n.contains("knee")||n.contains("shin")||n.contains("ankle")||n.contains("foot"));float[] a=before[i].get(new float[16]),b=global[i].get(new float[16]);float delta=0;for(int k=0;k<16;k++)delta=Math.max(delta,Math.abs(a[k]-b[k]));if(!arm&&delta>1e-6)throw new AssertionError("Guard source overwrote actual non-active transform "+n);if(arm)activeDeviation=Math.max(activeDeviation,delta);}
        frames++;before=null;
    }
}
