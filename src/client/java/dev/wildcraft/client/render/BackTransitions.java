package dev.wildcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.wildcraft.equipment.BackEquipment;
import dev.wildcraft.equipment.BackVisual;
import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;

/** Local settling after an authoritative ownership switch; never creates an outgoing item. */
public final class BackTransitions {
    public record Settle(int category, int owner, float weight) { }
    public static final RenderStateDataKey<Settle[]> SETTLES = RenderStateDataKey.create(() -> "wildcraft:back_settles");
    private static final Map<Player, Motion> MOTION = new WeakHashMap<>();
    private static final float[][] BACK = {{-.6F,1.4F,.8F,-6,0,0},{-.9F,.6F,1,2,8,0},{0,1.6F,1,5,0,0}};
    private static final float[][] HAND = {{.25F,.45F,.65F,4,0,3},{-.35F,.3F,.5F,0,-5,4},{.3F,.5F,.7F,-4,0,4}};
    private static final class Motion {
        BackVisual previous;
        final long[] start = new long[3];
        Object dimension;
    }
    private BackTransitions() { }
    public static void initialize() { ClientPlayConnectionEvents.DISCONNECT.register((h,c) -> MOTION.clear()); }
    public static void extract(Player p, AvatarRenderState state) {
        var client = Minecraft.getInstance();
        BackVisual current = p.getAttached(BackEquipment.VISUAL);
        state.setData(SETTLES, null);
        if (current == null) { MOTION.remove(p); return; }
        Motion m = MOTION.computeIfAbsent(p, ignored -> new Motion());
        boolean reset = m.previous == null || m.dimension != p.level().dimension() || client.isPaused() || !p.isAlive();
        long now = System.nanoTime();
        Settle[] result = new Settle[3];
        for (int c = 0; c < 3; c++) {
            var entry = current.item(c);
            if (reset) m.start[c] = 0;
            else if (!entry.equals(m.previous.item(c))) {
                int oldOwner = m.previous.item(c).owner();
                // Only the same recorded reference can move between the hand and the back.
                boolean switchOwner = entry.reference() == m.previous.item(c).reference()
                        && oldOwner != 0 && entry.owner() != 0 && oldOwner != entry.owner();
                m.start[c] = switchOwner ? now : 0;
            }
            if (entry.owner() == 0 || entry.owner() != 3 && (state.currentSwing != null || p.isUsingItem())) m.start[c] = 0;
            float duration = entry.owner() == 3 ? (c == 0 ? .18F : .20F) : (c == 0 ? .14F : .16F);
            float t = m.start[c] == 0 ? 1 : Math.clamp((now - m.start[c]) / 1_000_000_000F / duration, 0, 1);
            result[c] = new Settle(c,entry.owner(),(1-t)*(1-t)*(1-t));
        }
        m.previous = current; m.dimension = p.level().dimension();
        state.setData(SETTLES,result);
    }
    public static void back(PoseStack poses, AvatarRenderState state, int category) {
        Settle[] settles = state.getData(SETTLES);
        if (settles != null && settles[category].owner == 3) transform(poses,BACK[category],settles[category].weight);
    }
    public static void hand(PoseStack poses, AvatarRenderState state, HumanoidArm arm) {
        Settle[] settles = state.getData(SETTLES);
        if (settles == null || state.currentSwing != null || state.isUsingItem) return;
        int owner = arm == state.mainArm ? 1 : 2;
        for (Settle s : settles) if (s.owner == owner) transform(poses,HAND[s.category],s.weight);
    }
    private static void transform(PoseStack poses, float[] values, float weight) {
        if (weight == 0) return;
        poses.translate(values[0]*weight/16, -values[1]*weight/16, values[2]*weight/16);
        poses.rotateDegrees(Axis.XP, -values[3]*weight);
        poses.rotateDegrees(Axis.YP, values[4]*weight);
        poses.rotateDegrees(Axis.ZP, -values[5]*weight);
    }
}
