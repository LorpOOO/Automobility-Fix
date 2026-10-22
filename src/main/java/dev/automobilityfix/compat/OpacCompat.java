package dev.automobilityfix.compat;

import net.minecraft.server.MinecraftServer;
import xaero.pac.common.server.api.OpenPACServerAPI;

import java.util.UUID;

/** Only loaded when Open Parties and Claims is installed. */
public final class OpacCompat {
    private OpacCompat() {
    }

    public static boolean isTrusted(MinecraftServer server, UUID owner, UUID player, boolean partyMembers, boolean allies) {
        if (!partyMembers && !allies) {
            return false;
        }

        var parties = OpenPACServerAPI.get(server).getPartyManager();
        var ownerParty = parties.getPartyByMember(owner);
        if (ownerParty == null) {
            return false;
        }

        if (partyMembers && ownerParty.getMemberInfo(player) != null) {
            return true;
        }

        if (allies) {
            var playerParty = parties.getPartyByMember(player);
            return playerParty != null && ownerParty.isAlly(playerParty.getId());
        }

        return false;
    }
}
