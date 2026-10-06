package dev.wildcraft.test;

import dev.wildcraft.weather.*;
import dev.wildcraft.network.WindView;
import dev.wildcraft.traversal.Gliding;
import dev.wildcraft.player.PlayerStamina;
import dev.wildcraft.mechanics.*;
import dev.wildcraft.energy.*;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.*;

public final class WindGameTests {
    /** Real open column, biome precipitation, solid roof and water; restore global weather. */
    @GameTest public void loadedLocalWeatherShelterAndWater(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(1,3,1));
        var restore=new java.util.HashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        int top=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,pos.getX(),pos.getZ());
        float rain=level.getRainLevel(1),thunder=level.getThunderLevel(1);
        for(int y=pos.getY();y<=top;y++){var b=new BlockPos(pos.getX(),y,pos.getZ());restore.put(b,level.getBlockState(b));level.setBlockAndUpdate(b,Blocks.AIR.defaultBlockState());}
        try{
            h.setBiome(Biomes.PLAINS);level.setRainLevel(0);level.setThunderLevel(0);var clear=WindSystem.sample(level,pos);
            h.assertTrue(clear.outdoors()&&clear.precipitation()==0,"Native exposed clear column has wind without wet penalty");
            level.setRainLevel(1);level.setThunderLevel(1);var storm=WindSystem.sample(level,pos);
            h.assertTrue(storm.outdoors()&&storm.precipitation()==1&&Math.hypot(storm.x(),storm.z())<=.040001,"Native local storm is bounded and wet");
            h.setBiome(Biomes.DESERT);h.assertTrue(WindSystem.sample(level,pos).precipitation()==0,"Global rain in a dry biome has no wet penalty");
            h.setBiome(Biomes.SNOWY_PLAINS);h.assertTrue(WindSystem.sample(level,pos).precipitation()==1,"Native snow is a real precipitation state");
            level.setBlockAndUpdate(pos.above(2),Blocks.STONE.defaultBlockState());h.assertTrue(!WindSystem.sample(level,pos).outdoors(),"Solid roof removes wind and wet penalty");
            level.setBlockAndUpdate(pos.above(2),Blocks.AIR.defaultBlockState());level.setBlockAndUpdate(pos,Blocks.WATER.defaultBlockState());
            h.assertTrue(!WindSystem.sample(level,pos).outdoors(),"Fluid column has no wind");
            var absent=new BlockPos(20_000_000,128,20_000_000);h.assertTrue(!WindSystem.sample(level,absent).outdoors()&&!level.hasChunkAt(absent),"Absent column stays unloaded");
            var nether=level.getServer().getLevel(net.minecraft.world.level.Level.NETHER);
            h.assertTrue(!WindSystem.sample(nether,pos).outdoors(),"No natural wind in ceiling dimension");
        }finally{level.setRainLevel(rain);level.setThunderLevel(rain==0?0:thunder/rain);restore.forEach(level::setBlockAndUpdate);}
        h.succeed();
    }
    @GameTest public void privateTransientCodecAndGameplayReadOnly(GameTestHelper h){
        var p=h.makeMockServerPlayerInLevel();p.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(1,3,1))));
        var peak=PlayerStamina.get(p);var movement=p.getDeltaMovement();float hp=p.getHealth();
        WindSystem.publish(p);h.assertTrue(peak.equals(PlayerStamina.get(p))&&p.getDeltaMovement().equals(movement)&&p.getHealth()==hp,"Readout leaves real stamina, health and walking untouched");
        h.assertTrue(!WindSystem.VIEW.isPersistent()&&!WindSystem.VIEW.copyOnDeath(),"Wind view is not saved or copied on death");
        var view=new WindView(p.level().dimension().identifier(),100,100,7,true);var buf=Unpooled.buffer();
        try{WindView.CODEC.encode(buf,view);h.assertTrue(view.equals(WindView.CODEC.decode(buf))&&Math.hypot(view.x(),view.z())<=.040001&&view.precipitation()==1,"Wire round-trip clamps finite wind and precipitation");}finally{buf.release();}
        boolean refused=false;try{new WindView(view.dimension(),Float.NaN,0,0,true);}catch(IllegalArgumentException expected){refused=true;}h.assertTrue(refused,"Malformed view rejected");
        var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());p.saveWithoutId(out);
        var restored=new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),p.getGameProfile(),p.clientInformation());
        restored.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),out.buildResult()));
        h.assertTrue(!restored.hasAttached(WindSystem.VIEW),"Real player save cannot restore stale wind");
        p.setAttached(WindSystem.VIEW,new WindView(net.minecraft.world.level.Level.NETHER.identifier(),.04F,0,1,true));
        h.assertTrue(!WindSystem.local(p).outdoors(),"Wrong dimension view cannot drive prediction");p.discard();h.succeed();
    }
    @GameTest public void gliderDirectionsAlwaysDescendWithinExistingGuard(GameTestHelper h){
        var dim=h.getLevel().dimension().identifier();
        for(float yaw:new float[]{0,45,90,180,270})for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)for(double angle:new double[]{0,1,2,3,4,5}){
            var wind=new WindView(dim,(float)(.04*Math.cos(angle)),(float)(.04*Math.sin(angle)),1,true);
            var input=new Vec3(x,0,z);var base=Gliding.movement(yaw,input);var actual=Gliding.movement(yaw,input,wind);
            h.assertTrue(actual.horizontalDistanceSqr()<.34*.34&&actual.y<0&&actual.y>=-.095001,"Every steering/strong-wind combination fits unchanged movement guard and descends");
            h.assertTrue(Math.abs(actual.x-base.x-wind.x())<1e-9&&Math.abs(actual.z-base.z-wind.z())<1e-9,"World wind yields expected side/tail/head drift");
        }h.succeed();
    }
    @GameTest public void passiveWingWindPreservesBatteryAndSave(GameTestHelper h){
        var p=h.makeMockServerPlayerInLevel();var body=h.spawn(MechanicsContent.MACHINE,new Vec3(2,4,2),EntitySpawnReason.COMMAND);
        var high=h.absolutePos(new BlockPos(2,4,2)).above(80);body.setPos(Vec3.atCenterOf(high));p.setPos(body.position());body.setOwner(p.getUUID());
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(MechanicsContent.WING));h.assertTrue(body.install(p,4,InteractionHand.MAIN_HAND),"Install actual wing");
        var battery=new ItemStack(EnergyContent.BATTERY);Batteries.charge(battery,317);p.setItemInHand(InteractionHand.MAIN_HAND,battery);h.assertTrue(body.install(p,0,InteractionHand.MAIN_HAND),"Install actual finite battery");
        body.setOnGround(false);body.setDeltaMovement(.25,-.4,0);var wind=WindSystem.sample(h.getLevel(),body.blockPosition());h.assertTrue(wind.outdoors(),"Wing fixture is outdoors");
        var before=body.position();double gain=WindRules.wingGain(1,body.mass());body.tick();
        h.assertTrue(body.energy()==317&&!body.working()&&body.active(4),"Passive wing consumes no energy and retains active lift");
        double drag=1-.01*wind.precipitation();var expected=new Vec3((.25+wind.x()*gain)*drag,Math.max(-.44,-.12*body.mass()-WindRules.descentPenalty(wind.precipitation())),wind.z()*gain*drag);
        h.assertTrue(body.position().subtract(before).distanceTo(expected)<1e-6,"Native body displacement includes bounded passive wind and wet descent");
        var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,h.getLevel().registryAccess());body.saveWithoutId(out);
        var restored=MechanicsContent.MACHINE.create(h.getLevel(),EntitySpawnReason.LOAD);restored.load(TagValueInput.create(ProblemReporter.DISCARDING,h.getLevel().registryAccess(),out.buildResult()));
        h.assertTrue(restored.kind(4)==3&&restored.energy()==317&&restored.owner().equals(p.getUUID()),"Format1 preserves wing, owner and finite charge without a wind migration");
        body.discard();p.discard();h.succeed();
    }
    @GameTest public void poweredCombinedWingKeepsFiniteCostsAndSpeedCap(GameTestHelper h){
        var p=h.makeMockServerPlayerInLevel();var body=h.spawn(MechanicsContent.MACHINE,new Vec3(2,4,2),EntitySpawnReason.COMMAND);
        body.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(2,4,2)).above(80)));p.setPos(body.position());body.setOwner(p.getUUID());
        var battery=new ItemStack(EnergyContent.BATTERY);Batteries.charge(battery,317);
        ItemStack[] parts={battery,new ItemStack(MechanicsContent.ROCKET),new ItemStack(MechanicsContent.SPRING),new ItemStack(MechanicsContent.FAN),new ItemStack(MechanicsContent.WING),new ItemStack(MechanicsContent.STABILIZER)};
        for(int n=0;n<6;n++){p.setItemInHand(InteractionHand.MAIN_HAND,parts[n]);h.assertTrue(body.install(p,n,InteractionHand.MAIN_HAND),"Real combined installation at "+n);}
        body.setEnabled(p,true);body.setOnGround(false);body.setDeltaMovement(2,-.4,2);body.tick();
        h.assertTrue(body.energy()==314&&body.rocketFuel(1)==RocketData.DURATION-1&&body.springCooldown(2)==0,"Wind creates no energy, fuel or extra spring cost; fan/stabilizer spend their existing 3 units");
        h.assertTrue(body.getDeltaMovement().horizontalDistanceSqr()<=.4*.4&&body.getDeltaMovement().y>=-.8,"Combined actual body movement stays within the existing cap");
        body.discard();p.discard();h.succeed();
    }
}
