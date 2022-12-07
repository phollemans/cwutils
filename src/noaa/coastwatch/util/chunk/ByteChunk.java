////////////////////////////////////////////////////////////////////////
/*

     File: ByteChunk.java
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
 * The <code>ByteChunk</code> class holds primitive byte data with optional
 * packing scheme and missing values.
 *
 * @author Peter Hollemans
 * @since 3.4.0
 */
public class ByteChunk implements IntegerValuedDataChunk {

  // Variables
  // ---------

  /** The primitive data array. */
  private byte[] byteData;
  
  /** The missing data value, or null for none. */
  private Byte missing;
  
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
      type = (isUnsigned ? DataType.SHORT : DataType.BYTE);
    else
      type = scheme.getUnpackedType();
    
    return (type);
  
  } // getExternalType

  ////////////////////////////////////////////////////////////

  @Override
  public void accept (ChunkVisitor visitor) { visitor.visitByteChunk (this); }

  ////////////////////////////////////////////////////////////

  @Override
  public int getValues() { return (byteData.length); }

  ////////////////////////////////////////////////////////////

  @Override
  public Object getPrimitiveData() { return (byteData); }

  ////////////////////////////////////////////////////////////

  @Override
  public DataChunk blankCopy () {
    
    return (blankCopyWithValues (byteData.length));
    
  } // blankCopy

  ////////////////////////////////////////////////////////////

  public DataChunk blankCopyWithValues (int values) {
  
    DataChunk chunk = new ByteChunk (new byte[values], isUnsigned, missing, scheme);
    return (chunk);

  } // blankCopyWithValues

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new initialized data chunk with scaling parameters.
   *
   * @param byteData the byte data values.
   * @param isUnsigned the unsigned flag, true if the byte data values are
   * in the range [0..255] or false for [-128..127].
   * @param missing the missing data value, or null for none.
   * @param scheme the packing scheme, or null for no packing.
   */
  public ByteChunk (
    byte[] byteData,
    boolean isUnsigned,
    Byte missing,
    PackingScheme scheme
  ) {

    this.byteData = byteData;
    this.isUnsigned = isUnsigned;
    this.missing = missing;
    this.scheme = scheme;
    
  } // ByteChunk constructor

  ////////////////////////////////////////////////////////////

  /**
   * Gets the missing value.  The missing value is used in the primitive data
   * to represent invalid data values.
   *
   * @return the missing value or null if the chunk has no missing value
   * specified.
   */
  public Byte getMissing () { return (missing); }

  ////////////////////////////////////////////////////////////

  /**
   * Gets the primitive array used to store data.
   *
   * @return the primitive byte data array containing the data.
   */
  public byte[] getByteData() { return (byteData); }

  ////////////////////////////////////////////////////////////

  @Override
  public boolean isCompatible (DataChunk chunk) {

    boolean compatible = false;
    if (chunk instanceof ByteChunk) {
      var otherChunk = (ByteChunk) chunk;
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

} // ByteChunk class

////////////////////////////////////////////////////////////////////////
