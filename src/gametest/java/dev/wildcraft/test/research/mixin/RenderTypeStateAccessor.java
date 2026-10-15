package dev.wildcraft.test.research.mixin;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
/** Read actual collector material texture; no render-state or material replacement. */
@Mixin(RenderType.class)
public interface RenderTypeStateAccessor {@Accessor("state") RenderSetup wildcraft$setup();}
