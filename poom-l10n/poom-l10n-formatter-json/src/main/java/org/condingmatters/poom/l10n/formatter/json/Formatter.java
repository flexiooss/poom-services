package org.condingmatters.poom.l10n.formatter.json;

import java.io.File;
import java.io.IOException;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Formatter {
    private Map<String, BundleClient> bundles;
    private Locale defaultLocale;
    private ZoneOffset defaultOffset;

    public Formatter() {
        this.bundles = new HashMap<>();
    }

    public Formatter withBundle(String idBundle, File jsonFile) throws IOException, FormatterException{
        if (this.bundles.containsKey(idBundle)) {
            throw new FormatterException("Bundle " + idBundle + " already present");
        }
        this.bundles.put(idBundle, new JsonFormatterClient(jsonFile));
        return this;
    }

    public Formatter withBundle(String idBundle, String bundle) throws IOException, FormatterException {
        if (this.bundles.containsKey(idBundle)) {
            throw new FormatterException("Bundle " + idBundle + " already present");
        }
        return this;
    }

    public Formatter at(Locale locale, ZoneOffset offset) {
        this.defaultLocale = locale;
        this.defaultOffset = offset;
        return this;
    }

    public FormatterValues format(String idBundle, String key) throws FormatterException {
        if (!this.bundles.containsKey(idBundle)) {
            throw new FormatterException("Bundle " + idBundle + " not present");
        }

        BundleClient bundle = this.bundles.get(idBundle);
        return new FormatterValues(bundle, key, defaultLocale, defaultOffset);
    }
}

