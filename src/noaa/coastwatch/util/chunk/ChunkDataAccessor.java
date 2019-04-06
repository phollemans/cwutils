////////////////////////////////////////////////////////////////////////
/*

     File: ChunkDataAccessor.java
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

/**
 * The <code>ChunkDataAccessor</code> class is a visitor that makes (possibly
 * unpacked) data values available from any type of {@link DataChunk} instance.
 * The type of data available after the visitor is accepted by a chunk
 * can be determined from the {@link DataChunk#getExternalType} method.<p>
 *
 * The family of methods for accessing chunk data values in this
 * class takes the form <code>getXXXValue(int)</code> where
 * <code>XXX</code> is one of either Byte, Short, Int, Long, Float,
 * or Double.  The {@link #isMissingValue} method is used to determine if a
 * data value at a given index is invalid or missing, for any integer and
 * floating-point data.<p>
 *
 * @author Peter Hollemans
 * @since 3.4.0
 */
public class ChunkDataAccessor implements ChunkVisitor {

  /** The array of byte values. */
  private byte[] byteArray;

  /** The array of short values. */
  private short[] shortArray;
  
  /** The array of int values. */
  private int[] intArray;

  /** The array of long values. */
  private long[] longArray;

  /** The array of float values. */
  private float[] floatArray;

  /** The array of double values. */
  private double[] doubleArray;

  /** The array of missing value flags. */
  private boolean[] isMissingArray;

  ////////////////////////////////////////////////////////////

  @Override
  public void visitByteChunk (ByteChunk chunk) {

    // Compute missing flags
    // ---------------------
    byteArray = chunk.getByteData();
    isMissingArray = new boolean[byteArray.length];
    Byte missing = chunk.getMissing();
    if (missing != null) {
      byte missingValue = missing;
      for (int i = 0; i < byteArray.length; i++) { isMissingArray[i] = (byteArray[i] == missingValue); }
    } // if

    // Unpack data from byte
    // ---------------------
    PackingScheme packing = chunk.getPackingScheme();
    if (packing != null) {
      packing.accept (new PackingSchemeVisitor () {

        @Override
        public void visitFloatPackingScheme (FloatPackingScheme scheme) {
          floatArray = new float[chunk.getValues()];
          scheme.unpackFromByteData (byteArray, floatArray, chunk.getMissing(), chunk.isUnsigned());
        } // visitFloatPackingScheme

        @Override
        public void visitDoublePackingScheme (DoublePackingScheme scheme) {
          doubleArray = new double[chunk.getValues()];
          scheme.unpackFromByteData (byteArray, doubleArray, chunk.getMissing(), chunk.isUnsigned());
        } // visitDoublePackingScheme

      });
      byteArray = null;
    } // if

    // Handle unpacked byte data
    // -------------------------
    else {
    
      // Convert unsigned data to short
      // ------------------------------
      if (chunk.isUnsigned()) {
        shortArray = new short[byteArray.length];
        for (int i = 0; i < byteArray.length; i++) { shortArray[i] = (short) (byteArray[i] & 0xff); }
        byteArray = null;
      } // if

    } // else
  
  } // visitByteChunk

  ////////////////////////////////////////////////////////////

  @Override
  public void visitShortChunk (ShortChunk chunk) {

    // Compute missing flags
    // ---------------------
    shortArray = chunk.getShortData();
    isMissingArray = new boolean[shortArray.length];
    Short missing = chunk.getMissing();
    if (missing != null) {
      short missingValue = missing;
      for (int i = 0; i < shortArray.length; i++) { isMissingArray[i] = (shortArray[i] == missingValue); }
    } // if

    // Unpack data from short
    // ----------------------
    PackingScheme packing = chunk.getPackingScheme();
    if (packing != null) {
      packing.accept (new PackingSchemeVisitor () {

        @Override
        public void visitFloatPackingScheme (FloatPackingScheme scheme) {
          floatArray = new float[chunk.getValues()];
          scheme.unpackFromShortData (shortArray, floatArray, chunk.getMissing(), chunk.isUnsigned());
        } // visitFloatPackingScheme

        @Override
        public void visitDoublePackingScheme (DoublePackingScheme scheme) {
          doubleArray = new double[chunk.getValues()];
          scheme.unpackFromShortData (shortArray, doubleArray, chunk.getMissing(), chunk.isUnsigned());
        } // visitDoublePackingScheme

      });
      shortArray = null;
    } // if

    // Handle unpacked short data
    // --------------------------
    else {

      // Convert unsigned data to int
      // ----------------------------
      if (chunk.isUnsigned()) {
        intArray = new int[shortArray.length];
        for (int i = 0; i < shortArray.length; i++) { intArray[i] = (int) (shortArray[i] & 0xffff); }
        shortArray = null;
      } // if

    } // else

  } // visitShortChunk

  ////////////////////////////////////////////////////////////

