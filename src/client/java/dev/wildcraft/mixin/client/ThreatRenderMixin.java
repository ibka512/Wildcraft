package dev.wildcraft.mixin.client;
import dev.wildcraft.client.animation.GuardPoses;
import net.minecraft.client.renderer.entity.AbstractZombieRenderer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.world.entity.monster.zombie.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AbstractZombieRenderer.class)
public abstract class ThreatRenderMixin {
    @Inject(method="extractRenderState(Lnet/minecraft/world/entity/monster/zombie/Zombie;Lnet/minecraft/client/renderer/entity/state/ZombieRenderState;F)V",at=@At("TAIL"))
    private void wildcraft$threat(Zombie entity,ZombieRenderState state,float partial,CallbackInfo ci){GuardPoses.extractThreat(entity,state,partial);}
}
