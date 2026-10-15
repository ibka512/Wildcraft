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
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

/** Adopted F07 icons follow the actual owner wind view and retain the aiming/bottom exclusion. */
public final class WindHud {
    private static final String[] ARROWS={"↑","↗","→","↘","↓","↙","←","↖"};
    public record Row(int y,int width,boolean compact){}
    private WindHud(){}
    public static int direction(float yaw,WindView wind){
        double angle=Math.toRadians(yaw);
        double forward=-Math.sin(angle)*wind.x()+Math.cos(angle)*wind.z();
        double right=-Math.cos(angle)*wind.x()-Math.sin(angle)*wind.z();
        return Math.floorMod((int)Math.round(Math.atan2(right,forward)/(Math.PI/4)),8);
    }
    public static String arrow(float yaw,WindView wind){return ARROWS[direction(yaw,wind)];}
    public static int strength(WindView wind){double speed=Math.hypot(wind.x(),wind.z());return speed<.012?0:speed<.024?1:2;}
    public static boolean fits(int width,int height,int y,int textWidth){
        return textWidth>0 && y>=0 && y+20<=height-46 && 12+textWidth<=width-12
                && (14+textWidth<=width/2-10 || y+20<=height/2-10 || y>=height/2+10);
    }
    /** Compact icons are preferable to cutting translated status text or covering existing HUD. */
    public static Row layout(int width,int height,int y,int labelWidth,boolean outdoors,boolean wet){
        int icons=(outdoors?36:16)+(wet?20:0);
        int full=icons+4+Math.max(0,labelWidth);
        if(fits(width,height,y,full))return new Row(y,full,false);
        if(fits(width,height,y,icons))return new Row(y,icons,true);
        return null;
    }
    private static void icon(GuiGraphicsExtractor g,int index,int x,int y){
        g.blit(RenderPipelines.GUI_TEXTURED,Wildcraft.id("textures/gui/wind-weather-atlas.png"),
                x,y,index%4*16,index/4*16,16,16,64,64);
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
            String key=!wind.outdoors()?"sheltered":switch(strength(wind)){case 0->"light";case 1->"moderate";default->"strong";};
            String label=Component.translatable("hud.wildcraft.wind."+key).getString();
            if(wind.precipitation()>.2)label+=Component.translatable("hud.wildcraft.wind.wet").getString();
            boolean wet=wind.precipitation()>.2;
            var row=layout(g.guiWidth(),g.guiHeight(),y,c.font.width(label),wind.outdoors(),wet);
            if(row==null)return;
            g.fill(10,y,14+row.width(),y+20,0x7015231B);int x=12;
            if(wind.outdoors()){icon(g,direction(p.getYRot(),wind),x,y+2);x+=20;icon(g,8+strength(wind),x,y+2);x+=16;}
            else {icon(g,11,x,y+2);x+=16;}
            if(wet){x+=4;icon(g,12,x,y+2);x+=16;}
            if(!row.compact())g.text(c.font,label,x+4,y+6,0xFFCBD8DD);
        });
    }
}
