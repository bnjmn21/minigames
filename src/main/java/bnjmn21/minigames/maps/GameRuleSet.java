package bnjmn21.minigames.maps;

import org.bukkit.GameRule;
import org.bukkit.World;

import java.util.HashMap;

public class GameRuleSet {
    private final HashMap<GameRule<Boolean>, Boolean> boolGameRules = new HashMap<>();
    private final HashMap<GameRule<Integer>, Integer> intGameRules = new HashMap<>();

    public GameRuleSet with(GameRule<Boolean> gameRule, boolean value) {
        boolGameRules.put(gameRule, value);
        return this;
    }

    public GameRuleSet with(GameRule<Integer> gameRule, int value) {
        intGameRules.put(gameRule, value);
        return this;
    }

    @SuppressWarnings("unchecked")
    public void validate(World world, Issues issues) {
        HashMap<GameRule<Boolean>, Boolean> allBoolGameRules = new HashMap<>();
        HashMap<GameRule<Integer>, Integer> allIntGameRules = new HashMap<>();
        for (GameRule<?> gameRule : GameRule.values()) {
            if (gameRule.getType() == Boolean.class) {
                try {
                    allBoolGameRules.put((GameRule<Boolean>) gameRule, boolGameRules.computeIfAbsent((GameRule<Boolean>) gameRule, world::getGameRuleDefault));
                } catch (IllegalArgumentException ignored) {
                    // gamerules may be behind disabled experiments, in which case we ignore it
                }
            }
            if (gameRule.getType() == Integer.class) {
                try {
                    allIntGameRules.put((GameRule<Integer>) gameRule, intGameRules.computeIfAbsent((GameRule<Integer>) gameRule, world::getGameRuleDefault));
                } catch (IllegalArgumentException ignored) {
                    // gamerules may be behind disabled experiments, in which case we ignore it
                }
            }
        }
        allBoolGameRules.forEach((gameRule, value) -> {
            Boolean currentValue = world.getGameRuleValue(gameRule);
            if (currentValue != null && !currentValue.equals(value)) {
                issues.addWarning(IssueCollection.WrongGameRule.INSTANCE, gameRule.getName());
            }
        });
        allIntGameRules.forEach((gameRule, value) -> {
            Integer currentValue = world.getGameRuleValue(gameRule);
            if (currentValue != null && !currentValue.equals(value)) {
                issues.addWarning(IssueCollection.WrongGameRule.INSTANCE, gameRule.getName());
            }
        });
    }

    @SuppressWarnings("unchecked")
    public void applyAndResetOthers(World world) {
        HashMap<GameRule<Boolean>, Boolean> allBoolGameRules = new HashMap<>();
        HashMap<GameRule<Integer>, Integer> allIntGameRules = new HashMap<>();
        for (GameRule<?> gameRule : GameRule.values()) {
            if (gameRule.getType() == Boolean.class) {
                try {
                    allBoolGameRules.put((GameRule<Boolean>) gameRule, boolGameRules.computeIfAbsent((GameRule<Boolean>) gameRule, world::getGameRuleDefault));
                } catch (IllegalArgumentException ignored) {
                    // gamerules may be behind disabled experiments, in which case we ignore it
                }
            }
            if (gameRule.getType() == Integer.class) {
                try {
                    allIntGameRules.put((GameRule<Integer>) gameRule, intGameRules.computeIfAbsent((GameRule<Integer>) gameRule, world::getGameRuleDefault));
                } catch (IllegalArgumentException ignored) {
                    // gamerules may be behind disabled experiments, in which case we ignore it
                }
            }
        }
        allBoolGameRules.forEach(world::setGameRule);
        allIntGameRules.forEach(world::setGameRule);
    }
}
