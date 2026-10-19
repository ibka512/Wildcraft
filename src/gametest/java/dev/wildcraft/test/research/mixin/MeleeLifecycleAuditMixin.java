package dev.wildcraft.test.research.mixin;
import dev.wildcraft.combat.MeleeActions;
import dev.wildcraft.network.MeleeIntent;
import dev.wildcraft.test.MeleeLifecycleAudit;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(MeleeActions.class)
public abstract class MeleeLifecycleAuditMixin {
 @Inject(method="receive",at=@At("HEAD")) private static void wildcraft$before(ServerPlayer p,MeleeIntent intent,CallbackInfo ci){MeleeLifecycleAudit.before(p,intent);}
 @Inject(method="receive",at=@At("RETURN")) private static void wildcraft$after(ServerPlayer p,MeleeIntent intent,CallbackInfo ci){MeleeLifecycleAudit.after(p,intent);}
}
