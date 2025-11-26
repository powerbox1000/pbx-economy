package io.github.powerbox1000.pbxeconomy.commands;

import io.github.powerbox1000.pbxeconomy.DataHandler;
import io.github.powerbox1000.pbxeconomy.Economy;
import io.github.powerbox1000.pbxeconomy.PermsHelper;
import me.lucko.fabric.api.permissions.v0.Permissions;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

import static net.minecraft.commands.Commands.*;

import java.util.UUID;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class PayCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            literal("pay")
                .requires(Permissions.require("pbxeconomy.command.pay", 0))
                .then(
                    argument("player", EntityArgument.player())
                        .then(
                            argument("amount", IntegerArgumentType.integer(1))
                                .executes(ctx -> {
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    
                                    if (Economy.DATA_HANDLER.getPlayerState(player).cash < amount) {
                                        ctx.getSource().sendFailure(Component.literal("You do not have enough money to pay this amount."));
                                        return 0;
                                    }
                                    
                                    Economy.DATA_HANDLER.getPlayerState(player).cash -= amount;
                                    Economy.DATA_HANDLER.getPlayerState(target).cash += amount;
                                    Economy.DATA_HANDLER.setDirty();

                                    target.sendSystemMessage(Component.literal("You have received $" + amount + " from " + player.getDisplayName().getString()));

                                    ctx.getSource().sendSuccess(() -> Component.literal("Paid $" + amount + " to " + target.getDisplayName().getString()), false);
                                    return 1;
                                })
                        )
                )
                .then(
                    literal("business")
                        .then(
                            argument("name", StringArgumentType.string())
                                .suggests(new BusinessCommand.BusinessSuggestionProvider(false))
                                .then(
                                    argument("amount", IntegerArgumentType.integer(1))
                                        .executes(ctx -> {
                                            String name = StringArgumentType.getString(ctx, "name");
                                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();

                                            DataHandler.BusinessEntry business = Economy.DATA_HANDLER.getBusinessState(name);
                                            if (business == null) {
                                                ctx.getSource().sendFailure(Component.literal("Business not found."));
                                                return 0;
                                            }

                                            if (Economy.DATA_HANDLER.getPlayerState(player).cash < amount) {
                                                ctx.getSource().sendFailure(Component.literal("You do not have enough money to pay this amount."));
                                                return 0;
                                            }

                                            Economy.DATA_HANDLER.getPlayerState(player).cash -= amount;
                                            business.cash += amount;
                                            Economy.DATA_HANDLER.setDirty();

                                            for (UUID owner : business.owners) {
                                                ServerPlayer p = ctx.getSource().getServer().getPlayerList().getPlayer(owner);
                                                if (p != null) {
                                                    p.sendSystemMessage(Component.literal(player.getDisplayName().getString() + " has paid $" + amount + " to your business " + name));
                                                }
                                            }
                                            for (UUID manager : business.managers) {
                                                ServerPlayer p = ctx.getSource().getServer().getPlayerList().getPlayer(manager);
                                                if (p != null) {
                                                    p.sendSystemMessage(Component.literal(player.getDisplayName().getString() + " has paid $" + amount + " to your business " + name));
                                                }
                                            }

                                            ctx.getSource().sendSuccess(() -> Component.literal("Paid $" + amount + " to business " + name), false);
                                            return 1;
                                        })
                                )
                        )
                )
                .then(
                    literal("asbusiness")
                    .then(
                        argument("name", StringArgumentType.string())
                        .suggests(new BusinessCommand.BusinessSuggestionProvider(false))
                        .then(
                            argument("player", EntityArgument.player())
                            .then(
                                argument("amount", IntegerArgumentType.integer(1))
                                .executes(ctx -> {
                                    String name = StringArgumentType.getString(ctx, "name");
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");

                                    DataHandler.BusinessEntry business = Economy.DATA_HANDLER.getBusinessState(name);
                                    if (business == null) {
                                        ctx.getSource().sendFailure(Component.literal("Business not found."));
                                        return 0;
                                    }

                                    if (!PermsHelper.hasEmployeePerms(ctx.getSource().getPlayerOrException().getUUID(), business)) {
                                        ctx.getSource().sendFailure(Component.literal("You do not have permission to pay from this business."));
                                        return 0;
                                    }

                                    if (business.cash < amount) {
                                        ctx.getSource().sendFailure(Component.literal("Business does not have enough money to pay this amount."));
                                        return 0;
                                    }

                                    business.cash -= amount;
                                    Economy.DATA_HANDLER.getPlayerState(target).cash += amount;
                                    Economy.DATA_HANDLER.setDirty();

                                    target.sendSystemMessage(Component.literal("You have received $" + amount + " from business " + name));

                                    ctx.getSource().sendSuccess(() -> Component.literal("Paid $" + amount + " to " + target.getDisplayName().getString() + " from business " + name), false);
                                    return 1;
                                })
                            )
                        )
                        .then(
                            literal("business")
                            .then(
                                argument("target", StringArgumentType.string())
                                .suggests(new BusinessCommand.BusinessSuggestionProvider(false))
                                .then(
                                    argument("amount", IntegerArgumentType.integer(1))
                                    .executes(ctx -> {
                                        String name = StringArgumentType.getString(ctx, "name");
                                        String targetName = StringArgumentType.getString(ctx, "target");
                                        int amount = IntegerArgumentType.getInteger(ctx, "amount");

                                        DataHandler.BusinessEntry business = Economy.DATA_HANDLER.getBusinessState(name);
                                        if (business == null) {
                                            ctx.getSource().sendFailure(Component.literal("Business not found."));
                                            return 0;
                                        }

                                        DataHandler.BusinessEntry targetBusiness = Economy.DATA_HANDLER.getBusinessState(targetName);
                                        if (targetBusiness == null) {
                                            ctx.getSource().sendFailure(Component.literal("Target business not found."));
                                            return 0;
                                        }

                                        if (!PermsHelper.hasEmployeePerms(ctx.getSource().getPlayerOrException().getUUID(), business)) {
                                            ctx.getSource().sendFailure(Component.literal("You do not have permission to pay from this business."));
                                            return 0;
                                        }

                                        if (business.cash < amount) {
                                            ctx.getSource().sendFailure(Component.literal("Business does not have enough money to pay this amount."));
                                            return 0;
                                        }

                                        business.cash -= amount;
                                        targetBusiness.cash += amount;
                                        Economy.DATA_HANDLER.setDirty();

                                        for (UUID owner : targetBusiness.owners) {
                                            ServerPlayer p = ctx.getSource().getServer().getPlayerList().getPlayer(owner);
                                            if (p != null) {
                                                p.sendSystemMessage(Component.literal("Business " + name + " has paid $" + amount + " to your business " + targetName));
                                            }
                                        }
                                        for (UUID manager : targetBusiness.managers) {
                                            ServerPlayer p = ctx.getSource().getServer().getPlayerList().getPlayer(manager);
                                            if (p != null) {
                                                p.sendSystemMessage(Component.literal("Business " + name + " has paid $" + amount + " to your business " + targetName));
                                            }
                                        }

                                        ctx.getSource().sendSuccess(() -> Component.literal("Paid $" + amount + " from business " + name + " to business " + targetName), false);
                                        return 1;
                                    })
                                )
                            )
                        )
                    )
                )
        );
    }
}
