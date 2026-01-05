package bnjmn21.minigames.maps;

import net.kyori.adventure.text.Component;
import net.megavex.scoreboardlibrary.api.sidebar.component.LineDrawable;
import net.megavex.scoreboardlibrary.api.sidebar.component.SidebarComponent;
import org.jetbrains.annotations.NotNull;

public record IssuesSidebarComponent(Editor editor) implements SidebarComponent {
    @Override
    public void draw(@NotNull LineDrawable drawable) {
        Issues issues = editor.validate();
        for (Component line : issues.view()) {
            drawable.drawLine(line);
        }
    }
}
