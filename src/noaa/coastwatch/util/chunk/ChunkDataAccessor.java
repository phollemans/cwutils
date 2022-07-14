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
import java.util.logging.Logger;

// Testing
import noaa.coastwatch.test.TestLogger;
import java.util.function.IntFunction;
import java.util.function.BiFunction;

/**
 * <p>The <code>ChunkDataAccessor</code> class is a visitor that makes (possibly
 * unpacked) data values available from any type of {@link DataChunk} instance.
 * The type of data available after the visitor is accepted by a chunk
 * can be determined from the {@link DataChunk#getExternalType} method.</p>
 *
 * <p>The family of methods for accessing chunk data values in this
 * class takes the form <code>getXXXValue(int)</code> where
 * <code>XXX</code> is one of either Byte, Short, Int, Long, Float,
 * or Double.  The {@link #isMissingValue} method is used to determine if a
 * data value at a given index is invalid or missing, for any integer and
 * floating-point data.</p>
 *
 * @author Peter Hollemans
 * @since 3.4.0
 */
@noaa.coastwatch.test.Testable
public class ChunkDataAccessor implements ChunkVisitor {

  private static final Logger LOGGER = Logger.getLogger (ChunkDataAccessor.class.getName());

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

    // Scale data
    // ----------
    ScalingScheme scaling = chunk.getScalingScheme();
    if (scaling != null) {
      scaling.accept (new ScalingSchemeVisitor () {

        @Override
        public void visitFloatScalingScheme (FloatScalingScheme scheme) {
          var rawArray = floatArray;
          var scaledArray = (floatArray == chunk.getFloatData() ? new float[floatArray.length] : floatArray);
          scheme.scaleFloatData (rawArray, scaledArray);
          if (floatArray != scaledArray) floatArray = scaledArray;
        } // visitFloatScalingScheme

        @Override
        public void visitDoubleScalingScheme (DoubleScalingScheme scheme) {
          throw new RuntimeException ("Double scaling for float data not supported");
        } // visitDoublePackingScheme

      });
    } // if

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

    // Scale data
    // ----------
    ScalingScheme scaling = chunk.getScalingScheme();
    if (scaling != null) {
      scaling.accept (new ScalingSchemeVisitor () {

        @Override
        public void visitFloatScalingScheme (FloatScalingScheme scheme) {
          throw new RuntimeException ("Float scaling for double data not supported");
        } // visitFloatScalingScheme

        @Override
        public void visitDoubleScalingScheme (DoubleScalingScheme scheme) {
          var rawArray = doubleArray;
          var scaledArray = (doubleArray == chunk.getDoubleData() ? new double[doubleArray.length] : doubleArray);
          scheme.scaleDoubleData (rawArray, scaledArray);
          if (doubleArray != scaledArray) doubleArray = scaledArray;
        } // visitDoublePackingScheme

      });
    } // if

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
  
  // Helps check the correct chunk values for testing.  The chunk is accessed,
  // and then the expected values compared to the actual values using a
  // comparison function.
  private static <T> void checkAccess (
    ChunkDataAccessor access,
    DataChunk chunk,
    IntFunction<T> expected,
    IntFunction<T> actual,
    BiFunction<T,T,Boolean> compare
  ) {

    chunk.accept (access);
    for (int i = 0; i < chunk.getValues(); i++) {
      var expectedVal = expected.apply (i);
      var actualVal = actual.apply (i);
      assert (compare.apply (actualVal, expectedVal)) : actualVal + " != " + expectedVal + " at i = " + i;
    } // for

  } // checkAccess

  ////////////////////////////////////////////////////////////

