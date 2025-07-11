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
    private boolean wasInFlowingWater = false;
    private double lastY = 0.0;
    private boolean wasGrounded = false;

    public PlayerMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "travel(Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"))
    private void onTravel(Vec3 movementInput, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (player.isCreative() || player.isSpectator() || !player.level().getGameRules().getBoolean(FallingWaterfallsModGameRules.NO_FALL_SWIMMING)) {
            LOGGER.debug("Skipping water logic for player {}: Creative, Spectator, or game rule noFallSwimming disabled", player.getName().getString());
            waterfallFallDistance = 0.0F;
            wasInFlowingWater = false;
            wasGrounded = player.onGround();
            player.getPersistentData().remove("falling_waterfalls:waterfall_fall");
            player.getPersistentData().remove("falling_waterfalls:fall_distance");
            lastY = player.getY();
            return;
        }

        if (player.hasEffect(FallingWaterfallsModMobEffects.SALMONS_STRIFE.get())) {
            LOGGER.debug("Skipping water logic for player {}: Has Salmon's Strife effect", player.getName().getString());
            waterfallFallDistance = 0.0F;
            wasInFlowingWater = false;
            wasGrounded = player.onGround();
            player.getPersistentData().remove("falling_waterfalls:waterfall_fall");
            player.getPersistentData().remove("falling_waterfalls:fall_distance");
            lastY = player.getY();
            return;
        }

        // Load persistent fall distance
        if (player.getPersistentData().contains("falling_waterfalls:fall_distance")) {
            waterfallFallDistance = player.getPersistentData().getFloat("falling_waterfalls:fall_distance");
        }

        // Check fluid state at multiple points in an expanded bounding box
        AABB boundingBox = player.getBoundingBox().expandTowards(0, 0.3, 0).expandTowards(0, -0.3, 0);
        boolean isInWater = false;
        boolean isInSourceWater = false;
        boolean isInFlowingWater = false;
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (double y = boundingBox.minY - 0.3; y <= boundingBox.maxY + 0.3; y += 0.1) {
            for (double x = boundingBox.minX; x <= boundingBox.maxX; x += 0.1) {
                for (double z = boundingBox.minZ; z <= boundingBox.maxZ; z += 0.1) {
                    mutablePos.set(Math.floor(x), Math.floor(y), Math.floor(z));
                    FluidState fluidState = player.level().getFluidState(mutablePos);
                    if (fluidState.is(Fluids.WATER)) {
                        isInWater = true;
                        isInSourceWater = true;
                        LOGGER.debug("Player {} detected in source water at {}", player.getName().getString(), mutablePos);
                        break;
                    }
                    if (fluidState.is(Fluids.FLOWING_WATER)) {
                        isInWater = true;
                        isInFlowingWater = true;
                        LOGGER.debug("Player {} detected in flowing water at {}", player.getName().getString(), mutablePos);
                        break;
                    }
                }
                if (isInWater) break;
            }
            if (isInWater) break;
        }

        // Check for source water at feet (up to 1.0 blocks below), only if player is in water
        boolean isFeetInSourceWater = false;
        double topWaterY = Double.NEGATIVE_INFINITY;
        if (player.isInWater() && !isInSourceWater) {
            for (double y = boundingBox.minY - 0.01; y >= boundingBox.minY - 1.0; y -= 0.1) {
                for (double x = boundingBox.minX; x <= boundingBox.maxX; x += 0.1) {
                    for (double z = boundingBox.minZ; z <= boundingBox.maxZ; z += 0.1) {
                        mutablePos.set(Math.floor(x), Math.floor(y), Math.floor(z));
                        FluidState fluidState = player.level().getFluidState(mutablePos);
                        if (fluidState.is(Fluids.WATER)) {
                            isFeetInSourceWater = true;
                            topWaterY = Math.max(topWaterY, Math.floor(y));
                            LOGGER.debug("Player {} feet in source water at {}, topWaterY={}", player.getName().getString(), mutablePos, topWaterY);
                            break;
                        }
                    }
                    if (isFeetInSourceWater) break;
                }
                if (isFeetInSourceWater) break;
            }
        }

        // Find the highest source water block in the player's column
        if (isInSourceWater || isFeetInSourceWater) {
            mutablePos.set(Math.floor(player.getX()), Math.floor(boundingBox.maxY + 0.3), Math.floor(player.getZ()));
            for (int y = mutablePos.getY(); y >= boundingBox.minY - 1.0; y--) {
                mutablePos.setY(y);
                FluidState fluidState = player.level().getFluidState(mutablePos);
                if (fluidState.is(Fluids.WATER)) {
                    topWaterY = Math.max(topWaterY, y);
                    LOGGER.debug("Player {} found source water in column at {}, topWaterY={}", player.getName().getString(), mutablePos, topWaterY);
                    break;
                }
            }
        }

        // Check if player is grounded (different leeway for source and flowing water)
        boolean isGrounded = player.onGround();
        if (!isGrounded) {
            for (double x = boundingBox.minX; x <= boundingBox.maxX; x += 0.1) {
                for (double z = boundingBox.minZ; z <= boundingBox.maxZ; z += 0.1) {
                    if (isInSourceWater || isFeetInSourceWater) {
                        mutablePos.set(Math.floor(x), Math.floor(boundingBox.minY - 0.1), Math.floor(z));
                        if (!player.level().getBlockState(mutablePos).getCollisionShape(player.level(), mutablePos).isEmpty()) {
                            isGrounded = true;
                            LOGGER.debug("Player {} grounded in source water at block {}", player.getName().getString(), mutablePos);
                            break;
                        }
                    }
                    if (isInFlowingWater) {
                        mutablePos.set(Math.floor(x), Math.floor(boundingBox.minY - 0.1), Math.floor(z));
                        if (!player.level().getBlockState(mutablePos).getCollisionShape(player.level(), mutablePos).isEmpty()) {
                            isGrounded = true;
                            LOGGER.debug("Player {} grounded in flowing water at block {} (0.1 check)", player.getName().getString(), mutablePos);
                            break;
                        }
                        mutablePos.setY((int) Math.floor(boundingBox.minY - 0.7));
                        if (!player.level().getBlockState(mutablePos).getCollisionShape(player.level(), mutablePos).isEmpty()) {
                            isGrounded = true;
                            LOGGER.debug("Player {} grounded in flowing water at block {} (0.7 check)", player.getName().getString(), mutablePos);
                            break;
                        }
                    }
                }
                if (isGrounded) break;
            }
        }

        // Check for adjacent solid blocks at water surface level (to allow climbing out in source water)
        boolean hasAdjacentBlock = false;
        if (isInSourceWater || isFeetInSourceWater) {
            double playerY = player.getY();
            for (double y = playerY; y <= playerY + 1.0; y += 0.1) {
                int blockY = (int) Math.floor(y);
                for (int[] offset : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    mutablePos.set(Math.floor(player.getX()) + offset[0], blockY, Math.floor(player.getZ()) + offset[1]);
                    if (!player.level().getBlockState(mutablePos).getCollisionShape(player.level(), mutablePos).isEmpty()) {
                        hasAdjacentBlock = true;
                        LOGGER.debug("Player {} found adjacent block at {}", player.getName().getString(), mutablePos);
                        break;
                    }
                }
                if (hasAdjacentBlock) break;
            }
        }

        // Check if player is in air (not in any fluid)
        boolean isInAir = !player.isInWater() && !isInWater && !isFeetInSourceWater;

        // Apply height restriction for source water when in water with air above
        if ((isInSourceWater || isFeetInSourceWater) && !isGrounded && !hasAdjacentBlock && topWaterY != Double.NEGATIVE_INFINITY) {
            mutablePos.set(Math.floor(player.getX()), Math.floor(player.getY() + player.getEyeHeight()), Math.floor(player.getZ()));
            if (player.level().getBlockState(mutablePos).isAir()) {
                double waterSurfaceY = topWaterY + 1.0 - player.getEyeHeight(); // Eyes at blockY + 1.0
                if (player.getY() >= waterSurfaceY - 0.3) {
                    // Proportional downward force
                    double constantForce = -0.01;
                    double proportionalForce = -0.08 * (player.getY() - waterSurfaceY);
                    double totalForce = Math.max(constantForce + proportionalForce, -0.15); // Cap at -0.15
                    player.push(0, totalForce, 0);
                    LOGGER.debug("Player {} in source water, applying downward force: constant={}, proportional={}, total={}, Y={}, targetY={}, topWaterY={}",
                            player.getName().getString(), constantForce, proportionalForce, totalForce, player.getY(), waterSurfaceY, topWaterY);

                    // Dampen upward velocity
                    double currentVelocityY = player.getDeltaMovement().y;
                    if (currentVelocityY > 0.03) {
                        player.setDeltaMovement(player.getDeltaMovement().x, 0.03, player.getDeltaMovement().z);
                        LOGGER.debug("Player {} in source water, damped velocityY from {} to 0.03", player.getName().getString(), currentVelocityY);
                    }
                }
            }
        }

        // Apply damage when transitioning to grounded state after significant waterfall fall
        if (isGrounded && !wasGrounded && waterfallFallDistance > 3.0F && wasInFlowingWater) {
            if (!player.level().getGameRules().getBoolean(FallingWaterfallsModGameRules.WATERFALL_DAMAGE)) {
                LOGGER.debug("Player {} landed after falling {} blocks, but waterfallDamage gamerule is disabled",
                        player.getName().getString(), waterfallFallDistance);
            } else if (!player.hasEffect(MobEffects.SLOW_FALLING)) {
                float damage = waterfallFallDistance - 3.0F; // 1 damage per block after 3 blocks
                if (damage > 0) {
                    LOGGER.debug("Player {} landed after falling {} blocks, wasInFlowingWater: {}, isInFlowingWater: {}, attempting to apply {} damage",
                            player.getName().getString(), waterfallFallDistance, wasInFlowingWater, isInFlowingWater, damage);
                    player.getPersistentData().putBoolean("falling_waterfalls:waterfall_fall", true); // Mark waterfall fall
                    player.hurt(FallingWaterfallsDamageSources.waterfallFall(player.level()), damage);
                    MinecraftForge.EVENT_BUS.post(new LivingFallEvent(player, damage, 1.0F));
                    LOGGER.debug("Applied {} damage to player {}", damage, player.getName().getString());
                }
            } else {
                LOGGER.debug("Player {} landed with Slow Falling, no damage applied, fall distance: {}", player.getName().getString(), waterfallFallDistance);
            }
            waterfallFallDistance = 0.0F;
            wasInFlowingWater = false;
            player.getPersistentData().remove("falling_waterfalls:waterfall_fall");
            player.getPersistentData().remove("falling_waterfalls:fall_distance");
        }

        if (isInFlowingWater) {
            LOGGER.debug("Player {} in flowing water, movementInput: {}, current motion: {}, grounded: {}, onGround: {}",
                    player.getName().getString(), movementInput, player.getDeltaMovement(), isGrounded, player.onGround());
            // Apply downward force based on grounded state
            double downwardForce = isGrounded ? -0.02 : -0.2;
            player.push(0, downwardForce, 0);
            LOGGER.debug("Player {} after downward force in flowing water, new motion: {}",
                    player.getName().getString(), player.getDeltaMovement());

            // Track fall distance based on actual Y displacement, reset if swimming upward
            double currentY = player.getY();
            if (player.getDeltaMovement().y > 0) {
                waterfallFallDistance = 0.0F;
                player.getPersistentData().remove("falling_waterfalls:fall_distance");
                LOGGER.debug("Player {} swimming upward in flowing water, resetting fall distance", player.getName().getString());
            } else if (currentY < lastY && lastY - currentY > 0.01) {
                waterfallFallDistance += (float) (lastY - currentY);
                player.getPersistentData().putFloat("falling_waterfalls:fall_distance", waterfallFallDistance);
                LOGGER.debug("Player {} fall distance in flowing water: {}, Y change: {}",
                        player.getName().getString(), waterfallFallDistance, lastY - currentY);
            }
            wasInFlowingWater = true;
            lastY = currentY;
        } else if (isInAir && wasInFlowingWater) {
            // Continue tracking fall distance in air after leaving flowing water
            LOGGER.debug("Player {} in air after flowing water, current motion: {}, grounded: {}, onGround: {}",
                    player.getName().getString(), player.getDeltaMovement(), isGrounded, player.onGround());
            double currentY = player.getY();
            if (currentY < lastY && lastY - currentY > 0.01) {
                waterfallFallDistance += (float) (lastY - currentY);
                player.getPersistentData().putFloat("falling_waterfalls:fall_distance", waterfallFallDistance);
                LOGGER.debug("Player {} fall distance in air after flowing water: {}, Y change: {}",
                        player.getName().getString(), waterfallFallDistance, lastY - currentY);
            }
            // Reset wasInFlowingWater if fall distance is too small to prevent erroneous damage
            if (waterfallFallDistance < 1.0F) {
                wasInFlowingWater = false;
                player.getPersistentData().remove("falling_waterfalls:fall_distance");
                LOGGER.debug("Player {} in air with small fall distance ({}), resetting wasInFlowingWater", player.getName().getString(), waterfallFallDistance);
            }
            lastY = currentY;
        } else {
            // Reset fall distance in source water or safe conditions
            if (!isInFlowingWater && !isInAir) {
                LOGGER.debug("Reset fall distance for player {}: In source water or safe condition", player.getName().getString());
                waterfallFallDistance = 0.0F;
                wasInFlowingWater = false;
                player.getPersistentData().remove("falling_waterfalls:waterfall_fall");
                player.getPersistentData().remove("falling_waterfalls:fall_distance");
                lastY = player.getY();
            }
        }
        wasGrounded = isGrounded;
    }
}