package dev.wildcraft.test;

import dev.wildcraft.art.ArtFeedback;
import dev.wildcraft.client.art.*;
import dev.wildcraft.client.cooking.MealHud;
import dev.wildcraft.client.render.CharacterPoses;
import dev.wildcraft.cooking.*;
import dev.wildcraft.energy.*;
import dev.wildcraft.fuse.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Native loaded models, GUI scale, real packet decoding and bounded particle providers. */
public final class SecondArtClientTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext c){
        var oldParticles=c.computeOnClient(client->client.options.particles().get());
        int oldScale=c.computeOnClient(client->client.options.guiScale().get());
        try(TestSingleplayerContext w=c.worldBuilder().create()){
            w.getConnection().waitForChunksRender();
            w.getServer().runOnServer(server->{
                var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.teleportTo(.5,120,.5);p.setNoGravity(true);
                for(int x=-3;x<4;x++)for(int z=-3;z<5;z++)server.overworld().setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());
                for(int k=0;k<4;k++)p.getInventory().setItem(k+1,CookingContent.meal(new MealData(1,k,k==0?0:2,k==0?0:30)));
                p.getInventory().setItem(8,new ItemStack(EnergyContent.BATTERY));p.getInventory().setItem(0,new ItemStack(Items.DIAMOND_SWORD));p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new ItemStack(Items.DIAMOND));
                check(FusionActions.fuse(p),"Real fusion transaction commits before mark");p.inventoryMenu.broadcastChanges();
                CookingEffects.eat(p,new MealData(1,1,2,30));CookingEffects.eat(p,new MealData(1,2,1,60));CookingEffects.eat(p,new MealData(1,3,2,10));
            });
            c.waitFor(client->client.player.getMainHandItem().has(FusionContent.VIEW)&&client.player.getAttached(CookingEffects.VIEW)!=null);
            c.runOnClient(client->{
                String[] kinds={"vegetable","warming","cooling","recovery"};
                for(int k=0;k<4;k++){
                    var out=new ItemStackRenderState();client.getItemModelResolver().updateForLiving(out,CookingContent.meal(new MealData(1,k,k==0?0:1,k==0?0:30)),ItemDisplayContext.GUI,client.player);
                    var sprite=out.pickParticleMaterial(client.level.getRandom()).sprite().contents().name();check(sprite.toString().equals("wildcraft:item/meal_"+kinds[k]),"Loaded resource dispatcher selects actual meal kind: "+sprite);
                }
                var battery=new ItemStack(EnergyContent.BATTERY);Batteries.charge(battery,333);var gui=new ItemStackRenderState();client.getItemModelResolver().updateForLiving(gui,battery,ItemDisplayContext.GUI,client.player);
                check(gui.pickParticleMaterial(client.level.getRandom()).sprite().contents().name().toString().endsWith("battery_charge_5"),"Actual charge mask resolves at a third-cell boundary");
                var held=new ItemStackRenderState();client.getItemModelResolver().updateForLiving(held,battery,ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,client.player);check(!held.isEmpty()&&held.getModelBoundingBox().getXsize()<.6,"Battery switches to full-size adopted 3D state");
                var missing=dev.wildcraft.client.fuse.FusionPresentation.model(client.getItemModelResolver(),new FusionVisual(dev.wildcraft.Wildcraft.id("unavailable_material"),6,7,false),client.level,client.player,0);check(!missing.isEmpty(),"Unavailable material gets a bounded nonrecursive marker");
                for(String name:List.of("machine","battery","battery_item","wing","fan","rocket","spent_rocket","spring","wheel","stabilizer","buoyancy"))check(ArtMesh.get(name).quadCount()>0,"Mesh loads "+name);
                var blended=CharacterPoses.blend(new float[]{0,0,0,0,0,(float)Math.toRadians(179)},new float[]{0,0,0,0,0,(float)Math.toRadians(-179)},.5F);check(Math.abs(blended[5])>3,"Rotation takes shortest path across wrap");
                int[] levels={2,1,2},seconds={127,84,56};check(MealHud.layout(426,240,64,levels,seconds).size()==3,"Three effects fit under hearts");check(MealHud.layout(200,140,92,levels,seconds).isEmpty(),"Extreme viewport never covers bottom HUD or aiming centre");
                client.options.guiScale().set(2);client.gui.setScreen(new InventoryScreen(client.player));
            });
            c.waitTicks(3);c.takeScreenshot("second-art-meals-fused-inventory");c.runOnClient(client->client.gui.setScreen(null));c.waitTicks(2);c.takeScreenshot("second-art-three-effects-native-hud");
            c.runOnClient(client->client.options.particles().set(ParticleStatus.ALL));
            w.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();ServerPlayNetworking.send(p,new ArtFeedback(server.overworld().dimension().identifier(),Long.MAX_VALUE-10,p.getUUID(),p.position().add(2,1,0),10,0));});
            c.waitFor(client->ArtFeedbackClient.particles()>0);
            c.runOnClient(client->{
                var position=client.player.position().add(2,1,0);var dim=client.level.dimension().identifier();int before=ArtFeedbackClient.particles();
                ArtFeedbackClient.receive(client,new ArtFeedback(dim,Long.MAX_VALUE-10,client.player.getUUID(),position,10,0));check(ArtFeedbackClient.particles()==before,"Duplicate ordered feedback does not replay particles");
                for(int n=0;n<8;n++)ArtFeedbackClient.receive(client,new ArtFeedback(dim,Long.MAX_VALUE-9+n,new UUID(1,n),position,n%12,1));
                check(ArtFeedbackClient.particles()==48&&ArtFeedbackClient.voices()>0&&ArtFeedbackClient.voices()<=6,"Actual native particles fill but never exceed the global budget, with bounded active voices");
                client.options.particles().set(ParticleStatus.MINIMAL);before=ArtFeedbackClient.particles();ArtFeedbackClient.receive(client,new ArtFeedback(dim,Long.MAX_VALUE-1,UUID.randomUUID(),position,-1,2));check(ArtFeedbackClient.particles()==before,"Minimal particle setting has no extra emission");
            });
            c.waitTicks(12);check(c.computeOnClient(client->ArtFeedbackClient.particles()==0),"Finite native particles expire without a deferred queue");
            c.runOnClient(client->{ArtFeedbackClient.receive(client,new ArtFeedback(client.level.dimension().identifier(),Long.MAX_VALUE,new UUID(2,0),client.player.position().add(2,1,0),4,-1));check(ArtFeedbackClient.voices()>0,"Source-removal check first starts a real voice");});
            // Feedback tracking uses actual elapsed time; accelerated world ticks are not milliseconds.
            c.waitFor(client->ArtFeedbackClient.voices()==0);
        }finally{c.runOnClient(client->{client.options.particles().set(oldParticles);client.options.guiScale().set(oldScale);});}
        check(c.computeOnClient(client->ArtFeedbackClient.voices()==0&&ArtFeedbackClient.particles()==0),"Disconnect clears feedback sources");
        System.out.println("WILDCRAFT SECOND ART loaded meal/charge/special models, fallback, mesh set, pose wrap, HUD bounds, server packet, duplicate/minimal/global/expiry/disconnect passed");
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
