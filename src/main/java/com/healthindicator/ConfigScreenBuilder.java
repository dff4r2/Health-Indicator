package com.healthindicator;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

/** The settings screen (Cloth Config). Opened from Mod Menu or with the O key. */
public final class ConfigScreenBuilder {
	private ConfigScreenBuilder() {}

	private static Text name(String id) {
		return Text.translatable("option.healthindicator." + id);
	}

	private static Text[] tip(String id) {
		String key = "tooltip.healthindicator." + id;
		return I18n.hasTranslation(key) ? new Text[]{Text.translatable(key)} : new Text[0];
	}

	private static Text cat(String id) {
		return Text.translatable("config.healthindicator.cat." + id);
	}

	public static Screen build(Screen parent) {
		HealthConfig c = HealthConfig.get();

		ConfigBuilder b = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(Text.translatable("config.healthindicator.title"))
				.setSavingRunnable(() -> {
					c.sanitize();
					c.save();
				})
				.setTransparentBackground(true);
		ConfigEntryBuilder eb = b.entryBuilder();

		// ---------------- General ----------------
		ConfigCategory general = b.getOrCreateCategory(cat("general"));
		bool(eb, general, "enabled", c.enabled, true, v -> c.enabled = v);
		bool(eb, general, "worldIndicator", c.worldIndicator, true, v -> c.worldIndicator = v);
		bool(eb, general, "hudPanel", c.hudPanel, true, v -> c.hudPanel = v);
		bool(eb, general, "damageNumbers", c.damageNumbers, true, v -> c.damageNumbers = v);

		// ---------------- Style ----------------
		ConfigCategory style = b.getOrCreateCategory(cat("style"));
		choice(eb, style, "style", HealthConfig.DisplayStyle.class, c.style,
				HealthConfig.DisplayStyle.HEARTS_AND_NUMBER, v -> c.style = v);
		slider(eb, style, "scale", Math.round(c.scale * 100f), 80, 30, 300,
				v -> Text.literal(v + "%"), v -> c.scale = v / 100f);
		slider(eb, style, "opacity", Math.round(c.opacity * 100f), 100, 10, 100,
				v -> Text.literal(v + "%"), v -> c.opacity = v / 100f);
		slider(eb, style, "verticalOffset", Math.round(c.verticalOffset * 100f), 25, -100, 300,
				v -> Text.literal(String.format(Locale.ROOT, "%.2f", v / 100f)), v -> c.verticalOffset = v / 100f);
		bool(eb, style, "seeThroughWalls", c.seeThroughWalls, false, v -> c.seeThroughWalls = v);
		bool(eb, style, "distanceScaling", c.distanceScaling, true, v -> c.distanceScaling = v);
		bool(eb, style, "hideWhenSneaking", c.hideWhenSneaking, true, v -> c.hideWhenSneaking = v);

		// ---------------- Hearts ----------------
		ConfigCategory hearts = b.getOrCreateCategory(cat("hearts"));
		slider(eb, hearts, "hpPerHeart", Math.round(c.hpPerHeart * 2f), 4, 1, 40,
				v -> Text.literal(Fmt.num(v / 2f) + " HP"), v -> c.hpPerHeart = v / 2f);
		slider(eb, hearts, "heartsPerRow", c.heartsPerRow, 10, 1, 20,
				v -> Text.literal(String.valueOf(v)), v -> c.heartsPerRow = v);
		slider(eb, hearts, "maxHearts", c.maxHearts, 20, 1, 100,
				v -> Text.literal(String.valueOf(v)), v -> c.maxHearts = v);
		choice(eb, hearts, "overflow", HealthConfig.Overflow.class, c.overflow,
				HealthConfig.Overflow.COMPACT, v -> c.overflow = v);
		bool(eb, hearts, "showEmptyContainers", c.showEmptyContainers, true, v -> c.showEmptyContainers = v);
		bool(eb, hearts, "showAbsorption", c.showAbsorption, true, v -> c.showAbsorption = v);
		bool(eb, hearts, "hardcoreHearts", c.hardcoreHearts, false, v -> c.hardcoreHearts = v);

		// ---------------- Effects ----------------
		ConfigCategory effects = b.getOrCreateCategory(cat("effects"));
		bool(eb, effects, "effectHearts", c.effectHearts, true, v -> c.effectHearts = v);
		bool(eb, effects, "regenAnimation", c.regenAnimation, true, v -> c.regenAnimation = v);
		bool(eb, effects, "hurtShake", c.hurtShake, true, v -> c.hurtShake = v);
		bool(eb, effects, "lowHealthBlink", c.lowHealthBlink, true, v -> c.lowHealthBlink = v);
		slider(eb, effects, "lowHealthThreshold", Math.round(c.lowHealthThreshold * 100f), 25, 5, 60,
				v -> Text.literal(v + "%"), v -> c.lowHealthThreshold = v / 100f);

		// ---------------- Filters ----------------
		ConfigCategory filters = b.getOrCreateCategory(cat("filters"));
		bool(eb, filters, "showPlayers", c.showPlayers, true, v -> c.showPlayers = v);
		bool(eb, filters, "showHostile", c.showHostile, true, v -> c.showHostile = v);
		bool(eb, filters, "showPassive", c.showPassive, true, v -> c.showPassive = v);
		bool(eb, filters, "showSelf", c.showSelf, false, v -> c.showSelf = v);
		bool(eb, filters, "onlyDamaged", c.onlyDamaged, false, v -> c.onlyDamaged = v);
		bool(eb, filters, "onlyLookingAt", c.onlyLookingAt, false, v -> c.onlyLookingAt = v);
		slider(eb, filters, "maxDistance", c.maxDistance, 32, 4, 128,
				v -> Text.literal(v + " m"), v -> c.maxDistance = v);
		strings(eb, filters, "alwaysShow", c.alwaysShow, v -> c.alwaysShow = v);
		strings(eb, filters, "blacklist", c.blacklist, v -> c.blacklist = v);

		// ---------------- Target panel ----------------
		ConfigCategory hud = b.getOrCreateCategory(cat("hud"));
		bool(eb, hud, "hudPanel", c.hudPanel, true, v -> c.hudPanel = v);
		choice(eb, hud, "hudStyle", HealthConfig.DisplayStyle.class, c.hudStyle,
				HealthConfig.DisplayStyle.HEARTS_AND_NUMBER, v -> c.hudStyle = v);
		choice(eb, hud, "hudPosition", HealthConfig.HudPosition.class, c.hudPosition,
				HealthConfig.HudPosition.TOP_CENTER, v -> c.hudPosition = v);
		slider(eb, hud, "hudOffsetX", c.hudOffsetX, 0, -400, 400,
				v -> Text.literal(String.valueOf(v)), v -> c.hudOffsetX = v);
		slider(eb, hud, "hudOffsetY", c.hudOffsetY, 0, -300, 300,
				v -> Text.literal(String.valueOf(v)), v -> c.hudOffsetY = v);
		slider(eb, hud, "hudScale", Math.round(c.hudScale * 100f), 100, 50, 300,
				v -> Text.literal(v + "%"), v -> c.hudScale = v / 100f);
		bool(eb, hud, "hudShowName", c.hudShowName, true, v -> c.hudShowName = v);
		bool(eb, hud, "hudShowArmor", c.hudShowArmor, true, v -> c.hudShowArmor = v);
		bool(eb, hud, "hudShowDistance", c.hudShowDistance, true, v -> c.hudShowDistance = v);
		bool(eb, hud, "hudBackground", c.hudBackground, true, v -> c.hudBackground = v);
		slider(eb, hud, "hudRange", c.hudRange, 24, 4, 64,
				v -> Text.literal(v + " m"), v -> c.hudRange = v);

		// ---------------- Damage numbers ----------------
		ConfigCategory popups = b.getOrCreateCategory(cat("popups"));
		bool(eb, popups, "damageNumbers", c.damageNumbers, true, v -> c.damageNumbers = v);
		bool(eb, popups, "healNumbers", c.healNumbers, false, v -> c.healNumbers = v);
		slider(eb, popups, "damageNumberScale", Math.round(c.damageNumberScale * 100f), 100, 50, 300,
				v -> Text.literal(v + "%"), v -> c.damageNumberScale = v / 100f);

		return b.build();
	}

