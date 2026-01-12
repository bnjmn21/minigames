package bnjmn21.minigames.maps;

public interface IssueCollection {
    String singular();
    String plural();

    class UnsetField implements IssueCollection {
        public static final UnsetField INSTANCE = new UnsetField();

        private UnsetField() {}

        @Override
        public String singular() {
            return "map_editor.issues.unset.singular";
        }

        @Override
        public String plural() {
            return "map_editor.issues.unset.plural";
        }
    }

    class WrongGameRule implements IssueCollection {
        public static final WrongGameRule INSTANCE = new WrongGameRule();

        private WrongGameRule() {}

        @Override
        public String singular() {
            return "map_editor.issues.wrong_game_rule.singular";
        }

        @Override
        public String plural() {
            return "map_editor.issues.wrong_game_rule.plural";
        }
    }
}
