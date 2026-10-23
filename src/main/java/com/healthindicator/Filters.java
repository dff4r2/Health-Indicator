package com.healthindicator;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Locale;

/** Decides which entities get an indicator. */
final class Filters {
	private Filters() {}

	/**
	 * @param applyState if true, also applies distance / only-damaged / only-looking-at.
	 *                   (Damage numbers pass false and do their own range check.)
	 */
	static boolean shouldShow(LivingEntity e, MinecraftClient mc, HealthConfig c,
							  double dist, LivingEntity looked, boolean applyState) {
		if (e instanceof ArmorStandEntity) return false;
		if (!e.isAlive()) return false;

		Entity cam = mc.getCameraEntity();
		if (e == cam) {
			if (!c.showSelf || mc.options.getPerspective().isFirstPerson()) return false;
		} else if (mc.player != null && e.isInvisibleTo(mc.player)) {
			return false;
		}

		if (matches(c.blacklist, e)) return false;

		if (matches(c.alwaysShow, e)) {
			return dist <= 128.0;
		}

		if (e instanceof PlayerEntity) {
			if (e != cam && !c.showPlayers) return false;
		} else if (e instanceof Monster) {
			if (!c.showHostile) return false;
		} else if (!c.showPassive) {
			return false;
		}

		if (applyState) {
			if (dist > c.maxDistance) return false;
			if (c.onlyDamaged && e.getHealth() >= e.getMaxHealth() - 0.01f) return false;
			if (c.onlyLookingAt && e != looked) return false;
		}
		return true;
	}

	/** Matches a player name, "minecraft:zombie" or just "zombie". */
	private static boolean matches(List<String> list, LivingEntity e) {
		if (list == null || list.isEmpty()) return false;
		String name = e.getName().getString().toLowerCase(Locale.ROOT);
		Identifier id = Registries.ENTITY_TYPE.getId(e.getType());
		String full = id.toString().toLowerCase(Locale.ROOT);
		String path = id.getPath().toLowerCase(Locale.ROOT);
		for (String raw : list) {
			if (raw == null) continue;
			String s = raw.trim().toLowerCase(Locale.ROOT);
			if (s.isEmpty()) continue;
			if (s.equals(name) || s.equals(full) || s.equals(path)) return true;
		}
		return false;
	}
}
