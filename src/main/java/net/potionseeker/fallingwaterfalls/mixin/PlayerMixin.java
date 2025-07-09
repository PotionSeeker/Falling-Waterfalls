package net.potionseeker.fallingwaterfalls.mixin;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.potionseeker.fallingwaterfalls.FallingWaterfallsMod;
import net.potionseeker.fallingwaterfalls.init.FallingWaterfallsModGameRules;
import net.potionseeker.fallingwaterfalls.init.FallingWaterfallsModMobEffects;
import net.potionseeker.fallingwaterfalls.init.FallingWaterfallsDamageSources;
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
    private boolean wasInWaterfall = false;
    private double lastY = 0.0;
    private boolean wasGrounded = false;

    public PlayerMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "travel(Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"))
    private void onTravel(Vec3 movementInput, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (player.isCreative() || player.isSpectator() || !player.level().getGameRules().getBoolean(FallingWaterfallsModGameRules.NO_FALL_SWIMMING)) {
            LOGGER.debug("Skipping waterfall logic for player {}: Creative, Spectator, or game rule disabled", player.getName().getString());
            waterfallFallDistance = 0.0F;
            wasInWaterfall = false;
            wasGrounded = player.onGround();
            player.getPersistentData().remove("falling_waterfalls:waterfall_fall");
            lastY = player.getY();
            return;
        }

        if (player.hasEffect(FallingWaterfallsModMobEffects.SALMONS_STRIFE.get())) {
            LOGGER.debug("Skipping waterfall logic for player {}: Has Salmon's Strife effect", player.getName().getString());
            waterfallFallDistance = 0.0F;
            wasInWaterfall = false;
            wasGrounded = player.onGround();
            player.getPersistentData().remove("falling_waterfalls:waterfall_fall");
            lastY = player.getY();
            return;
        }

        // Check fluid state at multiple points in an expanded bounding box
        AABB boundingBox = player.getBoundingBox().expandTowards(0, 0.3, 0).expandTowards(0, -0.3, 0);
        boolean isInFlowingWater = false;
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (double y = boundingBox.minY - 0.3; y <= boundingBox.maxY + 0.3; y += 0.1) {
            for (double x = boundingBox.minX; x <= boundingBox.maxX; x += 0.1) {
                for (double z = boundingBox.minZ; z <= boundingBox.maxZ; z += 0.1) {
                    mutablePos.set(Math.floor(x), Math.floor(y), Math.floor(z));
                    FluidState fluidState = player.level().getFluidState(mutablePos);
                    if (fluidState.is(Fluids.FLOWING_WATER) && !fluidState.isSource()) {
                        isInFlowingWater = true;
                        break;
                    }
                }
                if (isInFlowingWater) break;
            }
            if (isInFlowingWater) break;
        }

        // Check if player is grounded or within 0.5 blocks above a solid block
        boolean isGrounded = player.onGround();
        if (!isGrounded) {
            for (double x = boundingBox.minX; x <= boundingBox.maxX; x += 0.1) {
                for (double z = boundingBox.minZ; z <= boundingBox.maxZ; z += 0.1) {
                    mutablePos.set(Math.floor(x), Math.floor(boundingBox.minY - 0.1), Math.floor(z));
                    if (!player.level().getBlockState(mutablePos).getCollisionShape(player.level(), mutablePos).isEmpty()) {
                        isGrounded = true;
                        break;
                    }
                    // Check up to 0.5 blocks above the ground
                    mutablePos.setY((int) Math.floor(boundingBox.minY - 0.7));
                    if (!player.level().getBlockState(mutablePos).getCollisionShape(player.level(), mutablePos).isEmpty()) {
                        isGrounded = true;
                        break;
                    }
                }
                if (isGrounded) break;
            }
        }

        // Check if player is in air (not in any fluid)
        boolean isInAir = !player.isInWater() && !isInFlowingWater;

        // Apply damage when transitioning to grounded state with downward motion
        if ((isInFlowingWater || (isInAir && wasInWaterfall)) && isGrounded && !wasGrounded && waterfallFallDistance > 3.0F && player.getDeltaMovement().y < 0) {
            if (!player.hasEffect(MobEffects.SLOW_FALLING)) {
                float damage = waterfallFallDistance - 3.0F; // 1 damage per block after 3 blocks
                if (damage > 0) {
                    LOGGER.debug("Player {} landed {} after falling {} blocks, velocityY: {}, attempting to apply {} damage",
                            player.getName().getString(), isInFlowingWater ? "in waterfall" : "in air after waterfall", waterfallFallDistance, player.getDeltaMovement().y, damage);
                    player.getPersistentData().putBoolean("falling_waterfalls:waterfall_fall", true); // Mark waterfall fall
                    player.hurt(FallingWaterfallsDamageSources.waterfallFall(player.level()), damage);
                    MinecraftForge.EVENT_BUS.post(new LivingFallEvent(player, damage, 1.0F));
                    LOGGER.debug("Applied {} damage to player {}", damage, player.getName().getString());
                }
            } else {
                LOGGER.debug("Player {} landed {} with Slow Falling, no damage applied",
                        player.getName().getString(), isInFlowingWater ? "in waterfall" : "in air after waterfall");
            }
            waterfallFallDistance = 0.0F;
            wasInWaterfall = false;
            player.getPersistentData().remove("falling_waterfalls:waterfall_fall");
        }

        if (isInFlowingWater) {
            LOGGER.debug("Player {} in flowing water, movementInput: {}, current motion: {}, grounded: {}, onGround: {}",
                    player.getName().getString(), movementInput, player.getDeltaMovement(), isGrounded, player.onGround());
            // Apply downward force based on grounded state
            double downwardForce = isGrounded ? -0.02 : -0.2;
            player.push(0, downwardForce, 0);
            LOGGER.debug("Player {} after downward force, new motion: {}",
                    player.getName().getString(), player.getDeltaMovement());

            // Track fall distance based on actual Y displacement, reset if swimming upward
            double currentY = player.getY();
            if (player.getDeltaMovement().y > 0) {
                waterfallFallDistance = 0.0F;
                LOGGER.debug("Player {} swimming upward, resetting fall distance", player.getName().getString());
            } else if (currentY < lastY && lastY - currentY > 0.01) {
                waterfallFallDistance += (float) (lastY - currentY);
                LOGGER.debug("Player {} fall distance in waterfall: {}, Y change: {}",
                        player.getName().getString(), waterfallFallDistance, lastY - currentY);
            }
            wasInWaterfall = true;
            lastY = currentY;
        } else if (isInAir && wasInWaterfall) {
            // Continue tracking fall distance in air after leaving waterfall
            LOGGER.debug("Player {} in air after waterfall, current motion: {}, grounded: {}, onGround: {}",
                    player.getName().getString(), player.getDeltaMovement(), isGrounded, player.onGround());
            double currentY = player.getY();
            if (currentY < lastY && lastY - currentY > 0.01) {
                waterfallFallDistance += (float) (lastY - currentY);
                LOGGER.debug("Player {} fall distance in air after waterfall: {}, Y change: {}",
                        player.getName().getString(), waterfallFallDistance, lastY - currentY);
            }
            lastY = currentY;
        } else {
            // Reset fall distance if in still water or other safe conditions
            if (!isInFlowingWater && !isInAir) {
                LOGGER.debug("Reset fall distance for player {}: In still water or safe condition", player.getName().getString());
                waterfallFallDistance = 0.0F;
                wasInWaterfall = false;
                player.getPersistentData().remove("falling_waterfalls:waterfall_fall");
                lastY = player.getY();
            }
        }
        wasGrounded = isGrounded;
    }
}