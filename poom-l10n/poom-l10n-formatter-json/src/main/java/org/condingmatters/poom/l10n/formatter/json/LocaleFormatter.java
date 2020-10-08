package org.condingmatters.poom.l10n.formatter.json;

import java.text.NumberFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocaleFormatter {
    private String sentence;
    private Map<String, Object> values;
    private Locale locale;
    private ZoneOffset offset;

    private final String regex = "\\{\\w+:\\w+\\}";
    final Pattern pattern = Pattern.compile(regex);

    public LocaleFormatter(String sentence, Locale locale, ZoneOffset offset) {
        this.sentence = sentence;
        this.locale = locale;
        this.offset = offset;
    }

    public String format(Map<String, Object> values) throws FormatterException {
        this.values = values;
        final Matcher matcher = pattern.matcher(this.sentence);
        String formattedSentence = this.sentence;
        while (matcher.find()) {
            formattedSentence = this.replace(formattedSentence, matcher.group(0));
        }
        return formattedSentence;
    }

    private String replace(String sentence, String group) throws FormatterException {
        String substring = group.substring(1, group.length() - 1);
        String[] split = substring.split(":");
        String key = split[0];
        String format = split[1];
        Object value = this.getValue(key);
        if (!this.checkValueFormat(value, format)) {
            throw new FormatterException("Value '" + value + "' cannot be formatted in this format '" + format + "'");
        }
        String formatValue = this.formatValue(value, format);
        return sentence.replaceFirst("\\{" + key + ":" + format + "\\}", formatValue);
    }

    private Object getValue(String key) throws FormatterException {
        if (!this.values.containsKey(key)) {
            throw new FormatterException("Error " + key + " not exist in values");
        }
        return this.values.get(key);
    }

    private boolean checkValueFormat(Object value, String format) throws FormatterException {
        switch (format) {
            case "s":
                return value instanceof String;
            case "f":
                return value instanceof Float || value instanceof Double;
            case "d":
                return value instanceof Byte || value instanceof Short ||
                        value instanceof Integer || value instanceof Long;
            case "t":
                return value instanceof LocalDateTime;
            case "td":
                return value instanceof LocalDateTime || value instanceof LocalDate;
            case "tt":
                return value instanceof LocalDateTime || value instanceof LocalTime;
            default:
                throw new FormatterException("Unknown format : " + format);
        }
    }

    private String formatValue(Object value, String format) throws FormatterException {
        OffsetDateTime offsetDateTime;
        switch (format) {
            case "s":
                return value.toString();
            case "f":
            case "d":
                return NumberFormat.getInstance(this.locale).format(value);
            case "t":
                return this.getDateTime(value);
            case "tt":
                return this.getTime(value);
            case "td":
                return this.getDate(value);
            default:
                throw new FormatterException("Unknown format : " + format);
        }
    }

    private String getDateTime(Object value) {
        OffsetDateTime offsetDateTime = this.offsetDateTime((LocalDateTime) value);
        DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder()
                .appendLocalized(FormatStyle.SHORT, FormatStyle.MEDIUM)
//                        .appendFraction(ChronoField.MILLI_OF_SECOND, 3, 3, true)
                .toFormatter(locale);
        return dateTimeFormatter.format(offsetDateTime);
    }

    private String getDate(Object value) {
        DateTimeFormatter timeFormatter = new DateTimeFormatterBuilder()
                .appendLocalized(FormatStyle.SHORT, null)
                .toFormatter(locale);

        if (value instanceof LocalDate) {
            return timeFormatter.format((LocalDate) value);
        }

        return timeFormatter.format(this.offsetDateTime((LocalDateTime) value));
    }

    private String getTime(Object value) {
        DateTimeFormatter timeFormatter = new DateTimeFormatterBuilder()
                .appendLocalized(null, FormatStyle.MEDIUM)
                .toFormatter(locale);

        if (value instanceof LocalTime){
         return timeFormatter.format((LocalTime)value);
        }

        return timeFormatter.format(this.offsetDateTime((LocalDateTime) value));
    }

    private OffsetDateTime offsetDateTime(LocalDateTime dateTime) {
        Instant instant = dateTime.toInstant(ZoneOffset.UTC);
        return instant.atOffset(this.offset);
    }
}
