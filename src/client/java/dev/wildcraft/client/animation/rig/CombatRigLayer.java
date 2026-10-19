package dev.wildcraft.client.animation.rig;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildcraft.client.render.BackTransitions;
import dev.wildcraft.combat.Combat;
import dev.wildcraft.combat.MeleeActions;
import dev.wildcraft.combat.MeleeWeapon;
import dev.wildcraft.network.MeleeView;
import java.util.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelType;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Qualified grounded, unarmored sword slice. Unsupported actions retain their existing native masks. */
public final class CombatRigLayer extends RenderLayer<AvatarRenderState,PlayerModel> {
    public static final Matrix4f BASIS=new Matrix4f().translation(0,1.5F,0).scale(1,-1,-1);
    public static final RenderStateDataKey<Frame> FRAME=RenderStateDataKey.create(()->"wildcraft:combat_rig_frame");
    public record Frame(CombatRig.Data data,Matrix4f[] global,String clip,float time,float weight){}
    private static final Map<Player,Motion> MOTION=new WeakHashMap<>();
    private static final PlayerModel STANDARD=model(false),SLIM=model(true);
    private static class Motion{long stamp;float elapsed,weight;String clip;float sample,join,defenseAge,lowerAge=-1;boolean wasDefending;Matrix4f[] from,last;Object dimension;PlayerModelType model;}
    private static PlayerModel model(boolean slim){return new PlayerModel(LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE,slim),64,64).bakeRoot(),slim);}
    public CombatRigLayer(RenderLayerParent<AvatarRenderState,PlayerModel> parent){super(parent);}
    public static void initialize(){
        DodgeFeedbackRig.initialize();
        // Load optional local data before gameplay: first parsing during a .32s settle
        // can consume its entire real-time window, especially for a first main-hand shield.
        ClientPlayConnectionEvents.JOIN.register((handler,sender,client)->{CombatRig.asset("standard");CombatRig.asset("slim");FirstPersonSword.asset("standard");FirstPersonSword.asset("slim");});
        ClientPlayConnectionEvents.DISCONNECT.register((h,c)->{MOTION.clear();JumpRig.clear();ClimbRig.clear();GlideRig.clear();LockedWalkRig.clear();HeavyMeleeRig.clear();BowIdleRig.clear();GroundBowRig.clear();AirBowRig.clear();});
        LivingEntityRenderLayerRegistrationCallback.EVENT.register((type,renderer,helper,context)->{if(renderer instanceof AvatarRenderer<?> avatar)helper.register(new CombatRigLayer(avatar));});
    }
    public static void extract(Player p,AvatarRenderState state,float partial){
        state.setData(dev.wildcraft.client.animation.MeleePoses.SOURCE_HURT,false);state.setData(FRAME,null);state.setData(GuardRig.PULSE,null);state.setData(FocusLegRig.PULSE,null);var client=Minecraft.getInstance();var v=MeleeActions.view(p);
        state.setData(GlideRig.CANOPY,null);JumpRig.observe(p,state);
        if(HurtRig.extract(p,state,partial)){MOTION.remove(p);ClimbRig.cancel(p);GlideRig.cancel(p);LockedWalkRig.cancel(p);HeavyMeleeRig.cancel(p);BowIdleRig.cancel(p);GroundBowRig.cancel(p);AirBowRig.cancel(p);return;}
        if(ClimbRig.extract(p,state,partial)){MOTION.remove(p);GlideRig.cancel(p);LockedWalkRig.cancel(p);HeavyMeleeRig.cancel(p);BowIdleRig.cancel(p);GroundBowRig.cancel(p);AirBowRig.cancel(p);return;}
        if(GlideRig.extract(p,state,partial)){MOTION.remove(p);LockedWalkRig.cancel(p);HeavyMeleeRig.cancel(p);BowIdleRig.cancel(p);GroundBowRig.cancel(p);AirBowRig.cancel(p);return;}
        if(AirBowRig.extract(p,state,partial)){MOTION.remove(p);GroundBowRig.cancel(p);BowIdleRig.cancel(p);LockedWalkRig.cancel(p);HeavyMeleeRig.cancel(p);return;}
        if(GroundBowRig.extract(p,state,partial)){MOTION.remove(p);BowIdleRig.cancel(p);LockedWalkRig.cancel(p);HeavyMeleeRig.cancel(p);return;}
        if(BowIdleRig.extract(p,state)){MOTION.remove(p);LockedWalkRig.cancel(p);HeavyMeleeRig.cancel(p);return;}
        if(ShieldTransitionRig.extract(p,state,partial)){MOTION.remove(p);LockedWalkRig.cancel(p);HeavyMeleeRig.cancel(p);return;}
        if(CrossbowTransitionRig.extract(p,state,partial)){MOTION.remove(p);LockedWalkRig.cancel(p);HeavyMeleeRig.cancel(p);return;}
        if(BowTransitionRig.extract(p,state,partial)){MOTION.remove(p);LockedWalkRig.cancel(p);HeavyMeleeRig.cancel(p);return;}
        if(MaceTransitionRig.extract(p,state,partial)){MOTION.remove(p);LockedWalkRig.cancel(p);HeavyMeleeRig.cancel(p);return;}
        if(SpearTransitionRig.extract(p,state,partial)){MOTION.remove(p);LockedWalkRig.cancel(p);HeavyMeleeRig.cancel(p);return;}
        if(TridentTransitionRig.extract(p,state,partial)){MOTION.remove(p);LockedWalkRig.cancel(p);HeavyMeleeRig.cancel(p);return;}
        if(AxeTransitionRig.extract(p,state,partial)){MOTION.remove(p);LockedWalkRig.cancel(p);HeavyMeleeRig.cancel(p);return;}
        if(SwordTransitionRig.extract(p,state,partial)){MOTION.remove(p);LockedWalkRig.cancel(p);HeavyMeleeRig.cancel(p);return;}
        if(AirMeleeRig.extract(p,state,partial)){MOTION.remove(p);LockedWalkRig.cancel(p);HeavyMeleeRig.cancel(p);return;}
        if(HopRig.extract(p,state,partial)){MOTION.remove(p);LockedWalkRig.cancel(p);HeavyMeleeRig.cancel(p);return;}
        if(LockedWalkRig.extract(p,state,partial)){MOTION.remove(p);HeavyMeleeRig.cancel(p);return;}
        if(AirMaceRig.extract(p,state,partial)){MOTION.remove(p);LockedWalkRig.cancel(p);HeavyMeleeRig.cancel(p);return;}
        if(AirSpearRig.extract(p,state,partial)){MOTION.remove(p);LockedWalkRig.cancel(p);HeavyMeleeRig.cancel(p);return;}
        if(AirTridentRig.extract(p,state,partial)){MOTION.remove(p);LockedWalkRig.cancel(p);HeavyMeleeRig.cancel(p);return;}
        if(GroundMaceRig.extract(p,state,partial)){MOTION.remove(p);LockedWalkRig.cancel(p);HeavyMeleeRig.cancel(p);return;}
        if(GroundSpearRig.extract(p,state,partial)){MOTION.remove(p);LockedWalkRig.cancel(p);HeavyMeleeRig.cancel(p);return;}
        if(GroundTridentRig.extract(p,state,partial)){MOTION.remove(p);LockedWalkRig.cancel(p);HeavyMeleeRig.cancel(p);return;}
        if(HeavyMeleeRig.extract(p,state,partial)){MOTION.remove(p);return;}
        if(JumpRig.extract(p,state,partial)){MOTION.remove(p);LockedWalkRig.cancel(p);HeavyMeleeRig.cancel(p);return;}
        boolean swing=v.phase()==MeleeView.SWING&&!v.charged();
        boolean shield=p.getOffhandItem().is(net.minecraft.world.item.Items.SHIELD);
        boolean defending=shield&&p.isUsingItem()&&p.getUsedItemHand()==net.minecraft.world.InteractionHand.OFF_HAND&&p.getUseItem().is(net.minecraft.world.item.Items.SHIELD);
        var previous=MOTION.get(p);boolean lowering=previous!=null&&!defending&&(previous.wasDefending||previous.lowerAge>=0&&previous.lowerAge<.18F);
        boolean supported=Combat.compatible(p)&&!p.isInvisible()&&!p.isCurrentlyGlowing()&&p.onGround()&&!p.isCrouching()&&(!p.isUsingItem()||defending)
            &&p.getDeltaMovement().horizontalDistanceSqr()<.0004&&MeleeWeapon.of(p.getMainHandItem())==MeleeWeapon.SWORD&&(p.getOffhandItem().isEmpty()||shield&&(swing||Combat.locked(p)&&(defending||lowering)))
            &&v.hurt()==0&&(dev.wildcraft.combat.ParryActions.view(p).remaining()==0||GuardRig.available(p,state))&&(v.phase()==MeleeView.IDLE||v.phase()==MeleeView.SWING&&!v.charged())
            &&!(state.showCape&&state.skin.cape()!=null)
            &&!(p==client.player&&client.gui.screen()!=null&&!client.gui.screen().isPauseScreen());
        for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET})supported&=p.getItemBySlot(slot).isEmpty();
        // Source take/return clips have not been qualified; preserve the existing mutually exclusive path.
        var settles=state.getData(BackTransitions.SETTLES);if(settles!=null)for(var s:settles)if(s!=null)supported=false;
        if(!supported){MOTION.remove(p);return;}
        String variant=state.skin.model()==PlayerModelType.SLIM?"slim":"standard";var data=CombatRig.asset(variant);if(data==null){MOTION.remove(p);return;}
        var m=MOTION.computeIfAbsent(p,k->new Motion());
        if(m.dimension!=p.level().dimension()||m.model!=state.skin.model()){m.weight=0;m.elapsed=0;m.clip=null;m.last=null;m.from=null;m.dimension=p.level().dimension();m.model=state.skin.model();}
        // Pause and cancellation already gate this clock; never discard an active frame gap.
        long now=System.nanoTime();float dt=m.stamp==0||client.isPaused()?0:Math.max(0,(now-m.stamp)/1_000_000_000F);m.stamp=now;m.elapsed+=dt;
        if(defending){m.defenseAge=m.wasDefending?m.defenseAge+dt:0;m.lowerAge=-1;}
        else if(m.wasDefending)m.lowerAge=0;
        else if(m.lowerAge>=0)m.lowerAge+=dt;
        m.wasDefending=defending;if(swing)m.lowerAge=-1;
        lowering=m.lowerAge>=0&&m.lowerAge<.18F;
        boolean selected=swing||defending||lowering||Combat.locked(p);String side=p.getMainArm()==net.minecraft.world.entity.HumanoidArm.RIGHT?"R":"L";
        m.weight+=Math.clamp((lowering?1-m.lowerAge/.18F:selected?1:0)-m.weight,-dt/.18F,dt/(swing?.07F:.18F));
        if(m.weight<=0){if(!selected)MOTION.remove(p);return;}
        if(selected){
            String next=(shield?(swing?"shield_A_":defending?(m.defenseAge<.18F?"raise_":"hold_"):lowering?"lower_":"shield_"):(swing?"A_":"idle_"))+side;
            if(!data.clips().containsKey(next)){MOTION.remove(p);return;} // Older local schema1 resources keep native guarding.
            if(!next.equals(m.clip)){m.from=m.last;m.join=0;m.clip=next;}
            m.join+=dt;
            // Art previews contain idle intro/outro and proposed combos; only the single strike/recovery window is used.
            float duration=data.clips().get(m.clip).duration();
            // Native rendering already freezes partial when paused; zeroing it rewinds the pose.
            m.sample=swing?(float)(.65+1.4*Math.clamp((v.age()+partial)/Math.max(1,v.duration()),0,1))/duration
                :lowering?Math.clamp(m.lowerAge/duration,0,1):defending&&m.defenseAge<.18F?Math.clamp(m.defenseAge/duration,0,1):m.elapsed%duration/duration;
        }
        if(m.clip==null)return;
        GuardRig.mark(p,state,partial);
        PlayerModel reference=state.skin.model()==PlayerModelType.SLIM?SLIM:STANDARD;reference.setupAnim(state);
        var global=CombatRig.sample(variant,m.clip,m.sample);
        float join=Math.clamp(m.join/(swing?.07F:.12F),0,1);join=join*join*(3-2*join);
        if(m.from!=null&&join<1)for(int i=0;i<global.length;i++)global[i]=CombatRig.blend(m.from[i],global[i],join);
        m.last=Arrays.stream(global).map(Matrix4f::new).toArray(Matrix4f[]::new);
        for(int i=0;i<data.bones().length;i++){
            ModelPart part=part(reference,data.bones()[i]);var nativeDelta=delta(part);var nativeBone=new Matrix4f(BASIS).mul(nativeDelta).mul(BASIS).mul(CombatRig.matrix(data.bindWorld()[i]));
            global[i]=CombatRig.blend(nativeBone,global[i],m.weight);
        }
        GuardRig.apply(p,state,data,global,m.weight);
        // Head follows independent player aim; source full-body turns never pull the camera.
        int head=Arrays.asList(data.bones()).indexOf("head");var aim=new Matrix4f(BASIS).mul(delta(reference.head)).mul(BASIS);
        var rotation=aim.getUnnormalizedRotation(new Quaternionf()).normalize();global[head].translationRotateScale(global[head].getTranslation(new Vector3f()),rotation,global[head].getScale(new Vector3f()));
        state.setData(FRAME,new Frame(data,global,m.clip,m.sample,m.weight));
    }
    static PlayerModel reference(PlayerModelType type){return type==PlayerModelType.SLIM?SLIM:STANDARD;}
    static ModelPart part(PlayerModel model,String bone){
        if(bone.equals("head"))return model.head;
        if(bone.startsWith("left_")&&(bone.contains("arm")||bone.contains("elbow")||bone.contains("wrist")||bone.contains("palm")||bone.contains("sword")||bone.contains("shield")))return model.leftArm;
        if(bone.startsWith("right_")&&(bone.contains("arm")||bone.contains("elbow")||bone.contains("wrist")||bone.contains("palm")||bone.contains("sword")||bone.contains("shield")))return model.rightArm;
        if(bone.startsWith("left_"))return model.leftLeg;if(bone.startsWith("right_"))return model.rightLeg;
        return model.body;
    }
    static Matrix4f delta(ModelPart part){
        var current=new PoseStack();part.translateAndRotate(current);var b=part.getInitialPose();
        var rest=new Matrix4f().translation(b.x()/16,b.y()/16,b.z()/16).rotateZYX(b.zRot(),b.yRot(),b.xRot()).scale(b.xScale(),b.yScale(),b.zScale());
        return new Matrix4f(current.last().pose()).mul(rest.invert());
    }
    public static Matrix4f body(Frame frame){int i=Arrays.asList(frame.data.bones()).indexOf("body");return new Matrix4f(BASIS).mul(frame.global[i]).mul(CombatRig.matrix(frame.data.bindWorld()[i]).invert()).mul(BASIS);}
    @Override public void submit(PoseStack poses,SubmitNodeCollector collector,int light,AvatarRenderState state,float yaw,float pitch){
        var frame=state.getData(FRAME);if(frame==null||state.isInvisible||state.isSpectator)return;
        submitSkin(poses,collector,light,state,frame,false);
    }
    /** Same sampled real skin for the world and the qualified first-person glider arms. */
    public static void submitSkin(PoseStack poses,SubmitNodeCollector collector,int light,AvatarRenderState state,Frame frame,boolean armsOnly){
        var data=frame.data;var transforms=new Matrix4f[data.skinBones()];var normals=new Matrix3f[data.skinBones()];
        for(int i=0;i<data.skinBones();i++){transforms[i]=new Matrix4f(frame.global[i]).mul(new Matrix4f().set(data.inverseBind()[i]));normals[i]=transforms[i].normal(new Matrix3f());}
        var type=RenderTypes.entityCutout(state.skin.body().texturePath());int overlay=LivingEntityRenderer.getOverlayCoords(state,0);
        poses.pushPose();poses.mulPose(BASIS);
        for(var mesh:data.meshes()){
            if(armsOnly&&!mesh.part().endsWith("arm"))continue;
            if(!CombatRig.visible(mesh,state))continue;float[] transformed=CombatRig.deform(mesh,transforms,normals);int[] indices=mesh.indices();
            collector.submitCustomGeometry(poses,type,(pose,buffer)->{
                for(int triangle=0;triangle<indices.length;triangle+=3)for(int corner=0;corner<4;corner++){
                    int index=indices[triangle+Math.min(corner,2)]*8;buffer.addVertex(pose,transformed[index],transformed[index+1],transformed[index+2]).setColor(-1).setUv(transformed[index+6],transformed[index+7]).setOverlay(overlay).setLight(light).setNormal(pose,transformed[index+3],transformed[index+4],transformed[index+5]);
                }
            });
        }
        poses.popPose();
    }
}
