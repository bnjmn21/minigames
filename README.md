# minigames

A plugin for my minecraft server. Currently, "The Bridge" (inspired by Hypixel) is implemented.

## Usage

Put your maps in `maps/the_bridge` (Create the directories if they don't yet exist), and the lobby world at `lobby`.
Then start the server with the plugin installed.

For each map, open it in the editor with `/the_bridge edit <map_name>`.
In the editor, you can set the necessary parameters for the map using `/the_bridge set <param> <value>`,
and set the necessary game rules using `/the_bridge set_default_game_rules`. See `/the_bridge help` for more info.

Once all maps have been configured, configure and start the game with `/game`.
Players can change their settings with `/settings`.

## Building

This project uses a standard Gradle setup.