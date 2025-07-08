package net.potionseeker.fallingwaterfalls.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.potionseeker.fallingwaterfalls.FallingWaterfallsMod;
import net.potionseeker.fallingwaterfalls.init.FallingWaterfallsModGameRules;
import net.potionseeker.fallingwaterfalls.init.FallingWaterfallsModMobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

@Mixin(Player.class)
public abstract class PlayerMixin extends Entity {
    private static final Logger LOGGER = LogManager.getLogger(PlayerMixin.class);
    private float waterfallFallDistance = 0.0F;

    public PlayerMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "travel(Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"))
    private void onTravel(Vec3 movementInput, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (player.isCreative() || player.isSpectator() || !player.level().getGameRules().getBoolean(FallingWaterfallsModGameRules.NO_FALL_SWIMMING)) {
            LOGGER.debug("Skipping waterfall logic for player {}: Creative, Spectator, or game rule disabled", player.getName().getString());
            waterfallFallDistance = 0.0F;
            return;
        }

        if (player.hasEffect(FallingWaterfallsModMobEffects.SALMONS_STRIFE.get())) {
            LOGGER.debug("Skipping waterfall logic for player {}: Has Salmon's Strife effect", player.getName().getString());
            waterfallFallDistance = 0.0F;
            return;
        }

        BlockPos pos = player.blockPosition();
        FluidState fluidState = player.level().getFluidState(pos);
        if (fluidState.is(Fluids.FLOWING_WATER) && !fluidState.isSource()) {
            LOGGER.debug("Player {} in flowing water at {}, applying downward force", player.getName().getString(), pos);
            player.setDeltaMovement(player.getDeltaMovement().subtract(0, 0.12, 0));
            player.setSwimming(false);
            player.hurtMarked = true;

            // Track fall distance while in flowing water
            if (player.getDeltaMovement().y < 0) {
                waterfallFallDistance += (float) -player.getDeltaMovement().y;
                LOGGER.debug("Player {} fall distance in waterfall: {}", player.getName().getString(), waterfallFallDistance);
            }
        } else {
            // Reset fall distance if not in flowing water
            if (!fluidState.is(Fluids.FLOWING_WATER) || fluidState.isSource()) {
                waterfallFallDistance = 0.0F;
                LOGGER.debug("Reset fall distance for player {} at {}", player.getName().getString(), pos);
            }
        }

        // Check for landing on solid block
        BlockPos belowPos = pos.below();
        if (!player.level().getBlockState(belowPos).getCollisionShape(player.level(), belowPos).isEmpty()) {
            if (waterfallFallDistance > 3.0F) {
                float damage = waterfallFallDistance - 3.0F; // 1 damage per block after 3 blocks
                LOGGER.debug("Player {} landed after falling {} blocks, applying {} damage", player.getName().getString(), waterfallFallDistance, damage);
                player.hurt(player.level().damageSources().fall(), damage);
                MinecraftForge.EVENT_BUS.post(new LivingFallEvent(player, damage, 1.0F));
            }
            waterfallFallDistance = 0.0F;
        }
    }
}