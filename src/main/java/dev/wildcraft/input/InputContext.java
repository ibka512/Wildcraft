package dev.wildcraft.input;

/** Current gameplay owner; menus and loss of focus suppress Wildcraft actions. */
public enum InputContext {
    INACTIVE, GUI, NORMAL, LOCKED_COMBAT, AIMING, CLIMBING, GLIDING, MECHANISM, RIDING
}
