package dev.wildcraft.test;
import dev.wildcraft.client.animation.rig.CombatRig;
import java.util.Arrays;
import net.fabricmc.fabric.api.client.gametest.v1.*;
import org.joml.Vector3f;
/** Reads candidate resources only. No native-use, renderer ownership or gameplay completion claim. */
public final class BowGroundMappingSourceProbe implements FabricClientGameTest {
    @Override public void runTest(net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext context){context.runOnClient(c->{
        for(String variant:new String[]{"standard","slim"}){
            var d=CombatRig.asset(variant);var clip=d.clips().get("bow_ground_draw_empty_L");if(clip==null||clip.pullTimes()==null)throw new AssertionError("Candidate ground source curve absent");var names=Arrays.asList(d.bones());int bow=names.indexOf("left_sword_reference"),hand=names.indexOf("right_palm_grip");
            var first=CombatRig.sample(variant,"bow_ground_draw_empty_L",CombatRig.pullSample(clip,0));var last=CombatRig.sample(variant,"bow_ground_draw_empty_L",CombatRig.pullSample(clip,1));float low=first[hand].getTranslation(new Vector3f()).distance(first[bow].getTranslation(new Vector3f())),high=last[hand].getTranslation(new Vector3f()).distance(last[bow].getTranslation(new Vector3f()));float error=0;
            for(int i=0;i<=400;i++){float pull=i/400F;var sample=CombatRig.sample(variant,"bow_ground_draw_empty_L",CombatRig.pullSample(clip,pull));float value=sample[hand].getTranslation(new Vector3f()).distance(sample[bow].getTranslation(new Vector3f()));error=Math.max(error,Math.abs((value-low)/(high-low)-pull));}
            System.out.println("C6P SOURCE MAPPING "+variant+" actual runtime palette max relative pull-distance error="+error+"; renderer/gameplay not activated");if(error>.005F)throw new AssertionError("Resampled source curve is not yet qualified: "+error);
        }
    });}
}
