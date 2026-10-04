package dev.wildcraft.mixin.client;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(ItemStackRenderState.class) public interface FusionStateAccessor{@Accessor("layers") ItemStackRenderState.LayerRenderState[] wildcraft$layers();}
