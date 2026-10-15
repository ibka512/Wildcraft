package dev.wildcraft.mixin;
import dev.wildcraft.focus.AttackClockAccess;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
@Mixin(LivingEntity.class)
public abstract class AttackClockMixin implements AttackClockAccess {
    @Shadow protected int attackStrengthTicker;
    public int wildcraft$attackTicks(){return attackStrengthTicker;}
    public void wildcraft$attackTicks(int ticks){attackStrengthTicker=ticks;}
}
