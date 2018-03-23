////////////////////////////////////////////////////////////////////////
/*

     File: FloatPackingScheme.java
   Author: Peter Hollemans
     Date: 2017/11/24

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
// -------
import noaa.coastwatch.util.chunk.DataChunk.DataType;

// Testing
import noaa.coastwatch.test.TestLogger;

/**
 * The <code>FloatPackingScheme</code> class implements a scale and offset
 * packing scheme for primitive float data.  Float values are packed as
 * <code>integer = float/scale + offset</code> and then rounded to the nearest
 * integer.  Various convenience methods are available for packing and unpacking
 * primitive data arrays.
 *
 * @author Peter Hollemans
 * @since 3.4.0
 */
@noaa.coastwatch.test.Testable
public class FloatPackingScheme implements PackingScheme {

  /** The scaling factor for float data. */
  public float scale;

  /** The offset for float data. */
  public float offset;

  ////////////////////////////////////////////////////////////

  @Override
  public DataType getUnpackedType() { return (DataType.FLOAT); }

  ////////////////////////////////////////////////////////////

  @Override
  public void accept (PackingSchemeVisitor visitor) {

    visitor.visitFloatPackingScheme (this);

  } // accept
  
  ////////////////////////////////////////////////////////////

  /**
   * Unpacks primitive data from a byte array into a float array.
   *
   * @param byteData the source array to unpack.
   * @param floatData the destination array to unpack into.
   * @param missing the missing value to detect (or null for none).
   * Values in the source array that match the missing value will be
   * assigned Float.NaN in the destination array.
   * @param isUnsigned the unsigned flag, true if the data is unsigned
   * or false if not.
   */
  public void unpackFromByteData (byte[] byteData, float[] floatData, Byte missing, boolean isUnsigned) {

    if (missing == null) {
      if (isUnsigned) unpackFromUByte (byteData, floatData);
      else unpackFromByte (byteData, floatData);
    } // if
    else {
      if (isUnsigned) unpackFromUByteWithMissing (byteData, floatData, missing);
      else unpackFromByteWithMissing (byteData, floatData, missing);
    } // else

  } // unpackFromByteData

  ////////////////////////////////////////////////////////////

  /**
   * Packs primitive data from a float array into a byte array.
   *
   * @param floatData the source array to pack.
   * @param byteData the destination array to pack into.
   * @param missing the missing value to detect (or null for none).
   * Non-finite (NaN and infinity values) in the source array will be
   * assigned the missing value in the destination array.
   * @param isUnsigned the unsigned flag, true if the destination data is
   * unsigned or false if not.
   */
  public void packToByteData (float[] floatData, byte[] byteData, Byte missing, boolean isUnsigned) {

    if (missing == null) {
      if (isUnsigned) packToUByte (floatData, byteData);
      else packToByte (floatData, byteData);
    } // if
    else {
      if (isUnsigned) packToUByteWithMissing (floatData, byteData, missing);
      else packToByteWithMissing (floatData, byteData, missing);
    } // else

  } // packToByteData

  ////////////////////////////////////////////////////////////

  private void unpackFromByteWithMissing (byte[] byteData, float[] floatData, byte missing) {

    for (int i = 0; i < byteData.length; i++) {
      if (byteData[i] == missing)
        floatData[i] = Float.NaN;
      else
        floatData[i] = unpackFloat (byteData[i]);
    } // for

  } // unpackFromByteWithMissing

  ////////////////////////////////////////////////////////////

  private void unpackFromByte (byte[] byteData, float[] floatData) {

    for (int i = 0; i < byteData.length; i++) {
      floatData[i] = unpackFloat (byteData[i]);
    } // for

  } // unpackFromByte

  ////////////////////////////////////////////////////////////

  private void packToByteWithMissing (float[] floatData, byte[] byteData, byte missing) {

    for (int i = 0; i < floatData.length; i++) {
      if (!Float.isFinite (floatData[i]))
        byteData[i] = missing;
      else
        byteData[i] = (byte) packFloat (floatData[i]);
    } // for

  } // packToByteWithMissing

  ////////////////////////////////////////////////////////////

  private void packToByte (float[] floatData, byte[] byteData) {

    for (int i = 0; i < floatData.length; i++) {
      byteData[i] = (byte) packFloat (floatData[i]);
    } // for

  } // packToByte

  ////////////////////////////////////////////////////////////

  private void unpackFromUByteWithMissing (byte[] byteData, float[] floatData, byte missing) {

    for (int i = 0; i < byteData.length; i++) {
      if (byteData[i] == missing)
        floatData[i] = Float.NaN;
      else
        floatData[i] = unpackFloat ((short) (byteData[i] & 0xff));
    } // for

  } // unpackFromUByteWithMissing

