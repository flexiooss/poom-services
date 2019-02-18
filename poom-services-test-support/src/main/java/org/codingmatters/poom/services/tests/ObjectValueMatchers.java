package org.codingmatters.poom.services.tests;

import org.codingmatters.value.objects.values.PropertyValue;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

public class ObjectValueMatchers {
    static public Matcher<Optional<PropertyValue>> property(String [] expected) {
        PropertyValue.Value[] values = new PropertyValue.Value[expected.length];
        for (int i = 0; i < expected.length; i++) {
            values[i] = PropertyValue.builder().stringValue(expected[i]).buildValue();
        }
        return property(PropertyValue.multiple(PropertyValue.Type.STRING, values));
    }

    static public Matcher<Optional<PropertyValue>> property(String expected) {
        return property(PropertyValue.builder().stringValue(expected).build());
    }


    static public Matcher<Optional<PropertyValue>> property(Long [] expected) {
        PropertyValue.Value[] values = new PropertyValue.Value[expected.length];
        for (int i = 0; i < expected.length; i++) {
            values[i] = PropertyValue.builder().longValue(expected[i]).buildValue();
        }
        return property(PropertyValue.multiple(PropertyValue.Type.LONG, values));
    }

    static public Matcher<Optional<PropertyValue>> property(Long expected) {
        return property(PropertyValue.builder().longValue(expected).build());
    }


    static public Matcher<Optional<PropertyValue>> property(Double [] expected) {
        PropertyValue.Value[] values = new PropertyValue.Value[expected.length];
        for (int i = 0; i < expected.length; i++) {
            values[i] = PropertyValue.builder().doubleValue(expected[i]).buildValue();
        }
        return property(PropertyValue.multiple(PropertyValue.Type.DOUBLE, values));
    }

    static public Matcher<Optional<PropertyValue>> property(Double expected) {
        return property(PropertyValue.builder().doubleValue(expected).build());
    }


    static public Matcher<Optional<PropertyValue>> property(Boolean [] expected) {
        PropertyValue.Value[] values = new PropertyValue.Value[expected.length];
        for (int i = 0; i < expected.length; i++) {
            values[i] = PropertyValue.builder().booleanValue(expected[i]).buildValue();
        }
        return property(PropertyValue.multiple(PropertyValue.Type.BOOLEAN, values));
    }

    static public Matcher<Optional<PropertyValue>> property(Boolean expected) {
        return property(PropertyValue.builder().booleanValue(expected).build());
    }


    static public Matcher<Optional<PropertyValue>> property(LocalDateTime[] expected) {
        PropertyValue.Value[] values = new PropertyValue.Value[expected.length];
        for (int i = 0; i < expected.length; i++) {
            values[i] = PropertyValue.builder().datetimeValue(expected[i]).buildValue();
        }
        return property(PropertyValue.multiple(PropertyValue.Type.DATETIME, values));
    }

    static public Matcher<Optional<PropertyValue>> property(LocalDateTime expected) {
        return property(PropertyValue.builder().datetimeValue(expected).build());
    }


    static public Matcher<Optional<PropertyValue>> property(LocalDate[] expected) {
        PropertyValue.Value[] values = new PropertyValue.Value[expected.length];
        for (int i = 0; i < expected.length; i++) {
            values[i] = PropertyValue.builder().dateValue(expected[i]).buildValue();
        }
        return property(PropertyValue.multiple(PropertyValue.Type.DATE, values));
    }

    static public Matcher<Optional<PropertyValue>> property(LocalDate expected) {
        return property(PropertyValue.builder().dateValue(expected).build());
    }


    static public Matcher<Optional<PropertyValue>> property(LocalTime[] expected) {
        PropertyValue.Value[] values = new PropertyValue.Value[expected.length];
        for (int i = 0; i < expected.length; i++) {
            values[i] = PropertyValue.builder().timeValue(expected[i]).buildValue();
        }
        return property(PropertyValue.multiple(PropertyValue.Type.TIME, values));
    }

    static public Matcher<Optional<PropertyValue>> property(LocalTime expected) {
        return property(PropertyValue.builder().timeValue(expected).build());
    }



    static public Matcher<Optional<PropertyValue>> property(PropertyValue expected) {
        return new TypeSafeMatcher<Optional<PropertyValue>>() {
            @Override
            protected boolean matchesSafely(Optional<PropertyValue> actual) {
                if(expected != null) {
                    if(actual.isPresent()) {
                        return expected.equals(actual.get());
                    } else {
                        return false;
                    }
                } else {
                    return ! actual.isPresent();
                }
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(String.format("property is ")).appendValue(expected);
            }
        };
    }

}
