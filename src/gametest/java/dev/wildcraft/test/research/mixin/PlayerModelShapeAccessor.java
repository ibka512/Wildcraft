package dev.wildcraft.test.research.mixin;
import net.minecraft.client.model.player.PlayerModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(PlayerModel.class)
public interface PlayerModelShapeAccessor {@Accessor("slim") boolean wildcraft$isSlim();}
