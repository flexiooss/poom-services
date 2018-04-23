package org.codingmatters.poom.services.support.date;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class UTC {

    static public LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC.normalized());
    }

}
