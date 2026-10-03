package dev.wildcraft.test;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildcraft.client.focus.FocusSettingsScreen;
import dev.wildcraft.client.render.CharacterPoses;
import dev.wildcraft.client.render.GliderPose;
import dev.wildcraft.client.render.ParagliderMesh;
import dev.wildcraft.equipment.BackEquipment;
import dev.wildcraft.player.GliderEquipment;
import dev.wildcraft.registry.WildcraftItems;
import dev.wildcraft.traversal.Gliding;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Vector3f;

/** Native skin grip, actual first-person geometry, and constrained settings layout. */
public final class CoreArtClientSmokeTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext c) {
        try (TestSingleplayerContext world = c.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            world.getServer().runCommand("gamemode survival @p");
            world.getServer().runCommand("time set day");
            world.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst();
                p.teleportTo(.5,150,.5); p.setNoGravity(true); p.setOnGround(false);
                GliderEquipment.set(p,new ItemStack(WildcraftItems.PARAGLIDER));
            });
            c.getInput().releaseKey(o -> o.keyJump);
            c.waitFor(client -> GliderEquipment.equipped(client.player) && client.player.getY()>149
                    && Gliding.eligible(client.player) && client.player.getAttached(Gliding.VIEW)!=null
                    && client.gui.screen()==null && client.gui.overlay()==null);
            c.waitTicks(3);
            c.getInput().holdKeyFor(o -> o.keyJump,1);
            c.waitFor(client -> Gliding.active(client.player));
            c.runOnClient(client -> {
                client.options.setCameraType(CameraType.FIRST_PERSON);
                var state = (AvatarRenderState) client.getEntityRenderDispatcher().getRenderer(client.player).createRenderState(client.player,1);
                check(ParagliderMesh.PARTS==22 && ParagliderMesh.quadCount()==132,"Approved sloping mesh is not replaced by a flat vanilla cuboid");
                var skin=state.skin;
                for (boolean slim : new boolean[]{false,true}) {
                    state.skin=PlayerSkin.insecure(skin.body(),null,null,slim?PlayerModelType.SLIM:PlayerModelType.WIDE);
                    var model=new PlayerModel(LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE,slim),64,64).bakeRoot(),slim);
                    state.setData(CharacterPoses.POSE,new CharacterPoses.Pose("glide",9,0));
                    model.setupAnim(state);
                    grips(model,slim,"authored third person");
                    GliderPose.apply(model,slim);
                    grips(model,slim,"first person");
                }
                check(!client.player.hasAttached(BackEquipment.DATA),"Visual animation does not synchronize saved private signatures");
            });
            c.waitTicks(3); c.takeScreenshot("art-glider-first-person-grips");
            c.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
            c.waitTicks(3); c.takeScreenshot("art-glider-third-person-front");
            c.getInput().holdKeyFor(o -> o.keyJump,1);
            c.waitFor(client -> !Gliding.active(client.player));
            c.setScreen(() -> new FocusSettingsScreen(null));
            c.waitTicks(2); c.takeScreenshot("art-settings-normal");
            c.runOnClient(client -> {
                client.gui.screen().resize(320,180);
                var buttons=client.gui.screen().children().stream().filter(Button.class::isInstance).map(Button.class::cast).toList();
                check(buttons.stream().anyMatch(b -> b.getY()+b.getHeight()<=180 && b.getMessage().equals(net.minecraft.network.chat.Component.translatable("gui.done"))),"Compact Done remains reachable");
                var container=(net.minecraft.client.gui.components.AbstractContainerWidget)client.gui.screen().children().stream()
                        .filter(net.minecraft.client.gui.components.AbstractContainerWidget.class::isInstance).findFirst().orElseThrow();
                var controls=container.children().stream().filter(Button.class::isInstance).map(Button.class::cast).toList();
                check(controls.size()==6,"All six existing preferences remain present");
                for (var b:controls) check(b.getX()>=0 && b.getRight()<=320,"Compact controls fit the viewport");
                container.setScrollAmount(1000);
                var last=controls.getLast();
                check(last.getY()>=container.getY() && last.getBottom()<=container.getBottom(),"Compact scrolling reveals the full last control");
            });
            c.waitTicks(2); c.takeScreenshot("art-settings-compact-layout");
            c.setScreen(() -> null);
            world.getServer().runOnServer(server -> {
                var p=server.getPlayerList().getPlayers().getFirst();
                p.getInventory().setItem(0,new ItemStack(Items.DIAMOND_SWORD));
                p.getInventory().setItem(1,p.getInventory().getItem(0).copy());
                p.getInventory().setSelectedSlot(0); BackEquipment.register(p,p.getMainHandItem());
                p.inventoryMenu.broadcastChanges();
            });
            c.waitFor(client -> client.player.getAttached(BackEquipment.VISUAL)!=null && client.player.getAttached(BackEquipment.VISUAL).melee().owner()==1);
            long ref=c.computeOnClient(client -> client.player.getAttached(BackEquipment.VISUAL).melee().reference());
            world.getServer().runOnServer(server -> {
                var p=server.getPlayerList().getPlayers().getFirst();p.getInventory().setSelectedSlot(1);BackEquipment.tick(p);p.inventoryMenu.broadcastChanges();
            });
            c.waitFor(client -> client.player.getAttached(BackEquipment.VISUAL).melee().owner()==3);
            check(c.computeOnClient(client -> client.player.getAttached(BackEquipment.VISUAL).melee().reference())==ref,"Identical spare does not take ownership of the recorded reference");
            world.getServer().runOnServer(server -> {
                var p=server.getPlayerList().getPlayers().getFirst();p.getInventory().setSelectedSlot(0);BackEquipment.register(p,p.getMainHandItem());
                p.getInventory().setItem(0,ItemStack.EMPTY);BackEquipment.tick(p);p.inventoryMenu.broadcastChanges();
            });
            c.waitFor(client -> client.player.getAttached(BackEquipment.VISUAL).melee().owner()==0);
        } finally {
            c.getInput().releaseKey(o -> o.keyJump);
            c.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
        }
        System.out.println("WILDCRAFT CORE ART native standard/slim grip, first-person mesh, compact layout, exact-reference ownership checks passed");
    }
    private static void grips(PlayerModel model,boolean slim,String label) {
        for(int side:new int[]{-1,1}) {
            var part=side<0?model.rightArm:model.leftArm;
            var poses=new PoseStack();part.translateAndRotate(poses);
            var end=poses.last().pose().transformPosition(new Vector3f(side*(slim?.5F:1)/16,10/16F,0));
            var target=new Vector3f(side*7.5F/16,-7/16F,-7/16F);
            check(end.distance(target)<.03F,label+" "+(slim?"slim":"standard")+" hand contacts grip: "+end);
        }
    }
    private static void check(boolean okay,String message) { if(!okay)throw new AssertionError(message); }
}
