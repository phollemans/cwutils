/*
 * CoastWatch Software Library and Utilities
 * Copyright (c) 2026 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.helpagent.markdown;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.Map;

/**
 * Renders Markdown events to a text terminal.
 *
 * @author Peter Hollemans
 * @since 4.2.0
 */
public class TerminalSink implements MarkdownSink {

  /** Identifiers for configured terminal styles. */
  private enum StyleKey {
    HEADING1,
    HEADING2,
    HEADING3,
    EMPHASIS,
    STRONG,
    INLINE_CODE,
    CODE_BLOCK
  } // StyleKey

  /** Simple terminal style descriptor. */
  private static class Style {

    String ansi;
    String prefix;

    Style (
      String ansi,
      String prefix
    ) {

      this.ansi = ansi;
      this.prefix = prefix;

    } // Style ctor

  } // Style

  /** Sample markdown used for terminal rendering tests. */
  private static final String SAMPLE_MARKDOWN =
    "# Heading 1 with `inline code` in the middle\n\n" +
    "This is a paragraph with *emphasis*, **strong text**, and `inline code`.\n\n" +
    "## Lists\n\n" +
    "### First a bulleted list\n" +
    "- First bullet item\n" +
    "  1. Nested numbered item in bullet A\n" +
    "  2. Nested numbered item in bullet B\n" +
    "  - Nested bullet item A\n" +
    "  - Nested bullet item B\n" +
    "    - Deep nested bullet item\n" +
    "- Second bullet item\n" +
    "- Third bullet item\n\n" +
    "### Second an ordered list\n" +
    "1. First numbered item\n" +
    "   - Nested bullet item in number A\n" +
    "   - Nested bullet item in number B\n" +
    "     1. Nested numbered item A\n" +
    "     2. Nested numbered item B\n" +
    "2. Second numbered item\n\n" +
    "## Table\n\n" +
    "| Name | Purpose |\n" +
    "| --- | --- |\n" +
    "| cwrender | Renders images from data |\n" +
    "| cwstats | Shows statistics |\n\n" +
    "## Code\n\n" +
    "```shell\n" +
    "cwrender --help\n" +
    "cwstats input.nc\n" +
    "```\n";

  /** The numver of characters per line in normal text output. */
  private static final int LINE_WRAP = 80;

  private final TerminalWriter writer;
  private final TerminalFlowFormatter flowFormatter;
  private final boolean colorEnabled;
  private final Map<StyleKey, Style> styles;
  private final Deque<Style> styleStack;
  private boolean inTableRow;
  private boolean firstTableCell;
  private int listDepth;
  private Deque<Integer> indentStack;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a terminal sink that writes to standard output.
   */
  public TerminalSink () {

    this (System.out, false);

  } // TerminalSink ctor

  ////////////////////////////////////////////////////////////

  /**
   * Creates a terminal sink.
   *
   * @param out the output stream for rendered text
   */
  public TerminalSink (
    PrintStream out
  ) {

    this (out, false);

  } // TerminalSink ctor

  ////////////////////////////////////////////////////////////

  /**
   * Creates a terminal sink.
   *
   * @param out the output stream for rendered text
   * @param colorEnabled true to enable ANSI terminal colors
   */
  public TerminalSink (
    PrintStream out,
    boolean colorEnabled
  ) {

    this (new PrintWriter (out, true), colorEnabled);

  } // TerminalSink ctor

  ////////////////////////////////////////////////////////////

  /**
   * Creates a terminal sink.
   *
   * @param out the output writer for rendered text
   * @param colorEnabled true to enable ANSI terminal colors
   */
  public TerminalSink (
    PrintWriter out,
    boolean colorEnabled
  ) {

    this.colorEnabled = colorEnabled;
    this.styles = createStyles();
    this.styleStack = new ArrayDeque<>();
    this.indentStack = new ArrayDeque<>();
    this.writer = new TerminalWriter (out);
    this.flowFormatter = new TerminalFlowFormatter (writer, LINE_WRAP);

  } // TerminalSink ctor

