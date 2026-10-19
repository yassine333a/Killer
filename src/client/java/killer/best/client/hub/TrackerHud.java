package killer.best.client.hub;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Small kill counter in the top-left corner of the screen.
 */
public final class TrackerHud {
	private static final int TYPE_LINES = 5;

	private TrackerHud() {
	}

	public static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		KillerConfig config = KillerConfig.INSTANCE;
		if (!config.showHud || (!config.running && KillTracker.totalKills() == 0)) {
			return;
		}

		List<String> lines = new ArrayList<>();
		lines.add("Killer: " + (config.running ? "ON" : "OFF"));
		lines.add("Kills: " + KillTracker.totalKills());
		lines.add("Speed: " + KillTracker.killsPerMinute() + "/min");
		List<Map.Entry<String, Integer>> types = KillTracker.byType();
		for (int i = 0; i < Math.min(types.size(), TYPE_LINES); i++) {
			lines.add(" " + types.get(i).getKey() + ": " + types.get(i).getValue());
		}

		Font font = Minecraft.getInstance().font;
		int width = 0;
		for (String line : lines) {
			width = Math.max(width, font.width(line));
		}
		int x = 4;
		int y = 4;
		graphics.fill(x - 2, y - 2, x + width + 2, y + lines.size() * 10, 0x80000000);
		for (int i = 0; i < lines.size(); i++) {
			int color = i == 0 ? (config.running ? 0xff55ff55 : 0xffff5555) : 0xffffffff;
			graphics.text(font, lines.get(i), x, y + i * 10, color, true);
		}
	}
}
