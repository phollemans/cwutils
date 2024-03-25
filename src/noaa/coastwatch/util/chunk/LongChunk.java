////////////////////////////////////////////////////////////////////////
/*

     File: LongChunk.java
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
import noaa.coastwatch.util.chunk.PackingScheme;

/**
 * The <code>LongChunk</code> class holds primitive long data with optional
 * packing scheme and missing values.
 *
 * @author Peter Hollemans
 * @since 3.4.0
 */
public class LongChunk implements IntegerValuedDataChunk {

  // Variables
  // ---------

  /** The primitive data array. */
  private long[] longData;
  
  /** The missing data value, or null for none. */
  private Long missing;
  
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
      type = DataType.LONG; // TODO: What do we return for unsigned here??
    else
      type = scheme.getUnpackedType();
    
    return (type);
  
  } // getExternalType

  ////////////////////////////////////////////////////////////

  @Override
  public void accept (ChunkVisitor visitor) { visitor.visitLongChunk (this); }

  ////////////////////////////////////////////////////////////

  @Override
  public int getValues() { return (longData.length); }

  ////////////////////////////////////////////////////////////

  @Override
  public Object getPrimitiveData() { return (longData); }

  ////////////////////////////////////////////////////////////

  @Override
  public DataChunk blankCopy () {
    
    return (blankCopyWithValues (longData.length));
    
  } // blankCopy

  ////////////////////////////////////////////////////////////

  public DataChunk blankCopyWithValues (int values) {
  
    DataChunk chunk = new LongChunk (new long[values], isUnsigned, missing, scheme);
    return (chunk);

  } // blankCopyWithValues

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new initialized data chunk with scaling parameters.
   *
   * @param longData the long data values.
   * @param isUnsigned the unsigned flag, true if the long data values are
   * in the range [0..2^64-1] or false for [-2^63..2^63-1].
   * @param missing the missing data value, or null for none.
   * @param scheme the packing scheme, or null for no packing.
   */
  public LongChunk (
    long[] longData,
    boolean isUnsigned,
    Long missing,
    PackingScheme scheme
  ) {

    this.longData = longData;
    this.isUnsigned = isUnsigned;
    this.missing = missing;
    this.scheme = scheme;
    
  } // LongChunk constructor

  ////////////////////////////////////////////////////////////

  /**
   * Gets the missing value.  The missing value is used in the primitive data
   * to represent invalid data values.
   *
   * @return the missing value or null if the chunk has no missing value
   * specified.
   */
  public Long getMissing () { return (missing); }

  ////////////////////////////////////////////////////////////

  /**
   * Gets the primitive array used to store data.
   *
   * @return the primitive long data array containing the data.
   */
  public long[] getLongData() { return (longData); }

  ////////////////////////////////////////////////////////////

  @Override
  public boolean isCompatible (DataChunk chunk) {

    boolean compatible = false;
    if (chunk instanceof LongChunk) {
      var otherChunk = (LongChunk) chunk;
      compatible = 
        (
          (this.missing == null && otherChunk.missing == null) ||
          (this.missing != null && this.missing.equals (otherChunk.missing))
        ) &&
        (
          (this.scheme == null && otherChunk.scheme == null) || 
          (this.scheme != null && this.scheme.equals (otherChunk.scheme))
        ) &&
        (
          this.isUnsigned == otherChunk.isUnsigned
        );
    } // if

    return (compatible);

  } // isCompatible

  ////////////////////////////////////////////////////////////

  @Override
  public int valueBytes() { return (8); }

  ////////////////////////////////////////////////////////////

} // LongChunk class

////////////////////////////////////////////////////////////////////////