  ////////////////////////////////////////////////////////////

  /**
   * Creates the initial style map used by the sink.
   *
   * @return the style map
   */
  private Map<StyleKey, Style> createStyles () {

    Map<StyleKey, Style> map = new EnumMap<> (StyleKey.class);
    map.put (StyleKey.HEADING1, new Style (Ansi.BOLD + Ansi.FG_BRIGHT_CYAN, ""));
    map.put (StyleKey.HEADING2, new Style (Ansi.BOLD + Ansi.FG_CYAN, ""));
    map.put (StyleKey.HEADING3, new Style (Ansi.FG_CYAN, ""));
    map.put (StyleKey.EMPHASIS, new Style (Ansi.ITALIC, ""));
    map.put (StyleKey.STRONG, new Style (Ansi.BOLD, ""));
    map.put (StyleKey.INLINE_CODE, new Style (Ansi.FG_YELLOW, ""));
    map.put (StyleKey.CODE_BLOCK, new Style (Ansi.FG_MAGENTA, ""));

    return (map);

  } // createStyles

  ////////////////////////////////////////////////////////////

  /**
   * Gets a configured style by key.
   *
   * @param key the style key
   *
   * @return the style
   */
  private Style getStyle (
    StyleKey key
  ) {

    return (styles.get (key));

  } // getStyle

  ////////////////////////////////////////////////////////////

  /**
   * Applies the currently active style stack.
   */
  private void refreshStyle () {

    if (!colorEnabled) return;

    writer.write (Ansi.RESET);
    for (var iter = styleStack.descendingIterator(); iter.hasNext(); ) {
      Style style = iter.next();
      if (!style.ansi.isEmpty()) writer.write (style.ansi);
    } // for

  } // refreshStyle

  ////////////////////////////////////////////////////////////

  /**
   * Pushes a style onto the active stack.
   *
   * @param style the style to push
   */
  private void pushStyle (
    Style style
  ) {

    styleStack.push (style);
    refreshStyle ();
    if (!style.prefix.isEmpty()) writer.write (style.prefix);

  } // pushStyle

  ////////////////////////////////////////////////////////////

  /**
   * Pops the current active style from the stack.
   */
  private void popStyle () {

    if (!styleStack.isEmpty()) styleStack.pop();
    refreshStyle ();

  } // popStyle

  ////////////////////////////////////////////////////////////

  @Override
  public void startDocument () {

  } // startDocument

  ////////////////////////////////////////////////////////////

  @Override
  public void endDocument () {

  } // endDocument

  ////////////////////////////////////////////////////////////

  @Override
  public void startParagraph () {

    if (writer.isStartOfLine() && !indentStack.isEmpty()) {
      String indent = getHangingIndent();
      if (!indent.isEmpty()) writer.write (indent);
    } // if

  } // startParagraph

  ////////////////////////////////////////////////////////////

  @Override
  public void endParagraph () {

    flowFormatter.reset();
    writer.blankLine();

  } // endParagraph

  ////////////////////////////////////////////////////////////

  @Override
  public void startHeading (
    int level
  ) {

    if (level == 1) pushStyle (getStyle (StyleKey.HEADING1));
    else if (level == 2) pushStyle (getStyle (StyleKey.HEADING2));
    else {
      Style style = getStyle (StyleKey.HEADING3);
      pushStyle (new Style (style.ansi, style.prefix.repeat (Math.max (1, level-2))));
    } // else

  } // startHeading

  ////////////////////////////////////////////////////////////

  @Override
  public void endHeading (
    int level
  ) {

    popStyle();
    flowFormatter.reset();
    writer.blankLine();

  } // endHeading

  ////////////////////////////////////////////////////////////

  @Override
  public void startEmphasis () {

    pushStyle (getStyle (StyleKey.EMPHASIS));

  } // startEmphasis

  ////////////////////////////////////////////////////////////

  @Override
  public void endEmphasis () {

    popStyle ();

  } // endEmphasis

  ////////////////////////////////////////////////////////////

