package dev.wildcraft.fabrication;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class FabricatorBlock extends BaseEntityBlock {
    public FabricatorBlock(Properties p){super(p);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new FabricatorEntity(pos,state);}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player p,BlockHitResult hit){
        if(!level.isClientSide()&&level.getBlockEntity(pos) instanceof FabricatorEntity machine)p.openMenu(machine);return InteractionResult.SUCCESS;
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide()?null:createTickerHelper(type,FabricationContent.ENTITY,FabricatorEntity::tick);}
    @Override public BlockState playerWillDestroy(Level level,BlockPos pos,BlockState state,Player p){
        if(!level.isClientSide()&&p.preventsBlockDrops()&&level.getBlockEntity(pos) instanceof FabricatorEntity machine&&(!machine.isEmpty()||machine.hasJob())){
            var stack=new ItemStack(FabricationContent.ITEM);stack.applyComponents(machine.collectComponents());net.minecraft.world.level.block.Block.popResource(level,pos,stack);
        }
        return super.playerWillDestroy(level,pos,state,p);
    }
}
