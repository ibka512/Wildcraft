package dev.wildcraft.cooking;

import dev.wildcraft.Wildcraft;
import dev.wildcraft.focus.ActivePlayClock;
import dev.wildcraft.network.MealView;
import java.util.IdentityHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class CookingEffects {
    public static final AttachmentType<MealEffects> DATA = AttachmentRegistry.create(Wildcraft.id("meal_effects"),
            b -> b.persistent(MealEffects.CODEC));
    public static final AttachmentType<MealView> VIEW = AttachmentRegistry.create(Wildcraft.id("meal_view"),
            b -> b.syncWith(MealView.CODEC,AttachmentSyncPredicate.targetOnly()));
    private static final Map<ServerPlayer,ActivePlayClock> CLOCKS = new IdentityHashMap<>();
    private CookingEffects() { }
    public static MealEffects get(ServerPlayer p) { return p.getAttachedOrElse(DATA,MealEffects.EMPTY); }
    public static void initialize() {
        ServerPlayerEvents.JOIN.register(p -> {CLOCKS.remove(p);publish(p);});
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server) -> CLOCKS.remove(handler.player));
        ServerPlayerEvents.AFTER_RESPAWN.register(Wildcraft.AFTER_ATTACHMENT_TRANSFER,(old,p,alive) -> {
            var previous=get(old);CLOCKS.remove(old);CLOCKS.remove(p);
            p.setAttached(DATA,alive?previous:MealEffects.EMPTY);publish(p);
        });
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((p,from,to) -> {CLOCKS.remove(p);publish(p);});
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> CLOCKS.clear());
        ServerTickEvents.END_SERVER_TICK.register(server -> advance(server,System.nanoTime(),false));
    }
    /** Called even while the integrated world is paused, preventing a resumed gap from being charged. */
    public static void paused(MinecraftServer server) { advance(server,System.nanoTime(),true); }
    private static void advance(MinecraftServer server,long now,boolean paused) {
        for(var p:server.getPlayerList().getPlayers()) {
            if(!p.isAlive()) {p.removeAttached(DATA);p.removeAttached(VIEW);CLOCKS.remove(p);continue;}
            int ms=(int)Math.round(CLOCKS.computeIfAbsent(p,k -> new ActivePlayClock()).advance(now,paused)*1000);
            var before=get(p);var after=before.elapse(ms);
            if(!after.equals(before)) p.setAttached(DATA,after);
            publish(p);
        }
    }
    public static void eat(ServerPlayer p,MealData meal) {
        int ms=(int)Math.round(CLOCKS.computeIfAbsent(p,k -> new ActivePlayClock()).advance(System.nanoTime(),false)*1000);
        p.setAttached(DATA,get(p).elapse(ms).eat(meal));
        publish(p);
    }
    public static double recoveryMultiplier(ServerPlayer p) { return 1 + get(p).recovery().strength()*.5; }
    public static boolean freezeStep(ServerPlayer p) {
        int strength=get(p).warmth().strength();
        return strength==0 || Math.floorMod(p.tickCount,strength+1)==0;
    }
    public static void publish(ServerPlayer p) {
        var view=MealView.of(get(p));
        if(!view.equals(p.getAttached(VIEW)))p.setAttached(VIEW,view);
    }
}
