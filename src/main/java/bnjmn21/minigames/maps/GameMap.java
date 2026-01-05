package bnjmn21.minigames.maps;

import bnjmn21.minigames.Game;
import bnjmn21.minigames.Minigames;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

import static bnjmn21.minigames.util.Paths.path;

/**
 * A thread-safe util for managing a map
 */
public class GameMap {
    public final String name;
    public final Game game;
    public final Component displayName;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ArrayList<String> currentlyCopying = new ArrayList<>();
    private final ReentrantReadWriteLock currentlyCopyingLock = new ReentrantReadWriteLock();
    private final Minigames plugin;

    public GameMap(String name, Game game, Component displayName, Minigames plugin) {
        this.name = name;
        this.game = game;
        this.displayName = displayName;
        this.plugin = plugin;
    }

    /**
     * Creates a readonly copy of the map.
     * Throws if it's already being copied.
     * You must call {@code Bukkit.unloadWorld(world, false)} once it's no longer used,
     * and before you create a new copy to the same location.
     */
    public void createReadonlyCopy(String copyName, Consumer<World> onCompleted) {
        var currentlyCopyingReadLock = currentlyCopyingLock.readLock();
        currentlyCopyingReadLock.lock();
        if (currentlyCopying.contains(copyName)) {
            currentlyCopyingReadLock.unlock();
            throw new RuntimeException("Already copying " + copyName);
        }
        currentlyCopyingReadLock.unlock();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            var currentlyCopyingWriteLock = currentlyCopyingLock.writeLock();
            currentlyCopyingWriteLock.lock();
            currentlyCopying.add(copyName);
            currentlyCopyingWriteLock.unlock();

            GameMap.deleteDirectory(path(copyName));
            var readLock = lock.readLock();
            readLock.lock();
            GameMap.copyDirectory(getPath(), path(copyName));
            readLock.unlock();

            var currentlyCopyingWriteLock1 = currentlyCopyingLock.writeLock();
            currentlyCopyingWriteLock1.lock();
            currentlyCopying.remove(copyName);
            currentlyCopyingWriteLock1.unlock();

            Bukkit.getScheduler().runTask(plugin, () -> {
                World world = new WorldCreator(copyName).createWorld();
                onCompleted.accept(world);
            });
        });
    }

    public boolean isCurrentlyCopying(String to) {
        var currentlyCopyingReadLock = currentlyCopyingLock.readLock();
        currentlyCopyingReadLock.lock();
        boolean res = currentlyCopying.contains(to);
        currentlyCopyingReadLock.unlock();
        return res;
    }

    /**
     * Creates a writeable copy of the map.
     * Once the map is closed, the changes will get applied to the original world.
     * Throws if it's already being copied.
     */
    public void createWritableCopy(String copyName, Consumer<Writeable> onCompleted) {
        var currentlyCopyingReadLock = currentlyCopyingLock.readLock();
        currentlyCopyingReadLock.lock();
        if (currentlyCopying.contains(copyName)) {
            currentlyCopyingReadLock.unlock();
            throw new RuntimeException("Already copying " + copyName);
        }
        currentlyCopyingReadLock.unlock();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            var currentlyCopyingWriteLock = currentlyCopyingLock.writeLock();
            currentlyCopyingWriteLock.lock();
            currentlyCopying.add(copyName);
            currentlyCopyingWriteLock.unlock();

            GameMap.deleteDirectory(path(copyName));
            var readLock = lock.readLock();
            readLock.lock();
            GameMap.copyDirectory(getPath(), path(copyName));
            readLock.unlock();

            var currentlyCopyingWriteLock1 = currentlyCopyingLock.writeLock();
            currentlyCopyingWriteLock1.lock();
            currentlyCopying.remove(copyName);
            currentlyCopyingWriteLock1.unlock();

            Bukkit.getScheduler().runTask(plugin, () -> {
                World world = new WorldCreator(copyName).createWorld();
                Writeable writeable = new Writeable(copyName, world, this, plugin);
                onCompleted.accept(writeable);
            });
        });
    }

    private Path getPath() {
        return path(name);
    }

    /**
     * A writeable copy of a map, e.g., for editing
     * You must call {@link Writeable#save} when the map is no longer used.
     */
    public record Writeable(String copyName, World world, GameMap original, Minigames plugin) {
        /**
         * Recovers a {@link Writeable} that wasn't correctly saved, e.g., after a server crash
         */
        public static void recover(String copyName, String originalName) {
            deleteDirectory(path(originalName));
            copyDirectory(path(copyName), path(originalName));
            deleteDirectory(path(copyName));
        }

        public void save(Runnable onCompleted) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getLogger().info("Saving " + copyName);
                world.save(true);
                Bukkit.unloadWorld(world, false);

                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    var writeLock = original.lock.writeLock();
                    writeLock.lock();
                    deleteDirectory(original.getPath());
                    copyDirectory(path(copyName), original.getPath());
                    deleteDirectory(path(copyName));
                    writeLock.unlock();
                    Bukkit.getScheduler().runTask(plugin, onCompleted);
                });
            });
        }
    }

    private static void copyDirectory(Path source, Path target) {
        try {
            Files.walkFileTree(source, new SimpleFileVisitor<>() {
                @Override
                public @NotNull FileVisitResult preVisitDirectory(@NotNull Path dir, @NotNull BasicFileAttributes attrs) throws IOException {
                    Path targetDir = target.resolve(source.relativize(dir));
                    Files.createDirectories(targetDir);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) throws IOException {
                    Files.copy(file, target.resolve(source.relativize(file)),
                            StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public static void deleteDirectory(Path dir) {
        if (!Files.exists(dir)) return;

        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                @Override
                public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public @NotNull FileVisitResult postVisitDirectory(@NotNull Path directory, IOException exc) throws IOException {
                    Files.delete(directory);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
