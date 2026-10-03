package dev.wildcraft.test;

import dev.wildcraft.temperature.TemperatureRules;

/** Threshold stability and frame-independent bounded interpolation, rather than implementation snapshots. */
public final class TemperatureRulesCheck {
    public static void main(String[] args) {
        check(TemperatureRules.biome(-.5F) < TemperatureRules.biome(.05F), "Frozen regions colder than cold forests");
        check(TemperatureRules.biome(.3F) < TemperatureRules.biome(.8F), "Taiga colder than plains");
        check(TemperatureRules.biome(.8F) < TemperatureRules.biome(.95F), "Jungle warmer than plains");
        check(TemperatureRules.biome(.95F) < TemperatureRules.biome(2), "Desert hotter than jungle");
        check(TemperatureRules.biome(Float.NaN) == 0, "Unknown invalid climate is neutral");
        check(TemperatureRules.target(0,0,true,0) < 0, "Water cools");
        check(TemperatureRules.target(3,0,false,100) == 3, "Heat cannot grow without bound");
        check(TemperatureRules.smooth(1, -3, -1) == 1, "Negative duration cannot reverse time");
        check(TemperatureRules.smooth(1, -3, Double.NaN) == 1, "Invalid duration remains inert");
        check(TemperatureRules.smooth(1, -3, 700) == TemperatureRules.smooth(1,-3,.25), "Stall interpolation bounded");
        for (int fps : new int[]{30,60,120}) {
            double value = -3;
            for (int i=0;i<fps*3;i++) value=TemperatureRules.smooth(value,3,1.0/fps);
            check(Math.abs(value-(3-6*Math.exp(-2.5)))<1e-9,"Same active duration at "+fps+" fps");
        }
        int band=3;
        for(int i=0;i<1000;i++) { band=TemperatureRules.band(i%2==0?.49:.51,band); check(band==3,"Threshold chatter suppressed"); }
        check(TemperatureRules.band(.7,band)==4,"Sustained warmth crosses band");
        check(TemperatureRules.band(.4,4)==4,"Cooling inside hysteresis stays warm");
        check(TemperatureRules.band(.3,4)==3,"Sustained cooling returns comfortable");
        for (float climate : new float[]{-.5F, .05F, .3F, .8F, .95F, 2}) {
            double value = 0; int state = 3;
            float target = TemperatureRules.biome(climate);
            for (int i=0;i<600;i++) { value=TemperatureRules.smooth(value,target,1.0/60); state=TemperatureRules.band(value,state); }
            check(state==TemperatureRules.initialBand(target),"Stable native climate reaches its intended band from neutral");
        }
        for(int target=-3;target<=3;target++) {
            double value=3;
            for(int i=0;i<1000;i++) {
                double previous=value;value=TemperatureRules.smooth(value,target,.02);
                check(value>=-3 && value<=3 && value<=previous+1e-12 && value>=target-1e-12,"Bounded monotonic approach");
            }
        }
        System.out.println("WILDCRAFT P4A temperature: climate/water/limits, 30/60/120fps, 1000 hysteresis and 7000 interpolation checks passed");
    }
    private static void check(boolean condition,String message) { if(!condition)throw new AssertionError(message); }
}
