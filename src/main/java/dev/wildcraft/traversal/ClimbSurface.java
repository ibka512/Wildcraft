package dev.wildcraft.traversal;

import dev.wildcraft.Wildcraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;

/** Probe collision shapes, including partial blocks and clear space over ledges. */
public final class ClimbSurface {
    public static final TagKey<Block> UNCLIMBABLE = TagKey.create(Registries.BLOCK, Wildcraft.id("unclimbable"));
    private static final Direction[] FACES = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
    private static final double REACH = 0.08;

    private ClimbSurface() {
    }

    public record Contact(Direction face, boolean mantle, Direction cornerFrom) {
        public Contact(Direction face, boolean mantle) {
            this(face, mantle, null);
        }
    }

    public static Contact find(Player player, Direction previous, boolean continuing, boolean movingUp) {
        return find(player, previous, continuing, movingUp, 0);
    }

    public static Contact find(Player player, Direction previous, boolean continuing, boolean movingUp, int sideways) {
        if (continuing && previous != null) {
            if (sideways != 0) {
                int tangentX = previous.getStepZ() * sideways;
                int tangentZ = -previous.getStepX() * sideways;
                for (Direction face : FACES) {
                    if (face.getStepX() == tangentX && face.getStepZ() == tangentZ && hasWall(player, face)) {
                        return new Contact(face, false);
                    }
                }
            }
            if (movingUp && canMantle(player, previous)) {
                return new Contact(previous, true);
            }
            if (hasWall(player, previous)) {
                return new Contact(previous, false);
            }
        }
        double yaw = Math.toRadians(player.getYRot());
        double lookX = -Math.sin(yaw);
        double lookZ = Math.cos(yaw);
        Direction selected = null;
        double best = -2;
        for (Direction face : FACES) {
            double facing = lookX * face.getStepX() + lookZ * face.getStepZ();
            if ((continuing || facing >= 0.25) && facing > best && hasWall(player, face)) {
                selected = face;
                best = facing;
            }
        }
        if (selected != null) {
            return new Contact(selected, false);
        }
        if (continuing && previous != null && sideways != 0) {
            AABB aroundCorner = player.getBoundingBox().move(previous.getStepX() * 0.06, 0, previous.getStepZ() * 0.06);
            for (Direction face : FACES) {
                if (face.getAxis() != previous.getAxis() && hasWall(player, aroundCorner, face)) {
                    return new Contact(face, false, previous);
                }
            }
        }
        return null;
    }

    public static boolean hasWall(Player player, Direction face) {
        return hasWall(player, player.getBoundingBox(), face);
    }

    private static boolean hasWall(Player player, AABB box, Direction face) {
        double y0 = box.minY + 0.1;
        double y1 = box.maxY - 0.1;
        AABB probe = switch (face) {
            case EAST -> new AABB(box.maxX - 0.001, y0, box.minZ + 0.02, box.maxX + REACH, y1, box.maxZ - 0.02);
            case WEST -> new AABB(box.minX - REACH, y0, box.minZ + 0.02, box.minX + 0.001, y1, box.maxZ - 0.02);
            case SOUTH -> new AABB(box.minX + 0.02, y0, box.maxZ - 0.001, box.maxX - 0.02, y1, box.maxZ + REACH);
            case NORTH -> new AABB(box.minX + 0.02, y0, box.minZ - REACH, box.maxX - 0.02, y1, box.minZ + 0.001);
            default -> throw new IllegalArgumentException("Horizontal wall required");
        };
        return solidCollision(player, probe);
    }

    private static boolean canMantle(Player player, Direction face) {
        AABB ahead = player.getBoundingBox().move(face.getStepX() * 0.18, 0.12, face.getStepZ() * 0.18);
        if (!player.level().noCollision(player, ahead)) {
            return false;
        }
        AABB support = new AABB(ahead.minX, player.getY() - 0.22, ahead.minZ,
                ahead.maxX, player.getY() + 0.06, ahead.maxZ);
        return solidCollision(player, support);
    }

    private static boolean solidCollision(Player player, AABB probe) {
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(probe.minX, probe.minY, probe.minZ),
                BlockPos.containing(probe.maxX, probe.maxY, probe.maxZ))) {
            var state = player.level().getBlockState(pos);
            if (state.is(UNCLIMBABLE)) {
                continue;
            }
            for (AABB shape : state.getCollisionShape(player.level(), pos, CollisionContext.of(player)).toAabbs()) {
                if (shape.move(pos.getX(), pos.getY(), pos.getZ()).intersects(probe)) {
                    return true;
                }
            }
        }
        return false;
    }
}
