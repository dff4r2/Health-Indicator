package com.healthindicator;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/** Finds the living entity under the crosshair (further than vanilla's reach). */
final class TargetFinder {
	private TargetFinder() {}

	static LivingEntity find(MinecraftClient mc, float tickDelta, double range) {
		Entity cam = mc.getCameraEntity();
		if (cam == null || mc.world == null) return null;

		Vec3d start = cam.getCameraPosVec(tickDelta);
		Vec3d dir = cam.getRotationVec(tickDelta);
		Vec3d end = start.add(dir.multiply(range));
		Box box = cam.getBoundingBox().stretch(dir.multiply(range)).expand(1.0);

		EntityHitResult hit = ProjectileUtil.raycast(
				cam, start, end, box,
				e -> e instanceof LivingEntity && !(e instanceof ArmorStandEntity) && !e.isSpectator() && e.canHit(),
				range * range
		);
		if (hit == null) return null;

		// Ignore targets hidden behind blocks.
		BlockHitResult block = mc.world.raycast(new RaycastContext(
				start, hit.getPos(),
				RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, cam));
		if (block.getType() != HitResult.Type.MISS) return null;

		return hit.getEntity() instanceof LivingEntity living ? living : null;
	}
}
