package bnjmn21.minigames.maps;

import bnjmn21.minigames.util.HumanReadableList;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.HashMap;

public class Issues {
    private final ArrayList<Component> warnings = new ArrayList<>();
    private final HashMap<IssueCollection, ArrayList<String>> warningCollections = new HashMap<>();
    private final ArrayList<Component> errors = new ArrayList<>();
    private final HashMap<IssueCollection, ArrayList<String>> errorCollections = new HashMap<>();

    public Issues() {}

    public void addWarning(Component item) {
        warnings.add(item);
    }

    public void addWarning(IssueCollection collection, String item) {
        warningCollections.computeIfAbsent(collection, k -> new ArrayList<>()).add(item);
    }

    public void addError(Component item) {
        errors.add(item);
    }

    public void addError(IssueCollection collection, String item) {
        errorCollections.computeIfAbsent(collection, k -> new ArrayList<>()).add(item);
    }

    public ArrayList<Component> view() {
        ArrayList<Component> lines = new ArrayList<>();
        errorCollections.forEach((collection, items) -> {
            if (items.isEmpty()) {
                return;
            }

            if (items.size() == 1) {
                lines.add(Component.translatable(collection.singular(), NamedTextColor.RED, Component.text(items.getFirst())));
            } else {
                lines.add(Component.translatable(collection.plural(), NamedTextColor.RED));
                lines.add(HumanReadableList.truncated(items.stream().map(i -> (Component) Component.text(i)).toList()).color(NamedTextColor.RED));
            }
        });
        for (Component error : errors) {
            lines.add(error.color(NamedTextColor.RED));
        }

        warningCollections.forEach((collection, items) -> {
            if (items.isEmpty()) {
                return;
            }

            if (items.size() == 1) {
                lines.add(Component.translatable(collection.singular(), NamedTextColor.YELLOW, Component.text(items.getFirst())));
            } else {
                lines.add(Component.translatable(collection.plural(), NamedTextColor.YELLOW));
                lines.add(HumanReadableList.truncated(items.stream().map(i -> (Component) Component.text(i)).toList()).color(NamedTextColor.YELLOW));
            }
        });
        for (Component warning : warnings) {
            lines.add(warning.color(NamedTextColor.YELLOW));
        }

        if (lines.size() > 10) {
            lines.subList(10, lines.size() - 10).clear();
            lines.add(Component.translatable("map_editor.issues.more").decorate(TextDecoration.ITALIC));
        }

        if (lines.isEmpty()) {
            lines.add(Component.translatable("map_editor.issues.none").color(NamedTextColor.GREEN));
        }

        return lines;
    }
}
