package killer.best.client.hub;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * The Killer Hub: a dark window with every setting on the left and the live Mob Tracker on the right.
 */
public class HubScreen extends Screen {
	// Theme
	private static final int WINDOW = 0xF0121218;
	private static final int PANEL = 0xFF1A1A22;
	private static final int BORDER = 0xFF2C2C38;
	private static final int ACCENT = 0xFFE5484D;
	private static final int GREEN = 0xFF3DD68C;
	private static final int YELLOW = 0xFFF5D90A;
	private static final int TEXT = 0xFFEDEDF0;
	private static final int MUTED = 0xFF8B8B99;
	private static final int BUTTON = 0xFF262630;
	private static final int OFF_DOT = 0xFF4A4A58;

	private static final int PAD = 8;
	private static final int LEFT_WIDTH = 220;
	private static final int RIGHT_WIDTH = 160;
	private static final int GAP = 8;
	private static final int HEADER = 26;
	private static final int ROW = 22;
	private static final int TYPE_LINES = 6;

	private final List<Slider> manualSliders = new ArrayList<>();
	private final List<Label> labels = new ArrayList<>();

	private int winX;
	private int winY;
	private int winW;
	private int winH;
	private int leftX;
	private int rightX;
	private int contentY;
	private int trackerY;

	public HubScreen() {
		super(Component.literal("Killer Hub"));
	}

	@Override
	protected void init() {
		KillerConfig config = KillerConfig.INSTANCE;
		manualSliders.clear();
		labels.clear();

		winW = PAD + LEFT_WIDTH + GAP + RIGHT_WIDTH + PAD;
		winH = HEADER + 6 + 24 + 3 * 13 + 6 * ROW + 4 + 20 + PAD;
		winX = (this.width - winW) / 2;
		winY = Math.max(2, (this.height - winH) / 2);
		leftX = winX + PAD;
		rightX = leftX + LEFT_WIDTH + GAP;
		contentY = winY + HEADER + 6;

		int half = (LEFT_WIDTH - 4) / 2;
		int col2 = leftX + half + 4;
		int y = contentY;

		addRenderableWidget(new FlatButton(leftX, y, LEFT_WIDTH, 20,
				() -> config.running ? "■  STOP" : "▶  START",
				() -> config.running ? ACCENT : 0xFF2A8C5E, null,
				() -> config.running = !config.running));
		y += 24;

		section("ATTACK", y);
		y += 13;
		addRenderableWidget(new Slider(leftX, y, half, "Range", 1, 6, 0.5, config.range, v -> config.range = v));
		addRenderableWidget(new Slider(col2, y, half, "Max targets", 1, 50, 1, config.maxTargets, v -> config.maxTargets = (int) v));
		y += ROW;

		section("TIMING", y);
		y += 13;
		toggle(leftX, y, half, "Smart timing", () -> config.smartTiming, v -> {
			config.smartTiming = v;
			updateManualSliders();
		});
		toggle(col2, y, half, "Full charge", () -> config.fullCharge, v -> config.fullCharge = v);
		y += ROW;
		// Only used with smart timing off; smart timing picks the fastest timing that still deals damage.
		manualSliders.add(addRenderableWidget(new Slider(leftX, y, half, "Attacks/tick", 1, 10, 1, config.attacksPerTick, v -> config.attacksPerTick = (int) v)));
		manualSliders.add(addRenderableWidget(new Slider(col2, y, half, "Hit delay", 0, 20, 1, config.hitDelay, v -> config.hitDelay = (int) v)));
		updateManualSliders();
		y += ROW;

		section("TARGETS", y);
		y += 13;
		toggle(leftX, y, half, "Villagers", () -> config.villagers, v -> config.villagers = v);
		toggle(col2, y, half, "Blazes", () -> config.blazes, v -> config.blazes = v);
		y += ROW;
		toggle(leftX, y, half, "Hostile", () -> config.hostile, v -> config.hostile = v);
		toggle(col2, y, half, "Animals", () -> config.passive, v -> config.passive = v);
		y += ROW;
		toggle(leftX, y, half, "All mobs", () -> config.allMobs, v -> config.allMobs = v);
		toggle(col2, y, half, "Players", () -> config.players, v -> config.players = v);
		y += ROW + 4;

		toggle(leftX, y, half, "HUD", () -> config.showHud, v -> config.showHud = v);
		addRenderableWidget(new FlatButton(col2, y, half, 20, () -> "Done", () -> BUTTON, null, this::onClose));

		// Tracker panel: text is drawn in extractRenderState, only the reset button is a widget.
		trackerY = contentY;
		addRenderableWidget(new FlatButton(rightX, y, RIGHT_WIDTH, 20, () -> "Reset stats", () -> BUTTON, null, KillTracker::reset));
	}

