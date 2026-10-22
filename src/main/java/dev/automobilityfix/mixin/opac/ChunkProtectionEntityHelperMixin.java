package dev.automobilityfix.mixin.opac;

import dev.automobilityfix.VehicleHelper;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.pac.common.server.claims.protection.ChunkProtectionEntityHelper;

import java.util.UUID;

/** Lets Open Parties and Claims know who owns a vehicle (like tamed animals), so owners can always use their own vehicles. */
@Mixin(value = ChunkProtectionEntityHelper.class, remap = false)
public abstract class ChunkProtectionEntityHelperMixin {
    @Inject(method = "getOwnerId", at = @At("HEAD"), cancellable = true)
    private void automobilityfix$vehicleOwner(Entity entity, CallbackInfoReturnable<UUID> cir) {
        var automobile = VehicleHelper.resolve(entity);
        if (automobile != null) {
            cir.setReturnValue(VehicleHelper.owner(automobile));
        }
    }
}
