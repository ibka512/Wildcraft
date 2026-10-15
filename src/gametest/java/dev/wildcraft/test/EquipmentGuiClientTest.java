package dev.wildcraft.test;
import dev.wildcraft.client.input.ActionInput;
import dev.wildcraft.client.render.BackTransitions;
import dev.wildcraft.equipment.BackEquipment;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
/** Regression: menu transitions can both occur between avatar render extractions. */
public final class EquipmentGuiClientTest implements FabricClientGameTest {
    private static AvatarRenderState state(Minecraft c){return (AvatarRenderState)c.getEntityRenderDispatcher().getRenderer(c.player).createRenderState(c.player,1);}
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    @Override public void runTest(ClientGameTestContext c){try(TestSingleplayerContext w=c.worldBuilder().create()){
        w.getConnection().waitForChunksRender();w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)s.overworld().setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());p.teleportTo(p.level(),.5,120,.5,Set.of(),0,0,true);p.getInventory().clearContent();p.getInventory().setSelectedSlot(0);p.getInventory().setItem(0,new ItemStack(Items.IRON_SPEAR));BackEquipment.register(p,p.getMainHandItem());p.inventoryMenu.broadcastChanges();});c.waitFor(v->v.player.getMainHandItem().is(Items.IRON_SPEAR)&&ActionInput.tickFrame().gameplay());c.runOnClient(v->v.options.setCameraType(CameraType.FIRST_PERSON));
        c.getInput().holdKeyFor(v->v.keyHotbarSlots[7],1);c.waitFor(v->v.player.getMainHandItem().isEmpty());c.waitTicks(12);c.getInput().holdKeyFor(v->v.keyHotbarSlots[0],1);c.waitFor(v->BackTransitions.forArm(state(v),v.player.getMainArm())!=null);
        c.runOnClient(v->{var before=BackTransitions.forArm(state(v),v.player.getMainArm());System.out.println("GUI REPRO before="+before+" visual="+v.player.getAttached(BackEquipment.VISUAL));v.gui.setScreen(new InventoryScreen(v.player));v.gui.setScreen(null);var after=BackTransitions.forArm(state(v),v.player.getMainArm());System.out.println("GUI REPRO after="+after+" visual="+v.player.getAttached(BackEquipment.VISUAL));check(after==null,"Real inventory opened and closed between render extractions must cancel old draw");});c.takeScreenshot("equipment-gui-no-render-between-menu-transitions");c.waitTicks(4);check(c.computeOnClient(v->BackTransitions.forArm(state(v),v.player.getMainArm())==null),"Canceled draw never restarts after actual GUI close");// Authority may arrive after the menu interruption; exercise actual native carried-item synchronization.
        c.getInput().holdKeyFor(v->v.keyHotbarSlots[7],1);c.waitFor(v->v.player.getMainHandItem().isEmpty());c.waitTicks(12);w.getServer().runCommand("tick rate 1");
        c.runOnClient(v->{v.player.getInventory().setSelectedSlot(0);((dev.wildcraft.mixin.client.CarriedItemAccess)v.gameMode).wildcraft$syncCarriedItem();System.out.println("GUI DELAY native selected="+v.player.getInventory().getSelectedSlot()+" visual="+v.player.getAttached(BackEquipment.VISUAL));v.gui.setScreen(new InventoryScreen(v.player));v.gui.setScreen(null);state(v);EquipmentAudit.begin();});
        c.waitFor(v->v.player.getAttached(BackEquipment.VISUAL).melee().owner()==1);check(c.computeOnClient(v->BackTransitions.forArm(state(v),v.player.getMainArm())==null),"Delayed actual ownership has no new canceled reach on its first observed frame");c.waitTicks(3);System.out.println("GUI DELAY actual submitted draws="+c.computeOnClient(v->EquipmentAudit.draw[0]));check(c.computeOnClient(v->EquipmentAudit.draw[0]==0),"Actual delayed ownership after menu close must not replay canceled draw");w.getServer().runCommand("tick rate 20");c.getInput().holdKeyFor(v->v.keyHotbarSlots[7],1);c.waitFor(v->v.player.getMainHandItem().isEmpty());c.waitTicks(12);EquipmentAudit.begin();c.getInput().holdKeyFor(v->v.keyHotbarSlots[0],1);c.waitFor(v->EquipmentAudit.draw[0]>0);c.takeScreenshot("equipment-gui-new-legitimate-draw-after-canceled-update");System.out.println("GUI REPRO real menu transition between renders, native delayed ownership consumed, next actual draw retained PASS");
    }finally{c.runOnClient(v->EquipmentAudit.enabled=false);}}
}
