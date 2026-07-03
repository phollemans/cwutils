/*
 * CoastWatch Software Library and Utilities
 * Copyright (c) 2026 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.helpagent.markdown;

/**
 * The <code>MarkdownSink</code> interface receives Markdown rendering
 * events from a renderer.  A sink implementation translates the event stream
 * into a specific output format.
 *
 * @author Peter Hollemans
 * @since 4.2.0
 */
public interface MarkdownSink {

  /** Starts a rendered document. */
  void startDocument ();

  /** Ends a rendered document. */
  void endDocument ();

  /** Starts a paragraph. */
  void startParagraph ();

  /** Ends a paragraph. */
  void endParagraph ();

  /** 
   * Starts a heading.
   *
   * @param level the heading level
   */
  void startHeading (
    int level
  );

  /** 
   * Ends a heading.
   *
   * @param level the heading level
   */
  void endHeading (
    int level
  );

  /** Starts emphasized text. */
  void startEmphasis ();

  /** Ends emphasized text. */
  void endEmphasis ();

  /** Starts strong text. */
  void startStrong ();

  /** Ends strong text. */
  void endStrong ();

  /** 
   * Writes plain text.
   *
   * @param text the text to write
   */
  void text (
    String text
  );

  /**
   * Writes inline code text.
   *
   * @param code the inline code text
   */
  void inlineCode (
    String code
  );

  /** Writes a line break. */
  void lineBreak ();

  /** Starts a bullet list. */
  void startBulletList ();

  /** Ends a bullet list. */
  void endBulletList ();

  /**
   * Starts an ordered list.
   *
   * @param startNumber the starting item number
   */
  void startOrderedList (
    int startNumber
  );

  /** Ends an ordered list. */
  void endOrderedList ();

  /** Starts a bullet list item. */
  void startBulletListItem ();

  /** Ends a bullet list item. */
  void endBulletListItem ();

  /**
   * Starts an ordered list item.
   *
   * @param index the rendered item number
   */
  void startOrderedListItem (
    int index
  );

  /** Ends an ordered list item. */
  void endOrderedListItem ();

  /**
   * Starts a code block.
   *
   * @param info the code block information string
   */
  void startCodeBlock (
    String info
  );

  /**
   * Writes code block text.
   *
   * @param text the code block text
   */
  void codeBlockText (
    String text
  );

  /** Ends a code block. */
  void endCodeBlock ();

  /** Writes a thematic break. */
  void thematicBreak ();

  /** Starts a table. */
  void startTable ();

  /** Ends a table. */
  void endTable ();

  /** Starts a table header. */
  void startTableHeader ();

  /** Ends a table header. */
  void endTableHeader ();

  /** Starts a table body. */
  void startTableBody ();

  /** Ends a table body. */
  void endTableBody ();

  /** Starts a table separator. */
  void startTableSeparator ();

  /** Ends a table separator. */
  void endTableSeparator ();

  /** Starts a table row. */
  void startTableRow ();

  /** Ends a table row. */
  void endTableRow ();

  /**
   * Starts a table cell.
   *
   * @param header true if the cell is in a table header
   */
  void startTableCell (boolean header);

  /** Ends a table cell. */
  void endTableCell ();

  /**
   * Writes an error message.
   *
   * @param text the error text
   */
  void error (String text);

} // MarkdownSink
