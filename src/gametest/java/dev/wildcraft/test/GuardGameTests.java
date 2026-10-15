package dev.wildcraft.test;
import dev.wildcraft.combat.*;
import dev.wildcraft.network.*;
import dev.wildcraft.fuse.*;
import dev.wildcraft.player.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.level.block.Blocks;
public final class GuardGameTests {
    private static ServerPlayer player(GameTestHelper h,int y){var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"c4-test-player"),false);var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),cookie.gameProfile(),cookie.clientInformation());var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);new io.netty.channel.embedded.EmbeddedChannel(connection);h.getLevel().getServer().getPlayerList().placeNewPlayer(connection,p,cookie);p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());p.setPos(h.absoluteVec(new Vec3(1.5,y,1.5)));p.setNoGravity(true);p.setOnGround(true);p.setYRot(0);p.setYHeadRot(0);p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.SHIELD));PlayerStamina.fill(p);return p;}
    private static Zombie enemy(GameTestHelper h,int y){var m=h.spawn(EntityTypes.HUSK,new BlockPos(1,y,3));m.setNoAi(true);m.setNoGravity(true);m.setPos(h.absoluteVec(new Vec3(1.5,y,2.8)));return m;}
    private static void defend(ServerPlayer p,boolean held){Combat.receive(p,new CombatIntent(CombatIntent.KEEP,held,(byte)1,false));}
    private static class Goal extends ZombieAttackGoal{Goal(Zombie z){super(z,1,true);}void attempt(LivingEntity t){checkAndPerformAttack(t);}}
    private static class TestArrow extends Arrow{byte pierce;@Override public byte getPierceLevel(){return pierce;}TestArrow(LivingEntity owner){super(owner.level(),owner,new ItemStack(Items.ARROW),null);}void impact(Entity e){onHitEntity(new EntityHitResult(e));}}
    @GameTest(maxTicks=30) public void nativeGoalPreparesAndHitsOnce(GameTestHelper h){
        var p=player(h,16);var m=enemy(h,16);m.setTarget(p);var g=new Goal(m);h.assertTrue(m.isWithinMeleeAttackRange(p),"Fixture starts inside native melee reach");g.attempt(p);for(int n=0;n<20;n++)g.attempt(p);
        h.assertTrue(p.getHealth()==20&&ThreatActions.view(m).phase()==ThreatView.WINDUP,"Native goal has no instant or repeated-call hit");
        h.runAfterDelay(9,()->{g.attempt(p);h.assertTrue(p.getHealth()==20,"Nine world ticks are still preparation");});
        h.runAfterDelay(10,()->{g.attempt(p);h.assertTrue(p.getHealth()<20&&ThreatActions.view(m).phase()==ThreatView.SWING,"Ten world ticks produce the actual single native attack");float health=p.getHealth();g.attempt(p);h.assertTrue(p.getHealth()==health,"Same attack cannot hit again");p.discard();m.discard();h.succeed();});
    }
    @GameTest public void wallRangeAndChangedTargetCancelPreparation(GameTestHelper h){
        var p=player(h,40);var m=enemy(h,40);m.setTarget(p);var g=new Goal(m);g.attempt(p);
        for(int y=40;y<=43;y++)h.setBlock(1,y,2,Blocks.STONE);g.attempt(p);h.assertTrue(ThreatActions.view(m).phase()==0&&p.getHealth()==20,"Wall cancels before native damage");
        for(int y=40;y<=43;y++)h.setBlock(1,y,2,Blocks.AIR);g.attempt(p);h.assertTrue(ThreatActions.view(m).phase()==ThreatView.WINDUP&&ThreatActions.view(m).age()==0,"Restored line of sight starts a complete new windup");
        p.setPos(p.position().add(0,0,-8));g.attempt(p);h.assertTrue(ThreatActions.view(m).phase()==0,"Leaving native range cancels");
        p.setPos(h.absoluteVec(new Vec3(1.5,40,1.5)));g.attempt(p);var other=h.spawn(EntityTypes.COW,new BlockPos(1,40,1));other.setNoAi(true);m.setTarget(other);g.attempt(other);h.assertTrue(ThreatActions.view(m).phase()==0,"Switching target cannot inherit an existing windup");
        p.discard();m.discard();other.discard();h.succeed();
    }
    @GameTest public void freshFrontShieldHasOneNativeWearFuseAndReward(GameTestHelper h){
        var p=player(h,64);var m=enemy(h,64);var shield=p.getOffhandItem();FusionItems.attach(shield,new ItemStack(Items.COBBLESTONE),h.getLevel().registryAccess());defend(p,true);double stamina=PlayerStamina.get(p).stamina();
        var source=p.damageSources().mobAttack(m);float amount=p.applyItemBlocking(h.getLevel(),source,4);int wear=shield.get(DataComponents.BLOCKS_ATTACKS).itemDamage().apply(4);
        h.assertTrue(amount==4&&shield.getDamageValue()==wear&&shield.get(FusionContent.DATA).remaining()==31&&ParryActions.view(p).serial()==1&&ThreatActions.staggered(m),"Instant full native block settles wear, Fuse and one perfect result");
        for(int n=0;n<6;n++)p.doTick();defend(p,true);p.applyItemBlocking(h.getLevel(),source,4);
        h.assertTrue(ParryActions.view(p).serial()==1&&shield.getDamageValue()==2*wear&&shield.get(FusionContent.DATA).remaining()==30,"Later ordinary block still works but KEEP cannot award twice");h.assertTrue(PlayerStamina.get(p).stamina()==stamina,"Defense has no extra fee or refill");p.discard();m.discard();h.succeed();
    }
    @GameTest public void backAndLargeExplosiveSourcesNeverGainPerfectBlock(GameTestHelper h){
        var p=player(h,88);var m=enemy(h,88);defend(p,true);p.setYHeadRot(180);p.setYRot(180);var shield=p.getOffhandItem();
        h.assertTrue(p.applyItemBlocking(h.getLevel(),p.damageSources().mobAttack(m),4)==0&&ParryActions.view(p).serial()==0&&shield.getDamageValue()==0,"Fresh defense cannot block from the back");
        p.setYHeadRot(0);p.setYRot(0);var giant=h.spawn(EntityTypes.IRON_GOLEM,new BlockPos(1,88,3));giant.setNoAi(true);
        h.assertTrue(p.applyItemBlocking(h.getLevel(),p.damageSources().mobAttack(giant),4)==0,"Large source keeps native delay, not an instant parry");
        h.assertTrue(p.applyItemBlocking(h.getLevel(),p.damageSources().explosion(m,m),4)==0&&ParryActions.view(p).serial()==0,"Explosion never gains perfect reward");
        p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.SHIELD));Combat.tick(p);h.assertTrue(!p.isUsingItem(),"Identical replacement closes real owned defense");p.discard();m.discard();giant.discard();h.succeed();
    }
    @GameTest(maxTicks=30) public void windowTimeoutAndFreshRearmUseWorldTicks(GameTestHelper h){
        var p=player(h,112);var m=enemy(h,112);defend(p,true);
        h.runAfterDelay(6,()->{defend(p,true);h.assertTrue(p.applyItemBlocking(h.getLevel(),p.damageSources().mobAttack(m),4)==0,"Six ticks and repeated held command cannot renew the instant window");defend(p,false);defend(p,true);h.assertTrue(p.applyItemBlocking(h.getLevel(),p.damageSources().mobAttack(m),4)==0,"Early release/repress is rate limited");});
        h.runAfterDelay(12,()->{defend(p,false);defend(p,true);h.assertTrue(p.applyItemBlocking(h.getLevel(),p.damageSources().mobAttack(m),4)==4&&ParryActions.view(p).serial()==1,"A genuinely fresh press after cooldown can parry");});
        h.runAfterDelay(25,()->{ThreatActions.tick(m);h.assertTrue(!ThreatActions.staggered(m)&&ThreatActions.view(m).phase()==0,"Light stagger expires and releases native AI");p.discard();m.discard();h.succeed();});
    }
    @GameTest public void arrowNativeCollisionReflectsSameEntityWithoutAutoAim(GameTestHelper h){
        var p=player(h,144);var shooter=h.spawn(EntityTypes.SKELETON,new BlockPos(1,144,6));shooter.setNoAi(true);var arrow=new TestArrow(shooter);arrow.setPos(p.position().add(0,1,1));arrow.setDeltaMovement(.2,0,-1.4);defend(p,true);int initialId=arrow.getId();arrow.impact(p);
        h.assertTrue(!arrow.isRemoved()&&arrow.getId()==initialId&&arrow.getOwner()==p&&p.getHealth()==20&&ParryActions.view(p).serial()==1,"Native collision blocks and reflects the existing arrow once");h.assertTrue(arrow.getDeltaMovement().z>1.3&&Math.abs(arrow.getDeltaMovement().x-.2)<.001,"Reflection follows shield surface and preserves lateral direction, not target aim");
        float before=shooter.getHealth();arrow.impact(shooter);h.assertTrue(shooter.getHealth()<before&&arrow.isRemoved(),"Reflected arrow still uses one native later hit/discard");p.discard();shooter.discard();h.succeed();
    }
    @GameTest public void piercingFirePotionFuseAndPlayerArrowsKeepNativeRules(GameTestHelper h){
        var p=player(h,176);var shooter=h.spawn(EntityTypes.SKELETON,new BlockPos(1,176,6));shooter.setNoAi(true);var arrow=new TestArrow(shooter);arrow.pierce=1;h.assertTrue(!ParryActions.ordinaryArrow(arrow),"Piercing arrows excluded");arrow.pierce=0;arrow.igniteForSeconds(2);h.assertTrue(!ParryActions.ordinaryArrow(arrow),"Fire arrows excluded");arrow.clearFire();arrow.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOWNESS,40));h.assertTrue(!ParryActions.ordinaryArrow(arrow),"Potion effect on a base arrow is still excluded");
        var fused=new TestArrow(shooter);FusionItems.attach(fused.getPickupItemStackOrigin(),new ItemStack(Items.COBBLESTONE),h.getLevel().registryAccess());h.assertTrue(!ParryActions.ordinaryArrow(fused),"Fuse projectile retains its own rules");var pvp=new TestArrow(p);h.assertTrue(!ParryActions.ordinaryArrow(pvp),"Player arrows excluded");
        defend(p,true);h.assertTrue(p.applyItemBlocking(h.getLevel(),p.damageSources().arrow(arrow,shooter),4)==0&&ParryActions.view(p).serial()==0,"Excluded actual arrow source does not bypass native shield delay");p.discard();shooter.discard();h.succeed();
    }
    @GameTest(maxTicks=35) public void nativeChargedHitCausesBoundedEnemyStagger(GameTestHelper h){
        var p=player(h,208);var m=enemy(h,208);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_SWORD));for(int n=0;n<25;n++)p.doTick();MeleeActions.receive(p,new MeleeIntent(MeleeIntent.BEGIN,(byte)0));
        h.runAfterDelay(12,()->{MeleeActions.receive(p,new MeleeIntent(MeleeIntent.RELEASE,(byte)0));h.assertTrue(m.getHealth()<20&&ThreatActions.staggered(m)&&ThreatActions.view(m).duration()==8,"Real C3 paid charge produces eight-tick enemy stagger");});
        h.runAfterDelay(21,()->{ThreatActions.tick(m);h.assertTrue(!ThreatActions.staggered(m),"No permanent AI/control lock");p.discard();m.discard();h.succeed();});
    }
}
