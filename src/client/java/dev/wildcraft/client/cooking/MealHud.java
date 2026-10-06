package dev.wildcraft.client.cooking;

import dev.wildcraft.Wildcraft;
import dev.wildcraft.cooking.CookingEffects;
import dev.wildcraft.client.art.ArtGui;
import dev.wildcraft.client.temperature.TemperatureHud;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import java.util.*;

public final class MealHud {
    public record Box(int kind,int x,int y,boolean compact){}
    private static final String[] ICONS={"warmth-meal","cooling-meal","stamina-recovery"};
    private MealHud(){}
    /** Fits the real heart stack, bottom HUD and a 20px aiming exclusion. */
    public static List<Box> layout(int width,int height,int startY,int[] levels,int[] seconds){
        var active=new ArrayList<Integer>();for(int i=0;i<3;i++)if(levels[i]>0&&seconds[i]>0)active.add(i);
        if(active.isEmpty())return List.of();int bottom=height-46,aimLeft=width/2-10,aimTop=height/2-10,aimBottom=height/2+10;
        int fullBottom=startY+(active.size()-1)*20+16;
        boolean full=width>=172 && fullBottom<=bottom && (160<=aimLeft || fullBottom<=aimTop || startY>=aimBottom);
        int columns=Math.max(1,Math.min(active.size(),(Math.min(width-12,aimLeft)-12+6)/59));
        var boxes=new ArrayList<Box>();for(int n=0;n<active.size();n++){
            int x=12+(full?0:n%columns*59),y=startY+(full?n*20:n/columns*24);
            int w=full?148:53,h=full?16:20;
            if(x+w>width-12 || y+h>bottom || x<width/2+10 && x+w>aimLeft && y<aimBottom && y+h>aimTop)return List.of();
            boxes.add(new Box(active.get(n),x,y,!full));
        }return List.copyOf(boxes);
    }
    public static String time(int seconds){return seconds/60+":"+String.format(Locale.ROOT,"%02d",seconds%60);}
    public static void initialize(){
        HudElementRegistry.attachElementAfter(Wildcraft.id("temperature"),Wildcraft.id("meals"),(g,t)->{
            var c=Minecraft.getInstance();var p=c.player;if(p==null||!p.isAlive()||p.isSpectator()||c.gui.hud.isHidden())return;
            var v=p.getAttached(CookingEffects.VIEW);if(v==null)return;
            int[] levels={v.warmth(),v.cooling(),v.recovery()},seconds={v.warmSeconds(),v.coolSeconds(),v.recoverySeconds()};
            for(var b:layout(g.guiWidth(),g.guiHeight(),TemperatureHud.rowY()+14,levels,seconds)){
                int i=b.kind(),x=b.x(),y=b.y();g.fill(x-2,y-1,x+(b.compact()?53:148),y+(b.compact()?20:16),0x7015231B);
                ArtGui.icon(g,ICONS[i],x,y+(b.compact()?2:0));String level=levels[i]==1?"I":"II";
                if(b.compact())g.text(c.font,level,x+19,y,0xFFE1D7AE);
                else {
                    String name=Component.translatable("hud.wildcraft.meal.name."+(i+1)).getString();int space=90-c.font.width(" "+level);
                    if(c.font.width(name)>space)name=c.font.plainSubstrByWidth(name,space-c.font.width("…"))+"…";
                    g.text(c.font,name+" "+level,x+20,y+4,0xFFE1D7AE);
                }
                g.text(c.font,time(seconds[i]),x+(b.compact()?19:114),y+(b.compact()?10:4),seconds[i]<=10?0xFFF0B84D:0xFFD4DCCB);
            }
        });
    }
}
