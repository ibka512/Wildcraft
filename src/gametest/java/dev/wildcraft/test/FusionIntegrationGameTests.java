package dev.wildcraft.test;

import dev.wildcraft.cooking.*;
import dev.wildcraft.fuse.*;
import dev.wildcraft.network.*;
import dev.wildcraft.player.*;
import dev.wildcraft.traversal.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;

public final class FusionIntegrationGameTests {
    @GameTest public void traversalHandsFeesAndMealKeepOneFusion(GameTestHelper h){
        var p=h.makeMockServerPlayerInLevel();p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());p.setAttached(PlayerStamina.DATA,new StaminaData(1,20,60,0));CookingEffects.eat(p,new MealData(1,MealData.RECOVERY,2,30));
        for(int y=1;y<=8;y++)h.setBlock(2,y,1,net.minecraft.world.level.block.Blocks.STONE);
        p.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(1.7,1,1.5)));p.setYRot(-90);p.setOnGround(true);Climbing.reset(p,false);var host=new ItemStack(Items.IRON_SWORD);p.setItemInHand(InteractionHand.MAIN_HAND,host);p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.SNOWBALL,2));Climbing.receive(p,new ClimbInput(true,(byte)1,(byte)0));h.assertTrue(Climbing.active(p),"Actual wall grip authorized");
        for(int n=0;n<20;n++)h.assertTrue(!FusionActions.receive(p,new FusionIntent(false)),"Climbing hands reject otherwise valid fusion");h.assertTrue(!host.has(FusionContent.DATA)&&p.getOffhandItem().getCount()==2,"Rejected intents neither attach nor debit material");Climbing.tick(p);PlayerStamina.tick(p);h.assertTrue(Math.abs(PlayerStamina.get(p).stamina()-59.6)<1e-6,"Recovery meal cannot recover during actual climbing cost");Climbing.receive(p,ClimbInput.RELEASED);h.assertTrue(FusionActions.fuse(p),"Same actual hands fuse after grip release");var retained=host.get(FusionContent.DATA);
        p.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(1,20,1)));p.setOnGround(false);Gliding.reset(p,false);GliderEquipment.set(p,new ItemStack(dev.wildcraft.registry.WildcraftItems.PARAGLIDER));Gliding.receive(p,GlideInput.OPEN);h.assertTrue(Gliding.active(p)&&!FusionActions.split(p),"True equipped glide rejects splitting a valid material");double before=PlayerStamina.get(p).stamina();Gliding.tick(p);PlayerStamina.tick(p);h.assertTrue(Math.abs(PlayerStamina.get(p).stamina()-(before-Gliding.COST))<1e-6&&host.get(FusionContent.DATA).equals(retained)&&p.getOffhandItem().getCount()==1,"Glide fee retains true held fusion/material; recovery meal adds no special-action recovery");Gliding.receive(p,GlideInput.CLOSED);
        p.getInventory().setItem(5,p.getOffhandItem());p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.SHIELD));p.getInventory().setItem(1,new ItemStack(Items.IRON_SWORD));p.getInventory().setSelectedSlot(1);p.startUsingItem(InteractionHand.OFF_HAND);h.assertTrue(p.isUsingItem()&&!FusionActions.fuse(p)&&p.getOffhandItem().getCount()==1&&!p.getMainHandItem().has(FusionContent.DATA),"Actual raised shield reserves hands and rejects otherwise valid fusion");p.stopUsingItem();h.assertTrue(FusionActions.fuse(p)&&p.getOffhandItem().isEmpty()&&host.get(FusionContent.DATA).equals(retained)&&PlayerStamina.get(p).highestLevel()==20&&PlayerStamina.get(p).capacity()==140,"Released shield can be one real material; previous fusion and historical peak unaffected");p.discard();h.succeed();
    }
}
