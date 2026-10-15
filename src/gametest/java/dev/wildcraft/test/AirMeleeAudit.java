package dev.wildcraft.test;
import dev.wildcraft.client.animation.rig.*;
import java.util.Arrays;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.joml.Matrix4f;
/** Test-only observation of real skin submission, not a source preview or fake player phase. */
public final class AirMeleeAudit {
    private static final java.util.Map<net.minecraft.world.entity.player.PlayerModelType,net.minecraft.client.model.player.PlayerModel> MODELS=new java.util.EnumMap<>(net.minecraft.world.entity.player.PlayerModelType.class);
    public static boolean enabled;
    public static int frames,full;
    public static String clip;
    public static float maxTime;
    public static void begin(){frames=full=0;clip=null;maxTime=0;enabled=true;}
    public static void observe(AvatarRenderState state){
        var f=state.getData(CombatRigLayer.FRAME);if(!enabled||f==null||!f.clip().contains("_air_attack_"))return;
        frames++;clip=f.clip();maxTime=Math.max(maxTime,f.time());
        if(f.weight()<.999999F)return;full++;
        var source=CombatRig.sample(f.data().variant(),f.clip(),f.time());var names=Arrays.asList(f.data().bones());
        for(int i=0;i<source.length;i++){
            if(names.get(i).equals("head"))continue;
            float[] a=new float[16],b=new float[16];source[i].get(a);f.global()[i].get(b);
            for(int j=0;j<16;j++)if(Math.abs(a[j]-b[j])>2e-5F)throw new AssertionError("Actual air source mismatch "+names.get(i));
        }
        var model=MODELS.computeIfAbsent(state.skin.model(),type->{boolean slim=type==net.minecraft.world.entity.player.PlayerModelType.SLIM;return new net.minecraft.client.model.player.PlayerModel(net.minecraft.client.model.geom.builders.LayerDefinition.create(net.minecraft.client.model.player.PlayerModel.createMesh(net.minecraft.client.model.geom.builders.CubeDeformation.NONE,slim),64,64).bakeRoot(),slim);});model.setupAnim(state);
        var nativePose=new com.mojang.blaze3d.vertex.PoseStack();model.head.translateAndRotate(nativePose);
        var aim=new Matrix4f(CombatRigLayer.BASIS).mul(nativePose.last().pose()).mul(CombatRigLayer.BASIS).getUnnormalizedRotation(new org.joml.Quaternionf()).normalize();
        if(Math.abs(aim.dot(f.global()[names.indexOf("head")].getUnnormalizedRotation(new org.joml.Quaternionf()).normalize()))<.99999)throw new AssertionError("Air attack changed native head aim");
        int root=names.indexOf("root");if(!f.global()[root].equals(CombatRig.matrix(f.data().bindWorld()[root]),2e-5F))throw new AssertionError("Air art must not add root lift");
    }
}
