package org.codingmatters.poom.services.support.hash;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class HashProcessorTest {

    @Test
    public void hashIsNotNull() throws Exception {
        assertThat(
                new HashProcessor().hash(HashMaterial.create().with("a", "b")),
                is(notNullValue())
        );
    }

    @Test
    public void hashIsStable() throws Exception {
        assertThat(
                new HashProcessor().hash(HashMaterial.create().with("a", "b")),
                is(new HashProcessor().hash(HashMaterial.create().with("a", "b")))
        );
    }

    @Test
    public void hashIsNotSensibleToOrder() throws Exception {
        assertThat(
                new HashProcessor().hash(HashMaterial.create().with("a", "1").with("b", "2")),
                is(new HashProcessor().hash(HashMaterial.create().with("b", "2").with("a", "1")))
        );
    }

    @Test
    public void hashWithMap() throws Exception {
        Map map = new HashMap();
        map.put("a", "b");
        assertThat(
                new HashProcessor().hash(map),
                is(new HashProcessor().hash(HashMaterial.create().with("a", "b")))
        );
    }
}