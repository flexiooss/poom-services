package org.codingmatters.poom.l10n.client;

import org.codingmatters.poom.services.support.date.UTC;

import java.time.ZoneOffset;
import java.util.TimeZone;

public interface ZoneOffsetProvider {

    static ZoneOffset at() {
        return Registerd.zoneOffsetThreadLocal.get();
    }

    static void set(ZoneOffset at) {
        Registerd.zoneOffsetThreadLocal.set(at);
    }

    class Registerd {
        static ThreadLocal<ZoneOffset> zoneOffsetThreadLocal = new InheritableThreadLocal<>() {
            @Override
            protected ZoneOffset initialValue() {
                return TimeZone.getTimeZone("Europe/Paris").toZoneId().getRules().getOffset(UTC.now());
            }
        };
    }
}
