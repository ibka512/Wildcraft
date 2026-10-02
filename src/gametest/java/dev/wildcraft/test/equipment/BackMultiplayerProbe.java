package dev.wildcraft.test.equipment;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.wildcraft.equipment.BackEquipment;
import dev.wildcraft.player.GliderEquipment;
import dev.wildcraft.player.PlayerStamina;
import dev.wildcraft.registry.WildcraftItems;
import dev.wildcraft.traversal.Gliding;
import java.util.Set;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Only active in a disposable loopback lab. Normal game jars contain none of these commands or fixtures. */
public final class BackMultiplayerProbe {
    public static final AttachmentType<Integer> STAGE = AttachmentRegistry.create(Identifier.fromNamespaceAndPath("wildcraft-test", "p31_stage"), b -> b.syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.all()));
    public static final AttachmentType<Integer> ACK = AttachmentRegistry.create(Identifier.fromNamespaceAndPath("wildcraft-test", "p31_ack"), b -> b.syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.all()));
    public static void initialize() {
        if (!Boolean.getBoolean("wildcraft.p31.server")) return;
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            var p = handler.player;
            if (p.getGameProfile().name().equals("WCObserver")) {
                p.teleportTo(server.overworld(), 0.5, 80, -3.5, Set.of(), 0, 10, false);
                p.getAbilities().mayfly = true; p.getAbilities().flying = true; p.onUpdateAbilities();
            } else if (p.getGameProfile().name().equals("WCActor")) {
                BackEquipment.clear(p); p.getInventory().clearContent();
                for (int x = -10; x <= 10; x++) for (int z = -10; z <= 10; z++) {
                    server.overworld().setBlock(new BlockPos(x, 79, z), Blocks.STONE.defaultBlockState(), 3);
                    for (int y = 80; y < 85; y++) server.overworld().setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                }
                p.teleportTo(server.overworld(), 0.5, 80, 0.5, Set.of(), 0, 0, false);
                p.getInventory().setItem(0, new ItemStack(Items.DIAMOND_SWORD));
                p.getInventory().setItem(1, new ItemStack(Items.SHIELD));
                p.getInventory().setItem(2, new ItemStack(Items.BOW));
                p.getInventory().setItem(9, new ItemStack(Items.ARROW, 32));
                p.getInventory().setSelectedSlot(0); p.inventoryMenu.broadcastChanges();
                GliderEquipment.set(p, new ItemStack(WildcraftItems.PARAGLIDER));
                var cow = EntityTypes.COW.create(server.overworld(), EntitySpawnReason.COMMAND);
                cow.setNoAi(true); cow.setPos(0.5, 80, 2); server.overworld().addFreshEntity(cow);
                p.setAttached(STAGE, 0); p.setAttached(ACK, -1);
            }
        });
        CommandRegistrationCallback.EVENT.register((d, r, env) -> {
            d.register(Commands.literal("p31next").executes(ctx -> {
                ServerPlayer p = ctx.getSource().getPlayerOrException();
                if (!p.getGameProfile().name().equals("WCActor")) return 0;
                advance(p); return 1;
            }));
            d.register(Commands.literal("p31observe").then(Commands.argument("stage", IntegerArgumentType.integer(0, 12)).executes(ctx -> {
                var source = ctx.getSource().getPlayerOrException();
                if (!source.getGameProfile().name().equals("WCObserver")) return 0;
                var actor = ctx.getSource().getServer().getPlayerList().getPlayerByName("WCActor");
                if (actor != null) actor.setAttached(ACK, IntegerArgumentType.getInteger(ctx, "stage"));
                return 1;
            })));
        });
    }
    private static void advance(ServerPlayer p) {
        int old = p.getAttachedOrElse(STAGE, 0), stage = old + 1;
        if (old <= 2) check(!BackEquipment.data(p).references().get(old == 0 ? 0 : old == 1 ? 1 : 2).signature().isEmpty(), "Actual use registered stage " + old);
        if (old >= 3) check(p.getAttachedOrElse(ACK, -1) == old, "Independent observer acknowledged stage " + old);
        p.stopUsingItem();
        if (stage == 1 || stage == 2 || stage == 3 || stage == 4) {
            p.getInventory().setSelectedSlot(stage == 1 ? 1 : stage == 2 ? 2 : stage == 4 ? 0 : 3);
            p.inventoryMenu.broadcastChanges();
        } else if (stage == 5) {
            p.teleportTo(p.level(), 0.5, 110, 0.5, Set.of(), 0, 0, false);
            p.setOnGround(false); PlayerStamina.fill(p);
            var observer = p.level().getServer().getPlayerList().getPlayerByName("WCObserver");
            observer.teleportTo(p.level(), 0.5, 110, -4.5, Set.of(), 0, 0, false);
        } else if (stage == 6) {
            Gliding.reset(p, false);
            ItemStack drop = p.getInventory().removeItemNoUpdate(0);
            if (!drop.isEmpty()) p.spawnAtLocation(p.level(), drop);
            p.getInventory().setSelectedSlot(3);
            p.teleportTo(p.level(), 0.5, 80, 0.5, Set.of(), 0, 0, false);
            var observer = p.level().getServer().getPlayerList().getPlayerByName("WCObserver");
            observer.teleportTo(p.level(), 0.5, 80, -3.5, Set.of(), 0, 10, false);
        } else if (stage == 7) {
            var chest = new net.minecraft.world.SimpleContainer(27);
            p.containerMenu = ChestMenu.threeRows(1, p.getInventory(), chest);
            p.containerMenu.clicked(55, 0, ContainerInput.QUICK_MOVE, p);
            check(chest.getItem(0).is(Items.SHIELD), "Native chest transfer actually removed shield");
            p.containerMenu = p.inventoryMenu;
        } else if (stage == 8) {
            var nether = p.level().getServer().getLevel(Level.NETHER);
            for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) for (int y = 119; y < 125; y++) nether.setBlock(new BlockPos(x, y, z), y == 119 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
            p.teleportTo(nether, 0.5, 120, 0.5, Set.of(), 0, 0, true);
        } else if (stage == 9) {
            p.teleportTo(p.level().getServer().overworld(), 0.5, 80, 0.5, Set.of(), 0, 0, true);
        } else if (stage == 12) {
            System.out.println("WILDCRAFT P3.1 TWO INDEPENDENT GRAPHICAL CLIENTS PASSED: actual attack/shield/bow, held/back exclusion, native glide, drop, chest, dimension tracking and observer TCP reconnect");
        }
        p.setAttached(STAGE, stage); p.setAttached(ACK, -1);
        BackEquipment.tick(p); p.inventoryMenu.broadcastChanges();
        System.out.println("WILDCRAFT P3.1 lab stage=" + stage + " players=" + p.level().getServer().getPlayerList().getPlayerCount());
    }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
