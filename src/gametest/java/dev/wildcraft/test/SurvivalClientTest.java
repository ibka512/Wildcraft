package dev.wildcraft.test;

import dev.wildcraft.cooking.*;
import dev.wildcraft.energy.*;
import dev.wildcraft.fabrication.*;
import dev.wildcraft.mechanics.*;
import dev.wildcraft.player.*;
import dev.wildcraft.traversal.*;
import dev.wildcraft.focus.FocusTime;
import dev.wildcraft.fuse.FusionContent;
import dev.wildcraft.registry.WildcraftItems;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import java.util.*;

/** Only raw materials are seeded; every mod item comes from native crafting or fabrication. */
public final class SurvivalClientTest implements FabricClientGameTest {
    private static final BlockPos TABLE=new BlockPos(2,120,0),FIRE=new BlockPos(3,119,2),POT=FIRE.above(),SOURCE=new BlockPos(4,120,3),CHARGER=SOURCE.east(),FACTORY=new BlockPos(3,120,5);
    @Override public void runTest(ClientGameTestContext c){
        TestWorldSave save;int[] retainedEnergy={0};
        try(TestSingleplayerContext w=c.worldBuilder().create()){
            w.getConnection().waitForChunksRender();w.getServer().runCommand("gamemode survival @p");w.getServer().runCommand("time set day");w.getServer().runCommand("weather clear");
            w.getServer().runOnServer(server->{
                var l=server.overworld();var p=server.getPlayerList().getPlayers().getFirst();
                for(int x=-8;x<=8;x++)for(int z=-8;z<=40;z++)l.setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());
                for(int x=-2;x<=2;x++)for(int y=120;y<=126;y++)l.setBlockAndUpdate(new BlockPos(x,y,5),Blocks.STONE.defaultBlockState());
                l.setBlockAndUpdate(TABLE,Blocks.CRAFTING_TABLE.defaultBlockState());l.setBlockAndUpdate(FIRE,Blocks.CAMPFIRE.defaultBlockState());
                p.teleportTo(l,.5,120,.5,Set.of(),0,0,false);p.getInventory().clearContent();p.giveExperienceLevels(20);
                Item[] raw={Items.IRON_INGOT,Items.COPPER_INGOT,Items.REDSTONE,Items.LEATHER,Items.STICK,Items.OAK_PLANKS,Items.FEATHER,Items.STRING,Items.BUCKET,Items.FURNACE,Items.BEETROOT,Items.POTATO,Items.CARROT,Items.BOWL,Items.ARROW,Items.STONE};
                int[] quantities={64,64,64,8,16,16,4,8,1,1,2,2,2,2,4,4};
                for(int i=0;i<raw.length;i++)p.getInventory().setItem(9+i,new ItemStack(raw[i],quantities[i]));p.inventoryMenu.broadcastChanges();
            });
            c.waitFor(client->client.player.getY()>119&&count(client.player.getInventory(),Items.IRON_INGOT)==64);
            useOn(c,TABLE,Direction.UP);c.waitFor(client->client.player.containerMenu.slots.size()==46);
            for(var recipe:List.of("paraglider","cooking_pot","battery","charger","machine_body","fan","fan","wheel","wheel","spring","wing","bow","iron_sword","redstone_block","redstone_block","fabricator"))craft(c,recipe);
            c.takeScreenshot("survival-native-crafted-kit");close(c);
            w.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();check(count(p.getInventory(),Items.IRON_INGOT)==22&&count(p.getInventory(),Items.COPPER_INGOT)==44&&count(p.getInventory(),Items.REDSTONE)==41,"Actual crafts debit the measured 42 iron, 20 copper and 23 redstone exactly");p.giveExperienceLevels(-20);check(PlayerStamina.get(p).highestLevel()==20&&PlayerStamina.get(p).capacity()==140,"Spending current levels retains historical capacity");});
            inventory(c);c.runOnClient(client->{int slot=findMenu(client.player.containerMenu,WildcraftItems.PARAGLIDER,9);client.gameMode.handleContainerInput(0,slot,0,ContainerInput.QUICK_MOVE,client.player);});c.waitFor(client->GliderEquipment.equipped(client.player));close(c);
            hotbar(c,CookingContent.POT_ITEM,0);useOn(c,FIRE,Direction.UP);c.waitFor(client->client.level.getBlockState(POT).is(CookingContent.POT));useOn(c,POT,Direction.UP);c.waitFor(client->client.player.containerMenu instanceof CookingMenu);
            for(int n=0;n<3;n++)insert(c,List.of(Items.BEETROOT,Items.POTATO,Items.CARROT).get(n),n,1,5);insert(c,Items.BOWL,3,1,5);
            c.waitFor(client->client.player.containerMenu.getSlot(4).hasItem());c.runOnClient(client->client.gameMode.handleContainerInput(client.player.containerMenu.containerId,4,0,ContainerInput.QUICK_MOVE,client.player));close(c);
            hotbar(c,CookingContent.MEAL,0);FusionClientSmokeTest.aim(c,new Vec3(.5,140,.5));c.getInput().holdKey(o->o.keyUse);c.waitFor(client->client.player.getAttached(CookingEffects.VIEW)!=null&&client.player.getAttached(CookingEffects.VIEW).warmth()==2);c.getInput().releaseKey(o->o.keyUse);c.waitFor(client->client.player.getMainHandItem().is(Items.BOWL));c.takeScreenshot("survival-native-cooked-and-eaten");
            traversal(c,w);
            hotbar(c,Items.IRON_SWORD,0);inventory(c);c.runOnClient(client->{int slot=findMenu(client.player.containerMenu,Items.STONE,9);client.gameMode.handleContainerInput(0,slot,40,ContainerInput.SWAP,client.player);});close(c);FusionClientSmokeTest.key(c,false);c.waitFor(client->client.player.getMainHandItem().has(FusionContent.VIEW));
            w.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();var target=net.minecraft.world.entity.EntityTypes.HUSK.create(server.overworld(),net.minecraft.world.entity.EntitySpawnReason.COMMAND);target.setNoAi(true);target.setPos(p.getX(),120,p.getZ()-2);server.overworld().addFreshEntity(target);});
            c.waitFor(client->!client.level.getEntitiesOfClass(net.minecraft.world.entity.monster.zombie.Husk.class,client.player.getBoundingBox().inflate(4)).isEmpty());
            var target=c.computeOnClient(client->client.level.getEntitiesOfClass(net.minecraft.world.entity.monster.zombie.Husk.class,client.player.getBoundingBox().inflate(4)).getFirst().getEyePosition());FusionClientSmokeTest.aim(c,target);c.waitFor(client->client.hitResult instanceof EntityHitResult);c.waitTicks(20);c.getInput().holdKeyFor(o->o.keyAttack,1);c.waitFor(client->client.player.getMainHandItem().get(FusionContent.VIEW).uses()==31);
            station(c,w,new Vec3(3.5,120,3.5));hotbar(c,Items.REDSTONE_BLOCK,0);useOn(c,SOURCE.below(),Direction.UP);c.waitFor(client->client.level.getBlockState(SOURCE).is(Blocks.REDSTONE_BLOCK));hotbar(c,EnergyContent.CHARGER_ITEM,0);useOn(c,CHARGER.below(),Direction.UP);c.waitFor(client->client.level.getBlockState(CHARGER).is(EnergyContent.CHARGER));useOn(c,CHARGER,Direction.UP);c.waitFor(client->client.player.containerMenu instanceof ChargerMenu);insert(c,EnergyContent.BATTERY,0,1,1);c.waitFor(client->Batteries.energy(client.player.containerMenu.getSlot(0).getItem())==1000);c.takeScreenshot("survival-native-charged-crafted-battery");c.runOnClient(client->client.gameMode.handleContainerInput(client.player.containerMenu.containerId,0,0,ContainerInput.QUICK_MOVE,client.player));close(c);
            station(c,w,new Vec3(.5,120,8.5));hotbar(c,MechanicsContent.BODY,0);useOn(c,new BlockPos(0,119,10),Direction.UP);c.waitFor(client->MechanicsClientSmokeTest.find(client.level)!=null);
            for(int n:new int[]{0,2,4,5,3}){stationAtNode(c,w,n);hotbar(c,switch(n){case 0->EnergyContent.BATTERY;case 2->MechanicsContent.FAN;case 3->MechanicsContent.WING;default->MechanicsContent.WHEEL;},0);MechanicsClientSmokeTest.aim(c,MachineNodes.point(n));MechanicsClientSmokeTest.click(c);final int node=n;c.waitFor(client->MechanicsClientSmokeTest.find(client.level).kind(node)>0);}
            stationAtNode(c,w,2);emptyHand(c);MechanicsClientSmokeTest.aim(c,MachineNodes.point(2));MechanicsClientSmokeTest.click(c);c.waitFor(client->client.player.getVehicle() instanceof MachineEntity);c.runOnClient(client->client.player.setXRot(0));c.takeScreenshot("survival-first-person-clear-riding");
            c.getInput().holdKeyFor(dev.wildcraft.client.mechanics.MachinePresentation.TOGGLE,1);c.waitFor(client->MechanicsClientSmokeTest.find(client.level).enabled());double start=c.computeOnClient(client->MechanicsClientSmokeTest.find(client.level).getZ());c.getInput().holdKeyFor(o->o.keyUp,20);
            check(c.computeOnClient(client->MechanicsClientSmokeTest.find(client.level).getZ())>start+1,"Real W input drives crafted vehicle");c.getInput().holdKeyFor(dev.wildcraft.client.mechanics.MachinePresentation.TOGGLE,1);c.waitFor(client->!MechanicsClientSmokeTest.find(client.level).enabled());retainedEnergy[0]=c.computeOnClient(client->MechanicsClientSmokeTest.find(client.level).energy());check(retainedEnergy[0]>0&&retainedEnergy[0]<1000,"Real powered ride spends finite energy");c.getInput().holdKeyFor(o->o.keyShift,1);c.waitFor(client->client.player.getVehicle()==null);w.getServer().runOnServer(server->MechanicsClientSmokeTest.find(server.overworld()).setDeltaMovement(Vec3.ZERO));
            for(int n:new int[]{4,5,3,2,0}){stationAtNode(c,w,n);emptyHand(c);MechanicsClientSmokeTest.sneakClick(c,MachineNodes.point(n));final int node=n;c.waitFor(client->MechanicsClientSmokeTest.find(client.level).kind(node)==0);}
            stationAtNode(c,w,2);emptyHand(c);c.getInput().holdKey(o->o.keyShift);c.waitTicks(3);MechanicsClientSmokeTest.aim(c,MachineNodes.point(2));c.getInput().holdKeyFor(o->o.keyAttack,1);c.getInput().releaseKey(o->o.keyShift);c.waitFor(client->MechanicsClientSmokeTest.find(client.level)==null);check(c.computeOnClient(client->count(client.player.getInventory(),MechanicsContent.BODY))==1,"Native empty-body recovery returns one crafted body");
            station(c,w,new Vec3(3.5,120,3.5));hotbar(c,FabricationContent.ITEM,0);useOn(c,FACTORY.below(),Direction.UP);c.waitFor(client->client.level.getBlockState(FACTORY).is(FabricationContent.BLOCK));useOn(c,FACTORY,Direction.UP);c.waitFor(client->client.player.containerMenu instanceof FabricatorMenu);insert(c,Items.COPPER_INGOT,0,4,3);insert(c,Items.REDSTONE,1,2,3);c.runOnClient(client->client.gameMode.handleInventoryButtonClick(client.player.containerMenu.containerId,0));c.waitFor(client->client.player.containerMenu.getSlot(2).hasItem());c.takeScreenshot("survival-native-fabricated-supply");c.runOnClient(client->client.gameMode.handleContainerInput(client.player.containerMenu.containerId,2,0,ContainerInput.QUICK_MOVE,client.player));close(c);
            w.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();check(count(p.getInventory(),Items.IRON_INGOT)==22&&count(p.getInventory(),Items.COPPER_INGOT)==40&&count(p.getInventory(),Items.REDSTONE)==39,"One native fabrication spends exactly four copper and two redstone");check(PlayerStamina.get(p).highestLevel()==20&&count(p.getInventory(),Items.ARROW)==3&&count(p.getInventory(),Items.BOWL)==2,"Exploration keeps peak, shoots one arrow and returns the cooked bowl");});save=w.getWorldSave();
        }finally{release(c);}
        try(TestSingleplayerContext w=save.open()){
            w.getConnection().waitForChunksRender();w.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();check(GliderEquipment.equipped(p)&&PlayerStamina.get(p).highestLevel()==20&&count(p.getInventory(),MechanicsContent.BODY)==1,"Real disk reload retains native crafted kit and peak");check(java.util.stream.IntStream.range(0,36).mapToObj(i->p.getInventory().getItem(i)).anyMatch(s->s.is(EnergyContent.BATTERY)&&Batteries.energy(s)==retainedEnergy[0]),"Recovered finite battery retains its exact charge after disk reload");});
        }
        System.out.println("WILDCRAFT SURVIVAL raw-only fixture/native crafts/peak/equip/pot/meal/climb/glide/bow/fuse/charge/install/drive/exact recovery/fabrication/disk reload passed");
    }
    private static void traversal(ClientGameTestContext c,TestSingleplayerContext w){
        hotbar(c,Items.BOW,1);hotbar(c,Items.IRON_SWORD,0);station(c,w,new Vec3(.5,120,4.68));c.runOnClient(client->{client.player.setYRot(0);client.player.setXRot(0);});c.getInput().holdKey(dev.wildcraft.client.input.ClimbControls.CLIMB);c.getInput().holdKey(o->o.keyUp);c.waitFor(client->Climbing.active(client.player)&&client.player.getY()>124,100);c.getInput().releaseKey(o->o.keyUp);c.getInput().releaseKey(dev.wildcraft.client.input.ClimbControls.CLIMB);c.runOnClient(client->client.player.setYRot(180));c.getInput().holdKeyFor(o->o.keyJump,1);c.waitFor(client->Gliding.active(client.player));c.waitTicks(16);c.takeScreenshot("survival-native-climb-to-glide");c.getInput().holdKeyFor(o->o.keyHotbarSlots[1],1);c.waitFor(client->!Gliding.active(client.player)&&client.player.getMainHandItem().is(Items.BOW));c.getInput().holdKey(o->o.keyUse);c.waitFor(client->FocusTime.active(client.player));c.waitTicks(8);c.getInput().releaseKey(o->o.keyUse);c.waitFor(client->!FocusTime.active(client.player));c.waitFor(client->client.player.onGround(),100);
        w.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();check(server.tickRateManager().tickrate()==20&&count(p.getInventory(),Items.ARROW)==3,"Real traversal restores rate and consumes exactly one arrow");});
    }
    private static void craft(ClientGameTestContext c,String recipe){var pattern=SurvivalRecipes.read(recipe);for(int n=0;n<9;n++)if(!pattern.inputs().get(n).isEmpty())insert(c,pattern.inputs().get(n).getItem(),n+1,1,10);c.waitFor(client->client.player.containerMenu.getSlot(0).getItem().is(pattern.output()));int before=c.computeOnClient(client->count(client.player.getInventory(),pattern.output()));c.runOnClient(client->client.gameMode.handleContainerInput(client.player.containerMenu.containerId,0,0,ContainerInput.QUICK_MOVE,client.player));c.waitFor(client->count(client.player.getInventory(),pattern.output())==before+1);System.out.println("SURVIVAL native crafted "+recipe);}
    private static void insert(ClientGameTestContext c,Item item,int destination,int quantity,int inventoryStart){c.runOnClient(client->{var menu=client.player.containerMenu;int source=findMenu(menu,item,inventoryStart);client.gameMode.handleContainerInput(menu.containerId,source,0,ContainerInput.PICKUP,client.player);for(int i=0;i<quantity;i++)client.gameMode.handleContainerInput(menu.containerId,destination,1,ContainerInput.PICKUP,client.player);client.gameMode.handleContainerInput(menu.containerId,source,0,ContainerInput.PICKUP,client.player);});c.waitTicks(2);}
    private static int findMenu(net.minecraft.world.inventory.AbstractContainerMenu menu,Item item,int start){for(int n=start;n<menu.slots.size();n++)if(menu.getSlot(n).getItem().is(item))return n;throw new AssertionError("Missing real ingredient "+item);}
    private static void hotbar(ClientGameTestContext c,Item item,int slot){inventory(c);c.runOnClient(client->{var menu=client.player.containerMenu;int source=findMenu(menu,item,9);client.gameMode.handleContainerInput(0,source,slot,ContainerInput.SWAP,client.player);client.player.getInventory().setSelectedSlot(slot);});close(c);c.waitFor(client->client.player.getMainHandItem().is(item));}
    private static void emptyHand(ClientGameTestContext c){inventory(c);c.runOnClient(client->{for(int n=9;n<36;n++)if(client.player.containerMenu.getSlot(n).getItem().isEmpty()){client.gameMode.handleContainerInput(0,n,8,ContainerInput.SWAP,client.player);client.player.getInventory().setSelectedSlot(8);return;}throw new AssertionError("Fixture requires one free real inventory slot");});close(c);c.waitFor(client->client.player.getMainHandItem().isEmpty());}
    private static void inventory(ClientGameTestContext c){c.runOnClient(client->client.gui.setScreen(new InventoryScreen(client.player)));c.waitTicks(2);}
    private static void close(ClientGameTestContext c){c.runOnClient(client->client.player.closeContainer());c.waitFor(client->client.gui.screen()==null);c.waitTicks(2);}
    private static void useOn(ClientGameTestContext c,BlockPos pos,Direction face){c.runOnClient(client->client.gameMode.useItemOn(client.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos).add(0,.5,0),face,pos,false)));c.waitTicks(3);}
    private static void station(ClientGameTestContext c,TestSingleplayerContext w,Vec3 pos){w.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();p.teleportTo(server.overworld(),pos.x,pos.y,pos.z,Set.of(),0,0,false);p.setDeltaMovement(Vec3.ZERO);});c.waitFor(client->client.player.position().distanceTo(pos)<.35);c.waitTicks(2);}
    private static void stationAtNode(ClientGameTestContext c,TestSingleplayerContext w,int n){var pos=c.computeOnClient(client->{var b=MechanicsClientSmokeTest.find(client.level);return b.position().add(switch(n){case 4->new Vec3(-2,0,0);case 5->new Vec3(2,0,0);case 3->new Vec3(0,0,2);default->new Vec3(0,0,-2);});});station(c,w,pos);}
    private static int count(net.minecraft.world.entity.player.Inventory inventory,Item item){int count=0;for(int i=0;i<36;i++)if(inventory.getItem(i).is(item))count+=inventory.getItem(i).getCount();return count;}
    private static void release(ClientGameTestContext c){c.getInput().releaseKey(o->o.keyUse);c.getInput().releaseKey(o->o.keyUp);c.getInput().releaseKey(o->o.keyJump);c.getInput().releaseKey(o->o.keyShift);c.getInput().releaseKey(dev.wildcraft.client.input.ClimbControls.CLIMB);}
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
