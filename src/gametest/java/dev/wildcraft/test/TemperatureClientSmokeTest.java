package dev.wildcraft.test;

import dev.wildcraft.client.temperature.TemperatureHud;
import dev.wildcraft.client.temperature.TemperatureOptions;
import dev.wildcraft.client.hud.StaminaHud;
import dev.wildcraft.temperature.EnvironmentTemperature;
import dev.wildcraft.network.TemperatureView;
import dev.wildcraft.player.PlayerStamina;
import dev.wildcraft.focus.FocusTime;
import dev.wildcraft.player.GliderEquipment;
import dev.wildcraft.registry.WildcraftItems;
import dev.wildcraft.traversal.Gliding;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Real server samples, packet-backed readout, gameplay integration, native rendering and reload. */
public final class TemperatureClientSmokeTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext c) {
        TestWorldSave save;
        String oldLanguage=c.computeOnClient(client -> client.options.languageCode);
        boolean oldTint=c.computeOnClient(client -> TemperatureOptions.get().edgeTint);
        try {
            language(c,"zh_cn");
            c.runOnClient(client -> TemperatureOptions.get().edgeTint=true);
            try (TestSingleplayerContext world=c.worldBuilder().create()) {
                world.getConnection().waitForChunksRender();
                world.getServer().runCommand("gamemode survival @p");
                world.getServer().runCommand("time set day");world.getServer().runCommand("weather clear");
                world.getServer().runOnServer(server -> {
                    var p=server.getPlayerList().getPlayers().getFirst();var level=server.overworld();
                    for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)level.setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());
                    p.teleportTo(level,.5,120,.5,Set.of(),0,0,false);p.setNoGravity(true);p.setOnGround(false);
                });
                c.waitFor(client -> client.player.getY()>119 && client.gui.screen()==null && client.gui.overlay()==null);
                biome(world,"snowy_plains");waitTarget(c,-2.8F);
                c.waitFor(client -> TemperatureHud.displayedBand()==0);c.takeScreenshot("p4a-snow-cold");
                biome(world,"desert");waitTarget(c,1.8F);c.waitTicks(2);
                check(c.computeOnClient(client -> TemperatureHud.displayed()<1.79),"Changing biome does not instantly jump the displayed reading");
                c.waitFor(client -> TemperatureHud.displayedBand()==5);c.takeScreenshot("p4a-desert-hot");
                world.getServer().runOnServer(server -> {
                    for(int x=-1;x<=1;x++)for(int y=120;y<=122;y++)for(int z=-1;z<=1;z++)server.overworld().setBlockAndUpdate(new BlockPos(x,y,z),Blocks.WATER.defaultBlockState());
                });
                c.waitFor(client -> client.player.isInWater());waitTarget(c,.6F);
                check(c.computeOnClient(client -> client.player.getHealth()==20),"Ordinary water/cold feedback has no damage");
                world.getServer().runOnServer(server -> {
                    for(int x=-1;x<=1;x++)for(int y=120;y<=122;y++)for(int z=-1;z<=1;z++)server.overworld().setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
                    var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(.5,120,.5);p.setNoGravity(true);
                });
                c.waitFor(client -> !client.player.isInWater());biome(world,"plains");waitTarget(c,0);
                world.getServer().runOnServer(server -> server.overworld().setBlockAndUpdate(new BlockPos(2,120,0),Blocks.CAMPFIRE.defaultBlockState()));
                c.waitFor(client -> view(client)> .6);c.waitFor(client -> TemperatureHud.displayedBand()==4);c.takeScreenshot("p4a-campfire-warm");
                world.getServer().runOnServer(server -> {
                    server.overworld().setBlockAndUpdate(new BlockPos(2,120,0),Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT,false));
                    var p=server.getPlayerList().getPlayers().getFirst();PlayerStamina.consume(p,30);p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(60);p.setHealth(60);p.setAbsorptionAmount(20);
                });
                waitTarget(c,0);c.waitTicks(3);
                check(c.computeOnClient(client -> TemperatureHud.rowY()>StaminaHud.heartBottom()+24),"Temperature row stays below multiple hearts and visible stamina");
                c.takeScreenshot("p4a-multiple-hearts-stamina");
                // Ordinary temperature feedback leaves the original powder-snow/boots rules operational.
                world.getServer().runOnServer(server -> {
                    for(int x=-1;x<=1;x++)for(int y=120;y<=122;y++)for(int z=-1;z<=1;z++)server.overworld().setBlockAndUpdate(new BlockPos(x,y,z),Blocks.POWDER_SNOW.defaultBlockState());
                });
                c.waitFor(client -> client.player.getTicksFrozen()>5);
                world.getServer().runOnServer(server -> {
                    var p=server.getPlayerList().getPlayers().getFirst();int frozen=p.getTicksFrozen();EnvironmentTemperature.publish(p);
                    check(p.getTicksFrozen()==frozen,"Sampling does not erase native freezing");
                    p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET,new ItemStack(Items.LEATHER_BOOTS));
                });
                c.waitFor(client -> client.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).is(Items.LEATHER_BOOTS) && client.player.getTicksFrozen()==0);
                c.takeScreenshot("p4a-native-leather-boots");
                world.getServer().runOnServer(server -> {
                    for(int x=-1;x<=1;x++)for(int y=120;y<=122;y++)for(int z=-1;z<=1;z++)server.overworld().setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
                    server.getPlayerList().getPlayers().getFirst().setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET,ItemStack.EMPTY);
                });
                // Native gliding still shares stamina, and environment sampling does not create another fee.
                world.getServer().runOnServer(server -> {
                    var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(.5,150,.5);p.setNoGravity(true);p.setOnGround(false);
                    GliderEquipment.set(p,new ItemStack(WildcraftItems.PARAGLIDER));
                });
                c.waitFor(client -> client.player.getY()>149 && !client.player.onGround());
                c.waitFor(client -> GliderEquipment.equipped(client.player));c.getInput().releaseKey(o -> o.keyJump);c.waitTicks(2);
                c.getInput().holdKeyFor(o -> o.keyJump,1);c.waitFor(client -> Gliding.active(client.player));
                check(c.computeOnClient(client -> client.player.getAttached(EnvironmentTemperature.VIEW)!=null),"Environment readout remains during actual gliding");
                c.takeScreenshot("p4a-temperature-gliding");c.getInput().holdKeyFor(o -> o.keyJump,1);c.waitFor(client -> !Gliding.active(client.player));
                biome(world,"desert");waitTarget(c,1.8F);c.waitFor(client -> TemperatureHud.edgeTintVisible());
                world.getServer().runOnServer(server -> {
                    var p=server.getPlayerList().getPlayers().getFirst();p.getInventory().setItem(0,new ItemStack(Items.BOW));p.getInventory().setItem(9,new ItemStack(Items.ARROW,64));p.getInventory().setSelectedSlot(0);PlayerStamina.fill(p);p.setOnGround(false);
                    p.inventoryMenu.broadcastChanges();
                });
                c.waitFor(client -> client.player.getMainHandItem().is(Items.BOW));c.getInput().holdKey(o -> o.keyUse);
                c.waitFor(client -> FocusTime.active(client.player));
                check(!c.computeOnClient(client -> TemperatureHud.edgeTintVisible()),"Real focus session suppresses only the temperature edge tint");
                check(c.computeOnClient(client -> client.player.getAttached(EnvironmentTemperature.VIEW)!=null),"Focus retains the authoritative temperature HUD");
                c.takeScreenshot("p4a-focus-priority");c.getInput().releaseKey(o -> o.keyUse);c.waitFor(client -> !FocusTime.active(client.player));
                c.runOnClient(client -> {TemperatureOptions.get().edgeTint=false;TemperatureOptions.save();TemperatureOptions.get().edgeTint=true;TemperatureOptions.load();});
                check(!c.computeOnClient(client -> TemperatureOptions.get().edgeTint || TemperatureHud.edgeTintVisible()),"Independent saved presentation preference can disable tint");
                check(c.computeOnClient(client -> view(client)>1.6),"Turning off tint never changes the environment reading");
                world.getServer().runCommand("gamemode spectator @p");
                c.waitFor(client -> client.player.isSpectator() && !client.player.hasAttached(EnvironmentTemperature.VIEW));
                world.getServer().runCommand("gamemode survival @p");waitTarget(c,1.8F);
                // Explicit neutral spawn-region fixture: native respawn may choose our new high platform.
                biome(world,"plains");waitTarget(c,0);
                world.getServer().runCommand("kill @p");c.waitFor(client -> !client.player.isAlive() && !client.player.hasAttached(EnvironmentTemperature.VIEW));
                c.runOnClient(client -> client.player.respawn());c.waitFor(client -> client.player.isAlive());
                waitTarget(c,0);
                world.getServer().runOnServer(server -> {
                    var p=server.getPlayerList().getPlayers().getFirst();var nether=server.getLevel(Level.NETHER);
                    p.teleportTo(nether,.5,120,.5,Set.of(),0,0,false);p.setNoGravity(true);
                });
                c.waitFor(client -> client.level.dimension().equals(Level.NETHER));waitTarget(c,3);
                var guestId=new AtomicReference<UUID>();
                world.getServer().runOnServer(server -> {
                    var owner=server.getPlayerList().getPlayers().getFirst();var profile=new GameProfile(UUID.randomUUID(),"TemperatureGuest");guestId.set(profile.id());
                    var guest=new ServerPlayer(server,owner.level(),profile,ClientInformation.createDefault());
                    var connection=new Connection(PacketFlow.SERVERBOUND);new EmbeddedChannel(connection);
                    server.getPlayerList().placeNewPlayer(connection,guest,CommonListenerCookie.createInitial(profile,false));
                    guest.teleportTo(owner.level(),owner.getX()+2,owner.getY(),owner.getZ(),Set.of(),0,0,false);guest.setNoGravity(true);
                    guest.setAttached(EnvironmentTemperature.VIEW,new TemperatureView(-3));
                });
                c.waitFor(client -> client.level.getPlayerByUUID(guestId.get())!=null);
                check(!c.computeOnClient(client -> client.level.getPlayerByUUID(guestId.get()).hasAttached(EnvironmentTemperature.VIEW)),"Actual owner connection does not receive another player's private reading");
                world.getServer().runOnServer(server -> {
                    server.getPlayerList().getPlayer(guestId.get()).discard();
                    server.getPlayerList().getPlayers().getFirst().setAttached(EnvironmentTemperature.VIEW,new TemperatureView(-3));
                });
                save=world.getWorldSave();
            }
            check(c.computeOnClient(client -> client.player==null),"World disconnect clears player view");
            try(TestSingleplayerContext world=save.open()) {
                world.getConnection().waitForChunksRender();waitTarget(c,3);
                check(c.computeOnClient(client -> client.level.dimension().equals(Level.NETHER)),"Reload resamples the saved destination rather than restoring injected climate state");
            }
        } finally {
            c.getInput().releaseKey(o -> o.keyUse);c.getInput().releaseKey(o -> o.keyJump);
            c.runOnClient(client -> {TemperatureOptions.get().edgeTint=oldTint;TemperatureOptions.save();});
            language(c,oldLanguage);
        }
        System.out.println("WILDCRAFT P4A actual biome/water/heat, native powder snow/leather, smooth HUD, gliding/focus, local preference, spectator/death, owner-only sync, dimension and reload checks passed");
    }
    private static void language(ClientGameTestContext c,String name) {
        var reload=c.computeOnClient(client -> {client.options.languageCode=name;client.getLanguageManager().setSelected(name);return client.reloadResourcePacks();});
        c.waitFor(client -> reload.isDone());reload.join();c.waitFor(client -> client.gui.overlay()==null);
    }
    private static float view(net.minecraft.client.Minecraft c) {var v=c.player==null?null:c.player.getAttached(EnvironmentTemperature.VIEW);return v==null?99:v.target();}
    private static void waitTarget(ClientGameTestContext c,float target) {c.waitFor(client -> Math.abs(view(client)-target)<.08);}
    private static void biome(TestSingleplayerContext world,String name) {
        world.getServer().runCommand("fillbiome -8 80 -8 8 160 8 minecraft:"+name);
        world.getServer().runOnServer(server -> EnvironmentTemperature.publish(server.getPlayerList().getPlayers().getFirst()));
    }
    private static void check(boolean value,String message) {if(!value)throw new AssertionError(message);}
}
