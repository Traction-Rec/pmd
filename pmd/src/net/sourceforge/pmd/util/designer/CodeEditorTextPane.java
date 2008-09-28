package net.sourceforge.pmd.util.designer;

import java.util.StringTokenizer;

import javax.swing.JTextPane;

import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.util.LineGetter;

public class CodeEditorTextPane extends JTextPane implements LineGetter {

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    public String getLine(int number) {
        int count = 1;
        for (StringTokenizer st = new StringTokenizer(getText(), "\n"); st.hasMoreTokens();) {
            String tok = st.nextToken();
            if (count == number) {
                return tok;
            }
            count++;
        }
        throw new RuntimeException("Line number " + number + " not found");
    }

    private int getPosition(String[] lines, int line, int column) {
        int pos = 0;
        for (int count = 0; count < lines.length;) {
            String tok = lines[count++];
            if (count == line) {
                int linePos = 0;
                int i;
                for (i = 0; linePos < column; i++) {
                    linePos++;
                    if (tok.charAt(i) == '\t') {
                        linePos--;
                        linePos += 8 - (linePos & 07);
                    }
                }

                return pos + i - 1;
            }
            pos += tok.length() + 1;
        }
        throw new RuntimeException("Line " + line + " not found");
    }

    public void select(Node node) {
        String[] lines = getText().split(LINE_SEPARATOR);
        if (node.getBeginLine() >= 0) {
	    setSelectionStart(getPosition(lines, node.getBeginLine(), node.getBeginColumn()));
	    setSelectionEnd(getPosition(lines, node.getEndLine(), node.getEndColumn()) + 1);
	}
        requestFocus();
    }
}