	private void section(String name, int y) {
		labels.add(new Label(name, leftX, y + 2));
	}

	private void toggle(int x, int y, int w, String name, BooleanSupplier getter, Consumer<Boolean> setter) {
		addRenderableWidget(new FlatButton(x, y, w, 20, () -> name, () -> BUTTON, getter,
				() -> setter.accept(!getter.getAsBoolean())));
	}

	private void updateManualSliders() {
		for (Slider slider : manualSliders) {
			slider.setAuto(KillerConfig.INSTANCE.smartTiming);
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractBackground(graphics, mouseX, mouseY, delta);

		// Window with a red accent line on top
		graphics.fill(winX - 1, winY - 1, winX + winW + 1, winY + winH + 1, BORDER);
		graphics.fill(winX, winY, winX + winW, winY + winH, WINDOW);
		graphics.fill(winX, winY, winX + winW, winY + 2, ACCENT);

		// Header
		graphics.text(this.font, "KILLER HUB", winX + PAD, winY + 10, ACCENT, true);
		String hint = "K  hub   ·   R  start/stop";
		graphics.text(this.font, hint, winX + winW - PAD - this.font.width(hint), winY + 10, MUTED, false);
		graphics.fill(winX + PAD, winY + HEADER, winX + winW - PAD, winY + HEADER + 1, BORDER);

		// Tracker panel
		graphics.fill(rightX, trackerY, rightX + RIGHT_WIDTH, winY + winH - PAD - 24, PANEL);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);

		for (Label label : labels) {
			graphics.text(this.font, label.text(), label.x(), label.y(), MUTED, false);
			int lineX = label.x() + this.font.width(label.text()) + 6;
			graphics.fill(lineX, label.y() + 4, leftX + LEFT_WIDTH, label.y() + 5, BORDER);
		}

		extractTracker(graphics);
	}

	private void extractTracker(GuiGraphicsExtractor graphics) {
		KillerConfig config = KillerConfig.INSTANCE;
		int x = rightX + 8;
		int y = trackerY + 8;
		int valueX = rightX + RIGHT_WIDTH - 8;

		graphics.text(this.font, "MOB TRACKER", x, y, TEXT, true);
		String status = config.running ? "● ON" : "● OFF";
		graphics.text(this.font, status, valueX - this.font.width(status), y, config.running ? GREEN : ACCENT, false);
		y += 14;

		y = stat(graphics, "In range", String.valueOf(KillAura.inRange().size()), YELLOW, x, valueX, y);
		y = stat(graphics, "Hitting now", String.valueOf(KillAura.hittingNow()), ACCENT, x, valueX, y);
		y += 4;
		y = stat(graphics, "Kills", String.valueOf(KillTracker.totalKills()), TEXT, x, valueX, y);
		y = stat(graphics, "Speed", KillTracker.killsPerMinute() + "/min", TEXT, x, valueX, y);
		y = stat(graphics, "Per second", String.format("%.1f", KillTracker.killsPerSecond()), TEXT, x, valueX, y);
		y = stat(graphics, "Best", KillTracker.bestPerMinute() + "/min", TEXT, x, valueX, y);
		y = stat(graphics, "Average", String.format("%.1f", KillTracker.averagePerMinute()) + "/min", TEXT, x, valueX, y);

		y += 4;
		graphics.fill(x, y, valueX, y + 1, BORDER);
		y += 6;
		graphics.text(this.font, "KILLS PER MOB", x, y, MUTED, false);
		y += 12;

		List<Map.Entry<String, Integer>> types = KillTracker.byType();
		if (types.isEmpty()) {
			graphics.text(this.font, "nothing yet", x, y, OFF_DOT, false);
		}
		int most = types.isEmpty() ? 1 : types.get(0).getValue();
		for (Map.Entry<String, Integer> entry : types.stream().limit(TYPE_LINES).toList()) {
			// Bar behind each row, sized by its share of the top mob's kills
			int barW = (valueX - x) * entry.getValue() / most;
			graphics.fill(x - 2, y - 1, x - 2 + barW, y + 9, 0x40E5484D);
			graphics.text(this.font, entry.getKey(), x, y, TEXT, false);
			String count = String.valueOf(entry.getValue());
			graphics.text(this.font, count, valueX - this.font.width(count), y, TEXT, false);
			y += 11;
		}
	}

