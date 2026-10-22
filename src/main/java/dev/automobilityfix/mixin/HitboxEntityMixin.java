package dev.automobilityfix.mixin;

import io.github.foundationgames.automobility.entity.AutomobileEntity;
import io.github.foundationgames.automobility.entity.HitboxEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HitboxEntity.class)
public abstract class HitboxEntityMixin {
    @Shadow
    public abstract AutomobileEntity automobile();

    /** A hitbox whose vehicle is gone (or not known to this client) must not act as an invisible wall. */
    @Inject(method = "canBeCollidedWith", at = @At("RETURN"), cancellable = true)
    private void automobilityfix$noOrphanCollision(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && this.automobilityfix$isOrphan()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isPickable", at = @At("RETURN"), cancellable = true)
    private void automobilityfix$noOrphanPicking(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && this.automobilityfix$isOrphan()) {
            cir.setReturnValue(false);
        }
    }

    private boolean automobilityfix$isOrphan() {
        var automobile = this.automobile();
        return automobile == null || automobile.isRemoved();
    }
}
