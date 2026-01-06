package bnjmn21.minigames.framework;

import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.World;

import java.time.Instant;

public class Countdown {
    private final World world;
    private int secs;
    private final Component subtitle;
    private final Runnable onComplete;
    private Instant lastSec;
    private boolean done = false;

    public Countdown(World world, int secs, Component subtitle, Runnable onComplete) {
        this.world = world;
        this.secs = secs;
        this.subtitle = subtitle;
        this.onComplete = onComplete;
        lastSec = Instant.now();
        show(secs, 10);
    }

    public Countdown(World world, int secs, Runnable onComplete) {
        this(world, secs, Component.empty(), onComplete);
    }

    public void tick() {
        if (done) {
            return;
        }

        if (Instant.now().isAfter(lastSec.plusSeconds(1))) {
            secs--;
            if (secs == 0) {
                done = true;
                onComplete.run();
                return;
            }
            show(secs, 0);
            lastSec = lastSec.plusSeconds(1);
        }
    }

    private void show(int num, int fadeInTicks) {
        world.showTitle(Title.title(Component.text(num, NamedTextColor.GREEN), subtitle, fadeInTicks, 9999, 0));
        world.playSound(Sound.sound().type(org.bukkit.Sound.BLOCK_NOTE_BLOCK_HAT).build());
    }
}
