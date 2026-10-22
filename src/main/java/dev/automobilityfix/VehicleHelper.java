package dev.automobilityfix;

import dev.automobilityfix.compat.FtbTeamsCompat;
import dev.automobilityfix.compat.OpacCompat;
import dev.automobilityfix.duck.AutomobileDuck;
import io.github.foundationgames.automobility.entity.AutomobileEntity;
import io.github.foundationgames.automobility.entity.HitboxEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.UUID;

public final class VehicleHelper {
    private VehicleHelper() {
    }

    /** Resolves a vehicle from either the vehicle entity itself or one of its hitboxes. */
    public static @Nullable AutomobileEntity resolve(@Nullable Entity entity) {
        if (entity instanceof AutomobileEntity automobile) {
            return automobile;
        }
        if (entity instanceof HitboxEntity hitbox) {
            return hitbox.automobile();
        }
        return null;
    }

    public static @Nullable UUID owner(AutomobileEntity automobile) {
        return ((AutomobileDuck) automobile).automobilityfix$getOwner();
    }

    public static void setOwner(Entity entity, @Nullable Player player) {
        if (player != null && entity instanceof AutomobileEntity automobile) {
            ((AutomobileDuck) automobile).automobilityfix$setOwner(player.getUUID());
        }
    }

    /** Whether the player may drive, open or dismantle the vehicle. */
    public static boolean canUse(Player player, AutomobileEntity automobile) {
        if (!FixConfig.get(FixConfig.OWNER_PROTECTION)) {
            return true;
        }

        if (owner(automobile) == null) {
            return true;
        }
        if (FixConfig.get(FixConfig.OPS_BYPASS) && player.hasPermissions(2)) {
            return true;
        }

        return isOwnerOrTeam(player, automobile);
    }

    /**
     * Whether the player is the vehicle's owner or in the owner's team (FTB Teams / OPAC party).
     * Unlike {@link #canUse}, vehicles without an owner never match, so this is safe to use for bypassing claim protection.
     */
    public static boolean isOwnerOrTeam(Player player, AutomobileEntity automobile) {
        var owner = owner(automobile);
        if (owner == null) {
            return false;
        }
        if (owner.equals(player.getUUID())) {
            return true;
        }

        boolean members = FixConfig.get(FixConfig.TRUST_PARTY_MEMBERS);
        boolean allies = FixConfig.get(FixConfig.TRUST_ALLIES);

        if (AutomobilityFix.isFtbTeamsLoaded() && FtbTeamsCompat.isTrusted(owner, player.getUUID(), members, allies)) {
            return true;
        }
        if (AutomobilityFix.isOpacLoaded() && player.getServer() != null &&
                OpacCompat.isTrusted(player.getServer(), owner, player.getUUID(), members, allies)) {
            return true;
        }

        return false;
    }

    /** Claim mods (OPAC, FTB Chunks) should let the owner's team use the vehicle inside foreign claims. */
    public static boolean bypassesClaimProtection(@Nullable Entity actor, @Nullable Entity target) {
        if (!(actor instanceof Player player) || !FixConfig.get(FixConfig.CLAIM_ACCESS_FOR_OWNER_TEAM)) {
            return false;
        }

        var automobile = resolve(target);
        return automobile != null && isOwnerOrTeam(player, automobile);
    }

    public static void notifyNotOwner(Player player, AutomobileEntity automobile) {
        var owner = owner(automobile);
        Component name = Component.translatable("message.automobility_fix.unknown_owner");
        var server = player.getServer();
        if (owner != null && server != null && server.getProfileCache() != null) {
            var profile = server.getProfileCache().get(owner);
            if (profile.isPresent()) {
                name = Component.literal(profile.get().getName());
            }
        }

        player.displayClientMessage(Component.translatable("message.automobility_fix.vehicle_owned", name), true);
    }

    /** Called (server side) whenever a vehicle leaves the level for any reason. */
    public static void onAutomobileRemoved(AutomobileEntity automobile, Entity.RemovalReason reason) {
        if (!(automobile.level() instanceof ServerLevel level)) {
            return;
        }

        MenuTracker.closeViewers(automobile);

        var hitboxes = new ArrayList<>(automobile.hitboxes);
        if (hitboxes.isEmpty()) {
            return;
        }

        Runnable discard = () -> {
            for (var hitbox : hitboxes) {
                if (!hitbox.isRemoved()) {
                    hitbox.discard();
                }
            }
        };

        if (reason == Entity.RemovalReason.UNLOADED_TO_CHUNK || reason == Entity.RemovalReason.UNLOADED_WITH_PLAYER) {
            // Do not touch other entities while the chunk is being unloaded; hitboxes in still-loaded chunks
            // would otherwise remain as invisible, permanent obstacles.
            var server = level.getServer();
            server.tell(new TickTask(server.getTickCount(), discard));
        } else {
            discard.run();
        }
    }
}
