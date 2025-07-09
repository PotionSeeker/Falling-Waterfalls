package net.potionseeker.fallingwaterfalls.init;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.potionseeker.fallingwaterfalls.FallingWaterfallsMod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FallingWaterfallsDamageSources {
    private static final Logger LOGGER = LogManager.getLogger(FallingWaterfallsDamageSources.class);

    public static DamageSource waterfallFall(Level level) {
        LOGGER.debug("Attempting to access DamageType: {}", FallingWaterfallsDamageTypes.WATERFALL_FALL.location());
        if (level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).containsKey(FallingWaterfallsDamageTypes.WATERFALL_FALL.location())) {
            LOGGER.debug("DamageType found: {}", FallingWaterfallsDamageTypes.WATERFALL_FALL.location());
            return new DamageSource(level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(FallingWaterfallsDamageTypes.WATERFALL_FALL));
        } else {
            LOGGER.warn("DamageType not found: {}. Falling back to custom WaterfallDamageSource with minecraft:fall type", FallingWaterfallsDamageTypes.WATERFALL_FALL.location());
            return new WaterfallDamageSource(
                    level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(
                            net.minecraft.resources.ResourceKey.create(
                                    net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                                    new net.minecraft.resources.ResourceLocation("minecraft", "fall")
                            )
                    )
            );
        }
    }

    public static class WaterfallDamageSource extends DamageSource {
        public WaterfallDamageSource(net.minecraft.core.Holder<DamageType> type) {
            super(type);
        }

        @Override
        public Component getLocalizedDeathMessage(LivingEntity entity) {
            LOGGER.debug("Returning custom death message for player {}", entity.getName().getString());
            return Component.translatable("death.fallingwaterfalls.waterfall", entity.getName());
        }
    }
}