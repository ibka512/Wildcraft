package dev.wildcraft.test.research;

import com.mojang.serialization.Codec;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Registrations exist only in the separate test mod, never the game jar. */
public final class ResearchFixtures implements ModInitializer {
    public static final DataComponentType<Integer> BATTERY_CHARGE = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE, id("battery_charge"),
            DataComponentType.<Integer>builder().persistent(Codec.intRange(0, 400))
                    .networkSynchronized(ByteBufCodecs.VAR_INT).build());
    public static final DataComponentType<FuseSample> FUSE = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE, id("fuse_sample"),
            DataComponentType.<FuseSample>builder().persistent(FuseSample.CODEC)
                    .networkSynchronized(StreamCodec.composite(ItemStack.STREAM_CODEC, FuseSample::material, FuseSample::new)).build());
    private static final ResourceKey<EntityType<?>> MACHINE_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, id("machine_sample"));
    public static final EntityType<MachineSample> MACHINE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE, MACHINE_KEY,
            EntityType.Builder.of(MachineSample::new, MobCategory.MISC)
                    .sized(1.2F, 0.6F).passengerAttachments(0.65F)
                    .clientTrackingRange(8).updateInterval(2).noLootTable().build(MACHINE_KEY));

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("wildcraft-test", path);
    }

    public static ItemStack battery(int charge) {
        ItemStack stack = new ItemStack(Items.REDSTONE);
        stack.set(BATTERY_CHARGE, charge);
        return stack;
    }

    @Override
    public void onInitialize() {
        dev.wildcraft.test.research.time.WorldTimeResearch.initialize();
        dev.wildcraft.test.equipment.BackMultiplayerProbe.initialize();
        dev.wildcraft.test.mechanics.MachineMultiplayerProbe.initialize();
        dev.wildcraft.test.mechanics.PartsMultiplayerProbe.initialize();
        dev.wildcraft.test.fabrication.FabricationMultiplayerProbe.initialize();
        // Static registration initializes this test-only registry.
    }
}
