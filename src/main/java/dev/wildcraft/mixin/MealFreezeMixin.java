package dev.wildcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.wildcraft.cooking.CookingEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Throttles only native powder-snow increments; never clears freezing or native damage. */
@Mixin(InsideBlockEffectType.class)
public abstract class MealFreezeMixin {
    @WrapOperation(method="lambda$static$0",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;setTicksFrozen(I)V"))
    private static void wildcraft$warmth(Entity entity,int frozen,Operation<Void> original) {
        if(!(entity instanceof ServerPlayer p) || CookingEffects.freezeStep(p))original.call(entity,frozen);
    }
}