	private int stat(GuiGraphicsExtractor graphics, String name, String value, int color, int x, int valueX, int y) {
		graphics.text(this.font, name, x, y, MUTED, false);
		graphics.text(this.font, value, valueX - this.font.width(value), y, color, false);
		return y + 11;
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

	private record Label(String text, int x, int y) {
	}

	/** Flat dark button. With an indicator it becomes a toggle with a green/grey dot. */
	private static class FlatButton extends AbstractButton {
		private final Supplier<String> label;
		private final IntSupplier fill;
		private final BooleanSupplier indicator;
		private final Runnable action;

		FlatButton(int x, int y, int w, int h, Supplier<String> label, IntSupplier fill, BooleanSupplier indicator, Runnable action) {
			super(x, y, w, h, Component.literal(label.get()));
			this.label = label;
			this.fill = fill;
			this.indicator = indicator;
			this.action = action;
		}

		@Override
		protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
			Font font = Minecraft.getInstance().font;
			int x = getX();
			int y = getY();
			int bg = fill.getAsInt();
			if (isHovered()) {
				bg = brighten(bg);
			}
			graphics.fill(x, y, x + width, y + height, bg);
			graphics.fill(x, y + height - 1, x + width, y + height, 0x40000000);

			String text = label.get();
			int textY = y + (height - 8) / 2;
			if (indicator == null) {
				graphics.text(font, text, x + (width - font.width(text)) / 2, textY, TEXT, true);
				return;
			}
			boolean on = indicator.getAsBoolean();
			graphics.text(font, text, x + 8, textY, on ? TEXT : MUTED, false);
			// Switch on the right side
			int sw = 16;
			int sx = x + width - sw - 6;
			int sy = y + (height - 8) / 2;
			graphics.fill(sx, sy, sx + sw, sy + 8, on ? 0xFF1F6B47 : 0xFF33333F);
			int knob = on ? sx + sw - 8 : sx;
			graphics.fill(knob, sy, knob + 8, sy + 8, on ? GREEN : OFF_DOT);
		}

		@Override
		public void onPress(InputWithModifiers input) {
			action.run();
			setMessage(Component.literal(label.get()));
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
		}
	}

	/** Flat slider; shows "Auto" and locks when smart timing controls it. */
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
		public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
			Font font = Minecraft.getInstance().font;
			int x = getX();
			int y = getY();
			graphics.fill(x, y, x + width, y + height, isHovered() && !auto ? brighten(BUTTON) : BUTTON);
			if (!auto) {
				int filled = (int) (value * (width - 4));
				graphics.fill(x, y + height - 3, x + 2 + filled, y + height, ACCENT);
				graphics.fill(x + filled, y + 3, x + filled + 4, y + height - 3, TEXT);
			}
			String text = getMessage().getString();
			graphics.text(font, text, x + (width - font.width(text)) / 2, y + (height - 8) / 2, auto ? MUTED : TEXT, false);
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

	private static int brighten(int color) {
		int r = Math.min(255, ((color >> 16) & 0xFF) + 18);
		int g = Math.min(255, ((color >> 8) & 0xFF) + 18);
		int b = Math.min(255, (color & 0xFF) + 18);
		return (color & 0xFF000000) | (r << 16) | (g << 8) | b;
	}
}
