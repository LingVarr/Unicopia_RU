package com.minelittlepony.unicopia;

import net.minecraft.world.GameRules;
import net.minecraft.world.GameRules.BooleanRule;
import net.minecraft.world.GameRules.IntRule;

public interface UGameRules {
    GameRules.Key<BooleanRule> SWAP_TRIBE_ON_DEATH = GameRules.register("swapTribeOnDeath", GameRules.Category.SPAWNING, BooleanRule.create(false));
    GameRules.Key<BooleanRule> ANNOUNCE_TRIBE_JOINS = GameRules.register("announceTribeJoins", GameRules.Category.SPAWNING, BooleanRule.create(false));
    GameRules.Key<IntRule> WEATHER_EFFECTS_STRENGTH = GameRules.register("weatherEffectsStrength", GameRules.Category.MISC, IntRule.create(100));

    static void bootstrap() { }
}
