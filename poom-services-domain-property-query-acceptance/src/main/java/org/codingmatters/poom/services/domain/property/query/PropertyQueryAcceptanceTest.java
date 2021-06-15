package org.codingmatters.poom.services.domain.property.query;

import org.codingmatters.generated.QAValue;
import org.codingmatters.generated.qavalue.Nested;
import org.codingmatters.generated.qavalue.nested.Deep;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;
import org.junit.Before;
import org.junit.Test;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public abstract class PropertyQueryAcceptanceTest {

    static public final LocalDate BASE_DATE = LocalDate.of(1985, 06, 12);
    static public final LocalTime BASE_TIME = LocalTime.of(12, 42, 33, 123456789);
    static public final LocalDateTime BASE_DATETIME = LocalDateTime.of(BASE_DATE, BASE_TIME);
    static public final ZonedDateTime BASE_ZONED_DATETIME = ZonedDateTime.of(BASE_DATETIME, ZoneId.of("+05:00"));

    private Repository<QAValue, PropertyQuery> repository;

    @Before
    public void setUp() throws Exception {
        this.repository = this.createRepository();

        for (int i = 0; i < 100; i++) {
            Boolean bool;
            switch (i % 3) {
                case 0:
                    bool = true;
                    break;
                case 1:
                    bool = false;
                    break;
                default:
                    bool = null;
            }
            QAValue value = QAValue.builder()
                    .stringProp("%03d", i)
                    .integerProp(i)
                    .longProp((long) i)
                    .floatProp(i + 0.2f)
                    .doubleProp(i + 0.2d)
                    .dateProp(BASE_DATE.plusDays(i))
                    .datetimeProp(BASE_DATETIME.plusDays(i))
                    .tzdatetimeProp(BASE_ZONED_DATETIME.plusDays(i))
                    .timeProp(BASE_TIME.plusMinutes(i))
                    .boolProp(bool)
                    .nested(Nested.builder()
                            .nestedProp("%03d", 100 - i)
                            .deep(Deep.builder().deepProp("04").build())
                            .build())
                    .build();
//            System.out.printf("VAL :: %s :: %s\n", value.stringProp(), DateTimeFormatter.ISO_ZONED_DATE_TIME.format(value.tzdatetimeProp()));
            Entity<QAValue> e = this.repository.create(value);
//            System.out.printf("INS :: %s :: %s\n", e.value().stringProp(), DateTimeFormatter.ISO_ZONED_DATE_TIME.format(e.value().tzdatetimeProp()));
        }
    }

    protected abstract Repository<QAValue, PropertyQuery> createRepository();

    protected Repository<QAValue, PropertyQuery> repository() {
        return this.repository;
    }

    @Test
    public void whenNoFilter_andNoOrderBy__thenAllValuesReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder().build(), 0, 1000);

        assertThat(actual.total(), is(this.repository.all(0, 0).total()));
    }

    @Test
    public void givenFilterOnStringProperty__whenIsEqual__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("stringProp == '006'")
                .build(), 0, 1000);

        assertThat(actual.total(), is(1L));
        assertThat(actual.get(0).value().stringProp(), is("006"));
    }

    @Test
    public void givenFilterOnStringProperty__whenIsIn__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("stringProp in ('042', '006', '012')")
                .build(), 0, 1000);

        assertThat(actual.total(), is(3L));

        assertThat(actual.valueList().stream().map(v -> v.stringProp()).collect(Collectors.toList()), containsInAnyOrder("006", "012", "042"));
    }

    @Test
    public void givenFilterOnStringProperty__whenMultivaluedContains__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("stringProp contains ('40', '006', '01')")
                .build(), 0, 1000);

        assertThat(actual.total(), is(13L));

        assertThat(actual.valueList().stream().map(v -> v.stringProp()).collect(Collectors.toList()), containsInAnyOrder(
                "006", "040", "001", "010", "011", "012", "013", "014", "015", "016", "017", "018", "019"
        ));
    }

    @Test
    public void givenFilterOnStringProperty__whenIsIn_orIsEquals__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("stringProp in ('042', '012') || stringProp == '006'")
                .build(), 0, 1000);

        assertThat(actual.total(), is(3L));

        assertThat(actual.valueList().stream().map(v -> v.stringProp()).collect(Collectors.toList()), containsInAnyOrder("006", "012", "042"));
    }

    @Test
    public void givenFilterOnIntegerProperty__whenIsEqual__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("integerProp == 6")
                .build(), 0, 1000);

        assertThat(actual.total(), is(1L));
        assertThat(actual.get(0).value().integerProp(), is(6));
    }

    @Test
    public void givenFilterOnLongProperty__whenIsEqual__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("longProp == 6")
                .build(), 0, 1000);

        assertThat(actual.total(), is(1L));
        assertThat(actual.get(0).value().longProp(), is(6L));
    }

    @Test
    public void givenFilterOnFloatProperty__whenIsEqual__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("floatProp == 6.2")
                .build(), 0, 1000);

        assertThat(actual.total(), is(1L));
        assertThat(actual.get(0).value().floatProp(), is(6.2f));
    }

    @Test
    public void givenFilterOnDoubleProperty__whenIsEqual__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("doubleProp == 6.2")
                .build(), 0, 1000);

        assertThat(actual.total(), is(1L));
        assertThat(actual.get(0).value().doubleProp(), is(6.2d));
    }

    @Test
    public void givenFilterOnDateProperty__whenIsEqual__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("dateProp == 1985-06-17")
                .build(), 0, 1000);

        assertThat(actual.total(), is(1L));
        assertThat(actual.get(0).value().dateProp(), is(LocalDate.of(1985, 6, 17)));
    }

    @Test
    public void givenFilterOnTimeProperty__whenIsEqual__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("timeProp == 12:47:33.123456789")
                .build(), 0, 1000);

        assertThat(actual.total(), is(1L));
        assertThat(DateTimeFormatter.ISO_LOCAL_TIME.format(actual.get(0).value().timeProp()), is(ts("12:47:33.123456789")));
    }

    @Test
    public void givenFilterOnDatetimeProperty__whenIsEqual__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("datetimeProp == 1985-06-17T12:42:33.123456789")
                .build(), 0, 1000);

        assertThat(actual.total(), is(1L));
        assertThat(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(actual.get(0).value().datetimeProp()), is(ts("1985-06-17T12:42:33.123456789")));
    }

    @Test
    public void givenFilterOnZonedDatetimeProperty__whenIsEqual__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("tzdatetimeProp == 1985-06-17T12:42:33.123456789+05:00")
                .build(), 0, 1000);

        assertThat(actual.total(), is(1L));
        assertThat(DateTimeFormatter.ISO_ZONED_DATE_TIME.format(actual.get(0).value().tzdatetimeProp()), is(ts("1985-06-17T12:42:33.123456789+05:00")));
    }

    @Test
    public void givenFilterOnBooleanProperty__whenIsEqualTrue__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("boolProp == true")
                .build(), 0, 1000);

        assertThat(actual.total(), is(34L));
        for (QAValue value : actual.valueList()) {
            assertThat(value.toString(), value.boolProp(), is(true));
        }
    }

    @Test
    public void givenFilterOnBooleanProperty__whenIsEqualFalse__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("boolProp == false")
                .build(), 0, 1000);

        assertThat(actual.total(), is(33L));
        for (QAValue value : actual.valueList()) {
            assertThat(value.toString(), value.boolProp(), is(false));
        }
    }

    @Test
    public void givenFilterOnBooleanProperty__whenIsNull__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("boolProp == null")
                .build(), 0, 1000);

        assertThat(actual.total(), is(33L));
        for (QAValue value : actual.valueList()) {
            assertThat(value.toString(), value.boolProp(), is(nullValue()));
        }
    }

    @Test
    public void givenFilterOnStringNestedProperty__whenIsEqual__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("nested.nestedProp == '006'")
                .build(), 0, 1000);

        assertThat(actual.total(), is(1L));
        assertThat(actual.get(0).value().nested().nestedProp(), is("006"));
    }

    @Test
    public void givenFilterOnStringProperty__whenIsEqualProperty__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("stringProp == nested.nestedProp")
                .build(), 0, 1000);

        assertThat(actual.total(), is(1L));
        assertThat(actual.get(0).value().stringProp(), is("050"));
        assertThat(actual.get(0).value().nested().nestedProp(), is("050"));
    }

    @Test
    public void givenFilterOnStringProperty__whenNotIsEqual__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("stringProp != '006'")
                .build(), 0, 1000);

        assertThat(actual.total(), is(99L));
        assertThat(
                actual.valueList().stream().map(complexValue -> complexValue.stringProp()).toArray(),
                not(hasItemInArray("006"))
        );
    }

    @Test
    public void givenFilterOnIntegerProperty__whenNotIsEqual__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("integerProp != 6")
                .build(), 0, 1000);

        assertThat(actual.total(), is(99L));
        assertThat(
                actual.valueList().stream().map(complexValue -> complexValue.integerProp()).toArray(),
                not(hasItemInArray(6))
        );
    }

    @Test
    public void givenFilterOnLongProperty__whenNotIsEqual__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("longProp != 6")
                .build(), 0, 1000);

        assertThat(actual.total(), is(99L));
        assertThat(
                actual.valueList().stream().map(complexValue -> complexValue.longProp()).toArray(),
                not(hasItemInArray(6L))
        );
    }

    @Test
    public void givenFilterOnFloatProperty__whenNotIsEqual__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("floatProp != 6.2")
                .build(), 0, 1000);

        assertThat(actual.total(), is(99L));
        assertThat(
                actual.valueList().stream().map(complexValue -> complexValue.floatProp()).toArray(),
                not(hasItemInArray(6.2f))
        );
    }

    @Test
    public void givenFilterOnDoubleProperty__whenNotIsEqual__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("doubleProp != 6.2")
                .build(), 0, 1000);

        assertThat(actual.total(), is(99L));
        assertThat(
                actual.valueList().stream().map(complexValue -> complexValue.doubleProp()).toArray(),
                not(hasItemInArray(6.2d))
        );
    }

    @Test
    public void givenFilterOnDateProperty__whenNotIsEqual__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("dateProp != 1985-06-17")
                .build(), 0, 1000);

        assertThat(actual.total(), is(99L));
        assertThat(
                actual.valueList().stream().map(complexValue -> complexValue.dateProp()).toArray(),
                not(hasItemInArray(LocalDate.of(1985, 6, 17)))
        );
    }

    @Test
    public void givenFilterOnTimeProperty__whenNotIsEqual__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("timeProp != 12:47:33.123456789")
                .build(), 0, 1000);

        assertThat(actual.total(), is(99L));
        assertThat(
                actual.valueList().stream().map(complexValue -> complexValue.timeProp()).toArray(),
                not(hasItemInArray(LocalTime.of(12, 47, 33, 123456789)))
        );
    }

    @Test
    public void givenFilterOnDateTimeProperty__whenNotIsEqual__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("datetimeProp != 1985-06-17T12:42:33.123456789")
                .build(), 0, 1000);

        assertThat(actual.total(), is(99L));
        assertThat(
                actual.valueList().stream().map(complexValue -> complexValue.datetimeProp()).toArray(),
                not(hasItemInArray(LocalDateTime.of(1985, 6, 17, 12, 42, 33, 123456789)))
        );
    }

    @Test
    public void givenFilterOnZonedDateTimeProperty__whenNotIsEqual__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("tzdatetimeProp != 1985-06-17T12:42:33.123456789+05:00")
                .build(), 0, 1000);

        assertThat(actual.total(), is(99L));
        assertThat(
                actual.valueList().stream().map(complexValue -> complexValue.tzdatetimeProp()).toArray(),
                not(hasItemInArray(ZonedDateTime.of(1985, 6, 17, 12, 42, 33, 123456789, ZoneId.of("+05:00"))))
        );
    }

    @Test
    public void givenFilterOnStringProperty__whenNotIsEqualProperty__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("stringProp != nested.nestedProp")
                .build(), 0, 1000);

        assertThat(actual.total(), is(99L));
        assertThat(
                actual.valueList().stream().map(complexValue -> complexValue.stringProp()).toArray(),
                not(hasItemInArray("050"))
        );
    }

    @Test
    public void givenFilterOnStringProperty__whenIsNull__thenSelectedValueReturned() throws Exception {
        Entity<QAValue> entity = this.repository.create(QAValue.builder().stringProp(null).build());

        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("stringProp == null")
                .build(), 0, 1000);

        assertThat(actual.total(), is(1L));
        assertThat(actual.get(0).id(), is(entity.id()));
    }

    @Test
    public void givenFilterOnIntegerProperty__whenIsNull__thenSelectedValueReturned() throws Exception {
        Entity<QAValue> entity = this.repository.create(QAValue.builder().integerProp(null).build());

        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("integerProp == null")
                .build(), 0, 1000);

        assertThat(actual.total(), is(1L));
        assertThat(actual.get(0).id(), is(entity.id()));
    }

    @Test
    public void givenFilterOnLongProperty__whenIsNull__thenSelectedValueReturned() throws Exception {
        Entity<QAValue> entity = this.repository.create(QAValue.builder().longProp(null).build());

        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("longProp == null")
                .build(), 0, 1000);

        assertThat(actual.total(), is(1L));
        assertThat(actual.get(0).id(), is(entity.id()));
    }

    @Test
    public void givenFilterOnFloatProperty__whenIsNull__thenSelectedValueReturned() throws Exception {
        Entity<QAValue> entity = this.repository.create(QAValue.builder().floatProp(null).build());

        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("floatProp == null")
                .build(), 0, 1000);

        assertThat(actual.total(), is(1L));
        assertThat(actual.get(0).id(), is(entity.id()));
    }

    @Test
    public void givenFilterOnDoubleProperty__whenIsNull__thenSelectedValueReturned() throws Exception {
        Entity<QAValue> entity = this.repository.create(QAValue.builder().doubleProp(null).build());

        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("doubleProp == null")
                .build(), 0, 1000);

        assertThat(actual.total(), is(1L));
        assertThat(actual.get(0).id(), is(entity.id()));
    }

    @Test
    public void givenFilterOnDateProperty__whenIsNull__thenSelectedValueReturned() throws Exception {
        Entity<QAValue> entity = this.repository.create(QAValue.builder().dateProp(null).build());

        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("dateProp == null")
                .build(), 0, 1000);

        assertThat(actual.total(), is(1L));
        assertThat(actual.get(0).id(), is(entity.id()));
    }

    @Test
    public void givenFilterOnTimeProperty__whenIsNull__thenSelectedValueReturned() throws Exception {
        Entity<QAValue> entity = this.repository.create(QAValue.builder().timeProp(null).build());

        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("timeProp == null")
                .build(), 0, 1000);

        assertThat(actual.total(), is(1L));
        assertThat(actual.get(0).id(), is(entity.id()));
    }

    @Test
    public void givenFilterOnDateTimeProperty__whenIsNull__thenSelectedValueReturned() throws Exception {
        Entity<QAValue> entity = this.repository.create(QAValue.builder().datetimeProp(null).build());

        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("datetimeProp == null")
                .build(), 0, 1000);

        assertThat(actual.total(), is(1L));
        assertThat(actual.get(0).id(), is(entity.id()));
    }

    @Test
    public void givenFilterOnZonedDateTimeProperty__whenIsNull__thenSelectedValueReturned() throws Exception {
        Entity<QAValue> entity = this.repository.create(QAValue.builder().tzdatetimeProp(null).build());

        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("tzdatetimeProp == null")
                .build(), 0, 1000);

        assertThat(actual.total(), is(1L));
        assertThat(actual.get(0).id(), is(entity.id()));
    }

    @Test
    public void givenFilterOnStringProperty__whenIsNotNull__thenSelectedValueReturned() throws Exception {
        Entity<QAValue> entity = this.repository.create(QAValue.builder().stringProp(null).build());

        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("stringProp != null")
                .build(), 0, 1000);

        assertThat(actual.total(), is(100L));
        assertThat(
                actual.stream().map(complexValueEntity -> complexValueEntity.id()).toArray(),
                not(hasItemInArray(entity.id()))
        );
    }

    @Test
    public void givenFilterOnIntegerProperty__whenIsNotNull__thenSelectedValueReturned() throws Exception {
        Entity<QAValue> entity = this.repository.create(QAValue.builder().integerProp(null).build());

        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("integerProp != null")
                .build(), 0, 1000);

        assertThat(actual.total(), is(100L));
        assertThat(
                actual.stream().map(complexValueEntity -> complexValueEntity.id()).toArray(),
                not(hasItemInArray(entity.id()))
        );
    }

    @Test
    public void givenFilterOnLongProperty__whenIsNotNull__thenSelectedValueReturned() throws Exception {
        Entity<QAValue> entity = this.repository.create(QAValue.builder().longProp(null).build());

        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("longProp != null")
                .build(), 0, 1000);

        assertThat(actual.total(), is(100L));
        assertThat(
                actual.stream().map(complexValueEntity -> complexValueEntity.id()).toArray(),
                not(hasItemInArray(entity.id()))
        );
    }

    @Test
    public void givenFilterOnFloatProperty__whenIsNotNull__thenSelectedValueReturned() throws Exception {
        Entity<QAValue> entity = this.repository.create(QAValue.builder().floatProp(null).build());

        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("floatProp != null")
                .build(), 0, 1000);

        assertThat(actual.total(), is(100L));
        assertThat(
                actual.stream().map(complexValueEntity -> complexValueEntity.id()).toArray(),
                not(hasItemInArray(entity.id()))
        );
    }

    @Test
    public void givenFilterOnDoubleProperty__whenIsNotNull__thenSelectedValueReturned() throws Exception {
        Entity<QAValue> entity = this.repository.create(QAValue.builder().floatProp(null).build());

        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("doubleProp != null")
                .build(), 0, 1000);

        assertThat(actual.total(), is(100L));
        assertThat(
                actual.stream().map(complexValueEntity -> complexValueEntity.id()).toArray(),
                not(hasItemInArray(entity.id()))
        );
    }

    @Test
    public void givenFilterOnDateProperty__whenIsNotNull__thenSelectedValueReturned() throws Exception {
        Entity<QAValue> entity = this.repository.create(QAValue.builder().dateProp(null).build());

        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("dateProp != null")
                .build(), 0, 1000);

        assertThat(actual.total(), is(100L));
        assertThat(
                actual.stream().map(complexValueEntity -> complexValueEntity.id()).toArray(),
                not(hasItemInArray(entity.id()))
        );
    }

    @Test
    public void givenFilterOnTimeProperty__whenIsNotNull__thenSelectedValueReturned() throws Exception {
        Entity<QAValue> entity = this.repository.create(QAValue.builder().timeProp(null).build());

        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("timeProp != null")
                .build(), 0, 1000);

        assertThat(actual.total(), is(100L));
        assertThat(
                actual.stream().map(complexValueEntity -> complexValueEntity.id()).toArray(),
                not(hasItemInArray(entity.id()))
        );
    }

    @Test
    public void givenFilterOnDateTimeProperty__whenIsNotNull__thenSelectedValueReturned() throws Exception {
        Entity<QAValue> entity = this.repository.create(QAValue.builder().datetimeProp(null).build());

        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("datetimeProp != null")
                .build(), 0, 1000);

        assertThat(actual.total(), is(100L));
        assertThat(
                actual.stream().map(complexValueEntity -> complexValueEntity.id()).toArray(),
                not(hasItemInArray(entity.id()))
        );
    }

    @Test
    public void givenFilterOnZonedDateTimeProperty__whenIsNotNull__thenSelectedValueReturned() throws Exception {
        Entity<QAValue> entity = this.repository.create(QAValue.builder().tzdatetimeProp(null).build());

        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("tzdatetimeProp != null")
                .build(), 0, 1000);

        assertThat(actual.total(), is(100L));
        assertThat(
                actual.stream().map(complexValueEntity -> complexValueEntity.id()).toArray(),
                not(hasItemInArray(entity.id()))
        );
    }

    @Test
    public void givenFilterOnStringProperty__whenGraterThan__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("stringProp > '097'")
                .build(), 0, 1000);

        assertThat(actual.total(), is(2L));
        assertThat(
                actual.valueList().stream().map(complexValue -> complexValue.stringProp()).toArray(),
                is(arrayContainingInAnyOrder("098", "099"))
        );
    }

    @Test
    public void givenFilterOnDateProperty__whenGraterThan__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("dateProp > 1985-09-17")
                .build(), 0, 1000);

        assertThat(actual.total(), is(2L));
        assertThat(
                actual.valueList().stream().map(complexValue -> DateTimeFormatter.ISO_LOCAL_DATE.format(complexValue.dateProp())).toArray(),
                is(arrayContainingInAnyOrder("1985-09-18", "1985-09-19"))
        );
    }

    @Test
    public void givenFilterOnTimeProperty__whenGraterThan__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("timeProp > 14:19:33.123456789")
                .build(), 0, 1000);

        assertThat(actual.total(), is(2L));
        assertThat(
                actual.valueList().stream().map(complexValue -> DateTimeFormatter.ISO_LOCAL_TIME.format(complexValue.timeProp())).toArray(),
                is(arrayContainingInAnyOrder(ts("14:20:33.123456789"), ts("14:21:33.123456789")))
        );
    }

    @Test
    public void givenFilterOnDateTimeProperty__whenGraterThan__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("datetimeProp > 1985-09-17T12:42:33.123456789")
                .build(), 0, 1000);

        assertThat(actual.total(), is(2L));
        assertThat(
                actual.valueList().stream().map(complexValue -> DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(complexValue.datetimeProp())).toArray(),
                is(arrayContainingInAnyOrder(ts("1985-09-18T12:42:33.123456789"), ts("1985-09-19T12:42:33.123456789")))
        );
    }

    protected String ts(String full) {
        return full;
    }

    @Test
    public void givenFilterOnZonedDateTimeProperty__whenGraterThan__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("tzdatetimeProp > 1985-09-17T12:42:33.123456789+05:00")
                .build(), 0, 1000);

        System.out.println(actual.valueList().stream().map(complexValue -> DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(complexValue.tzdatetimeProp())).toArray());
        assertThat(actual.total(), is(2L));
        assertThat(
                actual.valueList().stream().map(complexValue -> DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(complexValue.tzdatetimeProp())).toArray(),
                is(arrayContainingInAnyOrder(ts("1985-09-18T12:42:33.123456789+05:00"), ts("1985-09-19T12:42:33.123456789+05:00")))
        );
    }

    @Test
    public void givenFilterOnIntegerProperty__whenGraterThan__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("integerProp > 97")
                .build(), 0, 1000);

        assertThat(actual.total(), is(2L));
        assertThat(
                actual.valueList().stream().map(complexValue -> complexValue.integerProp()).toArray(),
                is(arrayContainingInAnyOrder(98, 99))
        );
    }

    @Test
    public void givenFilterOnLongProperty__whenGraterThan__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("longProp > 97")
                .build(), 0, 1000);

        assertThat(actual.total(), is(2L));
        assertThat(
                actual.valueList().stream().map(complexValue -> complexValue.longProp()).toArray(),
                is(arrayContainingInAnyOrder(98L, 99L))
        );
    }

    @Test
    public void givenFilterOnFloatProperty__whenGraterThan__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("floatProp > 97.2")
                .build(), 0, 1000);

        assertThat(actual.total(), is(2L));
        assertThat(
                actual.valueList().stream().map(complexValue -> complexValue.floatProp()).toArray(),
                is(arrayContainingInAnyOrder(98.2f, 99.2f))
        );
    }

    @Test
    public void givenFilterOnDoubleProperty__whenGraterThan__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("doubleProp > 97.2")
                .build(), 0, 1000);

        assertThat(actual.total(), is(2L));
        assertThat(
                actual.valueList().stream().map(complexValue -> complexValue.doubleProp()).toArray(),
                is(arrayContainingInAnyOrder(98.2d, 99.2d))
        );
    }

    @Test
    public void givenFilterOnStringProperty__whenGraterThanProperty__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("stringProp > nested.nestedProp")
                .build(), 0, 1000);

        assertThat(actual.total(), is(49L));
        for (Entity<QAValue> entity : actual) {
            assertThat(entity.value().toString(), entity.value().stringProp(), is(greaterThan(entity.value().nested().nestedProp())));
        }
    }

    @Test
    public void givenFilterOnStringProperty__whenGraterThanOrEquals__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("stringProp >= '097'")
                .build(), 0, 1000);

        assertThat(actual.total(), is(3L));
        assertThat(
                actual.valueList().stream().map(complexValue -> complexValue.stringProp()).toArray(),
                is(arrayContainingInAnyOrder("097", "098", "099"))
        );
    }

    @Test
    public void givenFilterOnDateProperty__whenGraterThanOrEquals__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("dateProp >= 1985-09-17")
                .build(), 0, 1000);

        assertThat(actual.total(), is(3L));
        assertThat(
                actual.valueList().stream().map(complexValue -> DateTimeFormatter.ISO_LOCAL_DATE.format(complexValue.dateProp())).toArray(),
                is(arrayContainingInAnyOrder("1985-09-17", "1985-09-18", "1985-09-19"))
        );
    }

    @Test
    public void givenFilterOnTimeProperty__whenGraterThanOrEquals__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("timeProp >= 14:19:33.123456789")
                .build(), 0, 1000);

        System.out.println(actual.valueList().stream().map(complexValue -> DateTimeFormatter.ISO_LOCAL_TIME.format(complexValue.timeProp())).toArray());
        assertThat(actual.total(), is(3L));
        assertThat(
                actual.valueList().stream().map(complexValue -> DateTimeFormatter.ISO_LOCAL_TIME.format(complexValue.timeProp())).toArray(),
                is(arrayContainingInAnyOrder(ts("14:19:33.123456789"), ts("14:20:33.123456789"), ts("14:21:33.123456789")))
        );
    }

    @Test
    public void givenFilterOnDateTimeProperty__whenGraterThanOrEquals__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("datetimeProp >= 1985-09-17T12:42:33.123456789")
                .build(), 0, 1000);

        assertThat(actual.total(), is(3L));
        assertThat(
                actual.valueList().stream().map(complexValue -> DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(complexValue.datetimeProp())).toArray(),
                is(arrayContainingInAnyOrder(ts("1985-09-17T12:42:33.123456789"), ts("1985-09-18T12:42:33.123456789"), ts("1985-09-19T12:42:33.123456789")))
        );
    }

    @Test
    public void givenFilterOnZonedDateTimeProperty__whenGraterThanOrEquals__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("tzdatetimeProp >= 1985-09-17T12:42:33.123456789+05:00")
                .build(), 0, 1000);

        System.out.println(actual.valueList().stream().map(complexValue -> DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(complexValue.tzdatetimeProp())).toArray());
        assertThat(actual.total(), is(3L));
        assertThat(
                actual.valueList().stream().map(complexValue -> DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(complexValue.tzdatetimeProp())).toArray(),
                is(arrayContainingInAnyOrder(ts("1985-09-17T12:42:33.123456789+05:00"), ts("1985-09-18T12:42:33.123456789+05:00"), ts("1985-09-19T12:42:33.123456789+05:00")))
        );
    }

    @Test
    public void givenFilterOnIntegerProperty__whenGraterThanOrEquals__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("integerProp >= 97")
                .build(), 0, 1000);

        assertThat(actual.total(), is(3L));
        assertThat(
                actual.valueList().stream().map(complexValue -> complexValue.integerProp()).toArray(),
                is(arrayContainingInAnyOrder(97, 98, 99))
        );
    }

    @Test
    public void givenFilterOnLongProperty__whenGraterThanOrEquals__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("longProp >= 97")
                .build(), 0, 1000);

        assertThat(actual.total(), is(3L));
        assertThat(
                actual.valueList().stream().map(complexValue -> complexValue.longProp()).toArray(),
                is(arrayContainingInAnyOrder(97L, 98L, 99L))
        );
    }

    @Test
    public void givenFilterOnFloatProperty__whenGraterThanOrEquals__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("floatProp >= 97.2")
                .build(), 0, 1000);

        assertThat(actual.total(), is(3L));
        assertThat(
                actual.valueList().stream().map(complexValue -> complexValue.floatProp()).toArray(),
                is(arrayContainingInAnyOrder(97.2f, 98.2f, 99.2f))
        );
    }

    @Test
    public void givenFilterOnDoubleProperty__whenGraterThanOrEquals__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("doubleProp >= 97.2")
                .build(), 0, 1000);

        assertThat(actual.total(), is(3L));
        assertThat(
                actual.valueList().stream().map(complexValue -> complexValue.doubleProp()).toArray(),
                is(arrayContainingInAnyOrder(97.2d, 98.2d, 99.2d))
        );
    }

    @Test
    public void givenFilterOnStringProperty__whenGraterThanOrEqualProperty__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("stringProp >= nested.nestedProp")
                .build(), 0, 1000);

        assertThat(actual.total(), is(50L));
        for (Entity<QAValue> entity : actual) {
            assertThat(entity.value().toString(), entity.value().stringProp(), is(greaterThanOrEqualTo(entity.value().nested().nestedProp())));
        }
    }

    @Test
    public void givenFilterOnStringProperty__whenLowerThan__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("stringProp < '002'")
                .build(), 0, 1000);

        assertThat(actual.total(), is(2L));
        assertThat(
                actual.valueList().stream().map(complexValue -> complexValue.stringProp()).toArray(),
                is(arrayContainingInAnyOrder("000", "001"))
        );
    }

    @Test
    public void givenFilterOnDateProperty__whenLowerThan__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("dateProp < 1985-06-14")
                .build(), 0, 1000);

        assertThat(actual.total(), is(2L));
        assertThat(
                actual.valueList().stream().map(complexValue -> DateTimeFormatter.ISO_LOCAL_DATE.format(complexValue.dateProp())).toArray(),
                is(arrayContainingInAnyOrder("1985-06-12", "1985-06-13"))
        );
    }

    @Test
    public void givenFilterOnTimeProperty__whenLowerThan__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("timeProp < 12:44:33.123456789")
                .build(), 0, 1000);

        System.out.println(actual.valueList().stream().map(complexValue -> DateTimeFormatter.ISO_LOCAL_TIME.format(complexValue.timeProp())).toArray());
        assertThat(actual.total(), is(2L));
        assertThat(
                actual.valueList().stream().map(complexValue -> DateTimeFormatter.ISO_LOCAL_TIME.format(complexValue.timeProp())).toArray(),
                is(arrayContainingInAnyOrder(ts("12:42:33.123456789"), ts("12:43:33.123456789")))
        );
    }

    @Test
    public void givenFilterOnDateTimeProperty__whenLowerThan__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("datetimeProp < 1985-06-14T12:42:33.123456789")
                .build(), 0, 1000);

        assertThat(actual.total(), is(2L));
        assertThat(
                actual.valueList().stream().map(complexValue -> DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(complexValue.datetimeProp())).toArray(),
                is(arrayContainingInAnyOrder(ts("1985-06-12T12:42:33.123456789"), ts("1985-06-13T12:42:33.123456789")))
        );
    }

    @Test
    public void givenFilterOnZonedDateTimeProperty__whenLowerThan__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("tzdatetimeProp < 1985-06-14T12:42:33.123456789+05:00")
                .build(), 0, 1000);

        System.out.println(actual.valueList().stream().map(complexValue -> DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(complexValue.tzdatetimeProp())).toArray());
        assertThat(actual.total(), is(2L));
        assertThat(
                actual.valueList().stream().map(complexValue -> DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(complexValue.tzdatetimeProp())).toArray(),
                is(arrayContainingInAnyOrder(ts("1985-06-12T12:42:33.123456789+05:00"), ts("1985-06-13T12:42:33.123456789+05:00")))
        );
    }

    @Test
    public void givenFilterOnIntegerProperty__whenLowerThan__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("integerProp < 2")
                .build(), 0, 1000);

        assertThat(actual.total(), is(2L));
        assertThat(
                actual.valueList().stream().map(complexValue -> complexValue.integerProp()).toArray(),
                is(arrayContainingInAnyOrder(0, 1))
        );
    }

    @Test
    public void givenFilterOnLongProperty__whenLowerThan__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("longProp < 2")
                .build(), 0, 1000);

        assertThat(actual.total(), is(2L));
        assertThat(
                actual.valueList().stream().map(complexValue -> complexValue.longProp()).toArray(),
                is(arrayContainingInAnyOrder(0L, 1L))
        );
    }

    @Test
    public void givenFilterOnFloatProperty__whenLowerThan__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("floatProp < 2.2")
                .build(), 0, 1000);

        assertThat(actual.total(), is(2L));
        assertThat(
                actual.valueList().stream().map(complexValue -> complexValue.floatProp()).toArray(),
                is(arrayContainingInAnyOrder(0.2f, 1.2f))
        );
    }

    @Test
    public void givenFilterOnDoubleProperty__whenLowerThan__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("doubleProp < 2.2")
                .build(), 0, 1000);

        assertThat(actual.total(), is(2L));
        assertThat(
                actual.valueList().stream().map(complexValue -> complexValue.doubleProp()).toArray(),
                is(arrayContainingInAnyOrder(0.2d, 1.2d))
        );
    }

    @Test
    public void givenFilterOnStringProperty__whenLowerThanProperty__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("stringProp < nested.nestedProp")
                .build(), 0, 1000);

        assertThat(actual.total(), is(50L));
        for (Entity<QAValue> entity : actual) {
            assertThat(entity.value().toString(), entity.value().stringProp(), is(lessThan(entity.value().nested().nestedProp())));
        }
    }

    @Test
    public void givenFilterOnStringProperty__whenLowerThanOrEquals__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("stringProp <= '002'")
                .build(), 0, 1000);

        assertThat(actual.total(), is(3L));
        assertThat(
                actual.valueList().stream().map(complexValue -> complexValue.stringProp()).toArray(),
                is(arrayContainingInAnyOrder("000", "001", "002"))
        );
    }

    @Test
    public void givenFilterOnDateProperty__whenLowerThanOrEquals__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("dateProp <= 1985-06-14")
                .build(), 0, 1000);

        assertThat(actual.total(), is(3L));
        assertThat(
                actual.valueList().stream().map(complexValue -> DateTimeFormatter.ISO_LOCAL_DATE.format(complexValue.dateProp())).toArray(),
                is(arrayContainingInAnyOrder("1985-06-12", "1985-06-13", "1985-06-14"))
        );
    }

    @Test
    public void givenFilterOnTimeProperty__whenLowerThanOrEquals__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("timeProp <= 12:44:33.123456789")
                .build(), 0, 1000);

        System.out.println(actual.valueList().stream().map(complexValue -> DateTimeFormatter.ISO_LOCAL_TIME.format(complexValue.timeProp())).toArray());
        assertThat(actual.total(), is(3L));
        assertThat(
                actual.valueList().stream().map(complexValue -> DateTimeFormatter.ISO_LOCAL_TIME.format(complexValue.timeProp())).toArray(),
                is(arrayContainingInAnyOrder(ts("12:42:33.123456789"), ts("12:43:33.123456789"), ts("12:44:33.123456789")))
        );
    }

    @Test
    public void givenFilterOnDateTimeProperty__whenLowerThanOrEquals__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("datetimeProp <= 1985-06-14T12:42:33.123456789")
                .build(), 0, 1000);

        assertThat(actual.total(), is(3L));
        assertThat(
                actual.valueList().stream().map(complexValue -> DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(complexValue.datetimeProp())).toArray(),
                is(arrayContainingInAnyOrder(ts("1985-06-12T12:42:33.123456789"), ts("1985-06-13T12:42:33.123456789"), ts("1985-06-14T12:42:33.123456789")))
        );
    }

    @Test
    public void givenFilterOnZonedDateTimeProperty__whenLowerThanOrEquals__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("tzdatetimeProp <= 1985-06-14T12:42:33.123456789+05:00")
                .build(), 0, 1000);

        System.out.println(actual.valueList().stream().map(complexValue -> DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(complexValue.tzdatetimeProp())).toArray());
        assertThat(actual.total(), is(3L));
        assertThat(
                actual.valueList().stream().map(complexValue -> DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(complexValue.tzdatetimeProp())).toArray(),
                is(arrayContainingInAnyOrder(ts("1985-06-12T12:42:33.123456789+05:00"), ts("1985-06-13T12:42:33.123456789+05:00"), ts("1985-06-14T12:42:33.123456789+05:00")))
        );
    }

    @Test
    public void givenFilterOnIntegerProperty__whenLowerThanOrEquals__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("integerProp <= 2")
                .build(), 0, 1000);

        assertThat(actual.total(), is(3L));
        assertThat(
                actual.valueList().stream().map(complexValue -> complexValue.integerProp()).toArray(),
                is(arrayContainingInAnyOrder(0, 1, 2))
        );
    }

    @Test
    public void givenFilterOnLongProperty__whenLowerThanOrEquals__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("longProp <= 2")
                .build(), 0, 1000);

        assertThat(actual.total(), is(3L));
        assertThat(
                actual.valueList().stream().map(complexValue -> complexValue.longProp()).toArray(),
                is(arrayContainingInAnyOrder(0L, 1L, 2L))
        );
    }

    @Test
    public void givenFilterOnFloatProperty__whenLowerThanOrEquals__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("floatProp <= 2.2")
                .build(), 0, 1000);

        assertThat(actual.total(), is(3L));
        assertThat(
                actual.valueList().stream().map(complexValue -> complexValue.floatProp()).toArray(),
                is(arrayContainingInAnyOrder(0.2f, 1.2f, 2.2f))
        );
    }

    @Test
    public void givenFilterOnDoubleProperty__whenLowerThanOrEquals__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("doubleProp <= 2.2")
                .build(), 0, 1000);

        assertThat(actual.total(), is(3L));
        assertThat(
                actual.valueList().stream().map(complexValue -> complexValue.doubleProp()).toArray(),
                is(arrayContainingInAnyOrder(0.2d, 1.2d, 2.2d))
        );
    }

    @Test
    public void givenFilterOnStringProperty__whenLowerThanOrEqualProperty__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("stringProp <= nested.nestedProp")
                .build(), 0, 1000);

        assertThat(actual.total(), is(51L));
        for (Entity<QAValue> entity : actual) {
            assertThat(entity.value().toString(), entity.value().stringProp(), is(lessThanOrEqualTo(entity.value().nested().nestedProp())));
        }
    }

    @Test
    public void givenFilterOnStringProperty__whenStartsWithProperty__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("stringProp starts with nested.deep.deepProp")
                .build(), 0, 1000);

        assertThat(actual.total(), is(10L));
        assertThat(
                actual.valueList().stream().map(complexValue -> complexValue.stringProp()).toArray(),
                is(arrayContainingInAnyOrder("040", "041", "042", "043", "044", "045", "046", "047", "048", "049"))
        );
    }

    @Test
    public void givenFilterOnStringProperty__whenStartsWith__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("stringProp starts with '01'")
                .build(), 0, 1000);

        assertThat(actual.total(), is(10L));
        assertThat(
                actual.valueList().stream().map(complexValue -> complexValue.stringProp()).toArray(),
                is(arrayContainingInAnyOrder("010", "011", "012", "013", "014", "015", "016", "017", "018", "019"))
        );
    }

    @Test
    public void givenFilterOnStringProperty__whenEndsWith__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("stringProp ends with '8'")
                .build(), 0, 1000);

        assertThat(actual.total(), is(10L));
        assertThat(
                actual.valueList().stream().map(complexValue -> complexValue.stringProp()).toArray(),
                is(arrayContainingInAnyOrder("008", "018", "028", "038", "048", "058", "068", "078", "088", "098"))
        );
    }

    @Test
    public void givenFilterOnStringProperty__whenEndsWithProperty__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("stringProp ends with nested.deep.deepProp")
                .build(), 0, 1000);

        assertThat(actual.total(), is(1L));
        assertThat(
                actual.valueList().stream().map(complexValue -> complexValue.stringProp()).toArray(),
                is(arrayContainingInAnyOrder("004"))
        );
    }

    @Test
    public void givenFilterOnStringProperty__whenContains__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("stringProp contains '08'")
                .build(), 0, 1000);

        assertThat(actual.total(), is(11L));
        assertThat(
                actual.valueList().stream().map(complexValue -> complexValue.stringProp()).toArray(),
                is(arrayContainingInAnyOrder("008", "080", "081", "082", "083", "084", "085", "086", "087", "088", "089"))
        );
    }

    @Test
    public void givenFilterOnStringProperty__whenContainsProperty__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("stringProp contains nested.deep.deepProp")
                .build(), 0, 1000);

        assertThat(actual.total(), is(11L));
        assertThat(
                actual.valueList().stream().map(complexValue -> complexValue.stringProp()).toArray(),
                is(arrayContainingInAnyOrder("004", "040", "041", "042", "043", "044", "045", "046", "047", "048", "049"))
        );
    }

    @Test
    public void givenFilterOnStringProperty__whenOr__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("stringProp == '001' || stringProp == '002'")
                .build(), 0, 1000);

        assertThat(actual.total(), is(2L));
        assertThat(
                actual.valueList().stream().map(complexValue -> complexValue.stringProp()).toArray(),
                is(arrayContainingInAnyOrder("001", "002"))
        );
    }

    @Test
    public void givenFilterOnStringProperty__whenAnd__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("stringProp == '050' && nested.nestedProp == '050'")
                .build(), 0, 1000);

        assertThat(actual.total(), is(1L));
        assertThat(
                actual.valueList().stream().map(complexValue -> complexValue.stringProp()).toArray(),
                is(arrayContainingInAnyOrder("050"))
        );
    }

    @Test
    public void givenFilterOnStringProperty__whenNot__thenSelectedValueReturned() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .filter("! stringProp <= '097'")
                .build(), 0, 1000);

        assertThat(actual.total(), is(2L));
        assertThat(
                actual.valueList().stream().map(complexValue -> complexValue.stringProp()).toArray(),
                is(arrayContainingInAnyOrder("098", "099"))
        );
    }

    @Test
    public void whenNoFilter_andOrderByOnePropertyDefaultDirection__thenAllValuesReturnedIsAscending() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .sort("stringProp")
                .build(), 0, 1000);

        assertThat(actual.valueList(), hasSize(100));
        assertThat(actual.valueList().get(0).stringProp(), is("000"));
        assertThat(actual.valueList().get(1).stringProp(), is("001"));
    }

    @Test
    public void whenNoFilter_andOrderByOneNestedPropertyDefaultDirection__thenAllValuesReturnedIsAscending() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .sort("nested.nestedProp")
                .build(), 0, 1000);

        assertThat(actual.valueList(), hasSize(100));
        assertThat(actual.valueList().get(0).nested().nestedProp(), is("001"));
        assertThat(actual.valueList().get(1).nested().nestedProp(), is("002"));
    }

    @Test
    public void whenNoFilter_andOrderByOnePropertyAsc__thenAllValuesReturnedIsAscending() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .sort("stringProp asc")
                .build(), 0, 1000);

        assertThat(actual.valueList(), hasSize(100));
        assertThat(actual.valueList().get(0).stringProp(), is("000"));
        assertThat(actual.valueList().get(1).stringProp(), is("001"));
    }

    @Test
    public void whenNoFilter_andOrderByOnePropertyDesc__thenAllValuesReturnedIsAscending() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .sort("stringProp desc")
                .build(), 0, 1000);

        assertThat(actual.valueList(), hasSize(100));
        assertThat(actual.valueList().get(0).stringProp(), is("099"));
        assertThat(actual.valueList().get(1).stringProp(), is("098"));
    }

    @Test
    public void whenNoFilter_andOrderByTwoProperties__thenAllValuesAreOrdered() throws Exception {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder()
                .sort("boolProp, stringProp")
                .build(), 0, 1000);

        assertThat(actual.valueList(), hasSize(100));
        assertThat(actual.valueList().get(0).stringProp(), is("001"));
        assertThat(actual.valueList().get(1).stringProp(), is("004"));
    }

    @Test(expected = RepositoryException.class)
    public void givenFilter__whenFilterNotParsable__thenThrowsRepositoryException() throws Exception {
        this.repository.search(PropertyQuery.builder().filter("gruut gruut").build(), 0, 1000);
    }

    @Test(expected = RepositoryException.class)
    public void givenSort__whenSortNotParsable__thenThrowsRepositoryException() throws Exception {
        this.repository.search(PropertyQuery.builder().sort("== = gruut gruut").build(), 0, 1000);
    }

    @Test
    public void givenFilterWithParenthesis_thenReturnOr() throws RepositoryException {
        PagedEntityList<QAValue> actual = this.repository.search(PropertyQuery.builder().filter(
                "(integerProp >= 28 && integerProp <= 30) || (integerProp >= 42 && integerProp <= 44)").build(),
                0, 1000
        );

        assertThat(actual.valueList(), hasSize(6));

        assertThat(
                actual.valueList().stream().map(v -> v.stringProp()).collect(Collectors.toList()),
                containsInAnyOrder("028", "029", "030", "042", "043", "044")
        );
    }
}
