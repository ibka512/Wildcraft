package dev.wildcraft.test.industry.mixin;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
/** Read-only readiness observation in this local test; never acknowledges or changes a teleport. */
@Mixin(ServerGamePacketListenerImpl.class)
public interface NativeIndustryConnectionAccess {
    @Accessor("awaitingPositionFromClient") Vec3 wildcraft$awaitingTeleport();
}
