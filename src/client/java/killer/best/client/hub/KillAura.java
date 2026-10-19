package killer.best.client.hub;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Attacks every matching mob around the player, either spamming hits every tick
 * or, with smart timing, hitting each mob again as soon as its hurt cooldown ends.
 */
public final class KillAura {
	// Entity id -> player tick when smart timing may hit it again.
	private static final Map<Integer, Integer> nextHit = new HashMap<>();

	private KillAura() {
	}

	public static void tick(Minecraft client) {
		KillerConfig config = KillerConfig.INSTANCE;
		Player player = client.player;
		if (!config.running || player == null || client.level == null || client.gameMode == null || !player.isAlive()) {
			return;
		}

		double rangeSq = config.range * config.range;
		AABB box = player.getBoundingBox().inflate(config.range);
		List<LivingEntity> targets = client.level.getEntitiesOfClass(LivingEntity.class, box,
				entity -> entity != player && entity.isAlive() && player.distanceToSqr(entity) <= rangeSq && isTarget(entity, config));
		if (targets.isEmpty()) {
			return;
		}

		if (config.fullCharge && player.getAttackStrengthScale(0.5F) < 1.0F) {
			return;
		}

		targets.sort(Comparator.comparingDouble(player::distanceToSqr));
		int count = Math.min(targets.size(), config.maxTargets);
		int now = player.tickCount;
		boolean swung = false;

		if (config.smartTiming) {
			// Also drops stale entries after respawn, when tickCount starts over.
			nextHit.values().removeIf(tick -> tick < now - 100 || tick > now + 100);
			for (int i = 0; i < count; i++) {
				LivingEntity target = targets.get(i);
				if (now >= nextHit.getOrDefault(target.getId(), Integer.MIN_VALUE)) {
					attack(client, player, target);
					nextHit.put(target.getId(), now + config.hitDelay);
					swung = true;
				}
			}
		} else {
			for (int hit = 0; hit < config.attacksPerTick; hit++) {
				for (int i = 0; i < count; i++) {
					LivingEntity target = targets.get(i);
					if (target.isAlive()) {
						attack(client, player, target);
						swung = true;
					}
				}
			}
		}

		if (swung) {
			player.swing(InteractionHand.MAIN_HAND);
		}
	}

	private static void attack(Minecraft client, Player player, LivingEntity target) {
		client.gameMode.attack(player, target);
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
