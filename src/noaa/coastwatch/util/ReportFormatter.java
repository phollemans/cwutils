/*
 * CoastWatch Software Library and Utilities
 * Copyright (c) 2022 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.util;

import java.util.List;
import java.util.Map;

/**
 * The <code>ReportFormatter</code> class is an interface for formatting 
 * reports of text data.
 * 
 * @author Peter Hollemans
 * @since 3.7.1
 */
public interface ReportFormatter {

  /** Starts the report. */
  void start();

  /** 
   * Formats a report title.
   * 
   * @param text the title of the report.
   */
  void title (String text);

  /** 
   * Formats a section header.
   * 
   * @param title the title of the section.
   */
  void section (String title);

  /** 
   * Formats a paragraph.
   * 
   * @param text the text of the paragraph.
   */
  void paragraph (String text);

  /** 
   * Formats a single line.
   * 
   * @param text the text of the line.
   */
  void line (String text);

  /**
   * Formats a table.
   * 
   * @param columns the column names.
   * @param rows the list of row data values.
   */
  void table (String[] columns, List<String[]> rows);

  /**
   * Formats a table with multiple sections.
   * 
   * @param columns the column names.
   * @param rowMap the map of section names to row data values.
   */
  void table (String[] columns, Map<String, List<String[]>> rowMap);

  /**
   * Formats a map of keys to values as a table with two columns.
   * 
   * @param map the map of keys to values.
   */
  void map (Map<String, String> map);

  /** Ends the report. */
  void end();

} // ReportFormatter class
