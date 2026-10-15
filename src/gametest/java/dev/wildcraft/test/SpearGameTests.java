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
public final class SpearGameTests {
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
    private static Husk target(GameTestHelper h,int y,double x,double z){var m=enemy(h,y);m.setPos(h.absoluteVec(new Vec3(x,y,z)));return m;}
    private static ItemStack fused(GameTestHelper h){var item=new ItemStack(Items.IRON_SPEAR);FusionItems.attach(item,new ItemStack(Items.COBBLESTONE),h.getLevel().registryAccess());return item;}
    @GameTest public void sevenNativeTypesAndForeignPiercingRemainSeparate(GameTestHelper h){
        for(var item:new Item[]{Items.WOODEN_SPEAR,Items.STONE_SPEAR,Items.COPPER_SPEAR,Items.IRON_SPEAR,Items.GOLDEN_SPEAR,Items.DIAMOND_SPEAR,Items.NETHERITE_SPEAR})h.assertTrue(MeleeWeapon.of(new ItemStack(item))==MeleeWeapon.SPEAR&&FusionItems.host(new ItemStack(item)),"Concrete original spear host: "+item);
        var foreign=new ItemStack(Items.STICK);foreign.set(net.minecraft.core.component.DataComponents.PIERCING_WEAPON,new ItemStack(Items.IRON_SPEAR).get(net.minecraft.core.component.DataComponents.PIERCING_WEAPON));h.assertTrue(!MeleeActions.weapon(foreign)&&!FusionItems.host(foreign),"A component alone cannot opt arbitrary items into the new system");h.succeed();
    }
    @GameTest public void nativePierceHitsTwoTargetsWithPerTargetWearFuseAndNoReplay(GameTestHelper h){
        var host=fused(h);var p=player(h,16,host);var a=target(h,16,1.5,4.2);var b=target(h,16,1.5,5.4);var close=target(h,16,1.5,2.3);var far=target(h,16,1.5,7);var side=target(h,16,3.5,4.2);
        SpearAttackAudit.begin(p);send(p,MeleeIntent.BEGIN);h.assertTrue(SpearAttackAudit.piercing==0,"Press never also emits native STAB");send(p,MeleeIntent.RELEASE);
        h.assertTrue(a.getHealth()<200&&b.getHealth()<200&&close.getHealth()==200&&far.getHealth()==200&&side.getHealth()==200,"Native min/max ray penetrates collinear targets, no invented sweep");h.assertTrue(SpearAttackAudit.piercing==1&&SpearAttackAudit.stabs==2&&CounterAttackAudit.calls==0&&host.getDamageValue()==2&&host.get(FusionContent.DATA).remaining()==30,"One dedicated operation, two native stabs, two wear and two real Fuse uses");
        send(p,MeleeIntent.RELEASE);send(p,MeleeIntent.BEGIN);send(p,MeleeIntent.RELEASE);h.assertTrue(SpearAttackAudit.piercing==1&&host.getDamageValue()==2,"Recovery/release replay cannot duplicate piercing");p.discard();for(var m:new Husk[]{a,b,close,far,side})m.discard();h.succeed();
    }
    @GameTest public void wallTruncatesPierceAndCounterAimUsesNativeRange(GameTestHelper h){
        var p=player(h,48,new ItemStack(Items.IRON_SPEAR));var a=target(h,48,1.5,4.2);var b=target(h,48,1.5,5.7);for(int y=48;y<52;y++)h.setBlock(1,y,5,Blocks.STONE);
        h.assertTrue(MeleeActions.pointed(p)==a,"Counter sees nearest real native spear target beyond sword reach");send(p,MeleeIntent.BEGIN);send(p,MeleeIntent.RELEASE);h.assertTrue(a.getHealth()<200&&b.getHealth()==200,"Native wall cuts off the second target");
        a.discard();MeleeActions.reset(p);for(int n=0;n<45;n++)p.doTick();h.assertTrue(MeleeActions.pointed(p)==null,"Wall also blocks counter eligibility");SpearAttackAudit.begin(p);MeleeActions.counterSwing(p,null);h.assertTrue(SpearAttackAudit.piercing==0&&b.getHealth()==200,"Whiff counter never invokes a ray that can hit an unrelated target");p.discard();b.discard();h.succeed();
    }
    @GameTest(maxTicks=30) public void chargedNativePiercePaysOnceAndKeepsBoundedScale(GameTestHelper h){
        var host=fused(h);var p=player(h,80,host);var a=target(h,80,1.5,4.2);SpearAttackAudit.begin(p);send(p,MeleeIntent.BEGIN);h.runAfterDelay(10,()->send(p,MeleeIntent.KEEP));
        h.runAfterDelay(18,()->{SpearAttackAudit.begin(p);double stamina=PlayerStamina.get(p).stamina();send(p,MeleeIntent.RELEASE);h.assertTrue(MeleeActions.view(p).charged()&&MeleeActions.view(p).duration()==13&&PlayerStamina.get(p).stamina()==stamina-16,"Own 18/16/13 contract");h.assertTrue(200-a.getHealth()>5&&host.getDamageValue()==1&&host.get(FusionContent.DATA).remaining()==31&&SpearAttackAudit.piercing==1&&SpearAttackAudit.stabs==1,"Charged base and native-scaled material follow one stab");h.assertTrue(MeleeActions.damageScale(p)==1&&MeleeActions.knockback(p)==0,"Scoped modifiers always restored");p.discard();a.discard();h.succeed();});
    }
    @GameTest public void nativeMinimumChargeReplacementAndUseCancelWithoutFee(GameTestHelper h){
        var host=fused(h);var p=player(h,112,host);var m=target(h,112,1.5,4.2);p.resetAttackStrengthTicker();SpearAttackAudit.begin(p);send(p,MeleeIntent.BEGIN);send(p,MeleeIntent.RELEASE);h.assertTrue(SpearAttackAudit.piercing==0&&host.getDamageValue()==0&&PlayerStamina.get(p).stamina()==100,"Native minimum attack charge not bypassed by light release");
        for(int n=0;n<45;n++)p.doTick();send(p,MeleeIntent.BEGIN);p.setItemInHand(InteractionHand.MAIN_HAND,host.copy());send(p,MeleeIntent.RELEASE);h.assertTrue(m.getHealth()==200&&!MeleeActions.blocksHands(p),"Same-looking spear replacement cancels");p.startUsingItem(InteractionHand.MAIN_HAND);send(p,MeleeIntent.BEGIN);h.assertTrue(!MeleeActions.blocksHands(p),"Kinetic use has priority over held melee");p.discard();m.discard();h.succeed();
    }
    @GameTest public void nativeEnchantmentsSavedComponentsSplitAndBackReference(GameTestHelper h){
        var host=fused(h);var enchant=h.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS);host.enchant(enchant,2);var p=player(h,144,host);var m=target(h,144,1.5,4.2);send(p,MeleeIntent.BEGIN);send(p,MeleeIntent.RELEASE);h.assertTrue(200-m.getHealth()>5&&host.getDamageValue()==1,"Native enchantments remain downstream");
        var ops=net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,h.getLevel().registryAccess());var saved=ItemStack.CODEC.encodeStart(ops,host).getOrThrow();var loaded=ItemStack.CODEC.parse(ops,saved).getOrThrow();h.assertTrue(loaded.is(Items.IRON_SPEAR)&&loaded.get(FusionContent.DATA).remaining()==31&&loaded.getDamageValue()==1&&loaded.get(net.minecraft.core.component.DataComponents.PIERCING_WEAPON)!=null,"Existing item codec saves all native and Fuse components, no new schema");h.assertTrue(FusionItems.splitMaterial(loaded,h.getLevel().registryAccess()).orElseThrow().get(FusionContent.WEAR)==31,"Existing worn material recovery");
        p.getInventory().setItem(1,new ItemStack(Items.STICK));p.getInventory().setSelectedSlot(1);BackEquipment.tick(p);h.assertTrue(BackEquipment.view(p).melee().is(Items.IRON_SPEAR),"Actual primary use records one existing melee reference");p.discard();m.discard();h.succeed();
    }
    @GameTest public void originalKineticSpeedContactAndDismountStayNative(GameTestHelper h){
        var host=fused(h);var p=player(h,176,host);var m=target(h,176,1.5,4.2);var kinetic=host.get(net.minecraft.core.component.DataComponents.KINETIC_WEAPON);p.startUsingItem(InteractionHand.MAIN_HAND);SpearAttackAudit.begin(p);kinetic.damageEntities(host,host.getUseDuration(p)-kinetic.delayTicks(),p,EquipmentSlot.MAINHAND);
        h.assertTrue(m.getHealth()==200&&SpearAttackAudit.stabs==0&&host.getDamageValue()==0&&host.get(FusionContent.DATA).remaining()==32,"Stationary native use grants no invented damage or Fuse use");p.discard();m.discard();
        host=fused(h);p=player(h,200,host);m=target(h,200,1.5,4.2);kinetic=host.get(net.minecraft.core.component.DataComponents.KINETIC_WEAPON);var boat=h.spawn(EntityTypes.OAK_BOAT,new Vec3(1.5,200,4.2),EntitySpawnReason.COMMAND);boat.setNoGravity(true);m.startRiding(boat);p.startUsingItem(InteractionHand.MAIN_HAND);p.setKnownMovement(new Vec3(0,0,1));SpearAttackAudit.begin(p);
        kinetic.damageEntities(host,host.getUseDuration(p)-kinetic.delayTicks(),p,EquipmentSlot.MAINHAND);float health=m.getHealth();int wear=host.getDamageValue();h.assertTrue(health<200&&!m.isPassenger()&&SpearAttackAudit.piercing==0&&SpearAttackAudit.stabs==1&&wear==1&&host.get(FusionContent.DATA).remaining()==31,"Native speed-dependent damage/dismount and one material effect, no primary call");
        kinetic.damageEntities(host,host.getUseDuration(p)-kinetic.delayTicks(),p,EquipmentSlot.MAINHAND);h.assertTrue(m.getHealth()==health&&host.getDamageValue()==wear&&SpearAttackAudit.stabs==1,"Native contact memory prevents repeat stab");p.discard();m.discard();boat.discard();h.succeed();
    }
    @GameTest public void offhandKineticUsesActualFusedStackAndBackReference(GameTestHelper h){
        var main=new ItemStack(Items.IRON_SWORD);FusionItems.attach(main,new ItemStack(Items.COBBLESTONE),h.getLevel().registryAccess());
        var host=fused(h);var p=player(h,224,main);p.setItemInHand(InteractionHand.OFF_HAND,host);var m=target(h,224,1.5,4.2);
        p.startUsingItem(InteractionHand.OFF_HAND);p.setKnownMovement(new Vec3(0,0,1));var kinetic=host.get(net.minecraft.core.component.DataComponents.KINETIC_WEAPON);
        kinetic.damageEntities(host,host.getUseDuration(p)-kinetic.delayTicks(),p,EquipmentSlot.OFFHAND);
        h.assertTrue(m.getHealth()<200&&host.getDamageValue()==1&&host.get(FusionContent.DATA).remaining()==31&&main.getDamageValue()==0&&main.get(FusionContent.DATA).remaining()==32,"Native offhand stab consumes only its actual host");
        p.stopUsingItem();p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);p.getInventory().setItem(2,host);BackEquipment.tick(p);
        h.assertTrue(BackEquipment.view(p).melee().is(Items.IRON_SPEAR),"Actual offhand damage records spear rather than main sword");p.discard();m.discard();h.succeed();
    }

}
