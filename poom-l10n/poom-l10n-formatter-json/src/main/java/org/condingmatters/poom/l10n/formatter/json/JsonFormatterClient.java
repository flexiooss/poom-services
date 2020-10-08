package org.condingmatters.poom.l10n.formatter.json;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.Map;

public class JsonFormatterClient implements BundleClient {
    private Map<String, Map<String, String>> localizations;

    public JsonFormatterClient(File path) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        URL resource = contextClassLoader.getResource(path.getPath());
        this.localizations = objectMapper.readValue(resource, Map.class);
    }

    public String get(Locale locale, String key) {
        if (locale == null) {
            return key;
        }
        String lang = locale.getLanguage();
        String variant = locale.toLanguageTag();

        if (this.localizations.containsKey(variant)) {
            return this.getKey(variant, key);
        } else {
            if (this.localizations.containsKey(lang)){
                return this.getKey(lang, key);
            }else {
                return key;
            }
        }
    }

    private String getKey(String lang, String key) {
        String line = this.localizations.get(lang).get(key);
        if (line == null) {
            return key;
        } else {
            return line;
        }
    }
}