  ////////////////////////////////////////////////////////////

  private void unpackFromUByte (byte[] byteData, float[] floatData) {

    for (int i = 0; i < byteData.length; i++) {
      floatData[i] = unpackFloat ((short) (byteData[i] & 0xff));
    } // for

  } // unpackFromUByte

  ////////////////////////////////////////////////////////////

  private void packToUByteWithMissing (float[] floatData, byte[] byteData, byte missing) {

    for (int i = 0; i < floatData.length; i++) {
      if (!Float.isFinite (floatData[i]))
        byteData[i] = missing;
      else
        byteData[i] = (byte) (packFloat (floatData[i]) & 0xff);
    } // for

  } // packToUByteWithMissing

  ////////////////////////////////////////////////////////////

  private void packToUByte (float[] floatData, byte[] byteData) {

    for (int i = 0; i < floatData.length; i++) {
      byteData[i] = (byte) (packFloat (floatData[i]) & 0xff);
    } // for

  } // packToUByte

  ////////////////////////////////////////////////////////////

  /**
   * Unpacks primitive data from a short array into a float array.
   *
   * @param shortData the source array to unpack.
   * @param floatData the destination array to unpack into.
   * @param missing the missing value to detect (or null for none).
   * Values in the source array that match the missing value will be
   * assigned Float.NaN in the destination array.
   * @param isUnsigned the unsigned flag, true if the data is unsigned
   * or false if not.
   */
  public void unpackFromShortData (short[] shortData, float[] floatData, Short missing, boolean isUnsigned) {

    if (missing == null) {
      if (isUnsigned) unpackFromUShort (shortData, floatData);
      else unpackFromShort (shortData, floatData);
    } // if
    else {
      if (isUnsigned) unpackFromUShortWithMissing (shortData, floatData, missing);
      else unpackFromShortWithMissing (shortData, floatData, missing);
    } // else

  } // unpackFromShortData

  ////////////////////////////////////////////////////////////

  /**
   * Packs primitive data from a float array into a short array.
   *
   * @param floatData the source array to pack.
   * @param shortData the destination array to pack into.
   * @param missing the missing value to detect (or null for none).
   * Non-finite (NaN and infinity values) in the source array will be
   * assigned the missing value in the destination array.
   * @param isUnsigned the unsigned flag, true if the destination data is
   * unsigned or false if not.
   */
  public void packToShortData (float[] floatData, short[] shortData, Short missing, boolean isUnsigned) {

    if (missing == null) {
      if (isUnsigned) packToUShort (floatData, shortData);
      else packToShort (floatData, shortData);
    } // if
    else {
      if (isUnsigned) packToUShortWithMissing (floatData, shortData, missing);
      else packToShortWithMissing (floatData, shortData, missing);
    } // else

  } // packToShortData

  ////////////////////////////////////////////////////////////

  private void unpackFromShortWithMissing (short[] shortData, float[] floatData, short missing) {

    for (int i = 0; i < shortData.length; i++) {
      if (shortData[i] == missing)
        floatData[i] = Float.NaN;
      else
        floatData[i] = unpackFloat (shortData[i]);
    } // for

  } // unpackFromShortWithMissing

  ////////////////////////////////////////////////////////////

  private void unpackFromShort (short[] shortData, float[] floatData) {

    for (int i = 0; i < shortData.length; i++) {
      floatData[i] = unpackFloat (shortData[i]);
    } // for

  } // unpackFromShort

  ////////////////////////////////////////////////////////////

  private void packToShortWithMissing (float[] floatData, short[] shortData, short missing) {

    for (int i = 0; i < floatData.length; i++) {
      if (!Float.isFinite (floatData[i]))
        shortData[i] = missing;
      else
        shortData[i] = (short) packFloat (floatData[i]);
    } // for

  } // packToShortWithMissing

  ////////////////////////////////////////////////////////////

  private void packToShort (float[] floatData, short[] shortData) {

    for (int i = 0; i < floatData.length; i++) {
      shortData[i] = (short) packFloat (floatData[i]);
    } // for

  } // packToShort

  ////////////////////////////////////////////////////////////

  private void unpackFromUShortWithMissing (short[] shortData, float[] floatData, short missing) {

    for (int i = 0; i < shortData.length; i++) {
      if (shortData[i] == missing)
        floatData[i] = Float.NaN;
      else
        floatData[i] = unpackFloat ((int) (shortData[i] & 0xffff));
    } // for

  } // unpackFromUShortWithMissing

  ////////////////////////////////////////////////////////////

