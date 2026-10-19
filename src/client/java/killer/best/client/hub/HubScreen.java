package killer.best.client.hub;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.Supplier;

/**
 * The Killer Hub: start/stop button and every setting on the left, live Mob Tracker on the right.
 */
public class HubScreen extends Screen {
	private static final int WIDTH = 220;
	private static final int PANEL_WIDTH = 170;
	private static final int GAP = 12;
	private static final int ROW = 22;
	private static final int TYPE_LINES = 8;

	private final List<StringWidget> trackerLines = new ArrayList<>();
	private final List<Slider> manualSliders = new ArrayList<>();

	public HubScreen() {
		super(Component.literal("Killer Hub"));
	}

	@Override
	protected void init() {
		KillerConfig config = KillerConfig.INSTANCE;
		trackerLines.clear();
		manualSliders.clear();

		int left = this.width / 2 - (WIDTH + GAP + PANEL_WIDTH) / 2;
		int half = (WIDTH - 4) / 2;
		int right = left + half + 4;
		int top = Math.max(6, this.height / 2 - 118);
		int y = top;

		addRenderableWidget(new StringWidget(left, y, WIDTH, 12,
				Component.literal("Killer Hub").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), this.font));
		y += 16;

		addRenderableWidget(Button.builder(startLabel(), button -> {
			config.running = !config.running;
			button.setMessage(startLabel());
		}).bounds(left, y, WIDTH, 20).build());
		y += ROW + 2;

		addRenderableWidget(new Slider(left, y, half, "Range", 1, 6, 0.5, config.range, v -> config.range = v));
		addRenderableWidget(new Slider(right, y, half, "Max targets", 1, 50, 1, config.maxTargets, v -> config.maxTargets = (int) v));
		y += ROW;
		// Only used with smart timing off; smart timing picks the fastest timing that still deals damage.
		manualSliders.add(addRenderableWidget(new Slider(left, y, half, "Attacks/tick", 1, 10, 1, config.attacksPerTick, v -> config.attacksPerTick = (int) v)));
		manualSliders.add(addRenderableWidget(new Slider(right, y, half, "Hit delay", 0, 20, 1, config.hitDelay, v -> config.hitDelay = (int) v)));
		updateManualSliders();
		y += ROW;
		addToggle(left, y, half, "Smart timing", () -> config.smartTiming, v -> {
			config.smartTiming = v;
			updateManualSliders();
		});
		addToggle(right, y, half, "Full charge", () -> config.fullCharge, v -> config.fullCharge = v);
		y += ROW + 4;

		addToggle(left, y, half, "Villagers", () -> config.villagers, v -> config.villagers = v);
		addToggle(right, y, half, "Blazes", () -> config.blazes, v -> config.blazes = v);
		y += ROW;
		addToggle(left, y, half, "Hostile", () -> config.hostile, v -> config.hostile = v);
		addToggle(right, y, half, "Animals", () -> config.passive, v -> config.passive = v);
		y += ROW;
		addToggle(left, y, half, "All mobs", () -> config.allMobs, v -> config.allMobs = v);
		addToggle(right, y, half, "Players", () -> config.players, v -> config.players = v);
		y += ROW + 4;

		addToggle(left, y, half, "HUD", () -> config.showHud, v -> config.showHud = v);
		addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
				.bounds(right, y, half, 20).build());

		initTracker(left + WIDTH + GAP, top);
	}

	private void initTracker(int x, int y) {
		addRenderableWidget(new StringWidget(x, y, PANEL_WIDTH, 12,
				Component.literal("Mob Tracker").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), this.font));
		y += 16;

		for (int i = 0; i < 7 + TYPE_LINES; i++) {
			StringWidget line = new StringWidget(x, y, PANEL_WIDTH, 10, Component.empty(), this.font);
			line.alignLeft();
			trackerLines.add(addRenderableWidget(line));
			y += 11;
		}
		y += 4;

		addRenderableWidget(Button.builder(Component.literal("Reset stats").withStyle(ChatFormatting.RED), button -> {
			KillTracker.reset();
			refreshTracker();
		}).bounds(x, y, PANEL_WIDTH, 20).build());

		refreshTracker();
	}

	@Override
	public void tick() {
		super.tick();
		refreshTracker();
	}

	private void refreshTracker() {
		List<Component> text = new ArrayList<>();
		int inRange = KillAura.inRange().size();
		int hittable = KillAura.hittable();
		text.add(stat("In range", inRange + (hittable < inRange ? " (hitting " + hittable + ")" : ""), ChatFormatting.YELLOW));
		text.add(stat("Kills", String.valueOf(KillTracker.totalKills()), ChatFormatting.WHITE));
		text.add(stat("Speed", KillTracker.killsPerMinute() + "/min  " + String.format("%.1f", KillTracker.killsPerSecond()) + "/s", ChatFormatting.WHITE));
		text.add(stat("Best", KillTracker.bestPerMinute() + "/min", ChatFormatting.WHITE));
		text.add(stat("Average", String.format("%.1f", KillTracker.averagePerMinute()) + "/min", ChatFormatting.WHITE));
		text.add(Component.empty());
		text.add(Component.literal("Kills per mob:").withStyle(ChatFormatting.GRAY));

		List<Map.Entry<String, Integer>> types = KillTracker.byType();
		if (types.isEmpty()) {
			text.add(Component.literal(" nothing yet").withStyle(ChatFormatting.DARK_GRAY));
		}
		for (Map.Entry<String, Integer> entry : types.stream().limit(TYPE_LINES).toList()) {
			text.add(stat(" " + entry.getKey(), String.valueOf(entry.getValue()), ChatFormatting.WHITE));
		}

		for (int i = 0; i < trackerLines.size(); i++) {
			trackerLines.get(i).setMessage(i < text.size() ? text.get(i) : Component.empty());
		}
	}

	private void updateManualSliders() {
		for (Slider slider : manualSliders) {
			slider.setAuto(KillerConfig.INSTANCE.smartTiming);
		}
	}

	private void addToggle(int x, int y, int w, String name, Supplier<Boolean> getter, Consumer<Boolean> setter) {
		addRenderableWidget(Button.builder(toggleLabel(name, getter.get()), button -> {
			setter.accept(!getter.get());
			button.setMessage(toggleLabel(name, getter.get()));
		}).bounds(x, y, w, 20).build());
	}

	private static Component stat(String name, String value, ChatFormatting color) {
		return Component.literal(name + ": ").withStyle(ChatFormatting.GRAY)
				.append(Component.literal(value).withStyle(color));
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
		private boolean auto = false;

		Slider(int x, int y, int w, String name, double min, double max, double step, double initial, DoubleConsumer setter) {
			super(x, y, w, 20, Component.empty(), (initial - min) / (max - min));
			this.name = name;
			this.min = min;
			this.max = max;
			this.step = step;
			this.setter = setter;
			updateMessage();
		}

		/** Locks the slider and shows "Auto" instead of its value. */
		void setAuto(boolean auto) {
			this.auto = auto;
			this.active = !auto;
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
			if (auto) {
				setMessage(Component.literal(name + ": Auto"));
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
