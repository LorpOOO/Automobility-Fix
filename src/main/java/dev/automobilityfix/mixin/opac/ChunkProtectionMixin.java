package dev.automobilityfix.mixin.opac;

import dev.automobilityfix.FixConfig;
import dev.automobilityfix.VehicleHelper;
import io.github.foundationgames.automobility.entity.AutomobileEntity;
import io.github.foundationgames.automobility.entity.HitboxEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.pac.common.claims.player.IPlayerChunkClaim;
import xaero.pac.common.server.IServerData;
import xaero.pac.common.server.claims.protection.ChunkProtection;
import xaero.pac.common.server.config.ServerConfig;
import xaero.pac.common.server.player.config.IPlayerConfig;
import xaero.pac.common.server.player.config.IPlayerConfigManager;

@SuppressWarnings({"rawtypes", "unchecked"})
@Mixin(value = ChunkProtection.class, remap = false)
public abstract class ChunkProtectionMixin {
    @Shadow private boolean ignoreChunkEnter;

    @Shadow
    private Object getAccessorInfo(Entity entity) {
        throw new AssertionError();
    }

    @Shadow
    private boolean shouldPreventEntityChunkEntry(IServerData serverData, IPlayerConfigManager playerConfigs, IPlayerChunkClaim toClaim, IPlayerChunkClaim fromClaim,
                                                  IPlayerConfig config, IPlayerConfig fromConfig, Entity entity, ChunkPos newChunk, ChunkPos oldChunk) {
        throw new AssertionError();
    }

    /**
     * Hitboxes follow their vehicle; barrier-teleporting them separately only desynced them.
     * If a passenger would be thrown out by a claim barrier, the vehicle is stopped at the border instead.
     */
    @Inject(method = "onEntityEnterChunk", at = @At("HEAD"), cancellable = true)
    private void automobilityfix$handleVehicles(IServerData serverData, Entity entity, double goodX, double goodZ, ChunkPos newChunk, ChunkPos oldChunk, CallbackInfo ci) {
        if (entity instanceof HitboxEntity) {
            ci.cancel();
            return;
        }

        if (!(entity instanceof AutomobileEntity automobile) || this.ignoreChunkEnter ||
                !FixConfig.get(FixConfig.OPAC_BLOCK_VEHICLE_FOR_PASSENGERS) || !ServerConfig.CONFIG.claimsEnabled.get()) {
            return;
        }

        var playerConfigs = (IPlayerConfigManager) serverData.getPlayerConfigManager();
        for (var passenger : automobile.getPassengers()) {
            if (passenger instanceof Player &&
                    this.shouldPreventEntityChunkEntry(serverData, playerConfigs, null, null, null, null, passenger, newChunk, oldChunk)) {
                double x = Mth.clamp(goodX, oldChunk.getMinBlockX() + 0.3, oldChunk.getMaxBlockX() + 0.7);
                double z = Mth.clamp(goodZ, oldChunk.getMinBlockZ() + 0.3, oldChunk.getMaxBlockZ() + 0.7);

                this.ignoreChunkEnter = true;
                try {
                    // EntityMixin sends the correction to the driver and stops the vehicle
                    automobile.moveTo(x, automobile.getY(), z, automobile.getYRot(), automobile.getXRot());
                } finally {
                    this.ignoreChunkEnter = false;
                }

                ci.cancel();
                return;
            }
        }
    }

    /** The driver (like for boats), or otherwise the owner, is responsible for what a vehicle does in claims. */
    @Inject(method = "getAccessorInfo", at = @At("HEAD"), cancellable = true)
    private void automobilityfix$vehicleAccessor(Entity entity, CallbackInfoReturnable<Object> cir) {
        if (!(entity instanceof AutomobileEntity || entity instanceof HitboxEntity)) {
            return;
        }

        var automobile = VehicleHelper.resolve(entity);
        if (automobile == null) {
            return;
        }

        if (FixConfig.get(FixConfig.OPAC_DRIVER_IS_ACCESSOR) && automobile.getControllingPassenger() instanceof Player driver) {
            cir.setReturnValue(driver);
        } else if (entity != automobile) {
            cir.setReturnValue(this.getAccessorInfo(automobile));
        }
    }

    /** The vehicle's owner and their team may use it inside anyone's claim. */
    @Inject(method = "onEntityInteraction(Lxaero/pac/common/server/IServerData;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;ZZZ)Z",
            at = @At("HEAD"), cancellable = true)
    private void automobilityfix$ownerTeamAccess(IServerData serverData, Entity interactingEntityIndirect, Entity interactingEntity, Entity target,
                                                 ItemStack heldItem, InteractionHand hand, boolean attack, boolean messages, boolean targetExceptions,
                                                 CallbackInfoReturnable<Boolean> cir) {
        var actor = interactingEntityIndirect != null ? interactingEntityIndirect : interactingEntity;
        if (VehicleHelper.bypassesClaimProtection(actor, target)) {
            cir.setReturnValue(false);
        }
    }

    /** Protection checks against a hitbox use the vehicle itself: its entity type and the chunk it is really in. */
    @ModifyVariable(method = "onEntityInteraction(Lxaero/pac/common/server/IServerData;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;ZZZ)Z",
            at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private Entity automobilityfix$hitboxAsAutomobile(Entity target) {
        if (target instanceof HitboxEntity hitbox && FixConfig.get(FixConfig.OPAC_HITBOX_AS_AUTOMOBILE)) {
            var automobile = hitbox.automobile();
            if (automobile != null) {
                return automobile;
            }
        }
        return target;
    }
}
