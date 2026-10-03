package dev.wildcraft.temperature;

import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Bounded, loaded-chunk-only server sampling. Heat uses the strongest visible source, not a sum. */
public final class TemperatureSampler {
    public record Sample(float target, float base, float weather, boolean water, float heat, int blockReads, int rays) { }
    private record Source(BlockPos position, float heat) { }
    private TemperatureSampler() { }
    public static Sample sample(ServerPlayer player) {
        var level = player.level();
        BlockPos centre = player.blockPosition();
        if (!level.hasChunkAt(centre)) return new Sample(0, 0, 0, false, 0, 0, 0);
        var biome = level.getBiome(centre);
        float base = level.dimension().equals(Level.NETHER) ? 3 : level.dimension().equals(Level.END) ? -.8F
                : biome.unwrapKey().filter(k -> k.identifier().getNamespace().equals("minecraft")).isPresent()
                ? TemperatureRules.biome(biome.value().getBaseTemperature()) : 0;
        Biome.Precipitation precipitation = level.precipitationAt(centre);
        float weather = switch (precipitation) { case RAIN -> -.25F; case SNOW -> -.35F; default -> 0; };
        Vec3 origin = player.position().add(0, .8, 0);
        var candidates = new ArrayList<Source>();
        int reads = 0;
        int radius = TemperatureRules.HEAT_RADIUS;
        for (int dx = -radius; dx <= radius; dx++) for (int dy = -radius; dy <= radius; dy++) for (int dz = -radius; dz <= radius; dz++) {
            BlockPos pos = centre.offset(dx, dy, dz);
            double distance = Vec3.atCenterOf(pos).distanceTo(origin);
            if (distance >= TemperatureRules.HEAT_RADIUS || !level.isInsideBuildHeight(pos.getY()) || !level.hasChunkAt(pos)) continue;
            var state = level.getBlockState(pos); reads++;
            float strength = state.getFluidState().is(FluidTags.LAVA) ? 2.4F
                    : CampfireBlock.isLitCampfire(state) ? 1.4F
                    : state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE) ? 1.8F : 0;
            if (strength > 0) candidates.add(new Source(pos, (float)(strength * (1 - distance / TemperatureRules.HEAT_RADIUS))));
        }
        candidates.sort(Comparator.comparingDouble(Source::heat).reversed());
        float heat = 0; int rays = 0;
        for (Source source : candidates) {
            if (rays >= TemperatureRules.MAX_HEAT_RAYS) break;
            // Clip never traverses an unloaded adjacent chunk, including diagonal chunk corners.
            boolean loaded = true;
            for (int cx = Math.min(centre.getX() >> 4, source.position.getX() >> 4); cx <= Math.max(centre.getX() >> 4, source.position.getX() >> 4); cx++)
                for (int cz = Math.min(centre.getZ() >> 4, source.position.getZ() >> 4); cz <= Math.max(centre.getZ() >> 4, source.position.getZ() >> 4); cz++)
                    loaded &= level.hasChunkAt(new BlockPos(cx * 16, centre.getY(), cz * 16));
            if (!loaded) continue;
            rays++;
            var hit = level.clip(new ClipContext(origin, Vec3.atCenterOf(source.position), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(source.position)) { heat = source.heat; break; }
        }
        boolean water = player.isInWater();
        return new Sample(TemperatureRules.target(base, weather, water, heat), base, weather, water, heat, reads, rays);
    }
}
