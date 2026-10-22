package dev.automobilityfix.compat;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;

import java.util.UUID;

/** Only loaded when FTB Teams is installed. */
public final class FtbTeamsCompat {
    private FtbTeamsCompat() {
    }

    public static boolean isTrusted(UUID owner, UUID player, boolean teamMembers, boolean allies) {
        var api = FTBTeamsAPI.api();
        if (api == null || !api.isManagerLoaded()) {
            return false;
        }

        var manager = api.getManager();
        if (teamMembers && manager.arePlayersInSameTeam(owner, player)) {
            return true;
        }

        if (allies) {
            return manager.getTeamForPlayerID(owner)
                    .map(team -> team.getRankForPlayer(player).isAllyOrBetter())
                    .orElse(false);
        }

        return false;
    }
}
