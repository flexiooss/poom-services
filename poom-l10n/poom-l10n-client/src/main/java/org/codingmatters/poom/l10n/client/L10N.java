package org.codingmatters.poom.l10n.client;

import java.time.ZoneOffset;
import java.util.Arrays;

@FunctionalInterface
public interface L10N {
    String m(String bundle, String key, Object...args);

    static L10N l10n(String localeSpec, ZoneOffset atOffset) {
        return L10NProvider.registeredProvider().l10n(localeSpec, atOffset);
    }

    class NOOP implements L10N {
        private final String localeSpec;
        private final ZoneOffset atOffset;

        public NOOP(String localeSpec, ZoneOffset atOffset) {
            this.localeSpec = localeSpec;
            this.atOffset = atOffset;
        }

        @Override
        public String m(String bundle, String key, Object... args) {
            if(args == null || args.length == 0) {
                return String.format("%s/%s(%s|%s)", bundle, key, this.localeSpec, this.atOffset);
            } else {
                return String.format("%s/%s(%s|%s):%s", bundle, key, this.localeSpec, this.atOffset, Arrays.asList(args));
            }
        }
    }
}
