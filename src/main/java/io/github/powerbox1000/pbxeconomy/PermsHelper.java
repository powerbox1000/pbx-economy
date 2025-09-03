package io.github.powerbox1000.pbxeconomy;

import java.util.UUID;

import me.lucko.fabric.api.permissions.v0.Permissions;

public class PermsHelper {
    public static boolean hasOwnerPerms(UUID player, DataHandler.BusinessEntry business) {
        return isOwner(player, business) || Permissions.check(player, "pbxeconomy.business.admin").join();
    }

    public static boolean hasManagerPerms(UUID player, DataHandler.BusinessEntry business) {
        return isManager(player, business) || hasOwnerPerms(player, business);
    }

    public static boolean hasEmployeePerms(UUID player, DataHandler.BusinessEntry business) {
        return isEmployee(player, business) || hasManagerPerms(player, business);
    }

    public static boolean isOwner(UUID player, DataHandler.BusinessEntry business) {
        return business.owners.contains(player);
    }

    public static boolean isManager(UUID player, DataHandler.BusinessEntry business) {
        return business.managers.contains(player);
    }

    public static boolean isEmployee(UUID player, DataHandler.BusinessEntry business) {
        return business.employees.contains(player);
    }
}
