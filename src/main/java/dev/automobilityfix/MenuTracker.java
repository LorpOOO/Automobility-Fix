package dev.automobilityfix;

import io.github.foundationgames.automobility.entity.AutomobileEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Remembers which menus were opened from a vehicle attachment (trunk, banner post, crafting table...).
 * Automobility marks those menus as always valid, so a player could keep a trunk open while the vehicle
 * or the attachment was destroyed (and its items dropped) and then take the items a second time.
 */
public final class MenuTracker {
    private record Entry(WeakReference<AutomobileEntity> automobile, WeakReference<Object> attachment) {
    }

    private static final Map<AbstractContainerMenu, Entry> MENUS = new WeakHashMap<>();
    private static final double MAX_DISTANCE = 8;

    private MenuTracker() {
    }

    public static void track(Player player, AutomobileEntity automobile) {
        if (player instanceof ServerPlayer sp && sp.containerMenu != sp.inventoryMenu) {
            MENUS.put(sp.containerMenu, new Entry(new WeakReference<>(automobile), new WeakReference<>(automobile.getRearAttachment())));
        }
    }

    /** Closes every menu that was opened from this vehicle. Must be called before the attachment drops its items. */
    public static void closeViewers(AutomobileEntity automobile) {
        if (MENUS.isEmpty() || !(automobile.level() instanceof ServerLevel level)) {
            return;
        }

        for (var player : level.getServer().getPlayerList().getPlayers()) {
            var entry = MENUS.get(player.containerMenu);
            if (entry != null && entry.automobile().get() == automobile) {
                player.closeContainer();
            }
        }
    }

    public static void tick(ServerPlayer player) {
        if (MENUS.isEmpty() || player.containerMenu == player.inventoryMenu) {
            return;
        }

        var entry = MENUS.get(player.containerMenu);
        if (entry == null) {
            return;
        }

        var automobile = entry.automobile().get();
        boolean valid = automobile != null &&
                !automobile.isRemoved() &&
                automobile.level() == player.level() &&
                automobile.getRearAttachment() == entry.attachment().get();

        if (valid) {
            double range = MAX_DISTANCE + automobile.getBbWidth();
            valid = player.distanceToSqr(automobile) <= range * range;
        }

        if (!valid) {
            player.closeContainer();
        }
    }
}
