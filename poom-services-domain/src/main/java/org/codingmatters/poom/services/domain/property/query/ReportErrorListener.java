package org.codingmatters.poom.services.domain.property.query;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;

import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;

public class ReportErrorListener extends BaseErrorListener {

    private final List<String> report = new LinkedList<>();

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
        this.report.add("line " + line + ":" + charPositionInLine + " " + msg);
    }

    public List<String> report() {
        return report;
    }
}
