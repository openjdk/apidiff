/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package jdk.codetools.apidiff.report.html;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Chunk;
import com.github.difflib.patch.Patch;
import jdk.codetools.apidiff.Log;
import jdk.codetools.apidiff.html.Content;
import jdk.codetools.apidiff.html.HtmlTree;
import jdk.codetools.apidiff.html.TagName;
import jdk.codetools.apidiff.html.Text;
import jdk.codetools.apidiff.model.API;
import jdk.codetools.apidiff.report.html.ResultTable.CountKind;

/**
 * A class to build HTML to display the differences between plain-text strings,
 * such as the documentation comments for corresponding elements in different
 * instances of an API.
 */
public class TextDiffBuilder extends PairwiseDiffBuilder<String> {

    /**
     * Creates an instance of a {@code TextDiffBuilder}.
     *
     * @param pageReporter the reporter for the parent page
     */
    public TextDiffBuilder(PageReporter<?> pageReporter) {
        super(pageReporter.parent.apis, pageReporter.log, pageReporter.msgs);
    }

    @Override
    protected Content build(List<API> refAPIs, String refItem,
                            List<API> focusAPIs, String focusItem,
                            Consumer<ResultTable.CountKind> counter) {
        String refNames = getNameList(refAPIs);
        String focusNames = getNameList(focusAPIs);
        if (refItem != null && focusItem != null) {
            // item in both groups: display the comparison
            return build(refNames, getLines(refItem), focusNames, getLines(focusItem), counter);
        } else if (refItem == null) {
            Content title = Text.of(String.format("Not in %s; only in %s", refNames, focusNames)); // TODO: improve
            counter.accept(CountKind.COMMENT_ADDED);
            return build(title, focusItem);
        } else {
            Content title = Text.of(String.format("Only in %s; not in %s", refNames, focusNames)); // TODO: improve
            counter.accept(CountKind.COMMENT_REMOVED);
            return build(title, refItem);
        }
    }

    @Override
    protected String getKeyString(String item) {
        return item;
    }

    /**
     * Builds HTML that displays the differences between two sets of lines.
     *
     * @param refTitle a title for the "reference" set of lines
     * @param refLines the "reference" set of lines
     * @param modTitle a title for the "modified" set of lines
     * @param modLines the "modified" set of lines
     * @param counter  a counter for the instances of the differences found in the strings
     *
     * @return the HTML nodes
     */
    private Content build(String refTitle, List<String> refLines,
                  String modTitle, List<String> modLines,
                  Consumer<CountKind> counter) {
        Patch<String> patch = DiffUtils.diff(refLines, modLines);
        count(patch, counter);
        return build(refTitle, refLines, modTitle, modLines, patch);
    }

    private Content build(Content title, String item) {
        HtmlTree title2 = HtmlTree.DIV(title)
                .setClass("xdiffs-title");

        HtmlTree pre = HtmlTree.PRE(Text.of(item));
        List<Content> contents = List.of(title2, pre);
        return new HtmlTree(TagName.DIV, contents).setClass("xdiffs");
    }

    private void count(Patch<String> patch, Consumer<CountKind> counter) {
        for (AbstractDelta<String> delta : patch.getDeltas()) {
            Chunk<String> ref = delta.getSource();
            Chunk<String> mod = delta.getTarget();
            CountKind ck = isInsert(ref, mod) ? CountKind.COMMENT_ADDED
                            : isInsert(mod, ref) ? CountKind.COMMENT_REMOVED
                            : CountKind.COMMENT_CHANGED;
            counter.accept(ck);
        }
    }

