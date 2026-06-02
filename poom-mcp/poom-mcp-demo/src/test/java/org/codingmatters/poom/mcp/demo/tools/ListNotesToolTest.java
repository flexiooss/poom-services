package org.codingmatters.poom.mcp.demo.tools;

import org.codingmatters.poom.mcp.demo.domain.NoteRepository;
import org.codingmatters.poom.mcp.demo.domain.NoteService;
import org.codingmatters.poom.mcp.types.CallToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class ListNotesToolTest {

    private NoteService service;
    private ListNotesTool tool;

    @BeforeEach
    void setUp() {
        service = new NoteService(NoteRepository.create());
        tool = new ListNotesTool(service);
    }

    @Test
    void noFilter_listsAllNotes() throws Exception {
        service.create("Note A", "content a", null);
        service.create("Note B", "content b", null);
        CallToolResult result = tool.apply(ToolTestHelper.noArgs());
        assertThat(result.isError(), is(false));
        String text = result.content().get(0).text();
        assertThat(text, containsString("Note A"));
        assertThat(text, containsString("Note B"));
    }

    @Test
    void tagFilter_returnsOnlyMatching() throws Exception {
        service.create("Work Note", "work content", List.of("work"));
        service.create("Personal Note", "personal content", List.of("personal"));
        CallToolResult result = tool.apply(ToolTestHelper.params("tag", "work"));
        assertThat(result.isError(), is(false));
        String text = result.content().get(0).text();
        assertThat(text, containsString("Work Note"));
        assertThat(text, not(containsString("Personal Note")));
    }

    @Test
    void empty_returnsNoNotesFound() {
        CallToolResult result = tool.apply(ToolTestHelper.noArgs());
        assertThat(result.isError(), is(false));
        assertThat(result.content().get(0).text(), is("No notes found."));
    }

    @Test
    void noteWithTags_tagsShownInOutput() throws Exception {
        service.create("Tagged", "body", List.of("alpha", "beta"));
        CallToolResult result = tool.apply(ToolTestHelper.noArgs());
        String text = result.content().get(0).text();
        assertThat(text, containsString("alpha"));
        assertThat(text, containsString("beta"));
    }
}
