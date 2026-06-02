package org.codingmatters.poom.mcp.demo.tools;

import org.codingmatters.poom.mcp.demo.domain.NoteRepository;
import org.codingmatters.poom.mcp.demo.domain.NoteService;
import org.codingmatters.poom.mcp.demo.domain.types.Note;
import org.codingmatters.poom.mcp.types.CallToolResult;
import org.codingmatters.poom.services.domain.entities.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class UpdateNoteToolTest {

    private NoteService service;
    private UpdateNoteTool tool;
    private GetNoteTool getTool;

    @BeforeEach
    void setUp() {
        service = new NoteService(NoteRepository.create());
        tool = new UpdateNoteTool(service);
        getTool = new GetNoteTool(service);
    }

    @Test
    void updateTitle_titleChangedContentUnchanged() throws Exception {
        Entity<Note> created = service.create("Original Title", "Original content", null);
        CallToolResult result = tool.apply(ToolTestHelper.params("id", created.id(), "title", "New Title"));
        assertThat(result.isError(), is(false));
        // Verify via get
        CallToolResult getResult = getTool.apply(ToolTestHelper.params("id", created.id()));
        String text = getResult.content().get(0).text();
        assertThat(text, containsString("New Title"));
        assertThat(text, containsString("Original content"));
    }

    @Test
    void unknownId_returnsError() {
        CallToolResult result = tool.apply(ToolTestHelper.params("id", "no-such-id", "title", "New Title"));
        assertThat(result.isError(), is(true));
    }

    @Test
    void missingIdArg_returnsError() {
        CallToolResult result = tool.apply(ToolTestHelper.params("title", "New Title"));
        assertThat(result.isError(), is(true));
    }
}
