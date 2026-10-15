package dev.wildcraft.client.animation.rig;

import dev.wildcraft.client.render.BackTransitions;
import dev.wildcraft.combat.*;
import dev.wildcraft.network.MeleeView;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Adopted grounded sword-charge/axe slice. Authoritative phases own timing and native attack rules. */
public final class HeavyMeleeRig {
    private static final Map<Player,Motion> MOTION=new WeakHashMap<>();
    private static final class Motion {
        long stamp;float elapsed,join,cancel=-1;String clip;Matrix4f[] last,from;
        Object dimension;PlayerModelType model;Item item,offhand;int slot;net.minecraft.world.entity.HumanoidArm arm;byte lastPhase;
    }
    private HeavyMeleeRig(){}
    static void clear(){MOTION.clear();}
    static void cancel(Player p){MOTION.remove(p);}
    private static boolean reject(Player p){MOTION.remove(p);return false;}
    static boolean extract(Player p,AvatarRenderState state,float partial){
        var client=Minecraft.getInstance();var view=MeleeActions.view(p);var weapon=MeleeWeapon.of(p.getMainHandItem());var m=MOTION.get(p);
        boolean charging=view.phase()==MeleeView.CHARGE;
        boolean swing=view.phase()==MeleeView.SWING;
        boolean supportedAction=weapon==MeleeWeapon.AXE&&(charging||swing)||weapon==MeleeWeapon.SWORD&&(charging||swing&&view.charged());
        boolean cancelling=m!=null&&view.phase()==MeleeView.IDLE&&(m.lastPhase==MeleeView.CHARGE||m.cancel>=0);
        if(!supportedAction&&!cancelling)return reject(p);
        if(!Combat.compatible(p)||!p.onGround()||p.isCrouching()||p.isInvisible()||p.isCurrentlyGlowing()||p.isUsingItem()
            ||p.getDeltaMovement().horizontalDistanceSqr()>=.0004||state.scale!=1||view.hurt()!=0||ParryActions.view(p).remaining()!=0
            ||!p.getOffhandItem().isEmpty()&&!p.getOffhandItem().is(Items.SHIELD)||state.showCape&&state.skin.cape()!=null
            ||p==client.player&&client.gui.screen()!=null&&!client.gui.screen().isPauseScreen())return reject(p);
        for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET})if(!p.getItemBySlot(slot).isEmpty())return reject(p);
        var settles=state.getData(BackTransitions.SETTLES);if(settles!=null)for(var settle:settles)if(settle!=null)return reject(p);
        if(weapon!=MeleeWeapon.SWORD&&weapon!=MeleeWeapon.AXE)return reject(p);
        if(m!=null&&(m.dimension!=p.level().dimension()||m.model!=state.skin.model()||m.item!=p.getMainHandItem().getItem()||m.offhand!=p.getOffhandItem().getItem()||m.slot!=p.getInventory().getSelectedSlot()||m.arm!=p.getMainArm()))return reject(p);
        String variant=state.skin.model()==PlayerModelType.SLIM?"slim":"standard";var data=CombatRig.asset(variant);if(data==null)return reject(p);
        if(m==null){m=new Motion();m.dimension=p.level().dimension();m.model=state.skin.model();m.item=p.getMainHandItem().getItem();m.offhand=p.getOffhandItem().getItem();m.slot=p.getInventory().getSelectedSlot();m.arm=p.getMainArm();MOTION.put(p,m);}
        long now=System.nanoTime();float dt=m.stamp==0||client.isPaused()?0:Math.max(0,(now-m.stamp)/1_000_000_000F);m.stamp=now;m.elapsed+=dt;
        if(charging||swing)m.cancel=-1;
        else if(m.lastPhase==MeleeView.CHARGE)m.cancel=0;
        else if(m.cancel>=0)m.cancel+=dt;
        m.lastPhase=view.phase();
        String family=weapon==MeleeWeapon.SWORD?"sword_heavy":swing&&!view.charged()?"axe_light":"axe_heavy";
        float age=view.age()+(client.isPaused()?0:partial);
        String phase=charging?(age<view.duration()?"charge":"hold"):swing?(view.charged()?"release":"attack"):"cancel";
        String name=family+"_"+phase+"_"+(p.getOffhandItem().isEmpty()?"empty":"carry")+"_"+(p.getMainArm()==net.minecraft.world.entity.HumanoidArm.RIGHT?"R":"L");
        var clip=data.clips().get(name);if(clip==null)return reject(p);
        if(phase.equals("cancel")&&(m.cancel<0||m.cancel>=clip.duration()))return reject(p);
        if(!name.equals(m.clip)){m.from=m.last;m.join=0;m.clip=name;}m.join+=dt;
        // Art durations/preview cycles never trigger attacks or replace the existing threshold/recovery.
        float time=phase.equals("hold")?m.elapsed%clip.duration()/clip.duration():phase.equals("cancel")?m.cancel/clip.duration():Math.clamp(age/Math.max(1,view.duration()),0,1);
        var global=CombatRig.sample(variant,name,time);float join=Math.clamp(m.join/(phase.equals("attack")||phase.equals("release")?.05F:.10F),0,1);join=join*join*(3-2*join);
        if(m.from!=null&&join<1)for(int i=0;i<global.length;i++)global[i]=CombatRig.blend(m.from[i],global[i],join);
        m.last=Arrays.stream(global).map(Matrix4f::new).toArray(Matrix4f[]::new);
        var reference=CombatRigLayer.reference(state.skin.model());reference.setupAnim(state);int head=Arrays.asList(data.bones()).indexOf("head");
        var aim=new Matrix4f(CombatRigLayer.BASIS).mul(CombatRigLayer.delta(reference.head)).mul(CombatRigLayer.BASIS).getUnnormalizedRotation(new Quaternionf()).normalize();
        global[head].translationRotateScale(global[head].getTranslation(new Vector3f()),aim,global[head].getScale(new Vector3f()));
        state.setData(CombatRigLayer.FRAME,new CombatRigLayer.Frame(data,global,name,time,1));return true;
    }
}
