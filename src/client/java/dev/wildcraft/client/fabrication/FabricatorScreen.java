package dev.wildcraft.client.fabrication;

import dev.wildcraft.client.art.ArtGui;
import dev.wildcraft.fabrication.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class FabricatorScreen extends AbstractContainerScreen<FabricatorMenu> {
    private Button start;
    public FabricatorScreen(FabricatorMenu m,Inventory i,Component title){super(m,i,title,176,192);titleLabelY=7;inventoryLabelY=100;}
    @Override protected void init(){super.init();start=addRenderableWidget(Button.builder(Component.translatable("fabrication.wildcraft.start"),b->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0)).bounds(leftPos+104,topPos+30,64,20).build());}
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float delta){
        super.extractBackground(g,mx,my,delta);start.active=menu.status()==0;int x=leftPos,y=topPos,status=Math.clamp(menu.status(),0,4);
        ArtGui.panel(g,"fabricator",x,y,176,192);
        ArtGui.text(g,Component.translatable("fabrication.wildcraft.cost"),x+8,y+19,160,0xFF354438);
        g.fill(x+81,y+60,x+81+41*menu.progress()/FabricatorEntity.DURATION,y+65,0xFF73A593);
        if(status==2||status==3)g.text(font,Component.literal(menu.progress()+" / "+FabricatorEntity.DURATION),x+82,y+50,0xFF354438,false);
        ArtGui.text(g,Component.translatable("fabrication.wildcraft.pool",12.5),x+8,y+76,160,0xFF354438);
        String atlas=status==1||status==3?"charger":"fabricator";int u=switch(status){case 0->32;case 1->8;case 2->40;case 3->32;default->48;};
        ArtGui.glyph(g,atlas,u,48,x+10,y+88);
        ArtGui.text(g,Component.translatable("fabrication.wildcraft.screen.status."+status),x+22,y+88,146,status==1||status==3?0xFF805014:0xFF354438);
        if(mx>=x+8&&mx<x+168&&my>=y+86&&my<y+98)g.setTooltipForNextFrame(font,Component.translatable("fabrication.wildcraft.screen.help."+status),mx,my);
        if(mx>=x+8&&mx<x+168&&my>=y+75&&my<y+85)g.setTooltipForNextFrame(font,Component.translatable("fabrication.wildcraft.pool",12.5),mx,my);
    }
}
