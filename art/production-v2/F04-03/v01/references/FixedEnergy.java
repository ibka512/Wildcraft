package dev.wildcraft.energy;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/** Only loaded, fixed world redstone blocks. Signals and carried items provide no energy. */
public final class FixedEnergy {
    public static final int OUTPUT_PER_TICK=20;
    private static final class Budget {long tick=Long.MIN_VALUE;final Set<BlockPos> claimed=new HashSet<>();final Set<BlockPos> supplied=new HashSet<>();}
    private static final Map<Level,Budget> BUDGETS=new WeakHashMap<>();
    private FixedEnergy(){ }
    public static void clear(){BUDGETS.clear();}
    public static boolean hasSource(Level level,BlockPos charger){
        for(var direction:Direction.values()){var source=charger.relative(direction);if(level.hasChunkAt(source) && level.getBlockState(source).is(Blocks.REDSTONE_BLOCK))return true;}
        return false;
    }
    public static int allocate(Level level,BlockPos charger,int wanted){
        if(level.isClientSide() || wanted<=0 || !level.hasChunkAt(charger) || !(level.getBlockEntity(charger) instanceof ChargerEntity own) || !own.canCharge())return 0;
        var budget=BUDGETS.computeIfAbsent(level,k -> new Budget());long time=level.getGameTime();
        if(budget.tick!=time){budget.tick=time;budget.claimed.clear();budget.supplied.clear();}
        if(budget.supplied.contains(charger))return 0;
        for(var direction:Direction.values()){
            var source=charger.relative(direction);
            if(budget.claimed.contains(source) || !level.hasChunkAt(source) || !level.getBlockState(source).is(Blocks.REDSTONE_BLOCK))continue;
            var eligible=new ArrayList<BlockPos>(6);
            for(var side:Direction.values()){
                var next=source.relative(side);
                if(level.hasChunkAt(next) && level.getBlockEntity(next) instanceof ChargerEntity c && c.canCharge())eligible.add(next);
            }
            // Same direction order on every caller; persistent candidates receive a turn each cycle.
            if(eligible.isEmpty() || !eligible.get(Math.floorMod(time,eligible.size())).equals(charger))continue;
            budget.claimed.add(source.immutable());budget.supplied.add(charger.immutable());
            return Math.min(wanted,OUTPUT_PER_TICK);
        }
        return 0;
    }
}
