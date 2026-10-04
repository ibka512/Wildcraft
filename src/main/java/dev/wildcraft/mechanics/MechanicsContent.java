package dev.wildcraft.mechanics;

import dev.wildcraft.Wildcraft;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;

public final class MechanicsContent {
    private static final ResourceKey<EntityType<?>> BODY_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Wildcraft.id("machine"));
    public static final EntityType<MachineEntity> MACHINE = Registry.register(BuiltInRegistries.ENTITY_TYPE, BODY_KEY,
            EntityType.Builder.of(MachineEntity::new, MobCategory.MISC).sized(1.4F, .65F)
                    .passengerAttachments(.7F).clientTrackingRange(10).updateInterval(2).noLootTable().build(BODY_KEY));
    private static final ResourceKey<Item> ITEM_KEY = ResourceKey.create(Registries.ITEM, Wildcraft.id("machine_body"));
    public static final Item BODY = Registry.register(BuiltInRegistries.ITEM, ITEM_KEY,
            new MachineBodyItem(new Item.Properties().setId(ITEM_KEY).stacksTo(1)));
    private static final ResourceKey<Item> FAN_KEY = ResourceKey.create(Registries.ITEM, Wildcraft.id("fan"));
    public static final Item FAN = Registry.register(BuiltInRegistries.ITEM, FAN_KEY, new Item(new Item.Properties().setId(FAN_KEY).stacksTo(16)));
    private MechanicsContent() { }
    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(o -> { o.accept(BODY); o.accept(FAN); });
    }
}
