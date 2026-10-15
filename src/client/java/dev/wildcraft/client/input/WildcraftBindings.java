package dev.wildcraft.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import dev.wildcraft.Wildcraft;
import dev.wildcraft.input.Action;
import java.util.Map;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;

/** Native bindings retain their identifiers, saved rebindings and controller-mod discovery. */
public final class WildcraftBindings {
    private static final Map<String, KeyMapping.Category> CATEGORIES = new java.util.HashMap<>();
    public static final KeyMapping CLIMB = register("climb", InputConstants.KEY_G, "controls");
    public static final KeyMapping FUSE = register("fuse", InputConstants.KEY_V, "fusion");
    public static final KeyMapping MACHINE_TOGGLE = register("machine_toggle", InputConstants.KEY_R, "mechanics");
    public static final KeyMapping SETTINGS = register("focus_settings", InputConstants.KEY_F8, "focus_settings");
    public static final KeyMapping DEFEND = register("defend", InputConstants.KEY_C, "combat");
    public static final KeyMapping LOCK_TARGET = register("lock_target", InputConstants.KEY_Z, "combat");
    public static final KeyMapping TARGET_LEFT = register("target_left", InputConstants.KEY_LBRACKET, "combat");
    public static final KeyMapping TARGET_RIGHT = register("target_right", InputConstants.KEY_RBRACKET, "combat");
    public static final KeyMapping DODGE = register("dodge", InputConstants.KEY_X, "combat");
    public static final KeyMapping CONFIRM_SKILL=register("confirm_skill",InputConstants.KEY_H,"combat");
    public static final Map<Action, KeyMapping> ALL = Map.of(Action.CLIMB, CLIMB, Action.FUSE, FUSE,
            Action.MACHINE_TOGGLE, MACHINE_TOGGLE, Action.SETTINGS, SETTINGS, Action.DEFEND, DEFEND, Action.LOCK_TARGET, LOCK_TARGET,
            Action.TARGET_LEFT, TARGET_LEFT, Action.TARGET_RIGHT, TARGET_RIGHT, Action.DODGE, DODGE, Action.CONFIRM_SKILL,CONFIRM_SKILL);
    private WildcraftBindings() { }
    private static KeyMapping register(String name, int key, String category) {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping("key.wildcraft." + name,
                InputConstants.Type.KEYBOARD, key, CATEGORIES.computeIfAbsent(category, categoryName -> KeyMapping.Category.register(Wildcraft.id(categoryName)))));
    }
}
