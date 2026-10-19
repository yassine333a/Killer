package killer.best.client;

import com.mojang.blaze3d.platform.InputConstants;
import killer.best.Killer;
import killer.best.client.hub.AutoRepair;
import killer.best.client.hub.HubScreen;
import killer.best.client.hub.KillAura;
import killer.best.client.hub.KillTracker;
import killer.best.client.hub.KillerConfig;
import killer.best.client.hub.TrackerHud;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class KillerClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		KillerConfig.load();

		KeyMapping.Category category = KeyMapping.Category.register(Killer.id("main"));
		KeyMapping openHub = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.killer.open_hub", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, category));
		KeyMapping toggle = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.killer.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, category));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openHub.consumeClick()) {
				if (client.gui.screen() == null) {
					client.gui.setScreen(new HubScreen());
				}
			}
			while (toggle.consumeClick()) {
				KillerConfig.INSTANCE.running = !KillerConfig.INSTANCE.running;
				client.gui.hud.setOverlayMessage(KillerConfig.INSTANCE.running
						? Component.literal("Killer: ON").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
						: Component.literal("Killer: OFF").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), false);
			}
			AutoRepair.tick(client);
			KillAura.tick(client);
			KillTracker.tick(client);
		});

		HudElementRegistry.addLast(Killer.id("tracker"), TrackerHud::extract);
	}
}
