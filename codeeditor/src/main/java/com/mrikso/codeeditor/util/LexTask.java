package com.mrikso.codeeditor.util;

import com.mrikso.codeeditor.lang.Language;

import java.util.ArrayList;
import java.util.List;

public abstract class LexTask implements Runnable {
    /**
     * A collection of Pairs, where Pair.first is the start
     * position of the token, and Pair.second is the type of the token.
     */
    protected final Language language;
    protected boolean abort;
    private Lexer _lexManager;
    /**
     * can be set by another thread to stop the scan immediately
     */
    private boolean isRun;

    public LexTask(Language language) {
        this.language = language;
        isRun = false;
        abort = false;
    }

    public void abort() {
        abort = true;
    }

    public boolean isRun() {
        return isRun;
    }

    void setLexer(Lexer p) {
        _lexManager = p;
    }

    @Override
    public synchronized void run() {
        isRun = true;
        List<Pair> _tokens = new ArrayList<>();
        abort = false;
        tokenize(_tokens, _lexManager.getText());
        if (_tokens.size() > 0)
            _lexManager.tokenizeDone(_tokens);
        isRun = false;
    }

    public int format(IndentStringBuilder sb, CharSequence input, int width, int curPos) {
        sb.append(input);
        return curPos;
    }

    public boolean canFormat() {
        return false;
    }

    public Selection expandSelection(String text, int oldStart, int oldEnd) {
        return new Selection(0, text.length() - 1);
    }

    protected abstract void tokenize(List<Pair> _tokens, String text);

    public final Language getLanguage() {
        return language;
    }

    public abstract String getLanguageType();


    public static class Selection {
        public final int start, len;

        public Selection(int start, int end) {
            this.start = start;
            this.len = end - start;
        }
    }
}
