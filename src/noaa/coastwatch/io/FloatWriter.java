////////////////////////////////////////////////////////////////////////
/*
     FILE: FloatWriter.java
  PURPOSE: A class to write binary format files as a stream of 
           32-bit IEEE floating point values.
   AUTHOR: Mark Robinson
     DATE: 2002/07/19
  CHANGES: 2002/08/25, PFH, rearranged
           2005/02/16, PFH, fixed getTypeMin() returning positive value

  CoastWatch Software Library and Utilities
  Copyright 1998-2005, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.io;

// Imports
// -------
import java.io.*;
import noaa.coastwatch.util.*;

/**
 * A float writer is a binary writer that writes data as a stream of
 * 32-bit IEEE floating point values.  The scaling and range methods
 * are not supported and perform no function for 32-bit float data.
 */
public class FloatWriter
  extends BinaryWriter {

  ////////////////////////////////////////////////////////////

  /**
   * Sets the scaling range.  This method overrides the super class and
   * simply sets the scaling to unity; scaling is not supported here.
   */
  public void setRange (
    double min, 
    double max
  ) {

    super.setScaling (null);

  } // setRange

  ////////////////////////////////////////////////////////////

  /**
   * Sets the scaling factor and offset.  This method overrides the
   * super class and simply sets the scaling to unity; scaling is not
   * supported here.
   */
  public void setScaling (double[] scaling) {

    super.setScaling (null);

  } // setScaling

  ////////////////////////////////////////////////////////////

  public byte[] convertValue (
    Number value
  ) {

    int intValue = Float.floatToRawIntBits (value.floatValue());
    return (getBytes (intValue));

  } // convertValue

  ////////////////////////////////////////////////////////////

  public double getTypeMin () { return (-Float.MAX_VALUE); }

  ////////////////////////////////////////////////////////////

  public double getTypeMax () { return (Float.MAX_VALUE); }

  ////////////////////////////////////////////////////////////

  /** 
   * Throws an error because this method should never be called for
   * this class.
   */
 public double getTypeRange () {
    
   throw new UnsupportedOperationException();

 } // getTypeRange
  
  ////////////////////////////////////////////////////////////

  public Number getDefaultMissing () { return (new Float (Float.NaN)); }

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new binary file from the specified Earth data info
   * and file name.
   *
   * @param info the Earth data info object.
   * @param file the new binary file name.
   *
   * @throws IOException if an error occurred opening the file.
   *
   * @see BinaryWriter#BinaryWriter
   */
  public FloatWriter (
    EarthDataInfo info,
    String file
  ) throws IOException {

    super (info, file);

  } // FloatWriter constructor

  ////////////////////////////////////////////////////////////

} // FloatWriter class

////////////////////////////////////////////////////////////////////////
