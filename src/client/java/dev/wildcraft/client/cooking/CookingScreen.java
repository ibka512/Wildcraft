package dev.wildcraft.client.cooking;

import dev.wildcraft.client.art.ArtGui;
import dev.wildcraft.cooking.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class CookingScreen extends AbstractContainerScreen<CookingMenu> {
    private static final int[] COLORS={0xFF716B51,0xFF966C35,0xFF9C662F,0xFF966C35,0xFF527046};
    public CookingScreen(CookingMenu menu,Inventory inventory,Component title){super(menu,inventory,title,176,182);titleLabelY=7;inventoryLabelY=89;}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta){
        super.extractBackground(g,mouseX,mouseY,delta);int x=leftPos,y=topPos;
        ArtGui.panel(g,"cooking",x,y,176,182);
        g.fill(x+92,y+49,x+92+30*menu.progress()/CookingPotEntity.COOK_TICKS,y+54,0xFFC38442);
        int status=Math.clamp(menu.status(),0,4),color=COLORS[status];
        // Five small state glyphs are drawn independently of text and the actual inventory.
        if(status==4){g.fill(x+11,y+81,x+13,y+83,color);g.fill(x+13,y+79,x+15,y+81,color);g.fill(x+15,y+77,x+17,y+79,color);}
        else if(status==0){for(int i=0;i<3;i++)g.fill(x+11+i*3,y+81,x+13+i*3,y+83,color);}
        else if(status==3){g.fill(x+11,y+80,x+18,y+82,color);g.fill(x+13,y+82,x+16,y+84,color);}
        else for(int i=0;i<3;i++)g.fill(x+11+i*3,y+78+(i%2),x+12+i*3,y+83,color);
        ArtGui.text(g,Component.translatable("cooking.wildcraft.status."+status),x+24,y+77,144,color);
        g.text(font,Component.translatable("cooking.wildcraft.ingredients"),x+16,y+21,0xFF443D32,false);
        g.text(font,Component.translatable("cooking.wildcraft.bowl"),x+67,y+61,0xFF443D32,false);
        g.text(font,Component.translatable("cooking.wildcraft.output"),x+127,y+30,0xFF443D32,false);
    }
}
