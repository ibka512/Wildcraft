package dev.wildcraft.test;
import dev.wildcraft.client.animation.rig.FirstPersonSword;
import dev.wildcraft.client.input.ActionInput;
import dev.wildcraft.combat.MeleeActions;
import dev.wildcraft.network.MeleeView;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
/** Launch the same production code with only the optional new source removed. */
public final class FirstPersonSwordFallbackClientTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext c){
        try(var w=c.worldBuilder().create()){
            w.getConnection().waitForChunksRender();w.getServer().runCommand("gamemode survival @p");
            w.getServer().runOnServer(s->{for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)s.overworld().setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(.5,120,.5);p.getInventory().clearContent();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.IRON_SWORD));p.inventoryMenu.broadcastChanges();});
            c.waitFor(v->v.player.getY()>119&&v.player.getMainHandItem().is(Items.IRON_SWORD)&&ActionInput.tickFrame().gameplay());c.waitTicks(30);
            c.runOnClient(v->{if(FirstPersonSword.asset("standard")!=null||FirstPersonSword.asset("slim")!=null)throw new AssertionError("Fallback package accidentally contains new source");v.options.setCameraType(CameraType.FIRST_PERSON);FirstPersonSwordAudit.begin();GripAudit.begin();v.options.keyAttack.setDown(true);ActionInput.frame(v);v.options.keyAttack.setDown(false);ActionInput.frame(v);});
            c.waitFor(v->GripAudit.swing>0);c.takeScreenshot("first-person-sword-absent-native-grip-fallback");
            if(!c.computeOnClient(v->FirstPersonSwordAudit.arms==0&&GripAudit.main>0))throw new AssertionError("Absent optional art did not retain original first-person hands");
            c.waitFor(v->MeleeActions.view(v.player).phase()==MeleeView.IDLE);System.out.println("C6AO_ABSENT_SOURCE_PASS");
        }finally{c.runOnClient(v->{FirstPersonSwordAudit.enabled=false;GripAudit.enabled=false;});}
    }
}
