package com.nexusuniverse.vice.guidebook;

import java.util.ArrayList;
import java.util.List;

/**
 * Minecraft written books have a real page-limit problem: the game enforces a hard ~798-character
 * cap per page, but the actual VISIBLE area only fits roughly 14 lines at default text size --
 * text well under the character cap can still get cut off/invisible if it's crammed onto one page
 * without respecting the line limit too. This wraps to a conservative character width and
 * hard-stops each page at a safe line count, starting a new page for overflow rather than
 * truncating anything. General-purpose, not guidebook-specific -- reusable for any future book
 * content in this plugin.
 */
public final class BookPaginator {

    private static final int CHARS_PER_LINE = 18;
    private static final int LINES_PER_PAGE = 13;

    private BookPaginator() {}

    /** Turns a list of paragraphs into finished book pages -- each paragraph word-wraps to one or more lines, with a blank line separating paragraphs (never at the very top of a fresh page). */
    public static List<String> paginate(List<String> paragraphs) {
        List<String> pages = new ArrayList<>();
        StringBuilder currentPage = new StringBuilder();
        int linesOnPage = 0;

        for (String paragraph : paragraphs) {
            for (String line : wrap(paragraph)) {
                if (linesOnPage >= LINES_PER_PAGE) {
                    pages.add(currentPage.toString());
                    currentPage = new StringBuilder();
                    linesOnPage = 0;
                }
                currentPage.append(line).append('\n');
                linesOnPage++;
            }
            if (linesOnPage > 0 && linesOnPage < LINES_PER_PAGE) {
                currentPage.append('\n'); // blank separator line between paragraphs
                linesOnPage++;
            }
        }
        if (currentPage.length() > 0) pages.add(currentPage.toString());
        return pages;
    }

    private static List<String> wrap(String text) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            int extra = line.length() > 0 ? 1 : 0;
            if (line.length() + word.length() + extra > CHARS_PER_LINE) {
                lines.add(line.toString());
                line = new StringBuilder();
            }
            if (line.length() > 0) line.append(' ');
            line.append(word);
        }
        if (line.length() > 0) lines.add(line.toString());
        return lines;
    }
}
