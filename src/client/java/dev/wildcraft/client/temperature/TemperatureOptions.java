package dev.wildcraft.client.temperature;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.wildcraft.Wildcraft;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/** Independent local preference; preserves the existing focus options format. */
public final class TemperatureOptions {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("wildcraft-temperature.json");
    private static TemperatureOptions current = new TemperatureOptions();
    public int schema = 1;
    public boolean edgeTint = true;
    public static TemperatureOptions get() { return current; }
    public static void load() {
        if (!Files.exists(FILE)) return;
        try (var reader = Files.newBufferedReader(FILE)) {
            var read = JSON.fromJson(reader, TemperatureOptions.class);
            if (read != null && read.schema == 1) current = read;
            else Wildcraft.LOGGER.warn("Unsupported temperature options schema; defaults retained and file left intact");
        } catch (IOException | RuntimeException ex) { Wildcraft.LOGGER.warn("Cannot read temperature presentation options", ex); }
    }
    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            var temporary = FILE.resolveSibling(FILE.getFileName() + ".tmp");
            Files.writeString(temporary, JSON.toJson(current) + "\n");
            Files.move(temporary, FILE, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) { Wildcraft.LOGGER.warn("Cannot save temperature presentation options", ex); }
    }
}
