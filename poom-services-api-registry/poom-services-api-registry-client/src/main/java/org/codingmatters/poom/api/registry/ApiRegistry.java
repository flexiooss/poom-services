package org.codingmatters.poom.api.registry;

import org.codingmatters.poom.api.registry.api.AnApiGetRequest;
import org.codingmatters.poom.api.registry.api.AnApiGetResponse;
import org.codingmatters.poom.api.registry.api.ApisGetRequest;
import org.codingmatters.poom.api.registry.api.ApisGetResponse;
import org.codingmatters.poom.api.registry.api.types.ApiSpec;
import org.codingmatters.poom.api.registry.api.types.optional.OptionalApiSpec;
import org.codingmatters.poom.api.registry.client.ApiRegistryClient;
import org.codingmatters.poom.api.registry.exception.ApiRegistryException;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.Env;
import org.codingmatters.poom.services.support.paging.client.Rfc7233Helper;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ApiRegistry implements AutoCloseable {
    static private final CategorizedLogger log = CategorizedLogger.getLogger(ApiRegistry.class);

    static public final long DEFAULT_REFRESH_TICK = Env.optional("API_REGISTRY_DEFAULT_REFRESH_TICK").orElse(new Env.Var(String.valueOf(2 * 60 * 1000))).asLong();

    private final ApiRegistryClient registryClient;
    private final ScheduledFuture<?> scheduledRefresher;

    private final Map<String, ApiSpec> cache = new HashMap<>();

    public ApiRegistry(ApiRegistryClient registryClient, ScheduledExecutorService scheduler) {
        this(registryClient, scheduler, DEFAULT_REFRESH_TICK);
    }
    public ApiRegistry(ApiRegistryClient registryClient, ScheduledExecutorService scheduler, long refreshTick) {
        this.registryClient = registryClient;
        if(scheduler != null && refreshTick > 0) {
            this.scheduledRefresher = scheduler.scheduleAtFixedRate(this::updateRegistry, refreshTick, refreshTick, TimeUnit.MILLISECONDS);
        } else {
            this.scheduledRefresher = null;
            log.debug("api registry - not activating refreshment (scheduler={} ; tick={})", scheduler, refreshTick);
        }
    }

    public OptionalApiSpec api(String apiName) throws ApiRegistryException {
        synchronized (this.cache) {
            if(this.cache.containsKey(apiName)) {
                return this.cache.get(apiName).opt();
            }
        }
        AnApiGetResponse response;
        try {
            response = this.registryClient.apis().anApi().get(AnApiGetRequest.builder().api(apiName).build());
        } catch (IOException e) {
            throw new ApiRegistryException("failed accessing api registry api for api " + apiName, e);
        }
        if(response.opt().status200().isPresent()) {
            ApiSpec spec = response.status200().payload();
            if(spec != null) {
                synchronized (this.cache) {
                    this.cache.put(apiName, spec);
                    return spec.opt();
                }
            } else {
                log.error("[GRAVE] unexpected response from api registry for api {} : {}", response);
                return OptionalApiSpec.of(null);
            }
        } else if(response.status404().opt().isPresent()) {
            return OptionalApiSpec.of(null);
        } else {
            throw new ApiRegistryException("failed getting api " + apiName + ", see logs with token " +
                    log.tokenized().error("failed getting api {}, response was : {}", apiName, response)
            );
        }
    }

    private void updateRegistry() {
        log.debug("updating registry");
        String filter = null;
        synchronized (this.cache) {
            if(this.cache.isEmpty()) {
                return;
            }
            filter = this.cache.values().stream()
                    .filter(spec -> spec.name() != null)
                    .map(spec -> "'" + spec.name().replaceAll("'", "\'") + "'")
                    .collect(Collectors.joining(",", "name in (", ")"));
        }
        List<ApiSpec> refreshed = new LinkedList<>();
        ApisGetResponse response;
        String range = null;
        do {
            try {
                response = this.registryClient.apis().get(ApisGetRequest.builder().filter(filter).range(range).build());
            } catch (IOException e) {
                log.error("[GRAVE] error refreshing api cache, failed reaching api", e);
                return;
            }
            if (response.opt().status200().isPresent()) {
                response.opt().status200().payload().safe().stream().forEach(apiSpec -> refreshed.add(apiSpec));
            } else if (response.opt().status206().isPresent()) {
                response.opt().status206().payload().safe().stream().forEach(apiSpec -> refreshed.add(apiSpec));
                try {
                    range = new Rfc7233Helper(response.status206().contentRange(), response.status206().acceptRange()).nextRange();
                } catch (Rfc7233Helper.UnparseableRfc7233Query e) {
                    log.error("[GRAVE] error refreshing api cache, unparsable range spec from api : " + response, e);
                    return;
                }
            } else {
                log.error("[GRAVE] error refreshing api cache, unexpected response from api : {}", response);
                return;
            }

        } while(response.opt().status206().isPresent());

        synchronized (this.cache) {
            for (ApiSpec apiSpec : refreshed) {
                this.cache.put(apiSpec.name(), apiSpec);
            }
        }
    }

    @Override
    public void close() throws Exception {
        if(this.scheduledRefresher != null) {
            this.scheduledRefresher.cancel(true);
        }
        log.debug("api registry closed");
    }
}