  @Override
  public void visitIntChunk (IntChunk chunk) {

    // Compute missing flags
    // ---------------------
    intArray = chunk.getIntData();
    isMissingArray = new boolean[intArray.length];
    Integer missing = chunk.getMissing();
    if (missing != null) {
      int missingValue = missing;
      for (int i = 0; i < intArray.length; i++) { isMissingArray[i] = (intArray[i] == missingValue); }
    } // if

    // Unpack data from int
    // --------------------
    PackingScheme packing = chunk.getPackingScheme();
    if (packing != null) {
      packing.accept (new PackingSchemeVisitor () {

        @Override
        public void visitFloatPackingScheme (FloatPackingScheme scheme) {
          if (chunk.isUnsigned()) throw new RuntimeException ("Unpacking unsigned int to float not supported");
          floatArray = new float[chunk.getValues()];
          scheme.unpackFromIntData (intArray, floatArray, chunk.getMissing());
        } // visitFloatPackingScheme

        @Override
        public void visitDoublePackingScheme (DoublePackingScheme scheme) {
          doubleArray = new double[chunk.getValues()];
          scheme.unpackFromIntData (intArray, doubleArray, chunk.getMissing(), chunk.isUnsigned());
        } // visitDoublePackingScheme

      });
      intArray = null;
    } // if

    // Handle unpacked int data
    // ------------------------
    else {

      // Convert unsigned data to long
      // -----------------------------
      if (chunk.isUnsigned()) {
        longArray = new long[intArray.length];
        for (int i = 0; i < intArray.length; i++) { longArray[i] = (long) (intArray[i] & 0xffffffff); }
        intArray = null;
      } // if

    } // else

  } // visitIntChunk

  ////////////////////////////////////////////////////////////

  @Override
  public void visitLongChunk (LongChunk chunk) {

    // Compute missing flags
    // ---------------------
    longArray = chunk.getLongData();
    isMissingArray = new boolean[longArray.length];
    Long missing = chunk.getMissing();
    if (missing != null) {
      long missingValue = missing;
      for (int i = 0; i < longArray.length; i++) { isMissingArray[i] = (longArray[i] == missingValue); }
    } // if

    // Unpack data from long
    // ---------------------
    PackingScheme packing = chunk.getPackingScheme();
    if (packing != null) {
      packing.accept (new PackingSchemeVisitor () {

        @Override
        public void visitFloatPackingScheme (FloatPackingScheme scheme) {
          throw new RuntimeException ("Unpacking long to float not supported");
        } // visitFloatPackingScheme

        @Override
        public void visitDoublePackingScheme (DoublePackingScheme scheme) {
          if (chunk.isUnsigned()) throw new RuntimeException ("Unpacking unsigned long to double not supported");
          doubleArray = new double[chunk.getValues()];
          scheme.unpackFromLongData (longArray, doubleArray, chunk.getMissing());
        } // visitDoublePackingScheme

      });
      longArray = null;
    } // if

    // Handle unpacked long data
    // ------------------------
    else {

      // Convert unsigned data to long
      // -----------------------------
      if (chunk.isUnsigned()) {

// TODO: Should we issue a warning here that Java doesn't support unsigned long?

//        longArray = new long[longArray.length];
//        for (int i = 0; i < longArray.length; i++) { longArray[i] = (long) (longArray[i] & 0xffffffff); }
//        longArray = null;

      } // if

    } // else

  } // visitLongChunk

  ////////////////////////////////////////////////////////////

  @Override
  public void visitFloatChunk (FloatChunk chunk) {

    // Get float data
    // --------------
    floatArray = chunk.getFloatData();

    // Copy values and flag missing
    // ----------------------------
    Float missing = chunk.getMissing();
    isMissingArray = new boolean[floatArray.length];
    if (missing != null && !missing.isNaN()) {
      float missingValue = missing;
      float[] newFloatArray = new float[floatArray.length];
      for (int i = 0; i < floatArray.length; i++) {
        if (floatArray[i] == missingValue) {
          newFloatArray[i] = Float.NaN;
          isMissingArray[i] = true;
        } // if
        else {
          newFloatArray[i] = floatArray[i];
        } // else
      } // for
      floatArray = newFloatArray;
    } // if
    else {
      for (int i = 0; i < floatArray.length; i++) {
        isMissingArray[i] = Float.isNaN (floatArray[i]);
      } // for
    } // else

  } // visitFloatChunk

  ////////////////////////////////////////////////////////////

  @Override
  public void visitDoubleChunk (DoubleChunk chunk) {

    // Get double data
    // --------------
    doubleArray = chunk.getDoubleData();

    // Copy values and flag missing
    // ----------------------------
    Double missing = chunk.getMissing();
    isMissingArray = new boolean[doubleArray.length];
    if (missing != null && !missing.isNaN()) {
      double missingValue = missing;
      double[] newDoubleArray = new double[doubleArray.length];
      for (int i = 0; i < doubleArray.length; i++) {
        if (doubleArray[i] == missingValue) {
          newDoubleArray[i] = Double.NaN;
          isMissingArray[i] = true;
        } // if
        else {
          newDoubleArray[i] = doubleArray[i];
        } // else
      } // for
      doubleArray = newDoubleArray;
    } // if
    else {
      for (int i = 0; i < doubleArray.length; i++) {
        isMissingArray[i] = Double.isNaN (doubleArray[i]);
      } // for
    } // else

  } // visitDoubleChunk

  ////////////////////////////////////////////////////////////

  public boolean isMissingValue (int index) { return (isMissingArray[index]); }
  public byte getByteValue (int index) { return (byteArray[index]); }
  public short getShortValue (int index) { return (shortArray[index]); }
  public int getIntValue (int index) { return (intArray[index]); }
  public long getLongValue (int index) { return (longArray[index]); }
  public float getFloatValue (int index) { return (floatArray[index]); }
  public double getDoubleValue (int index) { return (doubleArray[index]); }

  ////////////////////////////////////////////////////////////

} // ChunkDataAccessor class

////////////////////////////////////////////////////////////////////////

