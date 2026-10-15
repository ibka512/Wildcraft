package dev.wildcraft.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.wildcraft.combat.MeleeActions;
import dev.wildcraft.equipment.BackEquipment;
import dev.wildcraft.equipment.BackVisual;
import dev.wildcraft.network.MeleeView;
import dev.wildcraft.traversal.Climbing;
import dev.wildcraft.traversal.Gliding;
import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Presentation of a same-reference hand/back switch. No delayed ownership or outgoing item copies. */
public final class BackTransitions {
    public record Settle(int category, int owner, int handOwner, float progress) {
        public boolean draw() { return owner != 3; }
        public float weight() { return draw() ? 1 - smooth(progress) : (float)Math.sin(Math.PI * progress); }
    }
    public static final RenderStateDataKey<Settle[]> SETTLES = RenderStateDataKey.create(() -> "wildcraft:back_settles");
    public static final RenderStateDataKey<Boolean> WAIST = RenderStateDataKey.create(() -> "wildcraft:equipment_waist");
    private static final Map<Player, Motion> MOTION = new WeakHashMap<>();
    private static final class Motion {
        BackVisual previous;
        final float[] elapsed = {1, 1, 1};
        final int[] hand = new int[3];
        Object dimension;
        long time;
        final long[] canceledReference = new long[3];
        final int[] canceledOwner = new int[3];
        ItemStack interruptedMain = ItemStack.EMPTY, interruptedOff = ItemStack.EMPTY;
        int interruptedSlot;
    }
    private BackTransitions() { }
    public static void initialize() {
        ClientPlayConnectionEvents.DISCONNECT.register((h,c) -> MOTION.clear());
        // A menu may open and close between avatar renders. Consume the interruption at the real event.
        // Pause retains the live progress and its existing frozen clock.
        ScreenEvents.AFTER_INIT.register((client,screen,width,height) -> {
            if(client.player != null && !screen.isPauseScreen()) {
                interrupt(client.player);
                ScreenEvents.remove(screen).register(closed -> {
                    if(client.player != null) interrupt(client.player);
                });
            }
        });
    }
    /** Accepted native uses can start and finish between avatar renders (for example loaded crossbow fire). */
    public static void nativeUse(Player p) { if(p==Minecraft.getInstance().player)interrupt(p); }
    private static void interrupt(Player p) {
        var current = p.getAttached(BackEquipment.VISUAL);
        if(current == null) { MOTION.remove(p); return; }
        var m = MOTION.computeIfAbsent(p, ignored -> new Motion());
        m.previous = current; m.dimension = p.level().dimension(); m.time = System.nanoTime();
        m.interruptedMain = p.getMainHandItem().copy(); m.interruptedOff = p.getOffhandItem().copy();
        m.interruptedSlot = p.getInventory().getSelectedSlot();
        for(int c=0;c<3;c++) {
            m.elapsed[c] = 1; var entry = current.item(c);
            int owner = BackEquipment.category(p.getMainHandItem()) == c ? 1 : BackEquipment.category(p.getOffhandItem()) == c ? 2 : 3;
            m.canceledReference[c] = entry.reference()!=0 && entry.owner()!=owner ? entry.reference() : 0;
            m.canceledOwner[c] = owner;
        }
    }
    public static float smooth(float t) { return t*t*(3-2*t); }
    public static float duration(int category, boolean draw) { return category == 0 ? (draw ? .34F : .38F) : .32F; }
    public static boolean switched(BackVisual.Entry before, BackVisual.Entry after) {
        return before.reference() != 0 && before.reference() == after.reference()
                && (before.owner() == 3 && (after.owner() == 1 || after.owner() == 2)
                || after.owner() == 3 && (before.owner() == 1 || before.owner() == 2));
    }
    public static void extract(Player p, AvatarRenderState state) {
        var client = Minecraft.getInstance();
        var current = p.getAttached(BackEquipment.VISUAL);
        state.setData(SETTLES, null);
        state.setData(WAIST, state.showCape && state.skin.cape() != null || p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).is(Items.ELYTRA));
        if (current == null) { MOTION.remove(p); return; }
        Motion m = MOTION.computeIfAbsent(p, ignored -> new Motion());
        // Consume actual elapsed time; a per-frame .05 cap stretches motions below 20 updates/s.
        long now = System.nanoTime();
        float dt = m.time == 0 || client.isPaused() ? 0 : Math.max(0,(now-m.time)/1_000_000_000F);
        boolean reset = m.previous == null || m.dimension != p.level().dimension();
        // Evaluate live native input too: synchronized combat cancellation can arrive a frame later.
        boolean blocked = !p.isAlive() || p.isSpectator() || p.isInvisible() || p.isUsingItem()
                || state.currentSwing != null || state.isAutoSpinAttack || p.isSleeping() || p.isPassenger() || p.isVisuallySwimming() || p.isFallFlying()
                || Gliding.active(p) || Climbing.active(p) || MeleeActions.view(p).phase() != MeleeView.IDLE
                || p == client.player && (client.gui.screen() != null && !client.gui.screen().isPauseScreen() || client.options.keyAttack.isDown() || client.options.keyUse.isDown());
        // An interrupted native hand selection can precede its authoritative visual update.
        // A genuinely new hand/slot selection ends that cancellation; saved inventory data is untouched.
        boolean sameInterruptedHands = !reset && m.interruptedSlot == p.getInventory().getSelectedSlot()
                && ItemStack.matches(m.interruptedMain,p.getMainHandItem()) && ItemStack.matches(m.interruptedOff,p.getOffhandItem());
        Settle[] result = new Settle[3];
        for (int c = 0; c < 3; c++) {
            var entry = current.item(c);
            boolean canceled = sameInterruptedHands && m.canceledReference[c] != 0 && m.canceledReference[c] == entry.reference();
            if(!sameInterruptedHands || m.canceledReference[c] != entry.reference()) m.canceledReference[c] = 0;
            if(canceled && entry.owner() == m.canceledOwner[c]) m.canceledReference[c] = 0;
            if (reset || blocked || canceled) m.elapsed[c] = 1;
            else if (!entry.equals(m.previous.item(c))) {
                var before = m.previous.item(c);
                m.elapsed[c] = switched(before,entry) ? 0 : 1;
                m.hand[c] = entry.owner() == 3 ? before.owner() : entry.owner();
            } else m.elapsed[c] += dt;
            float t = Math.clamp(m.elapsed[c]/duration(c,entry.owner()!=3),0,1);
            if (t < 1 && entry.owner() != 0) result[c] = new Settle(c,entry.owner(),m.hand[c],t);
        }
        m.previous = current; m.dimension = p.level().dimension(); m.time = now;
        state.setData(SETTLES,result);
    }
    public static ItemStack held(AvatarRenderState state, HumanoidArm arm) {
        return arm == HumanoidArm.RIGHT ? state.rightHandItemStack : state.leftHandItemStack;
    }
    public static Settle forArm(AvatarRenderState state, HumanoidArm arm) {
        var settles = state.getData(SETTLES);
        if (settles == null || state.isUsingItem || state.currentSwing != null || state.isSpectator || state.isInvisible || state.isAutoSpinAttack) return null;
        int owner = arm == state.mainArm ? 1 : 2;
        Settle outgoing = null;
        for (var s : settles) if (s != null && s.handOwner == owner) {
            if (s.draw()) { if (BackEquipment.category(held(state,arm)) == s.category) return s; continue; }
            // A replacement torch/weapon owns its native arm; never carry it towards the back.
            if (held(state,arm).isEmpty()) outgoing = s;
        }
        return outgoing;
    }
    public static void apply(HumanoidModel<?> model, AvatarRenderState state) {
        for (var arm : HumanoidArm.values()) {
            var s = forArm(state,arm); if (s == null) continue;
            var part = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
            float sign = arm == HumanoidArm.RIGHT ? 1 : -1;
            boolean waist = state.getDataOrDefault(WAIST,false);
            float weight = s.weight();
            // Positive X reaches behind the native rig. Waist reaches stay low; head and legs stay native.
            float x = waist ? .35F : s.category == 2 ? 2.1F : 1.2F;
            float y = sign*(waist ? -.4F : s.category == 1 ? .45F : s.category == 2 ? .8F : -.25F), z = sign*(waist ? .35F : -.15F);
            part.xRot += (x-part.xRot)*weight; part.yRot += (y-part.yRot)*weight; part.zRot += (z-part.zRot)*weight;
            model.body.yRot += sign*.04F*weight;
            var kind = dev.wildcraft.combat.MeleeWeapon.of(held(state,arm));
            if (s.draw() && arm == state.mainArm && dev.wildcraft.client.animation.MeleeHands.freeSupport(state)
                    && forArm(state,arm.getOpposite()) == null
                    && (kind == dev.wildcraft.combat.MeleeWeapon.TRIDENT || kind == dev.wildcraft.combat.MeleeWeapon.SPEAR || kind == dev.wildcraft.combat.MeleeWeapon.MACE)) {
                var support = arm == HumanoidArm.RIGHT ? model.leftArm : model.rightArm;
                support.xRot += (-.8F-support.xRot)*weight;
            }
        }
    }
    public static void back(PoseStack poses, AvatarRenderState state, int category, boolean waist) {
        var settles = state.getData(SETTLES); var s = settles == null ? null : settles[category];
        if (s == null || s.draw()) return;
        float w = 1-smooth(s.progress);
        poses.translate((waist?.15F:category==1?-.15F:.2F)*w, -.06F*w, -.04F*w);
        poses.rotateDegrees(Axis.ZP, (category==1?-6:6)*w);
    }
    public static void hand(PoseStack poses, AvatarRenderState state, HumanoidArm arm) {
        var s = forArm(state,arm); if (s == null || !s.draw()) return;
        poses.rotateDegrees(Axis.ZP, (arm == HumanoidArm.RIGHT ? 1 : -1)*8*s.weight());
    }
}