  /**
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   *
   * @since 3.6.1
   */
  public static void main (String[] argv) throws Exception {

    TestLogger logger = TestLogger.getInstance();
    logger.startClass (ChunkDataAccessor.class);

    var access = new ChunkDataAccessor();
    var factory = DataChunkFactory.getInstance();

    var floatPacking = new FloatPackingScheme (0.1f, 1.0f);
    var doublePacking = new DoublePackingScheme (0.1, 1.0);

    var floatScaling = new FloatScalingScheme (0.1f, 1.0f);
    var doubleScaling = new DoubleScalingScheme (0.1, 1.0);

    float[] floatValues = new float[] {0, 0.1f, Float.NaN, 0.3f, 0.4f};
    double[] doubleValues = new double[] {0, 0.1, Double.NaN, 0.3, 0.4};

    BiFunction<Byte,Byte,Boolean> byteCompare = (a,b) -> (a == b);
    BiFunction<Short,Short,Boolean> shortCompare = (a,b) -> (a == b);
    BiFunction<Integer,Integer,Boolean> intCompare = (a,b) -> (a == b);
    BiFunction<Long,Long,Boolean> longCompare = (a,b) -> (a == b);
    BiFunction<Float,Float,Boolean> floatCompare = (a,b) -> a.isNaN() ? b.isNaN() : Math.abs (a - b) < 5*Math.ulp (a);
    BiFunction<Double,Double,Boolean> doubleCompare = (a,b) -> a.isNaN() ? b.isNaN() : Math.abs (a - b) < 5*Math.ulp (a);

    logger.test ("visitByteChunk");
    byte[] byteValues = new byte[] {1,2,3,4,5};
    byte byteMissing = 3;

    var chunk = factory.create (byteValues, false, byteMissing, null);
    checkAccess (access, chunk, i -> byteValues[i], i -> access.getByteValue (i), byteCompare);

    chunk = factory.create (byteValues, true, byteMissing, null);
    checkAccess (access, chunk, i -> (short) byteValues[i], i -> access.getShortValue (i), shortCompare);

    chunk = factory.create (byteValues, false, byteMissing, floatPacking);
    checkAccess (access, chunk, i -> floatValues[i], i -> access.getFloatValue (i), floatCompare);

    chunk = factory.create (byteValues, false, byteMissing, doublePacking);
    checkAccess (access, chunk, i -> doubleValues[i], i -> access.getDoubleValue (i), doubleCompare);

    logger.passed();

    logger.test ("visitShortChunk");
    short[] shortValues = new short[] {1,2,3,4,5};
    short shortMissing = 3;

    chunk = factory.create (shortValues, false, shortMissing, null);
    checkAccess (access, chunk, i -> shortValues[i], i -> access.getShortValue (i), shortCompare);

    chunk = factory.create (shortValues, true, shortMissing, null);
    checkAccess (access, chunk, i -> (int) shortValues[i], i -> access.getIntValue (i), intCompare);

    chunk = factory.create (shortValues, false, shortMissing, floatPacking);
    checkAccess (access, chunk, i -> floatValues[i], i -> access.getFloatValue (i), floatCompare);

    chunk = factory.create (shortValues, false, shortMissing, doublePacking);
    checkAccess (access, chunk, i -> doubleValues[i], i -> access.getDoubleValue (i), doubleCompare);

    logger.passed();

    logger.test ("visitIntChunk");
    int[] intValues = new int[] {1,2,3,4,5};
    int intMissing = 3;

    chunk = factory.create (intValues, false, intMissing, null);
    checkAccess (access, chunk, i -> intValues[i], i -> access.getIntValue (i), intCompare);

    chunk = factory.create (intValues, true, intMissing, null);
    checkAccess (access, chunk, i -> (long) intValues[i], i -> access.getLongValue (i), longCompare);

    chunk = factory.create (intValues, false, intMissing, floatPacking);
    checkAccess (access, chunk, i -> floatValues[i], i -> access.getFloatValue (i), floatCompare);

    chunk = factory.create (intValues, false, intMissing, doublePacking);
    checkAccess (access, chunk, i -> doubleValues[i], i -> access.getDoubleValue (i), doubleCompare);

    logger.passed();
    
    logger.test ("visitLongChunk");
    long[] longValues = new long[] {1,2,3,4,5};
    long longMissing = 3;

    chunk = factory.create (longValues, false, longMissing, null);
    checkAccess (access, chunk, i -> longValues[i], i -> access.getLongValue (i), longCompare);

    chunk = factory.create (longValues, true, longMissing, null);
    checkAccess (access, chunk, i -> (long) longValues[i], i -> access.getLongValue (i), longCompare);

    chunk = factory.create (longValues, false, longMissing, doublePacking);
    checkAccess (access, chunk, i -> doubleValues[i], i -> access.getDoubleValue (i), doubleCompare);

    logger.passed();
    
    logger.test ("visitFloatChunk");
    float[] rawFloatValues = new float[] {1,2,3,4,5};
    float floatMissing = 3;

    chunk = factory.create (rawFloatValues, false, floatMissing, null, null);
    checkAccess (access, chunk, i -> rawFloatValues[i] == floatMissing ? Float.NaN : rawFloatValues[i], i -> access.getFloatValue (i), floatCompare);

    chunk = factory.create (rawFloatValues, false, floatMissing, null, floatScaling);
    checkAccess (access, chunk, i -> floatValues[i], i -> access.getFloatValue (i), floatCompare);

    logger.passed();
    
    logger.test ("visitDoubleChunk");
    double[] rawDoubleValues = new double[] {1,2,3,4,5};
    double doubleMissing = 3;

    chunk = factory.create (rawDoubleValues, false, doubleMissing, null, null);
    checkAccess (access, chunk, i -> rawDoubleValues[i] == doubleMissing ? Double.NaN : rawDoubleValues[i], i -> access.getDoubleValue (i), doubleCompare);

    chunk = factory.create (rawDoubleValues, false, doubleMissing, null, doubleScaling);
    checkAccess (access, chunk, i -> doubleValues[i], i -> access.getDoubleValue (i), doubleCompare);

    logger.passed();

  } // main
  
  ////////////////////////////////////////////////////////////

} // ChunkDataAccessor class

////////////////////////////////////////////////////////////////////////

