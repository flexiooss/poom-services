package org.codingmatters.poom.services.domain.repositories;

import org.codingmatters.generated.QAValue;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poom.servives.domain.entities.MutableEntity;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThrows;

public abstract class RepositoryBaseAcceptanceTest {

    protected Repository<QAValue, Void> repository;
    protected abstract Repository<QAValue, Void> createRepository() throws Exception;

    @Before
    public void setUp() throws Exception {
        this.repository = this.createRepository();
    }

    @Test
    public void whenNothingCreated__thenRepositoryIsEmpty() throws Exception {
        assertThat(this.repository.all(0, 0).total(), is(0L));
    }

    @Test
    public void givenRepositoryEmpty__whenRetrieving__thenNullReturned() throws Exception {
        assertThat(this.repository.retrieve("no-such-entity"), is(nullValue()));
    }

    @Test
    public void whenEntityCreated__thenEntityWithValueStored() throws Exception {
        QAValue value = QAValue.builder().stringProp("a value").build();
        this.repository.create(value);

        assertThat(this.repository.all(0, 0).total(), is(1L));
        assertThat(this.repository.all(0, 0).get(0).value(), is(value));
    }

    @Test
    public void whenEntityCreated__thenReturnedEntityHasIdValueAndVersionOne() throws Exception {
        QAValue value = QAValue.builder().stringProp("a value").build();
        Entity<QAValue> entity = this.repository.create(value);

        assertThat(entity.id(), is(notNullValue()));
        assertThat(entity.value(), is(value));
        assertThat(entity.version(), is(BigInteger.ONE));
    }

    @Test
    public void whenEntityCreated__thenEntityRetrievableById() throws Exception {
        QAValue value = QAValue.builder().stringProp("a value").build();
        Entity<QAValue> entity = this.repository.create(value);

        assertThat(this.repository.retrieve(entity.id()).id(), is(entity.id()));
        assertThat(this.repository.retrieve(entity.id()).value(), is(value));
        assertThat(this.repository.retrieve(entity.id()).version(), is(BigInteger.ONE));
    }

    @Test
    public void whenEntityCreatedWithId__thenEntityStoredWithGivenId() throws Exception {
        QAValue value = QAValue.builder().stringProp("a value").build();
        Entity<QAValue> entity = this.repository.createWithId("12", value);

        assertThat(entity.id(), is("12"));
        assertThat(entity.value(), is(value));
        assertThat(this.repository.all(0, 0).get(0).id(), is("12"));
        assertThat(this.repository.all(0, 0).get(0).value(), is(value));
        assertThat(this.repository.all(0, 0).get(0).version(), is(BigInteger.ONE));
        assertThat(this.repository.retrieve("12").id(), is("12"));
        assertThat(this.repository.retrieve("12").value(), is(value));
        assertThat(this.repository.retrieve("12").version(), is(BigInteger.ONE));
    }

    @Test
    public void givenIdStoredWithId__whenCreatingEntityWithSameId__thenRepositoryException() throws Exception {
        String existingId = this.repository.create(QAValue.builder().stringProp("existing entity").build()).id();

        assertThrows("entity already exists : " + existingId, RepositoryException.class,() ->
                this.repository.createWithId(existingId, QAValue.builder().stringProp("a value").build())
        );
    }

    @Test
    public void whenCreatingWithIdAndVersion__thenEntityStoredWithIdAndVersion() throws Exception {
        QAValue value = QAValue.builder().stringProp("a value").build();
        Entity<QAValue> entity = this.repository.createWithIdAndVersion("12", BigInteger.valueOf(42), value);

        assertThat(entity.id(), is("12"));
        assertThat(entity.value(), is(value));
        assertThat(entity.version(), is(BigInteger.valueOf(42)));
        assertThat(this.repository.all(0, 0).get(0).id(), is("12"));
        assertThat(this.repository.all(0, 0).get(0).value(), is(value));
        assertThat(this.repository.all(0, 0).get(0).version(), is(BigInteger.valueOf(42)));
        assertThat(this.repository.retrieve("12").id(), is("12"));
        assertThat(this.repository.retrieve("12").value(), is(value));
        assertThat(this.repository.retrieve("12").version(), is(BigInteger.valueOf(42)));
    }

    @Test
    public void givenIdStoredWithId__whenCreatingEntityWithSameIdAndDifferentVersion__thenRepositoryException() throws Exception {
        this.repository.createWithIdAndVersion("existing", BigInteger.valueOf(12), QAValue.builder().stringProp("existing entity").build());

        assertThrows("entity already exists : existing", RepositoryException.class,() ->
                this.repository.createWithIdAndVersion("existing", BigInteger.valueOf(10), QAValue.builder().stringProp("a value").build())
        );
        assertThrows("entity already exists : existing", RepositoryException.class,() ->
                this.repository.createWithIdAndVersion("existing", BigInteger.valueOf(12), QAValue.builder().stringProp("a value").build())
        );
        assertThrows("entity already exists : existing", RepositoryException.class,() ->
                this.repository.createWithIdAndVersion("existing", BigInteger.valueOf(42), QAValue.builder().stringProp("a value").build())
        );
    }

