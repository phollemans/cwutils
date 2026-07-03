/*
 * CoastWatch Software Library and Utilities
 * Copyright (c) 2026 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.helpagent.markdown;

/**
 * Formats flowing terminal text using word wrapping and continuation indents.
 *
 * @author Peter Hollemans
 * @since 4.2.0
 */
public class TerminalFlowFormatter {

  private final TerminalWriter writer;
  private final int lineWidth;
  private boolean pendingSpace;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new terminal flow formatter.
   *
   * @param writer the terminal writer used for output
   * @param lineWidth the visible width used for wrapping
   */
  public TerminalFlowFormatter (
    TerminalWriter writer,
    int lineWidth
  ) {

    this.writer = writer;
    this.lineWidth = lineWidth;

  } // TerminalFlowFormatter ctor

  ////////////////////////////////////////////////////////////

  /**
   * Writes flowing text using wrapping and a continuation indent.
   *
   * @param text the text to write
   * @param continuationIndent the indent for wrapped continuation lines
   */
  public void writeWrapped (
    String text,
    String continuationIndent
  ) {

    if (text.isEmpty()) return;
    if (continuationIndent == null) continuationIndent = "";

    int length = text.length();
    int index = 0;

    while (index < length) {
      char ch = text.charAt (index);

      // Preserve ANSI escape sequences as part of the current output style.
      if (ch == '\u001B' && index + 1 < length && text.charAt (index + 1) == '[') {
        int end = index + 2;
        while (end < length) {
          char code = text.charAt (end++);
          if (code >= 0x40 && code <= 0x7E) break;
        } // while
        writer.write (text.substring (index, end));
        index = end;
        continue;
      } // if

      // Treat any run of spaces or tabs as a pending single word separator.
      if (ch == ' ' || ch == '\t') {
        pendingSpace = true;
        index++;
        continue;
      } // if

      // Preserve explicit newlines from the markdown renderer.
      if (ch == '\n') {
        writer.newline();
        if (!continuationIndent.isEmpty()) writer.write (continuationIndent);
        pendingSpace = false;
        index++;
        continue;
      } // if

      // Emit a non-whitespace token, wrapping first if needed.
      int start = index;
      while (index < length) {
        ch = text.charAt (index);
        if (ch == '\u001B' || ch == ' ' || ch == '\t' || ch == '\n') break;
        index++;
      } // while

      String token = text.substring (start, index);
      int tokenWidth = token.length();
      int neededWidth = tokenWidth;
      if (pendingSpace && !writer.isStartOfLine()) neededWidth++;

      if (
        !writer.isStartOfLine() &&
        writer.getCurrentColumn() + neededWidth > lineWidth &&
        !isLeadingPunctuation (token)
      ) {
        writer.newline();
        if (!continuationIndent.isEmpty()) writer.write (continuationIndent);
        pendingSpace = false;
      } // if

      if (pendingSpace && !writer.isStartOfLine()) writer.write (" ");
      writer.write (token);
      pendingSpace = false;
    } // while

  } // writeWrapped

  ////////////////////////////////////////////////////////////

  /**
   * Detects punctuation that should stay attached to the previous token when
   * wrapping flowing text.
   *
   * @param token the token to inspect
   *
   * @return true if the token starts with leading punctuation
   */
  private static boolean isLeadingPunctuation (
    String token
  ) {

    if (token.isEmpty()) return (false);
    return (",.;:!?)]}".indexOf (token.charAt (0)) >= 0);

  } // isLeadingPunctuation

  ////////////////////////////////////////////////////////////

  /**
   * Resets pending inline flow state at a block boundary.
   */
  public void reset () {

    pendingSpace = false;

  } // reset

  ////////////////////////////////////////////////////////////

} // TerminalFlowFormatter
