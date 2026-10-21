package dev.wildcraft.test;
import org.joml.Matrix4f;
/** Probe-owned actual deferred native geometry observations, with no production state writes. */
public final class BatteryPoseAudit {
    public static boolean enabled,inBattery;
    public static Matrix4f parent;
    public static int gpu;
    public static float maxMatrixError;
    public static void begin(){enabled=true;inBattery=false;parent=null;gpu=0;maxMatrixError=0;}
    public static void geometry(Matrix4f actual,Matrix4f expected){
        if(!enabled)return;float[] a=new float[16],b=new float[16];actual.get(a);expected.get(b);float error=0;
        for(int i=0;i<16;i++)error=Math.max(error,Math.abs(a[i]-b[i]));maxMatrixError=Math.max(maxMatrixError,error);
        if(error>2e-5F)throw new AssertionError("Battery GPU repeats native item transform: "+error);gpu++;
    }
}
