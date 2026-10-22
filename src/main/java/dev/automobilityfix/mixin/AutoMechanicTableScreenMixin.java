package dev.automobilityfix.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.foundationgames.automobility.screen.AutoMechanicTableScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.Map;

@Mixin(AutoMechanicTableScreen.class)
public abstract class AutoMechanicTableScreenMixin {
    /**
     * The default categories (frames, engines, wheels) exist even without recipes. Hovering the recipe panel of an
     * empty category (recipes removed by a pack, or not synced because of recipe-related mods) crashed the client.
     */
    @WrapOperation(method = "getHoveredRecipe", at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object automobilityfix$emptyCategory(Map<?, ?> map, Object key, Operation<Object> original) {
        var result = original.call(map, key);
        return result != null ? result : List.of();
    }
}
