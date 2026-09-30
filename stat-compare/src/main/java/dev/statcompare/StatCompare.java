package dev.statcompare;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.equipment.Equippable;

/** Works out which item to compare against, and builds the coloured tooltip lines for it. */
public final class StatCompare {

    private static final double EPSILON = 1.0E-4;

    private StatCompare() {
    }

    /**
     * The item to compare {@code hovered} against, or null if there's nothing sensible to compare
     * it with. Armour (helmet/chestplate/leggings/boots) compares against the equipped piece in
     * the same slot. A tool or weapon (swords, axes, tridents and the like, detected by having a
     * Tool component or attribute modifiers) compares against the main hand item, but only when
     * that main hand item is itself a tool too - so a random, non-tool item never shows a
     * comparison, whether it's the one being hovered or the one in your hand.
     */
    public static ItemStack findReference(Player player, ItemStack hovered) {
        Equippable equippable = hovered.get(DataComponents.EQUIPPABLE);
        if (equippable != null) {
            EquipmentSlot slot = equippable.slot();
            if (slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST
                    || slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET) {
                return player.getItemBySlot(slot);
            }
            return null;
        }
        if (!isTool(hovered)) {
            return null;
        }
        ItemStack mainhand = player.getItemBySlot(EquipmentSlot.MAINHAND);
        return isTool(mainhand) ? mainhand : null;
    }

    private static boolean isTool(ItemStack stack) {
        return stack.get(DataComponents.TOOL) != null || stack.get(DataComponents.ATTRIBUTE_MODIFIERS) != null;
    }

