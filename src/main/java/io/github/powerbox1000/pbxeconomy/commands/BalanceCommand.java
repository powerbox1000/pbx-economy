package io.github.powerbox1000.pbxeconomy.commands;

import io.github.powerbox1000.pbxeconomy.DataHandler;
import io.github.powerbox1000.pbxeconomy.Economy;
import me.lucko.fabric.api.permissions.v0.Permissions;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;

import static net.minecraft.commands.Commands.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class BalanceCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        final LiteralCommandNode<CommandSourceStack> root = dispatcher.register(
            literal("balance")
                .requires(Permissions.require("pbxeconomy.command.balance", 0))
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    final int balance = Economy.DATA_HANDLER.getPlayerState(player).cash;
                    ctx.getSource().sendSuccess(() -> Component.literal("Your balance is $" + balance), false);
                    return balance;
                })
                .then(
                    argument("player", EntityArgument.player())
                    .requires(Permissions.require("pbxeconomy.command.balance.player", 2))
                    .executes(ctx -> {
                        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                        final int balance = Economy.DATA_HANDLER.getPlayerState(player).cash;
                        ctx.getSource().sendSuccess(() -> Component.literal(player.getDisplayName().getString() + "'s balance is $" + balance), false);
                        return balance;
                    })
                )
                .then(
                    literal("set")
                    .requires(Permissions.require("pbxeconomy.command.balance.set", 2))
                    .then(
                        argument("player", EntityArgument.player())
                        .then(
                            argument("amount", IntegerArgumentType.integer())
                            .executes(ctx -> {
                                ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                Economy.DATA_HANDLER.getPlayerState(target).cash = amount;
                                Economy.DATA_HANDLER.setDirty();
                                ctx.getSource().sendSuccess(() -> Component.literal("Set " + target.getDisplayName().getString() + "'s balance to $" + amount), false);
                                return amount;
                            })
                        )
                    )
                    .then(
                        literal("business")
                        .then(
                            argument("name", StringArgumentType.string())
                            .suggests(new BusinessCommand.BusinessSuggestionProvider(false))
                            .then(
                                argument("amount", IntegerArgumentType.integer())
                                .executes(ctx -> {
                                    String name = StringArgumentType.getString(ctx, "name");
                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                    final DataHandler.BusinessEntry business = Economy.DATA_HANDLER.getBusinessState(name);
                                    if (business == null) {
                                        ctx.getSource().sendFailure(Component.literal("Business '" + name + "' does not exist!"));
                                        return -1;
                                    }
                                    business.cash = amount;
                                    Economy.DATA_HANDLER.setDirty();
                                    ctx.getSource().sendSuccess(() -> Component.literal("Set " + business.name + "'s balance to $" + amount), false);
                                    return amount;
                                })
                            )
                        )
                    )
                )
                .then(
                    literal("add")
                    .requires(Permissions.require("pbxeconomy.command.balance.set", 2))
                    .then(
                        argument("player", EntityArgument.player())
                        .then(
                            argument("amount", IntegerArgumentType.integer())
                            .executes(ctx -> {
                                ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                Economy.DATA_HANDLER.getPlayerState(target).cash += amount;
                                Economy.DATA_HANDLER.setDirty();
                                ctx.getSource().sendSuccess(() -> Component.literal("Added $" + amount + " to " + target.getDisplayName().getString() + "'s balance"), false);
                                return amount;
                            })
                        )
                    )
                    .then(
                        literal("business")
                        .then(
                            argument("name", StringArgumentType.string())
                            .suggests(new BusinessCommand.BusinessSuggestionProvider())
                            .then(
                                argument("amount", IntegerArgumentType.integer())
                                .executes(ctx -> {
                                    String name = StringArgumentType.getString(ctx, "name");
                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                    final DataHandler.BusinessEntry business = Economy.DATA_HANDLER.getBusinessState(name);
                                    if (business == null) {
                                        ctx.getSource().sendFailure(Component.literal("Business '" + name + "' does not exist!"));
                                        return -1;
                                    }
                                    business.cash += amount;
                                    Economy.DATA_HANDLER.setDirty();
                                    ctx.getSource().sendSuccess(() -> Component.literal("Added $" + amount + " to " + business.name + "'s balance"), false);
                                    return amount;
                                })
                            )
                        )
                    )
                )
        );

        // Aliases
        dispatcher.register(literal("bal").redirect(root));
    }
}
