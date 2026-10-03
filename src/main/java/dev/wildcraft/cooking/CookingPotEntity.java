package dev.wildcraft.cooking;

import java.util.List;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;

/** Three ingredient slots, a real bowl input, and one immutable cooked output. */
public final class CookingPotEntity extends BaseContainerBlockEntity {
    public static final int COOK_TICKS=100;
    private NonNullList<ItemStack> items=NonNullList.withSize(5,ItemStack.EMPTY);
    private int progress;
    private MealData pending;
    private int status;
    private final ContainerData data=new ContainerData() {
        public int get(int index){return index==0?progress:status;}
        public void set(int index,int value){if(index==0)progress=value;else status=value;}
        public int getCount(){return 2;}
    };
    public CookingPotEntity(BlockPos pos,BlockState state){super(CookingContent.POT_ENTITY,pos,state);}
    @Override public int getContainerSize(){return 5;}
    @Override protected NonNullList<ItemStack> getItems(){return items;}
    @Override protected void setItems(NonNullList<ItemStack> items){this.items=items;}
    @Override protected Component getDefaultName(){return Component.translatable("block.wildcraft.cooking_pot");}
    @Override protected AbstractContainerMenu createMenu(int id,Inventory inventory){return new CookingMenu(id,inventory,this,data);}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){return slot<3?CookingRecipes.accepts(stack):slot==3 && stack.is(Items.BOWL);}
    @Override protected void loadAdditional(ValueInput in){
        super.loadAdditional(in);items=NonNullList.withSize(5,ItemStack.EMPTY);ContainerHelper.loadAllItems(in,items);
        progress=Math.clamp(in.getIntOr("CookTicks",0),0,COOK_TICKS-1);pending=in.read("PendingMeal",MealData.CODEC).orElse(null);
        if(pending==null)progress=0;
    }
    @Override protected void saveAdditional(ValueOutput out){
        super.saveAdditional(out);ContainerHelper.saveAllItems(out,items);out.putInt("CookTicks",progress);out.storeNullable("PendingMeal",MealData.CODEC,pending);
    }
    public int progress(){return progress;}
    public int status(){return status;}
    public static void tick(Level level,BlockPos pos,BlockState state,CookingPotEntity pot){
        var recipe=CookingRecipes.match(List.of(pot.items.get(0),pot.items.get(1),pot.items.get(2)));
        if(!java.util.Objects.equals(recipe,pot.pending)){pot.pending=recipe;pot.progress=0;pot.setChanged();}
        var under=level.getBlockState(pos.below());
        boolean hot=(under.getBlock() instanceof CampfireBlock && under.getValue(CampfireBlock.LIT)) || under.getBlock() instanceof BaseFireBlock;
        pot.status=!pot.items.get(4).isEmpty()?4:recipe==null?0:!pot.items.get(3).is(Items.BOWL)?3:!hot?1:2;
        if(pot.status!=2)return;
        if(++pot.progress>=COOK_TICKS){
            // Revalidate all live slots above each tick. Output is committed once, with no early ingredient charge.
            pot.items.set(4,CookingContent.meal(recipe));
            for(int i=0;i<4;i++)if(!pot.items.get(i).isEmpty())pot.items.get(i).shrink(1);
            pot.progress=0;
        }
        pot.setChanged();
    }
}
