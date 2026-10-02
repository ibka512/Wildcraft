package dev.wildcraft.test.research;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.NoopRenderer;

public final class ResearchClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Geometry/ownership research has no finished model. The body still tracks normally.
        EntityRenderers.register(ResearchFixtures.MACHINE, NoopRenderer::new);
    }
}
