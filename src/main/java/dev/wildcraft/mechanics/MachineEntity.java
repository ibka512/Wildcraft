package dev.wildcraft.mechanics;

import dev.wildcraft.energy.Batteries;
import dev.wildcraft.energy.EnergyContent;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.NonNullList;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;

/** Server owns movement and full stacks; tracking exposes only kind, charge and switch. */
public final class MachineEntity extends Entity {
    private static final EntityDataAccessor<Integer> FANS = SynchedEntityData.defineId(MachineEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BATTERY = SynchedEntityData.defineId(MachineEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CHARGE = SynchedEntityData.defineId(MachineEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> ENABLED = SynchedEntityData.defineId(MachineEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> WORKING = SynchedEntityData.defineId(MachineEntity.class, EntityDataSerializers.BOOLEAN);
    private final NonNullList<ItemStack> parts = NonNullList.withSize(MachineNodes.COUNT, ItemStack.EMPTY);
    private UUID owner;

    public MachineEntity(EntityType<? extends MachineEntity> type, Level level) { super(type, level); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(FANS, 0); b.define(BATTERY, -1); b.define(CHARGE, 0); b.define(ENABLED, false); b.define(WORKING, false);
    }
    public void setOwner(UUID value) { if (owner == null) owner = value; }
    public UUID owner() { return owner; }
    public int fanMask() { return entityData.get(FANS); }
    public int batteryNode() { return entityData.get(BATTERY); }
    public int energy() { return entityData.get(CHARGE); }
    public boolean enabled() { return entityData.get(ENABLED); }
    public boolean working() { return entityData.get(WORKING); }
    public int kind(int node) { return !MachineNodes.valid(node) ? 0 : node == batteryNode() ? 2 : (fanMask() & (1 << node)) != 0 ? 1 : 0; }
    public ItemStack part(int node) { return MachineNodes.valid(node) ? parts.get(node).copy() : ItemStack.EMPTY; }
    public boolean canModify(Player p) {
        return !level().isClientSide() && isAlive() && p.isAlive() && !p.isRemoved() && !p.isSpectator() && p.level() == level()
                && owner != null && owner.equals(p.getUUID()) && p.distanceToSqr(this) <= 25 && p.hasLineOfSight(this);
    }
    public static boolean accepts(ItemStack stack) { return stack.is(MechanicsContent.FAN) || stack.is(EnergyContent.BATTERY); }
    public boolean install(ServerPlayer p, int node, InteractionHand hand) {
        var source = p.getItemInHand(hand);
        if (!canModify(p) || enabled() || !MachineNodes.valid(node) || !parts.get(node).isEmpty() || !accepts(source)
                || source.is(EnergyContent.BATTERY) && batteryNode() >= 0) return false;
        parts.set(node, source.copyWithCount(1)); source.shrink(1); syncParts(); return true;
    }
    public boolean recover(ServerPlayer p, int node) {
        if (!canModify(p) || enabled() || !MachineNodes.valid(node) || parts.get(node).isEmpty()) return false;
        ItemStack copy = parts.get(node).copy(); var inventory = p.getInventory();
        // Even creative mode must have real room before native add can consume a copy.
        if (inventory.getFreeSlot() < 0 && inventory.getSlotWithRemainingSpace(copy) < 0) return false;
        inventory.add(copy);
        if (!copy.isEmpty()) return false;
        parts.set(node, ItemStack.EMPTY); syncParts(); return true;
    }
    public boolean setEnabled(ServerPlayer p, boolean on) {
        if (!canModify(p)) return false;
        entityData.set(ENABLED, on); if (!on) entityData.set(WORKING, false); return true;
    }
    public boolean recoverBody(ServerPlayer p) {
        if (!canModify(p) || enabled() || isVehicle() || fanMask() != 0 || batteryNode() >= 0) return false;
        var stack = new ItemStack(MechanicsContent.BODY); var inventory = p.getInventory();
        if (inventory.getFreeSlot() < 0) return false;
        inventory.add(stack); if (!stack.isEmpty()) return false; discard(); return true;
    }
    private void syncParts() {
        int mask = 0, battery = -1;
        for (int n = 0; n < parts.size(); n++) {
            if (parts.get(n).is(MechanicsContent.FAN)) mask |= 1 << n;
            if (parts.get(n).is(EnergyContent.BATTERY)) battery = n;
        }
        entityData.set(FANS, mask); entityData.set(BATTERY, battery);
        entityData.set(CHARGE, battery < 0 ? 0 : Batteries.energy(parts.get(battery)));
    }
    @Override public void tick() {
        super.tick();
        if (level().isClientSide()) return;
        // Passenger movement packets never become authoritative: no controlling passenger.
        if (getFirstPassenger() instanceof ServerPlayer p && canModify(p)) {
            var input = p.getLastClientInput();
            if (input.left() != input.right()) setYRot(getYRot() + (input.left() ? -3F : 3F));
        }
        int cost = Integer.bitCount(fanMask());
        boolean paid = enabled() && cost > 0 && batteryNode() >= 0 && Batteries.consume(parts.get(batteryNode()), cost);
        entityData.set(WORKING, paid);
        Vec3 thrust = Vec3.ZERO;
        if (paid) for (int n = 0; n < MachineNodes.COUNT; n++) if (kind(n) == 1) thrust = thrust.add(MachineNodes.thrust(n).scale(.06));
        Vec3 velocity = getDeltaMovement().add(MachineNodes.rotate(thrust, getYRot())).add(0, -.04, 0);
        double horizontal = Math.sqrt(velocity.horizontalDistanceSqr());
        if (horizontal > .4) velocity = new Vec3(velocity.x * .4 / horizontal, velocity.y, velocity.z * .4 / horizontal);
        velocity = new Vec3(velocity.x, Math.clamp(velocity.y, -.8, .3), velocity.z);
        // Do not force-load chunks. A body waits at an unloaded boundary without burning charge.
        var next = getBoundingBox().move(velocity);
        if (!level().hasChunkAt(net.minecraft.core.BlockPos.containing(next.minX, getY(), next.minZ))
                || !level().hasChunkAt(net.minecraft.core.BlockPos.containing(next.maxX, getY(), next.maxZ))) {
            if (paid) Batteries.charge(parts.get(batteryNode()), cost);
            entityData.set(WORKING, false); setDeltaMovement(Vec3.ZERO); syncParts(); return;
        }
        move(MoverType.SELF, velocity);
        setDeltaMovement(new Vec3(horizontalCollision ? 0 : velocity.x * (onGround() ? .8 : .94),
                verticalCollision ? 0 : velocity.y * .98, horizontalCollision ? 0 : velocity.z * (onGround() ? .8 : .94)));
        syncParts();
    }
    @Override protected void addAdditionalSaveData(ValueOutput out) {
        out.putInt("wildcraft_schema", 1);
        if (owner != null) out.putString("wildcraft_owner", owner.toString());
        out.store("wildcraft_parts", ItemStack.OPTIONAL_CODEC.listOf(), List.copyOf(parts));
        out.putBoolean("wildcraft_enabled", enabled());
    }
    @Override protected void readAdditionalSaveData(ValueInput in) {
        if (in.getIntOr("wildcraft_schema", 1) != 1) throw new IllegalArgumentException("Unsupported Wildcraft machine schema");
        try { owner = UUID.fromString(in.getStringOr("wildcraft_owner", "")); } catch (IllegalArgumentException e) { owner = null; }
        var decoded = in.read("wildcraft_parts", ItemStack.OPTIONAL_CODEC.listOf());
        if (decoded.isPresent() && decoded.get().size() != MachineNodes.COUNT)
            throw new IllegalArgumentException("Invalid Wildcraft machine node count");
        var saved = decoded.orElse(List.of());
        boolean batterySeen = false;
        for (int n = 0; n < parts.size(); n++) {
            var item = n < saved.size() ? saved.get(n) : ItemStack.EMPTY;
            if (!item.isEmpty() && (!accepts(item) || item.getCount() != 1 || item.is(EnergyContent.BATTERY) && batterySeen))
                throw new IllegalArgumentException("Invalid Wildcraft machine part at node " + n);
            parts.set(n, item.copy()); if (item.is(EnergyContent.BATTERY)) batterySeen = true;
        }
        syncParts(); entityData.set(ENABLED, owner != null && in.getBooleanOr("wildcraft_enabled", false));
    }
    @Override public boolean isPickable() { return true; }
    @Override public boolean canBeCollidedWith(Entity other) { return true; }
    @Override public boolean hurtServer(ServerLevel level, DamageSource source, float amount) { return false; }
    @Override public ItemStack getPickResult() { return new ItemStack(MechanicsContent.BODY); }
}
