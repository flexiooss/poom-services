package org.codingmatters.poom.fast.failing;

import org.codingmatters.poom.fast.failing.exceptions.FailFastException;
import org.codingmatters.poom.fast.failing.exceptions.RecoverableFFException;
import org.codingmatters.poom.fast.failing.exceptions.UnrecoverableFFException;

public class FastFailingInterceptor {

    private final FastFailer failer;

    public FastFailingInterceptor(FastFailer failer) {
        this.failer = failer;
    }

    public void failFast(Throwable t) throws Throwable {
        if(t instanceof UnrecoverableFFException){
            this.failer.failAndStop((UnrecoverableFFException) t);
        } else if(t instanceof RecoverableFFException) {
            this.failer.failAndContinue((RecoverableFFException) t);
        } else {
            throw t;
        }
    }
}
