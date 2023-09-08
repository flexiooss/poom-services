import org.codingmatters.poom.containers.ApiContainerRuntime;
import org.codingmatters.poom.containers.acceptance.ApiContainerRuntimeAcceptanceTest;
import org.codingmatters.poom.containers.runtime.netty.NettyApiContainerRuntime;
import org.codingmatters.poom.services.logging.CategorizedLogger;

public class NettyApiContainerRuntimeTest extends ApiContainerRuntimeAcceptanceTest {
    @Override
    protected ApiContainerRuntime createContainer(String host, int port, CategorizedLogger log) {
        return new NettyApiContainerRuntime(host, port, log);
    }
}
