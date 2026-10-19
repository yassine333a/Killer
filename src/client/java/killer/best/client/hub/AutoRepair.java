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
 * sword in the menu that opens; below stopAt% it stops the killer right away.
 */
public final class AutoRepair {
	// Ticks to wait for the repair menu to open before giving up.
	private static final int OPEN_TIMEOUT = 60;
	// Ticks to let the menu's items arrive before clicking.
	private static final int SETTLE_TICKS = 4;
	// Ticks between the click and closing the menu.
	private static final int CLOSE_DELAY = 4;
	// Ticks before trying again after a repair attempt.
	private static final int COOLDOWN = 60;

	private enum State { IDLE, WAITING_FOR_MENU, CLOSING, COOLDOWN }

	private static State state = State.IDLE;
	private static int timer = 0;

	private AutoRepair() {
	}

	/** True while a repair is in progress, so the killer pauses its attacks. */
	public static boolean busy() {
		return state == State.WAITING_FOR_MENU || state == State.CLOSING;
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
			state = State.IDLE;
			return;
		}

		int durability = heldDurability(player);

		// Safety stop, even in the middle of a repair.
		if (config.running && durability >= 0 && durability < config.stopAt) {
			config.running = false;
			client.gui.hud.setOverlayMessage(Component.literal("Killer stopped: weapon at " + durability + "%")
					.withStyle(ChatFormatting.RED, ChatFormatting.BOLD), false);
		}

		switch (state) {
			case IDLE -> {
				if (config.autoRepair && config.running && durability >= 0 && durability < config.repairAt
						&& client.gui.screen() == null) {
					player.connection.sendCommand(config.repairCommand);
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
						state = State.CLOSING;
						timer = 0;
					} else if (timer >= OPEN_TIMEOUT) {
						player.closeContainer();
						fail(client, "no golden sword in the repair menu");
					}
				} else if (timer >= OPEN_TIMEOUT) {
					fail(client, "the repair menu did not open");
				}
			}
			case CLOSING -> {
				if (++timer >= CLOSE_DELAY) {
					if (client.gui.screen() instanceof AbstractContainerScreen<?>) {
						player.closeContainer();
					}
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

	private static void fail(Minecraft client, String reason) {
		client.gui.hud.setOverlayMessage(Component.literal("Auto repair failed: " + reason)
				.withStyle(ChatFormatting.GOLD), false);
		state = State.COOLDOWN;
		timer = 0;
	}
}
