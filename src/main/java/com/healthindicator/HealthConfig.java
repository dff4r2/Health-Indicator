package com.healthindicator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** All user settings. Saved to config/healthindicator.json. */
public class HealthConfig {

	public enum DisplayStyle { HEARTS, HEARTS_AND_NUMBER, NUMBER, PERCENT, BAR }

	public enum Overflow { COMPACT, SCALE_TO_FIT }

	public enum HudPosition { TOP_CENTER, TOP_LEFT, TOP_RIGHT, BOTTOM_CENTER, BOTTOM_LEFT, BOTTOM_RIGHT }

	// ---- General ----
	public boolean enabled = true;
	public boolean worldIndicator = true;

	// ---- Style (above entities) ----
	public DisplayStyle style = DisplayStyle.HEARTS_AND_NUMBER;
	public float scale = 0.8f;
	public float opacity = 1.0f;
	public float verticalOffset = 0.25f;
	public boolean seeThroughWalls = false;
	public boolean distanceScaling = true;
	public boolean hideWhenSneaking = true;

	// ---- Hearts ----
	public float hpPerHeart = 2.0f;
	public int heartsPerRow = 10;
	public int maxHearts = 20;
	public Overflow overflow = Overflow.COMPACT;
	public boolean showEmptyContainers = true;
	public boolean showAbsorption = true;
	public boolean hardcoreHearts = false;

	// ---- Effects ----
	public boolean effectHearts = true;
	public boolean regenAnimation = true;
	public boolean hurtShake = true;
	public boolean lowHealthBlink = true;
	public float lowHealthThreshold = 0.25f;

	// ---- Filters ----
	public boolean showPlayers = true;
	public boolean showHostile = true;
	public boolean showPassive = true;
	public boolean showSelf = false;
	public boolean onlyDamaged = false;
	public boolean onlyLookingAt = false;
	public int maxDistance = 32;
	public List<String> alwaysShow = new ArrayList<>();
	public List<String> blacklist = new ArrayList<>();

	// ---- Target panel (HUD) ----
	public boolean hudPanel = true;
	public DisplayStyle hudStyle = DisplayStyle.HEARTS_AND_NUMBER;
	public HudPosition hudPosition = HudPosition.TOP_CENTER;
	public int hudOffsetX = 0;
	public int hudOffsetY = 0;
	public float hudScale = 1.0f;
	public boolean hudShowName = true;
	public boolean hudShowArmor = true;
	public boolean hudShowDistance = true;
	public boolean hudBackground = true;
	public int hudRange = 24;

	// ---- Damage numbers ----
	public boolean damageNumbers = true;
	public boolean healNumbers = false;
	public float damageNumberScale = 1.0f;

	// ------------------------------------------------------------------

	private static final Logger LOGGER = LoggerFactory.getLogger("healthindicator");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static HealthConfig instance;

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("healthindicator.json");
	}

	public static HealthConfig get() {
		if (instance == null) load();
		return instance;
	}

	public static void load() {
		HealthConfig loaded = null;
		try {
			Path p = path();
			if (Files.exists(p)) {
				loaded = GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), HealthConfig.class);
			}
		} catch (Exception e) {
			LOGGER.warn("Could not read config, using defaults", e);
		}
		instance = loaded != null ? loaded : new HealthConfig();
		instance.sanitize();
		instance.save();
	}

	public void save() {
		try {
			Files.writeString(path(), GSON.toJson(this), StandardCharsets.UTF_8);
		} catch (Exception e) {
			LOGGER.warn("Could not save config", e);
		}
	}

	/** Fixes missing/out-of-range values (e.g. after hand-editing the file). */
	public void sanitize() {
		if (style == null) style = DisplayStyle.HEARTS_AND_NUMBER;
		if (hudStyle == null) hudStyle = DisplayStyle.HEARTS_AND_NUMBER;
		if (overflow == null) overflow = Overflow.COMPACT;
		if (hudPosition == null) hudPosition = HudPosition.TOP_CENTER;
		if (alwaysShow == null) alwaysShow = new ArrayList<>();
		if (blacklist == null) blacklist = new ArrayList<>();

		scale = cl(scale, 0.3f, 3f);
		opacity = cl(opacity, 0.1f, 1f);
		verticalOffset = cl(verticalOffset, -1f, 3f);
		hpPerHeart = cl(hpPerHeart, 0.5f, 20f);
		heartsPerRow = cl(heartsPerRow, 1, 20);
		maxHearts = cl(maxHearts, 1, 100);
		lowHealthThreshold = cl(lowHealthThreshold, 0.05f, 0.6f);
		maxDistance = cl(maxDistance, 4, 128);
		hudOffsetX = cl(hudOffsetX, -400, 400);
		hudOffsetY = cl(hudOffsetY, -300, 300);
		hudScale = cl(hudScale, 0.5f, 3f);
		hudRange = cl(hudRange, 4, 64);
		damageNumberScale = cl(damageNumberScale, 0.5f, 3f);
	}

	private static float cl(float v, float min, float max) {
		return Math.max(min, Math.min(max, v));
	}

	private static int cl(int v, int min, int max) {
		return Math.max(min, Math.min(max, v));
	}
}
