////////////////////////////////////////////////////////////////////////
/*

     File: ShortChunk.java
   Author: Peter Hollemans
     Date: 2017/11/01

  CoastWatch Software Library and Utilities
  Copyright (c) 2017 National Oceanic and Atmospheric Administration
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
import noaa.coastwatch.util.chunk.PackingScheme;

/**
 * The <code>ShortChunk</code> class holds primitive short data with optional
 * packing scheme and missing values.
 *
 * @author Peter Hollemans
 * @since 3.4.0
 */
public class ShortChunk implements IntegerValuedDataChunk {

  // Variables
  // ---------

  /** The primitive data array. */
  private short[] shortData;
  
  /** The missing data value, or null for none. */
  private Short missing;
  
  /** The packing scheme, or null for none. */
  private PackingScheme scheme;

  /** The unsigned flag. */
  private boolean isUnsigned;

  ////////////////////////////////////////////////////////////

  @Override
  public boolean isUnsigned() { return (isUnsigned); }

  ////////////////////////////////////////////////////////////

  @Override
  public PackingScheme getPackingScheme () { return (scheme); }

  ////////////////////////////////////////////////////////////

  @Override
  public DataType getExternalType() {
  
    DataType type;
    if (scheme == null)
      type = (isUnsigned ? DataType.INT : DataType.SHORT);
    else
      type = scheme.getUnpackedType();
    
    return (type);
  
  } // getExternalType

  ////////////////////////////////////////////////////////////

  @Override
  public void accept (ChunkVisitor visitor) { visitor.visitShortChunk (this); }

  ////////////////////////////////////////////////////////////

  @Override
  public int getValues() { return (shortData.length); }

  ////////////////////////////////////////////////////////////

  @Override
  public Object getPrimitiveData() { return (shortData); }

  ////////////////////////////////////////////////////////////

  @Override
  public DataChunk blankCopy () {
    
    return (blankCopyWithValues (shortData.length));
    
  } // blankCopy

  ////////////////////////////////////////////////////////////

  public DataChunk blankCopyWithValues (int values) {
  
    DataChunk chunk = new ShortChunk (new short[values], isUnsigned, missing, scheme);
    return (chunk);

  } // blankCopyWithValues

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new initialized data chunk with scaling parameters.
   *
   * @param shortData the short data values.
   * @param isUnsigned the unsigned flag, true if the short data values are
   * in the range [0..65535] or false for [-32768..32767].
   * @param missing the missing data value, or null for none.
   * @param scheme the packing scheme, or null for no packing.
   */
  public ShortChunk (
    short[] shortData,
    boolean isUnsigned,
    Short missing,
    PackingScheme scheme
  ) {

    this.shortData = shortData;
    this.isUnsigned = isUnsigned;
    this.missing = missing;
    this.scheme = scheme;
    
  } // ShortChunk constructor

  ////////////////////////////////////////////////////////////

  /**
   * Gets the missing value.  The missing value is used in the primitive data
   * to represent invalid data values.
   *
   * @return the missing value or null if the chunk has no missing value
   * specified.
   */
  public Short getMissing () { return (missing); }

  ////////////////////////////////////////////////////////////

  /**
   * Gets the primitive array used to store data.
   *
   * @return the primitive short data array containing the data.
   */
  public short[] getShortData() { return (shortData); }

  ////////////////////////////////////////////////////////////

} // ShortChunk class

////////////////////////////////////////////////////////////////////////
