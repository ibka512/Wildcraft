package dev.wildcraft.test;
import dev.wildcraft.client.animation.rig.CombatRigLayer;
import net.minecraft.world.entity.player.PlayerSkin;
/** Test-owned controlled native skins and actual GPU submission observations. */
public final class RigAudit {
    public static boolean enabled;
    public static PlayerSkin fixture;
    public static Float bodyYaw;
    public static Boolean cape;
    public static boolean sleeves=true;
    public static int bodies,hands,shields;
    public static CombatRigLayer.Frame last;
    public static String texture;
    public static void begin(){enabled=true;bodies=hands=shields=0;last=null;}
}
