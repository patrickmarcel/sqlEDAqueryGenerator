package com.alexscode.utilities.json;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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
            else repr = entry.getValue().toString().replace("\n", "").replace("\r", "");
            builder.append(repr).append(",\n");
        }
        String out = builder.toString();
        out = out.substring(0, out.lastIndexOf(","));
        return out + "\n}";
    }

    private String getArrayRepr(Object value) {
        return Arrays.deepToString((Object[]) value);
    }

    private String getStrRepr(Object value) {
        String in = (String) value;
        String out = in.replace("\r\n", "\\r\\n").replace("\n", "\\n");
        return "\"" + out + "\"";
    }
}
