package dev.statcompare;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Adds a coloured stat comparison to item tooltips while an inventory-like screen is open:
 * armour is compared against what's equipped in that slot, tools (including weapons) against
 * the item in your main hand. The H key (rebindable under Options > Controls > Key Binds >
 * Stat Compare) toggles the comparison on and off. See StatCompare for the comparison logic.
 */
public class StatCompareClient implements ClientModInitializer {

    public static final String MOD_ID = "statcompare";

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

    /** Shows/hides the comparison lines. Rebindable in Options > Controls > Key Binds. */
    public static final KeyMapping TOGGLE_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.statcompare.toggle",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_H,
            CATEGORY));

    /** Whether comparison lines are currently shown. */
    public static boolean enabled = true;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (TOGGLE_KEY.consumeClick()) {
                enabled = !enabled;
            }
        });

        ItemTooltipCallback.EVENT.register((stack, context, type, tooltip) -> {
            if (!enabled) {
                return;
            }
            Minecraft client = Minecraft.getInstance();
            // only show the comparison while a container/inventory screen is open, so ordinary
            // gameplay (e.g. hovering the hotbar) isn't cluttered with a comparison against itself
            if (!(client.gui.screen() instanceof AbstractContainerScreen)) {
                return;
            }
            LocalPlayer player = client.player;
            if (player == null || stack.isEmpty()) {
                return;
            }

            ItemStack reference = StatCompare.findReference(player, stack);
            if (reference == null || reference.isEmpty() || reference == stack) {
                return;
            }
            StatCompare.appendComparison(tooltip, stack, reference);
        });
    }
}
