package dev.wildcraft.test;

import dev.wildcraft.combat.*;
import dev.wildcraft.network.*;
import dev.wildcraft.player.PlayerStamina;
import dev.wildcraft.fuse.*;
import dev.wildcraft.equipment.BackEquipment;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Real survival players: mock game mode silently has creative attributes in 26.3. */
public final class WeaponGameTests {
    private static ServerPlayer player(GameTestHelper h,int y,ItemStack item){
        var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"c6-test-player"),false);
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),cookie.gameProfile(),cookie.clientInformation());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);new io.netty.channel.embedded.EmbeddedChannel(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection,p,cookie);p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        p.setPos(h.absoluteVec(new Vec3(1.5,y,1.5)));p.setNoGravity(true);p.setOnGround(true);p.setYRot(0);p.setXRot(0);p.setYHeadRot(0);
        p.setItemInHand(InteractionHand.MAIN_HAND,item);PlayerStamina.fill(p);for(int n=0;n<45;n++)p.doTick();return p;
    }
    private static Husk enemy(GameTestHelper h,int y){var m=h.spawn(EntityTypes.HUSK,new net.minecraft.core.BlockPos(1,y,3));m.setNoAi(true);m.setNoGravity(true);m.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);m.setHealth(200);return m;}
    private static void send(ServerPlayer p,byte op){MeleeActions.receive(p,new MeleeIntent(op,(byte)0));}
    @GameTest public void concreteTypesAndCodecKeepSwordAxeAndNativeSpear(GameTestHelper h){
        h.assertTrue(MeleeWeapon.of(new ItemStack(Items.TRIDENT))==MeleeWeapon.TRIDENT&&MeleeWeapon.of(new ItemStack(Items.MACE))==MeleeWeapon.MACE,"Separate concrete types");
        for(var item:new Item[]{Items.STICK,Items.BOW})h.assertTrue(!MeleeActions.weapon(new ItemStack(item))&&!FusionItems.host(new ItemStack(item)),"Non-weapon is not owned: "+item);
        for(var kind:MeleeWeapon.values()){var v=new MeleeView(MeleeView.CHARGE,kind.id(),true,7,kind.threshold,(byte)0,20,7,0,0);var buf=io.netty.buffer.Unpooled.buffer();try{MeleeView.CODEC.encode(buf,v);h.assertTrue(v.equals(MeleeView.CODEC.decode(buf)),"Kind byte round trip");}finally{buf.release();}}
        h.assertTrue(new MeleeView(MeleeView.CHARGE,true,false,1,20,(byte)0,0,0,0,0).axe(),"Old axe source constructor retained");h.succeed();
    }
    @GameTest public void lightHitsConsumeNativeWearFuseOnceAndRecordOwnedStack(GameTestHelper h){
        int y=16;for(var item:new Item[]{Items.TRIDENT,Items.MACE}){
            var host=new ItemStack(item);h.assertTrue(FusionItems.attach(host,new ItemStack(Items.COBBLESTONE),h.getLevel().registryAccess()),"New real Fuse host");var p=player(h,y,host);var m=enemy(h,y);
            double stamina=PlayerStamina.get(p).stamina();send(p,MeleeIntent.BEGIN);h.assertTrue(m.getHealth()==200,"Press does not attack");send(p,MeleeIntent.RELEASE);
            float health=m.getHealth();h.assertTrue(health<200&&host.getDamageValue()==1&&host.get(FusionContent.DATA).remaining()==31,"One native ground hit/wear/Fuse");
            send(p,MeleeIntent.RELEASE);send(p,MeleeIntent.BEGIN);send(p,MeleeIntent.RELEASE);h.assertTrue(m.getHealth()==health&&host.getDamageValue()==1&&host.get(FusionContent.DATA).remaining()==31&&PlayerStamina.get(p).stamina()==stamina,"Replay/recovery has no extra result or fee");
            p.getInventory().setItem(1,new ItemStack(Items.STICK));p.getInventory().setSelectedSlot(1);BackEquipment.tick(p);
            h.assertTrue(BackEquipment.view(p).melee().is(item)&&BackEquipment.view(p).melee().get(FusionContent.VIEW).uses()==31,"Actual-use record retains sanitized original fused native model");
            p.discard();m.discard();y+=24;
        }h.succeed();
    }
    @GameTest(maxTicks=40) public void longAndHeavyStrongUseDistinctThresholdAndExactFee(GameTestHelper h){
        var trident=new ItemStack(Items.TRIDENT);var mace=new ItemStack(Items.MACE);var p=player(h,72,trident);var q=player(h,96,mace);var a=enemy(h,72);var b=enemy(h,96);
        send(p,MeleeIntent.BEGIN);send(q,MeleeIntent.BEGIN);
        h.runAfterDelay(10,()->{send(p,MeleeIntent.KEEP);send(q,MeleeIntent.KEEP);});
        h.runAfterDelay(16,()->{double stamina=PlayerStamina.get(p).stamina();send(p,MeleeIntent.RELEASE);h.assertTrue(MeleeActions.view(p).kind()==MeleeWeapon.TRIDENT&&MeleeActions.view(p).charged()&&MeleeActions.view(p).duration()==12&&PlayerStamina.get(p).stamina()==stamina-14,"Trident 16-tick charge, 14 energy, 12 recovery");h.assertTrue(200-a.getHealth()>11&&trident.getDamageValue()==1,"Native boosted trident hits once");});
        h.runAfterDelay(20,()->send(q,MeleeIntent.KEEP));
        h.runAfterDelay(26,()->{double stamina=PlayerStamina.get(q).stamina();send(q,MeleeIntent.RELEASE);h.assertTrue(MeleeActions.view(q).kind()==MeleeWeapon.MACE&&MeleeActions.view(q).charged()&&MeleeActions.view(q).duration()==20&&PlayerStamina.get(q).stamina()==stamina-22,"Mace own 26/22/20 contract");h.assertTrue(200-b.getHealth()>8&&mace.getDamageValue()==1,"Ground heavy native hit once");p.discard();q.discard();a.discard();b.discard();h.succeed();});
    }
    @GameTest public void wallAimRangeAndChangedWeaponCannotHit(GameTestHelper h){
        int y=128;for(var item:new Item[]{Items.TRIDENT,Items.MACE}){
            var p=player(h,y,new ItemStack(item));var m=enemy(h,y);
            for(int n=y;n<y+4;n++)h.setBlock(1,n,2,Blocks.STONE);send(p,MeleeIntent.BEGIN);send(p,MeleeIntent.RELEASE);h.assertTrue(m.getHealth()==200,"Blocked new weapon never hits");
            for(int n=y;n<y+4;n++)h.setBlock(1,n,2,Blocks.AIR);MeleeActions.reset(p);p.setYRot(90);send(p,MeleeIntent.BEGIN);send(p,MeleeIntent.RELEASE);h.assertTrue(m.getHealth()==200,"No automatic nearby aim");
            p.setYRot(0);MeleeActions.reset(p);send(p,MeleeIntent.BEGIN);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item));send(p,MeleeIntent.RELEASE);h.assertTrue(m.getHealth()==200&&!MeleeActions.blocksHands(p),"Same-looking replacement cancels");
            m.setPos(m.position().add(0,0,5));send(p,MeleeIntent.BEGIN);send(p,MeleeIntent.RELEASE);h.assertTrue(m.getHealth()==200,"No reach extension");p.discard();m.discard();y+=24;
        }h.succeed();
    }
    @GameTest(maxTicks=40) public void nativeSmashPreservesBonusFallResetAndFuseWithoutNewCharge(GameTestHelper h){
        var host=new ItemStack(Items.MACE);FusionItems.attach(host,new ItemStack(Items.COBBLESTONE),h.getLevel().registryAccess());var p=player(h,176,host);var m=enemy(h,176);send(p,MeleeIntent.BEGIN);
        h.runAfterDelay(10,()->send(p,MeleeIntent.KEEP));h.runAfterDelay(20,()->send(p,MeleeIntent.KEEP));
        h.runAfterDelay(26,()->{p.setOnGround(false);p.fallDistance=8;p.setDeltaMovement(0,-.3,0);double stamina=PlayerStamina.get(p).stamina();send(p,MeleeIntent.RELEASE);
            h.assertTrue(200-m.getHealth()>25,"Original mace fall bonus retained");h.assertTrue(p.fallDistance==0&&host.getDamageValue()==1&&host.get(FusionContent.DATA).remaining()==31,"Native successful smash resets fall, one durability/Fuse despite distinct damage source");h.assertTrue(!MeleeActions.view(p).charged()&&PlayerStamina.get(p).stamina()==stamina&&MeleeActions.damageScale(p)==1&&MeleeActions.knockback(p)==0,"Smash pays no useless Wildcraft fee and has no stacked bonus");
            p.getInventory().setItem(1,new ItemStack(Items.STICK));p.getInventory().setSelectedSlot(1);BackEquipment.tick(p);h.assertTrue(BackEquipment.view(p).melee().is(Items.MACE),"Smash actual use records the heavy weapon");p.discard();m.discard();h.succeed();});
    }
    @GameTest public void originalTridentThrowAndNewCounterUseNativePath(GameTestHelper h){
        var trident=new ItemStack(Items.TRIDENT);FusionItems.attach(trident,new ItemStack(Items.COBBLESTONE),h.getLevel().registryAccess());var p=player(h,208,trident);var m=enemy(h,208);
        MeleeActions.counterSwing(p,m);float health=m.getHealth();h.assertTrue(health<200&&trident.getDamageValue()==1&&trident.get(FusionContent.DATA).remaining()==31&&!MeleeActions.view(p).charged(),"C5 counter reuses native trident once without strong multiplier");
        MeleeActions.reset(p);p.startUsingItem(InteractionHand.MAIN_HAND);send(p,MeleeIntent.BEGIN);h.assertTrue(!MeleeActions.blocksHands(p),"Original use excludes new charge");
        ((TridentItem)Items.TRIDENT).releaseUsing(trident,h.getLevel(),p,72000-10);
        var thrown=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.projectile.arrow.ThrownTrident.class,p.getBoundingBox().inflate(3));
        h.assertTrue(trident.isEmpty()&&thrown.size()==1,"Original native throw consumes real hand and produces exactly one projectile");
        h.assertTrue(thrown.getFirst().getPickupItemStackOrigin().get(FusionContent.DATA).remaining()==31,"Original components survive throw without invented projectile Fuse consumption");p.discard();m.discard();thrown.forEach(Entity::discard);h.succeed();
    }
}
