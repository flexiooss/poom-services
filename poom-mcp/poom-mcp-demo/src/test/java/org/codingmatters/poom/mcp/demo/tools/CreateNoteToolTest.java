package org.codingmatters.poom.mcp.demo.tools;

import org.codingmatters.poom.mcp.demo.domain.NoteRepository;
import org.codingmatters.poom.mcp.demo.domain.NoteService;
import org.codingmatters.poom.mcp.types.CallToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class CreateNoteToolTest {

    private NoteService service;
    private CreateNoteTool tool;

    @BeforeEach
    void setUp() {
        service = new NoteService(NoteRepository.create());
        tool = new CreateNoteTool(service);
    }

    @Test
    void titleAndContent_createsNote() {
        CallToolResult result = tool.apply(ToolTestHelper.params("title", "My Title", "content", "My Content"));
        assertThat(result.isError(), is(false));
        assertThat(result.content().get(0).text(), containsString("Note created with id:"));
    }

    @Test
    void missingTitle_returnsError() {
        CallToolResult result = tool.apply(ToolTestHelper.params("content", "Some content"));
        assertThat(result.isError(), is(true));
    }

    @Test
    void withTags_noteIsTagged() throws Exception {
        tool.apply(ToolTestHelper.params("title", "Tagged Note", "content", "Body", "tags", List.of("work", "urgent")));
        assertThat(service.list("work"), hasSize(1));
        assertThat(service.list("urgent"), hasSize(1));
    }
}
