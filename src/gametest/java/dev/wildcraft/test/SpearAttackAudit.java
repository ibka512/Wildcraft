package dev.wildcraft.test;
import java.util.UUID;
import net.minecraft.world.entity.player.Player;
/** Dedicated piercing invocations and native per-target stabs; test mod only. */
public final class SpearAttackAudit {
    public static UUID player;public static int piercing,stabs,nativePackets;
    private SpearAttackAudit(){}
    public static void begin(Player p){player=p.getUUID();piercing=stabs=nativePackets=0;CounterAttackAudit.begin(p);}
    public static void observe(Player p,boolean primary){if(!p.level().isClientSide()&&p.getUUID().equals(player)){if(primary)piercing++;else stabs++;}}
}
