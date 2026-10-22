package dev.automobilityfix;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class FixConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue PHYSICS_SANITY;
    public static final ModConfigSpec.DoubleValue MAX_COLLISION_IMPULSE;
    public static final ModConfigSpec.DoubleValue MAX_ADDED_VELOCITY;
    public static final ModConfigSpec.BooleanValue GUARD_UNLOADED_CHUNKS;

    public static final ModConfigSpec.BooleanValue OWNER_PROTECTION;
    public static final ModConfigSpec.BooleanValue OPS_BYPASS;
    public static final ModConfigSpec.BooleanValue TRUST_PARTY_MEMBERS;
    public static final ModConfigSpec.BooleanValue TRUST_ALLIES;
    public static final ModConfigSpec.BooleanValue CLAIM_ACCESS_FOR_OWNER_TEAM;

    public static final ModConfigSpec.BooleanValue OPAC_DRIVER_IS_ACCESSOR;
    public static final ModConfigSpec.BooleanValue OPAC_HITBOX_AS_AUTOMOBILE;
    public static final ModConfigSpec.BooleanValue OPAC_BLOCK_VEHICLE_FOR_PASSENGERS;
    public static final ModConfigSpec.BooleanValue SYNC_SERVER_CORRECTIONS;

    static {
        var builder = new ModConfigSpec.Builder();

        builder.push("physics");
        PHYSICS_SANITY = builder
                .comment("Resets NaN/infinite physics values and clamps runaway velocities (fixes AABB errors and vehicles being flung).")
                .define("physicsSanity", true);
        MAX_COLLISION_IMPULSE = builder
                .comment("Maximum push (blocks per tick) that vehicle-to-vehicle collisions may accumulate.")
                .defineInRange("maxCollisionImpulse", 0.8, 0.05, 10.0);
        MAX_ADDED_VELOCITY = builder
                .comment("Hard limit (blocks per tick) for the extra velocity a vehicle can accumulate from knockback/collisions.")
                .defineInRange("maxAddedVelocity", 3.0, 0.1, 20.0);
        GUARD_UNLOADED_CHUNKS = builder
                .comment("Stops vehicles before they drive into unloaded (or, for autopilot/mob-driven vehicles, non-ticking) chunks instead of falling through the world.")
                .define("guardUnloadedChunks", true);
        builder.pop();

        builder.push("ownership");
        OWNER_PROTECTION = builder
                .comment("Vehicles remember the player who placed/assembled them. Only the owner (and their team, see below) can",
                        "get in, drive, open the trunk or dismantle them - everywhere, not only inside claims.",
                        "Vehicles placed before this mod was installed have no owner and stay usable by everyone.")
                .define("ownerProtection", true);
        OPS_BYPASS = builder
                .comment("Operators (permission level 2+) may use any vehicle.",
                        "Note: in singleplayer worlds opened to LAN with cheats, every player counts as an operator.")
                .define("opsBypass", false);
        TRUST_PARTY_MEMBERS = builder
                .comment("Members of the owner's team count as owners: FTB Teams party and/or Open Parties and Claims party.")
                .define("trustPartyMembers", true);
        TRUST_ALLIES = builder
                .comment("Allies of the owner's team (FTB Teams allies / OPAC allied parties) may use the vehicle too.")
                .define("trustAllies", false);
        CLAIM_ACCESS_FOR_OWNER_TEAM = builder
                .comment("The owner and their team may use their vehicle inside anyone's claims (Open Parties and Claims, FTB Chunks).",
                        "Other claim rules are not affected. Vehicles without an owner always follow the claim rules.")
                .define("ownerTeamIgnoresClaims", true);
        builder.pop();

        builder.push("opac");
        OPAC_DRIVER_IS_ACCESSOR = builder
                .comment("Treat the driver (like for boats), or otherwise the owner, as the one responsible for a vehicle in claims.")
                .define("driverIsClaimAccessor", true);
        OPAC_HITBOX_AS_AUTOMOBILE = builder
                .comment("Protection checks against a vehicle hitbox are performed against the vehicle itself (its type and its chunk).")
                .define("hitboxCountsAsAutomobile", true);
        OPAC_BLOCK_VEHICLE_FOR_PASSENGERS = builder
                .comment("If a claim barrier would throw a passenger out, stop the whole vehicle at the claim border instead.")
                .define("stopVehicleInsteadOfEjecting", true);
        SYNC_SERVER_CORRECTIONS = builder
                .comment("When the server moves a player-driven vehicle (claim barrier, teleport), send the correction to the driver.",
                        "Fixes the vehicle being duplicated on claim borders (one copy inside, one outside).")
                .define("syncServerCorrections", true);
        builder.pop();

        SPEC = builder.build();
    }

    private FixConfig() {
    }

    /** Reads a value without crashing when the config is not loaded yet (e.g. on a client before joining). */
    public static boolean get(ModConfigSpec.BooleanValue value) {
        try {
            return value.get();
        } catch (IllegalStateException e) {
            return value.getDefault();
        }
    }

    public static double get(ModConfigSpec.DoubleValue value) {
        try {
            return value.get();
        } catch (IllegalStateException e) {
            return value.getDefault();
        }
    }
}
