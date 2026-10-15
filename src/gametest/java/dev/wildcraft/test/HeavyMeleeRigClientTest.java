package dev.wildcraft.test;
import dev.wildcraft.client.animation.rig.*;
import dev.wildcraft.client.combat.CombatOptions;
import dev.wildcraft.client.input.ActionInput;
import dev.wildcraft.combat.*;
import dev.wildcraft.fuse.*;
import dev.wildcraft.network.MeleeView;
import dev.wildcraft.player.PlayerStamina;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Native actual inputs and one attack/wear/Fuse, independently from the adopted animation. */
public final class HeavyMeleeRigClientTest implements FabricClientGameTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static AvatarRenderState state(Minecraft c){return (AvatarRenderState)c.getEntityRenderDispatcher().getRenderer(c.player).createRenderState(c.player,1);}
    private static CombatRigLayer.Frame frame(Minecraft c){return state(c).getData(CombatRigLayer.FRAME);}
    private static PlayerSkin skin(PlayerModelType type){for(int i=0;i<100;i++){var s=DefaultPlayerSkin.get(new UUID(0,i));if(s.model()==type)return s;}throw new AssertionError("Native skin fixture");}
    private static void setup(ClientGameTestContext c,TestSingleplayerContext w,int mob,Item item,ItemStack off,HumanoidArm hand,PlayerModelType type){
        c.getInput().releaseKey(v->v.keyAttack);c.waitTicks(2);
        w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.getInventory().clearContent();p.getInventory().setSelectedSlot(0);p.setMainArm(hand);var stack=new ItemStack(item);check(FusionItems.attach(stack,new ItemStack(Items.COBBLESTONE),s.registryAccess()),"Actual Fuse fixture");p.setItemInHand(InteractionHand.MAIN_HAND,stack);p.setItemInHand(InteractionHand.OFF_HAND,off.copy());p.teleportTo(p.level(),.5,120,.5,Set.of(),0,0,true);PlayerStamina.fill(p);p.inventoryMenu.broadcastChanges();var target=(LivingEntity)s.overworld().getEntity(mob);target.setPos(.5,120,2.7);target.setHealth(200);target.setInvulnerableTime(0);target.setDeltaMovement(Vec3.ZERO);CounterAttackAudit.begin(p);});
        c.waitFor(v->v.player.getMainHandItem().is(item)&&v.player.getMainArm()==hand&&v.player.getMainHandItem().getDamageValue()==0&&MeleeActions.view(v.player).phase()==MeleeView.IDLE);
        c.runOnClient(v->{v.player.getInventory().setSelectedSlot(0);v.player.setYRot(0);v.player.setXRot(0);RigAudit.fixture=skin(type);RigAudit.begin();v.options.setCameraType(CameraType.THIRD_PERSON_BACK);});c.waitFor(v->ActionInput.tickFrame().gameplay());c.waitTicks(25);
    }
    private static void support(Minecraft c,boolean carry){
        var f=frame(c);var names=Arrays.asList(f.data().bones());String main=c.player.getMainArm()==HumanoidArm.RIGHT?"right":"left",other=main.equals("right")?"left":"right";
        var ref=f.global()[names.indexOf(main+"_sword_reference")];var target=ref.transformPosition(new Vector3f(0,-.14F,0));var palm=f.global()[names.indexOf(other+"_palm_grip")].getTranslation(new Vector3f());
        if(carry)check(palm.distance(target)>.2,"Real shield hand is never borrowed as an axe support hand");else check(palm.distance(target)<.015,"Actual empty-hand source support matches real adopted axe reference");
    }
    @Override public void runTest(ClientGameTestContext c){
        var id=new AtomicInteger();var opts=CombatOptions.get();int follow=opts.follow;
        try(TestSingleplayerContext w=c.worldBuilder().create()){
            w.getConnection().waitForChunksRender();w.getServer().runCommand("gamemode survival @p");w.getServer().runCommand("time set day");
            w.getServer().runOnServer(s->{for(int x=-8;x<=8;x++)for(int z=-8;z<=12;z++)s.overworld().setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());var p=s.getPlayerList().getPlayers().getFirst();p.teleportTo(.5,120,.5);var m=EntityTypes.HUSK.create(s.overworld(),EntitySpawnReason.COMMAND);m.setPos(.5,120,2.7);m.setNoAi(true);m.setNoGravity(true);m.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);m.setHealth(200);s.overworld().addFreshEntity(m);id.set(m.getId());});c.waitFor(v->v.level.getEntity(id.get())!=null);c.getInput().resizeWindow(1280,960);c.runOnClient(v->{opts.follow=0;v.options.guiScale().set(2);RigAudit.enabled=true;});
            for(var item:new Item[]{Items.IRON_SWORD,Items.IRON_AXE})for(var type:new PlayerModelType[]{PlayerModelType.WIDE,PlayerModelType.SLIM})for(var hand:HumanoidArm.values())for(boolean carry:new boolean[]{false,true}){
                String suffix=(carry?"carry":"empty")+"_"+(hand==HumanoidArm.RIGHT?"R":"L");String family=item==Items.IRON_SWORD?"sword_heavy":"axe_heavy";var kind=MeleeWeapon.of(new ItemStack(item));
                setup(c,w,id.get(),item,carry?new ItemStack(Items.SHIELD):ItemStack.EMPTY,hand,type);
                c.getInput().holdKey(v->v.keyAttack);c.waitFor(v->MeleeActions.view(v.player).phase()==MeleeView.CHARGE&&frame(v)!=null&&frame(v).clip().equals(family+"_charge_"+suffix));c.takeScreenshot("heavy-rig-charge-"+item.toString().replace(':','-')+"-"+type+"-"+suffix);
                c.waitFor(v->MeleeActions.view(v.player).age()>=kind.threshold&&frame(v)!=null&&frame(v).clip().equals(family+"_hold_"+suffix));c.waitTicks(2);if(item==Items.IRON_AXE)c.runOnClient(v->support(v,carry));
                check(c.computeOnClient(v->RigAudit.bodies>0&&RigAudit.hands>0&&(!carry||RigAudit.shields>0)&&RigAudit.texture.equals(RigAudit.fixture.body().texturePath().toString())),"Real native skin and actual item models reach GPU");c.takeScreenshot("heavy-rig-hold-"+item.toString().replace(':','-')+"-"+type+"-"+suffix);
                c.getInput().releaseKey(v->v.keyAttack);c.waitFor(v->MeleeActions.view(v.player).phase()==MeleeView.SWING&&MeleeActions.view(v.player).charged()&&frame(v)!=null&&frame(v).clip().equals(family+"_release_"+suffix));c.takeScreenshot("heavy-rig-release-"+item.toString().replace(':','-')+"-"+type+"-"+suffix);c.waitFor(v->MeleeActions.view(v.player).phase()==MeleeView.IDLE);c.waitTicks(3);
                w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(CounterAttackAudit.calls==1,"Exactly one native attack for adopted release");check(p.getMainHandItem().getDamageValue()==(item==Items.IRON_AXE?2:1),"Original sword/axe wear retained");check(p.getMainHandItem().get(FusionContent.DATA).remaining()==31,"Exactly one actual Fuse use");check(PlayerStamina.get(p).stamina()>=100-kind.cost&&PlayerStamina.get(p).stamina()<100,"Original charged cost, not author preview timing");if(carry)check(p.getOffhandItem().getDamageValue()==0&&!p.isUsingItem(),"Carried shield gains no block or fake attack wear");});
                if(item==Items.IRON_AXE){
                    setup(c,w,id.get(),item,carry?new ItemStack(Items.SHIELD):ItemStack.EMPTY,hand,type);c.runOnClient(v->{v.options.keyAttack.setDown(true);ActionInput.frame(v);v.options.keyAttack.setDown(false);ActionInput.frame(v);});c.waitFor(v->frame(v)!=null&&frame(v).clip().equals("axe_light_attack_"+suffix));c.takeScreenshot("heavy-rig-real-light-axe-"+type+"-"+suffix);c.waitFor(v->MeleeActions.view(v.player).phase()==MeleeView.IDLE);c.waitTicks(3);w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(CounterAttackAudit.calls==1&&p.getMainHandItem().getDamageValue()==2&&p.getMainHandItem().get(FusionContent.DATA).remaining()==31,"Native light axe has one hit/2 wear/1 Fuse");check(PlayerStamina.get(p).stamina()==100,"Light axe remains free");});
                }
            }
            setup(c,w,id.get(),Items.IRON_AXE,ItemStack.EMPTY,HumanoidArm.RIGHT,PlayerModelType.WIDE);c.getInput().holdKey(v->v.keyAttack);c.waitFor(v->frame(v)!=null&&frame(v).clip().startsWith("axe_heavy_hold"));
            c.setScreen(()->new PauseScreen(true));c.waitFor(Minecraft::isPaused);var before=c.computeOnClient(HeavyMeleeRigClientTest::frame);long until=System.nanoTime()+350_000_000L;c.waitFor(v->System.nanoTime()>until);if(before!=null)check(c.computeOnClient(v->frame(v)==null||!frame(v).clip().equals(before.clip())||java.lang.Math.abs(frame(v).time()-before.time())<.0001),"Pause freezes same clip or genuine input cancellation exits");c.takeScreenshot("heavy-rig-pause");c.setScreen(()->null);c.getInput().releaseKey(v->v.keyAttack);c.waitTicks(8);
            setup(c,w,id.get(),Items.IRON_AXE,ItemStack.EMPTY,HumanoidArm.RIGHT,PlayerModelType.WIDE);c.getInput().holdKey(v->v.keyAttack);c.waitFor(v->frame(v)!=null);c.setScreen(()->new InventoryScreen(Minecraft.getInstance().player));c.waitTicks(3);check(c.computeOnClient(v->frame(v)==null),"GUI immediately owns hands");c.takeScreenshot("heavy-rig-gui");c.getInput().releaseKey(v->v.keyAttack);c.setScreen(()->null);
            setup(c,w,id.get(),Items.IRON_AXE,new ItemStack(Items.TORCH),HumanoidArm.RIGHT,PlayerModelType.WIDE);c.getInput().holdKey(v->v.keyAttack);c.waitFor(v->MeleeActions.view(v.player).phase()==MeleeView.CHARGE);check(c.computeOnClient(v->frame(v)==null),"Occupied unsupported offhand retains actual native item");c.takeScreenshot("heavy-rig-native-occupied");c.getInput().releaseKey(v->v.keyAttack);c.waitTicks(20);
            setup(c,w,id.get(),Items.IRON_AXE,ItemStack.EMPTY,HumanoidArm.RIGHT,PlayerModelType.WIDE);w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();PlayerStamina.consume(p,PlayerStamina.get(p).stamina()-5);});c.getInput().holdKey(v->v.keyAttack);c.waitFor(v->MeleeActions.view(v.player).age()>=20&&frame(v)!=null&&frame(v).clip().startsWith("axe_heavy_hold"));c.getInput().releaseKey(v->v.keyAttack);c.waitFor(v->MeleeActions.view(v.player).phase()==MeleeView.IDLE&&frame(v)!=null&&frame(v).clip().startsWith("axe_heavy_cancel"));c.takeScreenshot("heavy-rig-real-insufficient-stamina-cancel");c.waitTicks(8);w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();check(CounterAttackAudit.calls==0&&p.getMainHandItem().getDamageValue()==0&&p.getMainHandItem().get(FusionContent.DATA).remaining()==32,"Failed charged activation has no fake attack, native wear or Fuse use");});check(c.computeOnClient(v->frame(v)==null),"Unconflicted source cancel relinquishes hands in .30s");
            System.out.println("C6J native sword heavy/axe light+heavy, real threshold/one attack/wear/Fuse/cost, wide/slim left/right carry/empty native items, real source support/nonborrowed shield, pause/GUI/occupied/actual failed-charge cancel PASS");
        }finally{c.getInput().releaseKey(v->v.keyAttack);c.runOnClient(v->{RigAudit.enabled=false;RigAudit.fixture=null;opts.follow=follow;v.options.setCameraType(CameraType.FIRST_PERSON);});}
    }
}
