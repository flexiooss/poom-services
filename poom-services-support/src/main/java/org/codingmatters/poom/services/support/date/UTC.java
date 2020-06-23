package org.codingmatters.poom.services.support.date;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.TimeZone;

public class UTC {

    static public LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC.normalized());
    }

    static public LocalDateTime from(LocalDateTime now, TimeZone tz) {
        ZonedDateTime atZone = now.atZone(tz.toZoneId());
        return atZone.withZoneSameInstant(ZoneOffset.UTC.normalized()).toLocalDateTime();
    }

    static public LocalDateTime from(ZonedDateTime zoned) {
        return zoned.withZoneSameInstant(ZoneOffset.UTC.normalized()).toLocalDateTime();
    }

    static public LocalDateTime at(LocalDateTime utc, TimeZone tz) {
        ZonedDateTime atZone = utc.atZone(ZoneOffset.UTC.normalized());
        return atZone.withZoneSameInstant(tz.toZoneId()).toLocalDateTime();
    }

    static public LocalDateTime at(TimeZone tz) {
        return at(now(), tz);
    }
}
