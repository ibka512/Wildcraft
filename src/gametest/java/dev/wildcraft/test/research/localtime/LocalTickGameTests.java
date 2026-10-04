package dev.wildcraft.test.research.localtime;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public final class LocalTickGameTests {
    @GameTest(maxTicks=100) public void nativeMobCadenceAndReleasePreserveWorldRate(GameTestHelper h){
        var slow=h.spawn(net.minecraft.world.entity.EntityTypes.HUSK,new net.minecraft.core.BlockPos(1,1,1));var normal=h.spawn(net.minecraft.world.entity.EntityTypes.HUSK,new net.minecraft.core.BlockPos(3,1,1));slow.setNoAi(true);normal.setNoAi(true);slow.setNoGravity(true);normal.setNoGravity(true);LocalTickProbe.arm(slow);
        h.runAfterDelay(2,() -> {
            int a=slow.tickCount,b=normal.tickCount;long time=h.getLevel().getGameTime();
            h.runAfterDelay(40,() -> {h.assertTrue(slow.tickCount-a==10&&normal.tickCount-b==40&&h.getLevel().getGameTime()-time==40&&h.getLevel().getServer().tickRateManager().tickrate()==20,"Native target ticks 10 versus 40 while world remains 20 TPS");LocalTickProbe.release(slow);int released=slow.tickCount;
                h.runAfterDelay(20,() -> {h.assertTrue(slow.tickCount-released==20,"Release resumes native cadence without catch-up or global changes");slow.discard();normal.discard();h.succeed();});
            });
        });
    }
}
