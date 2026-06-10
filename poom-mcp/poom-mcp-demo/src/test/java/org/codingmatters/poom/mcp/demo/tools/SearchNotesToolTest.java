package org.codingmatters.poom.mcp.demo.tools;

import org.codingmatters.poom.mcp.demo.domain.NoteRepository;
import org.codingmatters.poom.mcp.demo.domain.NoteService;
import org.codingmatters.poom.mcp.types.CallToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class SearchNotesToolTest {

    private NoteService service;
    private SearchNotesTool tool;

    @BeforeEach
    void setUp() {
        service = new NoteService(NoteRepository.create());
        tool = new SearchNotesTool(service);
    }

    @Test
    void matchInTitle_found() throws Exception {
        service.create("Meeting agenda", "content here", null);
        service.create("Shopping list", "milk eggs", null);
        CallToolResult result = tool.apply(ToolTestHelper.params("query", "meeting"));
        assertThat(result.isError(), is(false));
        String text = result.content().get(0).text();
        assertThat(text, containsString("content here"));
    }

    @Test
    void matchInContent_found() throws Exception {
        service.create("Random title", "contains the keyword inside", null);
        CallToolResult result = tool.apply(ToolTestHelper.params("query", "keyword"));
        assertThat(result.isError(), is(false));
        String text = result.content().get(0).text();
        assertThat(text, containsString("keyword"));
    }

    @Test
    void noMatch_returnsNoNotesMatch() throws Exception {
        service.create("Some title", "some content", null);
        CallToolResult result = tool.apply(ToolTestHelper.params("query", "xyzzy-unique"));
        assertThat(result.isError(), is(false));
        assertThat(result.content().get(0).text(), containsString("No notes match"));
        assertThat(result.content().get(0).text(), containsString("xyzzy-unique"));
    }

    @Test
    void missingQuery_returnsError() {
        CallToolResult result = tool.apply(ToolTestHelper.noArgs());
        assertThat(result.isError(), is(true));
    }
}
