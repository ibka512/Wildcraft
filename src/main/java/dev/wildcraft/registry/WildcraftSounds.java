package dev.wildcraft.registry;

import dev.wildcraft.Wildcraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

public final class WildcraftSounds {
    public static final SoundEvent FOCUS_ENTER = register("focus.enter");
    public static final SoundEvent FOCUS_EXIT = register("focus.exit");
    private WildcraftSounds() { }
    private static SoundEvent register(String name) {
        var id = Wildcraft.id(name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }
    public static void initialize() { }
}
