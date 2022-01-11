package org.codingmatters.poom.services.domain.repositories;

import org.codingmatters.generated.QAValue;
import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.servives.domain.entities.ImmutableEntity;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public abstract class RepositoryOptimisticLockingAcceptanceTest extends RepositoryBaseAcceptanceTest {

    /**
     * Fails when optimistics locking activated
     * @throws Exception
     */
    @Override
    public void givenEntityStored__whenEntityUpdatedFromId__thenIdKept_andVersionIncremented_andValueChanged() throws Exception {}

    /**
     * Fails when optimistics locking activated
     * @throws Exception
     */
    @Override
    public void givenEntityStoredWithVersion_andEntityUpdatedFromId__whenFromGratherVersion__thenIdKept_andVersionIncremented_andValueChanged() throws Exception {}

    /**
     * Fails when optimistics locking activated
     * @throws Exception
     */
    @Override
    public void givenEntityStoredWithVersion_andEntityUpdatedFromId__whenFromLowerVersion__thenIdKept_andVersionIncremented_andValueChanged() throws Exception {}

    @Test
    public void whenUpdatingFromSameVersion__thenUpdated() throws Exception {
        this.repository.createWithIdAndVersion("1", BigInteger.valueOf(18), QAValue.builder().stringProp("value").build());

        this.repository.update(new ImmutableEntity<>("1", BigInteger.valueOf(18), null), QAValue.builder().stringProp("changed").build());

        assertThat(this.repository.retrieve("1").version(), is(BigInteger.valueOf(19)));
        assertThat(this.repository.retrieve("1").value().stringProp(), is("changed"));
    }

    @Test
    public void whenUpdatingWithoutVersion__thenRepositoryException_andEntityLeftUnchanged() throws Exception {
        this.repository.createWithIdAndVersion("1", BigInteger.valueOf(18), QAValue.builder().stringProp("value").build());

        Assert.assertThrows("cannot update entity : since optimistic locking activated, must provide a version", RepositoryException.class, () -> {
            this.repository.update(new ImmutableEntity<>("1", null, null), QAValue.builder().stringProp("changed").build());
        });

        assertThat(this.repository.retrieve("1").version(), is(BigInteger.valueOf(18)));
        assertThat(this.repository.retrieve("1").value().stringProp(), is("value"));
    }

    @Test
    public void whenUpdatingFromGraterVersion__thenRepositoryException_andEntityLeftUnchanged() throws Exception {
        this.repository.createWithIdAndVersion("1", BigInteger.valueOf(18), QAValue.builder().stringProp("value").build());

        Assert.assertThrows("cannot update entity : optimistic locking error, version 42 does not match", RepositoryException.class, () -> {
            this.repository.update(new ImmutableEntity<>("1", BigInteger.valueOf(42), null), QAValue.builder().stringProp("changed").build());
        });

        assertThat(this.repository.retrieve("1").version(), is(BigInteger.valueOf(18)));
        assertThat(this.repository.retrieve("1").value().stringProp(), is("value"));
    }

    @Test
    public void whenUpdatingFromLowerVersion__thenRepositoryException_andEntityLeftUnchanged() throws Exception {
        this.repository.createWithIdAndVersion("1", BigInteger.valueOf(18), QAValue.builder().stringProp("value").build());

        Assert.assertThrows("cannot update entity : optimistic locking error, version 12 does not match", RepositoryException.class, () -> {
            this.repository.update(new ImmutableEntity<>("1", BigInteger.valueOf(12), null), QAValue.builder().stringProp("changed").build());
        });

        assertThat(this.repository.retrieve("1").version(), is(BigInteger.valueOf(18)));
        assertThat(this.repository.retrieve("1").value().stringProp(), is("value"));
    }
}
