package org.codingmatters.poom.mcp.processor;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class TaskIdsTest {

    @Test
    void givenToolTaskIdWithDots__whenRoundTrip__thenIdenticalAndCreatedAtKept() {
        Instant createdAt = Instant.ofEpochMilli(1_760_000_000_123L);
        String taskId = TaskIds.encode("execute_function", createdAt, "eyJhIjoiYiJ9.x.y");

        assertThat(TaskIds.decode(taskId), is(Optional.of(new TaskIds.Ref("execute_function", createdAt, "eyJhIjoiYiJ9.x.y"))));
    }

    @Test
    void givenGarbage__whenDecode__thenEmpty() {
        assertThat(TaskIds.decode("%%%"), is(Optional.empty()));
        assertThat(TaskIds.decode("nodot"), is(Optional.empty()));
        assertThat(TaskIds.decode("a.b"), is(Optional.empty()));
        assertThat(TaskIds.decode(""), is(Optional.empty()));
        assertThat(TaskIds.decode(null), is(Optional.empty()));
    }

    @Test
    void givenUnreadableMillis__whenDecode__thenEmpty() {
        String name = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString("t".getBytes());

        assertThat(TaskIds.decode(name + ".notanumber.exec-1"), is(Optional.empty()));
    }
}
