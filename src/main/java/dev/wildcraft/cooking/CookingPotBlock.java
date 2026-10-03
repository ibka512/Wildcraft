package dev.wildcraft.cooking;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class CookingPotBlock extends BaseEntityBlock {
    public CookingPotBlock(Properties properties) {super(properties);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) {return new CookingPotEntity(pos,state);}
    @Override protected VoxelShape getShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,CollisionContext context) {
        return net.minecraft.world.level.block.Block.box(2,0,2,14,10,14);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        if(!level.isClientSide() && level.getBlockEntity(pos) instanceof CookingPotEntity pot)player.openMenu(pot);
        return InteractionResult.SUCCESS;
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        return level.isClientSide()?null:createTickerHelper(type,CookingContent.POT_ENTITY,CookingPotEntity::tick);
    }
}
