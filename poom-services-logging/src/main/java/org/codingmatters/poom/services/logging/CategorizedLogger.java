package org.codingmatters.poom.services.logging;

import org.codingmatters.poom.services.logging.marked.MarkedCategorizedLogger;
import org.slf4j.LoggerFactory;

public interface CategorizedLogger extends Log {

    static CategorizedLogger getLogger(Class<?> clazz) {
        return new MarkedCategorizedLogger(LoggerFactory.getLogger(clazz));
    }
    static CategorizedLogger getLogger(String name) {
        return new MarkedCategorizedLogger(LoggerFactory.getLogger(name));
    }

    Log personalData();

    Log confidential();

    Log audit();
}
