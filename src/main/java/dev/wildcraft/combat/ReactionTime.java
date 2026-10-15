package dev.wildcraft.combat;

import dev.wildcraft.Wildcraft;
import dev.wildcraft.focus.*;
import dev.wildcraft.network.ReactionView;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.entity.event.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.util.IdentityHashMap;
import java.util.Map;

/** A real miss earns one short episode. It grants no invulnerability or damage multiplier. */
public final class ReactionTime {
    public static final double CONFIRM_SECONDS=.65,ACTIVE_SECONDS=1.5;
    public static final AttachmentType<ReactionView> VIEW=AttachmentRegistry.create(Wildcraft.id("reaction_view"),b->b.syncWith(ReactionView.CODEC,AttachmentSyncPredicate.targetOnly()));
    private static final Map<MinecraftServer,Session> SESSIONS=new IdentityHashMap<>();
    private static int serial;
    private static final class Session {
        final ServerPlayer player;final LivingEntity threat;final ItemStack weapon;
        final ActivePlayClock clock=new ActivePlayClock();
        final int id=++serial;
        TimeLease lease;double elapsed;int attackTicks;
        Session(ServerPlayer p,LivingEntity m){player=p;threat=m;weapon=p.getMainHandItem();attackTicks=((AttackClockAccess)p).wildcraft$attackTicks();clock.advance(System.nanoTime(),false);}
    }
    private ReactionTime(){}
    public static void initialize(){
        ServerTickEvents.END_SERVER_TICK.register(ReactionTime::pulse);
        ServerLifecycleEvents.SERVER_STOPPING.register(ReactionTime::close);
        ServerPlayConnectionEvents.DISCONNECT.register((h,s)->close(s));
        ServerPlayerEvents.AFTER_RESPAWN.register(Wildcraft.AFTER_ATTACHMENT_TRANSFER,(old,p,alive)->{cancel(old);p.setAttached(VIEW,ReactionView.OFF);});
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((p,a,b)->cancel(p));
        ServerLivingEntityEvents.AFTER_DAMAGE.register((e,source,base,taken,blocked)->{if(e instanceof ServerPlayer p&&taken>0&&!blocked)cancel(p);});
    }
    public static ReactionView view(Player p){var v=p.getAttached(VIEW);return v==null?ReactionView.OFF:v;}
    public static boolean active(Player p){return view(p).phase()==ReactionView.ACTIVE;}
    public static boolean active(MinecraftServer s){return SESSIONS.containsKey(s);}
    private static boolean valid(Session s){return Combat.compatible(s.player)&&!s.player.isUsingItem()&&s.player.getMainHandItem()==s.weapon&&ThreatActions.supported(s.threat)&&s.threat instanceof net.minecraft.world.entity.Mob mob&&mob.getTarget()==s.player&&s.threat.isAlive()&&!s.threat.isRemoved()&&s.threat.level()==s.player.level()&&s.player.distanceToSqr(s.threat)<64&&SkillPreferences.dodge(s.player)!=2;}
    public static boolean reward(ServerPlayer p,LivingEntity threat){
        var server=p.level().getServer();
        if(active(server)||FocusTime.active(server)||!FocusTime.allowedServer(server)||FocusTime.paused(server)||SkillPreferences.dodge(p)==2)return false;
        var s=new Session(p,threat);if(!valid(s))return false;
        SESSIONS.put(server,s);
        if(SkillPreferences.dodge(p)==0&&!begin(s)){close(server);return false;}
        publish(s);return true;
    }
    private static boolean begin(Session s){
        var server=s.player.level().getServer();
        if(!valid(s)||FocusTime.paused(server)||FocusTime.active(server))return false;
        s.lease=TimeLease.acquire(server,FocusTime.WORLD_RATE);
        if(s.lease==null)return false;
        s.elapsed=0;s.attackTicks=((AttackClockAccess)s.player).wildcraft$attackTicks();s.clock.advance(System.nanoTime(),false);publish(s);return true;
    }
    public static boolean confirm(ServerPlayer p){var s=SESSIONS.get(p.level().getServer());if(s==null||s.player!=p||s.lease!=null||SkillPreferences.dodge(p)!=1)return false;pulse(p.level().getServer());s=SESSIONS.get(p.level().getServer());if(s==null)return false;if(!begin(s))close(p.level().getServer());return true;}
    /** Runs on the server thread between world ticks. Entity, movement and damage immunity ticks remain native. */
    public static void pulse(MinecraftServer server){
        var s=SESSIONS.get(server);if(s==null)return;
        if(FocusTime.paused(server)||!FocusTime.allowedServer(server)||!valid(s)||s.lease!=null&&!s.lease.ownsControl()){close(server);return;}
        s.elapsed+=s.clock.advance(System.nanoTime(),false);
        if(s.lease!=null){var access=(AttackClockAccess)s.player;int cap=(int)Math.ceil(s.player.getCurrentItemAttackStrengthDelay());access.wildcraft$attackTicks(Math.max(access.wildcraft$attackTicks(),Math.min(cap,s.attackTicks+(int)(s.elapsed*20))));}
        if(s.elapsed>=(s.lease==null?CONFIRM_SECONDS:ACTIVE_SECONDS)){close(server);return;}
        publish(s);
    }
    /** One explicitly requested native swing, at the player's current aim and native reach. Whiffs consume the episode too. */
    public static boolean counter(ServerPlayer p){
        var server=p.level().getServer();pulse(server);var s=SESSIONS.get(server);
        if(s==null||s.player!=p||s.lease==null||!MeleeActions.weapon(p.getMainHandItem()))return false;
        var target=MeleeActions.pointed(p);boolean hit=target==s.threat;
        // Consume before the native attack so reentrant events cannot settle this episode twice.
        close(server);MeleeActions.counterSwing(p,hit?target:null);return true;
    }
    public static void cancel(ServerPlayer p){var s=SESSIONS.get(p.level().getServer());if(s!=null&&s.player==p)close(p.level().getServer());}
    public static void close(MinecraftServer server){var s=SESSIONS.remove(server);if(s==null)return;if(s.lease!=null)s.lease.close();s.player.setAttached(VIEW,ReactionView.OFF);}
    private static void publish(Session s){s.player.setAttached(VIEW,new ReactionView(s.lease==null?ReactionView.READY:ReactionView.ACTIVE,s.threat.getId(),Math.max(0,(s.lease==null?CONFIRM_SECONDS:ACTIVE_SECONDS)-s.elapsed),s.id));}
}
