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

public class LocaleFormatterStringTest {
    private Map<String, Object> values = new HashMap<>();
    private LocalTime time = LocalTime.of(12,30,15,200 * 1000000);
    private LocalDate date = LocalDate.of(2020, 10, 29);
    private LocalDateTime dateTime = LocalDateTime.of(date, time);

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void givenFormatString__giveString__thenGetString() throws Exception {
        String s = "{a:s}";
        LocaleFormatter formatter = new LocaleFormatter(s, Locale.FRANCE, ZoneOffset.UTC);
        this.values.put("a", "plok");
        assertThat(formatter.format(values), is("plok"));
    }

    @Test
    public void givenFormatString__giveInt__thenGetError() throws Exception {
        String s = "{a:s}";
        LocaleFormatter formatter = new LocaleFormatter(s, Locale.FRANCE, ZoneOffset.UTC);
        this.values.put("a", 16);
        thrown.expect(FormatterException.class);
        assertThat(formatter.format(values), is("16"));
    }

    @Test
    public void givenFormatString__giveFloat__thenGetError() throws Exception {
        String s = "{a:s}";
        LocaleFormatter formatter = new LocaleFormatter(s, Locale.FRANCE, ZoneOffset.UTC);
        this.values.put("a", 1.6F);
        thrown.expect(FormatterException.class);
        formatter.format(values);
    }

    @Test
    public void givenFormatString__giveDateTime__thenGetError() throws Exception {
        String s = "{a:s}";
        LocaleFormatter formatter = new LocaleFormatter(s, Locale.FRANCE, ZoneOffset.UTC);
        this.values.put("a", dateTime);
        thrown.expect(FormatterException.class);
        formatter.format(values);
    }

    @Test
    public void givenFormatString__giveDate__thenGetError() throws Exception {
        String s = "{a:s}";
        LocaleFormatter formatter = new LocaleFormatter(s, Locale.FRANCE, ZoneOffset.UTC);
        this.values.put("a", date);
        thrown.expect(FormatterException.class);
        formatter.format(values);
    }

    @Test
    public void givenFormatString__giveTime__thenGetError() throws Exception {
        String s = "{a:s}";
        LocaleFormatter formatter = new LocaleFormatter(s, Locale.FRANCE, ZoneOffset.UTC);
        this.values.put("a", time);
        thrown.expect(FormatterException.class);
        formatter.format(values);
    }
}
