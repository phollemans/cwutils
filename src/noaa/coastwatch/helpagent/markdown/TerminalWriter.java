/*
 * CoastWatch Software Library and Utilities
 * Copyright (c) 2026 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.helpagent.markdown;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * The <code>TerminalWriter</code> class writes text to a terminal output
 * stream while maintaining simple layout state for higher-level formatters.
 * The writer tracks the current visible output column and the number of
 * trailing newline characters so callers can wrap text and separate rendered
 * blocks without inspecting the output stream directly.
 *
 * <p>ANSI escape codes may be written through this class and are ignored when
 * calculating visible column width.  Newline helpers are also provided for
 * emitting a single line break or ensuring one blank line of separation between
 * output blocks.</p>
 *
 * @author Peter Hollemans
 * @since 4.2.0
 */
public class TerminalWriter {

  private final PrintWriter out;
  private int currentColumn;
  private int trailingNewlines;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new terminal writer.
   *
   * @param out the output stream to write to
   */
  public TerminalWriter (
    PrintStream out
  ) {

    this (new PrintWriter (out, true));

  } // TerminalWriter ctor

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new terminal writer.
   *
   * @param out the output writer to write to
   */
  public TerminalWriter (
    PrintWriter out
  ) {

    this.out = out;

  } // TerminalWriter ctor

  ////////////////////////////////////////////////////////////

  /**
   * Writes text to the output stream and updates terminal layout state.
   *
   * @param text the text to write
   */
  public void write (
    String text
  ) {

    if (text.isEmpty()) return;

    out.print (text);
    out.flush();
    updateState (text);

  } // write

  ////////////////////////////////////////////////////////////

  /**
   * Emits a newline if fewer than two trailing newlines are present.
   */
  public void newline () {

    if (trailingNewlines < 2) {
      out.println();
      out.flush();
      trailingNewlines++;
      currentColumn = 0;
    } // if

  } // newline

  ////////////////////////////////////////////////////////////

  /**
   * Ensures exactly one blank line of separation between blocks.
   */
  public void blankLine () {

    while (trailingNewlines < 2) newline();

  } // blankLine

  ////////////////////////////////////////////////////////////

  /**
   * Gets the current visible output column.
   *
   * @return the current column
   */
  public int getCurrentColumn () {

    return (currentColumn);

  } // getCurrentColumn

  ////////////////////////////////////////////////////////////

  /**
   * Checks if output is currently at the start of a line.
   *
   * @return true if the current column is zero
   */
  public boolean isStartOfLine () {

    return (currentColumn == 0);

  } // isStartOfLine

  ////////////////////////////////////////////////////////////

  /**
   * Gets the number of trailing newlines currently emitted.
   *
   * @return the trailing newline count, capped at 2
   */
  public int getTrailingNewlines () {

    return (trailingNewlines);

  } // getTrailingNewlines

  ////////////////////////////////////////////////////////////

  /**
   * Updates the terminal layout state based on emitted text.
   *
   * @param text the emitted text
   */
  private void updateState (
    String text
  ) {

    int length = text.length();
    int index = 0;
    int newlineCount = 0;
    boolean sawLayoutChar = false;
 
    while (index < length) {
      char ch = text.charAt (index);

      // Skip ANSI escape sequences when calculating visible width.
      if (ch == '\u001B' && index + 1 < length && text.charAt (index + 1) == '[') {
        index += 2;
        while (index < length) {
          char code = text.charAt (index++);
          if (code >= 0x40 && code <= 0x7E) break;
        } // while
        continue;
      } // if

      sawLayoutChar = true;

      if (ch == '\n') {
        currentColumn = 0;
        newlineCount++;
      } // if
      else {
        currentColumn++;
        newlineCount = 0;
      } // else

      index++;
    } // while

    if (sawLayoutChar) {
      trailingNewlines = Math.min (2, newlineCount);
    } // if

  } // updateState

  ////////////////////////////////////////////////////////////

} // TerminalWriter
