package dev.wildcraft.client.combat;

import dev.wildcraft.client.input.ActionInput;
import dev.wildcraft.combat.Combat;
import dev.wildcraft.input.Action;
import dev.wildcraft.network.CombatIntent;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

/** Client ownership follows the use hand across inventory synchronization; exact stack identity is server-owned. */
public final class CombatControls {
    private static LocalPlayer observed;
    private static boolean owned, acknowledged;
    private static int selectedSlot;
    private static InteractionHand hand;
    private static boolean blocked;
    private static CombatIntent last;
    private CombatControls(){}
    public static void initialize(){
        CombatOptions.load();
        ClientTickEvents.START_CLIENT_TICK.register(CombatControls::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((h,c)->reset());
    }
    private static void stop(){if(owned&&observed!=null&&observed.isUsingItem()&&observed.getUsedItemHand()==hand&&observed.getUseItem().is(Items.SHIELD))observed.stopUsingItem();owned=false;acknowledged=false;hand=null;}
    private static void reset(){stop();observed=null;blocked=false;last=null;}
    public static boolean ownsDefense(){
        return observed!=null&&owned&&observed.isUsingItem()&&observed.getUsedItemHand()==hand&&observed.getUseItem().is(Items.SHIELD)
            &&ActionInput.down(Action.DEFEND)&&!ActionInput.down(Action.USE)&&Combat.compatible(observed);
    }
    private static void tick(Minecraft c){
        if(c.player!=observed){reset();observed=c.player;}
        var p=c.player;if(p==null||!ClientPlayNetworking.canSend(CombatIntent.TYPE))return;
        var input=ActionInput.tickFrame();var o=CombatOptions.get();
        boolean available=input.gameplay()&&Combat.compatible(p);
        boolean held=available&&!dev.wildcraft.combat.MeleeActions.blocksHands(p)&&input.down(Action.DEFEND)&&!input.triggered(Action.USE);
        if(!input.down(Action.DEFEND))blocked=false;
        if(owned&&Combat.view(p).defending())acknowledged=true;
        if(owned&&(!p.getItemInHand(hand).is(Items.SHIELD)||p.getCooldowns().isOnCooldown(p.getItemInHand(hand))
                ||hand==InteractionHand.MAIN_HAND&&p.getInventory().getSelectedSlot()!=selectedSlot
                ||!p.isUsingItem()||p.getUsedItemHand()!=hand||!p.getUseItem().is(Items.SHIELD)
                ||acknowledged&&!Combat.view(p).defending())){
            stop();blocked=input.down(Action.DEFEND)&&!dev.wildcraft.combat.MeleeActions.blocksHands(p);
        }
        if(!held)stop();
        else if(!blocked&&!owned&&!p.isUsingItem()){
            var candidate=Combat.shieldHand(p);
            if(candidate!=null){p.startUsingItem(candidate);if(p.isUsingItem()){hand=candidate;owned=true;acknowledged=false;selectedSlot=p.getInventory().getSelectedSlot();}}
        }
        byte operation=CombatIntent.KEEP;
        if(!available)operation=CombatIntent.CLEAR;
        else if(o.hold){
            if(input.released().contains(Action.LOCK_TARGET))operation=CombatIntent.CLEAR;
            else if(input.fresh(Action.LOCK_TARGET))operation=CombatIntent.SELECT;
        }else if(input.clicks(Action.LOCK_TARGET)%2==1)operation=CombatIntent.TOGGLE;
        if(available&&operation==CombatIntent.KEEP){
            if(input.clicks(Action.TARGET_LEFT)>0&&input.clicks(Action.TARGET_RIGHT)==0)operation=CombatIntent.LEFT;
            if(input.clicks(Action.TARGET_RIGHT)>0&&input.clicks(Action.TARGET_LEFT)==0)operation=CombatIntent.RIGHT;
        }
        var intent=new CombatIntent(operation,held&&!blocked,(byte)o.range,o.neutral);
        boolean command=operation!=CombatIntent.KEEP&&(operation!=CombatIntent.CLEAR||last==null||last.operation()!=CombatIntent.CLEAR);
        if(command||!intent.equals(last)||available&&p.tickCount%5==0){ClientPlayNetworking.send(intent);last=intent;}
    }
}
