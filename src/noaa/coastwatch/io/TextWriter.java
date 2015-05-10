////////////////////////////////////////////////////////////////////////
/*
     FILE: TextWriter.java
  PURPOSE: A class to write ASCII text format files.
   AUTHOR: Peter Hollemans
           Mark Robinson
     DATE: 2002/04/16
  CHANGES: 2002/07/26, MSR, added implementation
           2002/07/31, PFH, converted to location classes
           2002/08/25, PFH, rearranged
           2002/11/26, PFH, optimized value printing loop
           2003/06/10, PFH, fixed printing of NaN missing values
           2004/02/15, PFH, added super() call in constructor

  CoastWatch Software Library and Utilities
  Copyright 1998-2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.io;

// Imports
// -------
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.List;
import noaa.coastwatch.io.EarthDataWriter;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.EarthDataInfo;
import noaa.coastwatch.util.EarthLocation;
import noaa.coastwatch.util.trans.EarthTransform;

/**
 * A text writer is an Earth data writer that writes variable data
 * as a series of lines in an ASCII text file.  An optional dimension
 * header may be prepended to each variable.
 *
 * @author Peter Hollemans
 * @author Mark Robinson
 * @since 3.1.0
 */
public class TextWriter
  extends EarthDataWriter {

  // Constants
  // ---------
  /** Output buffer size in kilobytes. */
  public static final int DEFAULT_CHUNK_SIZE = 512;

  // Variables
  // ---------
  /** The coordinates printing flag. */
  private boolean coords;

  /** The coordinates reversed printing flag. */
  private boolean reverse;

  /** The string for delimiting data fields. */
  private String delimiter;

  /** The header flag. */
  private boolean header;

  /** The decimals places for each geographic coordinate value. */
  private int decimals;

  /** The missing data value. */
  private Number missing;

  /** The print output stream. */
  private PrintStream out;

  ////////////////////////////////////////////////////////////

  /**
   * Sets the decimal places.
   *
   * @param decimals the number of decimal places to use in coordinate
   * value printing.
   */
  public void setDecimals (int decimals) { this.decimals = decimals; }

  ////////////////////////////////////////////////////////////

  /** 
   * Sets coordinate printing.
   *
   * @param coords the coordinate flag.  If true, the Earth location
   * coordinates are printed along with each data value in the order
   * latitude, longitude.  Otherwise no coordinates are printed. 
   */
  public void setCoords (boolean coords) { this.coords = coords; }

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the reverse flag. If true, the order of latitude and
   * longitude printing is reversed.
   *
   * @param reverse the reverse flag.
   *
   * @see #setCoords
   */
  public void setReverse (boolean reverse) { this.reverse = reverse; }

  ////////////////////////////////////////////////////////////

  /**
   * Sets the field delimiter.
   *
   * @param delimiter the field delimiter.  The delimiter is used to
   * separate multiple data fields on one line.
   */
  public void setDelimiter (String delimiter) { this.delimiter = delimiter; }

  ////////////////////////////////////////////////////////////

  /**
   * Sets the header flag.
   *
   * @param header the header flag.  If true, a header line is printed
   * prior to any data values.  The header line consists of one
   * integer specifying the number of dimensions followed by a series
   * of integers specifying the dimension lengths.  If false, no header
   * line is printed.
   */
  public void setHeader (boolean header) { this.header = header; }

  ////////////////////////////////////////////////////////////

  /**
   * Sets the missing value.
   *
   * @param missing the missing value.  The missing value is used to
   * represent missing or out of range data values.  The missing value
   * is printed when such data is encountered in the variable data.  If the
   * missing value is null, the value <code>Double.NaN</code> is used.
   */
  public void setMissing (Number missing) { 

    this.missing = (missing == null ? new Double (Double.NaN) : missing);

  } // setNissing

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new text file from the specified Earth data info
   * and file name.  By default, each line contains the latitude,
   * longitude, and data value to 6 decimal places, separated by a
   * space with no header line.  The double value
   * <code>Double.NaN</code> is used for the missing value.
   *
   * @param info the Earth data info object.
   * @param file the new binary file name.
   *
   * @throws IOException if an error occurred opening the file.
   */
  public TextWriter (
    EarthDataInfo info,
    String file
  ) throws IOException {
    
    super (file);

    // Initialize variables
    // --------------------
    this.info = info;

    // Set defaults
    // -------------
    setDecimals (6);
    setCoords (true);
    setReverse (false);
    setDelimiter (" ");
    setHeader (false);
    setMissing (new Double (Double.NaN));

    // Create output file
    // ------------------
    out = new PrintStream (new BufferedOutputStream (
      new FileOutputStream (file), DEFAULT_CHUNK_SIZE*1024));

  } // TextWriter constructor

  ////////////////////////////////////////////////////////////

  /**
   * Writes a dimension header.  The header line consists of one
   * integer specifying the number of dimensions followed by a series
   * of integers specifying the dimension lengths.
   *
   * @param var the data variable to write a header for.
   *
   * @throws IOException if an error occurred writing the header data
   * to the file.
   */
  protected void writeHeader (
    DataVariable var
  ) throws IOException {

    // Write dimension rank
    // --------------------
    int[] dims = var.getDimensions();
    out.print (dims.length);
    out.print (delimiter);

    // Write dimension lengths
    // -----------------------
    for (int i = 0; i < dims.length; i++) {
      out.print (dims[i]);
      if (i != dims.length-1) out.print (delimiter);
    } // for
    out.println();

  } // writeHeader

  ////////////////////////////////////////////////////////////

  public void flush () throws IOException {

    // Check for canceled
    // ------------------
    if (isCanceled) return;

    // Initialize progress counters
    // ----------------------------
    synchronized (this) {
      writeProgress = 0;
      writeVariables = 0;
    } // synchronized

    // Loop over each variable
    // -----------------------
    while (variables.size() != 0) {

      // Write variable
      // --------------
      DataVariable var = (DataVariable) variables.remove (0);
      writeVariableName = var.getName();
      if (header) writeHeader (var);
      writeVariable (var);

      // Update progress
      // ---------------
      synchronized (this) {
        writeProgress = 0;
        writeVariables++;
      } // synchronized

      // Check for canceled
      // ------------------
      if (isCanceled) return;

    } // while

    // Flush output stream
    // -------------------
    out.flush();

  } // flush

  ////////////////////////////////////////////////////////////

  /**
   * Writes the variable data as a stream of text lines.
   *
   * @param var the data variable to write data for.
   *
   * @throws IOException if an error occurred writing the data
   * to the file.
   */
  protected void writeVariable (
    DataVariable var
  ) throws IOException {

    // Initialize variables
    // --------------------
    EarthTransform trans = info.getTransform();
    int[] dims = var.getDimensions();
    String formatPattern = "0.";
    for (int i = 0; i < decimals; i++) formatPattern += "#";
    DecimalFormat format = new DecimalFormat (formatPattern);
    double missingDouble = missing.doubleValue();
    String missingDoubleStr = (Double.isNaN (missingDouble) ? "NaN" :
      var.format (missingDouble)); 

    // Loop over each data value
    // -------------------------
    int values = var.getValues();
    for (int i = 0; i < values; i++) {

      // Print Earth location
      // --------------------
      DataLocation loc = new DataLocation (i, dims);
      if (coords) {
        EarthLocation geo = trans.transform (loc);
        double[] latlon = new double[] {
          (reverse ? geo.lon : geo.lat), (reverse ? geo.lat : geo.lon)};
        out.print (format.format (latlon[0]));
        out.print (delimiter);
        out.print (format.format (latlon[1]));
        out.print (delimiter);
      } // if

      // Print value
      // -----------
      double value = var.getValue (loc);
      if (Double.isNaN (value))
        out.print (missingDoubleStr);
      else
        out.print (var.format (value));
      out.println();

      // Set progress
      // ------------
      writeProgress = ((i+1)*100)/values;

      // Check for canceled
      // ------------------
      if (isCanceled) return;

    } // for

  } // writeVariable

  ////////////////////////////////////////////////////////////

  public void close () throws IOException {

    // Flush and close
    // ---------------
    flush();
    out.close();

  } // close

  ////////////////////////////////////////////////////////////

} // TextWriter class

////////////////////////////////////////////////////////////////////////
