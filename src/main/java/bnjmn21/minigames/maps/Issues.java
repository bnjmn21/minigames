package bnjmn21.minigames.maps;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.HashMap;

public class Issues {
    private final ArrayList<String> warnings = new ArrayList<>();
    private final HashMap<IssueCollection, ArrayList<String>> warningCollections = new HashMap<>();
    private final ArrayList<String> errors = new ArrayList<>();
    private final HashMap<IssueCollection, ArrayList<String>> errorCollections = new HashMap<>();

    public Issues() {}

    public void addWarning(String item) {
        warnings.add(item);
    }

    public void addWarning(IssueCollection collection, String item) {
        warningCollections.computeIfAbsent(collection, k -> new ArrayList<>()).add(item);
    }

    public void addError(String item) {
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
                lines.add(Component.text(collection.singular(items.getFirst())).color(NamedTextColor.RED));
            } else if (items.size() <= 3) {
                lines.add(Component.text(String.join(", ", items)).color(NamedTextColor.RED));
            } else {
                lines.add(Component.text(collection.plural()).color(NamedTextColor.RED));
                lines.add(Component.text(
                        String.join(", ", items.subList(0, 2)) + ", and " + (items.size() - 2) + " more")
                        .color(NamedTextColor.RED)
                );
            }
        });
        for (String error : errors) {
            lines.add(Component.text(error).color(NamedTextColor.RED));
        }

        warningCollections.forEach((collection, items) -> {
            if (items.isEmpty()) {
                return;
            }

            if (items.size() == 1) {
                lines.add(Component.text(collection.singular(items.getFirst())).color(NamedTextColor.YELLOW));
            } else if (items.size() <= 3) {
                lines.add(Component.text(String.join(", ", items)).color(NamedTextColor.YELLOW));
            } else {
                lines.add(Component.text(collection.plural()).color(NamedTextColor.YELLOW));
                lines.add(Component.text(
                                String.join(", ", items.subList(0, 2)) + ", and " + (items.size() - 2) + " more")
                        .color(NamedTextColor.YELLOW)
                );
            }
        });
        for (String warning : warnings) {
            lines.add(Component.text(warning).color(NamedTextColor.YELLOW));
        }

        if (lines.size() > 10) {
            lines.subList(10, lines.size() - 10).clear();
            lines.add(Component.text("... and more"));
        }

        if (lines.isEmpty()) {
            lines.add(Component.text("No issues!").color(NamedTextColor.GREEN));
        }

        return lines;
    }
}
