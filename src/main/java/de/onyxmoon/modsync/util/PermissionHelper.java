package de.onyxmoon.modsync.util;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;

/**
 * Helper class for permission checks.
 */
public final class PermissionHelper {

    /**
     * Permission node required to use ModSync commands.
     */
    public static final String MODSYNC_ADMIN = "modsync.admin";

    private PermissionHelper() {
        // Utility class
    }

    /**
     * Check if a player has the ModSync admin permission.
     * If not, sends an error message to the player.
     *
     * @param playerRef the player to check
     * @return true if the player has permission, false otherwise
     */
    public static boolean checkAdminPermission(PlayerRef playerRef) {
        if (!hasAdminPermission(playerRef)) {
            playerRef.sendMessage(Message.raw("You don't have permission to use this command.").color("red"));
            return false;
        }
        return true;
    }

    /**
     * Check if a player has the ModSync admin permission.
     *
     * @param playerRef the player to check
     * @return true if the player has permission
     */
    public static boolean hasAdminPermission(PlayerRef playerRef) {
        return PermissionsModule.get().hasPermission(playerRef.getUuid(), MODSYNC_ADMIN);
    }
}