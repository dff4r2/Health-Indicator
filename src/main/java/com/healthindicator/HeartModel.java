package com.healthindicator;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;

import java.util.ArrayList;
import java.util.List;

/** Works out which heart icons to show for an entity. Rendering is done elsewhere. */
final class HeartModel {
	private HeartModel() {}

	enum Fill { NONE, HALF, FULL }

	static final class Icon {
		final int col;
		final int row;
		final Fill fill;
		final boolean absorb;
		/** Vertical pixel offset (negative = up). */
		final float wiggle;

		Icon(int col, int row, Fill fill, boolean absorb, float wiggle) {
			this.col = col;
			this.row = row;
			this.fill = fill;
			this.absorb = absorb;
			this.wiggle = wiggle;
		}
	}

	static final class Result {
		final List<Icon> icons = new ArrayList<>();
		int[] rowCount = new int[0];
		int rows = 0;
		/** Too many hearts: a single heart is shown and the number carries the info. */
		boolean compact = false;
		String prefix = "";
		boolean hardcore = false;

		String containerName() {
			return hardcore ? "container_hardcore" : "container";
		}

		String fillName(Icon i) {
			String size = i.fill == Fill.FULL ? "full" : "half";
			String hc = hardcore ? "hardcore_" : "";
			return (i.absorb ? "absorbing_" : prefix) + hc + size;
		}

		/** Left edge of an icon, centred per row. */
		float iconX(Icon i) {
			int n = rowCount[i.row];
			float w = (n - 1) * 8f + 9f;
			return -w / 2f + i.col * 8f;
		}

		float rowWidth(int row) {
			int n = rowCount[row];
			return (n - 1) * 8f + 9f;
		}
	}

	static Result build(LivingEntity e, HealthConfig c, boolean worldHardcore, float t) {
		Result r = new Result();
		r.hardcore = c.hardcoreHearts && worldHardcore;

		if (c.effectHearts) {
			if (e.hasStatusEffect(StatusEffects.WITHER)) r.prefix = "withered_";
			else if (e.hasStatusEffect(StatusEffects.POISON)) r.prefix = "poisoned_";
			else if (e.getFrozenTicks() >= e.getMinFreezeDamageTicks()) r.prefix = "frozen_";
		}

		float hp = Math.max(0f, e.getHealth());
		float max = Math.max(1f, e.getMaxHealth());
		float per = Math.max(0.5f, c.hpPerHeart);
		int total = (int) Math.ceil(max / per);

		if (total > c.maxHearts) {
			if (c.overflow == HealthConfig.Overflow.COMPACT) {
				r.compact = true;
				r.icons.add(new Icon(0, 0, Fill.FULL, false, 0f));
				r.rowCount = new int[]{1};
				r.rows = 1;
				return r;
			}
			// SCALE_TO_FIT: every heart represents more health.
			per = max / c.maxHearts;
			total = c.maxHearts;
		}

		int perRow = Math.max(1, c.heartsPerRow);
		int regen = (c.regenAnimation && e.hasStatusEffect(StatusEffects.REGENERATION))
				? ((int) t) % Math.max(1, total + 5) : -1;
		boolean hurt = c.hurtShake && e.hurtTime > 0;

		for (int i = 0; i < total; i++) {
			Fill f = fillFor(hp - i * per, per, i == 0 && hp > 0f);
			if (f == Fill.NONE && !c.showEmptyContainers) continue;
			float w = (i == regen) ? -2f : 0f;
			if (hurt) w += ((i * 7 + (int) t) % 3) - 1;
			r.icons.add(new Icon(i % perRow, i / perRow, f, false, w));
		}

		int healthRows = r.icons.isEmpty() ? 0 : r.icons.get(r.icons.size() - 1).row + 1;

		float abs = c.showAbsorption ? e.getAbsorptionAmount() : 0f;
		if (abs > 0f) {
			int ah = Math.min((int) Math.ceil(abs / per), c.maxHearts);
			for (int j = 0; j < ah; j++) {
				Fill f = fillFor(abs - j * per, per, true);
				if (f == Fill.NONE) f = Fill.HALF;
				r.icons.add(new Icon(j % perRow, healthRows + j / perRow, f, true, 0f));
			}
		}

		int rows = 0;
		for (Icon i : r.icons) rows = Math.max(rows, i.row + 1);
		r.rows = rows;
		r.rowCount = new int[rows];
		for (Icon i : r.icons) r.rowCount[i.row]++;
		return r;
	}

	private static Fill fillFor(float remaining, float per, boolean atLeastHalf) {
		if (remaining >= per * 0.75f) return Fill.FULL;
		if (remaining >= per * 0.25f || atLeastHalf) return Fill.HALF;
		return Fill.NONE;
	}
}
