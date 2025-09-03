package io.github.powerbox1000.pbxeconomy;

import io.github.powerbox1000.pbxeconomy.commands.*;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
// import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
// import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Economy implements ModInitializer {
	public static final String MOD_ID = "pbxeconomy";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static DataHandler DATA_HANDLER;

	@Override
	public void onInitialize() {
		LOGGER.info("PBX Economy Initialized!");

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			BalanceCommand.register(dispatcher);
			BusinessCommand.register(dispatcher);
			PayCommand.register(dispatcher);
		});

		ServerLifecycleEvents.SERVER_STARTED.register((server) -> {
			DATA_HANDLER = DataHandler.getServerState(server);
		});

		// ServerTickEvents.END_SERVER_TICK.register((server) -> {
		// 	DATA_HANDLER.setDirty();
		// });

		// ServerPlayerEvents.JOIN.register((player) -> {
		// 	DATA_HANDLER.getPlayerState(player);
		// });
	}
}