  @Override
  public void startStrong () {

    pushStyle (getStyle (StyleKey.STRONG));

  } // startStrong

  ////////////////////////////////////////////////////////////

  @Override
  public void endStrong () {

    popStyle ();

  } // endStrong

  ////////////////////////////////////////////////////////////

  @Override
  public void text (
    String text
  ) {

    flowFormatter.writeWrapped (text, getHangingIndent());

  } // text

  ////////////////////////////////////////////////////////////

  @Override
  public void inlineCode (
    String code
  ) {

    pushStyle (getStyle (StyleKey.INLINE_CODE));
    flowFormatter.writeWrapped (code, getHangingIndent());
    popStyle ();

  } // inlineCode

  ////////////////////////////////////////////////////////////

  @Override
  public void lineBreak () {

    flowFormatter.reset();
    writer.newline();
    String indent = getHangingIndent();
    if (!indent.isEmpty()) writer.write (indent);

  } // lineBreak

  ////////////////////////////////////////////////////////////

  @Override
  public void startBulletList () {

    listDepth++;

  } // startBulletList

  ////////////////////////////////////////////////////////////

  @Override
  public void endBulletList () {

    if (listDepth > 0) listDepth--;
    writer.blankLine();

  } // endBulletList

  ////////////////////////////////////////////////////////////

  @Override
  public void startOrderedList (
    int startNumber
  ) {

    listDepth++;

  } // startOrderedList

  ////////////////////////////////////////////////////////////

  @Override
  public void endOrderedList () {

    if (listDepth > 0) listDepth--;
    writer.blankLine();

  } // endOrderedList

  ////////////////////////////////////////////////////////////

  private void startListItem (String prefix) {

    writer.write (prefix);
    indentStack.push (prefix.length());

  } // startListItem

  ////////////////////////////////////////////////////////////

  private void endListItem () {

    indentStack.pop();
    flowFormatter.reset();

  } // endListItem

  ////////////////////////////////////////////////////////////

  @Override
  public void startBulletListItem () {

    startListItem (getListIndent() + getBulletSymbol());

  } // startBulletListItem

  ////////////////////////////////////////////////////////////

  @Override
  public void endBulletListItem () {

    endListItem();

  } // endBulletListItem

  ////////////////////////////////////////////////////////////

  @Override
  public void startOrderedListItem (
    int index
  ) {

    startListItem (getListIndent() + index + ". ");

  } // startOrderedListItem

  ////////////////////////////////////////////////////////////

  @Override
  public void endOrderedListItem () {

    endListItem();

  } // endOrderedListItem

  ////////////////////////////////////////////////////////////

  /**
   * Gets the current list hanging indentation.
   *
   * @return the hanging indentation string
   */
  private String getHangingIndent () {

    String indent = "";
    if (listDepth > 0 && !indentStack.isEmpty()) {
      indent = " ".repeat (indentStack.peek());
    } // if

    return (indent);

  } // getHangingIndent

  ////////////////////////////////////////////////////////////

  /**
   * Gets the current list indentation.
   *
   * @return the indentation string
   */
  private String getListIndent () {

    int depth = Math.max (listDepth, 1);
    return ("  ".repeat (depth));

  } // getListIndent

  ////////////////////////////////////////////////////////////

  /**
   * Writes verbatim block text using the current block indentation.
   *
   * @param text the text to write
   * @param indent the indentation to apply at the start of each line
   */
  private void writeIndentedBlockText (
    String text,
    String indent
  ) {

    int index = 0;
    while (index < text.length()) {
      if (!indent.isEmpty() && writer.isStartOfLine()) writer.write (indent);
      int newline = text.indexOf ('\n', index);
      if (newline < 0) {
        writer.write (text.substring (index));
        index = text.length();
      } // if
      else {
        writer.write (text.substring (index, newline + 1));
        index = newline + 1;
      } // else
    } // while

  } // writeIndentedBlockText

  ////////////////////////////////////////////////////////////

