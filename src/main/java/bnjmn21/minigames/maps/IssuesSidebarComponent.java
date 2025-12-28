package bnjmn21.minigames.maps;

import net.kyori.adventure.text.Component;
import net.megavex.scoreboardlibrary.api.sidebar.component.LineDrawable;
import net.megavex.scoreboardlibrary.api.sidebar.component.SidebarComponent;
import org.jetbrains.annotations.NotNull;

public class IssuesSidebarComponent implements SidebarComponent {
    private final Editor editor;

    public IssuesSidebarComponent(Editor editor) {
        this.editor = editor;
    }

    @Override
    public void draw(@NotNull LineDrawable drawable) {
        Issues issues = editor.validate();
        for (Component line : issues.view()) {
            drawable.drawLine(line);
        }
    }
}
