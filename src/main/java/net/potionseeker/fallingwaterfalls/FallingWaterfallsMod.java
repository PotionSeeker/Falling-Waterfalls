package net.potionseeker.fallingwaterfalls;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.common.MinecraftForge;
import net.potionseeker.fallingwaterfalls.init.FallingWaterfallsModMobEffects;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

@Mod(FallingWaterfallsMod.MODID)
public class FallingWaterfallsMod {
	public static final String MODID = "falling_waterfalls";
	public static final Logger LOGGER = LogManager.getLogger(FallingWaterfallsMod.class);

	public FallingWaterfallsMod() {
		IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		FallingWaterfallsModMobEffects.REGISTRY.register(modEventBus);
		MinecraftForge.EVENT_BUS.register(this);
	}
}