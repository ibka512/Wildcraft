package dev.wildcraft.weather;

/** Deterministic, bounded horizontal wind. Never advances a game's random generator. */
public final class WindRules {
    public static final double MAX_SPEED=.04;
    public static final int PERIOD=2400;
    public record Flow(double x,double z){
        public double speed(){return Math.hypot(x,z);}
    }
    public static final Flow CALM=new Flow(0,0);
    private WindRules(){}
    private static long mix(long n){n=(n^(n>>>30))*0xBF58476D1CE4E5B9L;n=(n^(n>>>27))*0x94D049BB133111EBL;return n^(n>>>31);}
    private static double unit(long n){return (mix(n)>>>11)*0x1.0p-53;}
    private static Flow anchor(long seed,long epoch){
        long key=seed+epoch*0x9E3779B97F4A7C15L;
        double angle=unit(key)*Math.PI*2,scale=.65+.35*unit(key+1);
        return new Flow(Math.cos(angle)*scale,Math.sin(angle)*scale);
    }
    public static Flow field(long seed,long tick,double rain,double thunder){
        rain=Math.clamp(rain,0,1);thunder=Math.clamp(thunder,0,1);
        double strength=.016+.012*rain+.012*thunder;
        long epoch=Math.floorDiv(tick,PERIOD);double t=Math.floorMod(tick,PERIOD)/(double)PERIOD;t=t*t*(3-2*t);
        var a=anchor(seed,epoch);var b=anchor(seed,epoch+1);
        double gust=.92+.08*Math.sin(Math.floorMod(tick,600)/600.0*Math.PI*2+unit(seed)*Math.PI*2);
        return new Flow((a.x+(b.x-a.x)*t)*strength*gust,(a.z+(b.z-a.z)*t)*strength*gust);
    }
    public static double wingGain(int wings,double mass){return Math.min(.10,.05*Math.max(0,wings)/Math.max(1,mass));}
    public static double descentPenalty(double precipitation){return .015*Math.clamp(precipitation,0,1);}
}
