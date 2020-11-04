package org.codingmatters.poom.services.support.hash;


import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class HashMaterialTest {

    @Test
    public void givenMaterialCreatedByKeyValuePairs__whenGettingBytes__thenEntriesAreSortedByKeys() throws Exception {
        assertThat(HashMaterial.create().with("b", "2").with("a", "1").asBytes(), is("a1b2".getBytes("UTF-8")));
    }

    @Test
    public void givenMaterialCreatedByMap__whenGettingBytes__thenEntriesAreSortedByKeys() throws Exception {
        Map map = new TreeMap();
        map.put("b", "2");
        map.put("a", "1");
        assertThat(HashMaterial.create().with(map).asBytes(), is("a1b2".getBytes("UTF-8")));
    }
}