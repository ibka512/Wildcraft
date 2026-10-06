package dev.wildcraft.client.weather;

import dev.wildcraft.Wildcraft;
import dev.wildcraft.client.cooking.MealHud;
import dev.wildcraft.client.temperature.TemperatureHud;
import dev.wildcraft.cooking.CookingEffects;
import dev.wildcraft.mechanics.MachineEntity;
import dev.wildcraft.network.WindView;
import dev.wildcraft.traversal.Gliding;
import dev.wildcraft.weather.WindSystem;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Provisional font feedback pending the B6 art; reserves the reticle and bottom HUD. */
public final class WindHud {
    private static final String[] ARROWS={"↑","↗","→","↘","↓","↙","←","↖"};
    private WindHud(){}
    public static String arrow(float yaw,WindView wind){
        double angle=Math.toRadians(yaw);
        double forward=-Math.sin(angle)*wind.x()+Math.cos(angle)*wind.z();
        double right=-Math.cos(angle)*wind.x()-Math.sin(angle)*wind.z();
        return ARROWS[Math.floorMod((int)Math.round(Math.atan2(right,forward)/(Math.PI/4)),8)];
    }
    public static boolean fits(int width,int height,int y,int textWidth){
        return y+12<=height-46 && 12+textWidth<=width-12
                && (12+textWidth<=width/2-10 || y+12<=height/2-10 || y>=height/2+10);
    }
    public static void initialize(){
        HudElementRegistry.attachElementAfter(Wildcraft.id("meals"),Wildcraft.id("wind"),(g,t)->{
            var c=Minecraft.getInstance();var p=c.player;
            if(p==null||!p.isAlive()||p.isSpectator()||c.gui.screen()!=null||c.gui.hud.isHidden())return;
            boolean wing=p.getVehicle() instanceof MachineEntity m && java.util.stream.IntStream.range(0,6).anyMatch(n->m.kind(n)==3);
            if(!Gliding.active(p)&&!wing)return;
            var wind=WindSystem.local(p);int y=TemperatureHud.rowY()+14;
            var meals=p.getAttached(CookingEffects.VIEW);
            if(meals!=null)for(var b:MealHud.layout(g.guiWidth(),g.guiHeight(),y,
                    new int[]{meals.warmth(),meals.cooling(),meals.recovery()},
                    new int[]{meals.warmSeconds(),meals.coolSeconds(),meals.recoverySeconds()}))y=Math.max(y,b.y()+(b.compact()?20:16)+4);
            double speed=Math.hypot(wind.x(),wind.z());
            String key=!wind.outdoors()?"sheltered":speed<.012?"light":speed<.024?"moderate":"strong";
            String label=(!wind.outdoors()?"":arrow(p.getYRot(),wind)+" ")+Component.translatable("hud.wildcraft.wind."+key).getString();
            if(wind.precipitation()>.2)label+=Component.translatable("hud.wildcraft.wind.wet").getString();
            int w=c.font.width(label)+4;if(!fits(g.guiWidth(),g.guiHeight(),y,w))return;
            g.fill(10,y-2,12+w,y+12,0x7015231B);g.text(c.font,label,12,y,0xFFCBD8DD);
        });
    }
}
