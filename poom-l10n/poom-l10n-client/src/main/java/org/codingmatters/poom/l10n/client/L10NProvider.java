package org.codingmatters.poom.l10n.client;

import java.time.ZoneOffset;

@FunctionalInterface
public interface L10NProvider {

    L10N l10n(String localeSpec, ZoneOffset atOffset);

    static L10NProvider registeredProvider() {
        return Registered.instance;
    }

    static void register(L10NProvider provider) {
        Registered.instance = provider;
    }

    class Registered {
        static private L10NProvider instance = (localeSpec, atOffset) -> new L10N.NOOP(localeSpec, atOffset);
    }

}
