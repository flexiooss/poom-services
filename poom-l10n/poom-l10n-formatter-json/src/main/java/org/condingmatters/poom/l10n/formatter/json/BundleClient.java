package org.condingmatters.poom.l10n.formatter.json;

import java.util.Locale;

public interface BundleClient {
    String get(Locale locale, String key);

}
