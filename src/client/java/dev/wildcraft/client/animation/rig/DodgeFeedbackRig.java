package dev.wildcraft.client.animation.rig;
import dev.wildcraft.combat.*;
import dev.wildcraft.network.*;
import dev.wildcraft.traversal.*;
import java.util.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.*;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
/** Native successful serial owns one short authored upper-body envelope, over a qualified current frame. */
public final class DodgeFeedbackRig {
    public record Pulse(String clip,float time,int serial){}
    public static final RenderStateDataKey<Pulse> PULSE=RenderStateDataKey.create(()->"wildcraft:dodge_feedback_rig");
    private static final Map<Player,Motion> MOTION=new WeakHashMap<>();
    private static final class Motion {int seen,blockedHop=-1,slot;float age=.24F;long stamp;String direction;Object dimension;Item main,off;HumanoidArm arm;}
    private DodgeFeedbackRig(){}
    public static void initialize(){
        ClientPlayConnectionEvents.DISCONNECT.register((h,c)->MOTION.clear());
        ScreenEvents.AFTER_INIT.register((c,s,w,h)->{if(c.player!=null&&!s.isPauseScreen()){interrupt(c.player);ScreenEvents.remove(s).register(closed->{if(c.player!=null)interrupt(c.player);});}});
    }
    private static void interrupt(Player p){var m=MOTION.computeIfAbsent(p,k->new Motion());m.age=.24F;m.dimension=p.level().dimension();m.blockedHop=MeleeActions.view(p).serial();}
    public static void extract(Player p,AvatarRenderState state){
        state.setData(PULSE,null);var c=Minecraft.getInstance();if(p!=c.player)return;
        var m=MOTION.computeIfAbsent(p,k->new Motion());var reaction=ReactionTime.view(p);var v=MeleeActions.view(p);
        if(m.dimension!=p.level().dimension()){m.seen=reaction.serial();m.age=.24F;m.stamp=0;m.blockedHop=v.serial();m.dimension=p.level().dimension();}
        long now=System.nanoTime();float dt=m.stamp==0||c.isPaused()?0:Math.max(0,(now-m.stamp)/1_000_000_000F);m.stamp=now;
        if(reaction.phase()!=ReactionView.IDLE&&reaction.serial()!=0&&reaction.serial()!=m.seen){
            m.seen=reaction.serial();m.direction=switch(v.direction()){case 1->"left";case -1->"right";case -2->"back";default->null;};
            m.age=m.direction==null||v.serial()==m.blockedHop?.24F:0;dt=0;m.slot=p.getInventory().getSelectedSlot();m.main=p.getMainHandItem().getItem();m.off=p.getOffhandItem().getItem();m.arm=p.getMainArm();
        }
        boolean action=v.phase()!=MeleeView.IDLE&&v.phase()!=MeleeView.HOP;
        if(reaction.phase()==ReactionView.IDLE||!Combat.compatible(p)||Climbing.active(p)||Gliding.active(p)||p.isUsingItem()||v.hurt()!=0||action||c.options.keyAttack.isDown()||c.options.keyUse.isDown()
            ||c.gui.screen()!=null&&!c.gui.screen().isPauseScreen()||p.getInventory().getSelectedSlot()!=m.slot||p.getMainHandItem().getItem()!=m.main||p.getOffhandItem().getItem()!=m.off||p.getMainArm()!=m.arm){m.age=.24F;return;}
        m.age+=dt;if(m.age>=.24F)return;
        var frame=state.getData(CombatRigLayer.FRAME);
        // Manual READY does not slow world ticks: the real HOP can finish before the .24s envelope.
        // Reconstruct the current native pose only in the same default sword scope; never retain a frozen HOP.
        if(frame==null){frame=nativeBase(p,state);if(frame==null){m.age=.24F;return;}}
        if(!(frame.clip().startsWith("dodge_")||frame.clip().startsWith("walk_")||frame.clip().startsWith("idle_"))){m.age=.24F;return;}
        String variant=state.skin.model()==PlayerModelType.SLIM?"slim":"standard";
        String name="dodge_feedback_"+m.direction+"_"+(p.getOffhandItem().isEmpty()?"empty":"carry")+"_"+(p.getMainArm()==HumanoidArm.RIGHT?"R":"L");if(!frame.data().clips().containsKey(name))return;
        state.setData(CombatRigLayer.FRAME,frame);apply(state,frame,variant,name,m.age/.24F,m.seen);
    }
    private static CombatRigLayer.Frame nativeBase(Player p,AvatarRenderState state){
        if(MeleeActions.view(p).phase()!=MeleeView.IDLE||p.getPose()!=Pose.STANDING||p.isCrouching()||p.isInvisible()||p.isCurrentlyGlowing()||state.scale!=1
            ||MeleeWeapon.of(p.getMainHandItem())!=MeleeWeapon.SWORD||!p.getOffhandItem().isEmpty()&&!p.getOffhandItem().is(Items.SHIELD)
            ||state.showCape&&state.skin.cape()!=null||ParryActions.view(p).remaining()!=0)return null;
        for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET})if(!p.getItemBySlot(slot).isEmpty())return null;
        var settles=state.getData(dev.wildcraft.client.render.BackTransitions.SETTLES);if(settles!=null)for(var s:settles)if(s!=null)return null;
        String variant=state.skin.model()==PlayerModelType.SLIM?"slim":"standard";var data=CombatRig.asset(variant);if(data==null)return null;
        var model=CombatRigLayer.reference(state.skin.model());model.setupAnim(state);var global=new Matrix4f[data.bones().length];
        for(int i=0;i<global.length;i++)global[i]=new Matrix4f(CombatRigLayer.BASIS).mul(CombatRigLayer.delta(CombatRigLayer.part(model,data.bones()[i]))).mul(CombatRigLayer.BASIS).mul(CombatRig.matrix(data.bindWorld()[i]));
        for(var arm:HumanoidArm.values()){
            boolean main=arm==p.getMainArm(),shield=!main&&p.getOffhandItem().is(Items.SHIELD);if(!main&&!shield)continue;
            if((arm==HumanoidArm.RIGHT?state.rightHandItemState:state.leftHandItemState).isEmpty())return null;
            String bone=(arm==HumanoidArm.RIGHT?"right_":"left_")+(shield?"shield_mount":"sword_reference");
            global[Arrays.asList(data.bones()).indexOf(bone)]=CombatRig.nativeFeedbackReference(model,state,arm,shield);
        }
        return new CombatRigLayer.Frame(data,global,"dodge_native_base",0,1);
    }
    private static void apply(AvatarRenderState state,CombatRigLayer.Frame frame,String variant,String name,float time,int serial){
        var start=CombatRig.sample(variant,name,0);var sample=CombatRig.sample(variant,name,time);var global=frame.global();int pelvis=Arrays.asList(frame.data().bones()).indexOf("pelvis");
        var basis=new Matrix4f(global[pelvis]).mul(new Matrix4f(start[pelvis]).invert());var inverse=new Matrix4f(basis).invert();
        for(int i=0;i<global.length;i++){
            String bone=frame.data().bones()[i];if(bone.equals("root")||bone.equals("pelvis")||bone.contains("thigh")||bone.contains("knee")||bone.contains("shin")||bone.contains("ankle")||bone.contains("foot"))continue;
            var target=new Matrix4f(basis).mul(sample[i]).mul(new Matrix4f(start[i]).invert()).mul(inverse).mul(global[i]);
            if(bone.equals("head"))target.translationRotateScale(target.getTranslation(new Vector3f()),global[i].getUnnormalizedRotation(new Quaternionf()).normalize(),global[i].getScale(new Vector3f()));global[i]=target;
        }
        state.setData(PULSE,new Pulse(name,time,serial));
    }
}
