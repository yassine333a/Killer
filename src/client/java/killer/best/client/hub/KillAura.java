package killer.best.client.hub;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;

/**
 * Attacks every matching mob around the player, several times per tick.
 */
public final class KillAura {
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

		targets.sort(Comparator.comparingDouble(player::distanceToSqr));
		int count = Math.min(targets.size(), config.maxTargets);

		for (int hit = 0; hit < config.attacksPerTick; hit++) {
			for (int i = 0; i < count; i++) {
				LivingEntity target = targets.get(i);
				if (target.isAlive()) {
					client.gameMode.attack(player, target);
				}
			}
		}
		player.swing(InteractionHand.MAIN_HAND);
	}

	private static boolean isTarget(LivingEntity entity, KillerConfig config) {
		EntityType<?> type = entity.getType();
		if (type == EntityType.ARMOR_STAND) {
			return false;
		}
		if (entity instanceof Player) {
			return config.players;
		}
		if (config.allMobs) {
			return true;
		}
		if (config.villagers && (type == EntityType.VILLAGER || type == EntityType.WANDERING_TRADER)) {
			return true;
		}
		if (config.blazes && type == EntityType.BLAZE) {
			return true;
		}
		MobCategory category = type.getCategory();
		if (config.hostile && category == MobCategory.MONSTER) {
			return true;
		}
		return config.passive && category != MobCategory.MONSTER && category != MobCategory.MISC;
	}
}
