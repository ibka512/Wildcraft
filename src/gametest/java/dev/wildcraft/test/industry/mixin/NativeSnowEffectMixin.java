package dev.wildcraft.test.industry.mixin;

import dev.wildcraft.test.industry.NativeSnowProbe;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Read before/after the real vanilla effect and the existing production warmth wrapper. */
@Mixin(InsideBlockEffectType.class)
public abstract class NativeSnowEffectMixin {
    @Inject(method="lambda$static$0",at=@At("HEAD"))
    private static void wildcraft$beforeSnow(Entity e,CallbackInfo ci){NativeSnowProbe.beforeEffect(e);}
    @Inject(method="lambda$static$0",at=@At("RETURN"))
    private static void wildcraft$afterSnow(Entity e,CallbackInfo ci){NativeSnowProbe.afterEffect(e);}
}
