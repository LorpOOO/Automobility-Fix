package dev.automobilityfix.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.automobilityfix.VehicleHelper;
import io.github.foundationgames.automobility.block.entity.AutomobileAssemblerBlockEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AutomobileAssemblerBlockEntity.class)
public abstract class AutomobileAssemblerBlockEntityMixin {
    @Unique
    private Player automobilityfix$interactingPlayer;

    @Inject(method = "interact", at = @At("HEAD"))
    private void automobilityfix$rememberPlayer(Player player, ItemStack stack, InteractionHand hand, CallbackInfoReturnable<ItemInteractionResult> cir) {
        this.automobilityfix$interactingPlayer = player;
    }

    @Inject(method = "interact", at = @At("RETURN"))
    private void automobilityfix$forgetPlayer(Player player, ItemStack stack, InteractionHand hand, CallbackInfoReturnable<ItemInteractionResult> cir) {
        this.automobilityfix$interactingPlayer = null;
    }

    /** The player who adds the last part becomes the owner of the assembled vehicle. */
    @WrapOperation(method = "tryConstructAutomobile", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean automobilityfix$assignOwner(Level level, Entity entity, Operation<Boolean> original) {
        VehicleHelper.setOwner(entity, this.automobilityfix$interactingPlayer);
        return original.call(level, entity);
    }
}