  /**
   * Gets the bullet symbol for the current list depth.
   *
   * @return the bullet symbol
   */
  private String getBulletSymbol () {

    int depth = Math.max (listDepth, 1);
    switch ((depth - 1) % 3) {
      case 0:
        return "• ";
      case 1:
        return "> ";
      default:
        return "◦ ";
    } // switch

  } // getBulletSymbol

  ////////////////////////////////////////////////////////////

  @Override
  public void startCodeBlock (
    String info
  ) {

    flowFormatter.reset();
    pushStyle (getStyle (StyleKey.CODE_BLOCK));
    if (!info.isEmpty()) {
      String indent = getHangingIndent();
      if (!indent.isEmpty() && writer.isStartOfLine()) writer.write (indent);
      writer.write ("[" + info + "]\n");
    } // if

  } // startCodeBlock

  ////////////////////////////////////////////////////////////

  @Override
  public void codeBlockText (
    String text
  ) {

    writeIndentedBlockText (text, getHangingIndent());
    if (!text.endsWith ("\n")) writer.newline();

  } // codeBlockText

  ////////////////////////////////////////////////////////////

  @Override
  public void endCodeBlock () {

    popStyle ();
    flowFormatter.reset();
    writer.blankLine();

  } // endCodeBlock

  ////////////////////////////////////////////////////////////

  @Override
  public void thematicBreak () {

    flowFormatter.reset();
    writer.write ("------------------------------------------------------------");
    writer.blankLine();

  } // thematicBreak

  ////////////////////////////////////////////////////////////

  @Override
  public void startTable () {

  } // startTable

  @Override
  public void startTableHeader () { }
  @Override
  public void endTableHeader () { }

  @Override
  public void startTableSeparator () { }
  @Override
  public void endTableSeparator () { }

  @Override
  public void startTableBody () { }
  @Override
  public void endTableBody () { }

  ////////////////////////////////////////////////////////////

  @Override
  public void endTable () {

    flowFormatter.reset();
    writer.blankLine();

  } // endTable

  ////////////////////////////////////////////////////////////

  @Override
  public void startTableRow () {

    inTableRow = true;
    firstTableCell = true;

  } // startTableRow

  ////////////////////////////////////////////////////////////

  @Override
  public void endTableRow () {

    if (inTableRow) writer.newline();
    flowFormatter.reset();
    inTableRow = false;

  } // endTableRow

  ////////////////////////////////////////////////////////////

  @Override
  public void startTableCell (boolean header) {

    if (!firstTableCell) writer.write (" | ");
    firstTableCell = false;

  } // startTableCell

  ////////////////////////////////////////////////////////////

  @Override
  public void endTableCell () {



  } // endTableCell

  ////////////////////////////////////////////////////////////

  /**
   * Resets terminal block state before writing out-of-band diagnostics.
   */
  private void resetBlockState () {

    styleStack.clear();
    refreshStyle();
    flowFormatter.reset();
    inTableRow = false;
    firstTableCell = false;
    listDepth = 0;
    indentStack.clear();
    writer.blankLine();

  } // resetBlockState

  ////////////////////////////////////////////////////////////

  @Override
  public void error (
    String text
  ) {

    resetBlockState();
    if (colorEnabled) writer.write (Ansi.FG_BRIGHT_RED);
    flowFormatter.writeWrapped (text, "");
    if (colorEnabled) writer.write (Ansi.RESET);
    flowFormatter.reset();
    writer.blankLine();

  } // error

  ////////////////////////////////////////////////////////////

  /**
   * Runs a simple terminal rendering test using representative markdown.
   *
   * @param argv the command line parameters
   */
  public static void main (
    String[] argv
  ) {

    MarkdownStreamListener listener = new MarkdownStreamListener (
      new TerminalSink (System.out, System.console() != null)
    );

    for (int i = 0; i < SAMPLE_MARKDOWN.length(); i += 24) {
      int end = Math.min (SAMPLE_MARKDOWN.length(), i + 24);
      listener.onToken (SAMPLE_MARKDOWN.substring (i, end));
    } // for

    listener.onDone ();

  } // main

  ////////////////////////////////////////////////////////////

} // TerminalSink
