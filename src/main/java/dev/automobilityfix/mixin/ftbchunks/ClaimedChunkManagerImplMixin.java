package dev.automobilityfix.mixin.ftbchunks;

import dev.automobilityfix.FixConfig;
import dev.automobilityfix.VehicleHelper;
import dev.ftb.mods.ftbchunks.api.Protection;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkManagerImpl;
import io.github.foundationgames.automobility.entity.HitboxEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ClaimedChunkManagerImpl.class, remap = false)
public abstract class ClaimedChunkManagerImplMixin {
    @Shadow
    public abstract boolean shouldPreventInteraction(Entity actor, InteractionHand hand, BlockPos pos, Protection protection, Entity targetEntity);

    @Inject(method = "shouldPreventInteraction", at = @At("HEAD"), cancellable = true)
    private void automobilityfix$vehicleAccess(Entity actor, InteractionHand hand, BlockPos pos, Protection protection, Entity targetEntity,
                                               CallbackInfoReturnable<Boolean> cir) {
        if (targetEntity == null) {
            return;
        }

        // The vehicle's owner and their team may use it inside anyone's claim
        if (VehicleHelper.bypassesClaimProtection(actor, targetEntity)) {
            cir.setReturnValue(false);
            return;
        }

        // Everyone else is checked against the vehicle itself and the chunk it is really in, not against a hitbox
        if (targetEntity instanceof HitboxEntity hitbox && FixConfig.get(FixConfig.OPAC_HITBOX_AS_AUTOMOBILE)) {
            var automobile = hitbox.automobile();
            if (automobile != null) {
                cir.setReturnValue(this.shouldPreventInteraction(actor, hand, automobile.blockPosition(), protection, automobile));
            }
        }
    }
}
