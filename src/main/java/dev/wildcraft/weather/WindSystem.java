package dev.wildcraft.weather;

import dev.wildcraft.Wildcraft;
import dev.wildcraft.network.WindView;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.entity.event.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

/** Loaded-column sampling only. Existing seed/time persistence is sufficient for wind. */
public final class WindSystem {
    public static final AttachmentType<WindView> VIEW=AttachmentRegistry.create(Wildcraft.id("wind_view"),b->b.syncWith(WindView.CODEC,AttachmentSyncPredicate.targetOnly()));
    private WindSystem(){}
    public static void initialize(){
        ServerPlayerEvents.JOIN.register(WindSystem::publish);
        ServerPlayerEvents.AFTER_RESPAWN.register(Wildcraft.AFTER_ATTACHMENT_TRANSFER,(old,p,alive)->{p.removeAttached(VIEW);publish(p);});
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((p,from,to)->{p.removeAttached(VIEW);publish(p);});
        ServerTickEvents.END_SERVER_TICK.register(server->{for(var p:server.getPlayerList().getPlayers()){
            if(!p.isAlive()||p.isSpectator()){p.removeAttached(VIEW);continue;}
            if(!p.hasAttached(VIEW)||Math.floorMod(p.tickCount,5)==0)publish(p);
        }});
    }
    public static WindView sample(ServerLevel level,BlockPos pos){
        var dim=level.dimension().identifier();
        if(!level.hasChunkAt(pos)||!level.canHaveWeather()||!level.isInsideBuildHeight(pos.getY())
                ||level.getHeight(Heightmap.Types.MOTION_BLOCKING,pos.getX(),pos.getZ())>pos.getY()
                ||!level.getFluidState(pos).isEmpty())return new WindView(dim,0,0,0,false);
        var flow=WindRules.field(level.getSeed()^dim.toString().hashCode(),level.getGameTime(),level.getRainLevel(1),level.getThunderLevel(1));
        float wet=level.precipitationAt(pos)==Biome.Precipitation.NONE?0:level.getRainLevel(1);
        return new WindView(dim,quantize(flow.x()),quantize(flow.z()),quantize(wet),true);
    }
    private static float quantize(double n){return (float)(Math.round(n*10000)/10000.0);}
    public static void publish(ServerPlayer p){
        if(!p.isAlive()||p.isSpectator()||!p.level().hasChunkAt(p.blockPosition())){p.removeAttached(VIEW);return;}
        var view=p.isInWater()||p.isInLava()?new WindView(p.level().dimension().identifier(),0,0,0,false)
                :sample(p.level(),p.getVehicle() instanceof dev.wildcraft.mechanics.MachineEntity body?body.blockPosition():p.blockPosition());
        if(!view.equals(p.getAttached(VIEW)))p.setAttached(VIEW,view);
    }
    public static WindView local(Player p){
        var view=p.getAttached(VIEW);
        return view!=null&&view.dimension().equals(p.level().dimension().identifier())?view:new WindView(p.level().dimension().identifier(),0,0,0,false);
    }
}
