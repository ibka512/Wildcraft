package dev.wildcraft.test;

import dev.wildcraft.player.StaminaRules;
import java.util.Random;

/** Dependency-free numerical checks executed by verifyRules as part of check/build. */
public final class StaminaRulesCheck {
    public static void main(String[] arguments) {
        FocusClockCheck.main(arguments);
        equal(StaminaRules.capacity(0), 100);
        equal(StaminaRules.capacity(15), 130);
        equal(StaminaRules.capacity(30), 160);
        equal(StaminaRules.capacity(Integer.MAX_VALUE), 4_294_967_394.0);
        equal(StaminaRules.spend(80, 130, 50), 30);
        equal(StaminaRules.spend(30, 130, 500), 0);
        equal(StaminaRules.recover(129.5, 130), 130);
        equal(StaminaRules.recover(130, 130), 130);
        rejects(() -> StaminaRules.capacity(-1));
        rejects(() -> StaminaRules.spend(10, 100, -1));
        rejects(() -> StaminaRules.spend(10, 100, Double.NaN));
        rejects(() -> StaminaRules.spend(10, 100, Double.POSITIVE_INFINITY));
        rejects(() -> StaminaRules.recover(101, 100));
        Random random = new Random(0x57494C44);
        for (int iteration = 0; iteration < 1000; iteration++) {
            double capacity = StaminaRules.capacity(random.nextInt(10_000));
            double current = random.nextDouble() * capacity;
            double spent = StaminaRules.spend(current, capacity, random.nextDouble() * capacity * 2);
            double recovered = StaminaRules.recover(spent, capacity);
            if (spent < 0 || spent > current || recovered < spent || recovered > capacity) {
                throw new AssertionError("Numerical bounds or monotonicity violated");
            }
        }
        System.out.println("WILDCRAFT stamina numerical checks passed: 13 examples + 1000 bound/monotonicity cases");
    }

    private static void equal(double actual, double expected) {
        if (actual != expected) {
            throw new AssertionError("Expected " + expected + ", got " + actual);
        }
    }

    private static void rejects(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Invalid input accepted");
    }
}
