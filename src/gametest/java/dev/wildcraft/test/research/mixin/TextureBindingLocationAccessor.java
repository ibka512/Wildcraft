package dev.wildcraft.test.research.mixin;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
/** Read the actual package-private material binding without reflection or replacement. */
@Mixin(targets="net.minecraft.client.renderer.rendertype.RenderSetup$TextureBinding")
public interface TextureBindingLocationAccessor {@Accessor("location") Identifier wildcraft$location();}
