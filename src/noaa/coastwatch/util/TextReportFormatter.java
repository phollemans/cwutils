/*
 * CoastWatch Software Library and Utilities
 * Copyright (c) 2022 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.util;

import java.util.List;
import java.util.Map;

/**
 * The <code>TextReportFormatter</code> class formats report data to raw text.
 * 
 * @author Peter Hollemans
 * @since 3.7.1
 */
public class TextReportFormatter implements ReportFormatter {

  /** The document content. */
  private StringBuffer content;

  /////////////////////////////////////////////////////////////////

  protected TextReportFormatter () { content = new StringBuffer(); }

  /////////////////////////////////////////////////////////////////

  public static TextReportFormatter create () { return (new TextReportFormatter()); }

  /////////////////////////////////////////////////////////////////

  /**
   * Gets the text content.
   * 
   * @return the text content.
   */
  public String getContent () { return (content.toString()); }

  /////////////////////////////////////////////////////////////////

  @Override
  public void start() { }

  /////////////////////////////////////////////////////////////////

  @Override
  public void title (String text) { section (text); }

  /////////////////////////////////////////////////////////////////

  @Override
  public void section (String title) {

    int len = content.length();
    if (len >= 2 && content.lastIndexOf ("\n\n") != len-2)
      content.append ("\n");

    content.append (title + "\n");

  } // section

  /////////////////////////////////////////////////////////////////

  @Override
  public void paragraph (String text) {

    content.append (text + "\n\n");

  } // paragraph

  /////////////////////////////////////////////////////////////////

  @Override
  public void line (String text) {

    content.append (text + "\n");

  } // line

  /////////////////////////////////////////////////////////////////

  @Override
  public void table (String[] columns, List<String[]> rows) {

    int cols = columns.length;
    int[] columnSizes = new int[cols];
    for (int i = 0; i < cols; i++) {
      var index = i;
      columnSizes[i] = Math.max (columns[i].length(), rows.stream().mapToInt (row -> row[index].length()).max().orElse (0));
    } // for

    var buf = new StringBuffer();
    buf.append ("  ");
    for (int i = 0; i < cols; i++) buf.append ("%-" + columnSizes[i] + "s   ");
    buf.append ("\n");
    var fmt = buf.toString();

    content.append (String.format (fmt, (Object[]) columns));
    rows.forEach (row -> content.append (String.format (fmt, (Object[]) row)));

  } // table

  /////////////////////////////////////////////////////////////////

  @Override
  public void table (String[] columns, Map<String, List<String[]>> rowMap) {


    throw new UnsupportedOperationException();


  } // table

  /////////////////////////////////////////////////////////////////

  @Override
  public void map (Map<String, String> map) {

    int maxKey = map.keySet().stream().mapToInt (key -> key.length()).max().orElse (0);
    var fmt = "  %-" + (maxKey+1) + "s     %s\n";
    map.forEach ((key, value) -> content.append (String.format (fmt, key + ":", value)));

  } // map

  /////////////////////////////////////////////////////////////////

  @Override
  public void end() { }

  /////////////////////////////////////////////////////////////////

} // HTMLReportFormatter class
