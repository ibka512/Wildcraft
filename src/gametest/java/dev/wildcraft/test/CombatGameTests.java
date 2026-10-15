package dev.wildcraft.test;

import dev.wildcraft.combat.Combat;
import dev.wildcraft.equipment.BackEquipment;
import dev.wildcraft.fuse.*;
import dev.wildcraft.network.CombatIntent;
import dev.wildcraft.player.PlayerStamina;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;

public final class CombatGameTests {
    private static ServerPlayer player(GameTestHelper h,int height){
        var p=h.makeMockServerPlayerInLevel();p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        p.setPos(h.absoluteVec(new Vec3(1.5,height,1.5)));p.setYRot(0);p.setYHeadRot(0);p.setNoGravity(true);p.setOnGround(true);return p;
    }
    private static <T extends Mob> T mob(GameTestHelper h,net.minecraft.world.entity.EntityType<T> type,int height,int x,int z){
        var m=h.spawn(type,new BlockPos(x,height,z));m.setNoAi(true);m.setNoGravity(true);return m;
    }
    private static CombatIntent intent(byte op,boolean defense,boolean neutral){return new CombatIntent(op,defense,(byte)1,neutral);}
    @GameTest public void targetSelectionIgnoresPassiveAndInvalidCommands(GameTestHelper h){
        var p=player(h,24);var cow=mob(h,EntityTypes.COW,24,1,3);var enemy=mob(h,EntityTypes.HUSK,24,1,6);
        Combat.receive(p,intent(CombatIntent.SELECT,false,false));h.assertTrue(Combat.target(p)==enemy,"Center enemy wins; unrelated cow excluded");
        enemy.discard();Combat.tick(p);h.assertTrue(!Combat.locked(p),"Removed target clears immediately");
        Combat.reset(p);Combat.receive(p,intent(CombatIntent.SELECT,false,false));h.assertTrue(!Combat.locked(p),"Passive-only area does not lock");
        Combat.reset(p);Combat.receive(p,intent(CombatIntent.SELECT,false,true));h.assertTrue(Combat.target(p)==cow,"Explicit neutral preference permits a cow");
        cow.discard();Combat.reset(p);var neutralMonster=mob(h,EntityTypes.ENDERMAN,24,1,6);
        Combat.receive(p,intent(CombatIntent.SELECT,false,false));h.assertTrue(!Combat.locked(p),"Unprovoked neutral monster is not classified as a hostile target");
        // Actual aggression is verified with a real survival player in CombatClientTest;
        // native makeMockServerPlayerInLevel overrides gameMode() to CREATIVE.
        neutralMonster.discard();Combat.receive(p,new CombatIntent((byte)90,true,(byte)9,true));h.assertTrue(!Combat.locked(p)&&!Combat.view(p).defending(),"Invalid payload clears transient state");
        p.discard();cow.discard();h.succeed();
    }
    @GameTest public void movementUsesTargetWithoutChangingAimAndKeepsCollision(GameTestHelper h){
        var p=player(h,56);var enemy=mob(h,EntityTypes.HUSK,56,1,6);
        Combat.receive(p,intent(CombatIntent.SELECT,false,false));p.setYRot(90);p.setDeltaMovement(Vec3.ZERO);
        p.moveRelative(.1F,new Vec3(0,0,1));h.assertTrue(p.getDeltaMovement().z>.099&&Math.abs(p.getDeltaMovement().x)<.001&&p.getYRot()==90,"Forward reference faces enemy while aim stays independent");
        p.setDeltaMovement(Vec3.ZERO);p.moveRelative(.1F,new Vec3(1,0,0));h.assertTrue(p.getDeltaMovement().x>.099&&Math.abs(p.getDeltaMovement().z)<.001,"Native left strafe orbits target");
        p.setDeltaMovement(Vec3.ZERO);p.moveRelative(.1F,new Vec3(1,0,1));h.assertTrue(Math.abs(p.getDeltaMovement().length()-.1)<.001,"Native diagonal normalization is retained");
        for(int y=56;y<=59;y++)h.setBlock(1,y,2,net.minecraft.world.level.block.Blocks.STONE);
        double before=p.getZ();p.move(net.minecraft.world.entity.MoverType.SELF,new Vec3(0,0,1));h.assertTrue(p.getZ()-before<.25,"Native collision prevents crossing a wall");
        p.setDeltaMovement(Vec3.ZERO);p.setSprinting(true);p.jumpFromGround();h.assertTrue(p.getDeltaMovement().z>.19&&Math.abs(p.getDeltaMovement().x)<.001&&p.getYRot()==90,"Sprint jump boost keeps target reference without steering aim");
        Combat.reset(p);p.discard();enemy.discard();h.succeed();
    }
    @GameTest public void shieldKeepsNativeFrontBackWearFuseAndNoAddedFee(GameTestHelper h){
        var p=player(h,88);var enemy=mob(h,EntityTypes.HUSK,88,1,6);var back=mob(h,EntityTypes.HUSK,88,1,0);
        var shield=new ItemStack(Items.SHIELD);FusionItems.attach(shield,new ItemStack(Items.BLAZE_POWDER),h.getLevel().registryAccess());
        p.setItemInHand(InteractionHand.OFF_HAND,shield);double stamina=PlayerStamina.get(p).stamina();
        Combat.receive(p,intent(CombatIntent.SELECT,true,false));p.setYRot(90);p.setYHeadRot(90);
        h.assertTrue(p.getItemBlockingWith()==null,"Native block delay is not bypassed");
        h.runAfterDelay(8,()->{
            for(int n=0;n<8;n++)p.doTick();
            float health=p.getHealth();p.hurtServer(h.getLevel(),p.damageSources().mobAttack(enemy),6);
            h.assertTrue(p.getHealth()==health&&shield.getDamageValue()>0,"Facing locked enemy blocks with native shield durability");
            h.assertTrue(shield.get(FusionContent.DATA).remaining()==31&&enemy.isOnFire(),"One real full block consumes one fused effect");
            h.assertTrue(PlayerStamina.get(p).stamina()==stamina,"Ordinary defense adds no stamina fee");
            h.assertTrue(!BackEquipment.data(p).references().get(BackEquipment.SHIELD).signature().isEmpty(),"Actual independent shield use registers back ownership");
            p.setInvulnerableTime(0);p.hurtServer(h.getLevel(),p.damageSources().mobAttack(back),6);
            h.assertTrue(p.getHealth()<health,"Rear attack is not blocked by a front-facing shield");
            Combat.receive(p,intent(CombatIntent.CLEAR,false,false));h.assertTrue(!p.isUsingItem()&&!Combat.view(p).defending(),"Release ends only owned shield use");
            p.discard();enemy.discard();back.discard();h.succeed();
        });
    }
    @GameTest public void replacingShieldQuarantinesAndNeverStopsBow(GameTestHelper h){
        var p=player(h,120);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BOW));p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.SHIELD));
        Combat.receive(p,intent(CombatIntent.KEEP,true,false));h.assertTrue(Combat.view(p).defending(),"Separate action starts actual offhand shield");
        p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.SHIELD));p.doTick();Combat.tick(p);
        h.assertTrue(!p.isUsingItem()&&!Combat.view(p).defending(),"Identical spare is still a different owned item");
        Combat.receive(p,intent(CombatIntent.KEEP,true,false));h.assertTrue(!p.isUsingItem(),"Held defense does not restart after replacement");
        Combat.receive(p,intent(CombatIntent.KEEP,false,false));Combat.receive(p,intent(CombatIntent.KEEP,true,false));h.assertTrue(p.isUsingItem(),"Release and fresh hold can use replacement");
        p.stopUsingItem();p.startUsingItem(InteractionHand.MAIN_HAND);Combat.tick(p);Combat.receive(p,intent(CombatIntent.KEEP,false,false));
        h.assertTrue(p.isUsingItem()&&p.getUseItem().is(Items.BOW),"Canceling defense never cancels a different bow use");
        p.stopUsingItem();Combat.reset(p);p.discard();h.succeed();
    }
    @GameTest public void occlusionGraceUsesWorldTimeAndVisibilityResetsIt(GameTestHelper h){
        var p=player(h,152);var enemy=mob(h,EntityTypes.HUSK,152,1,6);
        Combat.receive(p,intent(CombatIntent.SELECT,false,false));
        for(int y=152;y<=155;y++)h.setBlock(1,y,3,net.minecraft.world.level.block.Blocks.STONE);
        Combat.tick(p);for(int n=0;n<30;n++)Combat.tick(p);
        h.assertTrue(Combat.locked(p),"Repeated calls in one world tick cannot burn the grace window");
        h.runAfterDelay(6,()->{
            Combat.tick(p);h.assertTrue(Combat.locked(p),"Brief complete occlusion preserves target");
            for(int y=152;y<=155;y++)h.setBlock(1,y,3,net.minecraft.world.level.block.Blocks.AIR);
            Combat.tick(p);h.assertTrue(Combat.locked(p),"Restored line of sight resets hidden timer");
            for(int y=152;y<=155;y++)h.setBlock(1,y,3,net.minecraft.world.level.block.Blocks.STONE);
            Combat.tick(p);
            h.runAfterDelay(12,()->{Combat.tick(p);h.assertTrue(!Combat.locked(p),"Long continuous occlusion releases target");Combat.reset(p);Combat.receive(p,intent(CombatIntent.SELECT,false,false));h.assertTrue(!Combat.locked(p),"Initial selection cannot see through a solid wall");p.discard();enemy.discard();h.succeed();});
        });
    }
    @GameTest(maxTicks=40) public void inputTimeoutReleasesNativeShieldAndSession(GameTestHelper h){
        var p=player(h,184);var enemy=mob(h,EntityTypes.HUSK,184,1,6);p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.SHIELD));
        Combat.receive(p,intent(CombatIntent.SELECT,true,false));h.assertTrue(Combat.locked(p)&&p.isUsingItem(),"Session begins with actual target and shield");
        h.runAfterDelay(22,()->{Combat.tick(p);h.assertTrue(!Combat.locked(p)&&!p.isUsingItem()&&!Combat.view(p).defending(),"Expired input releases gameplay and presentation");p.discard();enemy.discard();h.succeed();});
    }
}
