package dev.wildcraft.player;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import java.util.Locale;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

/** Administrator verification tools; normal traversal will call PlayerStamina directly. */
public final class StaminaCommands {
    private StaminaCommands() {
    }

    public static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registries, environment) -> dispatcher.register(
                Commands.literal("wildcraft")
                        .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                        .then(Commands.literal("stamina")
                                .then(Commands.literal("status")
                                        .executes(context -> status(context.getSource(), context.getSource().getPlayerOrException()))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(context -> status(context.getSource(), EntityArgument.getPlayer(context, "player")))))
                                .then(Commands.literal("consume")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.001, 1_000_000))
                                                        .executes(context -> {
                                                            ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                                            double consumed = PlayerStamina.consume(player, DoubleArgumentType.getDouble(context, "amount"));
                                                            context.getSource().sendSuccess(() -> Component.translatable(
                                                                    "command.wildcraft.stamina.consumed", number(consumed)), false);
                                                            return 1;
                                                        }))))
                                .then(Commands.literal("fill")
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(context -> {
                                                    PlayerStamina.fill(EntityArgument.getPlayer(context, "player"));
                                                    context.getSource().sendSuccess(() -> Component.translatable("command.wildcraft.stamina.filled"), false);
                                                    return 1;
                                                }))))));
    }

    private static int status(CommandSourceStack source, ServerPlayer player) {
        StaminaData data = PlayerStamina.get(player);
        source.sendSuccess(() -> Component.translatable("command.wildcraft.stamina.status",
                number(data.stamina()), number(data.capacity()), data.highestLevel()), false);
        return 1;
    }

    private static String number(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
