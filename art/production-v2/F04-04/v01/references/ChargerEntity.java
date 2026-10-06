package dev.wildcraft.energy;

import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;

public final class ChargerEntity extends BaseContainerBlockEntity {
    private NonNullList<ItemStack> items=NonNullList.withSize(1,ItemStack.EMPTY);
    private int status;
    private final ContainerData data=new ContainerData(){
        public int get(int i){return i==0?Batteries.energy(items.getFirst()):status;}
        public void set(int i,int v){if(i==1)status=v;}
        public int getCount(){return 2;}
    };
    public ChargerEntity(BlockPos pos,BlockState state){super(EnergyContent.CHARGER_ENTITY,pos,state);}
    public boolean canCharge(){return getItem(0).is(EnergyContent.BATTERY) && Batteries.energy(getItem(0))<BatteryData.CAPACITY;}
    @Override public int getContainerSize(){return 1;}
    @Override protected NonNullList<ItemStack> getItems(){return items;}
    @Override protected void setItems(NonNullList<ItemStack> items){this.items=items;}
    @Override protected Component getDefaultName(){return Component.translatable("block.wildcraft.charger");}
    @Override protected AbstractContainerMenu createMenu(int id,Inventory inv){return new ChargerMenu(id,inv,this,data);}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){return slot==0 && stack.is(EnergyContent.BATTERY);}
    @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);items=NonNullList.withSize(1,ItemStack.EMPTY);ContainerHelper.loadAllItems(in,items);}
    @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);ContainerHelper.saveAllItems(out,items);}
    public static void tick(Level level,BlockPos pos,BlockState state,ChargerEntity charger){
        var stack=charger.getItem(0);
        if(!stack.is(EnergyContent.BATTERY)){charger.status=0;return;}
        if(!charger.canCharge()){charger.status=3;return;}
        if(!FixedEnergy.hasSource(level,pos)){charger.status=1;return;}
        int transfer=FixedEnergy.allocate(level,pos,BatteryData.CAPACITY-Batteries.energy(stack));
        charger.status=transfer>0?2:4;
        if(Batteries.charge(stack,transfer)>0)charger.setChanged();
    }
}
