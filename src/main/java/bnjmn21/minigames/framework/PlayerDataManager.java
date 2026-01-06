package bnjmn21.minigames.framework;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.bukkit.NamespacedKey;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.Supplier;

public class PlayerDataManager {
    public HashMap<UUID, JsonObject> data;
    Gson gson = new Gson();

    public PlayerDataManager() {
        Type type = new TypeToken<HashMap<UUID, JsonObject>>() {}.getType();

        try (Reader reader = Files.newBufferedReader(Path.of("minigames_player_data.json"))) {
            this.data = gson.fromJson(reader, type);
        } catch (IOException e) {
            this.data = new HashMap<>();
        }
    }

    public <T> T get(UUID player, NamespacedKey key, TypeToken<T> token, Supplier<T> defaultValue) {
        String strKey = key.asString();

        if (!this.data.containsKey(player)) {
            T value = defaultValue.get();
            JsonObject obj = new JsonObject();
            obj.add(strKey, gson.toJsonTree(value, token.getType()));
            data.put(player, obj);
            return value;
        }

        JsonObject playerData = this.data.get(player);
        if (!playerData.has(strKey)) {
            T value = defaultValue.get();
            JsonObject obj = data.get(player);
            obj.add(strKey, gson.toJsonTree(value, token.getType()));
            data.put(player, obj);
            return value;
        }

        return gson.fromJson(playerData.get(strKey), token);
    }

    public <T> void set(UUID player, NamespacedKey key, TypeToken<T> token, T value) {
        JsonObject playerData = this.data.computeIfAbsent(player, ignored -> new JsonObject());
        playerData.add(key.asString(), gson.toJsonTree(value, token.getType()));
    }

    public void save() {
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
        try (Writer writer = Files.newBufferedWriter(Path.of("minigames_player_data.json"))) {
            gson.toJson(this.data, writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
