package dev.automobilityfix.duck;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface AutomobileDuck {
    @Nullable UUID automobilityfix$getOwner();

    void automobilityfix$setOwner(@Nullable UUID owner);

    /** Zeroes every speed/velocity value of the vehicle. */
    void automobilityfix$stopMotion();

    /** Moves all hitboxes of the vehicle to where they belong right now. */
    void automobilityfix$syncHitboxes();
}
