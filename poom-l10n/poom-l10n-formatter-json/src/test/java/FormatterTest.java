import org.condingmatters.poom.l10n.formatter.json.Formatter;
import org.condingmatters.poom.l10n.formatter.json.JsonFormatterClient;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Locale;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class FormatterTest {
    private JsonFormatterClient client;
    private LocalDateTime localDateTime = LocalDateTime.of(LocalDate.of(2020, 10, 29), LocalTime.of(12, 30, 15, 254 * 1000000));
    private Formatter formatter;
    private final String idBundle = "json";

    @Before
    public void setUp() throws Exception {
        this.formatter = new Formatter().withBundle(idBundle, new File("spec/Localizations.json"));
    }

    @Test
    public void testEmpty() throws Exception {
        Formatter formatter = this.formatter.at(Locale.FRANCE, ZoneOffset.UTC);
        String result = formatter.format(idBundle, "not exist").here();

        assertThat(result, is("not exist"));
    }

    @Test
    public void testCompleteHere() throws Exception {
        Formatter formatter = this.formatter.at(Locale.FRANCE, ZoneOffset.UTC);
        String format = formatter.format(idBundle, "r1")
                .with("user", "Toto")
                .with("count", 16)
                .with("date", localDateTime)
                .with("minutes", 1.5)
                .with("msgs", 15000)
                .here();

        assertThat(format, is("Bonjour Toto, nous sommes le 29/10/2020 à 12:30:15 (29/10/2020 12:30:15) vous etes venus 16 fois pendant 1,5 minutes. Vous avez 15 000 messages !"));
    }

    @Test
    public void testCompleteFR() throws Exception {
        String format = this.formatter.format(idBundle, "r1")
                .with("user", "Toto")
                .with("count", 16)
                .with("date", localDateTime)
                .with("minutes", 1.5)
                .with("msgs", 15000)
                .at(Locale.FRANCE, ZoneOffset.UTC);

        assertThat(format, is("Bonjour Toto, nous sommes le 29/10/2020 à 12:30:15 (29/10/2020 12:30:15) vous etes venus 16 fois pendant 1,5 minutes. Vous avez 15 000 messages !"));
    }

    @Test
    public void testCompleteUK() throws Exception {
        LocalDateTime localDateTime = LocalDateTime.of(LocalDate.of(2020, 10, 29), LocalTime.of(12, 30, 15));

        String format = this.formatter.format(idBundle, "r1")
                .with("user", "Toto")
                .with("count", 16)
                .with("date", localDateTime)
                .with("minutes", 1.5)
                .with("msgs", 15000)
                .at(Locale.UK, ZoneOffset.UTC);

        assertThat(format, is("Hello Toto, we are on 29/10/2020 at 12:30:15 (29/10/2020, 12:30:15) you have come 16 times for 1.5 minutes. You have 15,000 messages!"));
    }

    @Test
    public void testCompleteUS() throws Exception {
        LocalDateTime localDateTime = LocalDateTime.of(LocalDate.of(2020, 10, 29), LocalTime.of(12, 30, 15));

        String format = this.formatter.format(idBundle, "r1")
                .with("user", "Toto")
                .with("count", 16)
                .with("date", localDateTime)
                .with("minutes", 1.5)
                .with("msgs", 15000)
                .at(Locale.US, ZoneOffset.UTC);

        assertThat(format, is("Hello Toto, we are on 10/29/20 at 12:30:15 PM (10/29/20, 12:30:15 PM) you have come 16 times for 1.5 minutes. You have 15,000 messages!"));
    }
}
