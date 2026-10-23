package dev.mak.noiseweaver;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;

public final class NoiseWeaverClient implements ClientModInitializer {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("noiseweaver", "keys"));
    private static final KeyMapping OPEN_MENU = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.noiseweaver.open_menu", InputConstants.Type.KEYBOARD, InputConstants.KEY_H, CATEGORY));
    private static final KeyMapping TOGGLE_TOOL = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.noiseweaver.toggle_tool", InputConstants.Type.KEYBOARD, InputConstants.KEY_T, CATEGORY));

    @Override public void onInitializeClient() {
        BlockWeights.load();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_MENU.consumeClick()) client.setScreenAndShow(new NoiseMenuScreen());
            while (TOGGLE_TOOL.consumeClick()) {
                NoiseWeaverState.placementModeEnabled = !NoiseWeaverState.placementModeEnabled;
                if (client.player != null) {
                    String key = NoiseWeaverState.placementModeEnabled ? "chat.noiseweaver.enabled" : "chat.noiseweaver.disabled";
                    client.player.sendSystemMessage(Component.translatable(key));
                }
            }
        });
    }
}
