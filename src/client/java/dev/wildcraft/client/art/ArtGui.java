package dev.wildcraft.client.art;

import dev.wildcraft.Wildcraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

/** Shared clipping of authored icons; neither inventory nor gameplay state is cached. */
public final class ArtGui {
    private ArtGui(){}
    public static void panel(GuiGraphicsExtractor g,String name,int x,int y,int w,int h){g.blit(RenderPipelines.GUI_TEXTURED,Wildcraft.id("textures/gui/"+name+".png"),x,y,0,0,w,h,w,h);}
    public static void icon(GuiGraphicsExtractor g,String name,int x,int y){g.blit(RenderPipelines.GUI_TEXTURED,Wildcraft.id("textures/gui/"+name+".png"),x,y,0,0,16,16,16,16);}
    public static void glyph(GuiGraphicsExtractor g,String atlas,int u,int v,int x,int y){g.blit(RenderPipelines.GUI_TEXTURED,Wildcraft.id("textures/block/"+atlas+"_atlas.png"),x,y,u,v,8,8,64,64);}
    public static void battery(GuiGraphicsExtractor g,int x,int y,int energy,boolean present){
        icon(g,"battery-empty",x,y);if(!present)return;
        for(int i=0;i<3;i++){
            int width=5*Math.clamp(3*energy-i*1000,0,1000)/1000;if(width==0)continue;
            int sy=12-3*i;g.blit(RenderPipelines.GUI_TEXTURED,Wildcraft.id("textures/gui/battery-full.png"),x+6,y+sy,6,sy,width,2,16,16);
        }
    }
    public static void text(GuiGraphicsExtractor g,Component text,int x,int y,int maxWidth,int colour){
        var font=Minecraft.getInstance().font;
        g.text(font,font.plainSubstrByWidth(text.getString(),maxWidth),x,y,colour,false);
    }
}
