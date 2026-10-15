package dev.wildcraft.client.combat;
import dev.wildcraft.Wildcraft;
import dev.wildcraft.combat.ParryActions;
import net.fabricmc.fabric.api.client.rendering.v1.hud.*;
import net.minecraft.client.Minecraft;
/** A short native-colored reticle spark; no large combat banner or new art asset. */
public final class GuardPresentation {
    private GuardPresentation(){}
    public static void initialize(){HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR,Wildcraft.id("guard_confirm"),(g,t)->{
        var c=Minecraft.getInstance();if(c.player==null||c.gui.screen()!=null||c.gui.hud.isHidden()||ParryActions.view(c.player).remaining()<=0)return;
        int x=g.guiWidth()/2,y=g.guiHeight()/2,color=0xFFFFD377;g.fill(x-9,y-1,x-5,y+1,color);g.fill(x+5,y-1,x+9,y+1,color);g.fill(x-1,y-9,x+1,y-5,color);g.fill(x-1,y+5,x+1,y+9,color);
    });}
}
