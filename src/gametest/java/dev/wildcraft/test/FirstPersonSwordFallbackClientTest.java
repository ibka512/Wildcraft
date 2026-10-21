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
            for(var item:new Item[]{Items.IRON_SWORD,Items.IRON_AXE}){
                w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item));p.inventoryMenu.broadcastChanges();});
                c.waitFor(v->v.player.getMainHandItem().is(item));c.waitTicks(10);
                c.runOnClient(v->{
                    for(var kind:new dev.wildcraft.combat.MeleeWeapon[]{dev.wildcraft.combat.MeleeWeapon.SWORD,dev.wildcraft.combat.MeleeWeapon.AXE})
                        for(var variant:new String[]{"standard","slim"})if(dev.wildcraft.client.animation.rig.FirstPersonHeavy.asset(variant,kind)!=null)throw new AssertionError("Private heavy data in public package");
                    FirstPersonSwordAudit.begin();GripAudit.begin();v.options.keyAttack.setDown(true);ActionInput.frame(v);
                });
                c.waitFor(v->MeleeActions.view(v.player).phase()==MeleeView.CHARGE&&MeleeActions.view(v.player).age()>=(item==Items.IRON_AXE?22:14));
                if(!c.computeOnClient(v->FirstPersonSwordAudit.arms==0&&GripAudit.main>0))throw new AssertionError("Heavy charge failed native arm fallback");
                c.takeScreenshot("public-"+(item==Items.IRON_AXE?"axe":"sword")+"-charge-native-fallback");
                c.runOnClient(v->{v.options.keyAttack.setDown(false);ActionInput.frame(v);});
                c.waitFor(v->GripAudit.swing>0);c.takeScreenshot("public-"+(item==Items.IRON_AXE?"axe":"sword")+"-release-native-fallback");
                c.waitFor(v->MeleeActions.view(v.player).phase()==MeleeView.IDLE);
            }
            System.out.println("PUBLIC_HEAVY_ABSENT_SOURCE_PASS sword/axe charge and release use native arms");
        }finally{c.runOnClient(v->{FirstPersonSwordAudit.enabled=false;GripAudit.enabled=false;});}
    }
}
