package killer.best.client;

import com.mojang.blaze3d.platform.InputConstants;
import killer.best.Killer;
import killer.best.client.hub.HubScreen;
import killer.best.client.hub.KillAura;
import killer.best.client.hub.KillerConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class KillerClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		KillerConfig.load();

		KeyMapping.Category category = KeyMapping.Category.register(Killer.id("main"));
		KeyMapping openHub = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.killer.open_hub", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, category));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openHub.consumeClick()) {
				if (client.screen == null) {
					client.setScreen(new HubScreen());
				}
			}
			KillAura.tick(client);
		});
	}
}
