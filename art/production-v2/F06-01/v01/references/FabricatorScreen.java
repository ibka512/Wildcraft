package dev.wildcraft.client.fabrication;

import dev.wildcraft.fabrication.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class FabricatorScreen extends AbstractContainerScreen<FabricatorMenu> {
    private Button start;
    public FabricatorScreen(FabricatorMenu m,Inventory i,Component title){super(m,i,title,176,192);inventoryLabelY=100;}
    @Override protected void init(){super.init();start=addRenderableWidget(Button.builder(Component.translatable("fabrication.wildcraft.start"),b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0)).bounds(leftPos+104,topPos+30,64,20).build());}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float delta){
        super.extractBackground(g,mx,my,delta);start.active=menu.status()==0;int x=leftPos,y=topPos;
        g.fill(x,y,x+imageWidth,y+imageHeight,0xFF526463);g.fill(x+3,y+3,x+imageWidth-3,y+imageHeight-3,0xFFD9D4BF);
        for(var slot:menu.slots){g.fill(x+slot.x-1,y+slot.y-1,x+slot.x+17,y+slot.y+17,0xFF6A7970);g.fill(x+slot.x,y+slot.y,x+slot.x+16,y+slot.y+16,0xFFAAAF9B);}
        g.text(font,Component.translatable("fabrication.wildcraft.cost"),x+8,y+19,0xFF354438,false);
        g.fill(x+80,y+59,x+123,y+66,0xFF6A7970);g.fill(x+81,y+60,x+81+41*menu.progress()/FabricatorEntity.DURATION,y+65,0xFF73A593);
        g.text(font,Component.translatable("fabrication.wildcraft.pool",12.5),x+8,y+76,0xFF354438,false);
        g.text(font,Component.translatable("fabrication.wildcraft.status."+menu.status()),x+8,y+87,0xFF354438,false);
    }
}
