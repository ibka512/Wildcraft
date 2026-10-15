package dev.wildcraft.test;

import com.mojang.blaze3d.platform.InputConstants;
import dev.wildcraft.client.focus.FocusSettingsScreen;
import dev.wildcraft.client.input.*;
import dev.wildcraft.fuse.FusionContent;
import dev.wildcraft.input.*;
import dev.wildcraft.player.GliderEquipment;
import dev.wildcraft.registry.WildcraftItems;
import dev.wildcraft.traversal.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;

/** Real native bindings, production packets, UI takeover, and rebindings in a disposable world. */
public final class InputClientTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext c) {
        try (TestSingleplayerContext w = c.worldBuilder().create()) {
            w.getConnection().waitForChunksRender(); w.getServer().runCommand("gamemode survival @p");
            w.getServer().runOnServer(server -> {
                var p=server.getPlayerList().getPlayers().getFirst();
                for(int x=-3;x<=3;x++)for(int z=-2;z<=8;z++)server.overworld().setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());
                for(int x=-3;x<=3;x++)for(int y=120;y<=128;y++)server.overworld().setBlockAndUpdate(new BlockPos(x,y,5),Blocks.STONE.defaultBlockState());
                p.teleportTo(.5,120,4.68);p.setYRot(0);p.setXRot(0);
                p.getInventory().setItem(0,new ItemStack(Items.IRON_SWORD));p.getInventory().setSelectedSlot(0);
                p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.COBBLESTONE,2));p.inventoryMenu.broadcastChanges();
            });
            c.waitFor(client->client.player.getY()>119 && client.player.getMainHandItem().is(Items.IRON_SWORD)
                    && client.player.getOffhandItem().is(Items.COBBLESTONE) && ActionInput.tickFrame().gameplay());
            c.runOnClient(client->{client.player.setYRot(0);client.player.setXRot(0);WildcraftBindings.FUSE.setKey(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_Y));KeyMapping.resetMapping();});
            c.getInput().holdKeyFor(WildcraftBindings.FUSE,1);
            c.waitFor(client->client.player.getMainHandItem().has(FusionContent.DATA));
            w.getServer().runOnServer(server->check(server.getPlayerList().getPlayers().getFirst().getOffhandItem().getCount()==1,"Rebound action consumes exactly one material"));
            c.setScreen(()->new FocusSettingsScreen(null));c.waitTicks(2);
            c.runOnClient(client->{WildcraftBindings.FUSE.setDown(true);KeyMapping.click(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_Y));client.options.keyShift.setDown(true);});
            c.waitTicks(2);
            check(c.computeOnClient(client->ActionInput.tickFrame().context()==InputContext.GUI && ActionInput.tickFrame().clicks(Action.FUSE)==0),"GUI owns queued command");
            c.setScreen(()->null);c.waitTicks(2);
            check(c.computeOnClient(client->client.player.getMainHandItem().has(FusionContent.DATA)),"Closing UI with held fuse does not split or replay");
            c.runOnClient(client->{WildcraftBindings.FUSE.setDown(false);client.options.keyShift.setDown(false);});c.waitTicks(2);
            c.getInput().holdKey(o->o.keyShift);c.getInput().holdKeyFor(WildcraftBindings.FUSE,1);
            c.waitFor(client->!client.player.getMainHandItem().has(FusionContent.DATA));c.getInput().releaseKey(o->o.keyShift);
            w.getServer().runOnServer(server->check(server.getPlayerList().getPlayers().getFirst().getOffhandItem().getCount()==2,"Shift+rebound fuse returns material exactly once"));
            c.getInput().holdKey(WildcraftBindings.CLIMB);c.getInput().holdKey(o->o.keyUp);
            c.waitFor(client->Climbing.active(client.player)&&client.player.getY()>121,100);
            c.getInput().releaseKey(o->o.keyUp);c.takeScreenshot("c1-unified-climb");
            c.setScreen(()->new FocusSettingsScreen(null));c.waitTicks(3);
            c.waitFor(client->!Climbing.active(client.player));
            c.runOnClient(client->WildcraftBindings.CLIMB.setDown(true));c.waitTicks(2);c.setScreen(()->null);c.waitTicks(3);
            check(c.computeOnClient(client->!ActionInput.tickFrame().down(Action.CLIMB)),"Held grip is quarantined after screen closes");
            c.getInput().releaseKey(WildcraftBindings.CLIMB);c.waitTicks(2);
            w.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(.5,150,.5);p.setNoGravity(true);p.setOnGround(false);GliderEquipment.set(p,new ItemStack(WildcraftItems.PARAGLIDER));});
            c.waitFor(client->Gliding.eligible(client.player)&&GliderEquipment.equipped(client.player)&&client.player.getY()>149);
            c.getInput().holdKeyFor(o->o.keyJump,1);c.waitFor(client->Gliding.active(client.player));
            c.takeScreenshot("c1-unified-glide");
            c.runOnClient(client->{client.options.keyUse.setDown(true);ActionInput.frame(client);client.options.keyUse.setDown(false);ActionInput.frame(client);});
            c.waitFor(client->!Gliding.active(client.player));
            check(c.computeOnClient(client->!client.player.isUsingItem()),"A captured short use tap closes canopy without synthesizing item use");
            c.getInput().holdKeyFor(o->o.keyJump,1);c.waitFor(client->Gliding.active(client.player));
            c.getInput().holdKeyFor(o->o.keyJump,1);c.waitFor(client->!Gliding.active(client.player));
            c.getInput().holdKeyFor(WildcraftBindings.SETTINGS,1);c.waitFor(client->client.gui.screen() instanceof FocusSettingsScreen);
            c.takeScreenshot("c1-unified-settings");c.setScreen(()->null);
            if(Boolean.getBoolean("wildcraft.c0.controller")) {
                c.runOnClient(client->{
                    var loader=net.fabricmc.loader.api.FabricLoader.getInstance();check(loader.isModLoaded("controlify"),"Optional Controlify initializes");
                    check(loader.isModLoaded("yet_another_config_lib_v3"),"Controlify config dependency initializes");
                    for(var binding:WildcraftBindings.ALL.values())check(java.util.Arrays.asList(client.options.keyMappings).contains(binding),"Native key mapping is discoverable by controller adapter");
                    try {
                        var api=Class.forName("dev.isxander.controlify.api.bind.ControlifyBindApi");var registry=api.getMethod("get").invoke(null);
                        for(var binding:WildcraftBindings.ALL.values()) {
                            var correlations=(java.util.List<?>)api.getMethod("getKeyCorrelation",KeyMapping.class).invoke(registry,binding);
                            check(!correlations.isEmpty(),"Controlify registers Wildcraft mapping: "+binding.getName());
                            System.out.println("C0 CONTROLIFY registered "+binding.getName()+" bindings="+correlations.size());
                        }
                    } catch(ReflectiveOperationException ex){throw new AssertionError("Controlify binding API probe failed",ex);}
                });
                System.out.println("C0 CONTROLIFY compatibility startup and native binding discovery PASS; physical hardware NOT tested");
            }
        } finally {
            c.getInput().releaseKey(WildcraftBindings.CLIMB);c.getInput().releaseKey(WildcraftBindings.FUSE);
            c.getInput().releaseKey(o->o.keyUp);c.getInput().releaseKey(o->o.keyJump);c.getInput().releaseKey(o->o.keyShift);
            c.runOnClient(client->{WildcraftBindings.FUSE.setKey(WildcraftBindings.FUSE.getDefaultKey());KeyMapping.resetMapping();});
        }
        System.out.println("C1 INPUT native rebind/fuse/split, GUI takeover/quarantine, climb/glide/settings PASS");
    }
    private static void check(boolean okay,String message){if(!okay)throw new AssertionError(message);}
}
