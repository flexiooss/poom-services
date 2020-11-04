package org.codingmatters.poom.services.support.hash;

import javax.xml.crypto.dsig.spec.HMACParameterSpec;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

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
        StringBuilder result = new StringBuilder();
        this.deleguate.keySet().stream().sorted().forEach(
                key -> result.append(key).append(this.deleguate.getOrDefault(key, "null").toString())
        );
        return result.toString().getBytes("UTF-8");
    }

}
