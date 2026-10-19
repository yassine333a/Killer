package killer.best.client.hub;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Live kill stats: total, speed and kills per mob type.
 */
public class TrackerScreen extends Screen {
	private static final int WIDTH = 220;
	private static final int TYPE_LINES = 10;

	private final List<StringWidget> lines = new ArrayList<>();

	public TrackerScreen() {
		super(Component.literal("Mob Tracker"));
	}

	@Override
	protected void init() {
		lines.clear();
		int left = this.width / 2 - WIDTH / 2;
		int y = Math.max(10, this.height / 2 - 120);

		addRenderableWidget(new StringWidget(left, y, WIDTH, 12,
				Component.literal("Mob Tracker").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), this.font));
		y += 18;

		for (int i = 0; i < 6 + TYPE_LINES; i++) {
			lines.add(addRenderableWidget(new StringWidget(left, y, WIDTH, 10, Component.empty(), this.font)));
			y += 12;
		}
		y += 6;

		int half = (WIDTH - 4) / 2;
		addRenderableWidget(Button.builder(Component.literal("Reset").withStyle(ChatFormatting.RED), button -> {
			KillTracker.reset();
			refresh();
		}).bounds(left, y, half, 20).build());
		addRenderableWidget(Button.builder(Component.literal("Back"), button -> onClose())
				.bounds(left + half + 4, y, half, 20).build());

		refresh();
	}

	@Override
	public void tick() {
		super.tick();
		refresh();
	}

	private void refresh() {
		List<Component> text = new ArrayList<>();
		int inRange = KillAura.inRange().size();
		text.add(stat("In range now", inRange + (KillAura.hittable() < inRange ? " (hitting " + KillAura.hittable() + ")" : "")));
		text.add(stat("Total kills", String.valueOf(KillTracker.totalKills())));
		text.add(stat("Speed", KillTracker.killsPerMinute() + " kills/min  (" + String.format("%.1f", KillTracker.killsPerSecond()) + "/s)"));
		text.add(stat("Best speed", KillTracker.bestPerMinute() + " kills/min"));
		text.add(stat("Average", String.format("%.1f", KillTracker.averagePerMinute()) + " kills/min"));
		text.add(Component.literal("Kills per mob:").withStyle(ChatFormatting.YELLOW));

		List<Map.Entry<String, Integer>> types = KillTracker.byType();
		if (types.isEmpty()) {
			text.add(Component.literal("  nothing yet").withStyle(ChatFormatting.GRAY));
		}
		for (int i = 0; i < Math.min(types.size(), TYPE_LINES); i++) {
			Map.Entry<String, Integer> entry = types.get(i);
			text.add(Component.literal("  " + entry.getKey() + ": ")
					.append(Component.literal(String.valueOf(entry.getValue())).withStyle(ChatFormatting.WHITE)));
		}

		for (int i = 0; i < lines.size(); i++) {
			lines.get(i).setMessage(i < text.size() ? text.get(i) : Component.empty());
		}
	}

	private static Component stat(String name, String value) {
		return Component.literal(name + ": ").withStyle(ChatFormatting.GRAY)
				.append(Component.literal(value).withStyle(ChatFormatting.WHITE));
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().gui.setScreen(new HubScreen());
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
