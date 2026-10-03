package dev.wildcraft.client.cooking;

import dev.wildcraft.Wildcraft;
import dev.wildcraft.cooking.CookingEffects;
import dev.wildcraft.client.temperature.TemperatureHud;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class MealHud {
    private MealHud(){ }
    public static void initialize(){
        HudElementRegistry.attachElementAfter(Wildcraft.id("temperature"),Wildcraft.id("meals"),(g,t) -> {
            var c=Minecraft.getInstance();var p=c.player;
            if(p==null || !p.isAlive() || p.isSpectator() || c.gui.hud.isHidden())return;
            var v=p.getAttached(CookingEffects.VIEW);if(v==null)return;
            int[] levels={v.warmth(),v.cooling(),v.recovery()},seconds={v.warmSeconds(),v.coolSeconds(),v.recoverySeconds()};
            int y=TemperatureHud.rowY()+13;
            for(int i=0;i<3;i++)if(levels[i]>0 && y+10<g.guiHeight()-2){
                var label=Component.translatable("hud.wildcraft.meal."+(i+1),levels[i],seconds[i]);
                g.text(c.font,label,12,y,0xFFE1D7AE);y+=11;
            }
        });
    }
}
