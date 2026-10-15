package dev.wildcraft.combat;
import dev.wildcraft.Wildcraft;
import dev.wildcraft.network.ThreatView;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
/** Only native zombie/husk melee goals participate. Navigation and the native hit are retained. */
public final class ThreatActions {
    public static final int WINDUP=10,SWING=6,STAGGER=12;
    public static final AttachmentType<ThreatView> VIEW=AttachmentRegistry.create(Wildcraft.id("threat_view"),b->b.syncWith(ThreatView.CODEC,AttachmentSyncPredicate.all()));
    private static final AttachmentType<Session> SESSION=AttachmentRegistry.create(Wildcraft.id("threat_session"));
    private static final class Session{LivingEntity target;byte phase;long start,nextAttack;int duration;ServerPlayer dodge;net.minecraft.world.phys.Vec3 origin;long dodgeStart;}
    private ThreatActions(){}
    public static void initialize(){}
    public static boolean supported(Entity e){return e.getType()==EntityTypes.ZOMBIE||e.getType()==EntityTypes.HUSK;}
    public static ThreatView view(LivingEntity e){var v=e.getAttached(VIEW);return v==null?ThreatView.OFF:v;}
    public static boolean staggered(LivingEntity e){var s=e.getAttached(SESSION);return s!=null&&s.phase==ThreatView.STAGGER&&e.level().getGameTime()-s.start<s.duration;}
    private static boolean qualified(PathfinderMob mob,LivingEntity t){return mob.isAlive()&&t!=null&&t==mob.getTarget()&&t.isAlive()&&t.level()==mob.level()&&!(t instanceof Player p&&(p.isCreative()||p.isSpectator()))&&mob.isWithinMeleeAttackRange(t)&&mob.hasLineOfSight(t);}
    /** Returns true only once, after a complete preparation against the same still-valid target. */
    public static boolean step(PathfinderMob mob,LivingEntity target,boolean ready){
        var s=mob.getAttachedOrCreate(SESSION,Session::new);long now=mob.level().getGameTime();
        if(staggered(mob))return false;
        if(!qualified(mob,target)){if(escapePending(mob,s,target)){finishEscape(mob,s);return false;}if(s.phase==ThreatView.SWING&&now-s.start<SWING){publish(mob,s);return false;}cancel(mob);return false;}
        if(s.phase==ThreatView.WINDUP){
            if(s.target!=target){cancel(mob);return false;}
            if(now-s.start>=WINDUP){s.phase=ThreatView.SWING;s.start=now;s.nextAttack=now+20;s.duration=SWING;s.target=null;s.dodge=null;publish(mob,s);return true;}
        }else if(ready&&now>=s.nextAttack&&s.phase!=ThreatView.SWING){s.phase=ThreatView.WINDUP;s.start=now;s.duration=WINDUP;s.target=target;s.dodge=null;}
        publish(mob,s);return false;
    }
    public static boolean keepGoal(PathfinderMob mob){var t=mob.getTarget();return view(mob).phase()==ThreatView.WINDUP&&(qualified(mob,t)||escapePending(mob,mob.getAttached(SESSION),t))||staggered(mob)&&t!=null&&t.isAlive()&&!(t instanceof Player p&&(p.isCreative()||p.isSpectator()));}
    public static void stagger(LivingEntity mob,int ticks){
        if(!supported(mob)||!mob.isAlive())return;
        var s=mob.getAttachedOrCreate(SESSION,Session::new);s.phase=ThreatView.STAGGER;s.start=mob.level().getGameTime();s.duration=ticks;s.target=null;s.dodge=null;
        if(mob instanceof PathfinderMob m)m.getNavigation().stop();publish(mob,s);
    }
    public static void cancel(LivingEntity mob){var s=mob.getAttached(SESSION);if(s!=null&&!staggered(mob)){s.phase=ThreatView.IDLE;s.target=null;s.dodge=null;publish(mob,s);}}
    public static void tick(LivingEntity mob){
        if(mob.level().isClientSide()||!supported(mob))return;var s=mob.getAttached(SESSION);if(s==null)return;
        long age=mob.level().getGameTime()-s.start;
        if(mob instanceof PathfinderMob m&&!qualified(m,s.target)&&escapePending(m,s,s.target)){finishEscape(m,s);publish(mob,s);return;}
        if(!mob.isAlive()||s.phase==ThreatView.WINDUP&&(!(mob instanceof PathfinderMob m)||!qualified(m,s.target))||s.phase!=ThreatView.WINDUP&&age>=s.duration){s.phase=ThreatView.IDLE;s.target=null;s.dodge=null;}
        publish(mob,s);
    }
    /** Capture only an imminent real native attack, after the server accepts and pays for a hop. */
    public static void dodgeStarted(ServerPlayer p){
        var threats=p.level().getEntitiesOfClass(PathfinderMob.class,p.getBoundingBox().inflate(3),m->supported(m));
        for(var m:threats){var s=m.getAttached(SESSION);if(s==null||s.dodge!=null||s.phase!=ThreatView.WINDUP||s.target!=p||!qualified(m,p))continue;
            long age=m.level().getGameTime()-s.start;if(age<6||age>=WINDUP)continue;
            s.dodge=p;s.origin=p.position();s.dodgeStart=m.level().getGameTime();
        }
    }
    private static boolean escapePending(PathfinderMob m,Session s,LivingEntity target){
        return s!=null&&s.phase==ThreatView.WINDUP&&s.dodge!=null&&s.dodge==s.target&&target==s.target&&m.getTarget()==target
            &&m.isAlive()&&target.isAlive()&&target.level()==m.level()&&MeleeActions.eligible(s.dodge)
            &&m.level().getGameTime()-s.dodgeStart<=MeleeRules.HOP_TICKS&&m.hasLineOfSight(target);
    }
    private static void finishEscape(PathfinderMob m,Session s){
        long now=m.level().getGameTime();if(now-s.start<WINDUP)return;
        var p=s.dodge;
        // A wall stopping the hop, walking early, teleporting, or still being in range is no perfect miss.
        if(!m.isWithinMeleeAttackRange(p)&&p.position().subtract(s.origin).horizontalDistance()>=.25&&MeleeActions.claimHop(p,s.origin))ReactionTime.reward(p,m);
        s.phase=ThreatView.SWING;s.start=now;s.nextAttack=now+20;s.duration=SWING;s.target=null;s.dodge=null;
    }
    private static void publish(LivingEntity e,Session s){var v=s.phase==ThreatView.IDLE?ThreatView.OFF:new ThreatView(s.phase,(int)Math.max(0,e.level().getGameTime()-s.start),s.duration);if(!v.equals(view(e)))e.setAttached(VIEW,v);}
}
