package org.codingmatters.poom.json.rpc;

import java.util.function.Function;

public interface RpcMethodHandler<Params, Result> extends Function<Params, Result> {
}
