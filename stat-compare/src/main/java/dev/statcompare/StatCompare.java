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
import net.minecraft.world.item.equipment.Equippable;

/** Works out which item to compare against, and builds the coloured tooltip lines for it. */
public final class StatCompare {

    private static final double EPSILON = 1.0E-4;

    private StatCompare() {
    }

    /**
     * The item to compare {@code hovered} against, or null if it isn't the kind of item this mod
     * compares (armour compares against the equipped piece in the same slot; weapons and tools,
     * detected by having a Tool component or attribute modifiers, compare against the main hand).
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
        if (hovered.get(DataComponents.TOOL) != null || hovered.get(DataComponents.ATTRIBUTE_MODIFIERS) != null) {
            return player.getItemBySlot(EquipmentSlot.MAINHAND);
        }
        return null;
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
            tooltip.add(line.render());
        }
    }

    private static List<Line> buildLines(ItemStack hovered, ItemStack reference) {
        List<Line> lines = new ArrayList<>();

        Map<String, Double> hoveredAttrs = attributeTotals(hovered);
        Map<String, Double> referenceAttrs = attributeTotals(reference);
        Map<String, Component> names = new LinkedHashMap<>();
        collectAttributeNames(hovered, names);
        collectAttributeNames(reference, names);
        for (Map.Entry<String, Component> entry : names.entrySet()) {
            double hv = hoveredAttrs.getOrDefault(entry.getKey(), 0.0);
            double rv = referenceAttrs.getOrDefault(entry.getKey(), 0.0);
            if (Math.abs(hv) < EPSILON && Math.abs(rv) < EPSILON) {
                continue;
            }
            lines.add(new Line(entry.getValue().getString(), hv, rv, ""));
        }

        int hoveredDurability = hovered.getMaxDamage();
        int referenceDurability = reference.getMaxDamage();
        if (hoveredDurability > 0 || referenceDurability > 0) {
            lines.add(new Line("Durability", hoveredDurability, referenceDurability, ""));
        }

        Tool hoveredTool = hovered.get(DataComponents.TOOL);
        Tool referenceTool = reference.get(DataComponents.TOOL);
        if (hoveredTool != null || referenceTool != null) {
            double hv = hoveredTool != null ? hoveredTool.defaultMiningSpeed() : 0.0;
            double rv = referenceTool != null ? referenceTool.defaultMiningSpeed() : 0.0;
            lines.add(new Line("Mining Speed", hv, rv, ""));
        }

        return lines;
    }

    /** Sums every flat (ADD_VALUE) attribute modifier on the stack, keyed by the attribute's id. */
    private static Map<String, Double> attributeTotals(ItemStack stack) {
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

    private static void collectAttributeNames(ItemStack stack, Map<String, Component> names) {
        ItemAttributeModifiers modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (modifiers == null) {
            return;
        }
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            Holder<Attribute> holder = entry.attribute();
            names.putIfAbsent(holder.getRegisteredName(), Component.translatable(holder.value().getDescriptionId()));
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