	// ------------------------------------------------------------------
	// helpers
	// ------------------------------------------------------------------

	private static void bool(ConfigEntryBuilder eb, ConfigCategory cat, String id,
							 boolean value, boolean def, Consumer<Boolean> save) {
		cat.addEntry(eb.startBooleanToggle(name(id), value)
				.setDefaultValue(def)
				.setTooltip(tip(id))
				.setSaveConsumer(save)
				.build());
	}

	private static void slider(ConfigEntryBuilder eb, ConfigCategory cat, String id,
							   int value, int def, int min, int max,
							   Function<Integer, Text> label, Consumer<Integer> save) {
		cat.addEntry(eb.startIntSlider(name(id), Math.max(min, Math.min(max, value)), min, max)
				.setDefaultValue(def)
				.setTooltip(tip(id))
				.setTextGetter(label)
				.setSaveConsumer(save)
				.build());
	}

	private static <T extends Enum<?>> void choice(ConfigEntryBuilder eb, ConfigCategory cat, String id,
												   Class<T> type, T value, T def, Consumer<T> save) {
		cat.addEntry(eb.startEnumSelector(name(id), type, value)
				.setDefaultValue(def)
				.setEnumNameProvider(e -> Text.translatable("enum.healthindicator." + e.name().toLowerCase(Locale.ROOT)))
				.setTooltip(tip(id))
				.setSaveConsumer(save)
				.build());
	}

	private static void strings(ConfigEntryBuilder eb, ConfigCategory cat, String id,
								List<String> value, Consumer<List<String>> save) {
		cat.addEntry(eb.startStrList(name(id), new ArrayList<>(value))
				.setDefaultValue(new ArrayList<String>())
				.setTooltip(tip(id))
				.setSaveConsumer(save)
				.build());
	}
}
