package killer.best.client.hub;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.Supplier;

/**
 * The Killer Hub: start/stop button plus every setting of the kill aura.
 */
public class HubScreen extends Screen {
	private static final int WIDTH = 220;
	private static final int ROW = 24;

	public HubScreen() {
		super(Component.literal("Killer Hub"));
	}

	@Override
	protected void init() {
		KillerConfig config = KillerConfig.INSTANCE;
		int left = this.width / 2 - WIDTH / 2;
		int half = (WIDTH - 4) / 2;
		int y = Math.max(10, this.height / 2 - 132);

		addRenderableWidget(new StringWidget(left, y, WIDTH, 12,
				Component.literal("Killer Hub").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), this.font));
		y += 18;

		addRenderableWidget(Button.builder(startLabel(), button -> {
			config.running = !config.running;
			button.setMessage(startLabel());
		}).bounds(left, y, WIDTH, 20).build());
		y += ROW + 4;

		addRenderableWidget(new Slider(left, y, WIDTH, "Range", 1, 6, 0.5, config.range, v -> config.range = v));
		y += ROW;
		addRenderableWidget(new Slider(left, y, WIDTH, "Attacks per tick", 1, 10, 1, config.attacksPerTick, v -> config.attacksPerTick = (int) v));
		y += ROW;
		addRenderableWidget(new Slider(left, y, WIDTH, "Max targets", 1, 50, 1, config.maxTargets, v -> config.maxTargets = (int) v));
		y += ROW + 4;

		addToggle(left, y, half, "Villagers", () -> config.villagers, v -> config.villagers = v);
		addToggle(left + half + 4, y, half, "Blazes", () -> config.blazes, v -> config.blazes = v);
		y += ROW;
		addToggle(left, y, half, "Hostile", () -> config.hostile, v -> config.hostile = v);
		addToggle(left + half + 4, y, half, "Animals", () -> config.passive, v -> config.passive = v);
		y += ROW;
		addToggle(left, y, half, "All mobs", () -> config.allMobs, v -> config.allMobs = v);
		addToggle(left + half + 4, y, half, "Players", () -> config.players, v -> config.players = v);
		y += ROW + 4;

		addRenderableWidget(Button.builder(Component.literal("Mob Tracker").withStyle(ChatFormatting.AQUA),
				button -> {
					KillerConfig.save();
					Minecraft.getInstance().gui.setScreen(new TrackerScreen());
				}).bounds(left, y, half, 20).build());
		addToggle(left + half + 4, y, half, "HUD", () -> config.showHud, v -> config.showHud = v);
		y += ROW;

		addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
				.bounds(left, y, WIDTH, 20).build());
	}

	private void addToggle(int x, int y, int w, String name, Supplier<Boolean> getter, Consumer<Boolean> setter) {
		addRenderableWidget(Button.builder(toggleLabel(name, getter.get()), button -> {
			setter.accept(!getter.get());
			button.setMessage(toggleLabel(name, getter.get()));
		}).bounds(x, y, w, 20).build());
	}

	private static Component startLabel() {
		return KillerConfig.INSTANCE.running
				? Component.literal("STOP").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
				: Component.literal("START").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD);
	}

	private static Component toggleLabel(String name, boolean on) {
		return Component.literal(name + ": ").append(on
				? Component.literal("ON").withStyle(ChatFormatting.GREEN)
				: Component.literal("OFF").withStyle(ChatFormatting.RED));
	}

	@Override
	public void onClose() {
		KillerConfig.save();
		super.onClose();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private static class Slider extends AbstractSliderButton {
		private final String name;
		private final double min;
		private final double max;
		private final double step;
		private final DoubleConsumer setter;

		Slider(int x, int y, int w, String name, double min, double max, double step, double initial, DoubleConsumer setter) {
			super(x, y, w, 20, Component.empty(), (initial - min) / (max - min));
			this.name = name;
			this.min = min;
			this.max = max;
			this.step = step;
			this.setter = setter;
			updateMessage();
		}

		private double current() {
			double raw = min + value * (max - min);
			return Math.max(min, Math.min(max, Math.round(raw / step) * step));
		}

		@Override
		protected void updateMessage() {
			if (name == null) {
				return;
			}
			double v = current();
			String text = step < 1 ? String.format("%.1f", v) : String.valueOf((int) v);
			setMessage(Component.literal(name + ": " + text));
		}

		@Override
		protected void applyValue() {
			setter.accept(current());
		}
	}
}
