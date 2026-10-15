package dev.wildcraft.client.animation.rig;

import dev.wildcraft.combat.ParryActions;
import java.util.Arrays;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.item.Items;
import org.joml.Matrix4f;

/** Authored successful-guard arm delta over the current qualified body; no gameplay or forced pose. */
public final class GuardRig {
    public record Pulse(String clip,float time){}
    public static final RenderStateDataKey<Pulse> PULSE=RenderStateDataKey.create(()->"wildcraft:guard_rig_pulse");
    private GuardRig(){}
    private static String variant(AvatarRenderState s){return s.skin.model()==PlayerModelType.SLIM?"slim":"standard";}
    private static String clip(Player p){return "guard_pulse_"+(p.getMainArm()==HumanoidArm.RIGHT?"R":"L");}
    static boolean available(Player p,AvatarRenderState s){
        if(ParryActions.view(p).remaining()<=0||s.scale!=1||!p.isUsingItem()||p.getUsedItemHand()!=InteractionHand.OFF_HAND||!p.getUseItem().is(Items.SHIELD))return false;
        var d=CombatRig.asset(variant(s));return d!=null&&d.clips().containsKey(clip(p));
    }
    static void mark(Player p,AvatarRenderState s,float partial){
        if(!available(p,s))return;
        float t=Math.clamp((ParryActions.PULSE-ParryActions.view(p).remaining()+(Minecraft.getInstance().isPaused()?0:partial))/ParryActions.PULSE,0,1);
        s.setData(PULSE,new Pulse(clip(p),t));
    }
    static void apply(Player p,AvatarRenderState s,CombatRig.Data d,Matrix4f[] global,float weight){
        var pulse=s.getData(PULSE);if(pulse==null)return;String v=variant(s);var start=CombatRig.sample(v,pulse.clip(),0);var sample=CombatRig.sample(v,pulse.clip(),pulse.time());var names=Arrays.asList(d.bones());int body=names.indexOf("body");
        // Conjugate only the authored active-arm change into this frame's body space.
        // Opposite hand, body, legs and head keep their actual already-selected transforms.
        var basis=new Matrix4f(global[body]).mul(new Matrix4f(start[body]).invert());var inverse=new Matrix4f(basis).invert();
        String active=p.getMainArm()==HumanoidArm.RIGHT?"left_":"right_";
        for(int i=0;i<global.length;i++){
            String n=d.bones()[i];if(!n.startsWith(active)||!(n.contains("upper_arm")||n.contains("elbow")||n.contains("forearm")||n.contains("wrist")||n.contains("palm_grip")||n.contains("shield_mount")||n.contains("sword_reference")))continue;
            var target=new Matrix4f(basis).mul(sample[i]).mul(new Matrix4f(start[i]).invert()).mul(inverse).mul(global[i]);global[i]=CombatRig.blend(global[i],target,weight);
        }
    }
}
