package net.potionseeker.fallingwaterfalls.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraft.world.effect.MobEffect;
import net.potionseeker.fallingwaterfalls.FallingWaterfallsMod;
import net.potionseeker.fallingwaterfalls.potion.SalmonsStrifeMobEffect;

public class FallingWaterfallsModMobEffects {
    public static final DeferredRegister<MobEffect> REGISTRY = DeferredRegister.create(
            ForgeRegistries.MOB_EFFECTS, FallingWaterfallsMod.MODID
    );
    public static final RegistryObject<MobEffect> SALMONS_STRIFE = REGISTRY.register(
            "salmons_strife", SalmonsStrifeMobEffect::new
    );
}