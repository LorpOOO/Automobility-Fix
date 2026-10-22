package dev.automobilityfix.mixin;

import io.github.foundationgames.automobility.automobile.attachment.front.AutopilotFrontAttachment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(AutopilotFrontAttachment.class)
public abstract class AutopilotFrontAttachmentMixin {
    /** {@code Math.clamp(anim, 0, values().length)} allowed an index one past the end (render thread crash). */
    @ModifyArg(method = "getState", at = @At(value = "INVOKE", target = "Ljava/lang/Math;clamp(JII)I"), index = 2)
    private int automobilityfix$clampToLastState(int max) {
        return max - 1;
    }
}