    @Test
    public void givenEntityStored__whenEntityUpdatedFromCreated__thenIdKept_andVersionIncremented_andValueChanged() throws Exception {
        QAValue value = QAValue.builder().stringProp("a value").build();
        Entity<QAValue> entity = this.repository.create(value);

        QAValue changed = QAValue.builder().stringProp("changed").build();
        Entity<QAValue> updated = this.repository.update(entity, changed);
        Entity<QAValue> current = this.repository.retrieve(entity.id());

        assertThat(updated.id(), is(entity.id()));
        assertThat(updated.version(), is(entity.version().add(BigInteger.ONE)));
        assertThat(updated.value(), is(changed));

        assertThat(current.id(), is(entity.id()));
        assertThat(current.version(), is(entity.version().add(BigInteger.ONE)));
        assertThat(current.value(), is(changed));
    }

    @Test
    public void givenEntityStored__whenEntityUpdatedFromId__thenIdKept_andVersionIncremented_andValueChanged() throws Exception {
        QAValue value = QAValue.builder().stringProp("a value").build();
        Entity<QAValue> entity = this.repository.create(value);

        QAValue changed = QAValue.builder().stringProp("changed").build();
        Entity<QAValue> updated = this.repository.update(new MutableEntity<>(entity.id(), null, null), changed);
        Entity<QAValue> current = this.repository.retrieve(entity.id());

        assertThat(updated.id(), is(entity.id()));
        assertThat(updated.version(), is(entity.version().add(BigInteger.ONE)));
        assertThat(updated.value(), is(changed));

        assertThat(current.id(), is(entity.id()));
        assertThat(current.version(), is(entity.version().add(BigInteger.ONE)));
        assertThat(current.value(), is(changed));
    }

    @Test
    public void givenEntityStored__whenEntityUpdatedFromIdAndVersion__thenIdKept_andVersionIncremented_andValueChanged() throws Exception {
        QAValue value = QAValue.builder().stringProp("a value").build();
        Entity<QAValue> entity = this.repository.create(value);

        QAValue changed = QAValue.builder().stringProp("changed").build();
        Entity<QAValue> updated = this.repository.update(new MutableEntity<>(entity.id(), entity.version(), null), changed);
        Entity<QAValue> current = this.repository.retrieve(entity.id());

        assertThat(updated.id(), is(entity.id()));
        assertThat(updated.version(), is(entity.version().add(BigInteger.ONE)));
        assertThat(updated.value(), is(changed));

        assertThat(current.id(), is(entity.id()));
        assertThat(current.version(), is(entity.version().add(BigInteger.ONE)));
        assertThat(current.value(), is(changed));
    }

    @Test
    public void givenEntityStoredWithVersion_andEntityUpdatedFromId__whenFromLowerVersion__thenIdKept_andVersionIncremented_andValueChanged() throws Exception {
        QAValue value = QAValue.builder().stringProp("a value").build();
        Entity<QAValue> entity = this.repository.createWithIdAndVersion("42", BigInteger.valueOf(18), value);

        QAValue changed = QAValue.builder().stringProp("changed").build();
        Entity<QAValue> updated = this.repository.update(new MutableEntity<>(entity.id(), BigInteger.valueOf(12), null), changed);
        Entity<QAValue> current = this.repository.retrieve(entity.id());

        assertThat(updated.id(), is(entity.id()));
        assertThat(updated.version(), is(entity.version().add(BigInteger.ONE)));
        assertThat(updated.value(), is(changed));

        assertThat(current.id(), is(entity.id()));
        assertThat(current.version(), is(entity.version().add(BigInteger.ONE)));
        assertThat(current.value(), is(changed));
    }

    @Test
    public void givenEntityStoredWithVersion_andEntityUpdatedFromId__whenFromGratherVersion__thenIdKept_andVersionIncremented_andValueChanged() throws Exception {
        QAValue value = QAValue.builder().stringProp("a value").build();
        Entity<QAValue> entity = this.repository.createWithIdAndVersion("42", BigInteger.valueOf(18), value);

        QAValue changed = QAValue.builder().stringProp("changed").build();
        Entity<QAValue> updated = this.repository.update(new MutableEntity<>(entity.id(), BigInteger.valueOf(42), null), changed);
        Entity<QAValue> current = this.repository.retrieve(entity.id());

        assertThat(updated.id(), is(entity.id()));
        assertThat(updated.version(), is(entity.version().add(BigInteger.ONE)));
        assertThat(updated.value(), is(changed));

        assertThat(current.id(), is(entity.id()));
        assertThat(current.version(), is(entity.version().add(BigInteger.ONE)));
        assertThat(current.value(), is(changed));
    }

    @Test
    public void givenRepositoryEmpty__whenDeleting__thenRepositoryException() throws Exception {
        assertThrows("cannot delete entity, no such entity in store : 12", RepositoryException.class, () ->
                this.repository.delete(new MutableEntity<>("12", null, null))
        );
    }

    @Test
    public void givenEntityCreated__whenDeletingFromWrongId__thenRepositoryException_andEntityLeftUntouched() throws Exception {
        this.repository.create(QAValue.builder().stringProp("created").build());

        assertThrows("cannot delete entity, no such entity in store : 12", RepositoryException.class, () ->
                this.repository.delete(new MutableEntity<>("12", null, null))
        );
        assertThat(this.repository.all(0, 0).total(), is(1L));
    }

    @Test
    public void givenEntityCreated__whenDeletingFromId__thenEntityRemovedFromRepository() throws Exception {
        Entity<QAValue> entity = this.repository.create(QAValue.builder().stringProp("created").build());

        this.repository.delete(new MutableEntity<>(entity.id(), null, null));

        assertThat(this.repository.all(0, 0).total(), is(0L));
    }
}
