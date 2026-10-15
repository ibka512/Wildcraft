package dev.wildcraft.combat;
import dev.wildcraft.Wildcraft;
import dev.wildcraft.fuse.FusionContent;
import dev.wildcraft.network.GuardView;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.entity.event.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
/** A fresh, server-observed independent defense edge. Native direction, wear and Fuse settle once. */
public final class ParryActions {
    public static final int WINDOW=6,REARM=12,PULSE=8;
    public static final AttachmentType<GuardView> VIEW=AttachmentRegistry.create(Wildcraft.id("guard_view"),b->b.syncWith(GuardView.CODEC,AttachmentSyncPredicate.all()));
    private static final AttachmentType<Session> SESSION=AttachmentRegistry.create(Wildcraft.id("guard_session"));
    private static final AttachmentType<Reflection> REFLECTION=AttachmentRegistry.create(Wildcraft.id("arrow_parry"));
    private static final class Session{ItemStack shield;long start,next,pulse;boolean armed;int serial;}
    public record Reflection(ServerPlayer defender,Vec3 velocity){}
    private ParryActions(){}
    public static void initialize(){
        ServerTickEvents.END_SERVER_TICK.register(s->s.getPlayerList().getPlayers().forEach(ParryActions::tick));
        ServerPlayerEvents.JOIN.register(ParryActions::reset);
        ServerPlayerEvents.AFTER_RESPAWN.register(Wildcraft.AFTER_ATTACHMENT_TRANSFER,(old,p,alive)->reset(p));
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((p,a,b)->reset(p));
        ServerPlayConnectionEvents.DISCONNECT.register((h,s)->reset(h.player));
        ServerLivingEntityEvents.AFTER_DAMAGE.register((e,source,base,taken,blocked)->{
            if(taken>0&&!blocked&&source.getDirectEntity() instanceof ServerPlayer p&&MeleeActions.damageScale(p)>1)ThreatActions.stagger(e,8);
        });
    }
    public static GuardView view(LivingEntity e){var v=e.getAttached(VIEW);return v==null?GuardView.OFF:v;}
    public static void arm(ServerPlayer p,ItemStack shield){
        var s=p.getAttachedOrCreate(SESSION,Session::new);long now=p.level().getGameTime();
        if(now<s.next||shield==null||shield.isEmpty()||p.getUseItem()!=shield||!shield.is(Items.SHIELD)||!Combat.compatible(p)||MeleeActions.blocksHands(p))return;
        s.shield=shield;s.start=now;s.armed=true;s.next=now+REARM;
    }
    public static void close(ServerPlayer p){var s=p.getAttached(SESSION);if(s!=null){s.armed=false;s.shield=null;}}
    public static boolean ordinaryArrow(AbstractArrow a){return a.getType()==EntityTypes.ARROW&&a instanceof net.minecraft.world.entity.projectile.arrow.Arrow normal&&normal.getColor()==-1&&a.getPierceLevel()==0&&!a.isOnFire()&&a.getOwner() instanceof Mob&&a.getPickupItemStackOrigin().is(Items.ARROW)&&!a.getPickupItemStackOrigin().has(FusionContent.DATA);}
    private static boolean sourceAllowed(DamageSource source){
        if(source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)||source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_SHIELD))return false;
        var direct=source.getDirectEntity();
        return source.is(DamageTypes.MOB_ATTACK)&&direct instanceof LivingEntity&&ThreatActions.supported(direct)
            ||source.is(DamageTypes.ARROW)&&direct instanceof AbstractArrow a&&ordinaryArrow(a);
    }
    private static boolean fresh(ServerPlayer p,ItemStack shield,DamageSource source){
        var s=p.getAttached(SESSION);long now=p.level().getGameTime();
        return s!=null&&s.armed&&now-s.start>=0&&now-s.start<WINDOW&&s.shield==shield&&p.isUsingItem()&&p.getUseItem()==shield&&p.getItemInHand(p.getUsedItemHand())==shield&&!shield.isEmpty()&&shield.is(Items.SHIELD)&&!p.getCooldowns().isOnCooldown(shield)&&Combat.compatible(p)&&!MeleeActions.blocksHands(p)&&sourceAllowed(source);
    }
    /** Immediate blocking is scoped to eligible damage; normal shield delay is otherwise untouched. */
    public static ItemStack blockingItem(LivingEntity e,DamageSource source,float damage,ItemStack nativeItem){
        return nativeItem==null&&damage>0&&e instanceof ServerPlayer p&&fresh(p,p.getUseItem(),source)?p.getUseItem():nativeItem;
    }
    /** Called only after native blocking has confirmed full front-facing damage reduction. */
    public static void afterBlock(LivingEntity e,ItemStack shield,DamageSource source,float damage,float blocked){
        if(!(e instanceof ServerPlayer p)||damage<=0||blocked<damage)return;
        // Capture identity before native wear may have broken the stack; a broken shield receives no reward.
        if(!fresh(p,shield,source))return;
        var s=p.getAttached(SESSION);s.armed=false;s.pulse=p.level().getGameTime()+PULSE;s.serial++;publish(p,s);
        if(source.getDirectEntity() instanceof LivingEntity attacker){ThreatActions.stagger(attacker,ThreatActions.STAGGER);attacker.knockback(.35,p.getX()-attacker.getX(),p.getZ()-attacker.getZ(),source,0,true);}
        else if(source.getDirectEntity() instanceof AbstractArrow arrow){
            Vec3 incoming=arrow.getDeltaMovement();double radians=Math.toRadians(Combat.facing(p,p.getYHeadRot()));Vec3 normal=new Vec3(-Math.sin(radians),0,Math.cos(radians));
            Vec3 reflected=incoming.subtract(normal.scale(2*incoming.dot(normal)));if(reflected.length()>3)reflected=reflected.normalize().scale(3);
            arrow.setAttached(REFLECTION,new Reflection(p,reflected));
        }
        p.level().playSound(null,p.getX(),p.getY(),p.getZ(),net.minecraft.sounds.SoundEvents.SHIELD_BLOCK,net.minecraft.sounds.SoundSource.PLAYERS,.7F,1.7F);
        p.level().sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,p.getX(),p.getY()+1.2,p.getZ(),5,.15,.15,.15,.04);
    }
    public static Reflection takeReflection(AbstractArrow a){var r=a.getAttached(REFLECTION);a.removeAttached(REFLECTION);return r;}
    public static void clearReflection(AbstractArrow a){a.removeAttached(REFLECTION);}
    public static void tick(ServerPlayer p){var s=p.getAttached(SESSION);if(s==null)return;if(!p.isAlive()||!Combat.compatible(p)||s.armed&&(p.getUseItem()!=s.shield||!p.isUsingItem()||p.level().getGameTime()-s.start>=WINDOW))close(p);publish(p,s);}
    private static void publish(ServerPlayer p,Session s){var v=new GuardView((int)Math.clamp(s.pulse-p.level().getGameTime(),0,PULSE),s.serial);if(!v.equals(view(p)))p.setAttached(VIEW,v);}
    public static void reset(ServerPlayer p){p.removeAttached(SESSION);p.setAttached(VIEW,GuardView.OFF);}
}
