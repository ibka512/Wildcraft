package dev.wildcraft.test.industry;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import dev.wildcraft.traversal.Gliding;
/** Read-only temporal correlation; never concludes touchdown from a transient flag. */
public final class NativeGroundAudit {
 private static final boolean REQUESTED=Boolean.getBoolean("wildcraft.c7.groundAudit");
 private static volatile ServerPlayer observedPlayer;
 public static void begin(ServerPlayer p){if(REQUESTED)observedPlayer=p;}
 private static final List<Map<String,Object>> ROWS=Collections.synchronizedList(new ArrayList<>());
 private static Map<String,Object> state(Player p,String stage){var d=new LinkedHashMap<String,Object>();d.put("stage",stage);d.put("nano",System.nanoTime());d.put("worldTick",p.level().getGameTime());d.put("playerTick",p.tickCount);d.put("x",p.getX());d.put("y",p.getY());d.put("z",p.getZ());d.put("boxMinY",p.getBoundingBox().minY);d.put("dy",p.getDeltaMovement().y);d.put("ground",p.onGround());d.put("gliding",Gliding.active(p));d.put("health",p.getHealth());d.put("noGravity",p.isNoGravity());d.put("footBlock",BuiltInRegistries.BLOCK.getKey(p.level().getBlockState(BlockPos.containing(p.getX(),p.getY()-.01,p.getZ())).getBlock()).toString());d.put("collisionBelow001",!p.level().noCollision(p,p.getBoundingBox().move(0,-.01,0)));return d;}
 public static void initialize(){if(!REQUESTED)return;ServerTickEvents.START_SERVER_TICK.register(s->{if(!s.getPlayerList().getPlayers().isEmpty())ROWS.add(state(s.getPlayerList().getPlayers().getFirst(),"server-start"));});ServerTickEvents.END_SERVER_TICK.register(s->{if(!s.getPlayerList().getPlayers().isEmpty())ROWS.add(state(s.getPlayerList().getPlayers().getFirst(),"server-end"));});}
 public static void client(LocalPlayer p){if(REQUESTED)ROWS.add(state(p,"client-control"));}
 public static void packet(ServerPlayer p,ServerboundMovePlayerPacket packet,String stage){if(!REQUESTED||!p.level().getServer().isSameThread())return;var d=state(p,stage);d.put("packetHasPosition",packet.hasPosition());d.put("packetX",packet.getX(p.getX()));d.put("packetY",packet.getY(p.getY()));d.put("packetZ",packet.getZ(p.getZ()));d.put("packetGround",packet.isOnGround());ROWS.add(d);}
 public static void detail(net.minecraft.world.entity.Entity entity,String stage,net.minecraft.world.phys.Vec3 movement,Boolean requestedGround,Double requestedY){
  if(!REQUESTED||entity!=observedPlayer||!(entity instanceof ServerPlayer p)||!p.level().getServer().isSameThread())return;
  var d=state(p,stage);if(movement!=null){d.put("moveX",movement.x);d.put("moveY",movement.y);d.put("moveZ",movement.z);}if(requestedGround!=null)d.put("requestedGround",requestedGround);if(requestedY!=null)d.put("requestedY",requestedY);
  if(Boolean.TRUE.equals(requestedGround)&&!p.onGround()||requestedY!=null&&Math.abs(requestedY-p.getY())>.5){d.put("trace",Arrays.stream(Thread.currentThread().getStackTrace()).filter(v->v.getClassName().startsWith("net.minecraft.")||v.getClassName().startsWith("dev.wildcraft.")).limit(24).map(StackTraceElement::toString).toList());}
  ROWS.add(d);
 }
 public static Map<String,Object> summary(){synchronized(ROWS){return Map.of("requested",REQUESTED,"readOnly",true,"rows",new ArrayList<>(ROWS));}}
}
