package dev.automobilityfix;

import io.github.foundationgames.automobility.entity.AutomobileEntity;
import io.github.foundationgames.automobility.entity.HitboxEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class FixEvents {
    private FixEvents() {
    }

    /**
     * Hitboxes are rebuilt by their vehicle, so ones stored in old chunk data (saved before this mod was installed)
     * no longer belong to anything. They used to stay around as invisible obstacles in non-ticking chunks.
     */
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() && event.getEntity() instanceof HitboxEntity && !event.getLevel().isClientSide()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MenuTracker.tick(player);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (denyAccess(event.getEntity(), event.getTarget())) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (denyAccess(event.getEntity(), event.getTarget())) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (denyAccess(event.getEntity(), event.getTarget())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMount(EntityMountEvent event) {
        if (event.isMounting() && event.getEntityBeingMounted() instanceof AutomobileEntity &&
                event.getEntityMounting() instanceof Player player && denyAccess(player, event.getEntityBeingMounted())) {
            event.setCanceled(true);
        }
    }

    private static boolean denyAccess(Player player, Entity target) {
        if (player.level().isClientSide() || !(target instanceof AutomobileEntity || target instanceof HitboxEntity)) {
            return false;
        }

        var automobile = VehicleHelper.resolve(target);
        if (automobile == null || VehicleHelper.canUse(player, automobile)) {
            return false;
        }

        VehicleHelper.notifyNotOwner(player, automobile);
        return true;
    }
}
