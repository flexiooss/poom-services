package org.codingmatters.poom.services.tests;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

public class StringMatchers {
    static public Matcher<String> matchesPattern(String pattern) {
        return new BaseMatcher<String>() {
            @Override
            public boolean matches(Object item) {
                return item != null && (item instanceof String) && item.toString().matches(pattern);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("a string matching " + pattern);
            }
        };
    }
}
