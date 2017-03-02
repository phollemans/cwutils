////////////////////////////////////////////////////////////////////////
/*

     File: ArcWriter.java
   Author: Mark Robinson
     Date: 2002/07/15

  CoastWatch Software Library and Utilities
  Copyright (c) 2002 National Oceanic and Atmospheric Administration
  All rights reserved.

  Developed by: CoastWatch / OceanWatch
                Center for Satellite Applications and Research
                http://coastwatch.noaa.gov

  For conditions of distribution and use, see the accompanying
  license.txt file.

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.io;

// Imports
// -------
import java.awt.geom.AffineTransform;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import noaa.coastwatch.io.BinaryWriter;
import noaa.coastwatch.io.FloatWriter;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.EarthDataInfo;
import noaa.coastwatch.util.Grid;
import noaa.coastwatch.util.trans.EarthTransform;
import noaa.coastwatch.util.trans.MapProjection;

/**
 * An Arc writer is a float writer that writes a binary format file
 * for input to ArcView or ArcInfo as a binary grid.  An optional
 * header file may also be created to specify the earth location and
 * other parameters.  The Arc writer limits the functionality of the
 * float writer in order to write files compatible with Arc.  Only one
 * variable per grid file is allowed, and the variable must have two
 * dimensions and a map projection.
 *
 * @author Mark Robinson
 * @since 3.1.0
 */
public class ArcWriter
  extends FloatWriter {

  // Variables
  // ---------
  /** The output file name. */
  private String file;

  /** Flag to indicate that flushing has occurred. */
  private boolean flushed;

  ////////////////////////////////////////////////////////////

  /**
   * Adds a data variable to the writer.  This method
   * overrides the <code>EarthDataWriter</code> method so that
   * only one variable may be present in the variable list.  Note that
   * only variables of type <code>Grid</code> are accepted; other
   * types are silently ignored.
   *
   * @param var the data variable to add.
   * 
   * @see EarthDataWriter#addVariable
   */
  public void addVariable (
    DataVariable var
  ) {

    // Check for grid
    // --------------
    if (!(var instanceof Grid)) return;

    // Add variable
    // ------------
    variables.clear();
    variables.add (var);

  } // addVariable

  ////////////////////////////////////////////////////////////

  /**
   * Sets the byte order.  This method overrides the super class and
   * simply sets the byte order to <code>MSB</code>.
   */  
  public void setOrder (int order) { super.setOrder (MSB); }

  ////////////////////////////////////////////////////////////

  /**
   * Sets the missing value.  This method overrides the super class and
   * simply sets the missing value to <code>-3.4e38</code>.
   */
  public void setMissing (Number missing) { 

    super.setMissing (new Float (-3.4e38f));

  } // setMissing

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new binary file from the specified earth data info
   * and file name.
   *
   * @param info the earth data info object.
   * @param file the new binary file name.
   *
   * @throws IOException if an error occurred opening the file.
   *
   * @see FloatWriter#FloatWriter
   * @see BinaryWriter#BinaryWriter
   */
  public ArcWriter (
    EarthDataInfo info,
    String file
  ) throws IOException {

    // Initialize variables
    // --------------------
    super (info, file);
    this.file = file;
    flushed = false;

    // Check for map projection
    // ------------------------
    EarthTransform trans = info.getTransform();
    if (!(trans instanceof MapProjection))
      throw new IOException ("Earth transform must be a map projection");

  } // ArcWriter constructor

  ////////////////////////////////////////////////////////////

  /**
   * Flushes all unwritten data to the destination.  This method
   * overrides the <code>BinaryWriter</code> method so that only one
   * variable is ever actually flushed to the destination.
   */
  public void flush () throws IOException {

    // Flush if not yet flushed
    // ------------------------
    if (!flushed) {
      super.flush ();
      flushed = true;
    } // if

    // Throw an error if already flushed
    // ---------------------------------
    else if (flushed && variables.size() != 0) {
      variables.clear();
      throw new IOException ("Only one variable allowed");
    } // else if
  
  } // flush

  ////////////////////////////////////////////////////////////

  /**
   * Writes an Arc header file.  The header is a text file with a
   * number of lines as follows:
   * <pre>
   *   nrows (number of data rows)
   *   ncols (number of data columns)
   *   xllcorner (lower left corner x coordinate in map coordinates)
   *   yllcorner (lower left corner y coordinate in map coordinates)
   *   cellsize (size of each grid cell in map coordinates)
   *   nodata_value (missing data value)
   *   byteorder (byte order as "LSBFIRST" or "MSBFIRST")
   *   nbits 32
   * </pre>
   * The name of the header file is derived from the output file name by
   * replacing any '.' followed by an extension with '.hdr'.
   *
   * @param var the data variable to write a header for.
   *
   * @throws IOException if an error occurred writing the header data
   * to the file.
   */
  protected void writeHeader (
    DataVariable var
  ) throws IOException {

    // Create header file
    // ------------------
    String headerFile = file;
    int dotIndex = headerFile.lastIndexOf ('.');
    if (dotIndex != -1) headerFile = headerFile.substring (0, dotIndex);
    headerFile = headerFile + ".hdr";
    PrintStream head = new PrintStream (new FileOutputStream (headerFile));

    // Write dimensions
    // ----------------
    int dims[] = var.getDimensions();
    head.println ("nrows " + dims[Grid.ROWS]);
    head.println ("ncols " + dims[Grid.COLS]);

    // Write lower-left corner
    // -----------------------
    double data[] = new double[] {dims[Grid.ROWS] - 0.5, -0.5};
    double map[] = new double[2];
    AffineTransform affine = ((MapProjection)info.getTransform()).getAffine();
    affine.transform (data, 0, map, 0, 1);
    head.println ("xllcorner " + map[0]); 
    head.println ("yllcorner " + map[1]); 
   
    // Write cell size
    // ---------------
    double matrix[] = new double[6];
    affine.getMatrix (matrix);
    double cellsize = Math.sqrt (matrix[0]*matrix[0] + matrix[1]*matrix[1]);
    head.println ("cellsize " + cellsize);

    // Write missing value
    // -------------------
    head.println ("nodata_value " + -3.4e38f);

    // Write byte order and bit count
    // ------------------------------
    head.println ("byteorder MSBFIRST");
    head.println ("nbits 32");

  } // writeHeader

  ////////////////////////////////////////////////////////////

} // ArcWriter class

////////////////////////////////////////////////////////////////////////

