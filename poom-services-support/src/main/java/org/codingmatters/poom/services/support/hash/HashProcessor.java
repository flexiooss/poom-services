package org.codingmatters.poom.services.support.hash;

import org.codingmatters.poom.services.support.Env;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

public class HashProcessor {

    private static final String HASH_PROCESSOR_DEFAULT_ALGORITHM = "HASH_PROCESSOR_DEFAULT_ALGORITHM";
    private final String hashAlgorithm;

    public HashProcessor() {
        this(Env.optional(HASH_PROCESSOR_DEFAULT_ALGORITHM).orElse(new Env.Var("SHA3-256")).asString());
    }

    public HashProcessor(String hashAlgorithm) {
        this.hashAlgorithm = hashAlgorithm;
    }


    public String hash(Map map) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        return this.hash(HashMaterial.createWith(map));
    }

    public String hash(HashMaterial material) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest digest = MessageDigest.getInstance(this.hashAlgorithm);
        byte[] hash = digest.digest(material.asBytes());
        return this.bytesToHex(hash);
    }

    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
