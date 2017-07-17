package org.codingmatters.poom.services.support.paging;

import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.services.test.utils.StringInMemoryRepository;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Created by nelt on 7/13/17.
 */
public class Rfc7233PagerTest {

    private Repository<String, String> repository = new StringInMemoryRepository();


    @Test
    public void whenNoRange__ifRepositoryIsEmpty__thenReturnEmptyPage_andNullRequestedRange() throws Exception {
        Rfc7233Pager.Page<String> page = Rfc7233Pager.forRequestedRange(null)
                .unit("String")
                .maxPageSize(10)
                .pager(this.repository)
                .page();

        assertThat(page.acceptRange(), is("String 10"));
        assertThat(page.contentRange(), is("String 0-0/0"));
        assertThat(page.requestedRange(), is(nullValue()));

        assertThat(page.isPartial(), is(false));
        assertThat(page.list(), is(empty()));

        assertThat(page.isValid(), is(true));
        assertThat(page.validationMessage(), is(nullValue()));
    }

    @Test
    public void whenNoRange__ifLessElementsThanMaxPageSize__thenReturnCompleteList() throws Exception {
        for(int i = 0 ; i < 5 ; i++) {
            this.repository.create("test-" + i);
        }

        Rfc7233Pager.Page<String> page = Rfc7233Pager.forRequestedRange(null)
                .unit("String")
                .maxPageSize(10)
                .pager(this.repository)
                .page();

        assertThat(page.acceptRange(), is("String 10"));
        assertThat(page.contentRange(), is("String 0-4/5"));
        assertThat(page.requestedRange(), is(nullValue()));

        assertThat(page.isPartial(), is(false));
        assertThat(page.list(), hasSize(5));
        for(int i = 0 ; i < 5 ; i++) {
            assertThat(page.list().get(i).value(), is("test-" + i));
        }

        assertThat(page.isValid(), is(true));
        assertThat(page.validationMessage(), is(nullValue()));
    }

    @Test
    public void whenNoRange__ifMoreElementsThanMaxPageSize__thenReturnCompleteList() throws Exception {
        for(int i = 0 ; i < 15 ; i++) {
            this.repository.create("test-" + i);
        }

        Rfc7233Pager.Page<String> page = Rfc7233Pager.forRequestedRange(null)
                .unit("String")
                .maxPageSize(10)
                .pager(this.repository)
                .page();

        assertThat(page.acceptRange(), is("String 10"));
        assertThat(page.contentRange(), is("String 0-9/15"));
        assertThat(page.requestedRange(), is(nullValue()));

        assertThat(page.isPartial(), is(true));
        assertThat(page.list(), hasSize(10));
        for(int i = 0 ; i < 10 ; i++) {
            assertThat(page.list().get(i).value(), is("test-" + i));
        }

        assertThat(page.isValid(), is(true));
        assertThat(page.validationMessage(), is(nullValue()));
    }

    @Test
    public void whenRange__ifRepositoryIsEmpty__thenReturnEmptyPage() throws Exception {
        Rfc7233Pager.Page<String> page = Rfc7233Pager.forRequestedRange("0-9")
                .unit("String")
                .maxPageSize(10)
                .pager(this.repository)
                .page();

        assertThat(page.acceptRange(), is("String 10"));
        assertThat(page.contentRange(), is("String 0-0/0"));
        assertThat(page.requestedRange(), is("0-9"));

        assertThat(page.isPartial(), is(false));
        assertThat(page.list(), is(empty()));

        assertThat(page.isValid(), is(true));
        assertThat(page.validationMessage(), is(nullValue()));
    }

    @Test
    public void whenRange_andRangeSizeUnderMaxSize__thenReturnCompleteList() throws Exception {
        for(int i = 0 ; i < 5 ; i++) {
            this.repository.create("test-" + i);
        }

        Rfc7233Pager.Page<String> page = Rfc7233Pager.forRequestedRange("0-5")
                .unit("String")
                .maxPageSize(10)
                .pager(this.repository)
                .page();

        assertThat(page.acceptRange(), is("String 10"));
        assertThat(page.contentRange(), is("String 0-4/5"));
        assertThat(page.requestedRange(), is("0-5"));

        assertThat(page.isPartial(), is(false));
        assertThat(page.list(), hasSize(5));
        for(int i = 0 ; i < 5 ; i++) {
            assertThat(page.list().get(i).value(), is("test-" + i));
        }

        assertThat(page.isValid(), is(true));
        assertThat(page.validationMessage(), is(nullValue()));
    }

    @Test
    public void whenRange_andRangeSizeUnderMaxSize__ifMoreElementsThanRangeSize__thenReturnCompleteList() throws Exception {
        for(int i = 0 ; i < 15 ; i++) {
            this.repository.create("test-" + i);
        }

        Rfc7233Pager.Page<String> page = Rfc7233Pager.forRequestedRange("0-5")
                .unit("String")
                .maxPageSize(10)
                .pager(this.repository)
                .page();

        assertThat(page.acceptRange(), is("String 10"));
        assertThat(page.contentRange(), is("String 0-5/15"));
        assertThat(page.requestedRange(), is("0-5"));

        assertThat(page.isPartial(), is(true));
        assertThat(page.list(), hasSize(6));
        for(int i = 0 ; i < 6 ; i++) {
            assertThat(page.list().get(i).value(), is("test-" + i));
        }

        assertThat(page.isValid(), is(true));
        assertThat(page.validationMessage(), is(nullValue()));
    }

    @Test
    public void whenOffsettedRange_andRangeSizeUnderMaxSize__ifMoreElementsThanRangeSize__thenReturnCompleteList() throws Exception {
        for(int i = 0 ; i < 15 ; i++) {
            this.repository.create("test-" + i);
        }

        Rfc7233Pager.Page<String> page = Rfc7233Pager.forRequestedRange("4-10")
                .unit("String")
                .maxPageSize(10)
                .pager(this.repository)
                .page();

        assertThat(page.acceptRange(), is("String 10"));
        assertThat(page.contentRange(), is("String 4-10/15"));
        assertThat(page.requestedRange(), is("4-10"));

        assertThat(page.isPartial(), is(true));
        assertThat(page.list(), hasSize(7));
        for(int i = 0 ; i < 7 ; i++) {
            assertThat(page.list().get(i).value(), is("test-" + (i + 4)));
        }

        assertThat(page.isValid(), is(true));
        assertThat(page.validationMessage(), is(nullValue()));
    }

    @Test
    public void whenRange_andRangeSizeOverMaxSize__thenRangeIsOverloadedAccordingToMaxPageSize() throws Exception {
        for(int i = 0 ; i < 15 ; i++) {
            this.repository.create("test-" + i);
        }

        Rfc7233Pager.Page<String> page = Rfc7233Pager.forRequestedRange("0-12")
                .unit("String")
                .maxPageSize(10)
                .pager(this.repository)
                .page();

        assertThat(page.acceptRange(), is("String 10"));
        assertThat(page.contentRange(), is("String 0-9/15"));
        assertThat(page.requestedRange(), is("0-12"));

        assertThat(page.isPartial(), is(true));
        assertThat(page.list(), hasSize(10));
        for(int i = 0 ; i < 10 ; i++) {
            assertThat(page.list().get(i).value(), is("test-" + i));
        }

        assertThat(page.isValid(), is(true));
        assertThat(page.validationMessage(), is(nullValue()));
    }

    @Ignore
    @Test
    public void validation() throws Exception {
        fail("NYIMPL");
    }

    @Ignore
    @Test
    public void query() throws Exception {
        fail("NYIMPL");
    }
}