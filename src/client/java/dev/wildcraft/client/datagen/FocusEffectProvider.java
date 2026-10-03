package dev.wildcraft.client.datagen;

import com.google.gson.JsonParser;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;

public final class FocusEffectProvider implements DataProvider {
    private final Path root;
    public FocusEffectProvider(FabricPackOutput output) { root = output.getOutputFolder().resolve("assets/wildcraft/post_effect/focus"); }
    @Override public String getName() { return "Wildcraft focus presentation variants"; }
    @Override public CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> writes = new ArrayList<>();
        for (int strength = 1; strength <= 3; strength++) for (int vignette = 0; vignette <= 1; vignette++) for (int pattern = 0; pattern <= 1; pattern++) {
            double intensity = strength == 1 ? 0.10 : strength == 2 ? 0.15 : 0.20;
            String uniforms = ", \"uniforms\": {\"FocusConfig\": [{\"name\":\"Settings\",\"type\":\"vec4\",\"value\":[" + intensity + "," + vignette + "," + pattern + ",0]}]}";
            writes.add(DataProvider.saveStable(cache, JsonParser.parseString(chain("focus", uniforms)), root.resolve(strength + "_" + vignette + "_" + pattern + ".json")));
        }
        writes.add(DataProvider.saveStable(cache, JsonParser.parseString(chain("focus_pulse", "")), root.resolve("low_stamina.json")));
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }
    private static String chain(String fragment, String uniforms) {
        return "{\"targets\":{\"swap\":{}},\"passes\":[{\"vertex_shader\":\"minecraft:core/screenquad\",\"fragment_shader\":\"wildcraft:post/" + fragment
                + "\",\"inputs\":[{\"sampler_name\":\"In\",\"target\":\"minecraft:main\"}],\"output\":\"swap\"" + uniforms
                + "},{\"vertex_shader\":\"minecraft:core/screenquad\",\"fragment_shader\":\"wildcraft:post/focus_copy\",\"inputs\":[{\"sampler_name\":\"In\",\"target\":\"swap\"}],\"output\":\"minecraft:main\"}]}";
    }
}
