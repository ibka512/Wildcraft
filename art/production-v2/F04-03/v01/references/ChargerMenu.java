package dev.wildcraft.energy;

import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class ChargerMenu extends AbstractContainerMenu {
    private final Container charger;
    private final ContainerData data;
    public ChargerMenu(int id,Inventory inv){this(id,inv,new SimpleContainer(1),new SimpleContainerData(2));}
    public ChargerMenu(int id,Inventory inv,Container charger,ContainerData data){
        super(EnergyContent.MENU,id);checkContainerSize(charger,1);checkContainerDataCount(data,2);this.charger=charger;this.data=data;
        addSlot(new Slot(charger,0,26,35){@Override public boolean mayPlace(ItemStack stack){return stack.is(EnergyContent.BATTERY);}});
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inv,9+row*9+col,8+col*18,84+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inv,col,8+col*18,142));
        addDataSlots(data);
    }
    public int energy(){return data.get(0);}
    public int status(){return data.get(1);}
    @Override public boolean stillValid(Player p){return charger.stillValid(p);}
    @Override public ItemStack quickMoveStack(Player p,int index){
        if(index<0 || index>=slots.size())return ItemStack.EMPTY;
        var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;
        var stack=slot.getItem();var before=stack.copy();
        if(index==0){if(!moveItemStackTo(stack,1,37,true))return ItemStack.EMPTY;}
        else if(!stack.is(EnergyContent.BATTERY) || !moveItemStackTo(stack,0,1,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();
        if(stack.getCount()==before.getCount())return ItemStack.EMPTY;
        slot.onTake(p,stack);return before;
    }
}
