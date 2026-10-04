package dev.wildcraft.test.research.localtime;

import net.fabricmc.fabric.api.attachment.v1.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

/** Test-only first gate, not a multiplayer skill or client simulation solution. */
public final class LocalTickProbe {
    private static final AttachmentType<Counter> GATE=AttachmentRegistry.create(Identifier.fromNamespaceAndPath("wildcraft-test","r2_tick_probe"));
    private static final class Counter { int phase; }
    private LocalTickProbe(){}
    public static void initialize(){}
    public static void arm(Mob target){target.setAttached(GATE,new Counter());}
    public static void release(Entity target){target.removeAttached(GATE);}
    public static boolean skip(Entity target){
        if(!(target instanceof Mob)||target.isPassenger()||target.isVehicle())return false;
        var counter=target.getAttached(GATE);return counter!=null&&++counter.phase%4!=0;
    }
}
