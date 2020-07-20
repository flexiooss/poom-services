package org.codingmatters.poom.generic.resource.domain;

public interface CheckedEntityActionProvider<Request, Action> {
    Action action(Request request) throws Exception;
}
