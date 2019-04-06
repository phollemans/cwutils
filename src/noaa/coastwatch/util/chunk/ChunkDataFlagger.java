////////////////////////////////////////////////////////////////////////
/*

     File: ChunkDataFlagger.java
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

/**
 * The <code>ChunkDataFlagger</code> class is a visitor that modifies
 * any type of {@link DataChunk} instance by flagging specific
 * data values as missing.  To use a flagger object, call {@link #setMissingData}
 * to pass an array of flag values to use, then pass the flagger object as a visitor
 * to a {@link DataChunk} instance.  If the chunk has no associated missing
 * value, or the flag array has not been set, the data is not modified.
 *
 * @author Peter Hollemans
 * @since 3.5.0
 */
public class ChunkDataFlagger implements ChunkVisitor {

  /** The array of missing value flags. */
  private boolean[] isMissingArray;

  ////////////////////////////////////////////////////////////

  /**
   * Sets the missing data array.
   *
   * @param isMissingArray the array of missing value flags.  For each
   * entry that is true in the array, the corresponding value in the
   * chunk is set to missing when the visitor is accepted.
   */
  public void setMissingData (boolean[] isMissingArray) { this.isMissingArray = isMissingArray; }

  ////////////////////////////////////////////////////////////

  @Override
  public void visitByteChunk (ByteChunk chunk) {

    if (isMissingArray != null) {

      Byte missing = chunk.getMissing();
      if (missing != null) {
        byte missingValue = missing;
        byte[] byteData = chunk.getByteData();
        for (int i = 0; i < byteData.length; i++) { if (isMissingArray[i]) byteData[i] = missingValue; }
      } // if

    } // if

  } // visitByteChunk

  ////////////////////////////////////////////////////////////

  @Override
  public void visitShortChunk (ShortChunk chunk) {

    if (isMissingArray != null) {

      Short missing = chunk.getMissing();
      if (missing != null) {
        short missingValue = missing;
        short[] shortData = chunk.getShortData();
        for (int i = 0; i < shortData.length; i++) { if (isMissingArray[i]) shortData[i] = missingValue; }
      } // if

    } // if

  } // visitShortChunk

  ////////////////////////////////////////////////////////////

  @Override
  public void visitIntChunk (IntChunk chunk) {

    if (isMissingArray != null) {

      Integer missing = chunk.getMissing();
      if (missing != null) {
        int missingValue = missing;
        int[] intData = chunk.getIntData();
        for (int i = 0; i < intData.length; i++) { if (isMissingArray[i]) intData[i] = missingValue; }
      } // if

    } // if

  } // visitIntChunk

  ////////////////////////////////////////////////////////////

  @Override
  public void visitLongChunk (LongChunk chunk) {

    if (isMissingArray != null) {

      Long missing = chunk.getMissing();
      if (missing != null) {
        long missingValue = missing;
        long[] longData = chunk.getLongData();
        for (int i = 0; i < longData.length; i++) { if (isMissingArray[i]) longData[i] = missingValue; }
      } // if

    } // if

  } // visitLongChunk

  ////////////////////////////////////////////////////////////

  @Override
  public void visitFloatChunk (FloatChunk chunk) {

    if (isMissingArray != null) {

      Float missing = chunk.getMissing();
      if (missing == null) missing = Float.NaN;
      
      float missingValue = missing;
      float[] floatData = chunk.getFloatData();
      for (int i = 0; i < floatData.length; i++) { if (isMissingArray[i]) floatData[i] = missingValue; }

    } // if

  } // visitFloatChunk

  ////////////////////////////////////////////////////////////

  @Override
  public void visitDoubleChunk (DoubleChunk chunk) {

    if (isMissingArray != null) {

      Double missing = chunk.getMissing();
      if (missing == null) missing = Double.NaN;

      double missingValue = missing;
      double[] doubleData = chunk.getDoubleData();
      for (int i = 0; i < doubleData.length; i++) { if (isMissingArray[i]) doubleData[i] = missingValue; }

    } // if

  } // visitDoubleChunk

  ////////////////////////////////////////////////////////////

} // ChunkDataFlagger class

////////////////////////////////////////////////////////////////////////



