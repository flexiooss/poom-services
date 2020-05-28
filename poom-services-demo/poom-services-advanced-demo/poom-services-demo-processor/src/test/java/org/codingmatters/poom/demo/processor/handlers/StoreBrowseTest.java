package org.codingmatters.poom.demo.processor.handlers;

import org.codingmatters.poom.apis.demo.api.StoresGetRequest;
import org.codingmatters.poom.apis.demo.api.StoresGetResponse;
import org.codingmatters.poom.demo.domain.spec.Store;
import org.codingmatters.poom.demo.processor.tests.StoreManagerResource;
import org.codingmatters.value.objects.values.ObjectValue;
import org.codingmatters.value.objects.values.PropertyValue;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.is;

public class StoreBrowseTest {

    @Rule
    public StoreManagerResource storeManagerResource = new StoreManagerResource();

    private StoreBrowse handler;

    @Before
    public void setUp() throws Exception {
        this.handler = new StoreBrowse(this.storeManagerResource.storeManager());
    }

    @Test
    public void givenNoStores__whenListing__then200_andEmptyList() throws Exception {
        StoresGetResponse response = this.handler.apply(StoresGetRequest.builder().build());

        response.opt().status200().orElseThrow(() -> new AssertionError("expected 200, got " + response));

        assertThat(response.status200().payload().toArray(), is(arrayWithSize(0)));
    }

    @Test
    public void givenSomeRepos__whenLessReposThanPageSize__then200_andAllReposInList() throws Exception {
        for (int i = 0; i < 10; i++) {
            this.storeManagerResource.storeRepository().create(Store.builder().name("store-" + i).build());
        }
        StoresGetResponse response = this.handler.apply(StoresGetRequest.builder().build());

        response.opt().status200().orElseThrow(() -> new AssertionError("expected 200, got " + response));

        assertThat(response.status200().payload().toArray(), is(arrayWithSize(10)));

        for (int i = 0; i < response.status200().payload().size(); i++) {
            assertThat("store " + i,
                    response.status200().payload().get(i),
                    is(ObjectValue.builder().property("store name", PropertyValue.builder().stringValue("store-" + i).build()).build())
            );
        }
    }

    @Test
    public void givenSomeRepos__whenMoreReposThanPageSize__then206_andFirstReposInList() throws Exception {
        for (int i = 0; i < 1000; i++) {
            this.storeManagerResource.storeRepository().create(Store.builder().name("store-" + i).build());
        }
        StoresGetResponse response = this.handler.apply(StoresGetRequest.builder().build());

        response.opt().status206().orElseThrow(() -> new AssertionError("expected 206, got " + response));

        assertThat(response.status206().payload().toArray(), is(arrayWithSize(10)));

        for (int i = 0; i < response.status206().payload().size(); i++) {
            assertThat("store " + i,
                    response.status206().payload().get(i),
                    is(ObjectValue.builder().property("store name", PropertyValue.builder().stringValue("store-" + i).build()).build())
            );
        }
    }
}