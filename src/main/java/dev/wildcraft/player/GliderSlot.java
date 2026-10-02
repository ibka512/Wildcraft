package dev.wildcraft.player;

import dev.wildcraft.Wildcraft;
import dev.wildcraft.registry.WildcraftItems;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class GliderSlot extends Slot {
    public static final int X = 77;
    public static final int Y = 8;

    public GliderSlot(Player owner) {
        super(new EquipmentContainer(owner), 0, X, Y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return stack.is(WildcraftItems.PARAGLIDER);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public Identifier getNoItemIcon() {
        return Wildcraft.id("container/slot/paraglider");
    }

    private static final class EquipmentContainer implements Container {
        private final Player owner;

        EquipmentContainer(Player owner) {
            this.owner = owner;
        }

        public int getContainerSize() { return 1; }
        public boolean isEmpty() { return GliderEquipment.get(owner).isEmpty(); }
        public ItemStack getItem(int slot) { return slot == 0 ? GliderEquipment.get(owner) : ItemStack.EMPTY; }
        public int getMaxStackSize() { return 1; }
        public boolean stillValid(Player player) { return player == owner; }
        public boolean canPlaceItem(int slot, ItemStack stack) { return slot == 0 && stack.is(WildcraftItems.PARAGLIDER); }

        public ItemStack removeItem(int slot, int count) {
            ItemStack result = getItem(slot).split(count);
            setChanged();
            return result;
        }

        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack result = getItem(slot);
            if (slot == 0) GliderEquipment.set(owner, ItemStack.EMPTY);
            return result;
        }

        public void setItem(int slot, ItemStack stack) {
            if (slot == 0) GliderEquipment.set(owner, stack);
        }

        public void setChanged() {
            // Slot transfers can mutate a returned stack before calling setChanged.
            GliderEquipment.set(owner, GliderEquipment.get(owner));
        }

        public void clearContent() { GliderEquipment.set(owner, ItemStack.EMPTY); }
    }
}
