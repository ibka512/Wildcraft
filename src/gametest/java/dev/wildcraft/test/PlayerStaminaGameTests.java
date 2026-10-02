package dev.wildcraft.test;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.wildcraft.player.PlayerStamina;
import dev.wildcraft.player.StaminaData;
import dev.wildcraft.player.StaminaRules;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

public final class PlayerStaminaGameTests {
    @GameTest
    public void recoveryDelayAndAirborneRules(GameTestHelper helper) {
        StaminaData data = StaminaData.initial(15).consume(50);
        for (int tick = 0; tick < 40; tick++) {
            data = data.tick(false);
        }
        helper.assertValueEqual(data.stamina(), 80.0, "Airborne never restores stamina");
        for (int tick = 0; tick < StaminaRules.RECOVERY_DELAY_TICKS; tick++) {
            data = data.tick(true);
        }
        helper.assertValueEqual(data.stamina(), 80.0, "Stable landing delay completes before recovery");
        data = data.tick(true);
        helper.assertValueEqual(data.stamina(), 81.0, "Recovery starts after the delay");
        data = data.consume(1000);
        helper.assertValueEqual(data.stamina(), 0.0, "Excess consumption clamps to zero");
        helper.assertValueEqual(data.filled().stamina(), 130.0, "Fill never exceeds derived capacity");
        helper.succeed();
    }

    @GameTest
    public void codecRoundTripDefaultsAndInvalidData(GameTestHelper helper) {
        StaminaData original = StaminaData.initial(30).consume(67);
        var encoded = StaminaData.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
        helper.assertValueEqual(StaminaData.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow(), original, "Saved value round trips");
        StaminaData defaults = StaminaData.CODEC.parse(JsonOps.INSTANCE, new JsonObject()).getOrThrow();
        helper.assertValueEqual(defaults.highestLevel(), 0, "Missing peak defaults safely");
        helper.assertValueEqual(defaults.stamina(), 0.0, "Missing stamina does not grant free fill");
        for (String field : new String[]{"schemaVersion", "highestLevel", "stamina", "recoveryDelay"}) {
            JsonObject invalid = encoded.getAsJsonObject().deepCopy();
            invalid.addProperty(field, field.equals("schemaVersion") ? 2 : -1);
            helper.assertTrue(StaminaData.CODEC.parse(JsonOps.INSTANCE, invalid).error().isPresent(), "Invalid " + field + " rejected");
        }
        helper.succeed();
    }

    @GameTest
    public void originalExperienceHooksAndTwoPlayerIsolation(GameTestHelper helper) {
        ServerPlayer first = helper.makeMockServerPlayerInLevel();
        ServerPlayer second = helper.makeMockServerPlayerInLevel();
        first.setExperienceLevels(15);
        first.setExperienceLevels(30);
        first.setExperienceLevels(5);
        second.setExperienceLevels(7);
        helper.assertValueEqual(PlayerStamina.get(first).highestLevel(), 30, "Same-tick peak survives XP spend");
        helper.assertValueEqual(PlayerStamina.get(first).capacity(), 160.0, "Peak drives capacity");
        StaminaData untouched = PlayerStamina.get(second);
        PlayerStamina.consume(first, 70);
        helper.assertValueEqual(PlayerStamina.get(second), untouched, "Second logged-in player remains unchanged");
        helper.assertTrue(AttachmentSyncPredicate.targetOnly().test(first, first), "Owner receives view");
        helper.assertFalse(AttachmentSyncPredicate.targetOnly().test(first, second), "Other player does not receive view");
        first.giveExperienceLevels(35);
        first.giveExperienceLevels(-39);
        helper.assertValueEqual(PlayerStamina.get(first).highestLevel(), 40, "Vanilla added levels preserve intermediate peak");
        first.discard();
        second.discard();
        helper.succeed();
    }

    @GameTest
    public void playerSaveAndRespawnCopySemantics(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.removeAttached(PlayerStamina.DATA);
        player.setExperienceLevels(15);
        helper.assertValueEqual(PlayerStamina.get(player).stamina(), 130.0, "Existing player initializes from current level");
        player.setExperienceLevels(30);
        PlayerStamina.consume(player, 60);
        StaminaData saved = PlayerStamina.get(player);
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        player.saveWithoutId(output);
        ServerPlayer restored = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), player.getGameProfile(), player.clientInformation());
        restored.load(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), output.buildResult()));
        helper.assertValueEqual(PlayerStamina.get(restored), saved, "Native player serialization preserves peak, stamina and timer");
        var playerList = helper.getLevel().getServer().getPlayerList();
        ServerPlayer aliveCopy = playerList.respawn(player, true, Entity.RemovalReason.CHANGED_DIMENSION);
        aliveCopy.connection.player = aliveCopy;
        helper.assertValueEqual(PlayerStamina.get(aliveCopy), saved, "Alive replacement keeps remaining stamina");
        ServerPlayer deadCopy = playerList.respawn(aliveCopy, false, Entity.RemovalReason.KILLED);
        deadCopy.connection.player = deadCopy;
        helper.assertValueEqual(PlayerStamina.get(deadCopy).highestLevel(), 30, "Death copy retains peak");
        helper.assertValueEqual(PlayerStamina.get(deadCopy).stamina(), 160.0, "Death copy refills stamina");
        deadCopy.discard();
        helper.succeed();
    }
}
