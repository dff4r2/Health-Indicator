package com.healthindicator;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

public class HealthIndicatorClient implements ClientModInitializer {

	private static KeyBinding toggleKey;
	private static KeyBinding configKey;
	private static KeyBinding cycleKey;

	@Override
	public void onInitializeClient() {
		HealthConfig.load();

		toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.healthindicator.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, "category.healthindicator"));
		configKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.healthindicator.config", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_O, "category.healthindicator"));
		cycleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.healthindicator.cycle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "category.healthindicator"));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			HealthConfig c = HealthConfig.get();

			while (toggleKey.wasPressed()) {
				c.enabled = !c.enabled;
				c.save();
				if (client.player != null) {
					client.player.sendMessage(Text.translatable(
							c.enabled ? "message.healthindicator.on" : "message.healthindicator.off"), true);
				}
			}

			while (configKey.wasPressed()) {
				client.setScreen(ConfigScreenBuilder.build(client.currentScreen));
			}

			while (cycleKey.wasPressed()) {
				HealthConfig.DisplayStyle[] all = HealthConfig.DisplayStyle.values();
				c.style = all[(c.style.ordinal() + 1) % all.length];
				c.save();
				if (client.player != null) {
					client.player.sendMessage(Text.translatable("message.healthindicator.style",
							Text.translatable("enum.healthindicator." + c.style.name().toLowerCase(Locale.ROOT))), true);
				}
			}

			DamagePopups.tick(client);
		});

		WorldRenderEvents.AFTER_ENTITIES.register(WorldIndicatorRenderer::render);
		HudRenderCallback.EVENT.register(HudPanel::render);
	}
}
