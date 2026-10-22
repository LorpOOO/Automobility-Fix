package dev.automobilityfix.mixin;

import dev.automobilityfix.FixConfig;
import dev.automobilityfix.VehicleHelper;
import dev.automobilityfix.duck.AutomobileDuck;
import io.github.foundationgames.automobility.entity.AutomobileEntity;
import io.github.foundationgames.automobility.entity.HitboxEntity;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    /**
     * Hitboxes are recreated by their vehicle on every load, but Automobility saved them to the chunk as well.
     * Loaded copies no longer knew their vehicle and lingered as "ghost" hitboxes.
     */
    @Inject(method = "shouldBeSaved", at = @At("HEAD"), cancellable = true)
    private void automobilityfix$doNotSaveHitboxes(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof HitboxEntity) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "setRemoved", at = @At("TAIL"))
    private void automobilityfix$cleanUpRemovedAutomobile(Entity.RemovalReason reason, CallbackInfo ci) {
        if ((Object) this instanceof AutomobileEntity automobile && !automobile.level().isClientSide()) {
            VehicleHelper.onAutomobileRemoved(automobile, reason);
        }
    }

    /**
     * A player-driven vehicle is simulated by the driver's client, which ignores normal position updates for it.
     * When the server moves it (Open Parties and Claims barrier, teleport...), the client kept driving its own copy:
     * the vehicle appeared twice and the hitboxes stayed with the server copy.
     */
    @Inject(method = "moveTo(DDDFF)V", at = @At("TAIL"))
    private void automobilityfix$syncServerCorrection(double x, double y, double z, float yRot, float xRot, CallbackInfo ci) {
        if ((Object) this instanceof AutomobileEntity automobile && !automobile.level().isClientSide() &&
                automobile.getControllingPassenger() instanceof ServerPlayer driver &&
                FixConfig.get(FixConfig.SYNC_SERVER_CORRECTIONS)) {
            var duck = (AutomobileDuck) automobile;
            duck.automobilityfix$stopMotion();
            duck.automobilityfix$syncHitboxes();
            driver.connection.send(new ClientboundMoveVehiclePacket(automobile));
        }
    }
}
