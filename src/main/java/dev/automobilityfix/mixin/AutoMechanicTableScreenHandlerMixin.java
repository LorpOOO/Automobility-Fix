package dev.automobilityfix.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.foundationgames.automobility.screen.AutoMechanicTableScreenHandler;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AutoMechanicTableScreenHandler.class)
public abstract class AutoMechanicTableScreenHandlerMixin {
    @Shadow @Final public Slot outputSlot;

    /** The original compared a Container with a Slot (always true), so double-click collecting pulled from the output slot. */
    @Inject(method = "canTakeItemForPickAll", at = @At("HEAD"), cancellable = true)
    private void automobilityfix$excludeOutputFromPickAll(ItemStack stack, Slot slot, CallbackInfoReturnable<Boolean> cir) {
        if (slot == this.outputSlot) {
            cir.setReturnValue(false);
        }
    }

    /** Shift-clicking ingredients skipped the last (9th) input slot. */
    @WrapOperation(method = "quickMoveStack", at = @At(value = "INVOKE",
            target = "Lio/github/foundationgames/automobility/screen/AutoMechanicTableScreenHandler;moveItemStackTo(Lnet/minecraft/world/item/ItemStack;IIZ)Z"))
    private boolean automobilityfix$includeLastInputSlot(AutoMechanicTableScreenHandler self, ItemStack stack, int start, int end, boolean reverse, Operation<Boolean> original) {
        if (start == 0 && end == 8) {
            end = 9;
        }
        return original.call(self, stack, start, end, reverse);
    }
}
