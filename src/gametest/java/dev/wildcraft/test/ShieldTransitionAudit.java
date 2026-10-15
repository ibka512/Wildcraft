package dev.wildcraft.test;
import dev.wildcraft.client.animation.rig.*;
import dev.wildcraft.client.render.*;
import java.util.Arrays;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.item.Items;
/** Test-only actual source body and real native equipment ownership/GPU observations. */
public final class ShieldTransitionAudit {
    private static final java.util.Map<net.minecraft.world.entity.player.PlayerModelType,net.minecraft.client.model.player.PlayerModel> MODELS=new java.util.EnumMap<>(net.minecraft.world.entity.player.PlayerModelType.class);
    public static boolean enabled;
    public static int frames,full,backGpu,handOwner;
    public static String clip;
    public static float latestTime;
    public static void begin(){enabled=true;frames=full=backGpu=0;clip=null;latestTime=0;handOwner=0;}
    public static void observe(AvatarRenderState state){
        var f=state.getData(CombatRigLayer.FRAME);if(!enabled||f==null||!f.clip().startsWith("shield_equipment_"))return;
        var events=state.getData(BackTransitions.SETTLES);var settle=events==null?null:events[1];if(settle==null||settle.handOwner()!=1&&settle.handOwner()!=2)throw new AssertionError("Source shield needs actual original owner1/2 event");
        var arm=settle.handOwner()==1?state.mainArm:state.mainArm.getOpposite();if(BackTransitions.forArm(state,arm)!=settle)throw new AssertionError("Shield source arm differs from actual ownership");
        handOwner=settle.handOwner();if(handOwner!=1)throw new AssertionError("Unverified offhand source must retain original path");frames++;clip=f.clip();latestTime=f.time();if(Math.abs(f.time()-settle.progress())>1e-6F)throw new AssertionError("Source take/return invented another clock");
        var main=BackTransitions.held(state,arm);var items=state.getData(BackEquipmentLayer.ITEMS);
        if(items==null||(settle.draw()? !main.is(Items.SHIELD)||!items[1].isEmpty():!main.isEmpty()||items[1].isEmpty()))throw new AssertionError("Real shield must have exactly its new hand/back owner, with no outgoing proxy");
        if(f.weight()<.999999F)return;full++;
        var model=MODELS.computeIfAbsent(state.skin.model(),type->{boolean slim=type==net.minecraft.world.entity.player.PlayerModelType.SLIM;return new net.minecraft.client.model.player.PlayerModel(net.minecraft.client.model.geom.builders.LayerDefinition.create(net.minecraft.client.model.player.PlayerModel.createMesh(net.minecraft.client.model.geom.builders.CubeDeformation.NONE,slim),64,64).bakeRoot(),slim);});model.setupAnim(state);var nativePose=new com.mojang.blaze3d.vertex.PoseStack();model.head.translateAndRotate(nativePose);
        var aim=new org.joml.Matrix4f(CombatRigLayer.BASIS).mul(nativePose.last().pose()).mul(CombatRigLayer.BASIS).getUnnormalizedRotation(new org.joml.Quaternionf()).normalize();
        if(Math.abs(aim.dot(f.global()[Arrays.asList(f.data().bones()).indexOf("head")].getUnnormalizedRotation(new org.joml.Quaternionf()).normalize()))<.99999)throw new AssertionError("Shield gesture changed native independent head aim");
        if(settle.draw()){
            var item=arm==net.minecraft.world.entity.HumanoidArm.RIGHT?state.rightHandItemState:state.leftHandItemState;org.joml.Matrix4f held;DodgeFeedbackAudit.checkingNativeItem=true;try{held=CombatRig.heldPose(f,item,arm,true);}finally{DodgeFeedbackAudit.checkingNativeItem=false;}
            var names=Arrays.asList(f.data().bones());String side=arm==net.minecraft.world.entity.HumanoidArm.RIGHT?"right":"left";
            var target=new org.joml.Matrix4f(CombatRigLayer.BASIS).mul(f.global()[names.indexOf(side+"_shield_mount")]);
            var center=held.transformPosition(nativePoint(item,arm,new org.joml.Vector3f(0,0,.09375F)));
            var handle=held.transformPosition(nativePoint(item,arm,new org.joml.Vector3f(0,0,.125F)));
            var palm=new org.joml.Matrix4f(CombatRigLayer.BASIS).mul(f.global()[names.indexOf(side+"_palm_grip")]).getTranslation(new org.joml.Vector3f());
            if(center.distance(target.getTranslation(new org.joml.Vector3f()))>2e-5F||handle.distance(palm)>.015F)throw new AssertionError("Actual native shield handle must reach authored .0625 wrist grip: "+handle.distance(palm));
            var hi=held.transformPosition(nativePoint(item,arm,new org.joml.Vector3f(0,1,.09375F)));if(hi.sub(center).normalize().dot(target.transformDirection(new org.joml.Vector3f(0,-1,0)).normalize())<.99999F)throw new AssertionError("Native shield plate keeps its actual authored axis");
        }
        var otherArm=arm.getOpposite();var other=BackTransitions.held(state,otherArm);
        if(!other.isEmpty()){
            var item=otherArm==net.minecraft.world.entity.HumanoidArm.RIGHT?state.rightHandItemState:state.leftHandItemState;org.joml.Matrix4f held;DodgeFeedbackAudit.checkingNativeItem=true;try{held=CombatRig.heldPose(f,item,otherArm,false);}finally{DodgeFeedbackAudit.checkingNativeItem=false;}
            String side=otherArm==net.minecraft.world.entity.HumanoidArm.RIGHT?"right":"left";var names=Arrays.asList(f.data().bones());
            var hilt=held.transformPosition(nativePoint(item,otherArm,new org.joml.Vector3f(.23F,.23F,.5F)));var target=new org.joml.Matrix4f(CombatRigLayer.BASIS).mul(f.global()[names.indexOf(side+"_sword_reference")]).getTranslation(new org.joml.Vector3f());var palm=new org.joml.Matrix4f(CombatRigLayer.BASIS).mul(f.global()[names.indexOf(side+"_palm_grip")]).getTranslation(new org.joml.Vector3f());
            if(hilt.distance(target)>2e-5F||hilt.distance(palm)>.015F)throw new AssertionError("Actual other-hand native sword hilt must reach its authored grip "+hilt.distance(palm));
        }
        var source=CombatRig.sample(f.data().variant(),f.clip(),f.time());var names=Arrays.asList(f.data().bones());
        source[names.indexOf((arm==net.minecraft.world.entity.HumanoidArm.RIGHT?"right":"left")+"_shield_mount")].translate(0,.125F,0);
        for(int i=0;i<source.length;i++){
            if(names.get(i).equals("head"))continue;float[] a=source[i].get(new float[16]),b=f.global()[i].get(new float[16]);
            for(int k=0;k<16;k++)if(Math.abs(a[k]-b[k])>2e-5F)throw new AssertionError("Actual shield take source mismatch "+names.get(i));
        }
    }
    public static void back(AvatarRenderState state){var f=state.getData(CombatRigLayer.FRAME);var items=state.getData(BackEquipmentLayer.ITEMS);if(enabled&&f!=null&&f.clip().startsWith("shield_equipment_stow_")&&items!=null&&!items[1].isEmpty())backGpu++;}
    private static org.joml.Vector3f nativePoint(net.minecraft.client.renderer.item.ItemStackRenderState item,net.minecraft.world.entity.HumanoidArm arm,org.joml.Vector3f raw){var layer=(dev.wildcraft.mixin.client.FusionLayerAccessor)((dev.wildcraft.mixin.client.FusionStateAccessor)item).wildcraft$layers()[0];var poses=new com.mojang.blaze3d.vertex.PoseStack();layer.wildcraft$transform().apply(arm==net.minecraft.world.entity.HumanoidArm.LEFT,poses.last());poses.mulPose(layer.wildcraft$local());return poses.last().pose().transformPosition(raw);}

}
