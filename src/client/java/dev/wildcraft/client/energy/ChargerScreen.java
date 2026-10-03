package dev.wildcraft.client.energy;

import dev.wildcraft.energy.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class ChargerScreen extends AbstractContainerScreen<ChargerMenu> {
    public ChargerScreen(ChargerMenu menu,Inventory inventory,Component title){super(menu,inventory,title);}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta){
        super.extractBackground(g,mouseX,mouseY,delta);int x=leftPos,y=topPos;
        g.fill(x,y,x+imageWidth,y+imageHeight,0xFFB4C2B5);g.fill(x+3,y+3,x+imageWidth-3,y+imageHeight-3,0xFFD3DDD1);
        for(var slot:menu.slots){g.fill(x+slot.x-1,y+slot.y-1,x+slot.x+17,y+slot.y+17,0xFF637066);g.fill(x+slot.x,y+slot.y,x+slot.x+16,y+slot.y+16,0xFF9AA99D);}
        g.text(font,Component.translatable("energy.wildcraft.charge",menu.energy(),BatteryData.CAPACITY),x+54,y+32,0xFF354438,false);
        g.fill(x+54,y+47,x+161,y+54,0xFF637066);g.fill(x+55,y+48,x+55+105*menu.energy()/BatteryData.CAPACITY,y+53,0xFF608865);
        g.text(font,Component.translatable("energy.wildcraft.status."+menu.status()),x+8,y+64,0xFF354438,false);
    }
}
