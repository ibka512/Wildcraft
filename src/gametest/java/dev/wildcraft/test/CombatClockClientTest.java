package dev.wildcraft.test;

import dev.wildcraft.client.combat.CombatOptions;
import dev.wildcraft.client.input.ActionInput;
import dev.wildcraft.combat.MeleeActions;
import dev.wildcraft.combat.MeleeWeapon;
import dev.wildcraft.fuse.*;
import dev.wildcraft.network.MeleeView;
import dev.wildcraft.player.PlayerStamina;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Actual native attacks and submitted third-person skin, rather than a synthetic partial=1 state. */
public final class CombatClockClientTest implements FabricClientGameTest {
    private static void check(boolean ok,String message) { if(!ok)throw new AssertionError(message); }
    private static PlayerSkin skin(PlayerModelType type) {
        for(int i=0;i<100;i++){var s=DefaultPlayerSkin.get(new UUID(0,i));if(s.model()==type)return s;}
        throw new AssertionError("Missing native skin");
    }
    private static void release(ClientGameTestContext c) {
        c.runOnClient(v->{v.options.keyAttack.setDown(false);ActionInput.frame(v);});
    }
    private static float expected(CombatClockAudit.Sample sample) {
        var v=sample.view();
        float nativeAge=Math.clamp((v.age()+sample.partial())/Math.max(1,v.duration()),0,1);
        if(v.phase()==MeleeView.AIR_SWING&&(v.kind()==MeleeWeapon.SWORD||v.kind()==MeleeWeapon.AXE))return nativeAge;
        // The source review windows differ; native recovery is the only attack clock.
        return switch(v.kind()) {
            case TRIDENT -> v.charged()?.625F+.375F*nativeAge:nativeAge;
            case SPEAR -> v.charged()?(float)(1.1/1.75)+(float)(.65/1.75)*nativeAge:nativeAge;
            case MACE -> v.charged()?(float)(1.5/2.5)+(float)(1.0/2.5)*nativeAge:nativeAge;
            case AXE -> nativeAge;
            case SWORD -> v.charged()?nativeAge:(float)(.65+1.4*nativeAge)/sample.frame().data().clips().get(sample.frame().clip()).duration();
        };
    }
    private static Item[] weapons() {
        return switch(System.getProperty("wildcraft.test.combatClockWeapon","")) {
            case "trident" -> new Item[]{Items.TRIDENT};
            case "mace" -> new Item[]{Items.MACE};
            case "spear" -> new Item[]{Items.IRON_SPEAR};
            case "sword" -> new Item[]{Items.IRON_SWORD};
            case "axe" -> new Item[]{Items.IRON_AXE};
            case "" -> Boolean.getBoolean("wildcraft.test.combatClockAir")
                ?new Item[]{Items.IRON_SWORD,Items.IRON_AXE,Items.TRIDENT,Items.MACE,Items.IRON_SPEAR}
                :Boolean.getBoolean("wildcraft.test.combatClockLongWeapons")
                ?new Item[]{Items.TRIDENT,Items.MACE,Items.WOODEN_SPEAR,Items.STONE_SPEAR,Items.COPPER_SPEAR,Items.IRON_SPEAR,Items.GOLDEN_SPEAR,Items.DIAMOND_SPEAR,Items.NETHERITE_SPEAR}
                :new Item[]{Items.IRON_SWORD,Items.IRON_AXE};
            default -> throw new IllegalArgumentException("Unknown isolated clock weapon");
        };
    }
    @Override public void runTest(ClientGameTestContext c) {
        var options=CombatOptions.get();boolean hold=options.hold;int follow=options.follow;
        var target=new AtomicInteger();int cases=0;
        boolean matrix=Boolean.getBoolean("wildcraft.test.combatClockMatrix");
        boolean air=Boolean.getBoolean("wildcraft.test.combatClockAir");
        boolean longWeapons=Boolean.getBoolean("wildcraft.test.combatClockLongWeapons")||!System.getProperty("wildcraft.test.combatClockWeapon","").isEmpty();
        var fractionalCoverage=new java.util.HashSet<String>();
        try(TestSingleplayerContext w=c.worldBuilder().create()) {
            w.getConnection().waitForChunksRender();w.getServer().runCommand("gamemode survival @p");w.getServer().runCommand("time set day");
            w.getServer().runOnServer(s->{
                for(int x=-5;x<=5;x++)for(int z=-4;z<=7;z++)s.overworld().setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());
                var p=s.getPlayerList().getPlayers().getFirst();p.getInventory().clearContent();p.teleportTo(.5,120,.5);
                var mob=EntityTypes.HUSK.create(s.overworld(),EntitySpawnReason.COMMAND);mob.setPos(.5,120,2.7);mob.setNoAi(true);mob.setNoGravity(true);
                mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);mob.setHealth(200);s.overworld().addFreshEntity(mob);target.set(mob.getId());
            });
            c.waitFor(v->v.player.getY()>119&&ActionInput.tickFrame().gameplay());c.getInput().resizeWindow(1280,960);
            c.runOnClient(v->{options.hold=false;options.follow=0;v.options.setCameraType(CameraType.THIRD_PERSON_BACK);RigAudit.enabled=true;});
            for(var type:new PlayerModelType[]{PlayerModelType.WIDE,PlayerModelType.SLIM})for(var arm:HumanoidArm.values())for(boolean shield:new boolean[]{false,true})for(var item:weapons())for(boolean strong:new boolean[]{false,true}) {
                if(air&&strong&&(item==Items.IRON_SWORD||item==Items.IRON_AXE))continue;
                if(!matrix&&cases>0)continue;
                CombatClockAudit.enabled=false;
                w.getServer().runOnServer(s->{
                    var p=s.getPlayerList().getPlayers().getFirst();MeleeActions.reset(p);p.setMainArm(arm);p.setDeltaMovement(Vec3.ZERO);p.teleportTo(.5,120,.5);p.setYRot(0);p.setXRot(0);
                    var stack=new ItemStack(item);check(FusionItems.attach(stack,new ItemStack(Items.COBBLESTONE),s.registryAccess()),"Fuse fixture");
                    p.setItemInHand(InteractionHand.MAIN_HAND,stack);p.setItemInHand(InteractionHand.OFF_HAND,shield?new ItemStack(Items.SHIELD):ItemStack.EMPTY);
                    PlayerStamina.fill(p);p.inventoryMenu.broadcastChanges();var mob=(LivingEntity)s.overworld().getEntity(target.get());mob.setHealth(200);mob.setInvulnerableTime(0);mob.setPos(.5,air?121:120,2.7);mob.setDeltaMovement(Vec3.ZERO);SpearAttackAudit.begin(p);
                });
                c.waitFor(v->v.player.getMainArm()==arm&&v.player.getMainHandItem().is(item)&&v.player.getMainHandItem().getDamageValue()==0&&v.player.getOffhandItem().isEmpty()!=shield&&MeleeActions.view(v.player).phase()==MeleeView.IDLE);
                c.runOnClient(v->{RigAudit.fixture=skin(type);v.player.setYRot(0);v.player.setXRot(0);});c.waitTicks(25);
                if(!air||strong)c.runOnClient(v->{v.options.keyAttack.setDown(true);ActionInput.frame(v);});
                if(strong)c.waitFor(v->{var view=MeleeActions.view(v.player);return view.phase()==MeleeView.CHARGE&&view.age()>=view.duration();});
                w.getServer().runCommand("tick rate 5");
                if(air) {
                    c.getInput().holdKey(v->v.keyJump);c.waitFor(v->!v.player.onGround()&&v.player.getDeltaMovement().y>0);c.getInput().releaseKey(v->v.keyJump);
                    if(!strong)c.runOnClient(v->{v.options.keyAttack.setDown(true);ActionInput.frame(v);});
                }
                c.runOnClient(v->{v.options.framerateLimit().set(10);CombatClockAudit.begin();});release(c);
                c.waitFor(v->CombatClockAudit.frames>0&&CombatClockAudit.last!=null,60);
                int beforePause=c.computeOnClient(v->CombatClockAudit.frames);
                c.setScreen(()->new PauseScreen(true));c.waitFor(Minecraft::isPaused);c.waitTicks(2);
                var sample=c.computeOnClient(v->CombatClockAudit.last);
                check(CombatClockAudit.frames>beforePause,"Paused source was actually submitted again");
                check(c.computeOnClient(v->MeleeActions.view(v.player).phase()==(air?MeleeView.AIR_SWING:MeleeView.SWING)&&v.player.onGround()!=air),"Native attack remains paused in its real scene; actual="+c.computeOnClient(v->MeleeActions.view(v.player))+" submitted="+sample.view());
                float expected=expected(sample),actual=sample.frame().time();
                var kind=sample.view().kind();
                String weaponLabel=air||longWeapons?item.toString().replace(':','-'):item==Items.IRON_AXE?"axe":"sword";
                String label=(air?"air-":"")+type+"-"+arm+"-shield-"+shield+"-"+weaponLabel+"-strong-"+strong;
                String prefix=air||longWeapons?"C6AQ":"C6AP";
                if(prefix.equals("C6AQ")) {
                    String family=kind.name().toLowerCase(java.util.Locale.ROOT);
                    String clipPrefix=air?(kind==MeleeWeapon.SWORD||kind==MeleeWeapon.AXE?family+"_air_attack_":family+"_attack_air_"+(strong?"heavy_":"light_")):family+"_attack_"+(strong?"heavy_":"light_");
                    check(sample.frame().clip().startsWith(clipPrefix),"Actual native weapon owns the submitted source clip: "+sample.frame().clip());
                }
                System.out.println(prefix+"_NATIVE_PAUSE "+label+" actual="+actual+" expected="+expected+" partial="+sample.partial()+" frames="+CombatClockAudit.frames);
                c.takeScreenshot("combat-clock-"+label);
                // A genuine zero partial is valid; require nonzero coverage per weapon/action across the matrix.
                if(sample.partial()>.001F)fractionalCoverage.add(item+":"+strong);
                if(!matrix)check(sample.partial()>.001F,"Single probe needs a nonzero actual native frozen fraction");
                check(Math.abs(actual-expected)<.00001F,"Third-person source preserves native frozen interpolation fraction: actual="+actual+" expected="+expected);
                var position=c.computeOnClient(v->v.player.position());var velocity=c.computeOnClient(v->v.player.getDeltaMovement());
                int before=CombatClockAudit.frames;long until=System.nanoTime()+350_000_000L;c.waitFor(v->System.nanoTime()>until);
                var frozen=c.computeOnClient(v->CombatClockAudit.last);
                check(c.computeOnClient(v->v.player.position().distanceToSqr(position)<1e-10&&v.player.getDeltaMovement().distanceToSqr(velocity)<1e-10),"Pause preserves native position and velocity, without freezing an unpaused world");
                check(CombatClockAudit.frames>before&&Math.abs(frozen.frame().time()-actual)<.00001F,"Actual paused renders keep the same source sample");
                for(int i=0;i<sample.frame().global().length;i++) {
                    var a=sample.frame().global()[i].get(new float[16]);var b=frozen.frame().global()[i].get(new float[16]);
                    for(int k=0;k<16;k++)check(Math.abs(a[k]-b[k])<.00002F,"Paused skin palette remains frozen");
                }
                if(cases==0) {
                    c.setScreen(()->new InventoryScreen(Minecraft.getInstance().player));
                    c.waitFor(v->MeleeActions.view(v.player).phase()==MeleeView.IDLE);
                    int stopped=CombatClockAudit.frames;c.waitTicks(2);
                    check(CombatClockAudit.frames==stopped,"Inventory still cancels an already paused source attack");
                    c.takeScreenshot("combat-clock-real-inventory-cancel");
                }
                c.setScreen(()->null);w.getServer().runCommand("tick rate 20");c.runOnClient(v->v.options.framerateLimit().set(120));
                c.waitFor(v->MeleeActions.view(v.player).phase()==MeleeView.IDLE&&v.player.onGround());
                w.getServer().runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();
                    if(kind==MeleeWeapon.SPEAR)check(SpearAttackAudit.piercing==1&&SpearAttackAudit.stabs==1&&SpearAttackAudit.nativePackets==0&&CounterAttackAudit.calls==0,"Exactly one native piercing/stab; no generic attack or extra STAB packet");
                    else check(CounterAttackAudit.calls==1,"Exactly one native melee attack");
                    check(p.getMainHandItem().getDamageValue()==(item==Items.IRON_AXE?2:1)&&p.getMainHandItem().get(FusionContent.DATA).remaining()==31&&p.getOffhandItem().getDamageValue()==0,"Pause/resume preserves original weapon/shield wear and one Fuse transaction");
                });
                cases++;
            }
            if(matrix)for(var item:weapons())for(boolean strong:new boolean[]{false,true})if(!(air&&strong&&(item==Items.IRON_SWORD||item==Items.IRON_AXE)))check(fractionalCoverage.contains(item+":"+strong),"Matrix needs nonzero native fraction for every weapon/action: "+item+":"+strong);
            System.out.println((air||longWeapons?"C6AQ":"C6AP")+"_COMPLETE scene="+(air?"air":"ground")+" actualPausedAttackCases="+cases+" nativeTicksAndTransactionsUnchanged=true");
        }finally {
            CombatClockAudit.enabled=false;
            c.getInput().releaseKey(v->v.keyJump);c.runOnClient(v->{v.options.keyAttack.setDown(false);RigAudit.enabled=false;RigAudit.fixture=null;options.hold=hold;options.follow=follow;v.options.framerateLimit().set(120);v.options.setCameraType(CameraType.FIRST_PERSON);});
        }
    }
}
