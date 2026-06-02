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

class DeleteNoteToolTest {

    private NoteService service;
    private DeleteNoteTool tool;

    @BeforeEach
    void setUp() {
        service = new NoteService(NoteRepository.create());
        tool = new DeleteNoteTool(service);
    }

    @Test
    void existingNote_deletedSuccessfully() throws Exception {
        Entity<Note> created = service.create("Title", "Content", null);
        CallToolResult result = tool.apply(ToolTestHelper.params("id", created.id()));
        assertThat(result.isError(), is(false));
        assertThat(result.content().get(0).text(), containsString(created.id()));
        // Verify note is gone
        assertThat(service.get(created.id()).isPresent(), is(false));
    }

    @Test
    void unknownId_returnsError() {
        CallToolResult result = tool.apply(ToolTestHelper.params("id", "no-such-id"));
        assertThat(result.isError(), is(true));
    }

    @Test
    void missingIdArg_returnsError() {
        CallToolResult result = tool.apply(ToolTestHelper.noArgs());
        assertThat(result.isError(), is(true));
    }
}
