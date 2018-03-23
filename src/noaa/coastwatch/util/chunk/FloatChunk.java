////////////////////////////////////////////////////////////////////////
/*

     File: FloatChunk.java
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
 * The <code>FloatChunk</code> class holds primitive float data with optional
 * packing scheme and missing values.
 *
 * @author Peter Hollemans
 * @since 3.4.0
 */
public class FloatChunk implements DataChunk {

  // Variables
  // ---------

  /** The primitive data array. */
  private float[] floatData;
  
  /** The missing data value, or null for none. */
  private Float missing;
  
  ////////////////////////////////////////////////////////////

  @Override
  public DataType getExternalType() {
  
    return (DataType.FLOAT);
  
  } // getExternalType

  ////////////////////////////////////////////////////////////

  @Override
  public void accept (ChunkVisitor visitor) { visitor.visitFloatChunk (this); }

  ////////////////////////////////////////////////////////////

  @Override
  public int getValues() { return (floatData.length); }

  ////////////////////////////////////////////////////////////

  @Override
  public Object getPrimitiveData() { return (floatData); }

  ////////////////////////////////////////////////////////////

  @Override
  public DataChunk blankCopy () {
    
    return (blankCopyWithValues (floatData.length));
    
  } // blankCopy

  ////////////////////////////////////////////////////////////

  public DataChunk blankCopyWithValues (int values) {
  
    DataChunk chunk = new FloatChunk (new float[values], missing);
    return (chunk);

  } // blankCopyWithValues

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new initialized data chunk with scaling parameters.
   *
   * @param floatData the float data values.
   * @param missing the missing data value, or null for none.
   */
  public FloatChunk (
    float[] floatData,
    Float missing
  ) {

    this.floatData = floatData;
    this.missing = missing;
    
  } // FloatChunk constructor

  ////////////////////////////////////////////////////////////

  /**
   * Gets the missing value.  The missing value is used in the primitive data
   * to represent invalid data values.
   *
   * @return the missing value or null if the chunk has no missing value
   * specified.
   */
  public Float getMissing () { return (missing); }

  ////////////////////////////////////////////////////////////

  /**
   * Gets the primitive array used to store data.
   *
   * @return the primitive float data array containing the data.
   */
  public float[] getFloatData() { return (floatData); }

  ////////////////////////////////////////////////////////////

} // FloatChunk class

////////////////////////////////////////////////////////////////////////
