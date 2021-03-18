////////////////////////////////////////////////////////////////////////
/*

     File: DoublePackingScheme.java
   Author: Peter Hollemans
     Date: 2017/11/25

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
 * The <code>DoublePackingScheme</code> class implements a scale and offset
 * packing scheme for primitive double data.  Double values are packed as
 * <code>integer = double/scale + offset</code> and then rounded to the nearest
 * long integer.
 *
 * @author Peter Hollemans
 * @since 3.4.0
 */
@noaa.coastwatch.test.Testable
public class DoublePackingScheme implements PackingScheme {

  /** The scaling factor for double data. */
  public double scale;

  /** The offset for float data. */
  public double offset;
  
  ////////////////////////////////////////////////////////////

  @Override
  public DataType getUnpackedType() { return (DataType.DOUBLE); }

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new packing instance.
   *
   * @param scale the scaling factor.
   * @param offset the offset value.
   *
   * @since 3.6.1
   */
  public DoublePackingScheme (double scale, double offset) {
  
    this.scale = scale;
    this.offset = offset;
  
  } // DoublePackingScheme

  ////////////////////////////////////////////////////////////

  @Override
  public void accept (PackingSchemeVisitor visitor) {

    visitor.visitDoublePackingScheme (this);

  } // accept
  
  ////////////////////////////////////////////////////////////

  /**
   * Unpacks primitive data from a byte array into a double array.
   *
   * @param byteData the source array to unpack.
   * @param doubleData the destination array to unpack into.
   * @param missing the missing value to detect (or null for none).
   * Values in the source array that match the missing value will be
   * assigned Double.NaN in the destination array.
   * @param isUnsigned the unsigned flag, true if the data is unsigned
   * or false if not.
   */
  public void unpackFromByteData (byte[] byteData, double[] doubleData, Byte missing, boolean isUnsigned) {

    if (missing == null) {
      if (isUnsigned) unpackFromUByte (byteData, doubleData);
      else unpackFromByte (byteData, doubleData);
    } // if
    else {
      if (isUnsigned) unpackFromUByteWithMissing (byteData, doubleData, missing);
      else unpackFromByteWithMissing (byteData, doubleData, missing);
    } // else

  } // unpackFromByteData

  ////////////////////////////////////////////////////////////

  /**
   * Packs primitive data from a double array into a byte array.
   *
   * @param doubleData the source array to pack.
   * @param byteData the destination array to pack into.
   * @param missing the missing value to detect (or null for none).
   * Non-finite (NaN and infinity values) in the source array will be
   * assigned the missing value in the destination array.
   * @param isUnsigned the unsigned flag, true if the destination data is
   * unsigned or false if not.
   */
  public void packToByteData (double[] doubleData, byte[] byteData, Byte missing, boolean isUnsigned) {

    if (missing == null) {
      if (isUnsigned) packToUByte (doubleData, byteData);
      else packToByte (doubleData, byteData);
    } // if
    else {
      if (isUnsigned) packToUByteWithMissing (doubleData, byteData, missing);
      else packToByteWithMissing (doubleData, byteData, missing);
    } // else

  } // packToByteData

  ////////////////////////////////////////////////////////////

  private void unpackFromByteWithMissing (byte[] byteData, double[] doubleData, byte missing) {

    for (int i = 0; i < byteData.length; i++) {
      if (byteData[i] == missing)
        doubleData[i] = Double.NaN;
      else
        doubleData[i] = unpackDouble (byteData[i]);
    } // for

  } // unpackFromByteWithMissing

  ////////////////////////////////////////////////////////////

  private void unpackFromByte (byte[] byteData, double[] doubleData) {

    for (int i = 0; i < byteData.length; i++) {
      doubleData[i] = unpackDouble (byteData[i]);
    } // for

  } // unpackFromByte

  ////////////////////////////////////////////////////////////

  private void packToByteWithMissing (double[] doubleData, byte[] byteData, byte missing) {

    for (int i = 0; i < doubleData.length; i++) {
      if (!Double.isFinite (doubleData[i]))
        byteData[i] = missing;
      else
        byteData[i] = (byte) packDouble (doubleData[i]);
    } // for

  } // packToByteWithMissing

  ////////////////////////////////////////////////////////////

  private void packToByte (double[] doubleData, byte[] byteData) {

    for (int i = 0; i < doubleData.length; i++) {
      byteData[i] = (byte) packDouble (doubleData[i]);
    } // for

  } // packToByte

  ////////////////////////////////////////////////////////////

  private void unpackFromUByteWithMissing (byte[] byteData, double[] doubleData, byte missing) {

    for (int i = 0; i < byteData.length; i++) {
      if (byteData[i] == missing)
        doubleData[i] = Double.NaN;
      else
        doubleData[i] = unpackDouble ((short) (byteData[i] & 0xff));
    } // for

  } // unpackFromUByteWithMissing

