package dev.wildcraft.client.combat;
import dev.wildcraft.Wildcraft;
import dev.wildcraft.client.input.*;
import dev.wildcraft.combat.*;
import dev.wildcraft.input.Action;
import dev.wildcraft.network.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.HitResult;
/** Frame delivery consumes the same Action edges, never samples another physical binding. */
public final class SkillControls {
    private static Object observed;private static int bow=-1,dodge=-1;private static boolean available,active,claim,pendingClaim,tickClaim;private static net.minecraft.world.item.ItemStack signature=net.minecraft.world.item.ItemStack.EMPTY;private static int slot;
    private SkillControls(){}
    public static void initialize(){HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR,Wildcraft.id("reaction_confirm"),(g,t)->{
        var c=Minecraft.getInstance();if(c.player==null||c.gui.screen()!=null||c.gui.hud.isHidden()||(ReactionTime.view(c.player).phase()!=ReactionView.READY&&!(CombatOptions.get().bowSkill==1&&dev.wildcraft.focus.FocusTime.eligible(c.player)&&!dev.wildcraft.focus.FocusTime.active(c.player))))return;
        var label=Component.translatable("combat.wildcraft.confirm_ready",WildcraftBindings.CONFIRM_SKILL.getTranslatedKeyMessage());int x=(g.guiWidth()-c.font.width(label))/2;g.text(c.font,label,x,g.guiHeight()/2+40,0xFFB6EB91);
    });}
    public static void startTick(){tickClaim=pendingClaim;pendingClaim=false;}
    public static boolean suppressNative(){return claim||active||pendingClaim||tickClaim;}
    private static void send(byte op){ClientPlayNetworking.send(new SkillIntent(op,(byte)CombatOptions.get().bowSkill,(byte)CombatOptions.get().dodgeSkill));}
    public static void frame(Minecraft c){
        if(c.player!=observed){observed=c.player;bow=dodge=-1;active=claim=available=pendingClaim=tickClaim=false;}
        if(c.player==null||!ClientPlayNetworking.canSend(SkillIntent.TYPE))return;
        var o=CombatOptions.get();if(bow!=o.bowSkill||dodge!=o.dodgeSkill){send(SkillIntent.PREFERENCES);bow=o.bowSkill;dodge=o.dodgeSkill;}
        boolean gameplay=ActionInput.gameplay(c);
        if(!gameplay){if(available)send(SkillIntent.CANCEL);available=false;active=false;claim=ActionInput.down(Action.ATTACK);return;}
        available=true;
        boolean next=ReactionTime.active(c.player);
        // A held attack from before the opportunity must be released before it can counter.
        if(next&&!active){ActionInput.takePress(Action.ATTACK);signature=c.player.getMainHandItem().copy();slot=c.player.getInventory().getSelectedSlot();}
        active=next;
        if(active&&(slot!=c.player.getInventory().getSelectedSlot()||!net.minecraft.world.item.ItemStack.isSameItemSameComponents(signature,c.player.getMainHandItem())||ActionInput.down(Action.USE)||ActionInput.down(Action.DEFEND))){send(SkillIntent.CANCEL);active=false;return;}
        if(active&&ActionInput.down(Action.ATTACK)&&c.hitResult!=null&&c.hitResult.getType()==HitResult.Type.BLOCK){send(SkillIntent.CANCEL);active=false;return;}
        if(!ActionInput.down(Action.ATTACK))claim=false;
        if(ActionInput.takePress(Action.CONFIRM_SKILL))send(SkillIntent.CONFIRM);
        if(active&&ActionInput.takePress(Action.ATTACK)&&MeleeActions.weapon(c.player.getMainHandItem())&&(c.hitResult==null||c.hitResult.getType()!=HitResult.Type.BLOCK)){
            // The native click queue is consumed on a later tick, even if the frame tap already ended.
            claim=pendingClaim=tickClaim=true;MeleeControls.syncCarriedItem();c.player.connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot(c.player.getYRot(),c.player.getXRot(),c.player.onGround(),c.player.horizontalCollision));ClientPlayNetworking.send(new MeleeIntent(MeleeIntent.RELEASE,(byte)0));
        }
    }
}
