package killer.best.client.hub;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Attacks every matching mob around the player. With smart timing each mob is hit again the moment
 * its hurt cooldown ends; otherwise it gets Attacks/tick hits, then waits Hit delay ticks.
 */
public final class KillAura {
	private static final int SMART_RETRY_TICKS = 2;

	// Entity id -> player tick when it may be hit again.
	private static final Map<Integer, Integer> nextHit = new HashMap<>();

	private static final int HITTING_WINDOW_TICKS = 20;

	private static List<LivingEntity> inRange = List.of();
	// Entity id -> player tick of our last hit on it, for the "Hitting" count.
	private static final Map<Integer, Integer> lastAttack = new HashMap<>();
	private static int hittingNow = 0;

	private KillAura() {
	}

	/** Mobs in range that the current settings would hit, closest first. */
	public static List<LivingEntity> inRange() {
		return inRange;
	}

	/** How many mobs in range were actually hit during the last second. */
	public static int hittingNow() {
		return hittingNow;
	}

	/** Mobs in range grouped by type name, most first. */
	public static List<Map.Entry<String, Integer>> inRangeByType() {
		Map<String, Integer> counts = new HashMap<>();
		for (LivingEntity entity : inRange) {
			counts.merge(entity.getType().getDescription().getString(), 1, Integer::sum);
		}
		List<Map.Entry<String, Integer>> list = new ArrayList<>(counts.entrySet());
		list.sort((a, b) -> b.getValue() - a.getValue());
		return list;
	}

	public static void tick(Minecraft client) {
		KillerConfig config = KillerConfig.INSTANCE;
		Player player = client.player;
		if (player == null || client.level == null || client.gameMode == null || !player.isAlive()) {
			inRange = List.of();
			lastAttack.clear();
			hittingNow = 0;
			return;
		}

		double rangeSq = config.range * config.range;
		AABB box = player.getBoundingBox().inflate(config.range);
		List<LivingEntity> targets = client.level.getEntitiesOfClass(LivingEntity.class, box,
				entity -> entity != player && entity.isAlive() && player.distanceToSqr(entity) <= rangeSq && isTarget(entity, config));
		targets.sort(Comparator.comparingDouble(player::distanceToSqr));
		// Counted even while stopped, so the HUD shows what START would hit.
		inRange = targets;
		updateHittingNow(player.tickCount);
		if (!config.running || targets.isEmpty()) {
			return;
		}

		if (config.fullCharge && player.getAttackStrengthScale(0.5F) < 1.0F) {
			return;
		}

		int count = Math.min(targets.size(), config.maxTargets);
		int now = player.tickCount;
		boolean swung = false;

		// Also drops stale entries after respawn, when tickCount starts over.
		nextHit.values().removeIf(tick -> tick < now - 100 || tick > now + 100);
		for (int i = 0; i < count; i++) {
			LivingEntity target = targets.get(i);
			if (now < nextHit.getOrDefault(target.getId(), Integer.MIN_VALUE)) {
				continue;
			}
			if (config.smartTiming) {
				// hurtTime counts down the mob's hurt cooldown (synced from the server); hits during it do
				// next to nothing, so hit the moment it reaches 0. The short wait covers the server's reply.
				if (target.hurtTime > 0) {
					continue;
				}
				attack(client, player, target);
				nextHit.put(target.getId(), now + SMART_RETRY_TICKS);
			} else {
				for (int hit = 0; hit < config.attacksPerTick; hit++) {
					attack(client, player, target);
				}
				nextHit.put(target.getId(), now + config.hitDelay);
			}
			swung = true;
		}

		if (swung) {
			player.swing(InteractionHand.MAIN_HAND);
			updateHittingNow(now);
		}
	}

	private static void updateHittingNow(int now) {
		lastAttack.values().removeIf(tick -> now - tick > HITTING_WINDOW_TICKS || tick > now);
		int count = 0;
		for (LivingEntity entity : inRange) {
			if (lastAttack.containsKey(entity.getId())) {
				count++;
			}
		}
		hittingNow = count;
	}

	private static void attack(Minecraft client, Player player, LivingEntity target) {
		client.gameMode.attack(player, target);
		lastAttack.put(target.getId(), player.tickCount);
		KillTracker.onAttack(target);
	}

	private static boolean isTarget(LivingEntity entity, KillerConfig config) {
		EntityType<?> type = entity.getType();
		if (type == EntityTypes.ARMOR_STAND) {
			return false;
		}
		if (entity instanceof Player) {
			return config.players;
		}
		if (config.allMobs) {
			return true;
		}
		if (config.villagers && (type == EntityTypes.VILLAGER || type == EntityTypes.WANDERING_TRADER)) {
			return true;
		}
		if (config.blazes && type == EntityTypes.BLAZE) {
			return true;
		}
		MobCategory category = type.getCategory();
		if (config.hostile && category == MobCategory.MONSTER) {
			return true;
		}
		return config.passive && category != MobCategory.MONSTER && category != MobCategory.MISC;
	}
}