    public static void appendComparison(List<Component> tooltip, ItemStack hovered, ItemStack reference) {
        List<Line> lines = buildLines(hovered, reference);
        if (lines.isEmpty()) {
            return;
        }
        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal("Compared to: " + reference.getHoverName().getString())
                .withStyle(ChatFormatting.GRAY));
        for (Line line : lines) {
            if (line == null) {
                tooltip.add(Component.literal("Enchantments:").withStyle(ChatFormatting.DARK_GRAY));
                continue;
            }
            tooltip.add(line.render());
        }
    }

    private static List<Line> buildLines(ItemStack hovered, ItemStack reference) {
        List<Line> lines = new ArrayList<>();

        // every attribute either item actually carries a modifier for, keyed by id so both
        // sides line up even if only one of them touches that attribute
        Map<String, Holder<Attribute>> attrHolders = new LinkedHashMap<>();
        collectAttributeHolders(hovered, attrHolders);
        collectAttributeHolders(reference, attrHolders);
        Map<String, Double> hoveredMods = attributeModifierTotals(hovered);
        Map<String, Double> referenceMods = attributeModifierTotals(reference);
        for (Map.Entry<String, Holder<Attribute>> entry : attrHolders.entrySet()) {
            Holder<Attribute> holder = entry.getValue();
            // the item's effective value for this attribute, the same number the vanilla advanced
            // tooltip (F3+H) shows: the attribute's own base value plus this item's modifiers.
            // Some modifiers (e.g. attack speed) are negative, so showing just the raw modifier
            // (like "-2.4") reads as nonsense; the base-inclusive total (like "1.6") is the real,
            // comparable number.
            double base = holder.value().getDefaultValue();
            double hv = base + hoveredMods.getOrDefault(entry.getKey(), 0.0);
            double rv = base + referenceMods.getOrDefault(entry.getKey(), 0.0);
            lines.add(new Line(Component.translatable(holder.value().getDescriptionId()).getString(), hv, rv, ""));
        }

        int hoveredDurability = hovered.getMaxDamage();
        int referenceDurability = reference.getMaxDamage();
        if (hoveredDurability > 0 && referenceDurability > 0) {
            lines.add(new Line("Durability", hoveredDurability, referenceDurability, ""));
        }

        Tool hoveredTool = hovered.get(DataComponents.TOOL);
        Tool referenceTool = reference.get(DataComponents.TOOL);
        if (hoveredTool != null && referenceTool != null) {
            lines.add(new Line("Mining Speed", hoveredTool.defaultMiningSpeed(),
                    referenceTool.defaultMiningSpeed(), ""));
        }

        Map<String, Integer> hoveredEnch = enchantmentLevels(hovered);
        Map<String, Integer> referenceEnch = enchantmentLevels(reference);
        Map<String, Component> enchNames = new LinkedHashMap<>();
        collectEnchantmentNames(hovered, enchNames);
        collectEnchantmentNames(reference, enchNames);
        boolean enchHeaderAdded = false;
        for (Map.Entry<String, Component> entry : enchNames.entrySet()) {
            int hv = hoveredEnch.getOrDefault(entry.getKey(), 0);
            int rv = referenceEnch.getOrDefault(entry.getKey(), 0);
            if (hv == 0 && rv == 0) {
                continue;
            }
            if (!enchHeaderAdded) {
                lines.add(null); // marks a section header; render() below skips it, appendComparison replaces it
                enchHeaderAdded = true;
            }
            lines.add(new Line(entry.getValue().getString(), hv, rv, ""));
        }

        return lines;
    }

    /** Levels of every enchantment actually applied to the item (not a book's stored ones), by id. */
    private static Map<String, Integer> enchantmentLevels(ItemStack stack) {
        Map<String, Integer> levels = new LinkedHashMap<>();
        ItemEnchantments enchantments = stack.get(DataComponents.ENCHANTMENTS);
        if (enchantments == null) {
            return levels;
        }
        for (Holder<Enchantment> holder : enchantments.keySet()) {
            levels.put(holder.getRegisteredName(), enchantments.getLevel(holder));
        }
        return levels;
    }

    private static void collectEnchantmentNames(ItemStack stack, Map<String, Component> names) {
        ItemEnchantments enchantments = stack.get(DataComponents.ENCHANTMENTS);
        if (enchantments == null) {
            return;
        }
        for (Holder<Enchantment> holder : enchantments.keySet()) {
            names.putIfAbsent(holder.getRegisteredName(), holder.value().description());
        }
    }

    /** Sums every flat (ADD_VALUE) attribute modifier on the stack, keyed by the attribute's id. */
    private static Map<String, Double> attributeModifierTotals(ItemStack stack) {
        Map<String, Double> totals = new LinkedHashMap<>();
        ItemAttributeModifiers modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (modifiers == null) {
            return totals;
        }
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            AttributeModifier modifier = entry.modifier();
            if (modifier.operation() != AttributeModifier.Operation.ADD_VALUE) {
                continue; // percentage modifiers don't add up into one meaningful number; skip them
            }
            totals.merge(entry.attribute().getRegisteredName(), modifier.amount(), Double::sum);
        }
        return totals;
    }

    private static void collectAttributeHolders(ItemStack stack, Map<String, Holder<Attribute>> holders) {
        ItemAttributeModifiers modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (modifiers == null) {
            return;
        }
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            Holder<Attribute> holder = entry.attribute();
            holders.putIfAbsent(holder.getRegisteredName(), holder);
        }
    }

    private static String format(double value) {
        if (Math.abs(value - Math.round(value)) < 0.01) {
            return String.valueOf(Math.round(value));
        }
        return String.format("%.1f", value);
    }

    /** One comparison row: the hovered item's value against the reference item's value. */
    private record Line(String label, double hoveredValue, double referenceValue, String suffix) {
        Component render() {
            double diff = hoveredValue - referenceValue;
            ChatFormatting color = diff > EPSILON ? ChatFormatting.GREEN
                    : diff < -EPSILON ? ChatFormatting.RED
                    : ChatFormatting.GRAY;
            StringBuilder text = new StringBuilder(label).append(": ").append(format(hoveredValue)).append(suffix);
            if (Math.abs(diff) > EPSILON) {
                text.append(" (").append(diff > 0 ? "+" : "").append(format(diff)).append(suffix).append(")");
            }
            return Component.literal(text.toString()).withStyle(color);
        }
    }
}
