package org.codingmatters.poom.services.support.hash;

import javax.xml.crypto.dsig.spec.HMACParameterSpec;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.stream.Collectors;

public class HashMaterial {
    static public HashMaterial create() {
        return new HashMaterial();
    }

    static public HashMaterial createWith(Object key, Object value) {
        return HashMaterial.create().with(key, value);
    }

    static public HashMaterial createWith(Map map) {
        return HashMaterial.create().with(map);
    }


    private final Map<String, Object> deleguate = new HashMap<>();

    private HashMaterial() {
    }

    public HashMaterial with(Map map) {
        for (Object key : map.keySet()) {
            this.with(key, map.get(key));
        }
        return this;
    }

    public HashMaterial with(Object key, Object value) {
        this.deleguate.put(key == null ? "null" : key.toString(), value);
        return this;
    }

    public byte[] asBytes() throws UnsupportedEncodingException {
        return this.formatMapValue(this.deleguate).getBytes("UTF-8");
    }

    private String formatValue(Object value) throws UnsupportedEncodingException {
        if(value == null) {
            return "null";
        } else if(value instanceof List) {
            return this.formatListValue((List)value);
        } else if(value instanceof Map) {
            return this.formatMapValue((Map)value);
        } else {
            return value.toString();
        }
    }

    private String formatMapValue(Map value) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder("{");
        ArrayList keys = new ArrayList<>(value.keySet());
        Collections.sort(keys);

        for (Object key : keys) {
            result.append("|").append(key).append(":")
                    .append(this.formatValue(value.get(key)));
        }

        return result.append("}").toString();
    }

    private String formatListValue(List value) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder("[");
        for (Object element : value) {
            result.append("|").append(this.formatValue(element));
        }
        return result.append("]").toString();
    }
}
