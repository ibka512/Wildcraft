package dev.wildcraft.client.cooking;

import dev.wildcraft.cooking.CookingMenu;
import dev.wildcraft.cooking.CookingPotEntity;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class CookingScreen extends AbstractContainerScreen<CookingMenu> {
    public CookingScreen(CookingMenu menu,Inventory inventory,Component title){super(menu,inventory,title,176,182);}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta){
        super.extractBackground(g,mouseX,mouseY,delta);
        int x=leftPos,y=topPos;
        g.fill(x,y,x+imageWidth,y+imageHeight,0xFFDDD4BD);
        g.fill(x+3,y+3,x+imageWidth-3,y+imageHeight-3,0xFFEAE3D0);
        for(var slot:menu.slots){g.fill(x+slot.x-1,y+slot.y-1,x+slot.x+17,y+slot.y+17,0xFF726C60);g.fill(x+slot.x,y+slot.y,x+slot.x+16,y+slot.y+16,0xFFADA590);}
        g.fill(x+91,y+48,x+123,y+55,0xFF726C60);
        g.fill(x+92,y+49,x+92+30*menu.progress()/CookingPotEntity.COOK_TICKS,y+54,0xFFC38442);
        g.text(font,Component.translatable("cooking.wildcraft.status."+menu.status()),x+8,y+77,0xFF443D32,false);
        g.text(font,Component.translatable("cooking.wildcraft.ingredients"),x+16,y+21,0xFF443D32,false);
        g.text(font,Component.translatable("cooking.wildcraft.bowl"),x+67,y+61,0xFF443D32,false);
    }
}
