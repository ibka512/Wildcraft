package dev.wildcraft.test;
import dev.wildcraft.combat.MeleeWeapon;
/** Test-only counts of actual first-person skin-arm submissions, never gameplay. */
public final class GripAudit {
    public static volatile boolean enabled;
    public static volatile int main,support,swing,air,drop;
    public static volatile MeleeWeapon kind;
    private GripAudit() { }
    public static void begin(){main=support=swing=air=drop=0;kind=null;enabled=true;}
    public static void observe(boolean auxiliary,MeleeWeapon weapon,byte phase){if(enabled){if(auxiliary)support++;else {main++;if(phase==dev.wildcraft.network.MeleeView.SWING)swing++;if(phase==dev.wildcraft.network.MeleeView.AIR_SWING)air++;if(phase==dev.wildcraft.network.MeleeView.DROP_SWING)drop++;}kind=weapon;}}
}
