package org.codingmatters.poom.fast.failing;

import org.codingmatters.poom.fast.failing.exceptions.FailFastException;
import org.codingmatters.poom.fast.failing.exceptions.RecoverableFFException;
import org.codingmatters.poom.fast.failing.exceptions.UnrecoverableFFException;

public interface FastFailer {
    void failAndStop(UnrecoverableFFException e);
    void failAndContinue(RecoverableFFException e);
}
