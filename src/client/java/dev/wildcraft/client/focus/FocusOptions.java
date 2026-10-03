package dev.wildcraft.client.focus;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.wildcraft.Wildcraft;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/** Local presentation only; disabling effects never changes time control or stamina. */
public final class FocusOptions {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("wildcraft-focus.json");
    private static FocusOptions current = new FocusOptions();
    public int schema = 1;
    public int strength = 2;
    public boolean vignette = true;
    public boolean reticle = true;
    public boolean pattern = false;
    public boolean lowStaminaPulse = false;
    public boolean fov = false;
    public static FocusOptions get() { return current; }
    public static void load() {
        if (!Files.exists(FILE)) return;
        try (var reader = Files.newBufferedReader(FILE)) {
            var read = JSON.fromJson(reader, FocusOptions.class);
            if (read != null && read.schema == 1) { read.strength = Math.clamp(read.strength, 0, 3); current = read; }
            else Wildcraft.LOGGER.warn("Unsupported focus options schema; defaults retained and file left intact");
        } catch (IOException | RuntimeException ex) { Wildcraft.LOGGER.warn("Cannot read focus presentation options; defaults retained", ex); }
    }
    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Path temp = FILE.resolveSibling(FILE.getFileName() + ".tmp");
            Files.writeString(temp, JSON.toJson(current) + "\n");
            Files.move(temp, FILE, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) { Wildcraft.LOGGER.warn("Cannot save focus presentation options", ex); }
    }
}