  private void unpackFromUShort (short[] shortData, float[] floatData) {

    for (int i = 0; i < shortData.length; i++) {
      floatData[i] = unpackFloat ((int) (shortData[i] & 0xffff));
    } // for

  } // unpackFromUShort

  ////////////////////////////////////////////////////////////

  private void packToUShortWithMissing (float[] floatData, short[] shortData, short missing) {

    for (int i = 0; i < floatData.length; i++) {
      if (!Float.isFinite (floatData[i]))
        shortData[i] = missing;
      else
        shortData[i] = (short) (packFloat (floatData[i]) & 0xffff);
    } // for

  } // packToUShortWithMissing

  ////////////////////////////////////////////////////////////

  private void packToUShort (float[] floatData, short[] shortData) {

    for (int i = 0; i < floatData.length; i++) {
      shortData[i] = (short) (packFloat (floatData[i]) & 0xffff);
    } // for

  } // packToUShort

  ////////////////////////////////////////////////////////////

  /**
   * Unpacks primitive data from a int array into a float array.
   *
   * @param intData the source array to unpack.
   * @param floatData the destination array to unpack into.
   * @param missing the missing value to detect (or null for none).
   * Values in the source array that match the missing value will be
   * assigned Float.NaN in the destination array.
   */
  public void unpackFromIntData (int[] intData, float[] floatData, Integer missing) {

    if (missing == null) unpackFromInt (intData, floatData);
    else unpackFromIntWithMissing (intData, floatData, missing);

  } // unpackFromIntData

  ////////////////////////////////////////////////////////////

  /**
   * Packs primitive data from a float array into a int array.
   *
   * @param floatData the source array to pack.
   * @param intData the destination array to pack into.
   * @param missing the missing value to detect (or null for none).
   * Non-finite (NaN and infinity values) in the source array will be
   * assigned the missing value in the destination array.
   */
  public void packToIntData (float[] floatData, int[] intData, Integer missing) {

    if (missing == null) packToInt (floatData, intData);
    else packToIntWithMissing (floatData, intData, missing);

  } // packToIntData

  ////////////////////////////////////////////////////////////

  private void unpackFromIntWithMissing (int[] intData, float[] floatData, int missing) {

    for (int i = 0; i < intData.length; i++) {
      if (intData[i] == missing)
        floatData[i] = Float.NaN;
      else
        floatData[i] = unpackFloat (intData[i]);
    } // for

  } // unpackFromIntWithMissing

  ////////////////////////////////////////////////////////////

  private void unpackFromInt (int[] intData, float[] floatData) {

    for (int i = 0; i < intData.length; i++) {
      floatData[i] = unpackFloat (intData[i]);
    } // for

  } // unpackFromInt

  ////////////////////////////////////////////////////////////

  private void packToIntWithMissing (float[] floatData, int[] intData, int missing) {

    for (int i = 0; i < floatData.length; i++) {
      if (!Float.isFinite (floatData[i]))
        intData[i] = missing;
      else
        intData[i] = (int) packFloat (floatData[i]);
    } // for

  } // packToIntWithMissing

  ////////////////////////////////////////////////////////////

  private void packToInt (float[] floatData, int[] intData) {

    for (int i = 0; i < floatData.length; i++) {
      intData[i] = (int) packFloat (floatData[i]);
    } // for

  } // packToInt

  ////////////////////////////////////////////////////////////

  /**
   * Packs a float value to an integer.
   *
   * @param value the float value to pack.
   *
   * @return the packed value as an integer, or 0 if the value is Float.NaN.
   */
  public int packFloat (float value) {

    int packed = Math.round (value/scale + offset);
    return (packed);
  
  } // packFloat

  ////////////////////////////////////////////////////////////

  /**
   * Unpacks an integer value to a float.
   *
   * @param packed the integer value to unpack.
   *
   * @return the unpacked float value.
   */
  public float unpackFloat (int packed) {

    float value = (packed - offset)*scale;
    return (value);

  } // unpackFloat

  ////////////////////////////////////////////////////////////

  /**
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) throws Exception {

    TestLogger logger = TestLogger.getInstance();
    logger.startClass (FloatPackingScheme.class);
    
    FloatPackingScheme scheme = new FloatPackingScheme();
    scheme.scale = 0.01f;
    scheme.offset = 3000.0f;
    float floatValue = 10.0f;
    int intValue = 4000;

    logger.test ("packFloat");
    assert (scheme.packFloat (floatValue) == intValue);
    logger.passed();

    logger.test ("unpackFloat");
    assert (scheme.unpackFloat (intValue) == floatValue);
    logger.passed();

  } // main
  
  ////////////////////////////////////////////////////////////

} // FloatPackingScheme class

////////////////////////////////////////////////////////////////////////


