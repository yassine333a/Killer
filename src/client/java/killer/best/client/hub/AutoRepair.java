package killer.best.client.hub;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Keeps the held sword/axe alive: below repairAt% it runs the repair command and clicks the golden
 * sword in the menu that opens, retrying up to repairTries times. Below stopAt% it stops the
 * killer right away, keeps trying to repair, and starts the killer again once the weapon is fixed.
 */
public final class AutoRepair {
	// Ticks to wait for the repair menu to open before trying again.
	private static final int OPEN_TIMEOUT = 60;
	// Ticks to let the menu's items arrive before clicking.
	private static final int SETTLE_TICKS = 4;
	// Ticks after a click to see whether the weapon got repaired.
	private static final int CLICK_CHECK_TICKS = 6;
	// Clicks on the golden sword per menu before closing it and running the command again.
	private static final int CLICKS_PER_MENU = 3;
	// Ticks between attempts.
	private static final int COOLDOWN = 20;

	private enum State { IDLE, WAITING_FOR_MENU, CLICKED, COOLDOWN }

	private static State state = State.IDLE;
	private static int timer = 0;
	private static int clicks = 0;
	private static int attempts = 0;
	// Set when the killer was stopped for low durability, so it starts again after a repair.
	private static boolean stoppedForDurability = false;
	private static boolean wasRunning = false;

	private AutoRepair() {
	}

	/** True while a repair is in progress, so the killer pauses its attacks. */
	public static boolean busy() {
		return state == State.WAITING_FOR_MENU || state == State.CLICKED;
	}

	/** Durability left on the held sword/axe in percent, or -1 when not holding one. */
	public static int heldDurability(LocalPlayer player) {
		ItemStack stack = player.getMainHandItem();
		if (!isWeapon(stack)) {
			return -1;
		}
		int max = stack.getMaxDamage();
		return (int) Math.floor(100.0 * (max - stack.getDamageValue()) / max);
	}

	private static boolean isWeapon(ItemStack stack) {
		return stack.isDamageableItem() && (stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES));
	}

	public static void tick(Minecraft client) {
		KillerConfig config = KillerConfig.INSTANCE;
		LocalPlayer player = client.player;
		if (player == null || client.gameMode == null) {
			reset();
			return;
		}

		int durability = heldDurability(player);
		boolean needsRepair = durability >= 0 && durability < config.repairAt;

		// Safety stop, even in the middle of a repair.
		if (config.running && durability >= 0 && durability < config.stopAt) {
			config.running = false;
			stoppedForDurability = true;
			message(client, "Killer stopped: weapon at " + durability + "%", ChatFormatting.RED);
		}

		// The user started or stopped the killer themselves: forget the safety stop.
		if (config.running || !config.autoRepair) {
			stoppedForDurability = false;
		}

		// Pressing START again gives auto repair a fresh set of tries.
		if (config.running && !wasRunning) {
			attempts = 0;
		}
		wasRunning = config.running;

		if (durability < 0) {
			attempts = 0;
		} else if (!needsRepair && state != State.CLICKED) {
			if (attempts > 0 || stoppedForDurability) {
				message(client, "Weapon repaired (" + durability + "%)", ChatFormatting.GREEN);
				if (stoppedForDurability) {
					config.running = true;
				}
			}
			attempts = 0;
			stoppedForDurability = false;
		}

		switch (state) {
			case IDLE -> {
				if (config.autoRepair && (config.running || stoppedForDurability) && needsRepair
						&& attempts < config.repairTries && client.gui.screen() == null) {
					player.connection.sendCommand(config.repairCommand);
					attempts++;
					clicks = 0;
					state = State.WAITING_FOR_MENU;
					timer = 0;
				}
			}
			case WAITING_FOR_MENU -> {
				timer++;
				if (client.gui.screen() instanceof AbstractContainerScreen<?> screen && timer >= SETTLE_TICKS) {
					AbstractContainerMenu menu = screen.getMenu();
					Slot slot = findGoldenSword(menu);
					if (slot != null) {
						client.gameMode.handleContainerInput(menu.containerId, slot.index, 0, ContainerInput.PICKUP, player);
						clicks++;
						state = State.CLICKED;
						timer = 0;
					} else if (timer >= OPEN_TIMEOUT) {
						player.closeContainer();
						retry(client, "no golden sword in the menu");
					}
				} else if (timer >= OPEN_TIMEOUT) {
					retry(client, "menu did not open");
				}
			}
			case CLICKED -> {
				if (++timer < CLICK_CHECK_TICKS) {
					return;
				}
				if (needsRepair && clicks < CLICKS_PER_MENU && client.gui.screen() instanceof AbstractContainerScreen<?>) {
					// Not repaired yet: click the golden sword again.
					state = State.WAITING_FOR_MENU;
					timer = SETTLE_TICKS;
					return;
				}
				if (client.gui.screen() instanceof AbstractContainerScreen<?>) {
					player.closeContainer();
				}
				if (needsRepair) {
					retry(client, "still at " + durability + "%");
				} else {
					state = State.COOLDOWN;
					timer = 0;
				}
			}
			case COOLDOWN -> {
				if (++timer >= COOLDOWN) {
					state = State.IDLE;
				}
			}
		}
	}

	private static Slot findGoldenSword(AbstractContainerMenu menu) {
		for (Slot slot : menu.slots) {
			// Only the menu's own slots, not the player's inventory below it.
			if (!(slot.container instanceof Inventory) && slot.getItem().is(Items.GOLDEN_SWORD)) {
				return slot;
			}
		}
		return null;
	}

	private static void retry(Minecraft client, String reason) {
		int tries = KillerConfig.INSTANCE.repairTries;
		message(client, "Repair try " + attempts + "/" + tries + " failed (" + reason + ")"
				+ (attempts < tries ? ", trying again" : ", giving up"), ChatFormatting.GOLD);
		state = State.COOLDOWN;
		timer = 0;
	}

	private static void message(Minecraft client, String text, ChatFormatting color) {
		client.gui.hud.setOverlayMessage(Component.literal(text).withStyle(color), false);
	}

	private static void reset() {
		state = State.IDLE;
		timer = 0;
		clicks = 0;
		attempts = 0;
		stoppedForDurability = false;
	}
}
