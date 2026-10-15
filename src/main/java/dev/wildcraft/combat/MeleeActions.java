package dev.wildcraft.combat;

import dev.wildcraft.Wildcraft;
import dev.wildcraft.network.*;
import dev.wildcraft.player.PlayerStamina;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.entity.event.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import java.util.Comparator;

/** Server-owned phases. Native attack remains the sole damage/durability/Fuse path. */
public final class MeleeActions {
    public static final AttachmentType<MeleeView> VIEW=AttachmentRegistry.create(Wildcraft.id("melee_view"),b->b.syncWith(MeleeView.CODEC,AttachmentSyncPredicate.all()));
    public static final AttachmentType<Boolean> JUMP_CLAIM=AttachmentRegistry.create(Wildcraft.id("dodge_jump_claim"));
    private static final AttachmentType<Session> SESSION=AttachmentRegistry.create(Wildcraft.id("melee_session"));
    private static final class Session {
        ItemStack weapon;
        byte phase,direction;MeleeWeapon kind=MeleeWeapon.SWORD;boolean charged;
        long start,input,nextAttack,nextHop,lastTick=Long.MIN_VALUE;
        int serial,hurt;float yaw,hurtYaw,damageScale=1,extraKnockback;
        double moved;boolean movedInvalid,rewarded;Vec3 hopOrigin;
    }
    private MeleeActions(){}
    public static void initialize(){
        PayloadTypeRegistry.serverboundPlay().register(MeleeIntent.TYPE,MeleeIntent.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(MeleeIntent.TYPE,(i,c)->receive(c.player(),i));
        ServerTickEvents.END_SERVER_TICK.register(s->s.getPlayerList().getPlayers().forEach(MeleeActions::tick));
        ServerPlayerEvents.JOIN.register(MeleeActions::reset);
        ServerPlayerEvents.AFTER_RESPAWN.register(Wildcraft.AFTER_ATTACHMENT_TRANSFER,(old,p,alive)->reset(p));
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((p,a,b)->reset(p));
        ServerPlayConnectionEvents.DISCONNECT.register((h,s)->reset(h.player));
        ServerLivingEntityEvents.AFTER_DAMAGE.register((e,source,base,taken,blocked)->{
            if(e instanceof ServerPlayer p&&taken>0&&!blocked){
                var s=session(p);if(s.phase==MeleeView.HOP){s.phase=MeleeView.IDLE;s.movedInvalid=true;}s.hurt=6;var from=source.getSourcePosition();s.hurtYaw=from==null?p.getYRot():(float)Math.toDegrees(Math.atan2(p.getX()-from.x,from.z-p.getZ()));publish(p,s);
            }
        });
    }
    private static Session session(ServerPlayer p){return p.getAttachedOrCreate(SESSION,Session::new);}
    public static boolean weapon(ItemStack item){return MeleeWeapon.of(item)!=null;}
    public static MeleeView view(Player p){var v=p.getAttached(VIEW);return v==null?MeleeView.OFF:v;}
    public static boolean hopping(Player p){return view(p).phase()==MeleeView.HOP;}
    public static boolean blocksHands(Player p){return view(p).phase()!=MeleeView.IDLE;}
    public static boolean eligible(Player p){return Combat.compatible(p)&&!dev.wildcraft.focus.FocusTime.active(p);}
    public static void receive(ServerPlayer p,MeleeIntent i){
        var s=session(p);long now=p.level().getGameTime();
        if(!i.valid()||i.operation()==MeleeIntent.CANCEL){cancel(p,s);return;}
        if(ReactionTime.active(p)){if(i.operation()==MeleeIntent.RELEASE)ReactionTime.counter(p);return;}
        if(i.operation()==MeleeIntent.DODGE){hop(p,s,i.direction(),now);return;}
        if(!eligible(p)||!weapon(p.getMainHandItem())||p.isUsingItem()){cancel(p,s);return;}
        if(i.operation()==MeleeIntent.BEGIN){
            if(s.phase!=MeleeView.IDLE||now<s.nextAttack)return;
            s.weapon=p.getMainHandItem();s.kind=MeleeWeapon.of(s.weapon);s.charged=false;s.phase=MeleeView.CHARGE;s.start=now;s.input=now;s.serial++;publish(p,s);
        }else if(s.phase==MeleeView.CHARGE){
            if(s.weapon!=p.getMainHandItem()||now-s.input>MeleeRules.INPUT_TIMEOUT||now-s.start>MeleeRules.MAX_CHARGE){cancel(p,s);return;}
            s.input=now;
            if(i.operation()==MeleeIntent.RELEASE)release(p,s,now);
        }
    }
    private static void release(ServerPlayer p,Session s,long now){
        if(s.kind==MeleeWeapon.SPEAR&&p.cannotAttackWithItem(s.weapon,5)){cancel(p,s);return;}
        boolean charged=now-s.start>=s.kind.threshold&&!(s.kind==MeleeWeapon.MACE&&MaceItem.canSmashAttack(p));
        if(charged&&(p.getAttackStrengthScale(.5F)<.9F||PlayerStamina.get(p).stamina()<s.kind.cost)){cancel(p,s);return;}
        if(charged)PlayerStamina.consume(p,s.kind.cost);
        s.charged=charged;s.phase=MeleeView.SWING;s.start=now;s.nextAttack=now+s.kind.recovery(charged);
        s.serial++;
        if(!p.onGround()){s.phase=p.fallDistance>=2?MeleeView.DROP_SWING:MeleeView.AIR_SWING;}publish(p,s);
        var target=pointed(p);
        if(s.kind!=MeleeWeapon.SPEAR&&target==null){nativeAttack(p,null,false);return;}
        // Native mace smash owns its fall bonus, radial knockback and successful-hit fall reset.
        boolean smash=s.kind==MeleeWeapon.MACE&&MaceItem.canSmashAttack(p);
        s.damageScale=Math.min(1.8F,Math.max(charged?s.kind.multiplier:1,dropMultiplier(p)));s.extraKnockback=charged?s.kind.knockback:0;
        if(smash){s.damageScale=1;s.extraKnockback=0;}
        try{nativeAttack(p,target,true);}finally{s.damageScale=1;s.extraKnockback=0;}
    }
    /** Current real aim and native reach; obstacles truncate the ray before entity selection. */
    public static LivingEntity pointed(Player p){
        if(MeleeWeapon.of(p.getMainHandItem())==MeleeWeapon.SPEAR){
            Vec3 origin=p.getEyePosition();
            return net.minecraft.world.entity.projectile.ProjectileUtil.getHitEntitiesAlong(p,p.getAttackRangeWith(p.getMainHandItem()),e->net.minecraft.world.item.component.PiercingWeapon.canHitEntity(p,e),ClipContext.Block.COLLIDER)
                .map(block->java.util.List.<EntityHitResult>of(),hits->hits).stream().filter(hit->hit.getEntity() instanceof LivingEntity)
                .min(Comparator.comparingDouble(hit->origin.distanceToSqr(hit.getLocation()))).map(hit->(LivingEntity)hit.getEntity()).orElse(null);
        }
        Vec3 eye=p.getEyePosition(),end=eye.add(p.getLookAngle().scale(p.entityInteractionRange()));
        var block=p.level().clip(new ClipContext(eye,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
        double max=block.getType()==HitResult.Type.MISS?eye.distanceToSqr(end):eye.distanceToSqr(block.getLocation());
        return p.level().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1),t->t!=p&&t.isAlive()&&t.isPickable()&&t.isAttackable()&&!p.isAlliedTo(t))
            .stream().filter(t->t.getBoundingBox().inflate(t.getPickRadius()).clip(eye,end).map(v->eye.distanceToSqr(v)<max).orElse(false))
            .min(Comparator.comparingDouble(t->t.getBoundingBox().inflate(t.getPickRadius()).clip(eye,end).map(eye::distanceToSqr).orElse(Double.MAX_VALUE))).orElse(null);
    }
    private static void hop(ServerPlayer p,Session s,byte direction,long now){
        if(!eligible(p)||!p.onGround()||s.phase==MeleeView.HOP||s.phase==MeleeView.SWING||s.phase==MeleeView.AIR_SWING||s.phase==MeleeView.DROP_SWING||now<s.nextHop
                ||p.isUsingItem()&&!p.getUseItem().is(Items.SHIELD)||PlayerStamina.get(p).stamina()<MeleeRules.HOP_COST)return;
        cancel(p,s);Combat.releaseDefense(p);
        if(p.isUsingItem())p.stopUsingItem();
        PlayerStamina.consume(p,MeleeRules.HOP_COST);s.phase=MeleeView.HOP;s.direction=direction;s.yaw=Combat.facing(p,p.getYRot());s.start=now;s.nextHop=now+MeleeRules.HOP_INTERVAL;s.serial++;s.moved=0;s.movedInvalid=false;s.rewarded=false;s.hopOrigin=p.position();ThreatActions.dodgeStarted(p);
        var v=hopVector(direction,s.yaw);p.setSprinting(false);p.setDeltaMovement(v.x,MeleeRules.HOP_UP,v.z);p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(p));publish(p,s);
    }
    public static Vec3 hopVector(byte direction,float yaw){
        double r=Math.toRadians(yaw);return direction==-2?new Vec3(Math.sin(r),0,-Math.cos(r)).scale(MeleeRules.HOP_SPEED):new Vec3(Math.cos(r)*direction,0,Math.sin(r)*direction).scale(MeleeRules.HOP_SPEED);
    }
    /** Only the native local movement authority applies motion; vanilla collision/gravity still run. */
    public static Vec3 travel(Player p,Vec3 input){
        if(hopping(p)&&p.level().isClientSide()&&p.isLocalInstanceAuthoritative()){
            var v=hopVector(view(p).direction(),view(p).yaw());p.setDeltaMovement(v.x,p.getDeltaMovement().y,v.z);p.setSprinting(false);return Vec3.ZERO;
        }return input;
    }
    public static boolean acceptMovement(ServerPlayer p,double x,double y,double z){
        if(!hopping(p))return true;
        var s=session(p);double d=Math.hypot(x-p.getX(),z-p.getZ());s.moved+=d;
        if(!Double.isFinite(d)||!Double.isFinite(y)||d>.65||Math.abs(y-p.getY())>.65||s.moved>3.5){s.movedInvalid=true;cancel(p,s);p.connection.teleport(p.getX(),p.getY(),p.getZ(),p.getYRot(),p.getXRot());return false;}
        return true;
    }
    public static boolean claimHop(ServerPlayer p,Vec3 origin){var s=p.getAttached(SESSION);if(s==null||s.movedInvalid||s.rewarded||!origin.equals(s.hopOrigin)||s.moved<=0||s.moved>3.5||p.position().distanceTo(origin)>3.5)return false;s.rewarded=true;return true;}
    public static float dropMultiplier(Player p){return !p.onGround()&&p.getDeltaMovement().y<0&&p.fallDistance>=2?(float)Math.min(1.5,1+.05*p.fallDistance):1;}
    public static void counterSwing(ServerPlayer p,LivingEntity target){
        var kind=MeleeWeapon.of(p.getMainHandItem());if(kind==null)return;
        var s=session(p);cancel(p,s);s.weapon=p.getMainHandItem();s.kind=kind;s.charged=false;s.phase=MeleeView.SWING;s.start=p.level().getGameTime();s.nextAttack=s.start+s.kind.recovery(false);s.serial++;publish(p,s);
        nativeAttack(p,target,target!=null);
    }
    private static void nativeAttack(ServerPlayer p,LivingEntity target,boolean allowPierce){
        if(MeleeWeapon.of(p.getMainHandItem())==MeleeWeapon.SPEAR){
            if(p.cannotAttackWithItem(p.getMainHandItem(),5))return;
            var piercing=p.getMainHandItem().get(net.minecraft.core.component.DataComponents.PIERCING_WEAPON);
            if(allowPierce) piercing.attack(p,EquipmentSlot.MAINHAND);
            else {p.onAttack();p.postPiercingAttack();piercing.makeSound(p);p.swingAndResetAttackStrength(InteractionHand.MAIN_HAND,p.getMainHandItem().getAttackAnimation(),false);}
        }else {p.swing(InteractionHand.MAIN_HAND,p.getMainHandItem().getAttackAnimation(),true);if(target==null)p.onAttack();else p.attack(target);}
    }
    public static float damageScale(Player p){var s=p.getAttached(SESSION);return s==null?1:s.damageScale;}
    public static float knockback(Player p){var s=p.getAttached(SESSION);return s==null?0:s.extraKnockback;}
    public static void tick(ServerPlayer p){
        var s=p.getAttached(SESSION);if(s==null)return;long now=p.level().getGameTime();
        if(!eligible(p)){cancel(p,s);s.hurt=0;publish(p,s);return;}
        if(s.lastTick!=now){s.lastTick=now;if(s.hurt>0)s.hurt--;}
        if((s.phase==MeleeView.CHARGE||s.phase==MeleeView.SWING||s.phase==MeleeView.AIR_SWING||s.phase==MeleeView.DROP_SWING)&&(s.weapon!=p.getMainHandItem()||p.isUsingItem()))cancel(p,s);
        if(s.phase==MeleeView.CHARGE&&(now-s.input>MeleeRules.INPUT_TIMEOUT||now-s.start>MeleeRules.MAX_CHARGE))cancel(p,s);
        if((s.phase==MeleeView.SWING||s.phase==MeleeView.AIR_SWING||s.phase==MeleeView.DROP_SWING)&&now>=s.nextAttack||s.phase==MeleeView.HOP&&now-s.start>=MeleeRules.HOP_TICKS)s.phase=MeleeView.IDLE;
        publish(p,s);
    }
    private static void cancel(ServerPlayer p,Session s){s.phase=MeleeView.IDLE;s.weapon=null;s.damageScale=1;s.extraKnockback=0;publish(p,s);}
    private static void publish(ServerPlayer p,Session s){
        int age=s.phase==MeleeView.IDLE?0:(int)Math.max(0,p.level().getGameTime()-s.start);
        int duration=s.phase==MeleeView.CHARGE?s.kind.threshold:s.phase==MeleeView.HOP?MeleeRules.HOP_TICKS:s.kind.recovery(s.charged);
        var v=new MeleeView(s.phase,s.kind.id(),s.charged,age,duration,s.direction,s.yaw,s.serial,s.hurt,s.hurtYaw);if(!v.equals(view(p)))p.setAttached(VIEW,v);
    }
    public static void reset(ServerPlayer p){p.removeAttached(SESSION);p.removeAttached(JUMP_CLAIM);p.setAttached(VIEW,MeleeView.OFF);}
}
