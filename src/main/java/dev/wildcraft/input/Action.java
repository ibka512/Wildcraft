package dev.wildcraft.input;

/** Device-independent intents. Vanilla actions are observed, not dispatched a second time. */
public enum Action {
    FORWARD, BACKWARD, LEFT, RIGHT, JUMP, SNEAK, SPRINT,
    ATTACK, USE, INVENTORY, DROP, SWAP_HAND,
    HOTBAR_1, HOTBAR_2, HOTBAR_3, HOTBAR_4, HOTBAR_5,
    HOTBAR_6, HOTBAR_7, HOTBAR_8, HOTBAR_9,
    DEFEND, LOCK_TARGET, TARGET_LEFT, TARGET_RIGHT, DODGE, CONFIRM_SKILL, CLIMB, FUSE, MACHINE_TOGGLE, SETTINGS
}
