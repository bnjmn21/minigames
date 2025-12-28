package bnjmn21.minigames.util;

import java.nio.file.FileSystems;
import java.nio.file.Path;

public class Paths {
    public static Path path(String of) {
        return FileSystems.getDefault().getPath(of);
    }

    public static String toString(Path path) {
        return path.toString().replace('\\', '/');
    }
}
