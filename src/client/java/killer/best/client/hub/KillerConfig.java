package killer.best.client.hub;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import killer.best.Killer;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * All settings shown in the Killer Hub. Saved to config/killer.json.
 */
public class KillerConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("killer.json");

	public static KillerConfig INSTANCE = new KillerConfig();

	// Not saved: the aura always starts stopped.
	public transient boolean running = false;

	public double range = 6.0;
	public int attacksPerTick = 10;
	public int maxTargets = 20;

	// Smart timing: hit each mob again the moment its hurt cooldown ends (ignores attacksPerTick and hitDelay).
	public boolean smartTiming = true;
	// Manual timing: ticks to wait before hitting the same mob again.
	public int hitDelay = 0;
	// Only swing when the weapon is fully charged, for full damage.
	public boolean fullCharge = false;

	public boolean villagers = true;
	public boolean blazes = true;
	public boolean hostile = true;
	public boolean passive = false;
	public boolean players = false;
	public boolean allMobs = false;

	public boolean showHud = true;

	// Auto repair: below repairAt% durability on the held sword/axe, run /repairCommand and click the
	// golden sword in the menu it opens. Below stopAt% the killer stops no matter what.
	public boolean autoRepair = true;
	public int repairAt = 15;
	public int stopAt = 5;
	public String repairCommand = "repairer";
	// How many times to run the command (each with up to 3 clicks) before giving up.
	public int repairTries = 5;

	public static void load() {
		if (!Files.exists(PATH)) {
			save();
			return;
		}
		try {
			KillerConfig loaded = GSON.fromJson(Files.readString(PATH), KillerConfig.class);
			if (loaded != null) {
				INSTANCE = loaded;
			}
		} catch (Exception e) {
			Killer.LOGGER.warn("Could not read {}, using defaults", PATH, e);
		}
	}

	public static void save() {
		try {
			Files.createDirectories(PATH.getParent());
			Files.writeString(PATH, GSON.toJson(INSTANCE));
		} catch (IOException e) {
			Killer.LOGGER.warn("Could not save {}", PATH, e);
		}
	}
}
