package org.codingmatters.poom.services.support.hash;


import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class HashMaterialTest {

    @Test
    public void givenMaterialCreatedByKeyValuePairs__whenGettingBytes__thenEntriesAreSortedByKeys() throws Exception {
        assertThat(HashMaterial.create().with("b", "2").with("a", "1").asBytes(), is("{|a:1|b:2}".getBytes("UTF-8")));
    }

    @Test
    public void givenMaterialCreatedByMap__whenGettingBytes__thenEntriesAreSortedByKeys() throws Exception {
        Map map = new TreeMap();
        map.put("b", "2");
        map.put("a", "1");
        assertThat(HashMaterial.create().with(map).asBytes(), is("{|a:1|b:2}".getBytes("UTF-8")));
    }

    @Test
    public void givenMaterialCreatedByMap__whenMapIsDeep__thenBytesAreCalculatedFromRecursive() throws Exception {
        Map map = new HashMap();
        List list = new LinkedList();
        list.add(Long.valueOf(12));
        list.add("e");
        Map submap = new HashMap();
        submap.put("c", "3");
        submap.put("d", "4");
        list.add(submap);
        map.put("a", list);
        map.put("b", "2");

        assertThat(
                HashMaterial.create().with(map).asBytes(),
                is("{|a:[|12|e|{|c:3|d:4}]|b:2}".getBytes("UTF-8"))
        );
    }
}