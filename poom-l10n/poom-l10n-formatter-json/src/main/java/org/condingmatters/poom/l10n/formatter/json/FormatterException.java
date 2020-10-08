package org.condingmatters.poom.l10n.formatter.json;

public class FormatterException extends Exception {
    public FormatterException(String s) {
        super(s);
    }

    public FormatterException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
