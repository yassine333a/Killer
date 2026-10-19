package killer.best.client.hub;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Counts the mobs the kill aura finishes off, per mob type, and how fast.
 */
public final class KillTracker {
	// A mob that dies this long after our last hit still counts as our kill.
	private static final long HIT_MEMORY_MS = 3000;
	private static final long SPEED_WINDOW_MS = 60_000;

	private static final Map<Integer, Long> lastHit = new HashMap<>();
	private static final Map<String, Integer> killsByType = new LinkedHashMap<>();
	private static final Deque<Long> recentKills = new ArrayDeque<>();

	private static int totalKills = 0;
	private static int bestPerMinute = 0;
	private static long firstKillTime = 0;

	private KillTracker() {
	}

	public static void onAttack(Entity target) {
		lastHit.put(target.getId(), System.currentTimeMillis());
	}

	public static void tick(Minecraft client) {
		long now = System.currentTimeMillis();
		if (client.level == null) {
			lastHit.clear();
			return;
		}

		Iterator<Map.Entry<Integer, Long>> it = lastHit.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Integer, Long> entry = it.next();
			Entity entity = client.level.getEntity(entry.getKey());
			if (entity instanceof LivingEntity living && living.isDeadOrDying()) {
				recordKill(living, now);
				it.remove();
			} else if (entity == null || now - entry.getValue() > HIT_MEMORY_MS) {
				it.remove();
			}
		}

		while (!recentKills.isEmpty() && now - recentKills.peekFirst() > SPEED_WINDOW_MS) {
			recentKills.pollFirst();
		}
	}

	private static void recordKill(LivingEntity entity, long now) {
		String name = entity.getType().getDescription().getString();
		killsByType.merge(name, 1, Integer::sum);
		totalKills++;
		if (firstKillTime == 0) {
			firstKillTime = now;
		}
		recentKills.addLast(now);
		bestPerMinute = Math.max(bestPerMinute, recentKills.size());
	}

	public static void reset() {
		lastHit.clear();
		killsByType.clear();
		recentKills.clear();
		totalKills = 0;
		bestPerMinute = 0;
		firstKillTime = 0;
	}

	public static int totalKills() {
		return totalKills;
	}

	/** Kills in the last 60 seconds. */
	public static int killsPerMinute() {
		return recentKills.size();
	}

	public static int bestPerMinute() {
		return bestPerMinute;
	}

	/** Kills per second over the last 10 seconds. */
	public static double killsPerSecond() {
		long now = System.currentTimeMillis();
		int count = 0;
		for (long time : recentKills) {
			if (now - time <= 10_000) {
				count++;
			}
		}
		return count / 10.0;
	}

	/** Average kills per minute since the first kill. */
	public static double averagePerMinute() {
		if (firstKillTime == 0) {
			return 0;
		}
		double minutes = Math.max(1.0 / 60, (System.currentTimeMillis() - firstKillTime) / 60_000.0);
		return totalKills / minutes;
	}

	/** Mob types sorted by kill count, most first. */
	public static List<Map.Entry<String, Integer>> byType() {
		List<Map.Entry<String, Integer>> list = new ArrayList<>(killsByType.entrySet());
		list.sort((a, b) -> b.getValue() - a.getValue());
		return list;
	}
}
