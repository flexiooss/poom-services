package org.codingmatters.poom.l10n.client;

import java.util.Arrays;

@FunctionalInterface
public interface L10N {
    String m(String bundle, String key, Object...args);

    static L10N l10n() {
        return L10NProvider.registeredProvider().l10n();
    }

    L10N NOOP = new L10N() {
        @Override
        public String m(String bundle, String key, Object... args) {
            if(args == null || args.length == 0) {
                return String.format("%s/%s", bundle, key);
            } else {
                return String.format("%s/%s:%s", bundle, key, Arrays.asList(args));
            }
        }
    };
}
