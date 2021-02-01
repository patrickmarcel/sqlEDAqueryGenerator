package fr.univtours.info.columnStore;


import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public enum Datatype {
    DATE(3), STRING(2), INTEGER(1), REAL(0);

    private int value;
    private static Map map = new HashMap<>();

    Datatype(int value) {
        this.value = value;
    }

    static {
        for (Datatype pageType : Datatype.values()) {
            map.put(pageType.value, pageType);
        }
    }

    public static Datatype valueOf(int pageType) {
        return (Datatype) map.get(pageType);
    }

    public int getValue() {
        return value;
    }

    public byte[] getBytes() {
        ByteBuffer b = ByteBuffer.allocate(4);
        b.putInt(value);
        return b.array();
    }

    public boolean isString(){
        return value == 2;
    }

    public boolean isInt(){
        return value == 1;
    }

    public boolean isReal(){
        return value == 0;
    }

    public boolean isDate(){return value == 3;}


}
