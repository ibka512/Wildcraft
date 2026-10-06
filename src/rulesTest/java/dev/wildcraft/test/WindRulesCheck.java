package dev.wildcraft.test;

import dev.wildcraft.weather.WindRules;

public final class WindRulesCheck {
    public static void main(String[] args){
        for(long seed:new long[]{0,1,-91,Long.MAX_VALUE})for(long tick=-2401;tick<9601;tick+=3){
            var clear=WindRules.field(seed,tick,0,0);var rain=WindRules.field(seed,tick,1,0);var storm=WindRules.field(seed,tick,1,1);
            check(clear.speed()<=.016+1e-12&&rain.speed()<=.028+1e-12&&storm.speed()<=.040+1e-12,"All weather states remain bounded");
            check(clear.equals(WindRules.field(seed,tick,0,0)),"Same saved seed/time is deterministic");
            check(Math.abs(clear.x()*2.5-storm.x())<1e-12&&Math.abs(clear.z()*2.5-storm.z())<1e-12,"Weather scales the same direction continuously");
            var next=WindRules.field(seed,tick+1,1,1);
            check(Math.hypot(next.x()-storm.x(),next.z()-storm.z())<.00012,"Per-tick gust and anchor transitions stay smooth");
        }
        check(WindRules.wingGain(1,2)<WindRules.wingGain(1,1),"Extra load reduces drift");
        check(WindRules.wingGain(6,1)==.10&&WindRules.wingGain(0,1)==0,"Wing force is bounded and requires a wing");
        check(WindRules.descentPenalty(-4)==0&&WindRules.descentPenalty(2)==.015,"Wet descent bounded");
        System.out.println("WILDCRAFT P10 16004 seed/time/weather bounds, deterministic reload, smooth transitions and load limits passed");
    }
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
