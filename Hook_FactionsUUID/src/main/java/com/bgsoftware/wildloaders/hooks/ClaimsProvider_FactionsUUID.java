package com.bgsoftware.wildloaders.hooks;

import com.bgsoftware.wildloaders.api.hooks.ClaimsProvider;
import com.massivecraft.factions.*;
import org.bukkit.Chunk;

import java.util.UUID;

public final class ClaimsProvider_FactionsUUID implements ClaimsProvider {

    @Override
    public boolean hasClaimAccess(UUID player, Chunk chunk) {
        FPlayer fPlayer = FPlayers.getInstance().getById(player.toString());
        FLocation fLocation = new FLocation(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        Faction faction = Board.getInstance().getFactionAt(fLocation);
        return !faction.isWilderness() && faction.getFPlayers().contains(fPlayer);
    }

}
