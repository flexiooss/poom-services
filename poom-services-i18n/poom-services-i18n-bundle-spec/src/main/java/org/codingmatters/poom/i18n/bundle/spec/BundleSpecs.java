package org.codingmatters.poom.i18n.bundle.spec;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.codingmatters.poom.i18n.bundle.spec.descriptors.BundleSpec;
import org.codingmatters.poom.i18n.bundle.spec.descriptors.json.BundleSpecReader;

import java.io.IOException;
import java.io.InputStream;

public class BundleSpecs {

    static private BundleSpecs YAML;
    static private BundleSpecs JSON;

    static synchronized public BundleSpecs fromYaml() {
        if(YAML == null) {
            YAML = new BundleSpecs(new YAMLFactory());
        }
        return YAML;
    }
    static synchronized public BundleSpecs fromJson() {
        if(JSON == null) {
            JSON = new BundleSpecs(new JsonFactory());
        }
        return JSON;
    }

    private final JsonFactory factory;

    public BundleSpecs(JsonFactory factory) {
        this.factory = factory;
    }

    public BundleSpec[] read(InputStream in) throws IOException {
        try(JsonParser parser = this.factory.createParser(in)) {
            return new BundleSpecReader().readArray(parser);
        }
    }
}
