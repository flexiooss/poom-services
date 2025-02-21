package org.codingmatters.poom.handler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.hamcrest.Matchers.*;

class CumulatingTestHandlerExtensionTest {

    @RegisterExtension
    static private final CumulatingTestHandlerExtension<String, String> handler = new CumulatingTestHandlerExtension<>() {
        @Override
        protected String defaultResponse(String request) {
            return "got -> " + request;
        }
    };

    @Test
    void plouf() throws Exception {
        assertThat(this.handler.lastRequest(), is(nullValue()));
        System.out.println(this.handler.apply("plouf"));
        assertThat(this.handler.lastRequest(), is("plouf"));
    }
    @Test
    void plif() throws Exception {
        assertThat(this.handler.lastRequest(), is(nullValue()));
        System.out.println(this.handler.apply("plif"));
        assertThat(this.handler.lastRequest(), is("plif"));
    }
    @Test
    void plaf() throws Exception {
        assertThat(this.handler.lastRequest(), is(nullValue()));
        System.out.println(this.handler.apply("plaf"));
        assertThat(this.handler.lastRequest(), is("plaf"));
    }
}