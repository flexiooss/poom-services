package org.codingmatters.poom.l10n.client;

import java.time.ZoneOffset;
import java.util.Arrays;

@FunctionalInterface
public interface L10N {
    String m(String bundle, String key, Object...args);
    default Message message(String bundle, String key) {
        return new Message(this, bundle, key);
    }


    static L10N l10n() {
        return L10N.l10n(null, null);
    }

    static L10N l10n(String localeSpec) {
        return L10N.l10n(localeSpec, null);
    }

    static L10N l10n(ZoneOffset atOffset) {
        return L10N.l10n(null, atOffset);
    }

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

    class Message implements MessageSec {
        private final L10N l10N;
        private final String bundle;
        private final String key;

        public Message(L10N l10N, String bundle, String key) {
            this.l10N = l10N;
            this.bundle = bundle;
            this.key = key;
        }

        public String m(Object... args) {
            return this.l10N.m(this.bundle, this.key, args);
        }

        public String bundle() {
            return this.bundle;
        }
        public String key() {
            return key;
        }
    }

    interface MessageSec {
        String bundle();
        String key();
    }
}
