package dev.wildcraft.test;

import dev.wildcraft.client.animation.rig.*;
import dev.wildcraft.client.render.BackTransitions;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.*;
import java.util.Arrays;

/** Observes real native models; does not manufacture equipment or render-state ownership. */
public final class SwordWaistAudit {
    public static boolean enabled;
    private static String family="equipment_";
    private static boolean capeOnly;
    public static int capeModels,capePoses,nativeGrips,quads;
    public static boolean inItem;
    public static ItemStackRenderState expectedItem;
    private static Vector3f expectedGrip,expectedAxis;
    public static float maxGripError,maxCapeError,maxWristError;
    public static void begin(){begin("equipment_");}
    public static void begin(String sourceFamily){capeOnly=false;family=sourceFamily;enabled=true;capeModels=capePoses=nativeGrips=quads=0;expectedItem=null;maxGripError=maxCapeError=maxWristError=0;}
    /** Native pole audits own their item raw coordinates; share only the real cape checks. */
    public static void beginCape(String sourceFamily){begin(sourceFamily);capeOnly=true;}
    private static boolean active(AvatarRenderState state){var f=state.getData(CombatRigLayer.FRAME);return enabled&&f!=null&&f.clip().startsWith(family)&&f.clip().contains("_waist_");}
    public static void capeSubmitted(Object model,Object state,net.minecraft.client.renderer.rendertype.RenderType type){
        if(!(model instanceof net.minecraft.client.model.player.PlayerCapeModel)||!(state instanceof AvatarRenderState a)||!active(a))return;
        var binding=((dev.wildcraft.test.research.mixin.RenderSetupTexturesAccessor)(Object)((dev.wildcraft.test.research.mixin.RenderTypeStateAccessor)(Object)type).wildcraft$setup()).wildcraft$textures().get("Sampler0");
        var texture=binding==null?null:((dev.wildcraft.test.research.mixin.TextureBindingLocationAccessor)binding).wildcraft$location();
        if(!a.showCape||a.skin.cape()==null||!a.skin.cape().texturePath().equals(texture)||!a.getDataOrDefault(BackTransitions.WAIST,false))throw new AssertionError("Waist source did not submit the real visible cape/texture/layout");
        capeModels++;
    }
    public static void capePose(net.minecraft.client.model.player.PlayerCapeModel model,AvatarRenderState state){
        if(!active(state))return;
        var p=new com.mojang.blaze3d.vertex.PoseStack();model.body.translateAndRotate(p);
        var expected=CombatRigLayer.body(state.getData(CombatRigLayer.FRAME));float[] a=expected.get(new float[16]),b=p.last().pose().get(new float[16]);
        for(int i=0;i<16;i++){maxCapeError=java.lang.Math.max(maxCapeError,java.lang.Math.abs(a[i]-b[i]));if(java.lang.Math.abs(a[i]-b[i])>2e-5F)throw new AssertionError("Native cape detached from adopted torso");}
        if(!Float.isFinite(model.body.getChild("cape").xRot))throw new AssertionError("Native cape flap lost finite pose");capePoses++;
    }
    public static void grip(CombatRigLayer.Frame f,ItemStackRenderState item,HumanoidArm arm,boolean shield,Matrix4f actual){
        if(!enabled||capeOnly||!f.clip().startsWith(family)||!f.clip().contains("_waist_")||DodgeFeedbackAudit.checkingNativeItem)return;
        String side=arm==HumanoidArm.RIGHT?"right":"left";var names=Arrays.asList(f.data().bones());
        var layer=(dev.wildcraft.mixin.client.FusionLayerAccessor)((dev.wildcraft.mixin.client.FusionStateAccessor)item).wildcraft$layers()[0];
        var local=new com.mojang.blaze3d.vertex.PoseStack();layer.wildcraft$transform().apply(arm==HumanoidArm.LEFT,local.last());local.mulPose(layer.wildcraft$local());
        var raw=shield?new Vector3f(0,0,.09375F):new Vector3f(.23F,.23F,.5F);
        var anchor=actual.transformPosition(local.last().pose().transformPosition(raw));
        var target=new Matrix4f(CombatRigLayer.BASIS).mul(f.global()[names.indexOf(side+(shield?"_shield_mount":"_sword_reference"))]);
        float error=anchor.distance(target.getTranslation(new Vector3f()));maxGripError=java.lang.Math.max(maxGripError,error);
        if(error>2e-5F)throw new AssertionError("Real native waist sword/shield lost authored reference "+error);
        if(!shield){var hilt=new Matrix4f(f.global()[names.indexOf(side+"_forearm")]).invert().transformPosition(f.global()[names.indexOf(side+"_sword_reference")].getTranslation(new Vector3f()));float width=f.data().variant().equals("slim")?.09375F:.125F;if(hilt.y<-.385F||hilt.y>-.24F||java.lang.Math.abs(hilt.x)>width+.01F||java.lang.Math.abs(hilt.z)>.135F)throw new AssertionError("Actual waist sword hilt left physical distal skin hand "+hilt);}
        if(!shield&&family.equals("axe_equipment_")&&f.weight()>=.999999F){
            var physicalWrist=f.global()[names.indexOf(side+"_wrist")].transformPosition(new Vector3f(0,.0625F,0));
            float errorToWrist=physicalWrist.distance(f.global()[names.indexOf(side+"_sword_reference")].getTranslation(new Vector3f()));
            maxWristError=java.lang.Math.max(maxWristError,errorToWrist);
            if(errorToWrist>=.002F)throw new AssertionError("Derived waist axe exceeded unchanged .002 declared wrist grip threshold "+errorToWrist);
        }
        // Native idle shield retains its forearm attachment during the fade. The
        // authored distal palm bound applies to the fully sampled source, not that native endpoint.
        // Real native model anchor/quads and the cape transform remain checked across the fade.
        if(shield&&f.weight()>=.999999F){
            var handle=actual.transformPosition(local.last().pose().transformPosition(new Vector3f(0,0,.125F)));
            var physical=new Matrix4f(CombatRigLayer.BASIS).mul(f.global()[names.indexOf(side+"_forearm")]).invert().transformPosition(handle);
            float width=f.data().variant().equals("slim")?.09375F:.125F;
            if(physical.y<-.385F||physical.y>-.24F||java.lang.Math.abs(physical.x)>width+.01F||java.lang.Math.abs(physical.z)>.135F)throw new AssertionError("Real native shield handle left distal skin hand "+physical);
        }
        nativeGrips++;
    }
    public static void handRoot(AvatarRenderState state,ItemStackRenderState item,HumanoidArm arm,com.mojang.blaze3d.vertex.PoseStack parent){
        expectedItem=null;var f=state.getData(CombatRigLayer.FRAME);if(!active(state)||capeOnly||arm!=state.mainArm)return;
        var reference=new Matrix4f(parent.last().pose()).mul(CombatRigLayer.BASIS).mul(f.global()[Arrays.asList(f.data().bones()).indexOf((arm==HumanoidArm.RIGHT?"right":"left")+"_sword_reference")]);
        expectedGrip=reference.getTranslation(new Vector3f());expectedAxis=reference.transformDirection(new Vector3f(0,1,0)).normalize();expectedItem=item;
    }
    public static void quads(com.mojang.blaze3d.vertex.PoseStack poses){
        if(!enabled||!inItem)return;
        var grip=poses.last().pose().transformPosition(new Vector3f(.23F,.23F,.5F));
        var axis=poses.last().pose().transformPosition(new Vector3f(.9F,.9F,.5F)).sub(grip).normalize();
        if(grip.distance(expectedGrip)>2e-5F||axis.dot(expectedAxis)<.99999F)throw new AssertionError("Submitted native sword generated quads lost source hilt/axis");quads++;
    }
}
