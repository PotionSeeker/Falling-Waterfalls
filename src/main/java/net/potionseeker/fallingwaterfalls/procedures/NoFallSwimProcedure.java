//package net.potionseeker.fallingwaterfalls.procedures;

//import net.minecraftforge.fml.common.Mod;
//import net.minecraftforge.eventbus.api.SubscribeEvent;
//import net.minecraftforge.event.TickEvent;
//import net.minecraft.world.entity.player.Player;
//import net.minecraft.world.entity.Entity;
//import net.minecraft.world.level.LevelAccessor;
//import net.minecraft.world.phys.Vec3;
//import net.minecraft.core.BlockPos;
//import net.minecraft.world.level.material.Fluids;
//import net.potionseeker.fallingwaterfalls.FallingWaterfallsMod;
//import net.potionseeker.fallingwaterfalls.init.FallingWaterfallsModGameRules;
//import net.potionseeker.fallingwaterfalls.init.FallingWaterfallsModMobEffects;
//import org.apache.logging.log4j.Logger;
//import org.apache.logging.log4j.LogManager;

//@Mod.EventBusSubscriber(modid = FallingWaterfallsMod.MODID)
//public class NoFallSwimProcedure {
//	private static final Logger LOGGER = LogManager.getLogger(NoFallSwimProcedure.class);
//
//	@SubscribeEvent
//	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
//		if (event.phase == TickEvent.Phase.END) {
//			execute(event.player.level(), event.player);
//		}
//	}
//
//	public static void execute(LevelAccessor world, Entity entity) {
//		if (!(entity instanceof Player player) || !world.getLevelData().getGameRules().getBoolean(FallingWaterfallsModGameRules.NO_FALL_SWIMMING)) {
//			//LOGGER.debug("Skipping: Not a player or game rule disabled for player {}", player.getName().getString());
//			return;
//		}
//
//		if (player.isCreative() || player.isSpectator()) {
//			LOGGER.debug("Skipping: Player {} in Creative or Spectator mode", player.getName().getString());
//			return;
//		}
//
//		BlockPos playerPos = player.blockPosition();
//		BlockPos belowPlayerPos = playerPos.below();
//		var blockStateBelow = world.getBlockState(belowPlayerPos);
//		if (!blockStateBelow.getCollisionShape(world, belowPlayerPos).isEmpty()) {
//			LOGGER.debug("Skipping: Solid block {} below player at {}", blockStateBelow.getBlock().getName().getString(), belowPlayerPos);
//			return;
//		}
//
//		if (player.hasEffect(FallingWaterfallsModMobEffects.SALMONS_STRIFE.get())) {
//			LOGGER.debug("Skipping: Player {} has Salmon's Strife effect", player.getName().getString());
//			return;
//		}
//
//		Vec3 playerVec = player.position();
//		boolean isInNonSourceWater = false;
//		double range = 0.3;
//
//
//		for (double x = -range; x <= range; x += 0.3) {
//			for (double z = -range; z <= range; z += 0.3) {
//				BlockPos checkPos = new BlockPos((int) (playerVec.x + x), (int) playerVec.y, (int) (playerVec.z + z));
//				var fluidState = world.getFluidState(checkPos);
//				if (fluidState.is(Fluids.FLOWING_WATER) && !fluidState.isSource()) {
//					isInNonSourceWater = true;
//					LOGGER.debug("Found non-source water at {} for player {} (fluid level: {})",
//							checkPos, player.getName().getString(), fluidState.getAmount());
//					break;
//				}
//			}
//			if (isInNonSourceWater) {
//				break;
//			}
//		}
//
//		if (isInNonSourceWater) {
//			player.setDeltaMovement(player.getDeltaMovement().subtract(0, 0.12, 0));
//			player.setSwimming(false);
//			player.hurtMarked = true;
//			LOGGER.debug("Applied downward force to player {} at {}", player.getName().getString(), playerVec);
//		} else {
//			LOGGER.debug("No non-source water detected around player {} at {}", player.getName().getString(), playerVec);
//		}
//	}
//}