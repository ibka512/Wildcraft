package dev.wildcraft.test;
import java.util.UUID;
import net.minecraft.world.entity.player.Player;
/** Test-mod-only native call count, including attempts rejected by damage immunity. */
public final class CounterAttackAudit {
    public static UUID player;public static int calls;
    private CounterAttackAudit(){}
    public static void begin(Player p){player=p.getUUID();calls=0;}
    public static void observed(Player p){if(!p.level().isClientSide()&&p.getUUID().equals(player))calls++;}
}
