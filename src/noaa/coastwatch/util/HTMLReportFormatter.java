/*
 * CoastWatch Software Library and Utilities
 * Copyright (c) 2022 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.util;

import java.util.List;
import java.util.Map;
import java.awt.Color;

/**
 * The <code>HTMLReportFormatter</code> class formats report data to HTML 3.2.
 * 
 * @author Peter Hollemans
 * @since 3.7.1
 */
public class HTMLReportFormatter implements ReportFormatter {

  /** The document content. */
  private StringBuffer content;

  /** The color of table headers and borders. */
  private Color border;

  /** The table spacing value. */
  private int spacing;

  /////////////////////////////////////////////////////////////////

  protected HTMLReportFormatter () { 

    content = new StringBuffer(); 
    border = Color.BLACK; 
    spacing = 4;

  } // HTMLReportFormatter

  /////////////////////////////////////////////////////////////////

  public static HTMLReportFormatter create () { return (new HTMLReportFormatter()); }

  /////////////////////////////////////////////////////////////////

  /**
   * Sets the HTML table border and header colour (default black).
   * 
   * @param border the new border/header colour.
   */
  public void setBorderColor (Color border) { this.border = border; }

  /////////////////////////////////////////////////////////////////

  /**
   * Sets the number of characters of space between table columns (default 4).
   * 
   * @param spacing the new table spacing in characters.
   */
  public void setSpacing (int spacing) { this.spacing = spacing; }

  /////////////////////////////////////////////////////////////////

  /**
   * Gets the HTML content.
   * 
   * @return the HTML content text.
   */
  public String getContent () { return (content.toString()); }

  /////////////////////////////////////////////////////////////////

  private String getSpaces (int n) {

    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < n; i++) buf.append ("&nbsp;");
    return (buf.toString());

  } // getSpaces

  /////////////////////////////////////////////////////////////////

  @Override
  public void start() {

    content.append ("<html>\n");
    content.append ("<head>\n");
    content.append ("<style>\n");
    var header = "#" + Integer.toHexString (border.getRGB() & 0x00ffffff);
    content.append ("th.columns { background: " + header + "; }");
    content.append ("th, td { text-align: left; }");
    content.append ("td { border: 1px solid " + header + "; }");

// Note: If we want to have different font sizes, we can use:
//   font-size: smaller;
//   font-size: larger;

    content.append ("td.clean { border: none; }");
    content.append ("td.clean_nowrap { border: none; white-space: nowrap; }");
    content.append ("</style>\n");
    content.append ("</head>\n");
    content.append ("<body>\n");

  } // start

  /////////////////////////////////////////////////////////////////

  @Override
  public void title (String text) {

    content.append ("<h3>" + text + "</h3>\n");

  } // title

  /////////////////////////////////////////////////////////////////

  @Override
  public void section (String title) {

    content.append ("<h3>" + title + "</h3>\n");

  } // section

  /////////////////////////////////////////////////////////////////

  @Override
  public void paragraph (String text) {

    content.append ("<p>" + text + "</p>\n");

  } // paragraph

  /////////////////////////////////////////////////////////////////

  @Override
  public void line (String text) {

    content.append (text + "<br/>\n");

  } // line

  /////////////////////////////////////////////////////////////////

  @Override
  public void table (String[] columns, List<String[]> rows) {

    content.append ("<table cellpadding=\"2\" cellspacing=\"0\">\n");

    int cols = columns.length;
    content.append ("<tr valign=\"top\">\n");
    for (int i = 0; i < cols; i++) {
      content.append ("<th class=\"columns\">" + columns[i] + getSpaces (i == cols-1 ? 0 : spacing) + "</th>\n");
    } // for
    content.append ("</tr>\n");

    rows.forEach (row -> {
      content.append ("<tr valign=\"top\">\n");
      for (int i = 0; i < cols; i++) 
        content.append ("<td>" + row[i] + getSpaces (i == cols-1 ? 0 : spacing) + "</td>\n");
      content.append ("</tr>\n");
    });

    content.append ("</table>\n");

  } // table

  /////////////////////////////////////////////////////////////////

  @Override
  public void table (String[] columns, Map<String, List<String[]>> rowMap) {

    int cols = columns.length;

    content.append ("<table cellpadding=\"2\" cellspacing=\"0\">\n");

    rowMap.forEach ((section, rows) -> {

      content.append ("<tr>\n");
      content.append ("<th colspan=\"" + cols + "\"><h3>" + section + ":</h3></th>\n");
      content.append ("</tr>\n");

      content.append ("<tr>\n");
      for (int i = 0; i < cols; i++) 
        content.append ("<th class=\"columns\">" + columns[i] + getSpaces (i == cols-1 ? 0 : spacing) + "</th>\n");
      content.append ("</tr>\n");

      rows.forEach (row -> {
        content.append ("<tr valign=\"top\">\n");
        for (int i = 0; i < cols; i++) 
          content.append ("<td>" + row[i] + getSpaces (i == cols-1 ? 0 : spacing) + "</td>\n");
        content.append ("</tr>\n");
      });

    });

    content.append ("</table>\n");

  } // table

  /////////////////////////////////////////////////////////////////

  @Override
  public void map (Map<String, String> map) {

    content.append ("<table cellpadding=\"2\" cellspacing=\"0\">\n");

    map.forEach ((key, value) -> {
      content.append ("<tr valign=\"top\">\n");
      content.append ("<td class=\"clean_nowrap\">" + key + (key.isBlank() ? "" : ":") + getSpaces (spacing) + "</td>\n");
      content.append ("<td class=\"clean\">" + value + "</td>\n");
      content.append ("</tr>\n");
    });

    content.append ("</table>\n");

  } // map

  /////////////////////////////////////////////////////////////////

  @Override
  public void end() {

    line ("");
    content.append ("</body>\n");
    content.append ("</html>\n");

  } // end

  /////////////////////////////////////////////////////////////////

} // HTMLReportFormatter class
