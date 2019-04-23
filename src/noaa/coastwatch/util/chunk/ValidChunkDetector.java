////////////////////////////////////////////////////////////////////////
/*

     File: ValidChunkDetector.java
   Author: Peter Hollemans
     Date: 2019/04/14

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
 * The <code>ValidChunkDetector</code> class is a visitor that checks data
 * chunks for missing values to determine if all the chunk's data values are
 * missing, or if the chunk contains some valid data.  To use, pass a detector
 * object as a visitor to a {@link DataChunk} instance and then call the
 * {@link #isValid} method.
 *
 * @author Peter Hollemans
 * @since 3.5.0
 */
public class ValidChunkDetector implements ChunkVisitor {

  /**
   * The valid flag, true if at least some of the data in the visited
   * chunk is not set to the missing value.
   */
  private boolean isValid;

  ////////////////////////////////////////////////////////////

  /**
   * Gets the valid flag value.
   *
   * @return true if at least some of the data in the visited
   * chunk is not set to the missing value.
   */
  public boolean isValid () { return (isValid); }

  ////////////////////////////////////////////////////////////

  @Override
  public void visitByteChunk (ByteChunk chunk) {

    Byte missing = chunk.getMissing();
    if (missing == null) {
      isValid = true;
    } // if
    else {
      byte missingValue = missing;
      byte[] byteData = chunk.getByteData();
      isValid = false;
      for (int i = 0; i < byteData.length; i++) {
        if (byteData[i] != missingValue) {
          isValid = true;
          break;
        } // if
      } // for
    } // else

  } // visitByteChunk

  ////////////////////////////////////////////////////////////

  @Override
  public void visitShortChunk (ShortChunk chunk) {

    Short missing = chunk.getMissing();
    if (missing == null) {
      isValid = true;
    } // if
    else {
      short missingValue = missing;
      short[] shortData = chunk.getShortData();
      isValid = false;
      for (int i = 0; i < shortData.length; i++) {
        if (shortData[i] != missingValue) {
          isValid = true;
          break;
        } // if
      } // for
    } // else

  } // visitShortChunk

  ////////////////////////////////////////////////////////////

  @Override
  public void visitIntChunk (IntChunk chunk) {

    Integer missing = chunk.getMissing();
    if (missing == null) {
      isValid = true;
    } // if
    else {
      int missingValue = missing;
      int[] intData = chunk.getIntData();
      isValid = false;
      for (int i = 0; i < intData.length; i++) {
        if (intData[i] != missingValue) {
          isValid = true;
          break;
        } // if
      } // for
    } // else

  } // visitIntChunk

  ////////////////////////////////////////////////////////////

  @Override
  public void visitLongChunk (LongChunk chunk) {

    Long missing = chunk.getMissing();
    if (missing == null) {
      isValid = true;
    } // if
    else {
      long missingValue = missing;
      long[] longData = chunk.getLongData();
      isValid = false;
      for (int i = 0; i < longData.length; i++) {
        if (longData[i] != missingValue) {
          isValid = true;
          break;
        } // if
      } // for
    } // else

  } // visitLongChunk

  ////////////////////////////////////////////////////////////

  @Override
  public void visitFloatChunk (FloatChunk chunk) {

    Float missing = chunk.getMissing();
    if (missing == null) {
      isValid = true;
    } // if
    else {

      float[] floatData = chunk.getFloatData();
      isValid = false;
      if (missing.isNaN()) {
        for (int i = 0; i < floatData.length; i++) {
          if (!Float.isNaN (floatData[i])) {
            isValid = true;
            break;
          } // if
        } // for
      } // if

      else {
        float missingValue = missing;
        for (int i = 0; i < floatData.length; i++) {
          if (floatData[i] != missingValue) {
            isValid = true;
            break;
          } // if
        } // for
      } // else

    } // else

  } // visitFloatChunk

  ////////////////////////////////////////////////////////////

  @Override
  public void visitDoubleChunk (DoubleChunk chunk) {

    Double missing = chunk.getMissing();
    if (missing == null) {
      isValid = true;
    } // if
    else {

      double[] doubleData = chunk.getDoubleData();
      isValid = false;
      if (missing.isNaN()) {
        for (int i = 0; i < doubleData.length; i++) {
          if (!Double.isNaN (doubleData[i])) {
            isValid = true;
            break;
          } // if
        } // for
      } // if

      else {
        double missingValue = missing;
        for (int i = 0; i < doubleData.length; i++) {
          if (doubleData[i] != missingValue) {
            isValid = true;
            break;
          } // if
        } // for
      } // else

    } // else

  } // visitDoubleChunk

  ////////////////////////////////////////////////////////////

} // ValidChunkDetector class

////////////////////////////////////////////////////////////////////////




