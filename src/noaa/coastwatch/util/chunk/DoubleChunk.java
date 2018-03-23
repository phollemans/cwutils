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
public class DoubleChunk implements DataChunk {

  // Variables
  // ---------

  /** The primitive data array. */
  private double[] doubleData;
  
  /** The missing data value, or null for none. */
  private Double missing;
  
  ////////////////////////////////////////////////////////////

  @Override
  public DataType getExternalType() {
  
    return (DataType.DOUBLE);
  
  } // getExternalType

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

  public DataChunk blankCopyWithValues (int values) {
  
    DataChunk chunk = new DoubleChunk (new double[values], missing);
    return (chunk);

  } // blankCopyWithValues

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new initialized data chunk with scaling parameters.
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

} // DoubleChunk class

////////////////////////////////////////////////////////////////////////