  ////////////////////////////////////////////////////////////

  private void unpackFromUByte (byte[] byteData, double[] doubleData) {

    for (int i = 0; i < byteData.length; i++) {
      doubleData[i] = unpackDouble ((short) (byteData[i] & 0xff));
    } // for

  } // unpackFromUByte

  ////////////////////////////////////////////////////////////

  private void packToUByteWithMissing (double[] doubleData, byte[] byteData, byte missing) {

    for (int i = 0; i < doubleData.length; i++) {
      if (!Double.isFinite (doubleData[i]))
        byteData[i] = missing;
      else
        byteData[i] = (byte) (packDouble (doubleData[i]) & 0xff);
    } // for

  } // packToUByteWithMissing

  ////////////////////////////////////////////////////////////

  private void packToUByte (double[] doubleData, byte[] byteData) {

    for (int i = 0; i < doubleData.length; i++) {
      byteData[i] = (byte) (packDouble (doubleData[i]) & 0xff);
    } // for

  } // packToUByte

  ////////////////////////////////////////////////////////////

  /**
   * Unpacks primitive data from a short array into a double array.
   *
   * @param shortData the source array to unpack.
   * @param doubleData the destination array to unpack into.
   * @param missing the missing value to detect (or null for none).
   * Values in the source array that match the missing value will be
   * assigned Double.NaN in the destination array.
   * @param isUnsigned the unsigned flag, true if the data is unsigned
   * or false if not.
   */
  public void unpackFromShortData (short[] shortData, double[] doubleData, Short missing, boolean isUnsigned) {

    if (missing == null) {
      if (isUnsigned) unpackFromUShort (shortData, doubleData);
      else unpackFromShort (shortData, doubleData);
    } // if
    else {
      if (isUnsigned) unpackFromUShortWithMissing (shortData, doubleData, missing);
      else unpackFromShortWithMissing (shortData, doubleData, missing);
    } // else

  } // unpackFromShortData

  ////////////////////////////////////////////////////////////

  /**
   * Packs primitive data from a double array into a short array.
   *
   * @param doubleData the source array to pack.
   * @param shortData the destination array to pack into.
   * @param missing the missing value to detect (or null for none).
   * Non-finite (NaN and infinity values) in the source array will be
   * assigned the missing value in the destination array.
   * @param isUnsigned the unsigned flag, true if the destination data is
   * unsigned or false if not.
   */
  public void packToShortData (double[] doubleData, short[] shortData, Short missing, boolean isUnsigned) {

    if (missing == null) {
      if (isUnsigned) packToUShort (doubleData, shortData);
      else packToShort (doubleData, shortData);
    } // if
    else {
      if (isUnsigned) packToUShortWithMissing (doubleData, shortData, missing);
      else packToShortWithMissing (doubleData, shortData, missing);
    } // else

  } // packToShortData

  ////////////////////////////////////////////////////////////

  private void unpackFromShortWithMissing (short[] shortData, double[] doubleData, short missing) {

    for (int i = 0; i < shortData.length; i++) {
      if (shortData[i] == missing)
        doubleData[i] = Double.NaN;
      else
        doubleData[i] = unpackDouble (shortData[i]);
    } // for

  } // unpackFromShortWithMissing

  ////////////////////////////////////////////////////////////

  private void unpackFromShort (short[] shortData, double[] doubleData) {

    for (int i = 0; i < shortData.length; i++) {
      doubleData[i] = unpackDouble (shortData[i]);
    } // for

  } // unpackFromShort

  ////////////////////////////////////////////////////////////

  private void packToShortWithMissing (double[] doubleData, short[] shortData, short missing) {

    for (int i = 0; i < doubleData.length; i++) {
      if (!Double.isFinite (doubleData[i]))
        shortData[i] = missing;
      else
        shortData[i] = (short) packDouble (doubleData[i]);
    } // for

  } // packToShortWithMissing

  ////////////////////////////////////////////////////////////

  private void packToShort (double[] doubleData, short[] shortData) {

    for (int i = 0; i < doubleData.length; i++) {
      shortData[i] = (short) packDouble (doubleData[i]);
    } // for

  } // packToShort

  ////////////////////////////////////////////////////////////

  private void unpackFromUShortWithMissing (short[] shortData, double[] doubleData, short missing) {

    for (int i = 0; i < shortData.length; i++) {
      if (shortData[i] == missing)
        doubleData[i] = Double.NaN;
      else
        doubleData[i] = unpackDouble ((int) (shortData[i] & 0xffff));
    } // for

  } // unpackFromUShortWithMissing

