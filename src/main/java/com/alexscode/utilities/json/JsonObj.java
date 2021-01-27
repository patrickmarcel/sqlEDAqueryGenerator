package com.alexscode.utilities.json;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JsonObj {
    HashMap<String, Object> map;

    public JsonObj() {
        map = new HashMap<>();
    }

    public void addNode(String key, Object value){
        map.put(key, value);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("{\n");
        for (Map.Entry entry : map.entrySet()){
            builder.append("\"").append(entry.getKey()).append("\":");
            String repr;
            if (entry.getValue() instanceof String) repr = getStrRepr(entry.getValue());
            else if (entry.getValue().getClass().isArray()) repr = getArrayRepr(entry.getValue());
            else if (entry.getValue() instanceof List){
                repr = "[" + ((List)entry.getValue()).stream().map(o -> {
                    if (o instanceof String) return getStrRepr(o);
                    return o.toString();
                }).collect(Collectors.joining(",\n")) + "]";

            }
            else repr = entry.getValue().toString().replace("\n", "").replace("\r", "");
            builder.append(repr).append(",\n");
        }
        String out = builder.toString();
        out = out.substring(0, out.lastIndexOf(","));
        return out + "\n}";
    }

    private static String getArrayRepr(Object value) {
        return Arrays.deepToString((Object[]) value);
    }

    public static String getStrRepr(Object value) {
        String in = (String) value;
        String out = in.replace("\r\n", "\\r\\n").replace("\n", "\\n");
        return "\"" + out + "\"";
    }
    public static String getLineRepr(Object value) {
        String in = (String) value;
        String out = in.replace("\r\n", "\\r\\n").replace("\n", "\\n");
        return "\"" + out + " \\n\"";
    }
}
