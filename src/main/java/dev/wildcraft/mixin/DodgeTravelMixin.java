package dev.wildcraft.mixin;
import dev.wildcraft.combat.MeleeActions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LivingEntity.class)
public abstract class DodgeTravelMixin {
    @ModifyVariable(method="travel",at=@At("HEAD"),argsOnly=true)
    private Vec3 wildcraft$hopMotion(Vec3 input){return (Object)this instanceof Player p?MeleeActions.travel(p,input):input;}
    @Inject(method="jumpFromGround",at=@At("HEAD"),cancellable=true)
    private void wildcraft$claimedJump(CallbackInfo ci){if((Object)this instanceof Player p&&p.level().isClientSide()&&Boolean.TRUE.equals(p.getAttached(MeleeActions.JUMP_CLAIM)))ci.cancel();}
}
