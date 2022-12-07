////////////////////////////////////////////////////////////////////////
/*

     File: IntChunk.java
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
 * The <code>IntChunk</code> class holds primitive int data with optional
 * packing scheme and missing values.
 *
 * @author Peter Hollemans
 * @since 3.4.0
 */
public class IntChunk implements IntegerValuedDataChunk {

  // Variables
  // ---------

  /** The primitive data array. */
  private int[] intData;
  
  /** The missing data value, or null for none. */
  private Integer missing;
  
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
      type = (isUnsigned ? DataType.LONG : DataType.INT);
    else
      type = scheme.getUnpackedType();
    
    return (type);
  
  } // getExternalType

  ////////////////////////////////////////////////////////////

  @Override
  public void accept (ChunkVisitor visitor) { visitor.visitIntChunk (this); }

  ////////////////////////////////////////////////////////////

  @Override
  public int getValues() { return (intData.length); }

  ////////////////////////////////////////////////////////////

  @Override
  public Object getPrimitiveData() { return (intData); }

  ////////////////////////////////////////////////////////////

  @Override
  public DataChunk blankCopy () {
    
    return (blankCopyWithValues (intData.length));
    
  } // blankCopy

  ////////////////////////////////////////////////////////////

  public DataChunk blankCopyWithValues (int values) {
  
    DataChunk chunk = new IntChunk (new int[values], isUnsigned, missing, scheme);
    return (chunk);

  } // blankCopyWithValues

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new initialized data chunk with scaling parameters.
   *
   * @param intData the int data values.
   * @param isUnsigned the unsigned flag, true if the int data values are
   * in the range [0..4294967295] or false for [-2147483648..2147483647].
   * @param missing the missing data value, or null for none.
   * @param scheme the packing scheme, or null for no packing.
   */
  public IntChunk (
    int[] intData,
    boolean isUnsigned,
    Integer missing,
    PackingScheme scheme
  ) {

    this.intData = intData;
    this.isUnsigned = isUnsigned;
    this.missing = missing;
    this.scheme = scheme;
    
  } // IntChunk constructor

  ////////////////////////////////////////////////////////////

  /**
   * Gets the missing value.  The missing value is used in the primitive data
   * to represent invalid data values.
   *
   * @return the missing value or null if the chunk has no missing value
   * specified.
   */
  public Integer getMissing () { return (missing); }

  ////////////////////////////////////////////////////////////

  /**
   * Gets the primitive array used to store data.
   *
   * @return the primitive int data array containing the data.
   */
  public int[] getIntData() { return (intData); }

  ////////////////////////////////////////////////////////////

  @Override
  public boolean isCompatible (DataChunk chunk) {

    boolean compatible = false;
    if (chunk instanceof IntChunk) {
      var otherChunk = (IntChunk) chunk;
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

} // IntChunk class

////////////////////////////////////////////////////////////////////////
