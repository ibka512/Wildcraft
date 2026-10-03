package dev.wildcraft.temperature;

import java.util.Locale;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

/** Read-only administrator diagnostics; clients cannot set the environment reading. */
public final class TemperatureCommands {
    private TemperatureCommands() { }
    public static void initialize() {
        CommandRegistrationCallback.EVENT.register((d, r, e) -> d.register(Commands.literal("wildcraft")
                .requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.literal("temperature").then(Commands.literal("status")
                        .executes(c -> status(c.getSource(), c.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> status(c.getSource(), EntityArgument.getPlayer(c, "player"))))))));
    }
    private static int status(CommandSourceStack source, ServerPlayer player) {
        var sample = TemperatureSampler.sample(player);
        source.sendSuccess(() -> Component.translatable("command.wildcraft.temperature.status",
                Component.translatable("hud.wildcraft.temperature." + TemperatureRules.STATES[TemperatureRules.initialBand(sample.target())]),
                String.format(Locale.ROOT, "%.2f", sample.target()), sample.base(), sample.weather(), sample.water(), sample.heat(), sample.blockReads(), sample.rays()), false);
        return 1;
    }
}
