package dev.wildcraft.combat;

import dev.wildcraft.Wildcraft;
import dev.wildcraft.network.*;
import dev.wildcraft.traversal.*;
import java.util.Comparator;
import java.util.List;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.entity.event.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** Target qualification and independent defense. Damage stays in native item blocking. */
public final class Combat {
    public static final AttachmentType<CombatView> VIEW=AttachmentRegistry.create(Wildcraft.id("combat_view"),b->b.syncWith(CombatView.CODEC,AttachmentSyncPredicate.all()));
    private static final AttachmentType<Session> SESSION=AttachmentRegistry.create(Wildcraft.id("combat_session"));
    public static final int OCCLUSION_GRACE=10, INPUT_TIMEOUT=20;
    private static final class Session {
        LivingEntity target;
        ItemStack shield;
        InteractionHand hand;
        boolean held, blocked, neutral;
        int range=1;
        long hiddenSince=Long.MIN_VALUE;
        long input=Long.MIN_VALUE, command=Long.MIN_VALUE;
    }
    private Combat(){}
    public static void initialize(){
        PayloadTypeRegistry.serverboundPlay().register(CombatIntent.TYPE,CombatIntent.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(CombatIntent.TYPE,(intent,ctx)->receive(ctx.player(),intent));
        ServerTickEvents.END_SERVER_TICK.register(s->s.getPlayerList().getPlayers().forEach(Combat::tick));
        ServerPlayerEvents.JOIN.register(Combat::reset);
        ServerPlayerEvents.AFTER_RESPAWN.register(Wildcraft.AFTER_ATTACHMENT_TRANSFER,(old,p,alive)->reset(p));
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((p,a,b)->reset(p));
        ServerPlayConnectionEvents.DISCONNECT.register((h,s)->reset(h.player));
    }
    public static boolean compatible(Player p){
        return p.isAlive()&&!p.isSpectator()&&!p.isSleeping()&&!p.isPassenger()&&!p.isInLiquid()
            &&!p.isSwimming()&&!p.isFallFlying()&&!p.getAbilities().flying
            &&!Climbing.active(p)&&!Climbing.intent(p).held()&&!Gliding.active(p)
            &&!(p.getAttached(Gliding.INTENT)!=null&&p.getAttached(Gliding.INTENT).open())
            &&p.containerMenu==p.inventoryMenu;
    }
    public static CombatView view(Player p){var v=p.getAttached(VIEW);return v==null?CombatView.IDLE:v;}
    public static LivingEntity target(Player p){
        var v=view(p);var e=v.targetId()<0?null:p.level().getEntity(v.targetId());
        return compatible(p)&&e instanceof LivingEntity t&&t.isAlive()&&!t.isRemoved()?t:null;
    }
    public static boolean locked(Player p){return target(p)!=null;}
    public static float yawTo(Player p,LivingEntity t){return (float)Math.toDegrees(Math.atan2(p.getX()-t.getX(),t.getZ()-p.getZ()));}
    public static float facing(Player p,float nativeYaw){var t=target(p);return t==null?nativeYaw:yawTo(p,t);}
    public static double searchRange(int range){return 8+4*Math.clamp(range,0,2);}
    public static boolean permitted(Player p,LivingEntity t,boolean neutral){
        if(t==p||t instanceof Player||!t.isAlive()||t.isRemoved()||t.isInvisible()||!t.isAttackable()||p.isAlliedTo(t))return false;
        boolean threat=t instanceof Mob m&&m.getTarget()==p;
        boolean passiveMonster=t instanceof net.minecraft.world.entity.NeutralMob
            ||t instanceof net.minecraft.world.entity.monster.piglin.Piglin
            ||t instanceof net.minecraft.world.entity.monster.spider.Spider;
        return threat||neutral&&t instanceof Mob||t instanceof Enemy&&!passiveMonster;
    }
    private static List<LivingEntity> candidates(ServerPlayer p,Session s,Vec3 forward){
        double range=searchRange(s.range);
        return p.level().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(range),t->permitted(p,t,s.neutral)
            &&p.distanceToSqr(t)<=range*range&&p.hasLineOfSight(t)
            &&t.getBoundingBox().getCenter().subtract(p.getEyePosition()).normalize().dot(forward)>.258819);
    }
    private static void choose(ServerPlayer p,Session s){
        Vec3 forward=p.getLookAngle();double range=searchRange(s.range);
        s.target=candidates(p,s,forward).stream().min(Comparator.comparingDouble(t->{
            double angle=1-t.getBoundingBox().getCenter().subtract(p.getEyePosition()).normalize().dot(forward);
            return angle*6+p.distanceTo(t)/range-(t instanceof Mob m&&m.getTarget()==p?.35:0);
        })).orElse(null);s.hiddenSince=Long.MIN_VALUE;
    }
    private static void switchTarget(ServerPlayer p,Session s,boolean left){
        if(s.target==null){choose(p,s);return;}
        float oldYaw=yawTo(p,s.target);
        Vec3 forward=s.target.position().subtract(p.position()).multiply(1,0,1).normalize();
        var selected=candidates(p,s,forward).stream().filter(t->t!=s.target)
            .filter(t->{float d=Mth.wrapDegrees(yawTo(p,t)-oldYaw);return left?d>.5:d<-.5;})
            .min(Comparator.comparingDouble(t->Math.abs(Mth.wrapDegrees(yawTo(p,t)-oldYaw)))).orElse(null);
        if(selected!=null){s.target=selected;s.hiddenSince=Long.MIN_VALUE;}
    }
    public static void receive(ServerPlayer p,CombatIntent i){
        if(!i.valid()){reset(p);return;}
        var s=p.getAttachedOrCreate(SESSION,Session::new);long now=p.level().getGameTime();s.input=now;
        boolean freshDefense=i.defend()&&!s.held;s.held=i.defend();if(!s.held)s.blocked=false;
        if(!compatible(p)){s.target=null;stopShield(p,s);publish(p,s);return;}
        if(i.operation()==CombatIntent.CLEAR){s.target=null;s.hiddenSince=Long.MIN_VALUE;}
        else if(i.operation()!=CombatIntent.KEEP&&s.command!=now){
            s.command=now;s.range=i.range();s.neutral=i.neutral();
            if(i.operation()==CombatIntent.TOGGLE&&s.target!=null){s.target=null;s.hiddenSince=Long.MIN_VALUE;}
            else if(i.operation()==CombatIntent.SELECT||i.operation()==CombatIntent.TOGGLE)choose(p,s);
            else switchTarget(p,s,i.operation()==CombatIntent.LEFT);
        }
        defense(p,s);if(freshDefense&&s.shield!=null)ParryActions.arm(p,s.shield);publish(p,s);
    }
    public static void tick(ServerPlayer p){
        var s=p.getAttached(SESSION);if(s==null)return;
        if(!compatible(p)||p.level().getGameTime()-s.input>INPUT_TIMEOUT){s.target=null;s.held=false;stopShield(p,s);publish(p,s);return;}
        if(s.target!=null){
            double range=searchRange(s.range)+4;
            if(s.target.level()!=p.level()||!permitted(p,s.target,s.neutral)||p.distanceToSqr(s.target)>range*range)s.target=null;
            else if(p.hasLineOfSight(s.target))s.hiddenSince=Long.MIN_VALUE;
            else if(s.hiddenSince==Long.MIN_VALUE)s.hiddenSince=p.level().getGameTime();
            else if(p.level().getGameTime()-s.hiddenSince>OCCLUSION_GRACE)s.target=null;
        }
        defense(p,s);publish(p,s);
    }
    public static InteractionHand shieldHand(Player p){
        for(var hand:new InteractionHand[]{InteractionHand.OFF_HAND,InteractionHand.MAIN_HAND}){
            var item=p.getItemInHand(hand);
            if(item.is(Items.SHIELD)&&!p.getCooldowns().isOnCooldown(item))return hand;
        }return null;
    }
    private static void defense(ServerPlayer p,Session s){
        if(s.shield!=null&&(p.getItemInHand(s.hand)!=s.shield||s.shield.isEmpty()||p.getCooldowns().isOnCooldown(s.shield)
                ||!p.isUsingItem()||p.getUseItem()!=s.shield)){
            stopShield(p,s);s.blocked=s.held;
        }
        if(!s.held||!compatible(p)){stopShield(p,s);return;}
        if(MeleeActions.blocksHands(p)){stopShield(p,s);return;}
        if(s.blocked||s.shield!=null||p.isUsingItem())return;
        var hand=shieldHand(p);if(hand==null)return;
        p.startUsingItem(hand);
        if(p.isUsingItem()&&p.getUseItem()==p.getItemInHand(hand)){s.hand=hand;s.shield=p.getUseItem();}
    }
    private static void stopShield(ServerPlayer p,Session s){
        ParryActions.close(p);
        if(s.shield!=null&&p.isUsingItem()&&p.getUsedItemHand()==s.hand&&p.getUseItem().is(Items.SHIELD))p.stopUsingItem();
        s.shield=null;s.hand=null;
    }
    private static void publish(ServerPlayer p,Session s){
        var v=new CombatView(s.target==null?-1:s.target.getId(),s.shield!=null&&p.isUsingItem()&&p.getUseItem()==s.shield);
        if(!v.equals(view(p)))p.setAttached(VIEW,v);
    }
    public static void releaseDefense(ServerPlayer p){var s=p.getAttached(SESSION);if(s!=null){stopShield(p,s);s.blocked=s.held;publish(p,s);}}
    public static void reset(ServerPlayer p){var s=p.getAttached(SESSION);if(s!=null)stopShield(p,s);p.removeAttached(SESSION);p.setAttached(VIEW,CombatView.IDLE);}
}
