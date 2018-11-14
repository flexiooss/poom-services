package org.codingmatters.poom.services.logging.logback.json;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.contrib.json.classic.JsonLayout;

import java.util.Map;

public class PoomJsonLayout extends JsonLayout {
    public static final String MARKER_ATTR_NAME = "marker";
    public static final String DEFAULT_MARKER = "MESSAGE";

    protected boolean includeMarker = true;

    @Override
    protected void addCustomDataToJsonMap(Map<String, Object> map, ILoggingEvent event) {
        this.add(MARKER_ATTR_NAME, this.includeMarker, marker(event), map);
        super.addCustomDataToJsonMap(map, event);
    }

    private String marker(ILoggingEvent event) {
        if(event.getMarker() != null) {
            if(event.getMarker().getName() != null) {
                return event.getMarker().getName();
            }
        }
        return DEFAULT_MARKER;
    }

    public void setIncludeMarker(boolean includeMarker) {
        this.includeMarker = includeMarker;
    }

    public boolean isIncludeMarker() {
        return includeMarker;
    }
}
