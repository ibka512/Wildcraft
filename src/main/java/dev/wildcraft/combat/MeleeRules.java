package dev.wildcraft.combat;
/** World-tick combat balance, separate from bow real-time compensation. */
public final class MeleeRules {
    public static final int MAX_CHARGE=60,INPUT_TIMEOUT=20,HOP_TICKS=6,HOP_INTERVAL=12;
    public static final double HOP_COST=8,HOP_SPEED=.32,HOP_UP=.22;
    private MeleeRules(){}
    public static int threshold(boolean axe){return axe?20:12;}
    public static int recovery(boolean axe,boolean charged){return axe?(charged?16:9):(charged?10:5);}
    public static double cost(boolean axe){return axe?18:12;}
    public static float multiplier(boolean axe){return axe?1.8F:1.5F;}
    public static byte direction(int forward,int sideways){return forward<0?(byte)-2:sideways>0?(byte)1:sideways<0?(byte)-1:forward>0?(byte)0:(byte)-2;}
}
