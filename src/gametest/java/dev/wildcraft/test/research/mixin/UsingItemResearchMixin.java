package dev.wildcraft.test.research.mixin;

import dev.wildcraft.test.research.time.UsingItemAccess;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LivingEntity.class)
public abstract class UsingItemResearchMixin implements UsingItemAccess {
    @Shadow protected int useItemRemaining;
    @Override public void wildcraftResearch$remaining(int ticks) { useItemRemaining = Math.clamp(ticks, 0, 72000); }
}
