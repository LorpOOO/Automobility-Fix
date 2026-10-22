package dev.automobilityfix.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.automobilityfix.VehicleHelper;
import io.github.foundationgames.automobility.item.AutomobileItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AutomobileItem.class)
public abstract class AutomobileItemMixin {
    @WrapOperation(method = "useOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean automobilityfix$assignOwner(Level level, Entity entity, Operation<Boolean> original, @Local(argsOnly = true) UseOnContext context) {
        VehicleHelper.setOwner(entity, context.getPlayer());
        return original.call(level, entity);
    }
}
