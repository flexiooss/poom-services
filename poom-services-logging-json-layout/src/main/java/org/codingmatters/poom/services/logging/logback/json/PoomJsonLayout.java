package org.codingmatters.poom.services.logging.logback.json;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.contrib.json.classic.JsonLayout;

import java.util.Map;

public class PoomJsonLayout extends JsonLayout {
    public static final String MARKER_ATTR_NAME = "marker";

    protected boolean includeMarker = true;

    @Override
    protected void addCustomDataToJsonMap(Map<String, Object> map, ILoggingEvent event) {
        this.add(MARKER_ATTR_NAME, this.includeMarker, event.getMarker().getName(), map);
        super.addCustomDataToJsonMap(map, event);
    }

    public void setIncludeMarker(boolean includeMarker) {
        this.includeMarker = includeMarker;
    }

    public boolean isIncludeMarker() {
        return includeMarker;
    }
}
