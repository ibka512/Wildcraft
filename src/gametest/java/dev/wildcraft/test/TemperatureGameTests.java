package dev.wildcraft.test;

import dev.wildcraft.temperature.*;
import dev.wildcraft.network.TemperatureView;
import dev.wildcraft.player.PlayerStamina;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

public final class TemperatureGameTests {
    @GameTest public void nativeBiomesWeatherAndShelter(GameTestHelper h) {
        var p=h.makeMockServerPlayerInLevel();var pos=h.absolutePos(new BlockPos(1,3,1));p.teleportTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
        var level=h.getLevel();float oldRain=level.getRainLevel(1);var roof=pos.above(2);var oldRoof=level.getBlockState(roof);
        // The GameTest enclosure is skylight-transparent but still blocks native precipitation.
        // Build an actual open column before testing weather, and restore it afterward.
        var above=new java.util.HashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        int top=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,pos.getX(),pos.getZ());
        for(int y=pos.getY();y<=top;y++){var block=new BlockPos(pos.getX(),y,pos.getZ());above.put(block,level.getBlockState(block));level.setBlockAndUpdate(block,Blocks.AIR.defaultBlockState());}
        try {
            level.setRainLevel(0);h.setBiome(Biomes.PLAINS);float plains=TemperatureSampler.sample(p).base();
            h.setBiome(Biomes.SNOWY_PLAINS);h.assertTrue(TemperatureSampler.sample(p).base()<plains,"Native snowy biome colder");
            h.setBiome(Biomes.JUNGLE);h.assertTrue(TemperatureSampler.sample(p).base()>plains,"Native jungle warmer");
            h.setBiome(Biomes.DESERT);h.assertTrue(TemperatureSampler.sample(p).base()>plains,"Native desert hotter");
            level.setRainLevel(1);h.assertTrue(TemperatureSampler.sample(p).weather()==0,"Dry desert excludes global rain");
            h.setBiome(Biomes.PLAINS);
            h.assertTrue(TemperatureSampler.sample(p).weather()<0,"Exposed local rain cools");
            level.setBlockAndUpdate(roof,Blocks.STONE.defaultBlockState());h.assertTrue(TemperatureSampler.sample(p).weather()==0,"Solid roof excludes rain");
            level.setBlockAndUpdate(roof,Blocks.AIR.defaultBlockState());h.setBiome(Biomes.SNOWY_PLAINS);
            h.assertTrue(TemperatureSampler.sample(p).weather()<0,"Native snow also cools");
        } finally {level.setRainLevel(oldRain);level.setBlockAndUpdate(roof,oldRoof);above.forEach(level::setBlockAndUpdate);p.discard();}
        h.succeed();
    }
    @GameTest public void realHeatLightDistanceWallAndNoStacking(GameTestHelper h) {
        var p=h.makeMockServerPlayerInLevel();var pos=h.absolutePos(new BlockPos(1,2,1));p.teleportTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
        var level=h.getLevel();var heat=pos.east(2);var extra=pos.east(3);var wall=pos.east();
        var a=level.getBlockState(heat);var b=level.getBlockState(extra);var c=level.getBlockState(wall);
        try {
            level.setBlockAndUpdate(wall,Blocks.AIR.defaultBlockState());level.setBlockAndUpdate(extra,Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(heat,Blocks.CAMPFIRE.defaultBlockState());float warm=TemperatureSampler.sample(p).heat();
            h.assertTrue(warm>0,"Lit campfire gives nearby heat");
            level.setBlockAndUpdate(extra,Blocks.CAMPFIRE.defaultBlockState());h.assertTrue(TemperatureSampler.sample(p).heat()==warm,"Further identical source is not added");
            level.setBlockAndUpdate(extra,Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(heat,Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT,false));h.assertTrue(TemperatureSampler.sample(p).heat()==0,"Unlit source gives no heat");
            level.setBlockAndUpdate(heat,Blocks.LAVA.defaultBlockState());h.assertTrue(TemperatureSampler.sample(p).heat()>warm,"Lava has stronger near-field warmth");
            level.setBlockAndUpdate(wall,Blocks.STONE.defaultBlockState());h.assertTrue(TemperatureSampler.sample(p).heat()==0,"Solid wall blocks line of sight");
            level.setBlockAndUpdate(wall,Blocks.AIR.defaultBlockState());p.teleportTo(pos.getX()-5,pos.getY(),pos.getZ()+.5);
            h.assertTrue(TemperatureSampler.sample(p).heat()==0,"Beyond four-block radius has no local heat");
        } finally {level.setBlockAndUpdate(heat,a);level.setBlockAndUpdate(extra,b);level.setBlockAndUpdate(wall,c);p.discard();}
        h.succeed();
    }
    @GameTest public void boundedSamplingWithoutGameplayMutation(GameTestHelper h) {
        var p=h.makeMockServerPlayerInLevel();var pos=h.absolutePos(new BlockPos(1,2,1));p.teleportTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
        PlayerStamina.consume(p,20);var stamina=PlayerStamina.get(p);float hp=p.getHealth();var movement=p.getDeltaMovement();p.setTicksFrozen(100);
        long start=System.nanoTime();int reads=0;
        for(int i=0;i<100;i++) {var sample=TemperatureSampler.sample(p);reads+=sample.blockReads();h.assertTrue(sample.blockReads()<=TemperatureRules.MAX_BLOCK_READS && sample.rays()<=TemperatureRules.MAX_HEAT_RAYS,"Scanning and clipping bounded");}
        System.out.println("WILDCRAFT P4A sample probe: 100 samples "+((System.nanoTime()-start)/1e6)+"ms; block reads="+reads);
        h.assertTrue(p.getHealth()==hp && PlayerStamina.get(p).equals(stamina) && p.getDeltaMovement().equals(movement) && p.getTicksFrozen()==100,"Readout never changes health, stamina, movement or native freezing");
        var dense=new java.util.HashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        try {
            for(int x=-3;x<=3;x++)for(int y=-3;y<=3;y++)for(int z=-3;z<=3;z++) {
                var block=pos.offset(x,y,z);dense.put(block,h.getLevel().getBlockState(block));
                h.getLevel().setBlockAndUpdate(block,(Math.max(Math.max(Math.abs(x),Math.abs(y)),Math.abs(z))<=1?Blocks.STONE:Blocks.LAVA).defaultBlockState());
            }
            var crowded=TemperatureSampler.sample(p);
            h.assertTrue(crowded.rays()==TemperatureRules.MAX_HEAT_RAYS && crowded.heat()==0,"Dense hidden sources stop at sixteen clips instead of scanning every candidate");
        } finally {dense.forEach(h.getLevel()::setBlockAndUpdate);}
        var far=new BlockPos(20_000_000,128,20_000_000);
        h.assertTrue(!h.getLevel().hasChunkAt(far),"Fixture location starts unloaded");
        p.setPos(far.getX(),far.getY(),far.getZ());
        var absent=TemperatureSampler.sample(p);
        h.assertTrue(absent.blockReads()==0 && absent.rays()==0 && !h.getLevel().hasChunkAt(far),"Sampling never loads an absent player chunk");
        p.discard();h.succeed();
    }
    @GameTest public void privateTransientViewAndTwoPlayerIsolation(GameTestHelper h) {
        var a=h.makeMockServerPlayerInLevel();var b=h.makeMockServerPlayerInLevel();
        a.setAttached(EnvironmentTemperature.VIEW,new TemperatureView(-3));b.setAttached(EnvironmentTemperature.VIEW,new TemperatureView(3));
        h.assertTrue(a.getAttached(EnvironmentTemperature.VIEW).target()==-3 && b.getAttached(EnvironmentTemperature.VIEW).target()==3,"Independent owner values");
        var buf=Unpooled.buffer();try {TemperatureView.CODEC.encode(buf,new TemperatureView(99));h.assertTrue(buf.readableBytes()==4,"Only one bounded float on wire");h.assertTrue(TemperatureView.CODEC.decode(buf).target()==3,"Encoded value stays bounded");}finally{buf.release();}
        h.assertTrue(!EnvironmentTemperature.VIEW.isPersistent() && !EnvironmentTemperature.VIEW.copyOnDeath(),"No body state in save or death transfer");
        var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());a.saveWithoutId(output);
        var restored=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),a.getGameProfile(),a.clientInformation());
        restored.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),output.buildResult()));
        h.assertTrue(!restored.hasAttached(EnvironmentTemperature.VIEW),"Reload cannot restore a stale climate reading");
        a.discard();b.discard();h.succeed();
    }
}
