package org.codingmatters.poom.mcp;

import org.codingmatters.poom.mcp.types.CallToolResult;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class McpTaskStateTest {

    @Test
    void givenFailed__whenBuilt__thenCarriesAJsonRpcErrorAndNoResult() {
        McpTaskState state = McpTaskState.failed(-32603, "boom");

        assertThat(state.status(), is(McpTaskState.Status.FAILED));
        assertThat(state.error().code(), is(-32603));
        assertThat(state.error().message(), is("boom"));
        assertThat(state.result(), is(nullValue()));
    }

    @Test
    void givenCompleted__whenBuilt__thenCarriesTheResult() {
        CallToolResult result = CallToolResult.builder().isError(true).build();

        McpTaskState state = McpTaskState.completed(result);

        assertThat(state.status(), is(McpTaskState.Status.COMPLETED));
        assertThat(state.result(), is(result));
    }

    @Test
    void givenATaskTool__whenDescribed__thenTheDescriptorCarriesIt() {
        McpToolTasks tasks = new McpToolTasks() {
            public Start start(org.codingmatters.poom.mcp.types.CallToolParams params) { return new Start.Running("t-1"); }
            public McpTaskState get(String toolTaskId) { return McpTaskState.working(); }
            public void cancel(String toolTaskId) {}
        };

        McpToolDescriptor tool = McpToolDescriptor.builder().name("slow").tasks(tasks).build();

        assertThat(tool.tasks(), is(sameInstance(tasks)));
    }
}
