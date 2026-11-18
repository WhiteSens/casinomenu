package com.casino.client;

import com.mojang.brigadier.Command;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class CasinoMod implements ClientModInitializer {
	public static final String MOD_ID = "casinomod";

	private static KeyBinding openKey;

	@Override
	public void onInitializeClient() {
		openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.casinomod.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "key.categories.casinomod"));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openKey.wasPressed()) {
				if (client.currentScreen == null) {
					client.setScreen(new MenuScreen());
				}
			}
		});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
				dispatcher.register(ClientCommandManager.literal("casino").executes(ctx -> {
					MinecraftClient mc = ctx.getSource().getClient();
					// открываем на следующем тике, иначе чат сразу закроет экран
					mc.send(() -> mc.setScreen(new MenuScreen()));
					return Command.SINGLE_SUCCESS;
				})));
	}
}
