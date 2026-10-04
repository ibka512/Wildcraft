package dev.wildcraft.fabrication;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;

/** Moving a paid machine transfers the actual item even through the creative wrapper. */
public final class FabricatorItem extends BlockItem {
    public FabricatorItem(Properties p){super(FabricationContent.BLOCK,p);}
    @Override public InteractionResult place(BlockPlaceContext context){
        var result=super.place(context);var p=context.getPlayer();
        if(result.consumesAction()&&p!=null&&p.getAbilities().instabuild){
            var source=context.getItemInHand();source.shrink(1);p.setItemInHand(context.getHand(),source.copy());
        }
        return result;
    }
}
