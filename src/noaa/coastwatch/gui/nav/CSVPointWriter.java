////////////////////////////////////////////////////////////////////////
/*
     FILE: CSVPointWriter.java
  PURPOSE: Writes navigation point data to CSV.
   AUTHOR: Peter Hollemans
     DATE: 2006/12/15
  CHANGES: n/a

  CoastWatch Software Library and Utilities
  Copyright 1998-2006, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.gui.nav;

// Imports
// -------
import java.io.*;
import java.util.*;
import noaa.coastwatch.io.*;
import noaa.coastwatch.util.*;

/**
 * The <code>CSVPointWriter</code> class writes navigation point data
 * to an CSV format file.  Each navigation point is written as one
 * line with commas separating the columns.  The file is prefixed with a
 * column name header.
 *
 * @author Peter Hollemans
 * @since 3.2.2
 */
public class CSVPointWriter
  extends NavigationPointWriter {

  ////////////////////////////////////////////////////////////

  /**
   * Creates an CSV point writer.
   * 
   * @param reader the reader to use for extra data to write with each
   * point.
   * @param variableList the list of variables from the reader to use
   * for writing variable data at each point.
   * @param pointList the list of navigation points to write.
   *
   * @throws IOException if an error occurred accessing a data
   * variable from the list.
   */
  public CSVPointWriter (
    EarthDataReader reader,
    List<String> variableList,
    List<NavigationPoint> pointList
  ) throws IOException {

    super (reader, variableList, pointList);

  } // CSVPointWriter constructor

  ////////////////////////////////////////////////////////////

  protected void writeHeader (
    PrintStream stream
  ) {

    stream.print (
      "\"LAT\"," +
      "\"LON\"," +
      "\"ROW\"," +
      "\"COL\"," +
      "\"ROW_NORTH\"," +
      "\"COL_NORTH\"," +
      "\"ROW_OFFSET\"," +
      "\"COL_OFFSET\"," +
      "\"COMMENT\"");
    for (String varName : variableList)
      stream.print (",\"" + varName.toUpperCase() + "\"");
    stream.println();

  } // writeHeader

  ////////////////////////////////////////////////////////////

  protected void writePoint (
    PrintStream stream,
    NavigationPoint point
  ) {

    // Write spatial coordinate data
    // -----------------------------
    EarthLocation earthLoc = point.getEarthLoc();
    stream.print (
      earthLoc.lat + "," +
      earthLoc.lon + ",");
    DataLocation dataLoc = point.getDataLoc();
    stream.print (
      dataLoc.get (Grid.ROWS) + "," +
      dataLoc.get (Grid.COLS) + ",");

    // Write orientation data
    // ----------------------
    double[] north = getNorth (dataLoc);
    stream.print (
      north[0] + "," +
      north[1] + ",");

    // Write navigation offset data
    // ----------------------------
    double[] offset = point.getOffset();
    stream.print (
      offset[Grid.ROWS] + "," +
      offset[Grid.COLS] + ",");

    // Write comment
    // -------------
    stream.print ("\"" + point.getComment() + "\"");

    // Write variable values
    // ---------------------
    for (String varName : variableList) {
      Grid grid = gridCache.get (varName);
      double value = grid.getValue (dataLoc);
      String valueStr = Double.isNaN (value) ? "\"NaN\"" : grid.format (value);
      stream.print ("," + valueStr);
    } // for

    stream.println();

  } // writePoint

  ////////////////////////////////////////////////////////////

  protected void writeFooter (
    PrintStream stream
  ) {
    
    // No footer for a CSV file

  } // writeFooter

  ////////////////////////////////////////////////////////////

  /** 
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) throws Exception {

    // Get reader
    // ----------
    EarthDataReader reader = EarthDataReaderFactory.create (argv[0]); 
    List<String> variableList = reader.getAllGrids();

    // Create writer
    // -------------
    List<NavigationPoint> pointList = NavigationPointWriter.getTestPoints (
      reader.getInfo().getTransform(), 20);
    for (NavigationPoint point : pointList) point.setComment ("Manual");
    NavigationPointWriter writer = new CSVPointWriter (reader, variableList,
      pointList);

    // Write points
    // ------------
    writer.write (new FileOutputStream (argv[0] + ".csv"));

  } // main

  ////////////////////////////////////////////////////////////

} // CSVPointWriter class

////////////////////////////////////////////////////////////////////////


