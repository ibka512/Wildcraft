package dev.wildcraft.test;
/** Packaged-client probe of real production submissions. Never shipped in the formal mod. */
public final class EquipmentAudit {
    public static volatile boolean enabled;
    public static final int[] draw=new int[3],holster=new int[3],body=new int[3];
    private EquipmentAudit() { }
    public static void begin(){java.util.Arrays.fill(draw,0);java.util.Arrays.fill(holster,0);java.util.Arrays.fill(body,0);enabled=true;}
}
