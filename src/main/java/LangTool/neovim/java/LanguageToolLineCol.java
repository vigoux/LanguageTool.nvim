package LangTool.neovim.java;

import java.util.List;

public class LanguageToolLineCol {

    private int line;
    private int col;

    public LanguageToolLineCol(int index, List<String> sourceText) {
        int lineCount = 0;
        for (String line : sourceText) {
            if ( line.length() > index ) {
                break;
            } else {
                index -= (line.length() + 1);
                lineCount += 1;
            }
        }

        this.line = lineCount;
        this.col = index;
    }

    public int getLine() {
        return this.line;
    }

    public int getCol() {
        return this.col;
    }

    public String toString() {
        return String.format("(%s, %s)", this.line, this.col);
    }
}
