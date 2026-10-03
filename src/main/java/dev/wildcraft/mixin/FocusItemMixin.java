package dev.wildcraft.mixin;

import dev.wildcraft.focus.FocusTime;
import dev.wildcraft.focus.UsingItemAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class FocusItemMixin implements UsingItemAccess {
    @Shadow protected int useItemRemaining;
    @Override public void wildcraft$bowRemaining(int ticks) { useItemRemaining = Math.max(0, ticks); }
    @Inject(method = "startUsingItem", at = @At("TAIL"))
    private void wildcraft$focusStart(InteractionHand hand, CallbackInfo ci) {
        if ((Object)this instanceof ServerPlayer p) FocusTime.tryBegin(p);
    }
    @Inject(method = "releaseUsingItem", at = @At("HEAD"))
    private void wildcraft$focusRelease(CallbackInfo ci) { FocusTime.beforeRelease((LivingEntity)(Object)this); }
    @Inject(method = "stopUsingItem", at = @At("HEAD"))
    private void wildcraft$focusStop(CallbackInfo ci) { FocusTime.stopped((LivingEntity)(Object)this); }
    @Inject(method = "travel", at = @At("HEAD"))
    private void wildcraft$focusFall(CallbackInfo ci) {
        if ((Object)this instanceof Player p) FocusTime.limitDescent(p);
    }
}
