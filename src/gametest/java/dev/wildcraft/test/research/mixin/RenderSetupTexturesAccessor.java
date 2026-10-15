package dev.wildcraft.test.research.mixin;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(RenderSetup.class)
public interface RenderSetupTexturesAccessor {@Accessor("textures") Map<String,Object> wildcraft$textures();}
