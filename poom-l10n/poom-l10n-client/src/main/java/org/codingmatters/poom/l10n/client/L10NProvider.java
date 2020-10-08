package org.codingmatters.poom.l10n.client;

@FunctionalInterface
public interface L10NProvider {

    L10N l10n();

    static L10NProvider registeredProvider() {
        return Registered.instance;
    }

    static void register(L10NProvider provider) {
        Registered.instance = provider;
    }

    class Registered {
        static private L10NProvider instance = () -> L10N.NOOP;
    }

}
