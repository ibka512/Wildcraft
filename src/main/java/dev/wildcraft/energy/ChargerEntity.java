package dev.wildcraft.energy;

import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;

public final class ChargerEntity extends BaseContainerBlockEntity {
    private NonNullList<ItemStack> items=NonNullList.withSize(1,ItemStack.EMPTY);
    private int status;
    private ItemStack audioBattery=ItemStack.EMPTY;
    private boolean audioStarted;
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
    @Override protected void loadAdditional(ValueInput in){
        if(in.getBooleanOr("WildcraftArtSnapshot",false)){clientArtStatus=Math.clamp(in.getIntOr("ArtState",0),0,4);clientArtEnergy=Math.clamp(in.getIntOr("ArtEnergy",0),0,1000);clientArtBattery=in.getBooleanOr("ArtBattery",false);return;}super.loadAdditional(in);items=NonNullList.withSize(1,ItemStack.EMPTY);ContainerHelper.loadAllItems(in,items);audioBattery=items.getFirst();audioStarted=audioBattery.is(EnergyContent.BATTERY);}
    @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);ContainerHelper.saveAllItems(out,items);}
    public static void tick(Level level,BlockPos pos,BlockState state,ChargerEntity charger){
        try {
        var stack=charger.getItem(0);
        if(charger.audioBattery!=stack){charger.audioBattery=stack;charger.audioStarted=false;}
        if(!stack.is(EnergyContent.BATTERY)){charger.status=0;return;}
        if(!charger.canCharge()){charger.status=3;return;}
        if(!FixedEnergy.hasSource(level,pos)){charger.status=1;return;}
        int transfer=FixedEnergy.allocate(level,pos,BatteryData.CAPACITY-Batteries.energy(stack));
        charger.status=transfer>0?2:4;
        int before=Batteries.energy(stack);
        if(Batteries.charge(stack,transfer)>0){
            charger.setChanged();
            if(level instanceof net.minecraft.server.level.ServerLevel server){
                var cue=before<1000&&Batteries.energy(stack)==1000?dev.wildcraft.art.ArtFeedback.Cue.CHARGE_FULL:!charger.audioStarted?dev.wildcraft.art.ArtFeedback.Cue.CHARGE_START:null;
                if(cue!=null)dev.wildcraft.art.ArtFeedback.send(server,dev.wildcraft.art.ArtFeedback.blockSource(pos),net.minecraft.world.phys.Vec3.atCenterOf(pos),cue,-1);
            }charger.audioStarted=true;
        }
        } finally {charger.syncArt();}
    }

    private int clientArtStatus,clientArtEnergy,lastArtStatus=-1,lastArtEnergy=-1; private boolean clientArtBattery,lastArtBattery;
    public int artStatus(){return level!=null&&level.isClientSide()?clientArtStatus:status;} public int artEnergy(){return level!=null&&level.isClientSide()?clientArtEnergy:Batteries.energy(getItem(0));} public boolean artBattery(){return level!=null&&level.isClientSide()?clientArtBattery:getItem(0).is(EnergyContent.BATTERY);}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
    @Override public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries){var out=new net.minecraft.nbt.CompoundTag();out.putBoolean("WildcraftArtSnapshot",true);out.putInt("ArtState",status);out.putInt("ArtEnergy",Batteries.energy(getItem(0)));out.putBoolean("ArtBattery",getItem(0).is(EnergyContent.BATTERY));return out;}

    public void syncArt(){
        if(level==null||level.isClientSide())return;
        int state=status;
        if(lastArtStatus==state &&lastArtEnergy==Batteries.energy(getItem(0))&&lastArtBattery==getItem(0).is(EnergyContent.BATTERY))return;
        lastArtStatus=state; lastArtEnergy=Batteries.energy(getItem(0));lastArtBattery=getItem(0).is(EnergyContent.BATTERY);
        level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);
    }
}
