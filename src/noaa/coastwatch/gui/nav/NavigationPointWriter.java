////////////////////////////////////////////////////////////////////////
/*
     FILE: NavigationPointWriter.java
  PURPOSE: Writes navigation point data.
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
import noaa.coastwatch.util.trans.*;

/**
 * The <code>NavigationPointWriter</code> class is an abstract class 
 * for writing out data from a list of navigation points.
 *
 * @author Peter Hollemans
 * @since 3.2.2
 */
public abstract class NavigationPointWriter {

  // Variables
  // ---------

  /** The reader to get extra data from. */
  protected EarthDataReader reader;

  /** The list of variables to access in the reader. */
  protected List<String> variableList;

  /** The list of navigation points to write. */
  private List<NavigationPoint> pointList;

  /** The cache of grid variables. */
  protected Map<String,Grid> gridCache;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a point writer.
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
  protected NavigationPointWriter (
    EarthDataReader reader,
    List<String> variableList,
    List<NavigationPoint> pointList
  ) throws IOException {

    // Initialize variables
    // --------------------
    this.reader = reader;
    this.variableList = variableList;
    this.pointList = pointList;

    // Create grid cache
    // -----------------
    gridCache = new HashMap<String,Grid>();
    for (String varName : variableList)
      gridCache.put (varName, (Grid) reader.getVariable (varName));

  } // NavigationPointWriter constructor

  ////////////////////////////////////////////////////////////

  /**
   * Gets the data location north vector.
   *
   * @param dataLoc the data location to query.
   *
   * @return the unit vector in the direction of north as [row, col].
   */
  public double[] getNorth (
    DataLocation dataLoc
  ) {

    // Compute difference points
    // -------------------------
    EarthTransform trans = reader.getInfo().getTransform();
    int[] dims = trans.getDimensions();
    DataLocation r1 = dataLoc.translate (-1, 0).truncate (dims);
    DataLocation r2 = dataLoc.translate (+1, 0).truncate (dims);
    double dr = r2.get (Grid.ROWS) - r1.get (Grid.ROWS);
    DataLocation c1 = dataLoc.translate (0, -1).truncate (dims);
    DataLocation c2 = dataLoc.translate (0, +1).truncate (dims);
    double dc = c2.get (Grid.COLS) - c1.get (Grid.COLS);

    // Compute latitude differences
    // ----------------------------
    double latr1 = trans.transform (r1).lat;
    double latr2 = trans.transform (r2).lat;
    double dlatr = latr2 - latr1;
    double latc1 = trans.transform (c1).lat;
    double latc2 = trans.transform (c2).lat;
    double dlatc = latc2 - latc1;

    // Compute north vector
    // --------------------
    double[] north = new double[] {
      dlatr/dr, dlatc/dc
    };
    double mag = Math.sqrt (north[0]*north[0] + north[1]*north[1]);
    north[0] /= mag;
    north[1] /= mag;

    return (north);

  } // getNorth

  ////////////////////////////////////////////////////////////

  /**
   * Writes the file header to the stream.
   *
   * @param stream the print stream to write to.
   */
  protected abstract void writeHeader (
    PrintStream stream
  );

  ////////////////////////////////////////////////////////////

  /**
   * Writes a single point to the stream.
   *
   * @param stream the print stream to write to.
   * @param point the navigation point to write.
   */
  protected abstract void writePoint (
    PrintStream stream,
    NavigationPoint point
  );

  ////////////////////////////////////////////////////////////

  /**
   * Writes the file footer to the stream.
   *
   * @param stream the print stream to write to.
   */
  protected abstract void writeFooter (
    PrintStream stream
  );

  ////////////////////////////////////////////////////////////

  /**
   * Writes the point data.
   *
   * @param stream the output stream to write to.
   */
  public void write (
    OutputStream stream
  ) {

    // Create print stream
    // -------------------
    PrintStream print = new PrintStream (stream);

    // Write data
    // ----------
    writeHeader (print);
    for (NavigationPoint point : pointList)
      writePoint (print, point);
    writeFooter (print);

  } // write

  ////////////////////////////////////////////////////////////

  /** 
   * Provides test data for this class.
   * 
   * @param trans the earth transform to generate test data for.
   * @param count the number of test data points desired.
   *
   * @return the list of test data points.  Each point will has a
   * randomly generated earth location and navigation offset.  The
   * comments in the point summarize the point data.
   */
  protected static List<NavigationPoint> getTestPoints (
    EarthTransform trans,
    int count
  ) {

    List<NavigationPoint> pointList = new ArrayList<NavigationPoint>();
    int[] dims = trans.getDimensions();
    for (int i = 0; i < 20; i++) {

      // Create random navigation point
      // ------------------------------
      DataLocation dataLoc = new DataLocation (
        Math.random()*(dims[Grid.ROWS]-1), Math.random()*(dims[Grid.COLS]-1));
      EarthLocation earthLoc = trans.transform (dataLoc);
      NavigationPoint point = new NavigationPoint (earthLoc, dataLoc);

      // Set offset to 1/10th of a pixel
      // -------------------------------
      double[] offset = new double[] {
        (Math.random() < 0.5 ? -1 : 1)*Math.random()*2,
        (Math.random() < 0.5 ? -1 : 1)*Math.random()*2
      };
      offset[0] = Math.rint (offset[0]*10)/10;
      offset[1] = Math.rint (offset[1]*10)/10;
      point.setOffset (offset);

      // Set the comment to reflect the data values
      // ------------------------------------------
      point.setComment ("earthLoc=" + earthLoc + ",dataLoc=" + dataLoc +
        ",offset=[" + offset[0] + "," + offset[1] + "]");

      // Add the point to the list
      // -------------------------
      pointList.add (point);

    } // for

    return (pointList);

  } // getTestPoints

  ////////////////////////////////////////////////////////////

} // NavigationPointWriter class

////////////////////////////////////////////////////////////////////////


