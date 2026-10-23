package com.healthindicator;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Floating "-3 hearts" style numbers when an entity takes damage (or heals). */
final class DamagePopups {
	private DamagePopups() {}

	private static final int LIFE_TICKS = 24;
	private static final double RANGE = 48.0;

	private static final class Popup {
		final double x, y, z;
		final float amount;
		final boolean heal;
		final long born;

		Popup(double x, double y, double z, float amount, boolean heal, long born) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.amount = amount;
			this.heal = heal;
			this.born = born;
		}
	}

	private static final Map<Integer, Float> LAST = new HashMap<>();
	private static final List<Popup> POPUPS = new ArrayList<>();
	private static final Random RNG = new Random();

	static void tick(MinecraftClient mc) {
		HealthConfig c = HealthConfig.get();
		if (mc.world == null || mc.player == null || !c.enabled || !c.damageNumbers) {
			LAST.clear();
			POPUPS.clear();
			return;
		}

		long now = mc.world.getTime();
		POPUPS.removeIf(p -> now - p.born > LIFE_TICKS);

		Vec3d cam = mc.gameRenderer.getCamera().getPos();
		Set<Integer> seen = new HashSet<>();

		for (Entity entity : mc.world.getEntities()) {
			if (!(entity instanceof LivingEntity e)) continue;
			int id = e.getId();
			seen.add(id);

			float hp = e.getHealth();
			Float prev = LAST.put(id, hp);
			if (prev == null) continue;

			float diff = hp - prev;
			if (Math.abs(diff) < 0.01f) continue;
			if (diff > 0f && !c.healNumbers) continue;

			double d = e.getPos().distanceTo(cam);
			if (d > RANGE) continue;
			if (!Filters.shouldShow(e, mc, c, d, null, false)) continue;

			POPUPS.add(new Popup(
					e.getX() + (RNG.nextDouble() - 0.5) * 0.6,
					e.getY() + e.getHeight() * 0.9,
					e.getZ() + (RNG.nextDouble() - 0.5) * 0.6,
					Math.abs(diff), diff > 0f, now));
		}

		LAST.keySet().retainAll(seen);
	}

	static void render(MinecraftClient mc, HealthConfig c, MatrixStack ms, Camera cam, Vec3d camPos,
					   float td, VertexConsumerProvider vcp, TextRenderer tr) {
		if (!c.damageNumbers || POPUPS.isEmpty() || mc.world == null) return;
		long now = mc.world.getTime();

		for (Popup p : POPUPS) {
			float prog = ((now - p.born) + td) / LIFE_TICKS;
			if (prog < 0f || prog > 1f) continue;

			float alpha = 1f - prog * prog * prog;
			float s = 0.03f * c.damageNumberScale * (1f + Math.max(0f, 0.3f - prog) * 1.5f);

			ms.push();
			ms.translate(p.x - camPos.x, p.y + prog * 0.9 - camPos.y, p.z - camPos.z);
			ms.multiply(cam.getRotation());
			ms.scale(-s, -s, s);
			Matrix4f m = ms.peek().getPositionMatrix();

			String txt = (p.heal ? "+" : "-") + Fmt.num(p.amount / c.hpPerHeart) + " ♥";
			int rgb = p.heal ? 0x55FF55 : 0xFF5555;
			int a = Math.max(4, (int) (alpha * 255f));
			tr.draw(txt, -tr.getWidth(txt) / 2f, -4f, (a << 24) | rgb, true, m, vcp,
					TextRenderer.TextLayerType.SEE_THROUGH, 0, LightmapTextureManager.MAX_LIGHT_COORDINATE);
			ms.pop();
		}
	}
}
