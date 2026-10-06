package dev.wildcraft.fabrication;

import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;

public final class FabricatorMenu extends AbstractContainerMenu {
    private final Container machine;
    private final ContainerData data;
    public FabricatorMenu(int id,Inventory inv){this(id,inv,new SimpleContainer(3),new SimpleContainerData(2));}
    public FabricatorMenu(int id,Inventory inv,Container machine,ContainerData data){
        super(FabricationContent.MENU,id);checkContainerSize(machine,3);checkContainerDataCount(data,2);this.machine=machine;this.data=data;
        for(int i=0;i<2;i++){final int slot=i;addSlot(new Slot(machine,i,26+i*28,35){@Override public boolean mayPlace(ItemStack s){return slot==0?s.is(Items.COPPER_INGOT):s.is(Items.REDSTONE);}});}
        addSlot(new Slot(machine,2,134,54){@Override public boolean mayPlace(ItemStack s){return false;}});
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inv,9+row*9+col,8+col*18,110+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inv,col,8+col*18,168));addDataSlots(data);
    }
    public int progress(){return data.get(0);}
    public int status(){return data.get(1);}
    @Override public boolean stillValid(Player p){return machine.stillValid(p);}
    @Override public boolean clickMenuButton(Player p,int button){return button==0&&stillValid(p)&&machine instanceof FabricatorEntity entity&&entity.start(p);}
    @Override public ItemStack quickMoveStack(Player p,int index){
        if(index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;var stack=slot.getItem();var before=stack.copy();
        if(index<3){if(!moveItemStackTo(stack,3,39,true))return ItemStack.EMPTY;}
        else if(stack.is(Items.COPPER_INGOT)){if(!moveItemStackTo(stack,0,1,false))return ItemStack.EMPTY;}
        else if(stack.is(Items.REDSTONE)){if(!moveItemStackTo(stack,1,2,false))return ItemStack.EMPTY;}
        else return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();if(stack.getCount()==before.getCount())return ItemStack.EMPTY;slot.onTake(p,stack);return before;
    }
}
