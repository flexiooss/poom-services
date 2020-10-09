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

public class LocaleFormatterFloatTest {
    private Map<String, Object> values = new HashMap<>();
    private LocalTime time = LocalTime.of(12, 30, 15, 200 * 1000000);
    private LocalDate date = LocalDate.of(2020, 10, 29);
    private LocalDateTime dateTime = LocalDateTime.of(date, time);
    private String s = "{a:f}";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void givenFormatInt__giveString__thenGetError() throws Exception {
        LocaleFormatter formatter = new LocaleFormatter(s, Locale.FRANCE, ZoneOffset.UTC);
        this.values.put("a", "plok");
        thrown.expect(FormatterException.class);
        formatter.format(values);
    }

    @Test
    public void givenFormatInt__giveInt__thenGetInt() throws Exception {
        LocaleFormatter formatter = new LocaleFormatter(s, Locale.FRANCE, ZoneOffset.UTC);
        this.values.put("a", 1000);
        thrown.expect(FormatterException.class);
        assertThat(formatter.format(values), is("1" + '\u202f' + "000"));
    }

    @Test
    public void givenFormatInt__giveDateTime__thenGetError() throws Exception {
        LocaleFormatter formatter = new LocaleFormatter(s, Locale.FRANCE, ZoneOffset.UTC);
        this.values.put("a", dateTime);
        thrown.expect(FormatterException.class);
        formatter.format(values);
    }

    @Test
    public void givenFormatInt__giveDate__thenGetError() throws Exception {
        LocaleFormatter formatter = new LocaleFormatter(s, Locale.FRANCE, ZoneOffset.UTC);
        this.values.put("a", date);
        thrown.expect(FormatterException.class);
        formatter.format(values);
    }

    @Test
    public void givenFormatInt__giveTime__thenGetError() throws Exception {
        LocaleFormatter formatter = new LocaleFormatter(s, Locale.FRANCE, ZoneOffset.UTC);
        this.values.put("a", time);
        thrown.expect(FormatterException.class);
        formatter.format(values);
    }

    @Test
    public void givenFormatInt__giveFloat__thenGetError() throws Exception {
        LocaleFormatter formatter = new LocaleFormatter(s, Locale.FRANCE, ZoneOffset.UTC);
        this.values.put("a", 15.5);
        String format = formatter.format(values);
        assertThat(format, is("15,5"));

        this.values.put("a", 1501520.5513120);
        format = formatter.format(values);
        assertThat(format, is("1\u202f501\u202f520,551"));

        formatter = new LocaleFormatter(s, Locale.US, ZoneOffset.UTC);
        format = formatter.format(values);
        assertThat(format, is("1,501,520.551"));
    }
}
