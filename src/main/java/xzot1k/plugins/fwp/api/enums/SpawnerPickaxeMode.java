package xzot1k.plugins.fwp.api.enums;

public enum SpawnerPickaxeMode {
    ALL(0), NATURAL(1), PLACED(2);

    final int index;

    SpawnerPickaxeMode(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public static SpawnerPickaxeMode getFromIndex(int index) {
        for (int i = -1; ++i < values().length; )
            if (i == index) return values()[i];
        return ALL;
    }

    public static SpawnerPickaxeMode getNext(int currentIndex) {
        return (((currentIndex + 1) >= (values().length - 1)) ? ALL : getFromIndex(currentIndex + 1));
    }

}