  ////////////////////////////////////////////////////////////

  private void unpackFromUShort (short[] shortData, double[] doubleData) {

    for (int i = 0; i < shortData.length; i++) {
      doubleData[i] = unpackDouble ((int) (shortData[i] & 0xffff));
    } // for

  } // unpackFromUShort

  ////////////////////////////////////////////////////////////

  private void packToUShortWithMissing (double[] doubleData, short[] shortData, short missing) {

    for (int i = 0; i < doubleData.length; i++) {
      if (!Double.isFinite (doubleData[i]))
        shortData[i] = missing;
      else
        shortData[i] = (short) (packDouble (doubleData[i]) & 0xffff);
    } // for

  } // packToUShortWithMissing

  ////////////////////////////////////////////////////////////

  private void packToUShort (double[] doubleData, short[] shortData) {

    for (int i = 0; i < doubleData.length; i++) {
      shortData[i] = (short) (packDouble (doubleData[i]) & 0xffff);
    } // for

  } // packToUShort

  ////////////////////////////////////////////////////////////

  /**
   * Unpacks primitive data from a int array into a double array.
   *
   * @param intData the source array to unpack.
   * @param doubleData the destination array to unpack into.
   * @param missing the missing value to detect (or null for none).
   * Values in the source array that match the missing value will be
   * assigned Double.NaN in the destination array.
   * @param isUnsigned the unsigned flag, true if the data is unsigned
   * or false if not.
   */
  public void unpackFromIntData (int[] intData, double[] doubleData, Integer missing, boolean isUnsigned) {

    if (missing == null) {
      if (isUnsigned) unpackFromUInt (intData, doubleData);
      else unpackFromInt (intData, doubleData);
    } // if
    else {
      if (isUnsigned) unpackFromUIntWithMissing (intData, doubleData, missing);
      else unpackFromIntWithMissing (intData, doubleData, missing);
    } // else

  } // unpackFromIntData

  ////////////////////////////////////////////////////////////

  /**
   * Packs primitive data from a double array into a int array.
   *
   * @param doubleData the source array to pack.
   * @param intData the destination array to pack into.
   * @param missing the missing value to detect (or null for none).
   * Non-finite (NaN and infinity values) in the source array will be
   * assigned the missing value in the destination array.
   * @param isUnsigned the unsigned flag, true if the destination data is
   * unsigned or false if not.
   */
  public void packToIntData (double[] doubleData, int[] intData, Integer missing, boolean isUnsigned) {

    if (missing == null) {
      if (isUnsigned) packToUInt (doubleData, intData);
      else packToInt (doubleData, intData);
    } // if
    else {
      if (isUnsigned) packToUIntWithMissing (doubleData, intData, missing);
      else packToIntWithMissing (doubleData, intData, missing);
    } // else

  } // packToIntData

  ////////////////////////////////////////////////////////////

  private void unpackFromIntWithMissing (int[] intData, double[] doubleData, int missing) {

    for (int i = 0; i < intData.length; i++) {
      if (intData[i] == missing)
        doubleData[i] = Double.NaN;
      else
        doubleData[i] = unpackDouble (intData[i]);
    } // for

  } // unpackFromIntWithMissing

  ////////////////////////////////////////////////////////////

  private void unpackFromInt (int[] intData, double[] doubleData) {

    for (int i = 0; i < intData.length; i++) {
      doubleData[i] = unpackDouble (intData[i]);
    } // for

  } // unpackFromInt

  ////////////////////////////////////////////////////////////

  private void packToIntWithMissing (double[] doubleData, int[] intData, int missing) {

    for (int i = 0; i < doubleData.length; i++) {
      if (!Double.isFinite (doubleData[i]))
        intData[i] = missing;
      else
        intData[i] = (int) packDouble (doubleData[i]);
    } // for

  } // packToIntWithMissing

  ////////////////////////////////////////////////////////////

  private void packToInt (double[] doubleData, int[] intData) {

    for (int i = 0; i < doubleData.length; i++) {
      intData[i] = (int) packDouble (doubleData[i]);
    } // for

  } // packToInt

  ////////////////////////////////////////////////////////////

  private void unpackFromUIntWithMissing (int[] intData, double[] doubleData, int missing) {

    for (int i = 0; i < intData.length; i++) {
      if (intData[i] == missing)
        doubleData[i] = Double.NaN;
      else
        doubleData[i] = unpackDouble ((long) (intData[i] & 0xffffffff));
    } // for

  } // unpackFromUIntWithMissing

  ////////////////////////////////////////////////////////////

