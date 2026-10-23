package com.healthindicator;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/** Draws the floating indicators (hearts / bar / text) above entities, plus damage numbers. */
public final class WorldIndicatorRenderer {
	private WorldIndicatorRenderer() {}

	private static final int LIGHT = LightmapTextureManager.MAX_LIGHT_COORDINATE;
	private static final Identifier WHITE = Identifier.of("minecraft", "textures/misc/white.png");
	private static final Map<String, Identifier> HEART_TEX = new HashMap<>();

	private static Identifier heart(String name) {
		return HEART_TEX.computeIfAbsent(name,
				n -> Identifier.of("minecraft", "textures/gui/sprites/hud/heart/" + n + ".png"));
	}

	public static void render(WorldRenderContext ctx) {
		HealthConfig c = HealthConfig.get();
		if (!c.enabled) return;

		MinecraftClient mc = MinecraftClient.getInstance();
		ClientWorld world = mc.world;
		if (world == null || mc.player == null) return;

		VertexConsumerProvider vcp = ctx.consumers();
		MatrixStack ms = ctx.matrixStack();
		if (vcp == null || ms == null) return;

		Camera camera = ctx.camera();
		Vec3d camPos = camera.getPos();
		float td = ctx.tickCounter().getTickDelta(false);
		float t = world.getTime() + td;
		boolean hardcore = world.getLevelProperties().isHardcore();
		TextRenderer tr = mc.textRenderer;

		if (c.worldIndicator) {
			LivingEntity looked = c.onlyLookingAt ? TargetFinder.find(mc, td, c.maxDistance) : null;
			for (Entity entity : world.getEntities()) {
				if (!(entity instanceof LivingEntity e)) continue;
				Vec3d p = e.getLerpedPos(td);
				double dist = p.distanceTo(camPos);
				if (!Filters.shouldShow(e, mc, c, dist, looked, true)) continue;
				if (c.hideWhenSneaking && e.isSneaking()) continue;
				drawIndicator(c, e, p, dist, camera, camPos, t, hardcore, vcp, ms, tr);
			}
		}

		DamagePopups.render(mc, c, ms, camera, camPos, td, vcp, tr);

		// Flush what we queued so it is actually drawn this frame.
		if (vcp instanceof VertexConsumerProvider.Immediate immediate) {
			immediate.draw();
		}
	}

	private static void drawIndicator(HealthConfig c, LivingEntity e, Vec3d p, double dist,
									  Camera cam, Vec3d camPos, float t, boolean hardcore,
									  VertexConsumerProvider vcp, MatrixStack ms, TextRenderer tr) {
		float hp = Math.max(0f, e.getHealth());
		float max = Math.max(1f, e.getMaxHealth());
		float ratio = Math.min(1f, hp / max);

		float s = 0.025f * c.scale;
		if (c.distanceScaling) s *= Math.min(4f, Math.max(1f, (float) dist / 12f));

		float alpha = c.opacity;
		if (c.lowHealthBlink && hp > 0f && ratio <= c.lowHealthThreshold) {
			alpha *= 0.35f + 0.65f * (0.5f + 0.5f * (float) Math.sin(t * 0.6f));
		}
		int a = Math.max(4, Math.min(255, (int) (alpha * 255f)));
		boolean see = c.seeThroughWalls;

		ms.push();
		ms.translate(p.x - camPos.x, p.y - camPos.y + e.getHeight() + 0.5 + c.verticalOffset, p.z - camPos.z);
		ms.multiply(cam.getRotation());
		ms.scale(-s, -s, s);
		Matrix4f m = ms.peek().getPositionMatrix();

		HealthConfig.DisplayStyle style = c.style;
		boolean icons = style == HealthConfig.DisplayStyle.HEARTS || style == HealthConfig.DisplayStyle.HEARTS_AND_NUMBER;
		HeartModel.Result hm = icons ? HeartModel.build(e, c, hardcore, t) : null;

		String label = null;
		if (style != HealthConfig.DisplayStyle.HEARTS || (hm != null && hm.compact)) {
			label = Fmt.label(e, c, style);
		}

		float bottom = 0f;
		int rgb = Fmt.healthRgb(ratio);

		if (label != null) {
			float w = tr.getWidth(label);
			float top = bottom - 9f;
			tr.draw(label, -w / 2f, top, (a << 24) | rgb, true, m, vcp,
					see ? TextRenderer.TextLayerType.SEE_THROUGH : TextRenderer.TextLayerType.NORMAL,
					0, LIGHT);
			bottom = top - 1f;
		}

		if (hm != null) {
			bottom = drawHearts(hm, bottom, (a << 24) | 0xFFFFFF, see, m, vcp);
		} else if (style == HealthConfig.DisplayStyle.BAR) {
			bottom = drawBar(ratio, rgb, a, bottom, see, m, vcp);
		}

		ms.pop();
	}

	private static float drawHearts(HeartModel.Result hm, float bottom, int tint, boolean see,
									Matrix4f m, VertexConsumerProvider vcp) {
		Identifier container = heart(hm.containerName());
		// All containers first, then all fills: only two render-layer switches per entity.
		for (HeartModel.Icon i : hm.icons) {
			float x = hm.iconX(i);
			float y = bottom - i.row * 10f - 9f + i.wiggle;
			quad(vcp, container, see, m, x, y, 9f, 9f, tint, 0f);
		}
		for (HeartModel.Icon i : hm.icons) {
			if (i.fill == HeartModel.Fill.NONE) continue;
			float x = hm.iconX(i);
			float y = bottom - i.row * 10f - 9f + i.wiggle;
			quad(vcp, heart(hm.fillName(i)), see, m, x, y, 9f, 9f, tint, 0.03f);
		}
		return bottom - hm.rows * 10f - 1f;
	}

	private static float drawBar(float ratio, int rgb, int a, float bottom, boolean see,
								 Matrix4f m, VertexConsumerProvider vcp) {
		float bw = 56f;
		float bh = 6f;
		float top = bottom - bh;
		int border = Math.max(4, (int) (a * 0.85f)) << 24;
		quad(vcp, WHITE, see, m, -bw / 2f - 1f, top - 1f, bw + 2f, bh + 2f, border, 0f);
		quad(vcp, WHITE, see, m, -bw / 2f, top, bw, bh, (a << 24) | 0x3A3A3A, 0.03f);
		quad(vcp, WHITE, see, m, -bw / 2f, top, bw * ratio, bh, (a << 24) | rgb, 0.06f);
		return top - 3f;
	}

	/** One textured, tinted quad. Vertex order matches vanilla text glyphs. */
	private static void quad(VertexConsumerProvider vcp, Identifier tex, boolean see, Matrix4f m,
							 float x, float y, float w, float h, int argb, float z) {
		RenderLayer layer = see ? RenderLayer.getTextSeeThrough(tex) : RenderLayer.getText(tex);
		VertexConsumer vc = vcp.getBuffer(layer);
		int a = (argb >>> 24) & 255;
		int r = (argb >> 16) & 255;
		int g = (argb >> 8) & 255;
		int b = argb & 255;
		vc.vertex(m, x, y, z).color(r, g, b, a).texture(0f, 0f).light(LIGHT);
		vc.vertex(m, x, y + h, z).color(r, g, b, a).texture(0f, 1f).light(LIGHT);
		vc.vertex(m, x + w, y + h, z).color(r, g, b, a).texture(1f, 1f).light(LIGHT);
		vc.vertex(m, x + w, y, z).color(r, g, b, a).texture(1f, 0f).light(LIGHT);
	}
}
