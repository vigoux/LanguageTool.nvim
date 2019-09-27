package LangTool.neovim.java;

public class LanguageToolLineCol {

    private int line;
    private int col;

    public LanguageToolLineCol(int line, int col) {
        this.line = line;
        this.col = col;
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
