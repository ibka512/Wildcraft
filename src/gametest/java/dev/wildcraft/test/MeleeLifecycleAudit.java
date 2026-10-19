package dev.wildcraft.test;
import dev.wildcraft.combat.MeleeActions;
import dev.wildcraft.network.*;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
/** Test-only actual received intent lifecycle; safe on the dedicated server without client classes. */
public final class MeleeLifecycleAudit {
 public static volatile UUID player;public static volatile int dodges,cancels,cancelFromHop;private static int before;
 private MeleeLifecycleAudit(){}
 public static void begin(ServerPlayer p){player=p.getUUID();dodges=cancels=cancelFromHop=0;before=MeleeView.IDLE;}
 public static void before(ServerPlayer p,MeleeIntent i){if(p.getUUID().equals(player))before=MeleeActions.view(p).phase();}
 public static void after(ServerPlayer p,MeleeIntent i){if(!p.getUUID().equals(player))return;var v=MeleeActions.view(p);if(i.operation()==MeleeIntent.DODGE&&v.phase()==MeleeView.HOP)dodges++;if(i.operation()==MeleeIntent.CANCEL&&v.phase()==MeleeView.IDLE){cancels++;if(before==MeleeView.HOP)cancelFromHop++;}}
}
