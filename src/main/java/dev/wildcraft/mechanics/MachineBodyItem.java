package dev.wildcraft.mechanics;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public final class MachineBodyItem extends Item {
    public MachineBodyItem(Properties properties) { super(properties); }
    @Override public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer(); var level = context.getLevel();
        if (player == null || !level.hasChunkAt(context.getClickedPos())) return InteractionResult.FAIL;
        var body = MechanicsContent.MACHINE.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (body == null) return InteractionResult.FAIL;
        var at = context.getClickLocation().add(context.getClickedFace().getUnitVec3().scale(.01));
        body.setPos(at.x, at.y, at.z); body.setYRot(Math.round(player.getYRot() / 90F) * 90F);
        body.setOwner(player.getUUID());
        if (!level.noCollision(body, body.getBoundingBox()) || !level.getWorldBorder().isWithinBounds(body.getBoundingBox())) return InteractionResult.FAIL;
        if (!level.isClientSide()) {
            if (!level.addFreshEntity(body)) return InteractionResult.FAIL;
            context.getItemInHand().consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }
}
