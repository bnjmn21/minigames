package bnjmn21.minigames;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings({"UnstableApiUsage", "unused"})
public class MinigamesLoader implements PluginLoader {
    @Override
    public void classloader(@NotNull PluginClasspathBuilder classpathBuilder) {
        MavenLibraryResolver resolver = new MavenLibraryResolver();
        resolver.addRepository(new RemoteRepository.Builder("central", "default", MavenLibraryResolver.MAVEN_CENTRAL_DEFAULT_MIRROR).build());

        String scoreboardLibraryVersion = "2.4.4";
        resolver.addDependency(new Dependency(new DefaultArtifact("net.megavex:scoreboard-library-api:" + scoreboardLibraryVersion), null));
        resolver.addDependency(new Dependency(new DefaultArtifact("net.megavex:scoreboard-library-implementation:" + scoreboardLibraryVersion), null));
        resolver.addDependency(new Dependency(new DefaultArtifact("net.megavex:scoreboard-library-modern:" + scoreboardLibraryVersion), null));

        classpathBuilder.addLibrary(resolver);
    }
}
