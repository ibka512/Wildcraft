package dev.wildcraft.test;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildcraft.client.animation.*;
import dev.wildcraft.client.render.*;
import dev.wildcraft.combat.*;
import dev.wildcraft.equipment.*;
import dev.wildcraft.fuse.*;
import dev.wildcraft.network.MeleeView;
import dev.wildcraft.player.PlayerStamina;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import org.joml.Matrix4f;

/** Actual use registration and native hotbar input, real production skin/body submissions and rig matrices. */
public final class EquipmentClientTest implements FabricClientGameTest {
    private static final Item[] WEAPONS={Items.IRON_SWORD,Items.IRON_AXE,Items.TRIDENT,Items.MACE,Items.IRON_SPEAR};
    @Override public void runTest(ClientGameTestContext c){
        var id=new AtomicInteger();
        try(TestSingleplayerContext w=c.worldBuilder().create()){
            w.getConnection().waitForChunksRender();w.getServer().runCommand("gamemode survival @p");
            w.getServer().runOnServer(s->{for(int x=-5;x<=5;x++)for(int z=-5;z<=9;z++)s.overworld().setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(.5,120,.5);p.setYRot(0);p.setXRot(0);for(int slot=0;slot<5;slot++){var host=new ItemStack(WEAPONS[slot]);FusionItems.attach(host,new ItemStack(Items.COBBLESTONE),s.registryAccess());p.getInventory().setItem(slot,host);}p.getInventory().setItem(5,new ItemStack(Items.BOW));p.getInventory().setItem(6,new ItemStack(Items.SHIELD));p.getInventory().setItem(8,new ItemStack(Items.TORCH));p.getInventory().setItem(9,new ItemStack(Items.ARROW,64));p.inventoryMenu.broadcastChanges();var m=EntityTypes.HUSK.create(s.overworld(),EntitySpawnReason.COMMAND);m.setPos(.5,120,2.7);m.setNoAi(true);m.setNoGravity(true);m.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);m.setHealth(200);s.overworld().addFreshEntity(m);id.set(m.getId());});
            c.waitFor(client->client.player.getY()>119&&dev.wildcraft.client.input.ActionInput.tickFrame().gameplay());c.runOnClient(client->{client.options.setCameraType(CameraType.FIRST_PERSON);client.player.setYRot(0);client.player.setXRot(0);});
            select(c,7,Items.AIR);c.waitTicks(3);
            for(int slot=0;slot<5;slot++){
                int index=slot;var item=WEAPONS[slot];select(c,slot,item);w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();PlayerStamina.fill(p);var m=(LivingEntity)s.overworld().getEntity(id.get());m.setPos(.5,120,2.7);m.setHealth(200);m.setInvulnerableTime(0);m.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);});c.waitTicks(25);

                c.runOnClient(client->System.out.println("C6D BEFORE "+item+" pos="+client.player.position()+" hit="+client.hitResult+" yaw="+client.player.getYRot()+" melee="+MeleeActions.view(client.player)));
                c.waitFor(client->client.hitResult instanceof net.minecraft.world.phys.EntityHitResult e&&e.getEntity().getId()==id.get());
                c.getInput().holdKey(v->v.keyAttack);c.waitTicks(2);
                c.runOnClient(client->System.out.println("C6D INPUT key="+client.options.keyAttack.isDown()+" frame="+dev.wildcraft.client.input.ActionInput.tickFrame()+" eligible="+MeleeActions.eligible(client.player)+" paused="+client.isPaused()+" focus="+client.isWindowActive()+" gui="+client.gui.screen()+" melee="+MeleeActions.view(client.player)));
                w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();System.out.println("C6D SERVER eligible="+MeleeActions.eligible(p)+" melee="+MeleeActions.view(p)+" hand="+p.getMainHandItem());});
                c.waitFor(client->MeleeActions.view(client.player).phase()==MeleeView.CHARGE);
                c.getInput().releaseKey(v->v.keyAttack);
                c.waitFor(client->client.player.getMainHandItem().getDamageValue()==(index==1?2:1));c.waitTicks(24);
                check(c.computeOnClient(client->client.player.getAttached(BackEquipment.VISUAL).melee().owner()==1),"Actual attack registers the actual held reference");
                EquipmentAudit.begin();select(c,7,Items.AIR);c.waitFor(client->EquipmentAudit.holster[0]>0);c.takeScreenshot("c6d-first-return-"+item.toString().replace(':','-'));c.waitTicks(12);
                EquipmentAudit.begin();select(c,slot,item);c.waitFor(client->EquipmentAudit.draw[0]>0);c.waitTicks(2);c.takeScreenshot("c6d-first-draw-"+item.toString().replace(':','-'));rig(c,0,false);c.waitTicks(12);
                w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(p.getInventory().getItem(index).getDamageValue()==(index==1?2:1)&&p.getInventory().getItem(index).get(FusionContent.DATA).remaining()==31,"Repeated draw/return does not alter wear or Fuse");check(PlayerStamina.get(p).stamina()==100,"Visual transitions add no stamina transaction");});
                w.getServer().runOnServer(s->s.overworld().getEntity(id.get()).setPos(4.5,120,8));c.runOnClient(client->client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));EquipmentAudit.begin();select(c,7,Items.AIR);c.waitFor(client->EquipmentAudit.body[0]>0);c.waitTicks(2);c.takeScreenshot("c6d-third-return-"+item.toString().replace(':','-'));c.waitTicks(12);
                EquipmentAudit.begin();select(c,slot,item);c.waitFor(client->EquipmentAudit.body[0]>0);c.waitTicks(2);c.takeScreenshot("c6d-third-draw-"+item.toString().replace(':','-'));c.waitTicks(12);c.runOnClient(client->client.options.setCameraType(CameraType.FIRST_PERSON));
            }
            for(int slot:new int[]{5,6}){
                var item=slot==5?Items.BOW:Items.SHIELD;select(c,slot,item);c.waitTicks(20);c.getInput().holdKey(v->v.keyUse);c.waitFor(client->client.player.isUsingItem());c.waitTicks(3);c.getInput().releaseKey(v->v.keyUse);c.waitFor(client->!client.player.isUsingItem());c.waitTicks(12);cycle(c,slot,item,slot==5?2:1,"native-"+item.toString().replace(':','-'));
            }
            // Real existing shield reference is transferred to/from the offhand; the main empty hand stays independent.
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setItemInHand(InteractionHand.OFF_HAND,p.getInventory().removeItemNoUpdate(6));p.inventoryMenu.broadcastChanges();});EquipmentAudit.begin();c.waitFor(client->client.player.getOffhandItem().is(Items.SHIELD)&&EquipmentAudit.draw[1]>0);c.takeScreenshot("c6d-offhand-shield-draw");c.waitTicks(12);
            EquipmentAudit.begin();w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.getInventory().setItem(6,p.getInventory().removeItemNoUpdate(40));p.inventoryMenu.broadcastChanges();});c.waitFor(client->client.player.getOffhandItem().isEmpty()&&EquipmentAudit.holster[1]>0);c.takeScreenshot("c6d-offhand-shield-empty-return");c.waitTicks(12);
            // A real crossbow shares the ranged category but has an independent native model/use path.
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.getInventory().setItem(5,new ItemStack(Items.CROSSBOW));p.inventoryMenu.broadcastChanges();});select(c,5,Items.CROSSBOW);c.waitTicks(20);c.getInput().holdKey(v->v.keyUse);c.waitTicks(30);c.getInput().releaseKey(v->v.keyUse);c.waitTicks(12);cycle(c,5,Items.CROSSBOW,2,"crossbow");
            // Left actual dominant hand and occupied supporting hand. No arm can erase a held torch.
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setMainArm(HumanoidArm.LEFT);p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.TORCH));p.inventoryMenu.broadcastChanges();});c.waitFor(client->client.player.getMainArm()==HumanoidArm.LEFT&&client.player.getOffhandItem().is(Items.TORCH));cycle(c,4,Items.IRON_SPEAR,0,"left-occupied-torch");
            // A replacement item owns the hand while the recorded weapon settles on the back.
            select(c,4,Items.IRON_SPEAR);c.waitTicks(12);EquipmentAudit.begin();select(c,8,Items.TORCH);c.waitTicks(3);check(c.computeOnClient(client->EquipmentAudit.holster[0]==0&&BackTransitions.forArm(state(client),client.player.getMainArm())==null),"Replacement torch is never carried back or hidden by a return arm");c.takeScreenshot("c6d-replacement-torch-owns-hand");c.waitTicks(12);
            // Immediate actual attack cancels equipment reach before synchronized melee state catches up.
            select(c,7,Items.AIR);c.waitTicks(12);select(c,4,Items.IRON_SPEAR);c.getInput().holdKey(v->v.keyAttack);c.waitFor(client->MeleeActions.view(client.player).phase()==MeleeView.CHARGE);EquipmentAudit.begin();c.waitTicks(2);check(c.computeOnClient(client->EquipmentAudit.draw[0]==0&&BackTransitions.forArm(state(client),client.player.getMainArm())==null),"Attack immediately takes ownership without waiting for draw completion");w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(p.getInventory().getSelectedSlot()==4&&MeleeActions.view(p).kind()==MeleeWeapon.SPEAR,"Native slot reaches the server before the correct weapon charge intent");});c.setScreen(()->new InventoryScreen(net.minecraft.client.Minecraft.getInstance().player));c.getInput().releaseKey(v->v.keyAttack);c.setScreen(()->null);c.waitTicks(12);
            select(c,7,Items.AIR);c.waitTicks(12);select(c,4,Items.IRON_SPEAR);c.setScreen(()->new InventoryScreen(net.minecraft.client.Minecraft.getInstance().player));c.waitTicks(2);c.setScreen(()->null);EquipmentAudit.begin();c.waitTicks(3);check(c.computeOnClient(client->EquipmentAudit.draw[0]==0),"GUI interruption never replays the old draw");
            // Pause freezes actual transition progress; manual slow ticks do not stretch its duration.
            select(c,7,Items.AIR);c.waitTicks(12);EquipmentAudit.begin();select(c,4,Items.IRON_SPEAR);c.waitFor(client->EquipmentAudit.draw[0]>0);c.runOnClient(client->client.gui.setScreen(new PauseScreen(true)));c.waitFor(client->client.isPaused());
            float before=c.computeOnClient(client->progress(state(client),0));check(before<1,"Pause retains a live draw");long until=System.nanoTime()+450_000_000L;c.waitFor(client->System.nanoTime()>until);check(c.computeOnClient(client->Math.abs(progress(state(client),0)-before)<.001F),"Paused actual time advances neither draw nor return");c.setScreen(()->null);c.waitTicks(12);
            w.getServer().runCommand("tick rate 5");select(c,7,Items.AIR);c.waitTicks(12);EquipmentAudit.begin();long started=System.nanoTime();select(c,4,Items.IRON_SPEAR);c.waitFor(client->EquipmentAudit.draw[0]>0);c.waitFor(client->progress(state(client),0)==1);check(System.nanoTime()-started<1_200_000_000L,"Equipment reaches completion in actual time at 5 TPS");w.getServer().runCommand("tick rate 20");
            // Real use takes priority; native crossbow keeps its model and receives no draw skin arm.
            select(c,7,Items.AIR);c.waitTicks(12);select(c,5,Items.CROSSBOW);c.getInput().holdKey(v->v.keyUse);c.waitTicks(2);EquipmentAudit.begin();c.waitTicks(2);check(c.computeOnClient(client->EquipmentAudit.draw[2]==0),"Native use wins over the ranged draw");c.getInput().releaseKey(v->v.keyUse);c.waitTicks(12);
            // Same-looking spare remains separate; merely selecting it cannot become recent equipment.
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.getInventory().setItem(0,p.getInventory().getItem(4).copy());p.inventoryMenu.broadcastChanges();});select(c,7,Items.AIR);c.waitTicks(12);EquipmentAudit.begin();select(c,0,Items.IRON_SPEAR);c.waitTicks(8);check(c.computeOnClient(client->EquipmentAudit.draw[0]==0&&!BackEquipment.view(client.player).melee().isEmpty()),"Identical spare has no recorded draw and cannot hide the genuine back reference");
            // Natural cloak layout uses the real elytra equipment slot, including armor alignment.
            w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.ELYTRA));p.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.IRON_HELMET));p.setItemSlot(EquipmentSlot.LEGS,new ItemStack(Items.IRON_LEGGINGS));p.setItemSlot(EquipmentSlot.FEET,new ItemStack(Items.IRON_BOOTS));p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);p.inventoryMenu.broadcastChanges();});c.waitFor(client->client.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)&&client.player.getOffhandItem().isEmpty());c.runOnClient(client->client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));select(c,7,Items.AIR);c.waitTicks(12);EquipmentAudit.begin();select(c,4,Items.IRON_SPEAR);c.waitFor(client->EquipmentAudit.body[0]>0);c.takeScreenshot("c6d-left-elytra-waist-draw");rig(c,0,true);c.waitTicks(12);
            // Rapid reversals always reach the final actual ownership without an animation lock.
            for(int i=0;i<4;i++){select(c,7,Items.AIR);select(c,4,Items.IRON_SPEAR);}c.waitTicks(15);check(c.computeOnClient(client->progress(state(client),0)==1&&BackEquipment.view(client.player).melee().isEmpty()),"Rapid switches terminate at true hand ownership");
            controlledQualification(c);
            System.out.println("C6D EQUIPMENT actual five weapon use registrations, native hotbar draw/empty-return/body, bow/shield/crossbow, occupied/left/waist/armor, attack/use/GUI/pause/5TPS/rapid/spare and controlled skin matrices PASS");
        }finally{EquipmentAudit.enabled=false;c.getInput().releaseKey(v->v.keyAttack);c.getInput().releaseKey(v->v.keyUse);c.setScreen(()->null);c.runOnClient(client->client.options.setCameraType(CameraType.FIRST_PERSON));}
    }
    private static AvatarRenderState state(net.minecraft.client.Minecraft client){return (AvatarRenderState)client.getEntityRenderDispatcher().getRenderer(client.player).createRenderState(client.player,1);}
    private static float progress(AvatarRenderState state,int category){var all=state.getData(BackTransitions.SETTLES);return all==null||all[category]==null?1:all[category].progress();}
    private static void select(ClientGameTestContext c,int slot,Item item){c.getInput().holdKeyFor(v->v.keyHotbarSlots[slot],1);c.waitFor(client->client.player.getInventory().getSelectedSlot()==slot&&client.player.getMainHandItem().is(item));}
    private static void cycle(ClientGameTestContext c,int slot,Item item,int category,String label){select(c,7,Items.AIR);c.waitTicks(12);EquipmentAudit.begin();select(c,slot,item);c.waitFor(client->EquipmentAudit.draw[category]>0);c.waitTicks(2);c.takeScreenshot("c6d-first-draw-"+label);rig(c,category,false);c.waitTicks(12);EquipmentAudit.begin();select(c,7,Items.AIR);c.waitFor(client->EquipmentAudit.holster[category]>0);c.takeScreenshot("c6d-first-return-"+label);c.waitTicks(12);}
    private static void rig(ClientGameTestContext c,int category,boolean waist){c.runOnClient(client->{
        EquipmentAudit.enabled=false;var avatar=state(client);var skin=avatar.skin;var item=client.player.getMainHandItem();var original=item.copy();avatar.setData(CharacterPoses.POSE,null);avatar.setData(CombatPoses.LAYER,null);avatar.setData(MeleePoses.LAYER,MeleeView.OFF);avatar.setData(BackTransitions.WAIST,waist);
        for(boolean slim:new boolean[]{false,true})for(var arm:HumanoidArm.values())for(boolean sleeves:new boolean[]{false,true}){
            avatar.mainArm=arm;avatar.skin=net.minecraft.world.entity.player.PlayerSkin.insecure(skin.body(),null,null,slim?PlayerModelType.SLIM:PlayerModelType.WIDE);avatar.showLeftSleeve=avatar.showRightSleeve=sleeves;avatar.rightHandItemStack=arm==HumanoidArm.RIGHT?item:client.player.getOffhandItem();avatar.leftHandItemStack=arm==HumanoidArm.LEFT?item:client.player.getOffhandItem();var all=new BackTransitions.Settle[3];all[category]=new BackTransitions.Settle(category,1,1,.4F);avatar.setData(BackTransitions.SETTLES,all);
            var hands=new FirstPersonHandsAndItemsRenderState();hands.mainHandItem=item;hands.offHandItem=client.player.getOffhandItem();client.getItemModelResolver().updateForLiving(hands.mainHandRenderState,item,arm==HumanoidArm.RIGHT?ItemDisplayContext.FIRST_PERSON_RIGHT_HAND:ItemDisplayContext.FIRST_PERSON_LEFT_HAND,client.player);
            var parts=new ArrayList<ModelPart>();SubmitNodeCollector collector=(SubmitNodeCollector)Proxy.newProxyInstance(SubmitNodeCollector.class.getClassLoader(),new Class<?>[]{SubmitNodeCollector.class},(o,m,args)->{if(m.getName().equals("submitModelPart")){var part=(ModelPart)args[0];parts.add(part);check(((PoseStack)args[1]).last().pose().isFinite(),"Skin submission has finite native matrix");check(part.getChild(arm==HumanoidArm.RIGHT&&parts.size()==1||arm==HumanoidArm.LEFT&&parts.size()==2?"right_sleeve":"left_sleeve").visible==sleeves,"Native sleeves follow the real visibility mask");}return null;});
            var poses=new PoseStack();var before=new Matrix4f(poses.last().pose());check(EquipmentHands.active(avatar,hands,InteractionHand.MAIN_HAND,item),"Actual corresponding draw item qualifies");EquipmentHands.place(avatar,hands,InteractionHand.MAIN_HAND,hands.mainHandRenderState,item,poses,.8F);var placed=new Matrix4f(poses.last().pose());EquipmentHands.submit(avatar,hands,InteractionHand.MAIN_HAND,hands.mainHandRenderState,item,poses,collector);var kind=MeleeWeapon.of(item);boolean support=(kind==MeleeWeapon.TRIDENT||kind==MeleeWeapon.SPEAR||kind==MeleeWeapon.MACE)&&hands.offHandItem.isEmpty();check(parts.size()==(support?2:1)&&poses.last().pose().equals(placed),"Exact free/occupied skin geometry count and restored parent transform");check(ItemStack.isSameItemSameComponents(original,item)&&item.getCount()==original.getCount(),"Skin/body rendering never changes stack components or quantity");
            var model=new PlayerModel(LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE,slim),64,64).bakeRoot(),slim);avatar.setData(BackTransitions.SETTLES,null);model.setupAnim(avatar);float head=model.head.yRot,otherX=(arm==HumanoidArm.RIGHT?model.leftArm:model.rightArm).xRot;avatar.setData(BackTransitions.SETTLES,all);model.setupAnim(avatar);check(model.head.yRot==head&&(support||(arm==HumanoidArm.RIGHT?model.leftArm:model.rightArm).xRot==otherX),"Independent head and occupied/single-handed other arm stay untouched");var armor=new HumanoidModel<AvatarRenderState>(LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE,0),64,32).bakeRoot());armor.setupAnim(avatar);check(Math.abs((arm==HumanoidArm.RIGHT?armor.rightArm:armor.leftArm).zRot-(arm==HumanoidArm.RIGHT?model.rightArm:model.leftArm).zRot)<.001,"Actual armor setup consumes the same reach once");
            all[category]=new BackTransitions.Settle(category,3,1,.4F);avatar.rightHandItemStack=avatar.leftHandItemStack=ItemStack.EMPTY;parts.clear();poses=new PoseStack();EquipmentHands.empty(avatar,hands,InteractionHand.MAIN_HAND,poses,collector);check(parts.size()==1&&poses.last().pose().equals(before),"Empty return has one skin arm and restores parent");
        }
        EquipmentAudit.begin();
    });}
    private static void controlledQualification(ClientGameTestContext c){c.runOnClient(client->{var a=state(client);var h=new FirstPersonHandsAndItemsRenderState();h.mainHandItem=client.player.getMainHandItem();var all=new BackTransitions.Settle[3];all[0]=new BackTransitions.Settle(0,1,1,.4F);a.setData(BackTransitions.SETTLES,all);a.isUsingItem=true;check(!EquipmentHands.active(a,h,InteractionHand.MAIN_HAND,h.mainHandItem),"Native use priority");a.isUsingItem=false;h.isScoping=true;check(!EquipmentHands.active(a,h,InteractionHand.MAIN_HAND,h.mainHandItem),"Scoping priority");h.isScoping=false;a.isSpectator=true;check(!EquipmentHands.active(a,h,InteractionHand.MAIN_HAND,h.mainHandItem),"Spectator priority");a.isSpectator=false;a.isAutoSpinAttack=true;check(!EquipmentHands.active(a,h,InteractionHand.MAIN_HAND,h.mainHandItem)&&BackTransitions.forArm(a,a.mainArm)==null,"Riptide owns first and third-person arms");
        check(!BackTransitions.switched(new BackVisual.Entry(8,3),new BackVisual.Entry(9,1))&&!BackTransitions.switched(new BackVisual.Entry(8,1),new BackVisual.Entry(8,2))&&!BackTransitions.switched(new BackVisual.Entry(0,3),new BackVisual.Entry(0,1)),"New reference, direct hand transfer and empty token never synthesize draw");});}
    private static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
}
