package dev.statcompare;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Adds a coloured stat comparison to item tooltips while an inventory-like screen is open:
 * armour is compared against what's equipped in that slot, weapons and tools against the
 * item in your main hand. See StatCompare for the actual comparison logic.
 */
public class StatCompareClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ItemTooltipCallback.EVENT.register((stack, context, type, tooltip) -> {
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
