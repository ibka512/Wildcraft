package dev.wildcraft.test;
import dev.wildcraft.combat.*;
import dev.wildcraft.network.*;
import dev.wildcraft.player.*;
import dev.wildcraft.fuse.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.Blocks;
public final class MeleeGameTests {
    private static ServerPlayer player(GameTestHelper h,int y,ItemStack item){
        var p=h.makeMockServerPlayerInLevel();p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        p.setPos(h.absoluteVec(new Vec3(1.5,y,1.5)));p.setNoGravity(true);p.setItemInHand(InteractionHand.MAIN_HAND,item);
        for(int n=0;n<25;n++)p.doTick();p.setYRot(0);p.setXRot(0);p.setOnGround(true);PlayerStamina.fill(p);return p;
    }
    private static net.minecraft.world.entity.monster.zombie.Husk enemy(GameTestHelper h,int y,int z){var m=h.spawn(EntityTypes.HUSK,new BlockPos(1,y,z));m.setNoAi(true);m.setNoGravity(true);return m;}
    private static void send(ServerPlayer p,byte op){MeleeActions.receive(p,new MeleeIntent(op,(byte)0));}
    @GameTest public void tapResolvesNativeOnceAndSpendsNoStamina(GameTestHelper h){
        var sword=new ItemStack(Items.IRON_SWORD);FusionItems.attach(sword,new ItemStack(Items.COBBLESTONE),h.getLevel().registryAccess());var p=player(h,16,sword);var m=enemy(h,16,3);
        double stamina=PlayerStamina.get(p).stamina();float health=m.getHealth();send(p,MeleeIntent.BEGIN);h.assertTrue(m.getHealth()==health,"Press alone never attacks");
        send(p,MeleeIntent.RELEASE);float after=m.getHealth();int wear=sword.getDamageValue();h.assertTrue(after<health&&wear==1&&sword.get(FusionContent.DATA).remaining()==31,"One native light hit, one durability and Fuse use");
        send(p,MeleeIntent.RELEASE);send(p,MeleeIntent.BEGIN);send(p,MeleeIntent.RELEASE);h.assertTrue(m.getHealth()==after&&sword.getDamageValue()==wear&&sword.get(FusionContent.DATA).remaining()==31,"Release replay and recovery spam cannot duplicate results");
        h.assertTrue(PlayerStamina.get(p).stamina()==stamina,"Normal attack has no extra stamina cost");p.discard();m.discard();h.succeed();
    }
    @GameTest(maxTicks=35) public void chargedSwordUsesNativeDamageAndExactCost(GameTestHelper h){
        var sword=new ItemStack(Items.IRON_SWORD);FusionItems.attach(sword,new ItemStack(Items.COBBLESTONE),h.getLevel().registryAccess());var p=player(h,40,sword);var m=enemy(h,40,3);var side=h.spawn(EntityTypes.HUSK,new BlockPos(2,40,3));side.setNoAi(true);side.setNoGravity(true);float before=m.getHealth();send(p,MeleeIntent.BEGIN);
        h.runAfterDelay(12,()->{double stamina=PlayerStamina.get(p).stamina();send(p,MeleeIntent.RELEASE);h.assertTrue(before-m.getHealth()>8,"Paid sword strike increases native damage");h.assertTrue(PlayerStamina.get(p).stamina()==stamina-12,"One sword fee");h.assertTrue(sword.getDamageValue()==1&&MeleeActions.view(p).charged(),"One native durability result and authoritative charged state");h.assertTrue(side.getHealth()<20&&sword.get(FusionContent.DATA).remaining()==30,"Native sweep strikes another target once; each real damaged target consumes one Fuse use");send(p,MeleeIntent.RELEASE);h.assertTrue(PlayerStamina.get(p).stamina()==stamina-12,"Replay adds no fee");p.discard();m.discard();side.discard();h.succeed();});
    }
    @GameTest(maxTicks=35) public void axeChargeAndInsufficientStamina(GameTestHelper h){
        var axe=new ItemStack(Items.IRON_AXE);var p=player(h,64,axe);var m=enemy(h,64,3);send(p,MeleeIntent.BEGIN);
        h.runAfterDelay(10,()->send(p,MeleeIntent.KEEP));
        h.runAfterDelay(20,()->{p.setAttached(PlayerStamina.DATA,new StaminaData(1,0,17,20));float health=m.getHealth();send(p,MeleeIntent.RELEASE);h.assertTrue(m.getHealth()==health&&axe.getDamageValue()==0&&PlayerStamina.get(p).stamina()==17,"Insufficient axe fee never becomes a free strong hit");PlayerStamina.fill(p);send(p,MeleeIntent.BEGIN);send(p,MeleeIntent.CANCEL);send(p,MeleeIntent.RELEASE);h.assertTrue(m.getHealth()==health,"Cancel never attacks");p.discard();m.discard();h.succeed();});
    }
    @GameTest public void rayRejectsWallRangeAndFreeAimAutoHit(GameTestHelper h){
        var p=player(h,80,new ItemStack(Items.IRON_SWORD));var near=enemy(h,80,3);h.assertTrue(MeleeActions.pointed(p)==near,"Native reach identifies real aim");
        for(int y=80;y<84;y++)h.setBlock(1,y,2,Blocks.STONE);send(p,MeleeIntent.BEGIN);send(p,MeleeIntent.RELEASE);h.assertTrue(near.getHealth()==20,"Wall prevents native skill damage");
        for(int y=80;y<84;y++)h.setBlock(1,y,2,Blocks.AIR);MeleeActions.reset(p);p.setYRot(90);send(p,MeleeIntent.BEGIN);send(p,MeleeIntent.RELEASE);h.assertTrue(near.getHealth()==20,"A nearby or locked target is not automatically hit");
        p.setYRot(0);near.discard();var far=enemy(h,80,6);MeleeActions.reset(p);send(p,MeleeIntent.BEGIN);send(p,MeleeIntent.RELEASE);h.assertTrue(far.getHealth()==20,"Native reach cannot be extended by intents");p.discard();far.discard();h.succeed();
    }
    @GameTest(maxTicks=35) public void replacementTimeoutAndInvalidPayloadCancel(GameTestHelper h){
        var p=player(h,104,new ItemStack(Items.IRON_SWORD));var m=enemy(h,104,3);send(p,MeleeIntent.BEGIN);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_SWORD));send(p,MeleeIntent.RELEASE);h.assertTrue(m.getHealth()==20&&!MeleeActions.blocksHands(p),"Identical spare is a different charging weapon");
        send(p,MeleeIntent.BEGIN);MeleeActions.receive(p,new MeleeIntent((byte)90,(byte)0));h.assertTrue(!MeleeActions.blocksHands(p),"Invalid operation resets active phase");send(p,MeleeIntent.BEGIN);
        h.runAfterDelay(22,()->{MeleeActions.tick(p);send(p,MeleeIntent.RELEASE);h.assertTrue(!MeleeActions.blocksHands(p)&&m.getHealth()==20,"Expired heartbeat cannot release an old charge");p.discard();m.discard();h.succeed();});
    }
    @GameTest public void dodgeDirectionFeesAndMovementBounds(GameTestHelper h){
        var p=player(h,136,new ItemStack(Items.IRON_SWORD));double stamina=PlayerStamina.get(p).stamina();MeleeActions.receive(p,new MeleeIntent(MeleeIntent.DODGE,(byte)1));h.assertTrue(MeleeActions.hopping(p)&&PlayerStamina.get(p).stamina()==stamina-8&&p.getDeltaMovement().x>.31,"Left hop uses native direction and exact fee");MeleeActions.receive(p,new MeleeIntent(MeleeIntent.DODGE,(byte)-1));h.assertTrue(PlayerStamina.get(p).stamina()==stamina-8,"Repeated hop cannot charge twice");
        h.assertTrue(!MeleeActions.acceptMovement(p,p.getX()+4,p.getY(),p.getZ())&&!MeleeActions.hopping(p),"Invalid oversized movement cancels and corrects");MeleeActions.receive(p,new MeleeIntent(MeleeIntent.DODGE,(byte)-2));h.assertTrue(!MeleeActions.hopping(p)&&PlayerStamina.get(p).stamina()==stamina-8,"Cancellation does not erase hop interval");
        MeleeActions.reset(p);p.setAttached(PlayerStamina.DATA,new StaminaData(1,0,7,20));MeleeActions.receive(p,new MeleeIntent(MeleeIntent.DODGE,(byte)-2));h.assertTrue(!MeleeActions.hopping(p)&&PlayerStamina.get(p).stamina()==7,"Insufficient energy gives no movement");MeleeActions.reset(p);PlayerStamina.fill(p);p.setOnGround(false);MeleeActions.receive(p,new MeleeIntent(MeleeIntent.DODGE,(byte)-2));h.assertTrue(!MeleeActions.hopping(p),"No repeated airborne hop");p.discard();h.succeed();
    }
}
