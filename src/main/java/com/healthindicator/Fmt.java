package com.healthindicator;

import net.minecraft.entity.LivingEntity;

import java.util.Locale;

/** Small formatting helpers shared by the renderers. */
final class Fmt {
	private Fmt() {}

	/** 7.0 -> "7", 7.25 -> "7.3" (one decimal at most). */
	static String num(float v) {
		float r = Math.round(v * 10f) / 10f;
		if (r == (int) r) return Integer.toString((int) r);
		return String.format(Locale.ROOT, "%.1f", r);
	}

	static int healthRgb(float ratio) {
		if (ratio > 0.6f) return 0x55FF55;
		if (ratio > 0.3f) return 0xFFFF55;
		return 0xFF5555;
	}

	/** Text shown next to / instead of the hearts. */
	static String label(LivingEntity e, HealthConfig c, HealthConfig.DisplayStyle style) {
		float hp = Math.max(0f, e.getHealth());
		float max = Math.max(1f, e.getMaxHealth());
		if (style == HealthConfig.DisplayStyle.PERCENT) {
			return Math.round(hp / max * 100f) + "%";
		}
		float per = c.hpPerHeart;
		String s = "♥ " + num(hp / per) + "/" + num(max / per);
		float abs = e.getAbsorptionAmount();
		if (c.showAbsorption && abs > 0f) s += " +" + num(abs / per);
		return s;
	}
}
