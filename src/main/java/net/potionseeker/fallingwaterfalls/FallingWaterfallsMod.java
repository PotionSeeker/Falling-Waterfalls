package net.potionseeker.fallingwaterfalls;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.common.MinecraftForge;
import net.potionseeker.fallingwaterfalls.init.FallingWaterfallsDamageSources;
import net.potionseeker.fallingwaterfalls.init.FallingWaterfallsModGameRules;
import net.potionseeker.fallingwaterfalls.init.FallingWaterfallsModMobEffects;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

@Mod(FallingWaterfallsMod.MODID)
public class FallingWaterfallsMod {
	public static final String MODID = "falling_waterfalls";
	public static final Logger LOGGER = LogManager.getLogger(FallingWaterfallsMod.class);

	public FallingWaterfallsMod() {
		LOGGER.debug("Initializing FallingWaterfallsMod");
		IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		FallingWaterfallsModMobEffects.REGISTRY.register(modEventBus);
		MinecraftForge.EVENT_BUS.register(this);
		LOGGER.debug("Registered mod event bus and Forge event bus");
	}

	@SubscribeEvent
	public void onLivingDeath(LivingDeathEvent event) {
		if (event.getEntity() instanceof Player player && player.level() instanceof ServerLevel) {
			if ((event.getSource().type().msgId().equals("fallingwaterfalls.waterfall") ||
					event.getSource() instanceof FallingWaterfallsDamageSources.WaterfallDamageSource) &&
					player.getPersistentData().getBoolean("falling_waterfalls:waterfall_fall")) {
				LOGGER.debug("Processing death for player {} due to waterfall fall with DamageSource: {}, msgId: {}",
						player.getName().getString(), event.getSource().getClass().getSimpleName(),
						event.getSource().type().msgId());
				player.getPersistentData().remove("falling_waterfalls:waterfall_fall"); // Clean up
			}
		}
	}
}