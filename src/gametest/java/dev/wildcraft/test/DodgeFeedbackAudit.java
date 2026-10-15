package dev.wildcraft.test;
import dev.wildcraft.client.animation.rig.*;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import java.util.Arrays;
public final class DodgeFeedbackAudit {
    private static final java.util.Map<net.minecraft.world.entity.player.PlayerModelType,net.minecraft.client.model.player.PlayerModel> FIXTURE_MODELS=new java.util.EnumMap<>(net.minecraft.world.entity.player.PlayerModelType.class);
    public static boolean enabled,checkingNativeItem;public static int frames,nativeFrames,nativeItemChecks,nativeGpuFrames;public static float torsoDelta;public static int serial;public static long firstNanos,finishedNanos,lastNanos;public static float firstTime,lastTime;private static Matrix4f[] before;
    public static void begin(){enabled=true;checkingNativeItem=false;frames=nativeFrames=nativeItemChecks=nativeGpuFrames=0;torsoDelta=0;serial=0;firstNanos=finishedNanos=lastNanos=0;firstTime=lastTime=0;before=null;}
    public static void before(AvatarRenderState s){var f=s.getData(CombatRigLayer.FRAME);before=enabled&&f!=null?Arrays.stream(f.global()).map(Matrix4f::new).toArray(Matrix4f[]::new):null;
        if(!enabled||f==null||!f.clip().equals("dodge_native_base"))return;
        var c=net.minecraft.client.Minecraft.getInstance();var renderer=(net.minecraft.client.renderer.entity.player.AvatarRenderer<?>)c.getEntityRenderDispatcher().getRenderer(c.player);var model=renderer.getModel();
        // RigFixture changes the extracted skin after renderer selection; match that controlled native model too.
        if(s.skin.model()!=c.player.getSkin().model())model=FIXTURE_MODELS.computeIfAbsent(s.skin.model(),type->{boolean slim=type==net.minecraft.world.entity.player.PlayerModelType.SLIM;return new net.minecraft.client.model.player.PlayerModel(net.minecraft.client.model.geom.builders.LayerDefinition.create(net.minecraft.client.model.player.PlayerModel.createMesh(net.minecraft.client.model.geom.builders.CubeDeformation.NONE,slim),64,64).bakeRoot(),slim);});
        model.setupAnim(s);
        for(var arm:net.minecraft.world.entity.HumanoidArm.values()){
            var stack=arm==net.minecraft.world.entity.HumanoidArm.RIGHT?s.rightHandItemStack:s.leftHandItemStack;var item=arm==net.minecraft.world.entity.HumanoidArm.RIGHT?s.rightHandItemState:s.leftHandItemState;if(item.isEmpty())continue;
            var pose=new com.mojang.blaze3d.vertex.PoseStack();model.translateToHand(s,arm,pose);pose.rotateDegrees(com.mojang.math.Axis.XP,-90);pose.rotateDegrees(com.mojang.math.Axis.YP,180);pose.translate(arm==net.minecraft.world.entity.HumanoidArm.LEFT?-.0625F:.0625F,.125F,-.625F);
            Matrix4f actual;checkingNativeItem=true;try{actual=CombatRig.heldPose(f,item,arm,stack.is(net.minecraft.world.item.Items.SHIELD));}finally{checkingNativeItem=false;}float[] a=pose.last().pose().get(new float[16]),b=actual.get(new float[16]);for(int i=0;i<16;i++)if(Math.abs(a[i]-b[i])>.00002)throw new AssertionError("Native feedback continuation changed vanilla held-item pose "+arm+" component "+i+": "+a[i]+" -> "+b[i]);nativeItemChecks++;
        }
    }
    public static void after(AvatarRenderState s){var pulse=s.getData(DodgeFeedbackRig.PULSE);var f=s.getData(CombatRigLayer.FRAME);if(pulse==null){if(enabled&&firstNanos!=0&&finishedNanos==0){finishedNanos=System.nanoTime();var c=net.minecraft.client.Minecraft.getInstance();System.out.println("DODGE SOURCE END last="+lastTime+" elapsed="+(finishedNanos-firstNanos)/1e9+" remaining="+(.24*(1-firstTime))+" native="+nativeFrames+" current="+(f==null?null:f.clip())+" pose="+c.player.getPose()+" melee="+dev.wildcraft.combat.MeleeActions.view(c.player)+" reaction="+dev.wildcraft.combat.ReactionTime.view(c.player));}return;}if(f==null||before==null)return;if(firstNanos==0&&pulse.time()>0){firstNanos=System.nanoTime();firstTime=pulse.time();}
        for(int i=0;i<before.length;i++){String n=f.data().bones()[i];var b=f.global()[i];if(!b.isFinite())throw new AssertionError("Actual feedback nonfinite");float[] x=before[i].get(new float[16]),y=b.get(new float[16]);float d=0;for(int k=0;k<16;k++)d=Math.max(d,Math.abs(x[k]-y[k]));if(n.equals("root")||n.equals("pelvis")||n.contains("thigh")||n.contains("knee")||n.contains("shin")||n.contains("ankle")||n.contains("foot")){if(d>1e-6)throw new AssertionError("Feedback changed real lower/root baseline "+n);}if(n.equals("body"))torsoDelta=Math.max(torsoDelta,d);if(n.equals("head")){var a=before[i].getUnnormalizedRotation(new Quaternionf()).normalize();var q=b.getUnnormalizedRotation(new Quaternionf()).normalize();if(Math.abs(a.dot(q))<.99999)throw new AssertionError("Feedback pulled actual head aim");}}
        lastTime=Math.max(lastTime,pulse.time());lastNanos=System.nanoTime();frames++;if(f.clip().equals("dodge_native_base"))nativeFrames++;serial=pulse.serial();before=null;
    }
}
