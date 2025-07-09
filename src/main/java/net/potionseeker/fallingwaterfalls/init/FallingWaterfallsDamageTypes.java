package net.potionseeker.fallingwaterfalls.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.potionseeker.fallingwaterfalls.FallingWaterfallsMod;

public class FallingWaterfallsDamageTypes {
    public static final ResourceKey<DamageType> WATERFALL_FALL = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            new ResourceLocation(FallingWaterfallsMod.MODID, "waterfall_fall")
    );
}