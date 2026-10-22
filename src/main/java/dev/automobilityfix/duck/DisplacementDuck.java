package dev.automobilityfix.duck;

public interface DisplacementDuck {
    /** Replaces NaN/infinite suspension state with sane values. */
    void automobilityfix$sanitize(double fallbackY);
}
