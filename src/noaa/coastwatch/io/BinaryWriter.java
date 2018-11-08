////////////////////////////////////////////////////////////////////////
/*

     File: BinaryWriter.java
   Author: Peter Hollemans
     Date: 2002/07/18

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
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.List;
import noaa.coastwatch.io.EarthDataWriter;
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.DataVariable;
import noaa.coastwatch.util.EarthDataInfo;

/**
 * A binary writer is an earth data writer that writes variable
 * data as a stream of binary values.  The data may be scaled and/or
 * byte swapped prior to writing.  An optional dimension header may be
 * prepended to each variable.
 *
 * @author Mark Robinson
 * @since 3.1.0
 */
abstract public class BinaryWriter
  extends EarthDataWriter {

  // Constants
  // ---------
  /** Host byte order. */
  public static final int HOST = 0;

  /** Most significant byte first. */
  public static final int MSB = 1;

  /** Least significant byte first. */
  public static final int LSB = 2;

  /** Output buffer size in kilobytes. */
  public static final int DEFAULT_CHUNK_SIZE = 512;

  // Variables
  // ---------
  /** The scaling as [factor, offset]. */
  private double[] scaling;

  /** The missing data value. */
  private Number missing;

  /** The output byte order. */
  private int order;

  /** The header flag. */
  private boolean header;

  /** The data output stream. */
  private DataOutputStream out;

  ////////////////////////////////////////////////////////////

  /**
   * Sets the header flag.
   *
   * @param header the header flag.  If true, a dimension header is
   * written before any data.
   */
  public void setHeader (boolean header) { this.header = header; }

  ////////////////////////////////////////////////////////////

  /**
   * Sets the byte order.
   *
   * @param order the byte order.  The options are <code>HOST</code>
   * for host byte order, <code>MSB</code> for most significant byte
   * first and <code>LSB</code> for least significant byte first.
   */
  public void setOrder (int order) {

    if (order == HOST) {
      if (ByteOrder.nativeOrder().equals (ByteOrder.LITTLE_ENDIAN))
        this.order = LSB;
      else
        this.order = MSB;
    } // if
    else
      this.order = order;

  } // setOrder

  ////////////////////////////////////////////////////////////

  /**
   * Sets the scaling range.  The range is used for data scaling to map
   * data values to integers using the equation:
   * <pre>
   *   integer = type_min + type_range*((value - min) / (max - min))
   * </pre>
   * where <code>type_min</code> and <code>type_range</code> are
   * determined by the implementing class.  Any data values outside
   * the specified range are mapped to the missing value.
   *
   * @param min the minimum data value.
   * @param max the maximum data value.
   */
  public void setRange (
    double min, 
    double max
  ) {

    scaling = new double[2];
    scaling[0] = (max - min) / getTypeRange();
    scaling[1] = getTypeMin() - getTypeRange()*min/(max - min);

  } // setRange

  ////////////////////////////////////////////////////////////

  /**
   * Sets the scaling factor and offset.  The scaling is used 
   * map data values to integers using the equation:
   * <pre>
   *   integer = value/factor + offset
   * </pre>
   *
   * @param scaling the scaling as [factor, offset].  If null, [1,0]
   * is used.
   */
  public void setScaling (double[] scaling) {

    if (scaling == null) this.scaling = new double[] {1, 0};
    else this.scaling = (double[]) scaling.clone();

  } // setScaling

  ////////////////////////////////////////////////////////////

  /**
   * Sets the missing value.
   *
   * @param missing the missing value.  The missing value is used to
   * represent missing or out of range data.  If null, the missing
   * value is set to the default.
   */
  public void setMissing (Number missing) {

    if (missing == null) this.missing = getDefaultMissing();
    else this.missing = missing;

  } // setMissing

  ////////////////////////////////////////////////////////////

  /** Performs a byte swap on a byte array. */
  public static byte[] byteSwap (
    byte[] array
  ) {

    byte[] swapped = new byte[array.length];
    for (int i = 0; i < array.length; i++)
      swapped[i] = array[(array.length-1)-i];
    return (swapped);

  } // byteSwap

  ////////////////////////////////////////////////////////////

  /** Gets the bytes representing an integer in MSB order. */
  public static byte[] getBytes (
    int value
  ) {

    byte[] array = new byte[4];
    array[0] = (byte) ((value & 0xff000000) >>> 24);
    array[1] = (byte) ((value & 0x00ff0000) >>> 16);
    array[2] = (byte) ((value & 0x0000ff00) >>> 8);
    array[3] = (byte) (value & 0x000000ff);
    return (array);

  } // getBytes

  ////////////////////////////////////////////////////////////

  /** Gets the bytes representing a short integer in MSB order. */
  public static byte[] getBytes (
    short value
  ) {

    byte[] array = new byte[2];
    array[0] = (byte) ((value & 0xff00) >>> 8);
    array[1] = (byte) (value & 0x00ff);
    return (array);

  } // getBytes

  ////////////////////////////////////////////////////////////

  /**
   * Writes a dimension header.  The header consists of one byte
   * specifying the number of dimensions followed by a series of
   * 32-bit signed integers specifying the dimension lengths.
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
    out.writeByte (dims.length);

    // Write dimension lengths
    // -----------------------
    for (int i = 0; i < dims.length; i++) {
      byte[] length = getBytes (dims[i]);
      if (order == LSB) length = byteSwap (length);
      out.write (length);
    } // for

  } // writeHeader

  ////////////////////////////////////////////////////////////

  /**
   * Converts a data value to a byte array.  The byte array has a
   * length appropriate for the subclass.  The value must be in the
   * range <code>getTypeMin()</code> to <code>getTypeMax()</code>.
   *
   * @param value the value for conversion.
   *
   * @return an array of bytes.
   */
  public abstract byte[] convertValue (
    Number value
  );

  ////////////////////////////////////////////////////////////

  /** Gets the type minimum as a double. */
  public abstract double getTypeMin ();

  ////////////////////////////////////////////////////////////

  /** Gets the type maximum as a double. */
  public abstract double getTypeMax ();

  ////////////////////////////////////////////////////////////

  /** Gets the type range as a double. */
  public abstract double getTypeRange ();

  ////////////////////////////////////////////////////////////

  /** Gets the default missing value. */
  public abstract Number getDefaultMissing ();

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new binary file from the specified earth data info
   * and file name.  By default the byte order is <code>HOST</code>,
   * scaling is unity, the missing value is set to the default, and
   * the header flag is false.
   *
   * @param info the earth data info object.
   * @param file the new binary file name.
   *
   * @throws IOException if an error occurred opening the file.
   */
  public BinaryWriter (
    EarthDataInfo info,
    String file
  ) throws IOException {

    super (file);

    // Initialize variables
    // --------------------
    this.info = info;

    // Set defaults
    // -------------
    setOrder (HOST);
    setScaling (null);
    setMissing (null);
    setHeader (false);

    // Create output file
    // ------------------
    out = new DataOutputStream (new BufferedOutputStream (
      new FileOutputStream (file), DEFAULT_CHUNK_SIZE*1024));

  } // BinaryWriter

  ////////////////////////////////////////////////////////////

  /**
   * Writes the variable data as a stream of binary data values.
   *
   * @param var the data variable to write data for.
   *
   * @throws IOException if an error occurred writing the data
   * to the file.
   */
  protected void writeVariable (
    DataVariable var
  ) throws IOException {

    // Get predefined values
    // ---------------------
    byte[] missingBytes = convertValue (missing);
    double min = getTypeMin();
    double max = getTypeMax();
    int[] dims = var.getDimensions();

    // Loop over each data value
    // -------------------------
    int values = var.getValues();
    for (int i = 0; i < values; i++) {

      // Scale value
      // -----------
      DataLocation loc = new DataLocation (i, dims);
      double value = var.getValue(loc);
      if (!Double.isNaN (value)) {
        value = value/scaling[0] + scaling[1];
        if (value < min || value > max) value = Double.NaN;
      } // if      

      // Write data
      // ----------
      byte[] array;
      if (Double.isNaN (value)) array = missingBytes;
      else array = convertValue (Double.valueOf (value));
      if (order == LSB) array = byteSwap (array);
      out.write (array);

      // Set progress
      // ------------
      writeProgress = ((i+1)*100)/values;

      // Check for canceled
      // ------------------
      if (isCanceled) return;

    } // for

  } // writeVariable

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

  public void close () throws IOException {

    // Flush and close
    // ---------------
    flush();
    out.close();

  } // close

  ////////////////////////////////////////////////////////////

} // BinaryWriter class

////////////////////////////////////////////////////////////////////////
