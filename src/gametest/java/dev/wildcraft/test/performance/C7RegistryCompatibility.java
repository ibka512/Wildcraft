package dev.wildcraft.test.performance;
import dev.wildcraft.test.research.ResearchFixtures;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.NoopRenderer;
/** Restores only original test registry IDs in a cloned generated world, without old research timers/commands/observers. */
public final class C7RegistryCompatibility implements ModInitializer,ClientModInitializer {
    @Override public void onInitialize(){if(ResearchFixtures.MACHINE==null)throw new AssertionError("Original fixture registry missing");if(Boolean.getBoolean("wildcraft.c7.partsBaseline"))net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(NativePartsBaseline::serverTick);else if(Boolean.getBoolean("wildcraft.c7.waterBaseline"))net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(NativeBuoyancyBaseline::serverTick);else if(Boolean.getBoolean("wildcraft.c7.terrainBaseline"))net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(NativeTerrainBaseline::serverTick);else if(Boolean.getBoolean("wildcraft.c7.driveBaseline"))net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(NativeDrivingBaseline::serverTick);}
    @Override public void onInitializeClient(){EntityRenderers.register(ResearchFixtures.MACHINE,NoopRenderer::new);}
}
