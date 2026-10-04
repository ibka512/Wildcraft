package dev.wildcraft.fabrication;

import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;

/** One committed job shares one saved record with its real inventory. */
public final class FabricatorEntity extends BaseContainerBlockEntity {
    public static final int DURATION=100;
    private NonNullList<ItemStack> items=NonNullList.withSize(3,ItemStack.EMPTY);
    private ItemStack pending=ItemStack.EMPTY;
    private int progress;
    private final ContainerData data=new ContainerData(){
        public int get(int i){return i==0?progress:status();}
        public void set(int i,int v){if(i==0)progress=v;}
        public int getCount(){return 2;}
    };
    public FabricatorEntity(BlockPos pos,BlockState state){super(FabricationContent.ENTITY,pos,state);}
    @Override public int getContainerSize(){return 3;}
    @Override protected NonNullList<ItemStack> getItems(){return items;}
    @Override protected void setItems(NonNullList<ItemStack> items){this.items=items;}
    @Override protected Component getDefaultName(){return Component.translatable("block.wildcraft.fabricator");}
    @Override protected AbstractContainerMenu createMenu(int id,Inventory inv){return new FabricatorMenu(id,inv,this,data);}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){return slot==0?stack.is(Items.COPPER_INGOT):slot==1&&stack.is(Items.REDSTONE);}
    public boolean hasJob(){return !pending.isEmpty();}
    public ItemStack pendingResult(){return pending.copy();}
    public int progress(){return progress;}
    private boolean materials(){return items.get(0).is(Items.COPPER_INGOT)&&items.get(0).getCount()>=4&&items.get(1).is(Items.REDSTONE)&&items.get(1).getCount()>=2;}
    public int status(){return hasJob()?(progress>=DURATION?3:2):!items.get(2).isEmpty()?4:materials()?0:1;}
    public boolean start(Player player){
        if(level==null||level.isClientSide()||isRemoved()||!player.isAlive()||player.isSpectator()||player.level()!=level||!stillValid(player)||status()!=0)return false;
        var result=FabricationPool.result(level.getRandom().nextInt(FabricationPool.PARTS.size()));
        items.get(0).shrink(4);items.get(1).shrink(2);pending=result;progress=0;setChanged();return true;
    }
    @Override protected void loadAdditional(ValueInput in){
        super.loadAdditional(in);if(in.getIntOr("FabricationSchema",1)!=1)throw new IllegalArgumentException("Unsupported fabrication schema");
        items=NonNullList.withSize(3,ItemStack.EMPTY);ContainerHelper.loadAllItems(in,items);
        pending=in.read("PendingOutput",ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);progress=in.getIntOr("FabricationTicks",0);
        if(in.getBooleanOr("HasJob",false)!=hasJob()||hasJob()&&!FabricationPool.valid(pending)||progress<0||progress>DURATION||!hasJob()&&progress!=0)
            throw new IllegalArgumentException("Invalid committed fabrication job; refusing to reroll");
    }
    @Override protected void saveAdditional(ValueOutput out){
        super.saveAdditional(out);ContainerHelper.saveAllItems(out,items);out.putInt("FabricationSchema",1);out.putInt("FabricationTicks",progress);out.putBoolean("HasJob",hasJob());out.store("PendingOutput",ItemStack.OPTIONAL_CODEC,pending);
    }
    @Override protected void collectImplicitComponents(DataComponentMap.Builder components){
        super.collectImplicitComponents(components);
        if(level!=null){var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());saveCustomOnly(out);removeComponentsFromTag(out);
            components.set(DataComponents.BLOCK_ENTITY_DATA,TypedEntityData.of(getType(),out.buildResult()));}
    }
    // Contents travel in the machine's single loot stack; the native default would scatter another copy.
    @Override public void preRemoveSideEffects(BlockPos pos,BlockState state){ }
    public static void tick(Level level,BlockPos pos,BlockState state,FabricatorEntity machine){
        if(!machine.hasJob())return;
        if(machine.progress<DURATION)machine.progress++;
        if(machine.progress>=DURATION&&machine.items.get(2).isEmpty()){machine.items.set(2,machine.pending);machine.pending=ItemStack.EMPTY;machine.progress=0;}
        machine.setChanged();
    }
}
