package dev.wildcraft.test.research.animation;
import net.fabricmc.api.ClientModInitializer;
public final class AnimationResearchClient implements ClientModInitializer {
    @Override public void onInitializeClient(){SkinProbeLayer.initialize();}
}
