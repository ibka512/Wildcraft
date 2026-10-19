package dev.wildcraft.test;
import dev.wildcraft.client.animation.MeleePoses;
import dev.wildcraft.client.animation.rig.CombatRigLayer;
import dev.wildcraft.network.MeleeView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
/** Observes the actual skin submission even after native GUI cancellation removes its source. */
public final class HopPauseAudit {
 public record Sample(CombatRigLayer.Frame frame,MeleeView view,float partial,int tick){}
 public static boolean enabled;public static int frames;public static Sample last;
 private HopPauseAudit(){}
 public static void begin(){enabled=true;frames=0;last=null;}
 public static void observe(AvatarRenderState s){if(!enabled)return;frames++;last=new Sample(s.getData(CombatRigLayer.FRAME),s.getDataOrDefault(MeleePoses.LAYER,MeleeView.OFF),s.getDataOrDefault(MeleePoses.PARTIAL,0F),Minecraft.getInstance().player.tickCount);}
}
