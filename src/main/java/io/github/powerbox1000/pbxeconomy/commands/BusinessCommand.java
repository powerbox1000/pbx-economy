package io.github.powerbox1000.pbxeconomy.commands;

import io.github.powerbox1000.pbxeconomy.DataHandler;
import io.github.powerbox1000.pbxeconomy.Economy;
import io.github.powerbox1000.pbxeconomy.PermsHelper;
import me.lucko.fabric.api.permissions.v0.Permissions;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import static net.minecraft.commands.Commands.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
// import net.minecraft.server.players.GameProfileCache;
import net.minecraft.server.players.PlayerList;

public class BusinessCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            literal("business")
                .then(
                    literal("create")
                    .requires(Permissions.require("pbxeconomy.command.business.create", 2))
                    .then(
                        argument("owner", EntityArgument.player())
                        .then(
                            argument("name", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                String name = StringArgumentType.getString(ctx, "name");
                                ServerPlayer player = EntityArgument.getPlayer(ctx, "owner");
                                final DataHandler.BusinessEntry business = Economy.DATA_HANDLER.createBusiness(name, player.getUUID());
                                if (business == null) {
                                    ctx.getSource().sendFailure(Component.literal("Business '" + name + "' already exists!"));
                                    return -1;
                                }
                                ctx.getSource().sendSuccess(() -> Component.literal("Created business '" + name + "' with owner " + player.getDisplayName().getString()), false);
                                return 1;
                            })
                        )
                    )
                )
                .then(
                    literal("remove")
                    .requires(Permissions.require("pbxeconomy.command.business.remove", 2))
                    .then(
                        argument("name", StringArgumentType.greedyString())
                        .suggests(new BusinessSuggestionProvider())
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "name");
                            final DataHandler.BusinessEntry business = Economy.DATA_HANDLER.getBusinessState(name);
                            if (business == null) {
                                ctx.getSource().sendFailure(Component.literal("Business '" + name + "' does not exist!"));
                                return -1;
                            }
                            Economy.DATA_HANDLER.businesses.remove(name);
                            ctx.getSource().sendSuccess(() -> Component.literal("Removed business '" + name + "'"), false);
                            return 1;
                        })
                    )
                )
                .then(
                    literal("info")
                    .requires(Permissions.require("pbxeconomy.command.business.info", 0))
                    .then(
                        argument("name", StringArgumentType.greedyString())
                        .suggests(new BusinessSuggestionProvider())
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "name");
                            final DataHandler.BusinessEntry business = Economy.DATA_HANDLER.getBusinessState(name);
                            if (business == null) {
                                ctx.getSource().sendFailure(Component.literal("Business '" + name + "' does not exist!"));
                                return -1;
                            }
                            StringBuilder builder = new StringBuilder();
                            builder.append("Business '").append(name).append("' info:\n")
                                   .append("- Owners: ").append(listToString(business.owners, ctx.getSource().getServer())).append("\n")
                                   .append("- Managers: ").append(listToString(business.managers, ctx.getSource().getServer())).append("\n");
                            if (PermsHelper.hasManagerPerms(ctx.getSource().getPlayerOrException().getUUID(), business)) {
                                builder.append("- Balance: $").append(business.cash).append("\n");
                            }
                            ctx.getSource().sendSuccess(() -> Component.literal(builder.toString()), false);
                            return 1;
                        })
                    )
                    .then(
                        literal("employees")
                        .then(
                            argument("name", StringArgumentType.greedyString())
                            .suggests(new BusinessSuggestionProvider())
                            .executes(ctx -> {
                                String name = StringArgumentType.getString(ctx, "name");
                                final DataHandler.BusinessEntry business = Economy.DATA_HANDLER.getBusinessState(name);
                                if (business == null) {
                                    ctx.getSource().sendFailure(Component.literal("Business '" + name + "' does not exist!"));
                                    return -1;
                                }
                                if (!PermsHelper.hasManagerPerms(ctx.getSource().getPlayerOrException().getUUID(), business)) {
                                    ctx.getSource().sendFailure(Component.literal("You do not have permission to view employees of this business."));
                                    return -1;
                                }
                                StringBuilder builder = new StringBuilder();
                                builder.append("Business '").append(name).append("' employees:\n")
                                       .append(listToString(business.employees, ctx.getSource().getServer()));
                                ctx.getSource().sendSuccess(() -> Component.literal(builder.toString()), false);
                                return 1;
                            })
                        )
                    )
                )
                .then(
                    literal("hire")
                    .then(
                        argument("name", StringArgumentType.string())
                        .suggests(new BusinessSuggestionProvider(false))
                        .then(
                            argument("player", EntityArgument.player())
                            .executes(ctx -> {
                                String name = StringArgumentType.getString(ctx, "name");
                                ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                                final DataHandler.BusinessEntry business = Economy.DATA_HANDLER.getBusinessState(name);
                                if (business == null) {
                                    ctx.getSource().sendFailure(Component.literal("Business '" + name + "' does not exist!"));
                                    return -1;
                                }
                                if (!PermsHelper.hasManagerPerms(ctx.getSource().getPlayerOrException().getUUID(), business)) {
                                    ctx.getSource().sendFailure(Component.literal("You do not have permission to hire employees for this business."));
                                    return -1;
                                }
                                if (business.employees.contains(player.getUUID())) {
                                    ctx.getSource().sendFailure(Component.literal("Player '" + player.getDisplayName().getString() + "' is already an employee of this business."));
                                    return -1;
                                }
                                business.employees.add(player.getUUID());
                                Economy.DATA_HANDLER.setDirty();
                                ctx.getSource().sendSuccess(() -> Component.literal("Hired " + player.getDisplayName().getString() + " to business '" + name + "'"), false);
                                return 1;
                            })
                        )
                    )
                )
                .then(
                    literal("fire")
                    .then(
                        argument("name", StringArgumentType.string())
                        .suggests(new BusinessSuggestionProvider(false))
                        .then(
                            argument("player", EntityArgument.player())
                            .suggests(new BusinessEmployeeSuggestionProvider())
                            .executes(ctx -> {
                                String name = StringArgumentType.getString(ctx, "name");
                                ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                                final DataHandler.BusinessEntry business = Economy.DATA_HANDLER.getBusinessState(name);
                                if (business == null) {
                                    ctx.getSource().sendFailure(Component.literal("Business '" + name + "' does not exist!"));
                                    return -1;
                                }
                                if (!PermsHelper.hasManagerPerms(ctx.getSource().getPlayerOrException().getUUID(), business)) {
                                    ctx.getSource().sendFailure(Component.literal("You do not have permission to fire employees for this business."));
                                    return -1;
                                }
                                if (!business.employees.contains(player.getUUID())) {
                                    ctx.getSource().sendFailure(Component.literal("Player '" + player.getDisplayName().getString() + "' is not an employee of this business."));
                                    return -1;
                                }
                                business.employees.remove(player.getUUID());
                                Economy.DATA_HANDLER.setDirty();
                                ctx.getSource().sendSuccess(() -> Component.literal("Fired " + player.getDisplayName().getString() + " from business '" + name + "'"), false);
                                return 1;
                            })
                        )
                    )
                )
                .then(
                    literal("manage")
                    .then(
                        literal("employee")
                        .then(
                            argument("name", StringArgumentType.string())
                            .suggests(new BusinessSuggestionProvider(false))
                            .then(
                                argument("player", EntityArgument.player())
                                .suggests(new BusinessEmployeeSuggestionProvider(new String[] {"employee", "manager", "owner"}))
                                .then(
                                    literal("setrole")
                                    .then(
                                        // Could also do literals but whatever
                                        argument("role", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            for (String role : new String[] {"employee", "manager", "owner"}) {
                                                builder.suggest(role);
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            final String role = StringArgumentType.getString(ctx, "role");
                                            if (!java.util.Arrays.asList("employee", "manager", "owner").contains(role)) {
                                                ctx.getSource().sendFailure(Component.literal("Invalid role '" + role + "'"));
                                                return -1;
                                            }
                                            final ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                                            final UUID playerId = player.getUUID();
                                            final DataHandler.BusinessEntry business = Economy.DATA_HANDLER.getBusinessState(StringArgumentType.getString(ctx, "name"));
                                            if (business != null) {
                                                business.setEmployeeRole(playerId, role);
                                                Economy.DATA_HANDLER.setDirty();
                                                ctx.getSource().sendSuccess(() -> Component.literal("Set role of " + player.getDisplayName().getString() + " to " + role), true);
                                            }
                                            return 1;
                                        })
                                    )
                                )
                            )
                        )
                    )
                )
        );
    }

    private static String listToString(List<UUID> list, MinecraftServer server) {
        if (list.size() == 0) {
            return "None";
        }

        StringBuilder builder = new StringBuilder();
        for (UUID obj : list) {
            // builder.append(server.getProfileCache().get(obj).orElseThrow().getName()).append(", ");
            builder.append(server.getPlayerList().getPlayer(obj).getScoreboardName()).append(", ");
        }
        
        // Remove the trailing comma and space
        if (builder.length() > 0) {
            builder.setLength(builder.length() - 2);
        }

        return builder.toString();
    }

    public static class BusinessSuggestionProvider implements SuggestionProvider<CommandSourceStack> {
        boolean greedy = true;

        public BusinessSuggestionProvider() {}
        public BusinessSuggestionProvider(boolean greedy) { this.greedy = greedy; }

        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
            for (String businessName : Economy.DATA_HANDLER.businesses.keySet()) {
                if (!greedy) businessName = '"' + businessName + '"';
                if (SharedSuggestionProvider.matchesSubStr(builder.getRemaining(), businessName)) builder.suggest(businessName);
            }
            return builder.buildFuture();
        }
    }

    public static class BusinessEmployeeSuggestionProvider implements SuggestionProvider<CommandSourceStack> {
        String arg = "name";
        String[] roles = {"employee"};

        public BusinessEmployeeSuggestionProvider() {}
        public BusinessEmployeeSuggestionProvider(String arg) { this.arg = arg; }
        public BusinessEmployeeSuggestionProvider(String[] roles) { this.roles = roles; }
        public BusinessEmployeeSuggestionProvider(String arg, String[] roles) {
            this.arg = arg;
            this.roles = roles;
        }

        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
            final DataHandler.BusinessEntry business = Economy.DATA_HANDLER.getBusinessState(StringArgumentType.getString(context, arg));
            if (business != null) {
                final PlayerList players = context.getSource().getServer().getPlayerList();
                
                if (java.util.Arrays.asList(roles).contains("employee")) {
                    for (UUID employeeId : business.employees) {
                        final String name = players.getPlayer(employeeId).getScoreboardName();
                        if (SharedSuggestionProvider.matchesSubStr(builder.getRemaining(), name)) builder.suggest(name);
                    }
                }

                if (java.util.Arrays.asList(roles).contains("manager")) {
                    for (UUID managerId : business.managers) {
                        final String name = players.getPlayer(managerId).getScoreboardName();
                        if (SharedSuggestionProvider.matchesSubStr(builder.getRemaining(), name)) builder.suggest(name);
                    }
                }

                if (java.util.Arrays.asList(roles).contains("owner")) {
                    for (UUID ownerId : business.owners) {
                        final String name = players.getPlayer(ownerId).getScoreboardName();
                        if (SharedSuggestionProvider.matchesSubStr(builder.getRemaining(), name)) builder.suggest(name);
                    }
                }
            }
            return builder.buildFuture();
        }
    }
}
