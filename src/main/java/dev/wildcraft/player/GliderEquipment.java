package dev.wildcraft.player;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildcraft.Wildcraft;
import dev.wildcraft.registry.WildcraftItems;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;

/** A dedicated saved equipment slot, independent of armor and the normal inventory. */
public final class GliderEquipment {
    public record Data(int schemaVersion, ItemStack item) {
        public static final Codec<Data> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(1, 1).optionalFieldOf("schemaVersion", 1).forGetter(Data::schemaVersion),
                ItemStack.OPTIONAL_CODEC.optionalFieldOf("item", ItemStack.EMPTY).forGetter(Data::item)
        ).apply(instance, Data::new));
    }

    public static final AttachmentType<Data> DATA = AttachmentRegistry.create(Wildcraft.id("glider_equipment"), builder -> builder
            .persistent(Data.CODEC).copyOnDeath()
            .syncWith(ItemStack.OPTIONAL_STREAM_CODEC.map(stack -> new Data(1, stack), Data::item), AttachmentSyncPredicate.targetOnly()));

    private GliderEquipment() {
    }

    public static void initialize() {
        ServerPlayerEvents.AFTER_RESPAWN.register(Wildcraft.AFTER_ATTACHMENT_TRANSFER, (oldPlayer, newPlayer, alive) ->
                set(newPlayer, get(newPlayer).copy()));
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player && !player.isSpectator()
                    && !player.level().getGameRules().get(GameRules.KEEP_INVENTORY)) {
                ItemStack dropped = get(player).copy();
                set(player, ItemStack.EMPTY);
                if (!dropped.isEmpty()) player.spawnAtLocation(player.level(), dropped);
            }
        });
    }

    public static ItemStack get(Player player) {
        return player.getAttachedOrCreate(DATA, () -> new Data(1, ItemStack.EMPTY)).item();
    }

    public static void set(Player player, ItemStack stack) {
        player.setAttached(DATA, new Data(1, stack.copy()));
    }

    public static boolean equipped(Player player) {
        return get(player).is(WildcraftItems.PARAGLIDER);
    }

    public static GliderSlot slot(AbstractContainerMenu menu) {
        return (GliderSlot) menu.slots.stream().filter(slot -> slot instanceof GliderSlot).findFirst().orElseThrow();
    }
}
