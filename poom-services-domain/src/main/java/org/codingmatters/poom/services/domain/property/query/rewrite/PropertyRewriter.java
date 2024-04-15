package org.codingmatters.poom.services.domain.property.query.rewrite;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.codingmatters.poom.services.domain.property.query.PropertyQuery;
import org.codingmatters.poom.services.domain.property.query.parsers.PropertyFilterLexer;
import org.codingmatters.poom.services.domain.property.query.parsers.PropertySortLexer;

import java.util.Collections;
import java.util.Map;

public class PropertyRewriter {

    static public PropertyRewriter idTo_id() {
        return new PropertyRewriter(Map.of("id", "_id"));
    }

    private final Map<String, String> rewrittenProperties;

    public PropertyRewriter(Map<String, String> rewrittenProperties) {
        this.rewrittenProperties = rewrittenProperties != null ? rewrittenProperties : Collections.emptyMap();
    }

    public PropertyQuery rewrite(PropertyQuery query) {
        if(query != null && query.filter() != null) {
            query = this.rewritedFilter(query);
        }
        if(query != null && query.sort() != null) {
            query = this.rewritedSort(query);
        }
        return query;
    }

    private PropertyQuery rewritedFilter(PropertyQuery query) {
        CodePointCharStream input = CharStreams.fromString(query.filter());
        CommonTokenStream tokens = new CommonTokenStream(new PropertyFilterLexer(input));
        tokens.fill();
        StringBuilder result = new StringBuilder();
        for (Token token : tokens.getTokens()) {
            if (token.getType() == PropertyFilterLexer.IDENTIFIER) {
                if(this.rewrittenProperties.containsKey(token.getText())) {
                    result.append(this.rewrittenProperties.get(token.getText()));
                } else {
                    result.append(token.getText());
                }
                result.append(" ");
            } else if (token.getType() != PropertyFilterLexer.EOF) {
                result.append(token.getText());
                result.append(" ");
            }
        }

        return query.withFilter(result.toString());
    }

    private PropertyQuery rewritedSort(PropertyQuery query) {
        CodePointCharStream input = CharStreams.fromString(query.sort());
        CommonTokenStream tokens = new CommonTokenStream(new PropertySortLexer(input));
        tokens.fill();
        StringBuilder result = new StringBuilder();
        for (Token token : tokens.getTokens()) {
            System.out.println(token.getText());
            if (token.getType() == PropertySortLexer.IDENTIFIER) {
                if(this.rewrittenProperties.containsKey(token.getText())) {
                    result.append(this.rewrittenProperties.get(token.getText()));
                } else {
                    result.append(token.getText());
                }
                result.append(" ");
            } else if (token.getType() != PropertySortLexer.EOF) {
                result.append(token.getText());
                result.append(" ");
            }
        }

        return query.withSort(result.toString());
    }
}
