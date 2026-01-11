package bnjmn21.minigames.util;

import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.stream.Stream;

public class HumanReadableList {
    public static Component of(List<Component> items) {
        return switch (items.size()) {
            case 0 -> Component.empty();
            case 1 -> items.getFirst();
            default -> //noinspection OptionalGetWithoutIsPresent
                    items.subList(0, items.size() - 1).stream().reduce(
                        (a, b) -> Component.empty().append(a, Component.text(", "), b)
                    ).get().append(Component.text(" and "), items.getLast());
        };
    }

    public static Component of(Stream<Component> items) {
        return of(items.toList());
    }
}
