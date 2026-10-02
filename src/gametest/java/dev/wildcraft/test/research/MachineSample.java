package dev.wildcraft.test.research;

import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/** One axis-aligned body, four fixed nodes, one battery and fan: an R0 fixture. */
public final class MachineSample extends Entity {
    private final NonNullList<ItemStack> parts = NonNullList.withSize(4, ItemStack.EMPTY);
    private boolean powered;

    public MachineSample(EntityType<? extends MachineSample> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public boolean tryInstall(int node, ItemStack source) {
        if (level().isClientSide() || node < 0 || node >= parts.size()
                || !parts.get(node).isEmpty() || source.isEmpty()) {
            return false;
        }
        boolean battery = source.is(Items.REDSTONE) && source.has(ResearchFixtures.BATTERY_CHARGE);
        boolean fan = source.is(Items.FEATHER);
        if ((!battery && !fan) || (battery && batteryNode() >= 0) || (fan && fanNode() >= 0)) {
            return false;
        }
        parts.set(node, source.copyWithCount(1));
        source.shrink(1);
        return true;
    }

    public boolean tryRecover(int node, Inventory inventory) {
        if (level().isClientSide() || node < 0 || node >= parts.size() || parts.get(node).isEmpty()) {
            return false;
        }
        ItemStack recovered = parts.get(node).copy();
        // Vanilla creative inventory.add can destroy an item when full and still
        // report success. Check real room before transferring ownership.
        if (inventory.getFreeSlot() < 0 && inventory.getSlotWithRemainingSpace(recovered) < 0) {
            return false;
        }
        inventory.add(recovered);
        if (!recovered.isEmpty()) {
            return false;
        }
        parts.set(node, ItemStack.EMPTY);
        return true;
    }

    public ItemStack part(int node) {
        return parts.get(node).copy();
    }

    public int charge() {
        int node = batteryNode();
        return node < 0 ? 0 : parts.get(node).getOrDefault(ResearchFixtures.BATTERY_CHARGE, 0);
    }

    public void setPowered(boolean powered) {
        this.powered = powered;
    }

    private int batteryNode() {
        for (int node = 0; node < parts.size(); node++) {
            if (parts.get(node).has(ResearchFixtures.BATTERY_CHARGE)) {
                return node;
            }
        }
        return -1;
    }

    private int fanNode() {
        for (int node = 0; node < parts.size(); node++) {
            if (parts.get(node).is(Items.FEATHER)) {
                return node;
            }
        }
        return -1;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && powered && fanNode() >= 0 && charge() > 0) {
            ItemStack battery = parts.get(batteryNode());
            battery.set(ResearchFixtures.BATTERY_CHARGE, charge() - 1);
            move(MoverType.SELF, new Vec3(0.1, 0, 0));
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.store("parts", ItemStack.OPTIONAL_CODEC.listOf(), List.copyOf(parts));
        output.putBoolean("powered", powered);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        List<ItemStack> saved = input.read("parts", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());
        for (int node = 0; node < parts.size(); node++) {
            parts.set(node, node < saved.size() ? saved.get(node).copy() : ItemStack.EMPTY);
        }
        powered = input.getBooleanOr("powered", false);
    }

    @Override
    public boolean canBeCollidedWith(Entity other) {
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }
}
