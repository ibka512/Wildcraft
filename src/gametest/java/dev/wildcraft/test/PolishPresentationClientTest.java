package dev.wildcraft.test;

import dev.wildcraft.client.cooking.CookingGuide;
import dev.wildcraft.client.cooking.CookingScreen;
import dev.wildcraft.client.mechanics.MachineRenderer;
import dev.wildcraft.client.weather.WindHud;
import dev.wildcraft.cooking.*;
import dev.wildcraft.energy.*;
import dev.wildcraft.mechanics.*;
import dev.wildcraft.player.PlayerStamina;
import dev.wildcraft.player.GliderEquipment;
import dev.wildcraft.registry.WildcraftItems;
import dev.wildcraft.traversal.*;
import dev.wildcraft.weather.WindSystem;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

/** Actual rendered guide, HUD combinations and mechanical contacts; fixtures are explicitly seeded. */
public final class PolishPresentationClientTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext c) {
        String oldLanguage=c.computeOnClient(client->client.options.languageCode);
        int oldScale=c.computeOnClient(client->client.options.guiScale().get());
        try {
            guide(c);
            hud(c);
            mechanics(c);
        } finally {
            c.runOnClient(client->{client.options.guiScale().set(oldScale);client.options.setCameraType(CameraType.FIRST_PERSON);});
            language(c,oldLanguage);
        }
        System.out.println("WILDCRAFT POLISH native four-category bilingual guide/scaled multiheart-food-wind HUD/hide/pause/contact camera and retained bottom-part boundary passed");
    }
    private static void guide(ClientGameTestContext c) {
        try(TestSingleplayerContext w=c.worldBuilder().create()) {
            w.getConnection().waitForChunksRender();
            w.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.teleportTo(.5,120,.5);p.setNoGravity(true);var pos=new BlockPos(0,120,2);server.overworld().setBlockAndUpdate(pos,CookingContent.POT.defaultBlockState());p.openMenu((CookingPotEntity)server.overworld().getBlockEntity(pos));});
            c.waitFor(client->client.gui.screen() instanceof CookingScreen);
            for(String lang:new String[]{"en_us","zh_cn"}) {
                language(c,lang);
                c.runOnClient(client->client.options.guiScale().set(2));c.getInput().resizeWindow(854,480);c.waitTicks(3);
                for(int k=0;k<4;k++) {
                    final int kind=k;
                    c.runOnClient(client->{var lines=CookingGuide.lines(kind);check(lines.size()==(kind==0?4:7),"Guide enumerates the actual category recipes");check(lines.stream().noneMatch(line->line.getString().contains("cooking.wildcraft")||line.getString().contains("hud.wildcraft")),"Actual selected language resolves all guide keys");});
                    var cursor=c.computeOnClient(client->{var win=client.getWindow();return new double[]{((win.getGuiScaledWidth()-176)/2+98+kind*16)*win.getWidth()/(double)win.getGuiScaledWidth(),((win.getGuiScaledHeight()-182)/2+10)*win.getHeight()/(double)win.getGuiScaledHeight()};});
                    c.getInput().setCursorPos(cursor[0],cursor[1]);c.waitTicks(3);c.takeScreenshot("polish-cooking-guide-"+lang+"-"+kind);
                }
            }
            c.runOnClient(client->client.player.closeContainer());c.waitForScreen(null);
        }
    }
    private static void hud(ClientGameTestContext c) {
        try(TestSingleplayerContext w=c.worldBuilder().create()) {
            w.getConnection().waitForChunksRender();w.getServer().runCommand("gamemode survival @p");w.getServer().runCommand("weather thunder");w.getServer().runCommand("fillbiome -8 160 -8 8 200 8 minecraft:plains");
            w.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();var l=server.overworld();p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(60);p.setHealth(60);p.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(8);p.setAbsorptionAmount(8);GliderEquipment.set(p,new ItemStack(WildcraftItems.PARAGLIDER));PlayerStamina.fill(p);for(int k=1;k<=3;k++)CookingEffects.eat(p,new MealData(1,k,2,180));p.setNoGravity(true);p.teleportTo(l,.5,190,.5,Set.of(),0,0,false);p.setDeltaMovement(Vec3.ZERO);p.setOnGround(false);l.setRainLevel(1);l.setThunderLevel(1);WindSystem.publish(p);});
            c.waitFor(client->client.player.getY()>189&&GliderEquipment.equipped(client.player)&&WindSystem.local(client.player).precipitation()>.9);
            c.runOnClient(client->{client.player.setNoGravity(true);client.player.setDeltaMovement(Vec3.ZERO);client.player.setOnGround(false);});c.getInput().releaseKey(o->o.keyJump);c.waitTicks(2);c.waitFor(client->Gliding.eligible(client.player)&&client.gui.screen()==null);c.getInput().holdKeyFor(o->o.keyJump,1);c.waitFor(client->Gliding.active(client.player));
            for(String lang:new String[]{"en_us","zh_cn"}) {
                language(c,lang);
                if(!c.computeOnClient(client->Gliding.active(client.player))){c.getInput().releaseKey(o->o.keyJump);c.waitTicks(2);c.getInput().holdKeyFor(o->o.keyJump,1);c.waitFor(client->Gliding.active(client.player));}
                for(int scale:new int[]{1,2,3}) {
                    c.getInput().resizeWindow(1280,960);c.runOnClient(client->client.options.guiScale().set(scale));c.waitTicks(5);check(c.computeOnClient(client->client.getWindow().getGuiScale()==scale),"Window actually renders requested GUI scale "+scale);check(c.computeOnClient(client->Gliding.active(client.player)&&client.player.getHealth()==60&&client.player.getAbsorptionAmount()==8&&client.player.getAttached(CookingEffects.VIEW).recovery()==2),"Rendered wind fixture retains actual glide, hearts and foods");c.takeScreenshot("polish-wind-multiheart-food-"+lang+"-scale"+scale);
                }
            }
            c.getInput().pressKey(com.mojang.blaze3d.platform.InputConstants.KEY_F1);c.waitFor(client->client.gui.hud.isHidden());c.takeScreenshot("polish-wind-hidden-hud");c.getInput().pressKey(com.mojang.blaze3d.platform.InputConstants.KEY_F1);c.waitFor(client->!client.gui.hud.isHidden());
            c.runOnClient(client->client.gui.setScreen(new PauseScreen(true)));c.waitFor(client->client.isPaused());
            long pausedTime=w.getServer().computeOnServer(server->server.overworld().getGameTime());var wind=c.computeOnClient(client->WindSystem.local(client.player));long started=System.nanoTime();c.waitFor(client->System.nanoTime()-started>700_000_000L,1000);
            check(w.getServer().computeOnServer(server->server.overworld().getGameTime())==pausedTime&&wind.equals(c.computeOnClient(client->WindSystem.local(client.player))),"Native pause retains world time and wind view");
            c.takeScreenshot("polish-wind-paused");c.runOnClient(client->client.gui.setScreen(null));c.waitFor(client->!client.isPaused());
        }
    }
    private static void mechanics(ClientGameTestContext c) {
        try(TestSingleplayerContext w=c.worldBuilder().create()) {
            w.getConnection().waitForChunksRender();w.getServer().runCommand("gamemode survival @p");w.getServer().runCommand("time set day");
            w.getServer().runOnServer(server->{var l=server.overworld();var p=server.getPlayerList().getPlayers().getFirst();for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++)l.setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());p.teleportTo(l,-2,120,-2,Set.of(),0,0,false);var b=MechanicsContent.MACHINE.create(l,EntitySpawnReason.COMMAND);b.setPos(.5,120,.5);b.setOwner(p.getUUID());l.addFreshEntity(b);for(int n:new int[]{4,5}){p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(MechanicsContent.WHEEL));check(b.install(p,n,InteractionHand.MAIN_HAND),"Wheel fixture installed");}p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.inventoryMenu.broadcastChanges();});
            c.waitFor(client->MechanicsClientSmokeTest.find(client.level)!=null&&MechanicsClientSmokeTest.find(client.level).kind(4)==6);c.waitTicks(5);MechanicsClientSmokeTest.aim(c,new Vec3(0,.25,0));c.takeScreenshot("polish-wheel-ground-contact");
            c.runOnClient(client->{check(Math.abs(MachineRenderer.partPoint(4,6).y-.25)<1e-9&&MachineNodes.point(4).y==.325,"Only wheel visual mount descends to its adopted radius");});
            w.getServer().runOnServer(server->{var b=MechanicsClientSmokeTest.find(server.overworld());var p=server.getPlayerList().getPlayers().getFirst();check(p.startRiding(b),"Native rider fixture mounts");});c.waitFor(client->client.player.getVehicle()!=null);c.runOnClient(client->{client.options.setCameraType(CameraType.FIRST_PERSON);client.player.setYRot(0);client.player.setXRot(0);check(client.player.getEyeY()>MechanicsClientSmokeTest.find(client.level).getBoundingBox().maxY+.5,"Actual passenger eyes clear body collision top");});c.waitTicks(3);c.takeScreenshot("polish-rider-first-person-level");c.runOnClient(client->client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));c.waitTicks(3);c.takeScreenshot("polish-rider-third-person-wheels");
            w.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();var b=MechanicsClientSmokeTest.find(server.overworld());p.stopRiding();p.teleportTo(-2,120,-2);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(MechanicsContent.SPRING));check(b.install(p,1,InteractionHand.MAIN_HAND),"Bottom spring is still a permitted fixture");p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.inventoryMenu.broadcastChanges();});c.runOnClient(client->client.options.setCameraType(CameraType.FIRST_PERSON));c.waitFor(client->client.player.getVehicle()==null&&MechanicsClientSmokeTest.find(client.level).kind(1)==5);MechanicsClientSmokeTest.aim(c,new Vec3(0,.1,0));c.takeScreenshot("polish-bottom-spring-known-boundary");
        }
    }
    private static void language(ClientGameTestContext c,String code){var reload=c.computeOnClient(client->{client.options.languageCode=code;client.getLanguageManager().setSelected(code);return client.reloadResourcePacks();});c.waitFor(client->reload.isDone());reload.join();c.waitFor(client->client.gui.overlay()==null);}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
