package bnjmn21.minigames.util;

import net.kyori.adventure.text.Component;

import java.util.List;

public class HumanReadableList {
    public static Component of(List<Component> items) {
        return switch (items.size()) {
            case 0 -> Component.empty();
            case 1 -> items.getFirst();
            default -> //noinspection OptionalGetWithoutIsPresent
                    items.subList(0, items.size() - 1).stream().reduce(
                        (a, b) -> Component.empty().append(a, Component.translatable("human_readable_list.comma"), b)
                    ).get().append(Component.translatable("human_readable_list.and"), items.getLast());
        };
    }

    public static Component truncated(List<Component> items) {
        if (items.size() <= 3) {
            return items.stream().reduce(
                    (a, b) -> Component.empty().append(a, Component.translatable("human_readable_list.comma"), b)
            ).orElseThrow();
        } else {
            return Component.translatable("human_readable_list.truncated", items.get(0), items.get(1), items.get(2), Component.text(items.size() - 2));
        }
    }
}
