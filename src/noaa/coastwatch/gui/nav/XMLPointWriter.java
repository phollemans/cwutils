////////////////////////////////////////////////////////////////////////
/*
     FILE: XMLPointWriter.java
  PURPOSE: Writes navigation point data to XML.
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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import noaa.coastwatch.gui.nav.NavigationPoint;
import noaa.coastwatch.gui.nav.NavigationPointWriter;
import noaa.coastwatch.io.EarthDataReader;
import noaa.coastwatch.io.EarthDataReaderFactory;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.EarthDataInfo;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.Grid;

/**
 * The <code>XMLPointWriter</code> class writes navigation point data
 * to an XML format file.  Each navigation point is written as an XML
 * <code>&lt;point&gt;</code> element with attributes and subelements
 * specifying the data.
 *
 * @author Peter Hollemans
 * @since 3.2.2
 */
public class XMLPointWriter 
  extends NavigationPointWriter {

  // Constants
  // ---------

  /** The schema instance for the document. */
  private static final String XSI = 
    "http://www.w3.org/2001/XMLSchema-instance";

  /** The namespace for the document. */
  private static final String XMLNS = "http://coastwatch.noaa.gov";

  /** The schema location for the document. */
  private static final String SCHEMA = 
    "http://coastwatch.noaa.gov http://coastwatch.noaa.gov/xml/pointlist.xsd";

  ////////////////////////////////////////////////////////////

  /**
   * Creates an XML point writer.
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
  public XMLPointWriter (
    EarthDataReader reader,
    List<String> variableList,
    List<NavigationPoint> pointList
  ) throws IOException {

    super (reader, variableList, pointList);

  } // XMLPointWriter constructor

  ////////////////////////////////////////////////////////////

  protected void writeHeader (
    PrintStream stream
  ) {

    stream.println ("<?xml version=\"1.0\"?>\n");
    stream.println ("<pointList");
    stream.println ("  xmlns=\"" + XMLNS + "\"");
    stream.println ("  xmlns:xsi=\"" + XSI + "\"");
    stream.println ("  xsi:schemaLocation=\"" + SCHEMA + "\">\n");

    stream.println ("  <dataset \n" +
      "    name=\"" + new File (reader.getSource()).getName() + "\"\n" +
      "    format=\"" + reader.getDataFormat() + "\"/>\n");

  } // writeHeader

  ////////////////////////////////////////////////////////////

  protected void writePoint (
    PrintStream stream,
    NavigationPoint point
  ) {

    // Start point tag
    // ---------------
    stream.println ("  <point>");
    
    // Write spatial coordinate data
    // -----------------------------
    EarthLocation earthLoc = point.getEarthLoc();
    stream.println ("    <earthLoc " +
      "lat=\"" + earthLoc.lat + "\" " +
      "lon=\"" + earthLoc.lon + "\"/>");
    DataLocation dataLoc = point.getDataLoc();
    stream.println ("    <dataLoc " +
      "row=\"" + dataLoc.get (Grid.ROWS) + "\" " +
      "col=\"" + dataLoc.get (Grid.COLS) + "\"/>");

    // Write orientation data
    // ----------------------
    double[] north = getNorth (dataLoc);
    stream.println ("    <northDir " +
      "row=\"" + north[0] + "\" " +
      "col=\"" + north[1] + "\"/>");

    // Write navigation offset data
    // ----------------------------
    double[] offset = point.getOffset();
    stream.println ("    <navOffset " +
      "row=\"" + offset[Grid.ROWS] + "\" " +
      "col=\"" + offset[Grid.COLS] + "\"/>");

    // Write comment
    // -------------
    stream.println ("    <comment " +
      "value=\"" + point.getComment() + "\"/>");

    // Write variable values
    // ---------------------
    for (String varName : variableList) {
      Grid grid = gridCache.get (varName);
      double value = grid.getValue (dataLoc);
      String valueStr = Double.isNaN (value) ? "NaN" : grid.format (value);
      stream.println ("    <varValue " +
        "name=\"" + varName + "\" " +
        "value=\"" + valueStr + "\" " +
        "units=\"" + grid.getUnits() + "\"/>");
    } // for

    // End point tag
    // -------------
    stream.println ("  </point>\n");

  } // writePoint

  ////////////////////////////////////////////////////////////

  protected void writeFooter (
    PrintStream stream
  ) {

    stream.println ("</pointList>");

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
    NavigationPointWriter writer = new XMLPointWriter (reader, variableList,
      pointList);

    // Write points
    // ------------
    writer.write (new FileOutputStream (argv[0] + ".xml"));

  } // main

  ////////////////////////////////////////////////////////////

} // XMLPointWriter class

////////////////////////////////////////////////////////////////////////


