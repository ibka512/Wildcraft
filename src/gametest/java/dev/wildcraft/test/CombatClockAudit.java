package dev.wildcraft.test;

import dev.wildcraft.client.animation.MeleePoses;
import dev.wildcraft.client.animation.rig.CombatRigLayer;
import dev.wildcraft.network.MeleeView;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/** Observes the actual submitted avatar, including the native frozen render fraction. */
public final class CombatClockAudit {
    public record Sample(CombatRigLayer.Frame frame, MeleeView view, float partial) { }
    public static boolean enabled;
    public static int frames;
    public static Sample last;
    private CombatClockAudit() { }
    public static void begin() { enabled=true; frames=0; last=null; }
    public static void observe(AvatarRenderState state) {
        if(!enabled)return;
        var frame=state.getData(CombatRigLayer.FRAME);
        var view=state.getData(MeleePoses.LAYER);
        if(frame!=null&&view!=null&&(view.phase()==MeleeView.SWING||view.phase()==MeleeView.AIR_SWING)) {
            frames++;
            last=new Sample(frame,view,state.getDataOrDefault(MeleePoses.PARTIAL,0F));
        }
    }
}
