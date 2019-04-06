////////////////////////////////////////////////////////////////////////
/*

     File: ChunkDataCopier.java
   Author: Peter Hollemans
     Date: 2019/02/03

  CoastWatch Software Library and Utilities
  Copyright (c) 2019 National Oceanic and Atmospheric Administration
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
// -------
import noaa.coastwatch.util.chunk.ChunkVisitor;

/**
 * The <code>ChunkDataCopier</code> class copies raw data values between
 * {@link DataChunk} instances.
 *
 * @author Peter Hollemans
 * @since 3.5.0
 */
public class ChunkDataCopier {

  /** The single values being copied. */
  private byte byteValue;
  private short shortValue;
  private int intValue;
  private long longValue;
  private float floatValue;
  private double doubleValue;

  /** The index of the value to get or set. */
  private int index;
  
  /** The visitors used for get and set in the copy. */
  private ChunkValueGet getVisitor = new ChunkValueGet();
  private ChunkValueSet setVisitor = new ChunkValueSet();
  
  ////////////////////////////////////////////////////////////

  /** Performs the get operation as a visitor. */
  private class ChunkValueGet implements ChunkVisitor {

    @Override
    public void visitByteChunk (ByteChunk chunk) { byteValue = chunk.getByteData()[index]; }

    @Override
    public void visitShortChunk (ShortChunk chunk) { shortValue = chunk.getShortData()[index]; }

    @Override
    public void visitIntChunk (IntChunk chunk) { intValue = chunk.getIntData()[index]; }

    @Override
    public void visitLongChunk (LongChunk chunk) { longValue = chunk.getLongData()[index]; }

    @Override
    public void visitFloatChunk (FloatChunk chunk) { floatValue = chunk.getFloatData()[index]; }

    @Override
    public void visitDoubleChunk (DoubleChunk chunk) { doubleValue = chunk.getDoubleData()[index]; }

  } // ChunkValueGet class

  ////////////////////////////////////////////////////////////

  /** Performs the set operation as a visitor. */
  private class ChunkValueSet implements ChunkVisitor {

    @Override
    public void visitByteChunk (ByteChunk chunk) { chunk.getByteData()[index] = byteValue; }

    @Override
    public void visitShortChunk (ShortChunk chunk) { chunk.getShortData()[index] = shortValue; }

    @Override
    public void visitIntChunk (IntChunk chunk) { chunk.getIntData()[index] = intValue; }

    @Override
    public void visitLongChunk (LongChunk chunk) { chunk.getLongData()[index] = longValue; }

    @Override
    public void visitFloatChunk (FloatChunk chunk) { chunk.getFloatData()[index] = floatValue; }

    @Override
    public void visitDoubleChunk (DoubleChunk chunk) { chunk.getDoubleData()[index] = doubleValue; }

  } // ChunkValueSet class

  ////////////////////////////////////////////////////////////

  /**
   * Copies a raw data value from a source to a destination chunk.  The
   * chunks must be compatible.
   *
   * @param sourceChunk the data source chunk.
   * @param sourceIndex the index of the value in the source chunk.
   * @param destChunk the data destination chunk.
   * @param destIndex the index of the value in the destination chunk.
   */
  public void copyValue (
    DataChunk sourceChunk,
    int sourceIndex,
    DataChunk destChunk,
    int destIndex
  ) {

    index = sourceIndex;
    sourceChunk.accept (getVisitor);
    index = destIndex;
    destChunk.accept (setVisitor);

  } // copyValue

  ////////////////////////////////////////////////////////////

} // ChunkDataCopier class

////////////////////////////////////////////////////////////////////////


