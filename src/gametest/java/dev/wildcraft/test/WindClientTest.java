package dev.wildcraft.test;

import dev.wildcraft.weather.*;
import dev.wildcraft.client.weather.WindHud;
import dev.wildcraft.traversal.*;
import dev.wildcraft.player.*;
import dev.wildcraft.focus.FocusTime;
import dev.wildcraft.registry.WildcraftItems;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Uses real jump/hotbar/bow input, owner view, weather and saved singleplayer world. */
public final class WindClientTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext c){
        TestWorldSave save;long[] seed={0},tick={0};WindRules.Flow[] retained={null};
        try(TestSingleplayerContext world=c.worldBuilder().create()){
            world.getConnection().waitForChunksRender();world.getServer().runCommand("gamemode survival @p");world.getServer().runCommand("weather thunder");world.getServer().runCommand("fillbiome -16 140 -16 16 165 16 minecraft:plains");
            world.getServer().runOnServer(server->{
                var level=server.overworld();var p=server.getPlayerList().getPlayers().getFirst();
                for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)level.setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());
                level.setRainLevel(1);level.setThunderLevel(1);p.getInventory().clearContent();p.getInventory().setItem(2,new ItemStack(Items.BOW));p.getInventory().setItem(9,new ItemStack(Items.ARROW,4));
                GliderEquipment.set(p,new ItemStack(WildcraftItems.PARAGLIDER));PlayerStamina.fill(p);p.getInventory().setSelectedSlot(0);
                for(int kind:new int[]{1,2,3})dev.wildcraft.cooking.CookingEffects.eat(p,new dev.wildcraft.cooking.MealData(1,kind,2,120));
                p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);p.teleportTo(level,.5,150,.5,Set.of(),0,0,false);p.setOnGround(false);p.inventoryMenu.broadcastChanges();WindSystem.publish(p);
            });
            c.waitFor(client->client.player.getY()>149&&GliderEquipment.equipped(client.player)&&WindSystem.local(client.player).precipitation()>.9);
            c.runOnClient(client->{client.player.setNoGravity(true);client.player.setDeltaMovement(Vec3.ZERO);});
            jump(c);c.waitFor(client->Gliding.active(client.player));c.waitTicks(5);
            var start=c.computeOnClient(client->client.player.position());var wind=c.computeOnClient(client->WindSystem.local(client.player));float yaw=c.computeOnClient(client->client.player.getYRot());
            c.waitTicks(16);var end=c.computeOnClient(client->client.player.position());
            check(end.y<start.y-1.1&&end.y>start.y-2.1,"Actual storm glide descends within its wet bound");
            System.out.println("P10 measured start="+start+" end="+end+" wind="+wind+" yaw="+yaw);
            var drift=end.subtract(start).subtract(Gliding.movement(yaw,Vec3.ZERO).scale(16));
            check(drift.x*wind.x()+drift.z*wind.z()>(wind.x()*wind.x()+wind.z()*wind.z())*9,"Real drift follows received wind including an exactly head-on direction");
            check(c.computeOnClient(client->Gliding.active(client.player)),"Strong natural wind does not trip existing movement guard");
            c.runOnClient(client->{check(WindHud.fits(427,240,80,100),"Normal HUD fits");check(!WindHud.fits(160,120,55,100),"Tiny HUD avoids reticle/bottom");});
            c.takeScreenshot("p10-native-thunder-glide-wind");
            world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();var b=p.blockPosition();for(int x=b.getX()-5;x<=b.getX()+5;x++)for(int z=b.getZ()-5;z<=b.getZ()+5;z++)server.overworld().setBlockAndUpdate(new BlockPos(x,153,z),Blocks.STONE.defaultBlockState());});
            c.waitFor(client->!WindSystem.local(client.player).outdoors());c.takeScreenshot("p10-native-roof-sheltered-glide");
            c.getInput().holdKeyFor(o->o.keyHotbarSlots[2],1);c.waitFor(client->!Gliding.active(client.player)&&client.player.getMainHandItem().is(Items.BOW));
            c.getInput().holdKey(o->o.keyUse);c.waitFor(client->FocusTime.active(client.player));
            world.getServer().runOnServer(server->check(server.tickRateManager().tickrate()==5,"Native air draw still acquires singleplayer focus"));
            c.waitTicks(10);
            c.getInput().releaseKey(o->o.keyUse);c.waitFor(client->!FocusTime.active(client.player));
            world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();check(server.tickRateManager().tickrate()==20&&p.getInventory().getItem(9).getCount()==3,"Release restores 20TPS and consumes one arrow");
                Gliding.reset(p,false);p.setNoGravity(false);p.teleportTo(server.overworld(),.5,120,.5,Set.of(),0,0,false);p.setDeltaMovement(Vec3.ZERO);seed[0]=server.overworld().getSeed();tick[0]=server.overworld().getGameTime();retained[0]=WindRules.field(seed[0],tick[0],1,1);});
            save=world.getWorldSave();
        }finally{c.getInput().releaseKey(o->o.keyUse);c.getInput().releaseKey(o->o.keyJump);}
        try(TestSingleplayerContext world=save.open()){
            world.getConnection().waitForChunksRender();c.waitFor(client->client.player.hasAttached(WindSystem.VIEW));
            world.getServer().runOnServer(server->{check(server.overworld().getSeed()==seed[0]&&retained[0].equals(WindRules.field(server.overworld().getSeed(),tick[0],1,1)),"Actual saved world retains deterministic wind seed");var p=server.getPlayerList().getPlayers().getFirst();check(!Gliding.active(p)&&!FocusTime.active(server)&&GliderEquipment.equipped(p),"Reload keeps equipped gear and clears temporary actions");
                p.setHealth(0);WindSystem.publish(p);check(!p.hasAttached(WindSystem.VIEW),"Death removes local wind view");});
        }
        System.out.println("WILDCRAFT P10 native storm drift/descending/unchanged guard/roof shelter/aim-safe HUD/hotbar close/5 TPS bow/one arrow/disk reload/death passed");
    }
    private static void jump(ClientGameTestContext c){c.getInput().releaseKey(o->o.keyJump);c.waitTicks(1);c.getInput().holdKeyFor(o->o.keyJump,1);c.waitTicks(2);}
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
