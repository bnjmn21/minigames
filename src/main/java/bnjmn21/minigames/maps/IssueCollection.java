package bnjmn21.minigames.maps;

public interface IssueCollection {
    String singular(String item);
    String plural();

    class UnsetField implements IssueCollection {
        public static UnsetField INSTANCE = new UnsetField();

        private UnsetField() {}

        @Override
        public String singular(String item) {
            return item + " is unset";
        }

        @Override
        public String plural() {
            return "Unset items:";
        }
    }

    class WrongGameRule implements IssueCollection {
        public static WrongGameRule INSTANCE = new WrongGameRule();

        private WrongGameRule() {}

        @Override
        public String singular(String item) {
            return "Gamerule " + item + " is wrong";
        }

        @Override
        public String plural() {
            return "Multiple gamerules wrong:";
        }
    }
}