  private void unpackFromUInt (int[] intData, double[] doubleData) {

    for (int i = 0; i < intData.length; i++) {
      doubleData[i] = unpackDouble ((long) (intData[i] & 0xffffffff));
    } // for

  } // unpackFromUInt

  ////////////////////////////////////////////////////////////

  private void packToUIntWithMissing (double[] doubleData, int[] intData, int missing) {

    for (int i = 0; i < doubleData.length; i++) {
      if (!Double.isFinite (doubleData[i]))
        intData[i] = missing;
      else
        intData[i] = (int) (packDouble (doubleData[i]) & 0xffffffff);
    } // for

  } // packToUIntWithMissing

  ////////////////////////////////////////////////////////////

  private void packToUInt (double[] doubleData, int[] intData) {

    for (int i = 0; i < doubleData.length; i++) {
      intData[i] = (int) (packDouble (doubleData[i]) & 0xffffffff);
    } // for

  } // packToUInt

  ////////////////////////////////////////////////////////////

  /**
   * Unpacks primitive data from a long array into a double array.
   *
   * @param longData the source array to unpack.
   * @param doubleData the destination array to unpack into.
   * @param missing the missing value to detect (or null for none).
   * Values in the source array that match the missing value will be
   * assigned Double.NaN in the destination array.
   */
  public void unpackFromLongData (long[] longData, double[] doubleData, Long missing) {

    if (missing == null) unpackFromLong (longData, doubleData);
    else unpackFromLongWithMissing (longData, doubleData, missing);

  } // unpackFromLongData

  ////////////////////////////////////////////////////////////

  /**
   * Packs primitive data from a double array into a long array.
   *
   * @param doubleData the source array to pack.
   * @param longData the destination array to pack into.
   * @param missing the missing value to detect (or null for none).
   * Non-finite (NaN and infinity values) in the source array will be
   * assigned the missing value in the destination array.
   */
  public void packToLongData (double[] doubleData, long[] longData, Long missing) {

    if (missing == null) packToLong (doubleData, longData);
    else packToLongWithMissing (doubleData, longData, missing);

  } // packToLongData

  ////////////////////////////////////////////////////////////

  private void unpackFromLongWithMissing (long[] longData, double[] doubleData, long missing) {

    for (int i = 0; i < longData.length; i++) {
      if (longData[i] == missing)
        doubleData[i] = Double.NaN;
      else
        doubleData[i] = unpackDouble (longData[i]);
    } // for

  } // unpackFromLongWithMissing

  ////////////////////////////////////////////////////////////

  private void unpackFromLong (long[] longData, double[] doubleData) {

    for (int i = 0; i < longData.length; i++) {
      doubleData[i] = unpackDouble (longData[i]);
    } // for

  } // unpackFromLong

  ////////////////////////////////////////////////////////////

  private void packToLongWithMissing (double[] doubleData, long[] longData, long missing) {

    for (int i = 0; i < doubleData.length; i++) {
      if (!Double.isFinite (doubleData[i]))
        longData[i] = missing;
      else
        longData[i] = (long) packDouble (doubleData[i]);
    } // for

  } // packToLongWithMissing

  ////////////////////////////////////////////////////////////

  private void packToLong (double[] doubleData, long[] longData) {

    for (int i = 0; i < doubleData.length; i++) {
      longData[i] = (long) packDouble (doubleData[i]);
    } // for

  } // packToLong

  ////////////////////////////////////////////////////////////

  /**
   * Packs a double value to an integer.
   *
   * @param value the double value to pack.
   *
   * @return the packed value as a long integer or 0 if the value is Double.NaN.
   */
  public long packDouble (double value) {

    long packed = Math.round (value/scale + offset);
    return (packed);
  
  } // packDouble

  ////////////////////////////////////////////////////////////

  /**
   * Unpacks a long integer value to a double.
   *
   * @param packed the long integer value to unpack.
   *
   * @return the unpacked double value.
   */
  public double unpackDouble (long packed) {

    double value = (packed - offset)*scale;
    return (value);

  } // unpackDouble

  ////////////////////////////////////////////////////////////

  /**
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) throws Exception {

    TestLogger logger = TestLogger.getInstance();
    logger.startClass (DoublePackingScheme.class);
    
    DoublePackingScheme scheme = new DoublePackingScheme (0.01, 3000.0);
    double doubleValue = 10.0;
    long longValue = 4000;

    logger.test ("packDouble");
    assert (scheme.packDouble (doubleValue) == longValue);
    logger.passed();

    logger.test ("unpackDouble");
    assert (scheme.unpackDouble (longValue) == doubleValue);
    logger.passed();

  } // main
  
  ////////////////////////////////////////////////////////////

} // DoublePackingScheme class

////////////////////////////////////////////////////////////////////////



