////////////////////////////////////////////////////////////////////////
/*

     File: DoubleChunk.java
   Author: Peter Hollemans
     Date: 2018/01/21

  CoastWatch Software Library and Utilities
  Copyright (c) 2018 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.util.chunk;

// Imports
// --------
import noaa.coastwatch.util.chunk.DataChunk;
import noaa.coastwatch.util.chunk.DataChunk.DataType;
import noaa.coastwatch.util.chunk.ChunkVisitor;

/**
 * The <code>DoubleChunk</code> class holds primitive double data with optional
 * packing scheme and missing values.
 *
 * @author Peter Hollemans
 * @since 3.4.0
 */
public class DoubleChunk implements FloatingPointValuedDataChunk {

  // Variables
  // ---------

  /** The primitive data array. */
  private double[] doubleData;
  
  /** The missing data value, or null for none. */
  private Double missing;

  /** The scaling scheme, or null for none. */
  private ScalingScheme scheme;

  ////////////////////////////////////////////////////////////

  @Override
  public DataType getExternalType() {
  
    return (DataType.DOUBLE);
  
  } // getExternalType

  ////////////////////////////////////////////////////////////

  @Override
  public ScalingScheme getScalingScheme() { return (scheme); }

  ////////////////////////////////////////////////////////////

  @Override
  public void accept (ChunkVisitor visitor) { visitor.visitDoubleChunk (this); }

  ////////////////////////////////////////////////////////////

  @Override
  public int getValues() { return (doubleData.length); }

  ////////////////////////////////////////////////////////////

  @Override
  public Object getPrimitiveData() { return (doubleData); }

  ////////////////////////////////////////////////////////////

  @Override
  public DataChunk blankCopy () {
    
    return (blankCopyWithValues (doubleData.length));
    
  } // blankCopy

  ////////////////////////////////////////////////////////////

  @Override
  public DataChunk blankCopyWithValues (int values) {
  
    DataChunk chunk = new DoubleChunk (new double[values], missing, scheme);
    return (chunk);

  } // blankCopyWithValues

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new initialized data chunk.
   *
   * @param doubleData the double data values.
   * @param missing the missing data value, or null for none.
   */
  public DoubleChunk (
    double[] doubleData,
    Double missing
  ) {

    this.doubleData = doubleData;
    this.missing = missing;
    
  } // DoubleChunk constructor

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new initialized data chunk with scaling parameters.
   *
   * @param doubleData the double data values.
   * @param missing the missing data value, or null for none.
   * @param scheme the scaling scheme or null for none.
   *
   * @since 3.6.1
   */
  public DoubleChunk (
    double[] doubleData,
    Double missing,
    ScalingScheme scheme
  ) {

    this.doubleData = doubleData;
    this.missing = missing;
    this.scheme = scheme;
    
  } // DoubleChunk constructor

  ////////////////////////////////////////////////////////////

  /**
   * Gets the missing value.  The missing value is used in the primitive data
   * to represent invalid data values.
   *
   * @return the missing value or null if the chunk has no missing value
   * specified.
   */
  public Double getMissing () { return (missing); }

  ////////////////////////////////////////////////////////////

  /**
   * Gets the primitive array used to store data.
   *
   * @return the primitive double data array containing the data.
   */
  public double[] getDoubleData() { return (doubleData); }

  ////////////////////////////////////////////////////////////

  @Override
  public boolean isCompatible (DataChunk chunk) {

    boolean compatible = false;
    if (chunk instanceof DoubleChunk) {
      var otherChunk = (DoubleChunk) chunk;
      compatible = 
        (
          (this.missing == null && otherChunk.missing == null) ||
          (this.missing != null && this.missing.equals (otherChunk.missing))
        ) &&
        (
          (this.scheme == null && otherChunk.scheme == null) || 
          (this.scheme != null && this.scheme.equals (otherChunk.scheme))
        );
    } // if

    return (compatible);

  } // isCompatible

  ////////////////////////////////////////////////////////////

  @Override
  public int valueBytes() { return (8); }

  ////////////////////////////////////////////////////////////

} // DoubleChunk class

////////////////////////////////////////////////////////////////////////
