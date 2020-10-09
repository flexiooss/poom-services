package org.condingmatters.poom.l10n.formatter.json;

import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FormatterValues {
    private JsonFormatterClient client;
    private Locale defaultLocale;
    private ZoneOffset defaultOffset;

    private String key;
    private Map<String, Object> values;

    private BundleClient bundle;

    public FormatterValues(BundleClient bundle, String key, Locale defaultLocale, ZoneOffset defaultOffset) {
        this.values = new HashMap<>();
        this.bundle = bundle;
        this.key = key;
        this.defaultLocale = defaultLocale;
        this.defaultOffset = defaultOffset;
    }

    public FormatterValues with(String key, Object value) throws FormatterException {
        if (this.values.containsKey(key)) {
            throw new FormatterException("Key already set");
        }
        this.values.put(key, value);
        return this;
    }

    public String at(Locale locale, ZoneOffset offset) throws FormatterException {
        String sentence = this.bundle.get(locale, this.key);
        return new LocaleFormatter(sentence, locale, offset).format(values);
    }

    public String here() throws FormatterException {
        if (defaultLocale == null){
            throw new FormatterException("Locale not set in formatter");
        }
        if (defaultOffset == null){
            throw new FormatterException("offset not set in formatter");
        }
        String sentence = this.bundle.get(defaultLocale, this.key);
        return new LocaleFormatter(sentence, defaultLocale, defaultOffset).format(values);
    }
}
