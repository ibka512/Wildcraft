package dev.wildcraft.test;

import dev.wildcraft.energy.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/** Exercises the actual loaded item atlas and baked particle material, not JSON structure. */
public final class BatteryAtlasClientTest implements FabricClientGameTest {
    private static final Identifier BATTERY=Identifier.fromNamespaceAndPath("wildcraft","entity/battery");
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    @Override public void runTest(ClientGameTestContext c){
        try(var w=c.worldBuilder().create()){
            w.getConnection().waitForChunksRender();
            w.getServer().runOnServer(s->{
                var p=s.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);p.teleportTo(.5,120,.5);p.getInventory().clearContent();
                for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)s.overworld().setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());
                var stack=new ItemStack(EnergyContent.BATTERY);Batteries.charge(stack,1000);p.setItemInHand(InteractionHand.MAIN_HAND,stack);p.inventoryMenu.broadcastChanges();
            });
            c.waitFor(v->v.player.getMainHandItem().is(EnergyContent.BATTERY)&&Batteries.energy(v.player.getMainHandItem())==1000);
            c.runOnClient(v->v.options.setCameraType(CameraType.FIRST_PERSON));c.waitTicks(10);c.takeScreenshot("battery-native-first-person");
            c.runOnClient(v->{
                var atlas=v.getAtlasManager().getAtlasOrThrow(Identifier.fromNamespaceAndPath("minecraft","items"));var sprite=atlas.getSprite(BATTERY);
                check(sprite.contents().name().equals(BATTERY),"Real item atlas battery sprite is missing: actual="+sprite.contents().name()+" atlas="+atlas.location());
                check(sprite.contents().width()==32&&sprite.contents().height()==32,"Adopted32px source remains unscaled and unmodified");
                for(int n=0;n<=18;n++){
                    var stack=new ItemStack(EnergyContent.BATTERY);Batteries.charge(stack,(int)Math.ceil(n*1000.0/18));var state=new ItemStackRenderState();v.getItemModelResolver().updateForLiving(state,stack,ItemDisplayContext.GUI,v.player);
                    var name=state.pickParticleMaterial(v.level.getRandom()).sprite().contents().name();check(name.toString().equals("wildcraft:item/battery_charge_"+n),"Actual19-step GUI charge boundary: "+n+" actual="+name);
                }
                for(var context:new ItemDisplayContext[]{ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,ItemDisplayContext.FIRST_PERSON_LEFT_HAND,ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,ItemDisplayContext.THIRD_PERSON_LEFT_HAND,ItemDisplayContext.GROUND}){
                    var state=new ItemStackRenderState();v.getItemModelResolver().updateForLiving(state,v.player.getMainHandItem(),context,v.player);var actual=state.pickParticleMaterial(v.level.getRandom()).sprite();
                    check(actual.contents().name().equals(BATTERY),"Real special-base particle material "+context+": "+actual.contents().name());check(actual.atlasLocation().equals(TextureAtlas.LOCATION_ITEMS),"Actual special-base sprite uses item atlas");
                }
                for(var item:new Item[]{Items.IRON_SWORD,Items.DIAMOND,Items.SHIELD,Items.STONE}){
                    var state=new ItemStackRenderState();v.getItemModelResolver().updateForLiving(state,new ItemStack(item),ItemDisplayContext.GUI,v.player);check(!state.pickParticleMaterial(v.level.getRandom()).sprite().contents().name().toString().equals("minecraft:missingno"),"Vanilla atlas sources survive merged extension: "+item);
                }
                v.gui.setScreen(new InventoryScreen(v.player));
            });
            c.waitTicks(3);c.takeScreenshot("battery-native-inventory");c.runOnClient(v->v.gui.setScreen(null));
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(Batteries.energy(p.getMainHandItem())==1000,"Model observation cannot debit battery");});
            System.out.println("C7_BATTERY_ATLAS_PASS actual sprite32px /GUI19/held-ground5/vanilla4/native battery1000 unchanged");
        }
    }
}
