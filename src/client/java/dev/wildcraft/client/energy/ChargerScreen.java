package dev.wildcraft.client.energy;

import dev.wildcraft.client.art.ArtGui;
import dev.wildcraft.energy.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class ChargerScreen extends AbstractContainerScreen<ChargerMenu> {
    public ChargerScreen(ChargerMenu menu,Inventory inventory,Component title){super(menu,inventory,title);titleLabelY=7;inventoryLabelY=72;}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mouseX,int mouseY,float delta){
        super.extractBackground(g,mouseX,mouseY,delta);int x=leftPos,y=topPos;
        ArtGui.panel(g,"charger",x,y,176,166);boolean present=menu.getSlot(0).getItem().is(EnergyContent.BATTERY);int energy=menu.energy(),status=Math.clamp(menu.status(),0,4);
        ArtGui.battery(g,x+54,y+29,energy,present);
        g.text(font,(present?Integer.toString(energy):"—")+" / "+BatteryData.CAPACITY,x+74,y+32,present&&energy>0&&energy<100?0xFF805014:0xFF354438,false);
        g.fill(x+55,y+48,x+55+105*energy/BatteryData.CAPACITY,y+53,0xFF608865);
        ArtGui.glyph(g,"charger",8*status,48,x+10,y+60);
        ArtGui.text(g,Component.translatable("energy.wildcraft.screen.status."+status),x+22,y+60,146,status==1||status==4?0xFF805014:0xFF354438);
        g.text(font,Component.translatable("energy.wildcraft.battery_label"),x+14,y+22,0xFF354438,false);
        g.text(font,Component.translatable("energy.wildcraft.charge_label"),x+78,y+22,0xFF354438,false);
        if(mouseX>=x+8&&mouseX<x+168&&mouseY>=y+57&&mouseY<y+71)g.setTooltipForNextFrame(font,Component.translatable("energy.wildcraft.screen.help."+status),mouseX,mouseY);
    }
}
