package dev.wildcraft.mixin;
import dev.wildcraft.fuse.FusionActions;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class FusionCreativeMixin{
    @Shadow public ServerPlayer player;
    @Inject(method="handleSetCreativeModeSlot",at=@At("HEAD"),cancellable=true) private void wildcraft$noProjectedOverwrite(ServerboundSetCreativeModeSlotPacket packet,CallbackInfo ci){
        PacketUtils.ensureRunningOnSameThread(packet,(ServerGamePacketListenerImpl)(Object)this,player.level());
        if(player.hasInfiniteMaterials()&&FusionActions.rejectsCreativeEdit(player,packet.slotNum(),packet.itemStack())){player.inventoryMenu.sendAllDataToRemote();ci.cancel();}
    }
}
