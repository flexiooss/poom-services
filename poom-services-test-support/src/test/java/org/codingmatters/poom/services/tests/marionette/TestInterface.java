package org.codingmatters.poom.services.tests.marionette;

import java.io.IOException;

public interface TestInterface {
    void proc(String str, Integer integer, Object o);

    String unary(Integer integer);

    Object nary(String str, Long l);

    Object oary();

    String throwing() throws IOException;

    void primitiveTypeArg(boolean b);
    boolean primitiveBooleanTypeResult();
    int primitiveIntTypeResult();
    long primitiveLongTypeResult();
    float primitiveFloatTypeResult();
    double primitiveDoubleTypeResult();
}
