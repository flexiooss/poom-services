package org.codingmatters.poom.services.support.paging;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Range {

    private final long start;
    private final long end;
    private final int maxPageSize;
    private boolean valid;
    private final String validationMessage;


    private Range(long start, long end, int maxPageSize, boolean valid, String validationMessage) {
        this.start = start;
        this.end = end;
        this.maxPageSize = maxPageSize;
        this.valid = valid;
        this.validationMessage = validationMessage;
    }

    public static Range fromRequestedRange(String requestedRange, int maxPageSize) {
        long startIndex = 0;
        long endIndex = startIndex + maxPageSize - 1;
        boolean valid;
        String validationMessage;


        if(requestedRange != null && ! requestedRange.isEmpty()) {
            Pattern RANGE_PATTERN = Pattern.compile("(\\d+)-(\\d+)");
            Matcher rangeMatcher = RANGE_PATTERN.matcher(requestedRange);
            if(rangeMatcher.matches()) {
                startIndex = Long.parseLong(rangeMatcher.group(1));
                endIndex = Long.parseLong(rangeMatcher.group(2));

                if(endIndex - startIndex > maxPageSize) {
                    endIndex = startIndex + maxPageSize - 1;
                }

                if(startIndex > endIndex) {
                    valid = false;
                    validationMessage = "start must be before end of range";
                } else {
                    valid = true;
                    validationMessage = null;
                }
            } else {
                valid = false;
                validationMessage = "range is not parsable";
            }
        } else {
            valid = true;
            validationMessage = null;
        }

        return new Range(startIndex, endIndex, maxPageSize, valid, validationMessage);
    }

    public long start() {
        return start;
    }

    public long end() {
        return end;
    }

    public int maxPageSize() {
        return maxPageSize;
    }

    public boolean isValid() {
        return valid;
    }

    public String validationMessage() {
        return validationMessage;
    }
}
