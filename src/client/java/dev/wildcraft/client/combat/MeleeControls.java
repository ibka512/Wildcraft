package dev.wildcraft.client.combat;
import dev.wildcraft.client.input.*;
import dev.wildcraft.combat.*;
import dev.wildcraft.input.Action;
import dev.wildcraft.network.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.*;
/** Claim once at press. Block digging and combat cannot acquire the same held mouse action. */
public final class MeleeControls {
    private static LocalPlayer observed;
    private static boolean owned,blocked,jumpOwned,claimedTick;
    private static ItemStack signature=ItemStack.EMPTY;
    private static int slot,seenSwing=-1;
    private MeleeControls(){}
    public static void initialize(){ClientTickEvents.START_CLIENT_TICK.register(MeleeControls::tick);ClientPlayConnectionEvents.DISCONNECT.register((h,c)->reset());}
    private static void reset(){owned=blocked=jumpOwned=claimedTick=false;observed=null;seenSwing=-1;signature=ItemStack.EMPTY;}
    public static boolean suppressNative(){return owned||blocked||claimedTick;}
    public static void syncCarriedItem(){var c=Minecraft.getInstance();if(c.player!=null&&c.gameMode!=null)((dev.wildcraft.mixin.client.CarriedItemAccess)c.gameMode).wildcraft$syncCarriedItem();}
    private static void send(byte op,byte dir){if(op==MeleeIntent.BEGIN||op==MeleeIntent.RELEASE)syncCarriedItem();ClientPlayNetworking.send(new MeleeIntent(op,dir));}
    private static boolean block(Minecraft c){return c.hitResult!=null&&c.hitResult.getType()==HitResult.Type.BLOCK;}
    private static void tick(Minecraft c){
        if(c.player!=observed){reset();observed=c.player;}var p=c.player;if(p==null||!ClientPlayNetworking.canSend(MeleeIntent.TYPE))return;
        var f=ActionInput.tickFrame();claimedTick=owned;
        if(ReactionTime.active(p)){if(owned){send(MeleeIntent.CANCEL,(byte)0);owned=false;}claimedTick=true;blocked=c.options.keyAttack.isDown();return;}
        var view=MeleeActions.view(p);if((view.phase()==MeleeView.SWING||view.phase()==MeleeView.AIR_SWING||view.phase()==MeleeView.DROP_SWING)&&view.serial()!=seenSwing){seenSwing=view.serial();p.resetAttackStrengthTicker();}
        if(!c.options.keyAttack.isDown())blocked=false;
        if(!f.down(Action.JUMP))jumpOwned=false;
        boolean available=f.gameplay()&&MeleeActions.eligible(p);
        // A closed-to-LAN integrated pause can receive a client tick before isPaused latches.
        // Already released attacks have settled their native hit; retain their paused recovery.
        // Preparation, inventory screens and non-pausing servers keep the ordinary cancellation.
        boolean pausedAttack=c.getSingleplayerServer()!=null&&!c.getSingleplayerServer().isPublished()
                &&c.gui.screen()!=null&&c.gui.screen().isPauseScreen()&&!owned
                &&(view.phase()==MeleeView.SWING||view.phase()==MeleeView.AIR_SWING||view.phase()==MeleeView.DROP_SWING);
        if(!available&&MeleeActions.blocksHands(p)&&!pausedAttack){
            send(MeleeIntent.CANCEL,(byte)0);p.setAttached(MeleeActions.VIEW,MeleeView.OFF);
            p.setDeltaMovement(0,p.getDeltaMovement().y,0);
        }
        boolean changed=owned&&(slot!=p.getInventory().getSelectedSlot()||!ItemStack.isSameItemSameComponents(signature,p.getMainHandItem()));
        boolean defending=f.triggered(Action.DEFEND)&&Combat.shieldHand(p)!=null;
        boolean using=f.triggered(Action.USE);
        boolean incompatible=!available||p.isUsingItem()||using||defending||changed||owned&&block(c);
        if(available&&!owned&&f.fresh(Action.ATTACK)&&MeleeActions.weapon(p.getMainHandItem())&&!block(c)&&(using||defending)){
            claimedTick=true;blocked=c.options.keyAttack.isDown();
        }
        if(owned&&incompatible){send(MeleeIntent.CANCEL,(byte)0);owned=false;blocked=c.options.keyAttack.isDown();}
        if(available&&!incompatible&&!blocked&&!owned&&f.fresh(Action.ATTACK)&&MeleeActions.weapon(p.getMainHandItem())&&!block(c)
                &&(!(c.hitResult instanceof EntityHitResult e)||MeleeWeapon.of(p.getMainHandItem())==MeleeWeapon.SPEAR||e.getEntity() instanceof net.minecraft.world.entity.LivingEntity)){
            owned=true;claimedTick=true;slot=p.getInventory().getSelectedSlot();signature=p.getMainHandItem().copy();send(MeleeIntent.BEGIN,(byte)0);
        }
        if(owned){
            if(f.released().contains(Action.ATTACK)){send(MeleeIntent.RELEASE,(byte)0);owned=false;}
            else if(p.tickCount%5==0)send(MeleeIntent.KEEP,(byte)0);
        }
        byte direction=MeleeRules.direction(f.forward(),f.sideways());
        boolean jump=CombatOptions.get().jumpDodge&&Combat.locked(p)&&p.onGround()&&f.fresh(Action.JUMP)&&(f.forward()<0||f.sideways()!=0);
        if(available&&direction!=0&&(f.fresh(Action.DODGE)||jump)){
            if(owned){send(MeleeIntent.CANCEL,(byte)0);owned=false;blocked=c.options.keyAttack.isDown();}
            send(MeleeIntent.DODGE,direction);if(jump)jumpOwned=true;
        }
        p.setAttached(MeleeActions.JUMP_CLAIM,jumpOwned);
    }
}
