package dev.automobilityfix.mixin;

import dev.automobilityfix.duck.DisplacementDuck;
import io.github.foundationgames.automobility.entity.AutomobileEntity;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/**
 * The suspension simulation normalizes a vector that can be zero, producing NaN rotations. Those propagate into the
 * renderer (vehicle disappears / is drawn broken) and into hitbox positions (invalid bounding boxes).
 */
@Mixin(AutomobileEntity.Displacement.class)
public abstract class DisplacementMixin implements DisplacementDuck {
    @Shadow private double lastVertical;
    @Shadow private double currVertical;
    @Shadow private double verticalTarget;
    @Shadow @Final private Quaternionf lastAngular;
    @Shadow @Final private Quaternionf currAngular;
    @Shadow @Final private Quaternionf angularTarget;

    @Override
    public void automobilityfix$sanitize(double fallbackY) {
        automobilityfix$fix(this.lastAngular);
        automobilityfix$fix(this.currAngular);
        automobilityfix$fix(this.angularTarget);

        if (!Double.isFinite(this.verticalTarget)) this.verticalTarget = fallbackY;
        if (!Double.isFinite(this.currVertical)) this.currVertical = fallbackY;
        if (!Double.isFinite(this.lastVertical)) this.lastVertical = this.currVertical;
    }

    @Unique
    private static void automobilityfix$fix(Quaternionf q) {
        if (!Float.isFinite(q.x) || !Float.isFinite(q.y) || !Float.isFinite(q.z) || !Float.isFinite(q.w) ||
                (q.x == 0 && q.y == 0 && q.z == 0 && q.w == 0)) {
            q.identity();
        }
    }
}
