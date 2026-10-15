package dev.wildcraft.test;
/** Test-owned observation of real glider skin and canopy submissions. */
public final class GlideRigAudit {
    public static boolean enabled;
    public static int worldCanopies,viewCanopies,viewArms;
    public static String texture;
    public static org.joml.Matrix4f viewPose,canopyPose;
    public static dev.wildcraft.client.animation.rig.CombatRigLayer.Frame viewFrame;
    public static void begin(){enabled=true;worldCanopies=viewCanopies=viewArms=0;texture=null;viewPose=canopyPose=null;viewFrame=null;}
}
