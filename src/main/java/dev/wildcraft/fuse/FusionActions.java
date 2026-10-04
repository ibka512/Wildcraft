package dev.wildcraft.fuse;
import dev.wildcraft.Wildcraft;
import dev.wildcraft.traversal.*;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
public final class FusionActions{
    private static final AttachmentType<Long> LAST=AttachmentRegistry.create(Wildcraft.id("fusion_last_intent"));
    private FusionActions(){}
    public static void initialize(){PayloadTypeRegistry.serverboundPlay().register(FusionIntent.TYPE,FusionIntent.CODEC);ServerPlayNetworking.registerGlobalReceiver(FusionIntent.TYPE,(input,c) -> receive(c.player(),input));}
    public static boolean eligible(ServerPlayer p){return p.isAlive()&&!p.isSpectator()&&!p.hasInfiniteMaterials()&&p.containerMenu==p.inventoryMenu&&!p.isUsingItem()&&!Gliding.active(p)&&!Climbing.active(p);}
    public static boolean receive(ServerPlayer p,FusionIntent input){
        if(!eligible(p))return false;long now=p.level().getGameTime();Long last=p.getAttached(LAST);if(last!=null&&now>=last&&now-last<4)return false;p.setAttached(LAST,now);
        boolean ok=input.split()?split(p):fuse(p);p.inventoryMenu.broadcastChanges();p.sendSystemMessage(Component.translatable("fuse.wildcraft."+(ok?(input.split()?"split_done":"done"):"refused")),true);return ok;
    }
    private static int emptySlot(ServerPlayer p){int held=p.getInventory().getSelectedSlot();for(int n=0;n<36;n++)if(n!=held&&p.getInventory().getItem(n).isEmpty())return n;return -1;}
    public static boolean fuse(ServerPlayer p){
        if(!eligible(p))return false;var host=p.getMainHandItem();var material=p.getOffhandItem();if(host==material||!FusionItems.host(host)||host.has(FusionContent.DATA)||host.has(FusionContent.VIEW))return false;
        int free=host.getCount()>1?emptySlot(p):-1;if(host.getCount()>1&&(!FusionRules.arrow(host)||free<0))return false;
        var target=host.getCount()==1?host:host.copyWithCount(1);if(!FusionItems.attach(target,material,p.registryAccess()))return false;
        if(host!=target){host.shrink(1);p.getInventory().setItem(free,host);p.setItemInHand(InteractionHand.MAIN_HAND,target);}return true;
    }
    public static boolean split(ServerPlayer p){
        if(!eligible(p))return false;var host=p.getMainHandItem();var recovered=FusionItems.splitMaterial(host,p.registryAccess());if(recovered.isEmpty())return false;var mat=recovered.get();var off=p.getOffhandItem();
        int remainderSlot=host.getCount()>1?emptySlot(p):-1;if(host.getCount()>1&&remainderSlot<0)return false;
        int free=-1;boolean inHand=off.isEmpty()||ItemStack.isSameItemSameComponents(off,mat)&&off.getCount()<off.getMaxStackSize();
        if(!inHand){for(int n=0;n<36;n++)if(n!=p.getInventory().getSelectedSlot()&&n!=remainderSlot&&p.getInventory().getItem(n).isEmpty()){free=n;break;}if(free<0)return false;}
        var single=host.getCount()==1?host:host.copyWithCount(1);
        if(inHand){if(off.isEmpty())p.setItemInHand(InteractionHand.OFF_HAND,mat);else off.grow(1);}else p.getInventory().setItem(free,mat);
        if(single!=host){host.shrink(1);p.getInventory().setItem(remainderSlot,host);p.setItemInHand(InteractionHand.MAIN_HAND,single);}
        single.remove(FusionContent.DATA);single.remove(FusionContent.VIEW);return true;
    }
    public static boolean rejectsCreativeEdit(ServerPlayer p,int slot,ItemStack incoming){return incoming.has(FusionContent.DATA)||incoming.has(FusionContent.VIEW)||slot>=0&&slot<p.inventoryMenu.slots.size()&&(p.inventoryMenu.getSlot(slot).getItem().has(FusionContent.DATA)||p.inventoryMenu.getSlot(slot).getItem().has(FusionContent.VIEW));}
}
