package org.codingmatters.poom.mcp;

import org.codingmatters.poom.mcp.types.CallToolResult;
import org.codingmatters.poom.mcp.types.McpError;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    void givenInconsistentStates__whenBuilt__thenRefused() {
        CallToolResult result = CallToolResult.builder().isError(false).build();
        McpError error = McpError.builder().code(-32603).message("boom").build();

        assertThrows(IllegalArgumentException.class, () -> new McpTaskState(null, null, null));
        assertThrows(IllegalArgumentException.class, () -> new McpTaskState(McpTaskState.Status.FAILED, null, null));
        assertThrows(IllegalArgumentException.class, () -> new McpTaskState(McpTaskState.Status.FAILED, null,
                McpError.builder().message("no code").build()));
        assertThrows(IllegalArgumentException.class, () -> new McpTaskState(McpTaskState.Status.FAILED, result, error));
        assertThrows(IllegalArgumentException.class, () -> new McpTaskState(McpTaskState.Status.COMPLETED, null, null));
        assertThrows(IllegalArgumentException.class, () -> new McpTaskState(McpTaskState.Status.COMPLETED, result, error));
        assertThrows(IllegalArgumentException.class, () -> new McpTaskState(McpTaskState.Status.WORKING, result, null));
        assertThrows(IllegalArgumentException.class, () -> new McpTaskState(McpTaskState.Status.WORKING, null, error));
        assertThrows(IllegalArgumentException.class, () -> McpTaskState.completed(null));
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
