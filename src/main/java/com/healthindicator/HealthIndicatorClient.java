package com.healthindicator;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

public class HealthIndicatorClient implements ClientModInitializer {

	/** Master switch, toggled with the H key. */
	public static boolean enabled = true;

	private static final double WORLD_BAR_RANGE = 32.0;
	private static final double HUD_RANGE = 24.0;
	private static final int BAR_SEGMENTS = 10;

	private static KeyBinding toggleKey;

	@Override
	public void onInitializeClient() {
		toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.healthindicator.toggle",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_H,
				"category.healthindicator"
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggleKey.wasPressed()) {
				enabled = !enabled;
				if (client.player != null) {
					client.player.sendMessage(
							Text.translatable(enabled ? "message.healthindicator.on" : "message.healthindicator.off"),
							true);
				}
			}
		});

		WorldRenderEvents.AFTER_ENTITIES.register(HealthIndicatorClient::renderWorldBars);
		HudRenderCallback.EVENT.register(HealthIndicatorClient::renderHud);
	}

	// ------------------------------------------------------------------
	// Health bars floating above entities
	// ------------------------------------------------------------------

	private static void renderWorldBars(WorldRenderContext context) {
		if (!enabled) return;

		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null || client.player == null) return;

		VertexConsumerProvider consumers = context.consumers();
		MatrixStack matrices = context.matrixStack();
		if (consumers == null || matrices == null) return;

		Camera camera = context.camera();
		Vec3d camPos = camera.getPos();
		float tickDelta = context.tickCounter().getTickDelta(false);
		TextRenderer textRenderer = client.textRenderer;

		for (Entity entity : client.world.getEntities()) {
			if (!(entity instanceof LivingEntity living)) continue;
			if (living == client.getCameraEntity()) continue;
			if (living.isInvisibleTo(client.player)) continue;
			if (!living.isAlive()) continue;
			if (living.squaredDistanceTo(camPos) > WORLD_BAR_RANGE * WORLD_BAR_RANGE) continue;

			Vec3d pos = living.getLerpedPos(tickDelta);
			double x = pos.x - camPos.x;
			double y = pos.y - camPos.y + living.getHeight() + 0.6;
			double z = pos.z - camPos.z;

			MutableText line = buildBar(living.getHealth(), living.getMaxHealth());

			matrices.push();
			matrices.translate(x, y, z);
			matrices.multiply(camera.getRotation());
			matrices.scale(-0.03f, -0.03f, 0.03f);

			Matrix4f matrix = matrices.peek().getPositionMatrix();
			float width = textRenderer.getWidth(line);
			textRenderer.draw(
					line,
					-width / 2f,
					0f,
					0xFFFFFFFF,
					false,
					matrix,
					consumers,
					TextRenderer.TextLayerType.NORMAL,
					0x40000000,
					LightmapTextureManager.MAX_LIGHT_COORDINATE
			);
			matrices.pop();
		}

		// Make sure the text we queued is actually drawn this frame.
		if (consumers instanceof VertexConsumerProvider.Immediate immediate) {
			immediate.draw();
		}
	}

	private static MutableText buildBar(float health, float maxHealth) {
		float max = Math.max(maxHealth, 1f);
		float ratio = Math.max(0f, Math.min(1f, health / max));
		int filled = Math.round(ratio * BAR_SEGMENTS);
		if (health > 0f && filled == 0) filled = 1;

		Formatting color = ratio > 0.6f ? Formatting.GREEN : ratio > 0.3f ? Formatting.YELLOW : Formatting.RED;

		MutableText bar = Text.literal("|".repeat(filled)).formatted(color);
		bar.append(Text.literal("|".repeat(BAR_SEGMENTS - filled)).formatted(Formatting.DARK_GRAY));
		// 1 heart = 2 health points
		bar.append(Text.literal(" ♥ " + format(health / 2f) + "/" + format(max / 2f)).formatted(Formatting.RED));
		return bar;
	}

	// ------------------------------------------------------------------
	// HUD panel for the entity under the crosshair
	// ------------------------------------------------------------------

	private static void renderHud(DrawContext drawContext, net.minecraft.client.render.RenderTickCounter tickCounter) {
		if (!enabled) return;

		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null || client.player == null || client.options.hudHidden) return;

		LivingEntity target = findTarget(client, tickCounter.getTickDelta(false));
		if (target == null) return;

		TextRenderer tr = client.textRenderer;
		int screenWidth = drawContext.getScaledWindowWidth();

		float health = target.getHealth();
		float max = Math.max(target.getMaxHealth(), 1f);
		float ratio = Math.max(0f, Math.min(1f, health / max));
		int barColor = ratio > 0.6f ? 0xFF55FF55 : ratio > 0.3f ? 0xFFFFFF55 : 0xFFFF5555;

		int barWidth = 120;
		int barHeight = 8;
		int x = (screenWidth - barWidth) / 2;
		int y = 22;

		Text name = target.getDisplayName();
		drawContext.drawCenteredTextWithShadow(tr, name, screenWidth / 2, y - 12, 0xFFFFFFFF);

		// background + border
		drawContext.fill(x - 1, y - 1, x + barWidth + 1, y + barHeight + 1, 0xFF000000);
		drawContext.fill(x, y, x + barWidth, y + barHeight, 0xFF3A3A3A);
		// fill
		drawContext.fill(x, y, x + Math.round(barWidth * ratio), y + barHeight, barColor);

		// 1 heart = 2 health points
		String label = "♥ " + format(health / 2f) + " / " + format(max / 2f);
		float absorption = target.getAbsorptionAmount();
		if (absorption > 0f) label += " (+" + format(absorption / 2f) + ")";
		drawContext.drawCenteredTextWithShadow(tr, label, screenWidth / 2, y + barHeight + 3, 0xFFFFFFFF);
	}

	private static LivingEntity findTarget(MinecraftClient client, float tickDelta) {
		Entity camera = client.getCameraEntity();
		if (camera == null) return null;

		Vec3d start = camera.getCameraPosVec(tickDelta);
		Vec3d dir = camera.getRotationVec(tickDelta);
		Vec3d end = start.add(dir.multiply(HUD_RANGE));
		Box box = camera.getBoundingBox().stretch(dir.multiply(HUD_RANGE)).expand(1.0);

		EntityHitResult hit = ProjectileUtil.raycast(
				camera,
				start,
				end,
				box,
				e -> e instanceof LivingEntity && !e.isSpectator() && e.canHit(),
				HUD_RANGE * HUD_RANGE
		);

		if (hit != null && hit.getEntity() instanceof LivingEntity living) {
			return living;
		}
		return null;
	}

	private static String format(float value) {
		if (Math.abs(value - Math.round(value)) < 0.05f) {
			return Integer.toString(Math.round(value));
		}
		return String.format(java.util.Locale.ROOT, "%.1f", value);
	}
}
