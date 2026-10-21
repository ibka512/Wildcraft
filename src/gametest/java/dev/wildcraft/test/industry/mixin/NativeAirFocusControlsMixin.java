package dev.wildcraft.test.industry.mixin;

import dev.wildcraft.client.input.GlideControls;
import dev.wildcraft.test.industry.NativeAirFocusProbe;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observe actual production reasons before a glide-control tick; no field or input writes. */
@Mixin(GlideControls.class)
public abstract class NativeAirFocusControlsMixin {
    @Shadow private static boolean requested;
    @Shadow private static boolean acknowledged;
    @Shadow private static int selectedSlot;
    @Shadow private static ItemStack mainHand;
    @Shadow private static ItemStack offHand;
    @Inject(method="tick",at=@At("HEAD"))
    private static void wildcraft$observeNativeControls(Minecraft client,CallbackInfo ci){NativeAirFocusProbe.observeClient(client,requested,acknowledged,selectedSlot,mainHand,offHand);}
}
