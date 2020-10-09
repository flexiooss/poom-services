package org.codingmatters.poom.l10n.client;

import java.util.Arrays;

@FunctionalInterface
public interface L10N {
    String m(String bundle, String key, Object...args);

    static L10N l10n(String localeSpec) {
        return L10NProvider.registeredProvider().l10n(localeSpec);
    }

    class NOOP implements L10N {
        private final String localeSpec;

        public NOOP(String localeSpec) {
            this.localeSpec = localeSpec;
        }

        @Override
        public String m(String bundle, String key, Object... args) {
            if(args == null || args.length == 0) {
                return String.format("%s/%s(%s)", bundle, key, this.localeSpec);
            } else {
                return String.format("%s/%s(%s):%s", bundle, key, this.localeSpec, Arrays.asList(args));
            }
        }
    }
}
