package dev.wildcraft.client.animation.rig;
import dev.wildcraft.combat.*;
import dev.wildcraft.network.MeleeView;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import org.joml.*;
/** Source-only ordinary jump from actual native ground transitions; never owns movement or action deadlines. */
public final class JumpRig {
 private static final Map<Player,Motion> MOTION=new WeakHashMap<>();
 private static final class Motion {boolean grounded,active;int start,land=-1;Object dimension;PlayerModelType model;ItemStack main,off;HumanoidArm arm;Matrix4f[] last,landing;}
 private JumpRig(){}
 static void clear(){MOTION.clear();}
 private static boolean supported(Player p,AvatarRenderState s){
  var c=Minecraft.getInstance();var v=MeleeActions.view(p);boolean shield=p.getOffhandItem().is(Items.SHIELD),guard=shield&&p.isUsingItem()&&p.getUsedItemHand()==InteractionHand.OFF_HAND&&p.getUseItem()==p.getOffhandItem();
  if(!Combat.compatible(p)||p.getPose()!=Pose.STANDING||p.isCrouching()||p.isInvisible()||p.isCurrentlyGlowing()||s.scale!=1||v.phase()!=MeleeView.IDLE||v.hurt()!=0||p.hurtTime>0||p.isUsingItem()&&!guard||ParryActions.view(p).remaining()!=0&&!guard
   ||MeleeWeapon.of(p.getMainHandItem())!=MeleeWeapon.SWORD||!p.getOffhandItem().isEmpty()&&!shield||s.showCape&&s.skin.cape()!=null||p==c.player&&c.gui.screen()!=null&&!c.gui.screen().isPauseScreen())return false;
  for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET})if(!p.getItemBySlot(slot).isEmpty())return false;
  var settles=s.getData(dev.wildcraft.client.render.BackTransitions.SETTLES);if(settles!=null)for(var settle:settles)if(settle!=null)return false;
  return true;
 }
 /** Observe even when another rig wins, so landing cannot revive a cancelled jump source. */
 static void observe(Player p,AvatarRenderState s){
  var m=MOTION.get(p);if(m==null||m.dimension!=p.level().dimension()||m.model!=s.skin.model()){m=new Motion();m.grounded=p.onGround();m.dimension=p.level().dimension();m.model=s.skin.model();MOTION.put(p,m);}
  boolean ground=p.onGround(),ok=supported(p,s),same=m.main==p.getMainHandItem()&&m.off==p.getOffhandItem()&&m.arm==p.getMainArm();
  if(!ok||m.active&&!same){m.active=false;m.land=-1;m.last=m.landing=null;}
  if(ok&&m.grounded&&!ground&&p.getDeltaMovement().y>0){m.active=true;m.start=p.tickCount;m.land=-1;m.main=p.getMainHandItem();m.off=p.getOffhandItem();m.arm=p.getMainArm();m.last=m.landing=null;}
  if(m.active&&!m.grounded&&ground){m.land=p.tickCount;m.landing=m.last;}
  m.grounded=ground;
 }
 static boolean extract(Player p,AvatarRenderState s,float partial){
  var m=MOTION.get(p);if(m==null||!m.active||!supported(p,s))return false;// Native rendering already freezes partial; clearing it rewinds takeoff, air and landing.
  float frac=partial;
  boolean guard=p.isUsingItem();String equip=guard?"guard":p.getOffhandItem().isEmpty()?"empty":"carry",side=p.getMainArm()==HumanoidArm.RIGHT?"R":"L",variant=s.skin.model()==PlayerModelType.SLIM?"slim":"standard";
  float age=(p.tickCount-m.start+frac)/20F;String phase;float time;
  if(m.land>=0){float landAge=(p.tickCount-m.land+frac)/20F;if(!p.onGround()||landAge>=.24F){m.active=false;return false;}phase="land";time=(.06F+landAge)/.3F;}
  else if(p.onGround()){m.active=false;return false;}
  else if(age<.2F){phase="takeoff";time=age/.2F;}
  else{phase="air";time=((age-.2F)%.6F)/.6F;}
  var data=CombatRig.asset(variant);String name="jump_"+phase+"_"+equip+"_"+side;if(data==null||!data.clips().containsKey(name)){m.active=false;return false;}var g=CombatRig.sample(variant,name,time);var names=Arrays.asList(data.bones());
  if(phase.equals("land")){
   for(String foot:new String[]{"left_foot","right_foot"}){var pos=g[names.indexOf(foot)].getTranslation(new Vector3f());Vec3 sole=LockedWalkRig.worldFoot(p.position(),s.bodyRot,new float[]{pos.x,pos.y,pos.z,1});var hit=p.level().clip(new ClipContext(sole.add(0,.16,0),sole.add(0,-.16,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));if(hit.getType()!=HitResult.Type.BLOCK||hit.getDirection()!=net.minecraft.core.Direction.UP||java.lang.Math.abs(hit.getLocation().y-p.getY())>.012){m.active=false;return false;}}
   float join=java.lang.Math.clamp((p.tickCount-m.land+frac),0,1);if(m.landing!=null&&join<1)for(int i=0;i<g.length;i++){String b=names.get(i);if(!b.equals("root")&&!b.equals("pelvis")&&!b.contains("thigh")&&!b.contains("shin")&&!b.contains("foot")&&!b.contains("knee")&&!b.contains("ankle"))g[i]=CombatRig.blend(m.landing[i],g[i],join);}
  }
  var model=CombatRigLayer.reference(s.skin.model());model.setupAnim(s);int head=names.indexOf("head");var aim=new Matrix4f(CombatRigLayer.BASIS).mul(CombatRigLayer.delta(model.head)).mul(CombatRigLayer.BASIS).getUnnormalizedRotation(new Quaternionf()).normalize();g[head].translationRotateScale(g[head].getTranslation(new Vector3f()),aim,g[head].getScale(new Vector3f()));
  if(guard){GuardRig.mark(p,s,partial);GuardRig.apply(p,s,data,g,1);}
  m.last=Arrays.stream(g).map(Matrix4f::new).toArray(Matrix4f[]::new);s.setData(CombatRigLayer.FRAME,new CombatRigLayer.Frame(data,g,name,time,1));return true;
 }
}
