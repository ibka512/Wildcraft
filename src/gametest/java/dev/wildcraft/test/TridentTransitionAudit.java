package dev.wildcraft.test;
import dev.wildcraft.client.animation.rig.*;
import dev.wildcraft.client.render.*;
import java.util.Arrays;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.item.Items;
/** Test-only actual source body and real native equipment ownership/GPU observations. */
public final class TridentTransitionAudit {
    private static final java.util.Map<net.minecraft.world.entity.player.PlayerModelType,net.minecraft.client.model.player.PlayerModel> MODELS=new java.util.EnumMap<>(net.minecraft.world.entity.player.PlayerModelType.class);
    public static boolean enabled;
    public static int frames,full,backGpu,specialGpu;
    public static net.minecraft.client.renderer.item.ItemStackRenderState expectedItem;public static org.joml.Vector3f expectedGrip,expectedTip;public static boolean submittingExpectedItem;
    public static String clip;
    public static float latestTime;
    public static void begin(){enabled=true;frames=full=backGpu=specialGpu=0;expectedItem=null;submittingExpectedItem=false;clip=null;latestTime=0;}
    public static void observe(AvatarRenderState state){
        var f=state.getData(CombatRigLayer.FRAME);if(!enabled||f==null||!f.clip().startsWith("trident_equipment_"))return;
        var settle=BackTransitions.forArm(state,state.mainArm);if(settle==null||settle.category()!=0||settle.handOwner()!=1)throw new AssertionError("Source gear must use real main-hand same-reference settle");
        frames++;clip=f.clip();latestTime=f.time();if(Math.abs(f.time()-settle.progress())>1e-6F)throw new AssertionError("Source take/return invented another clock");
        var main=BackTransitions.held(state,state.mainArm);var items=state.getData(BackEquipmentLayer.ITEMS);
        if(items==null||(settle.draw()? !main.is(Items.TRIDENT)||!items[0].isEmpty():!main.isEmpty()||items[0].isEmpty()))throw new AssertionError("Real trident must have exactly its new hand/back owner, with no outgoing proxy");
        if(f.weight()<.999999F)return;full++;
        var model=MODELS.computeIfAbsent(state.skin.model(),type->{boolean slim=type==net.minecraft.world.entity.player.PlayerModelType.SLIM;return new net.minecraft.client.model.player.PlayerModel(net.minecraft.client.model.geom.builders.LayerDefinition.create(net.minecraft.client.model.player.PlayerModel.createMesh(net.minecraft.client.model.geom.builders.CubeDeformation.NONE,slim),64,64).bakeRoot(),slim);});model.setupAnim(state);var nativePose=new com.mojang.blaze3d.vertex.PoseStack();model.head.translateAndRotate(nativePose);
        var aim=new org.joml.Matrix4f(CombatRigLayer.BASIS).mul(nativePose.last().pose()).mul(CombatRigLayer.BASIS).getUnnormalizedRotation(new org.joml.Quaternionf()).normalize();
        if(Math.abs(aim.dot(f.global()[Arrays.asList(f.data().bones()).indexOf("head")].getUnnormalizedRotation(new org.joml.Quaternionf()).normalize()))<.99999)throw new AssertionError("Trident gesture changed native independent head aim");
        var names=Arrays.asList(f.data().bones());
        for(var arm:net.minecraft.world.entity.HumanoidArm.values()){
            var stack=BackTransitions.held(state,arm);boolean shield=stack.is(Items.SHIELD);boolean trident=arm==state.mainArm&&stack.is(Items.TRIDENT)&&settle.draw();if(!shield&&!trident)continue;
            var item=arm==net.minecraft.world.entity.HumanoidArm.RIGHT?state.rightHandItemState:state.leftHandItemState;org.joml.Matrix4f held;DodgeFeedbackAudit.checkingNativeItem=true;try{held=CombatRig.heldPose(f,item,arm,shield);}finally{DodgeFeedbackAudit.checkingNativeItem=false;}
            String side=arm==net.minecraft.world.entity.HumanoidArm.RIGHT?"right":"left";var target=new org.joml.Matrix4f(CombatRigLayer.BASIS).mul(f.global()[names.indexOf(side+(shield?"_shield_mount":"_sword_reference"))]);var palm=new org.joml.Matrix4f(CombatRigLayer.BASIS).mul(f.global()[names.indexOf(side+"_palm_grip")]).getTranslation(new org.joml.Vector3f());
            var anchor=held.transformPosition(nativePoint(item,arm,shield?new org.joml.Vector3f(0,0,.09375F):new org.joml.Vector3f(0,1.1F,0)));var handle=shield?held.transformPosition(nativePoint(item,arm,new org.joml.Vector3f(0,0,.125F))):new org.joml.Vector3f(anchor);
            if(anchor.distance(target.getTranslation(new org.joml.Vector3f()))>2e-5F||handle.distance(palm)>.015F)throw new AssertionError("Real native trident pole grip/shield handle lost authored palm "+handle.distance(palm));
            var hi=held.transformPosition(nativePoint(item,arm,shield?new org.joml.Vector3f(0,1,.09375F):new org.joml.Vector3f(0,0,0)));if(hi.sub(anchor).normalize().dot(target.transformDirection(new org.joml.Vector3f(0,shield?-1:1,0)).normalize())<.99999F)throw new AssertionError("Real native trident/shield axis differs from source reference");
        }
        var source=CombatRig.sample(f.data().variant(),f.clip(),f.time());
        for(String side:new String[]{"left","right"})source[names.indexOf(side+"_palm_grip")].setTranslation(source[names.indexOf(side+"_wrist")].transformPosition(new org.joml.Vector3f(0,.0625F,0)));
        String mainSide=state.mainArm==net.minecraft.world.entity.HumanoidArm.RIGHT?"right":"left";source[names.indexOf(mainSide+"_sword_reference")].setTranslation(source[names.indexOf(mainSide+"_palm_grip")].getTranslation(new org.joml.Vector3f()));
        if(BackTransitions.held(state,state.mainArm.getOpposite()).is(Items.SHIELD))source[names.indexOf((state.mainArm==net.minecraft.world.entity.HumanoidArm.RIGHT?"left":"right")+"_shield_mount")].translate(0,.125F,0);
        for(int i=0;i<source.length;i++){
            if(names.get(i).equals("head"))continue;float[] a=source[i].get(new float[16]),b=f.global()[i].get(new float[16]);
            for(int k=0;k<16;k++)if(Math.abs(a[k]-b[k])>2e-5F)throw new AssertionError("Actual take source mismatch "+names.get(i));
        }
    }
    public static void back(AvatarRenderState state){var f=state.getData(CombatRigLayer.FRAME);var items=state.getData(BackEquipmentLayer.ITEMS);if(enabled&&f!=null&&f.clip().startsWith("trident_equipment_stow_")&&items!=null&&!items[0].isEmpty())backGpu++;}
    public static void handRoot(AvatarRenderState state,net.minecraft.client.renderer.item.ItemStackRenderState item,net.minecraft.world.entity.HumanoidArm arm,com.mojang.blaze3d.vertex.PoseStack parent){
        expectedItem=null;var f=state.getData(CombatRigLayer.FRAME);if(!enabled||f==null||!f.clip().startsWith("trident_equipment_draw_")||f.weight()<.999999F||arm!=state.mainArm)return;
        var ref=new org.joml.Matrix4f(parent.last().pose()).mul(CombatRigLayer.BASIS).mul(f.global()[Arrays.asList(f.data().bones()).indexOf((arm==net.minecraft.world.entity.HumanoidArm.RIGHT?"right":"left")+"_sword_reference")]);expectedItem=item;expectedGrip=ref.getTranslation(new org.joml.Vector3f());expectedTip=ref.transformDirection(new org.joml.Vector3f(0,1,0)).normalize();
    }
    public static void nativeModel(com.mojang.blaze3d.vertex.PoseStack poses){
        if(!enabled||!submittingExpectedItem)return;var grip=poses.last().pose().transformPosition(new org.joml.Vector3f(0,1.1F,0));var tip=poses.last().pose().transformPosition(new org.joml.Vector3f(0,0,0)).sub(grip).normalize();if(grip.distance(expectedGrip)>2e-5F||tip.dot(expectedTip)<.99999F)throw new AssertionError("Actual TridentSpecialRenderer model/pole grip differs from source "+grip.distance(expectedGrip)+" axis="+tip.dot(expectedTip));specialGpu++;
    }
    private static org.joml.Vector3f nativePoint(net.minecraft.client.renderer.item.ItemStackRenderState item,net.minecraft.world.entity.HumanoidArm arm,org.joml.Vector3f raw){var layer=(dev.wildcraft.mixin.client.FusionLayerAccessor)((dev.wildcraft.mixin.client.FusionStateAccessor)item).wildcraft$layers()[0];var poses=new com.mojang.blaze3d.vertex.PoseStack();layer.wildcraft$transform().apply(arm==net.minecraft.world.entity.HumanoidArm.LEFT,poses.last());poses.mulPose(layer.wildcraft$local());return poses.last().pose().transformPosition(raw);}
}
