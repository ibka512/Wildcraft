package dev.wildcraft.cooking;

import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;

public final class CookingMenu extends AbstractContainerMenu {
    private final Container pot;
    private final ContainerData data;
    public CookingMenu(int id,Inventory inventory){this(id,inventory,new SimpleContainer(5),new SimpleContainerData(2));}
    public CookingMenu(int id,Inventory inventory,Container pot,ContainerData data){
        super(CookingContent.MENU,id);checkContainerSize(pot,5);checkContainerDataCount(data,2);this.pot=pot;this.data=data;
        for(int i=0;i<3;i++)addSlot(new Slot(pot,i,26+i*20,33){@Override public boolean mayPlace(ItemStack stack){return CookingRecipes.accepts(stack);}});
        addSlot(new Slot(pot,3,46,58){@Override public boolean mayPlace(ItemStack stack){return stack.is(Items.BOWL);}});
        addSlot(new Slot(pot,4,134,45){@Override public boolean mayPlace(ItemStack stack){return false;}});
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,9+row*9+col,8+col*18,100+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,8+col*18,158));
        addDataSlots(data);
    }
    public int progress(){return data.get(0);}
    public int status(){return data.get(1);}
    @Override public boolean stillValid(Player player){return pot.stillValid(player);}
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(index<0 || index>=slots.size())return ItemStack.EMPTY;
        var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;
        var stack=slot.getItem();var before=stack.copy();
        if(index<5){if(!moveItemStackTo(stack,5,41,true))return ItemStack.EMPTY;}
        else if(stack.is(Items.BOWL)){if(!moveItemStackTo(stack,3,4,false))return ItemStack.EMPTY;}
        else if(CookingRecipes.accepts(stack)){if(!moveItemStackTo(stack,0,3,false))return ItemStack.EMPTY;}
        else return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();
        if(stack.getCount()==before.getCount())return ItemStack.EMPTY;
        slot.onTake(player,stack);return before;
    }
}
