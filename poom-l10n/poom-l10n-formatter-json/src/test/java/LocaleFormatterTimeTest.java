import org.condingmatters.poom.l10n.formatter.json.FormatterException;
import org.condingmatters.poom.l10n.formatter.json.LocaleFormatter;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class LocaleFormatterTimeTest {
    private Map<String, Object> values = new HashMap<>();
    private LocalTime time = LocalTime.of(12,30,15,200 * 1000000);
    private LocalDate date = LocalDate.of(2020, 10, 29);
    private LocalDateTime dateTime = LocalDateTime.of(date, time);
    private String s = "{a:tt}";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void givenFormatDateTime__giveString__thenGetError() throws Exception {
        LocaleFormatter formatter = new LocaleFormatter(s, Locale.FRANCE, ZoneOffset.UTC);
        this.values.put("a", "plok");
        thrown.expect(FormatterException.class);
        formatter.format(values);
    }

    @Test
    public void givenFormatDateTime__giveInt__thenGetError() throws Exception {
        LocaleFormatter formatter = new LocaleFormatter(s, Locale.FRANCE, ZoneOffset.UTC);
        this.values.put("a", 42);
        thrown.expect(FormatterException.class);
        formatter.format(values);
    }

    @Test
    public void givenFormatDateTime__giveFloat__thenGetError() throws Exception {
        LocaleFormatter formatter = new LocaleFormatter(s, Locale.FRANCE, ZoneOffset.UTC);
        this.values.put("a", 15.5);
        thrown.expect(FormatterException.class);
        formatter.format(values);
    }

    @Test
    public void givenFormatDateTime__giveDateTime__thenGetError() throws Exception {
        LocaleFormatter formatter = new LocaleFormatter(s, Locale.FRANCE, ZoneOffset.UTC);
        this.values.put("a", time);
        assertThat(formatter.format(values), is("12:30:15"));

        formatter = new LocaleFormatter(s, Locale.FRANCE, ZoneOffset.of("+00:00"));
        this.values.put("a", dateTime);
        assertThat(formatter.format(values), is("12:30:15"));

        formatter = new LocaleFormatter(s, Locale.FRANCE, ZoneOffset.of("+02:00"));
        this.values.put("a", dateTime);
        assertThat(formatter.format(values), is("14:30:15"));

        formatter = new LocaleFormatter(s, Locale.UK, ZoneOffset.UTC);
        this.values.put("a", dateTime);
        assertThat(formatter.format(values), is("12:30:15"));

        formatter = new LocaleFormatter(s, Locale.UK, ZoneOffset.of("+02:00"));
        this.values.put("a", dateTime);
        assertThat(formatter.format(values), is("14:30:15"));

        formatter = new LocaleFormatter(s, Locale.US, ZoneOffset.UTC);
        this.values.put("a", dateTime);
        assertThat(formatter.format(values), is("12:30:15 PM"));

        formatter = new LocaleFormatter(s, Locale.US, ZoneOffset.of("+02:00"));
        this.values.put("a", dateTime);
        assertThat(formatter.format(values), is("2:30:15 PM"));
    }
}
