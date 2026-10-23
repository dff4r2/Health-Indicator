package com.healthindicator;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** The panel shown at the top (or wherever you put it) for the entity you look at. */
public final class HudPanel {
	private HudPanel() {}

	private static final Map<String, Identifier> SPRITES = new HashMap<>();

	private static Identifier sprite(String path) {
		return SPRITES.computeIfAbsent(path, p -> Identifier.of("minecraft", p));
	}

	public static void render(DrawContext dc, RenderTickCounter tickCounter) {
		HealthConfig c = HealthConfig.get();
		MinecraftClient mc = MinecraftClient.getInstance();
		if (!c.enabled || !c.hudPanel) return;
		if (mc.world == null || mc.player == null || mc.options.hudHidden) return;

		float td = tickCounter.getTickDelta(false);
		LivingEntity e = TargetFinder.find(mc, td, c.hudRange);
		if (e == null) return;

		TextRenderer tr = mc.textRenderer;
		float t = mc.world.getTime() + td;
		float hp = Math.max(0f, e.getHealth());
		float max = Math.max(1f, e.getMaxHealth());
		float ratio = Math.min(1f, hp / max);
		int rgb = Fmt.healthRgb(ratio);

		HealthConfig.DisplayStyle style = c.hudStyle;
		boolean icons = style == HealthConfig.DisplayStyle.HEARTS || style == HealthConfig.DisplayStyle.HEARTS_AND_NUMBER;
		HeartModel.Result hm = icons ? HeartModel.build(e, c, mc.world.getLevelProperties().isHardcore(), t) : null;
		boolean bar = style == HealthConfig.DisplayStyle.BAR;

		String label = null;
		if (style != HealthConfig.DisplayStyle.HEARTS || (hm != null && hm.compact)) {
			label = Fmt.label(e, c, style);
		}
		String name = e.getDisplayName().getString();
		int armor = e.getArmor();
		boolean showArmor = c.hudShowArmor && armor > 0;
		String dist = c.hudShowDistance
				? String.format(Locale.ROOT, "%.1f m", mc.player.distanceTo(e)) : null;

		// ---- measure ----
		final int pad = 5;
		int contentW = 60;
		int contentH = 0;
		if (c.hudShowName) {
			contentW = Math.max(contentW, tr.getWidth(name));
			contentH += 12;
		}
		if (hm != null) {
			for (int r = 0; r < hm.rows; r++) contentW = Math.max(contentW, (int) hm.rowWidth(r));
			contentH += hm.rows * 10 + 2;
		}
		if (bar) {
			contentW = Math.max(contentW, 100);
			contentH += 11;
		}
		if (label != null) {
			contentW = Math.max(contentW, tr.getWidth(label));
			contentH += 11;
		}
		if (showArmor) {
			contentW = Math.max(contentW, 81);
			contentH += 11;
		}
		if (dist != null) {
			contentW = Math.max(contentW, tr.getWidth(dist));
			contentH += 11;
		}
		int w = contentW + pad * 2;
		int h = contentH + pad * 2 - 2;

		// ---- place ----
		float sc = c.hudScale;
		int sw = (int) (dc.getScaledWindowWidth() / sc);
		int sh = (int) (dc.getScaledWindowHeight() / sc);
		int x0;
		int y0;
		switch (c.hudPosition) {
			case TOP_LEFT -> { x0 = 6; y0 = 6; }
			case TOP_RIGHT -> { x0 = sw - w - 6; y0 = 6; }
			case BOTTOM_CENTER -> { x0 = (sw - w) / 2; y0 = sh - h - 60; }
			case BOTTOM_LEFT -> { x0 = 6; y0 = sh - h - 60; }
			case BOTTOM_RIGHT -> { x0 = sw - w - 6; y0 = sh - h - 60; }
			default -> { x0 = (sw - w) / 2; y0 = 6; }
		}
		x0 += c.hudOffsetX;
		y0 += c.hudOffsetY;
		int cx = x0 + w / 2;

		dc.getMatrices().push();
		dc.getMatrices().scale(sc, sc, 1f);

		if (c.hudBackground) {
			dc.fill(x0, y0, x0 + w, y0 + h, 0x99000000);
			dc.drawBorder(x0, y0, w, h, 0x66FFFFFF);
		}

		int y = y0 + pad;

		if (c.hudShowName) {
			dc.drawCenteredTextWithShadow(tr, name, cx, y, 0xFFFFFFFF);
			y += 12;
		}

		if (hm != null) {
			for (HeartModel.Icon i : hm.icons) {
				int x = Math.round(cx + hm.iconX(i));
				int yy = Math.round(y + i.row * 10 + i.wiggle);
				dc.drawGuiTexture(sprite("hud/heart/" + hm.containerName()), x, yy, 9, 9);
			}
			for (HeartModel.Icon i : hm.icons) {
				if (i.fill == HeartModel.Fill.NONE) continue;
				int x = Math.round(cx + hm.iconX(i));
				int yy = Math.round(y + i.row * 10 + i.wiggle);
				dc.drawGuiTexture(sprite("hud/heart/" + hm.fillName(i)), x, yy, 9, 9);
			}
			y += hm.rows * 10 + 2;
		}

		if (bar) {
			int bw = 100;
			int bx = cx - bw / 2;
			dc.fill(bx - 1, y - 1, bx + bw + 1, y + 9, 0xFF000000);
			dc.fill(bx, y, bx + bw, y + 8, 0xFF3A3A3A);
			dc.fill(bx, y, bx + Math.round(bw * ratio), y + 8, 0xFF000000 | rgb);
			y += 11;
		}

		if (label != null) {
			dc.drawCenteredTextWithShadow(tr, label, cx, y, 0xFF000000 | rgb);
			y += 11;
		}

		if (showArmor) {
			int ax = cx - 40;
			for (int i = 0; i < 10; i++) {
				int v = armor - i * 2;
				String s = v >= 2 ? "hud/armor_full" : (v == 1 ? "hud/armor_half" : "hud/armor_empty");
				dc.drawGuiTexture(sprite(s), ax + i * 8, y, 9, 9);
			}
			y += 11;
		}

		if (dist != null) {
			dc.drawCenteredTextWithShadow(tr, dist, cx, y, 0xFFAAAAAA);
		}

		dc.getMatrices().pop();
	}
}
