package org.codingmatters.poom.services.tests.mock.support;

import java.io.IOException;

public interface TestInterface {
    void proc(String str, Integer integer, Object o);

    String unary(Integer integer);

    Object nary(String str, Long l);

    Object oary();

    String throwing() throws IOException;
}
