package net.potionseeker.fallingwaterfalls.init;

import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.level.GameRules;
import net.potionseeker.fallingwaterfalls.FallingWaterfallsMod;

@Mod.EventBusSubscriber(modid = FallingWaterfallsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class FallingWaterfallsModGameRules {
	public static final GameRules.Key<GameRules.BooleanValue> NO_FALL_SWIMMING = GameRules.register(
			"noFallSwimming", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true)
	);
	public static final GameRules.Key<GameRules.BooleanValue> WATERFALL_DAMAGE = GameRules.register(
			"waterfallDamage", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true)
	);
}