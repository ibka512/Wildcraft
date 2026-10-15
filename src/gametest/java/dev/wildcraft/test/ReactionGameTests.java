package dev.wildcraft.test;
import dev.wildcraft.combat.*;
import dev.wildcraft.focus.*;
import dev.wildcraft.network.*;
import dev.wildcraft.player.PlayerStamina;
import dev.wildcraft.fuse.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;

public final class ReactionGameTests {
    private static ServerPlayer player(GameTestHelper h,int y){var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"c5-test-player"),false);var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),cookie.gameProfile(),cookie.clientInformation());var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);new io.netty.channel.embedded.EmbeddedChannel(connection);h.getLevel().getServer().getPlayerList().placeNewPlayer(connection,p,cookie);p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());p.setPos(h.absoluteVec(new Vec3(1.5,y,1.5)));p.setNoGravity(true);p.setOnGround(true);p.setYRot(0);p.setYHeadRot(0);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_SWORD));PlayerStamina.fill(p);for(int n=0;n<25;n++)p.doTick();return p;}
    private static Zombie enemy(GameTestHelper h,int y){var m=h.spawn(EntityTypes.HUSK,new BlockPos(1,y,3));m.setNoAi(true);m.setNoGravity(true);m.setPos(h.absoluteVec(new Vec3(1.5,y,2.8)));return m;}
    private static class Goal extends ZombieAttackGoal{Goal(Zombie z){super(z,1,true);}void attempt(LivingEntity t){checkAndPerformAttack(t);}}
    private static void hop(ServerPlayer p){MeleeActions.receive(p,new MeleeIntent(MeleeIntent.DODGE,(byte)-2));}
    private static void move(ServerPlayer p,double dz){var v=p.position().add(0,0,dz);if(MeleeActions.acceptMovement(p,v.x,v.y,v.z))p.setPos(v);}
    @GameTest(maxTicks=25) public void imminentAttackMissRequiresVerifiedDisplacement(GameTestHelper h){
        var p=player(h,16);var m=enemy(h,16);m.setTarget(p);var g=new Goal(m);g.attempt(p);
        h.runAfterDelay(6,()->{hop(p);h.assertTrue(MeleeActions.hopping(p)&&PlayerStamina.get(p).stamina()==92,"Accepted hop pays exactly eight");move(p,-.6);move(p,-.6);});
        h.runAfterDelay(10,()->{g.attempt(p);h.assertTrue(p.getHealth()==20&&ThreatActions.view(m).phase()==ThreatView.SWING,"Actual out-of-range miss settles a full windup");h.assertTrue(!ReactionTime.active(p)&&h.getLevel().getServer().tickRateManager().tickrate()==20,"Dedicated arena never slows a shared world");p.discard();m.discard();h.succeed();});
    }
    @GameTest(maxTicks=25) public void stoppedHopRetainsNativeHitWithoutInvulnerability(GameTestHelper h){
        var p=player(h,40);var m=enemy(h,40);m.setTarget(p);var g=new Goal(m);g.attempt(p);
        h.runAfterDelay(6,()->hop(p));h.runAfterDelay(10,()->{g.attempt(p);h.assertTrue(p.getHealth()<20&&!ReactionTime.active(p),"A stationary / wall-stopped hop still takes the native attack");p.discard();m.discard();h.succeed();});
    }
    @GameTest(maxTicks=25) public void earlyHopAndWalkingCannotReservePerfectReward(GameTestHelper h){
        var p=player(h,64);var m=enemy(h,64);m.setTarget(p);var g=new Goal(m);g.attempt(p);hop(p);move(p,-.6);g.attempt(p);
        h.assertTrue(ThreatActions.view(m).phase()==0&&!ReactionTime.active(p),"Before final four ticks leaving range cancels normally without reward");p.discard();m.discard();h.succeed();
    }
    @GameTest(maxTicks=25) public void invalidHopMovementRejectsAndCannotClaimEpisode(GameTestHelper h){
        var p=player(h,88);var origin=p.position();hop(p);
        h.assertTrue(!MeleeActions.acceptMovement(p,p.getX(),p.getY(),p.getZ()-2)&&!MeleeActions.hopping(p),"Oversized movement is rejected and hop is cancelled");
        h.assertTrue(!MeleeActions.claimHop(p,origin),"Rejected packet cannot become a perfect escape");h.assertTrue(PlayerStamina.get(p).stamina()==92,"Rejected motion grants no fee refund or repeated fee");p.discard();h.succeed();
    }
    @GameTest public void noThreatOrFakeCounterCannotAttackOrSlowWorld(GameTestHelper h){
        var p=player(h,112);var m=enemy(h,112);float health=m.getHealth();MeleeActions.receive(p,new MeleeIntent(MeleeIntent.RELEASE,(byte)0));SkillPreferences.receive(p,new SkillIntent(SkillIntent.CONFIRM,(byte)0,(byte)0));
        h.assertTrue(m.getHealth()==health&&!ReactionTime.active(p)&&p.getMainHandItem().getDamageValue()==0,"Counter request has no authority without an earned episode");hop(p);h.assertTrue(!ReactionTime.active(p),"Ordinary hop by a nearby enemy grants no episode");p.discard();m.discard();h.succeed();
    }
    @GameTest public void airborneAndDownwardNativeHitKeepOneWearAndFuse(GameTestHelper h){
        var p=player(h,144);var m=enemy(h,144);FusionItems.attach(p.getMainHandItem(),new ItemStack(Items.COBBLESTONE),h.getLevel().registryAccess());p.setOnGround(false);p.fallDistance=100;p.setDeltaMovement(0,-.5,0);
        h.assertTrue(MeleeActions.dropMultiplier(p)==1.5F,"Arbitrarily high fall is capped at 1.5 modifier");MeleeActions.receive(p,new MeleeIntent(MeleeIntent.BEGIN,(byte)0));MeleeActions.receive(p,new MeleeIntent(MeleeIntent.RELEASE,(byte)0));
        h.assertTrue(MeleeActions.view(p).phase()==MeleeView.DROP_SWING&&m.getHealth()<20&&p.getMainHandItem().getDamageValue()==1&&p.getMainHandItem().get(FusionContent.DATA).remaining()==31,"Actual drop swing reaches native damage/wear/Fuse exactly once");h.assertTrue(PlayerStamina.get(p).stamina()==100&&p.fallDistance==100,"Uncharged airborne hit is free and does not erase fall damage distance");p.discard();m.discard();h.succeed();
    }
    @GameTest public void airborneModifierNeedsActualDescentAndGroundResetsIt(GameTestHelper h){var p=player(h,176);p.fallDistance=20;p.setOnGround(false);p.setDeltaMovement(0,.2,0);h.assertTrue(MeleeActions.dropMultiplier(p)==1,"Rising motion gets no drop boost");p.setDeltaMovement(0,-.2,0);p.fallDistance=1.9;h.assertTrue(MeleeActions.dropMultiplier(p)==1,"Short fall gets no high-drop bonus");p.setOnGround(true);p.fallDistance=100;h.assertTrue(MeleeActions.dropMultiplier(p)==1,"Ground attacks cannot reuse old fall height");p.discard();h.succeed();}
    @GameTest public void preferencesAreValidatedAndManualBowRequiresConfirmation(GameTestHelper h){var p=player(h,208);SkillPreferences.receive(p,new SkillIntent(SkillIntent.PREFERENCES,(byte)2,(byte)1));h.assertTrue(SkillPreferences.bow(p)==2&&SkillPreferences.dodge(p)==1,"Independent valid preferences recorded");SkillPreferences.receive(p,new SkillIntent(SkillIntent.PREFERENCES,(byte)99,(byte)-1));h.assertTrue(SkillPreferences.bow(p)==2&&SkillPreferences.dodge(p)==1,"Invalid preference does not rewrite previous values");p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BOW));p.startUsingItem(InteractionHand.MAIN_HAND);p.setOnGround(false);h.assertTrue(!FocusTime.tryBegin(p)&&!ReactionTime.confirm(p),"Disabled bow and unearned manual counter remain inactive");p.discard();h.succeed();}
}
