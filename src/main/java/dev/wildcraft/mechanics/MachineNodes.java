package dev.wildcraft.mechanics;

import net.minecraft.world.phys.Vec3;

/** Stable local coordinates: top, bottom, rear, front, left, right. */
public final class MachineNodes {
    public static final int COUNT = 6;
    private static final Vec3[] POINTS = {
        new Vec3(0, .65, 0), new Vec3(0, 0, 0),
        new Vec3(0, .325, -.7), new Vec3(0, .325, .7),
        new Vec3(-.7, .325, 0), new Vec3(.7, .325, 0)
    };
    private static final Vec3[] THRUST = {
        new Vec3(0,-1,0), new Vec3(0,1,0), new Vec3(0,0,1),
        new Vec3(0,0,-1), new Vec3(1,0,0), new Vec3(-1,0,0)
    };
    private MachineNodes() { }
    public static boolean valid(int node) { return node >= 0 && node < COUNT; }
    public static Vec3 point(int node) { return POINTS[node]; }
    public static Vec3 thrust(int node) { return THRUST[node]; }
    public static Vec3 rotate(Vec3 local, float yaw) { return local.yRot((float)Math.toRadians(-yaw)); }
    public static boolean panel(Vec3 bodyRelative, float yaw) {
        if (!bodyRelative.isFinite()) return false;
        Vec3 local = rotate(bodyRelative, -yaw);
        return local.y >= .55 && local.y <= .72 && Math.abs(local.x) <= .25 && local.z >= .32 && local.z <= .5;
    }
    public static int nearest(Vec3 bodyRelative, float yaw) {
        if (!bodyRelative.isFinite() || Math.abs(bodyRelative.x) > .76 || Math.abs(bodyRelative.z) > .76
                || bodyRelative.y < -.06 || bodyRelative.y > .71) return -1;
        Vec3 local = rotate(bodyRelative, -yaw);
        int best = -1; double distance = .34 * .34;
        for (int n = 0; n < COUNT; n++) {
            double d = local.distanceToSqr(POINTS[n]);
            if (d <= distance) { distance = d; best = n; }
        }
        return best;
    }
}
