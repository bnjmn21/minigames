package bnjmn21.minigames.the_bridge;

import bnjmn21.minigames.Minigames;
import bnjmn21.minigames.framework.PlayerDataField;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.structure.Structure;
import org.bukkit.structure.StructureManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class Cages {
    public HashMap<String, String> cages;
    private final Minigames plugin;

    public static final PlayerDataField<String> cageField = new PlayerDataField<>(
            Minigames.ns("the_bridge/cage"),
            new TypeToken<>() {},
            () -> "default"
    );

    public Cages(Minigames plugin) {
        this.plugin = plugin;

        Type type = new TypeToken<HashMap<String, String>>() {}.getType();
        Gson gson = new Gson();
        InputStream in = plugin.getResource("the_bridge_cages/index.json");
        if (in == null) return;
        Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8);

        this.cages = gson.fromJson(reader, type);
    }

    public Structure load(String id, boolean blue) {
        StructureManager mgr = plugin.getServer().getStructureManager();
        try (InputStream in = plugin.getResource("the_bridge_cages/" + id + (blue ? "/blue.nbt" : "/red.nbt"))) {
            if (in == null) {
                throw new RuntimeException("Cage not found (" + id + ")");
            }

            return mgr.loadStructure(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