    /**
     * Returns {@code true} if the chunk on the right is as if some text has been
     * inserted into the chunk on the left.
     *
     * @param left the left chunk
     * @param right the right chunk
     *
     * @return {@code true} if the chunk on the right is as if some text has been
     *         inserted into the chunk on the left
     */
    private boolean isInsert(Chunk<String> left, Chunk<String> right) {
        if (left.size() == 0) {
            return true;
        }

        if (left.size() == 1 && right.size() > 0) {
            String leftLine = left.getLines().get(0);

            List<String> rightLines = right.getLines();
            String firstRightLine = rightLines.get(0);
            String lastRightLine = rightLines.get(rightLines.size() - 1);
            int l = Math.min(leftLine.length(), firstRightLine.length());
            for (int i = 0; i < l && leftLine.charAt(i) == firstRightLine.charAt(i); i++) {
                if (lastRightLine.endsWith(leftLine.substring(i))) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Build HTML representing the content of a patch.
     *
     * @param refTitle the title for the "reference" side of the comparison
     * @param refLines the lines for the "reference" side of the comparison
     * @param modTitle the title for the "modified" side of the comparison
     * @param modLines the lines for the "modified" side of the comparison
     * @param patch the patch containing the differences
     * @return HTML displaying the differences
     */
    // TODO: we could support alternate presentations, perhaps
    //       selecting one-of-n views using some input control
    //       (e.g. radio buttons or a choice item) and JavaScript.
    Content build(String refTitle, List<String> refLines,
                  String modTitle, List<String> modLines,
                  Patch<String> patch) {
        return new SDiffs()
                .setReference(refTitle, refLines)
                .setModified(modTitle, modLines)
                .build(patch);
    }

    private List<String> getLines(String text) {
        return List.of(text.split("\\R"));
    }

    /**
     * A builder for side-by-side text diffs.
     *
     * <p>The structure is as follows:
     * <pre>{@code
     *     <div class="sdiffs">
     *         <div class="sdiffs-ref">
     *             reference-title
     *             <pre>
     *             ...
     *             reference-diffs
     *             ...
     *             </pre>
     *         </div>
     *         <div class="sdiffs-mod">
     *             modified-title
     *             <pre>
     *             ...
     *             modified-diffs
     *             ...
     *             </pre>
     *         </div>
     *     </div>
     * }</pre>
     */
    public static class SDiffs {
        private String refTitle;
        private List<String> refLines;
        private String modTitle;
        private List<String> modLines;

        private int contextSize = 5;
        private boolean showLineNumbers = true;

        /**
         * Sets the title and lines for the "reference" side of the comparison.
         *
         * @param title the title
         * @param lines the lines
         * @return this object
         */
        public SDiffs setReference(String title, List<String> lines) {
            this.refTitle = title;
            this.refLines = lines;
            return this;
        }

        /**
         * Sets the title and lines for the "modified" side of the comparison.
         *
         * @param title the title
         * @param lines the lines
         * @return this object
         */
        public SDiffs setModified(String title, List<String> lines) {
            this.modTitle = title;
            this.modLines = lines;
            return this;
        }

        /**
         * Sets the amount of context to show before and after each difference.
         *
         * @param size the number of lines to show
         * @return this object
         */
        public SDiffs setContextSize(int size) {
            contextSize = size;
            return this;
        }

        /**
         * Sets whether to show line numbers in the output.
         *
         * @param showLineNumbers whether to show line numbers
         * @return this object
         */
        public SDiffs setShowLineNumbers(boolean showLineNumbers) {
            this.showLineNumbers = showLineNumbers;
            return this;
        }

        /**
         * Build HTML to display the differences between the two sets of input.
         * If an exception occurs while computing the differences, a message will
         * be written to the log, and {@link Content#empty} returned.
         *
         * @param log the log
         * @return the HTML node, or {@link Content#empty}
         */
        public Content build(Log log) {
            Patch<String> patch = DiffUtils.diff(refLines, modLines); // just differences; do not include EQUAL chunks
            return build(patch);
        }

        /**
         * Build HTML to display the differences contained in a patch.
         *
         * @param patch the patch
         * @return the HTML nodes
         */
        public Content build(Patch<String> patch) {
            if (patch.getDeltas().isEmpty()) {
                return Content.empty;
            }

            List<Content> refDiffs = new ArrayList<>();
            List<Content> modDiffs = new ArrayList<>();

            int refIndex = 0;
            int modIndex = 0;
            for (AbstractDelta<String> delta : patch.getDeltas()) {
                Chunk<String> refChunk = delta.getSource();
                Chunk<String> modChunk = delta.getTarget();

                addContext(refDiffs, refLines, refIndex, refChunk.getPosition());
                addContext(modDiffs, modLines, modIndex, modChunk.getPosition());

//                int maxSize = Math.max(refChunk.size(), modChunk.size());
//                addDiffLines(refDiffs, refChunk, maxSize);
//                addDiffLines(modDiffs, modChunk, maxSize);

                addDiffLines(refDiffs, modDiffs, delta);

                refIndex = refChunk.last() + 1;
                modIndex = modChunk.last() + 1;
            }

            addContext(refDiffs, refLines, refIndex, Math.min(refIndex + contextSize, refLines.size()));
            addContext(modDiffs, modLines, modIndex, Math.min(modIndex + contextSize, modLines.size()));

            Content refDiv = HtmlTree.DIV()
                    .setClass("sdiffs-ref")
                    .add(HtmlTree.DIV(new Text(refTitle)).setClass("sdiffs-title"))
                    .add(refDiffs);
            Content modDiv = HtmlTree.DIV()
                    .setClass("sdiffs-mod")
                    .add(HtmlTree.DIV(new Text(modTitle)).setClass("sdiffs-title"))
                    .add(modDiffs);
            return HtmlTree.DIV(refDiv, modDiv).setClass("sdiffs");
        }

        private void addContext(List<Content> contents, List<String> lines, int from, int to) {
            if (to > from + 2 * contextSize) {
                addLines(contents, lines, from, from + contextSize);
                contents.add(new HtmlTree(TagName.HR));
                addLines(contents, lines, to - contextSize, to);
            } else {
                addLines(contents, lines, from, to);
            }
        }

        void addLines(List<Content> contents, List<String> lines, int from, int to) {
            StringBuilder sb = new StringBuilder();
            for (int i = from; i < to; i++) {
                if (showLineNumbers) {
                    sb.append(formatLineNumber(i + 1));
                }
                sb.append(lines.get(i)).append("\n");
            }
            HtmlTree pre = ensurePre(contents);
            pre.add(new Text(sb.toString()));
        }

        /**
         * Show the content of a chunk from a line-oriented diff.
         * The content is shown in a single block (including line numbers)
         * with CSS class {@code sdiffs-changed}.
         * The content is padding with blank lines, if necessary,
         * up to a given number of lines.
         *
         * @param contents the contents to which to add the details
         * @param chunk the chunk
         * @param maxSize the number of lines to be displayed
         */
        // An alternate presentation would be to just style the content
        // of each line (but not the line number) but that would not
        // highlight blank lines.
        // Another alternate presentation would be to use background color
        // for the CSS style.
        void addDiffLines(List<Content> contents, Chunk<String> chunk, int maxSize) {
            StringBuilder sb = new StringBuilder();
            List<String> lines = chunk.getLines();
            for (int i = 0; i < maxSize; i++) {
                if (i < lines.size()) {
                    if (showLineNumbers) {
                        sb.append(formatLineNumber(chunk.getPosition() + i + 1));
                    }
                    sb.append(lines.get(i));
                } else {
                    sb.append(" ");
                }
                sb.append("\n");
            }
            HtmlTree pre = ensurePre(contents);
            pre.add(HtmlTree.SPAN(new Text(sb.toString())).setClass("sdiffs-changed"));
        }

        /**
         * Show the differences in a delta from a line-oriented diff.
         * The chunks are tokenized and diffed, in order to show intra-delta diffs.
         *
         * @param refDiffs the content for the diffs for the reference text
         * @param modDiffs the content for the diffs for the modified text
         * @param deltaLines the delta between the two sides
         */
        void addDiffLines(List<Content> refDiffs, List<Content> modDiffs, AbstractDelta<String> deltaLines) {
            var refChunk = deltaLines.getSource();
            var refDefaultCSSClass = switch (deltaLines.getType()) {
                case CHANGE -> "sdiffs-lines-changed";
                case DELETE -> "sdiffs-lines-deleted";
                default -> null;
            };
            var refTDiffs = new TokenDiffs(refDiffs, refChunk, refDefaultCSSClass);
            List<String> refTokens = tokens(refChunk.getLines());

            var modChunk = deltaLines.getTarget();
            var modDefaultCSSClass = switch (deltaLines.getType()) {
                case CHANGE -> "sdiffs-lines-changed";
                case INSERT -> "sdiffs-lines-inserted";
                default -> null;
            };
            var modTDiffs = new TokenDiffs(modDiffs, modChunk, modDefaultCSSClass);
            List<String> modTokens = tokens(modChunk.getLines());

            Patch<String> patch = DiffUtils.diff(refTokens, modTokens, true); // include EQUAL chunks

            for (var delta : patch.getDeltas()) {
                var deltaType = delta.getType();
                String cssClass = switch (deltaType) {
                    case EQUAL -> null;
                    case DELETE -> "sdiffs-chars-deleted";
                    case CHANGE -> "sdiffs-chars-changed";
                    case INSERT -> "sdiffs-chars-inserted";
                };
                delta.getSource().getLines().forEach(t -> refTDiffs.add(t, cssClass));
                delta.getTarget().getLines().forEach(t -> modTDiffs.add(t, cssClass));
            }

            int refChunkSize = refChunk.size();
            int modChunkSize = modChunk.size();
            int maxChunkSize = Math.max(refChunkSize, modChunkSize);
            refTDiffs.padNewlines(maxChunkSize - refChunkSize);
            modTDiffs.padNewlines(maxChunkSize - modChunkSize);
        }

        /**
         * A builder for the diffs in a sequence of tokens, that can be used to show
         * the diffs within a chunk in an {@link SDiffs} comparison.
         */
        class TokenDiffs {
            private final HtmlTree pre;
            private StringBuilder pendingText;
            private String pendingCSSClass;
            private int pendingLineNumber;
            private int displayedLineNumber;
            private final String defaultCSSClass;

            /**
             * Creates a builder to display one side of the differences in a {@code SDiffs} chunk.
             *
             * @param diffs the content of line-diffs to which the token-diffs will be added
             * @param lineChunk the chunk containing the differences, used to get the initial line number
             * @param defaultCSSClass the default CSS class for the diffs, such as a background color
             */
            TokenDiffs(List<Content> diffs, Chunk<String> lineChunk, String defaultCSSClass) {
                pre = ensurePre(diffs);
                pendingText = new StringBuilder();
                pendingCSSClass = null;
                pendingLineNumber = lineChunk.getPosition() + 1;
                displayedLineNumber = -1;
                this.defaultCSSClass = defaultCSSClass;
            }

            /**
             * Adds a token to the display.
             * Newlines should be presented as a single-character string, and not within a longer string.
             *
             * @param token the token
             * @param cssClass the class for the token, or {@code null} if none required
             */
            void add(String token, String cssClass) {
                if (token.equals("\n")) {
                    flush();
                    pre.add("\n");
                    pendingLineNumber++;
                    return;
                }

                if (Objects.equals(cssClass, pendingCSSClass)) {
                    pendingText.append(token);
                } else {
                    flush();
                    pendingText.append(token);
                    pendingCSSClass = cssClass;
                }
            }

            /**
             * Flush any pending text.
             */
            void flush() {
                if (pendingLineNumber > displayedLineNumber) {
                    pre.add(new Text(formatLineNumber(pendingLineNumber)));
                    displayedLineNumber = pendingLineNumber;
                }
                if (!pendingText.isEmpty()) {
                    var text = new Text(pendingText.toString());
                    pre.add(pendingCSSClass != null ? HtmlTree.SPAN(text).setClass(pendingCSSClass)
                            : defaultCSSClass != null ? HtmlTree.SPAN(text).setClass(defaultCSSClass)
                            : text);
                }
                pendingText.setLength(0);
            }

            /**
             * Pad the output with newlines.
             *
             * @param n the number of newlines required
             */
            void padNewlines(int n) {
                if (n > 0) {
                    pre.add(new Text("\n".repeat(n)));
                }
            }
        }

        /**
         * Break a series of lines into a series of smaller tokens.
         * In this implementation, the tokens are:
         *
         * <ul>
         * <li>identifiers
         * <li>decimal integers
         * <li>runs of horizontal whitespace
         * <li>other individual characters
         * </ul>
         *
         * Newline characters should not be found in the input lines.
         * A string containing a newline character will be added to the list of
         * tokens after each line has been processed.
         *
         * Other implementations are possible, including all characters as individual tokens.
         * The tradeoff is the desired granularity and resolution of the resulting tokens.
         *
         * @param lines the input lines
         * @return the tokens
         */
        List<String> tokens(List<String> lines) {
            var result = new ArrayList<String>();
            for (var line : lines) {
                int i = 0;
                while (i < line.length()) {
                    char ch = line.charAt(i);
                    if (Character.isUnicodeIdentifierStart(ch)) {
                        int p = i++;
                        while (i < line.length()) {
                            ch = line.charAt(i);
                            if (!Character.isUnicodeIdentifierPart(ch)) {
                                break;
                            }
                            i++;
                        }
                        result.add(line.substring(p, i));
                    } else if (Character.isDigit(ch)) {
                        int p = i++;
                        while (i < line.length()) {
                            ch = line.charAt(i);
                            if (!Character.isDigit(ch)) {
                                break;
                            }
                            i++;
                        }
                        result.add(line.substring(p, i));
                    } else if (Character.isWhitespace(ch)) {
                        int p = i++;
                        while (i < line.length()) {
                            ch = line.charAt(i);
                            if (!Character.isWhitespace(ch)) {
                                break;
                            }
                            i++;
                        }
                        result.add(line.substring(p, i));
                    } else {
                        result.add(String.valueOf(ch));
                        i++;
                    }
                }
                result.add("\n");
            }
            return result;
        }

        /**
         * Ensures that a list of contents has a {@code <pre>} element as the last
         * element, and return that element.  A new element will be created and
         * added to the list if the last element is not a {@code <pre>} element.
         *
         * @param contents the list of contents
         * @return the {@code <pre>} element at the end of the list
         */
        HtmlTree ensurePre(List<Content> contents) {
            if (contents.size() > 0) {
                Content last = contents.get(contents.size() - 1);
                if (last instanceof HtmlTree t && t.hasTag(TagName.PRE)) {
                    return t;
                }
            }
            HtmlTree t = HtmlTree.PRE();
            contents.add(t);
            return t;
        }

        private String formatLineNumber(int n) {
            return String.format("%4d ", n);
        }

//        private void showChunk(String name, Chunk<String> c) {
//            System.err.println("CHUNK: " + name + " " + (c == null ? "null" : c.toString()));
//        }

    }
}
