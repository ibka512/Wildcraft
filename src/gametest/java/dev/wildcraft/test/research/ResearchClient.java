package dev.wildcraft.test.research;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.NoopRenderer;

public final class ResearchClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        dev.wildcraft.test.research.time.ResearchBackLayer.initialize();
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register((entity, level) ->
                { dev.wildcraft.test.research.time.NativeTimeProbe.arrowSpawned(entity); dev.wildcraft.test.research.time.FocusNativeProbe.arrowSpawned(entity); });
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) dev.wildcraft.test.research.time.WorldTimeResearch.clientTick(client.player, client.isPaused());
        });
        // Geometry/ownership research has no finished model. The body still tracks normally.
        EntityRenderers.register(ResearchFixtures.MACHINE, NoopRenderer::new);
    }
}
