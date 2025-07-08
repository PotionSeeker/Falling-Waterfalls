package net.potionseeker.fallingwaterfalls.procedures;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.LiquidBlock;
import net.potionseeker.fallingwaterfalls.FallingWaterfallsMod;
import net.potionseeker.fallingwaterfalls.init.FallingWaterfallsModGameRules;
import net.potionseeker.fallingwaterfalls.init.FallingWaterfallsModMobEffects;

@Mod.EventBusSubscriber(modid = FallingWaterfallsMod.MODID)
public class NoFallSwimProcedure {
	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase == TickEvent.Phase.END) {
			execute(event.player.level, event.player);
		}
	}

	public static void execute(LevelAccessor world, Entity entity) {
		if (!(entity instanceof Player player) || !world.getLevelData().getGameRules().getBoolean(FallingWaterfallsModGameRules.NO_FALL_SWIMMING)) {
			return;
		}

		if (player.isCreative() || player.isSpectator()) {
			return;
		}

		BlockPos belowPlayerPos = new BlockPos(player.getX(), player.getY() - 1, player.getZ());
		if (world.getBlockState(belowPlayerPos).isSolidRender(world, belowPlayerPos)) {
			return;
		}

		if (player.hasEffect(FallingWaterfallsModMobEffects.SALMONS_STRIFE.get())) {
			return;
		}

		Vec3 playerPos = player.position();
		boolean isInNonSourceFluid = false;
		double range = 0.3;

		for (double x = -range; x <= range; x += 0.3) {
			for (double z = -range; z <= range; z += 0.3) {
				BlockPos checkPos = new BlockPos(playerPos.add(x, 0, z));
				if (!world.getFluidState(checkPos).isEmpty() && !world.getFluidState(checkPos).isSource()) {
					isInNonSourceFluid = true;
					break;
				}
			}
			if (isInNonSourceFluid) {
				break;
			}
		}

		if (isInNonSourceFluid) {
			player.setDeltaMovement(player.getDeltaMovement().subtract(0, 0.09, 0));
			player.setSwimming(false);
		}
	}
}