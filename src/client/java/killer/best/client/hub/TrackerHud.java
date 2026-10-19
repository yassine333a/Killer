package killer.best.client.hub;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Small overlay in the top-left corner: mobs in range right now and kills so far.
 */
public final class TrackerHud {
	private static final int TYPE_LINES = 5;
	private static final int GREEN = 0xff55ff55;
	private static final int RED = 0xffff5555;
	private static final int YELLOW = 0xffffff55;
	private static final int WHITE = 0xffffffff;

	private record Line(String text, int color) {
	}

	private TrackerHud() {
	}

	public static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		KillerConfig config = KillerConfig.INSTANCE;
		int inRange = KillAura.inRange().size();
		if (!config.showHud || (!config.running && inRange == 0 && KillTracker.totalKills() == 0)) {
			return;
		}

		List<Line> lines = new ArrayList<>();
		lines.add(new Line("Killer: " + (config.running ? "ON" : "OFF"), config.running ? GREEN : RED));
		int hittable = KillAura.hittable();
		lines.add(new Line("In range: " + inRange + (hittable < inRange ? " (hitting " + hittable + ")" : ""), YELLOW));
		for (Map.Entry<String, Integer> entry : KillAura.inRangeByType().stream().limit(TYPE_LINES).toList()) {
			lines.add(new Line(" " + entry.getKey() + ": " + entry.getValue(), YELLOW));
		}
		lines.add(new Line("Kills: " + KillTracker.totalKills(), WHITE));
		lines.add(new Line("Speed: " + KillTracker.killsPerMinute() + "/min", WHITE));
		for (Map.Entry<String, Integer> entry : KillTracker.byType().stream().limit(TYPE_LINES).toList()) {
			lines.add(new Line(" " + entry.getKey() + ": " + entry.getValue(), WHITE));
		}

		Font font = Minecraft.getInstance().font;
		int width = 0;
		for (Line line : lines) {
			width = Math.max(width, font.width(line.text()));
		}
		int x = 4;
		int y = 4;
		graphics.fill(x - 2, y - 2, x + width + 2, y + lines.size() * 10, 0x80000000);
		for (int i = 0; i < lines.size(); i++) {
			graphics.text(font, lines.get(i).text(), x, y + i * 10, lines.get(i).color(), true);
		}
	}
}
