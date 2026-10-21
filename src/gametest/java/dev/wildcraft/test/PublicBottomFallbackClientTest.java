package dev.wildcraft.test;

import dev.wildcraft.client.art.ArtMesh;
import dev.wildcraft.client.mechanics.*;
import dev.wildcraft.energy.*;
import dev.wildcraft.mechanics.*;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;

/** Public build must render and transfer real parts when optional bottom meshes are absent. */
public final class PublicBottomFallbackClientTest implements FabricClientGameTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static MachineEntity body(net.minecraft.world.level.Level l){return MechanicsClientSmokeTest.find(l);}
    @Override public void runTest(ClientGameTestContext c){
        for(var item:new Item[]{EnergyContent.BATTERY,MechanicsContent.ROCKET,MechanicsContent.WHEEL,MechanicsContent.STABILIZER}){
            int kind=item==EnergyContent.BATTERY?2:item==MechanicsContent.ROCKET?4:item==MechanicsContent.WHEEL?6:7;
            String legacy=switch(kind){case 2->"battery";case 4->"rocket";case 6->"wheel";default->"stabilizer";};
            try(var w=c.worldBuilder().create()){
                w.getConnection().waitForChunksRender();w.getServer().runCommand("gamemode survival @p");w.getServer().runCommand("time set day");
                c.runOnClient(v->{check(BottomArt.name(kind)==null,"Private bottom mesh must be absent");check(BottomArt.body(kind).equals("machine"),"Original body fallback");check(BottomArt.part(kind,1,legacy).equals(legacy),"Original part fallback");check(ArtMesh.get(legacy).quadCount()>0&&ArtMesh.get("machine").quadCount()>0,"Real public mesh data");});
                w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)s.overworld().setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());s.overworld().setBlockAndUpdate(new BlockPos(-1,122,0),Blocks.STONE.defaultBlockState());p.teleportTo(s.overworld(),.5,120,-1.5,Set.of(),0,0,false);p.getInventory().clearContent();var stack=new ItemStack(item);if(kind==2)Batteries.charge(stack,500);p.setItemInHand(InteractionHand.MAIN_HAND,stack);p.inventoryMenu.broadcastChanges();var b=MechanicsContent.MACHINE.create(s.overworld(),EntitySpawnReason.COMMAND);b.setPos(.5,123,.5);b.setOwner(p.getUUID());s.overworld().addFreshEntity(b);});
                c.waitFor(v->v.player.getMainHandItem().is(item)&&body(v.level)!=null&&body(v.level).onGround());c.runOnClient(v->v.options.setCameraType(CameraType.FIRST_PERSON));
                MechanicsClientSmokeTest.aim(c,MachineNodes.point(1).add(0,.001,0));
                c.waitFor(v->MachinePresentation.selectedNode(body(v.level))==1&&MachinePresentation.canPreview(body(v.level),1));c.waitTicks(3);
                c.takeScreenshot("public-bottom-"+legacy+"-legacy-ghost");MechanicsClientSmokeTest.click(c);
                c.waitFor(v->body(v.level).kind(1)==kind&&v.player.getMainHandItem().isEmpty());c.waitTicks(3);
                c.takeScreenshot("public-bottom-"+legacy+"-legacy-installed");
                MechanicsClientSmokeTest.sneakClick(c,MachineNodes.point(1).add(0,.001,0));c.waitFor(v->body(v.level).kind(1)==0&&v.player.getMainHandItem().is(item));
                w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(!body(s.overworld()).recover(p,1),"Recovery cannot duplicate");check(p.getMainHandItem().getCount()==1,"Exactly one real part returned");if(kind==2)check(Batteries.energy(p.getMainHandItem())==500,"Original battery energy preserved");});
                c.takeScreenshot("public-bottom-"+legacy+"-legacy-recovered");
            }
        }
        System.out.println("PUBLIC_BOTTOM_FALLBACK_PASS four real preview/install/recovery paths");
    }
}
