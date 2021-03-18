////////////////////////////////////////////////////////////////////////
/*

     File: ChunkDataModifier.java
   Author: Peter Hollemans
     Date: 2017/12/03

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

// Testing
import noaa.coastwatch.test.TestLogger;
import java.util.Comparator;
import java.util.Arrays;

/**
 * The <code>ChunkDataModifier</code> class is a visitor that modifies
 * data values in any type of {@link DataChunk} instance.  The family of methods
 * in this class for specifying the source primitive data to use takes the
 * form <code>setXXXData(xxx[])</code> where <code>XXX</code> is one of
 * either Byte, Short, Int, Long, Float, or Double and <code>xxx</code> is
 * byte, short, int, float, or double.  If the source primitive data is
 * integer-valued (byte, short, int, long) and certain values should be marked
 * as missing in the chunk data, the {@link #setMissingData} method should be
 * used to mark which values are missing.  In the case of float and double
 * primitive data, the values are checked for NaN values and those
 * values marked as missing in the chunk.  No such standard sentinel
 * values exist for integer-valued data, so missing data must be marked as
 * missing separately from the data itself.
 *
 * @author Peter Hollemans
 * @since 3.4.0
 */
@noaa.coastwatch.test.Testable
public class ChunkDataModifier implements ChunkVisitor {

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

  public void setMissingData (boolean[] isMissingArray) { this.isMissingArray = isMissingArray; }
  public void setByteData (byte[] byteArray) { this.byteArray = byteArray; }
  public void setShortData (short[] shortArray) { this.shortArray = shortArray; }
  public void setIntData (int[] intArray) { this.intArray = intArray; }
  public void setLongData (long[] longArray) { this.longArray = longArray; }
  public void setFloatData (float[] floatArray) { this.floatArray = floatArray; }
  public void setDoubleData (double[] doubleArray) { this.doubleArray = doubleArray; }

  ////////////////////////////////////////////////////////////

  @Override
  public void visitByteChunk (ByteChunk chunk) {

    // Pack data to byte
    // -----------------
    PackingScheme packing = chunk.getPackingScheme();
    if (packing != null) {
      packing.accept (new PackingSchemeVisitor () {

        @Override
        public void visitFloatPackingScheme (FloatPackingScheme scheme) {
          if (floatArray == null) throw new RuntimeException ("No float data available for packing (type mismatch)");
          byte[] byteData = chunk.getByteData();
          scheme.packToByteData (floatArray, byteData, chunk.getMissing(), chunk.isUnsigned());
        } // visitFloatPackingScheme

        @Override
        public void visitDoublePackingScheme (DoublePackingScheme scheme) {
          if (doubleArray == null) throw new RuntimeException ("No double data available for packing (type mismatch)");
          byte[] byteData = chunk.getByteData();
          scheme.packToByteData (doubleArray, byteData, chunk.getMissing(), chunk.isUnsigned());
        } // visitDoublePackingScheme

      });
    } // if

    // Copy byte data
    // --------------
    else {

      // Convert short data to unsigned byte
      // -----------------------------------
      if (chunk.isUnsigned() && shortArray != null) {
        byteArray = new byte[shortArray.length];
        for (int i = 0; i < shortArray.length; i++) byteArray[i] = (byte) (shortArray[i] & 0xff);
        shortArray = null;
      } // if

      // Check for byte data
      // -------------------
      else {
        if (byteArray == null) throw new RuntimeException ("No byte data available (type mismatch)");
      } // else

      // Copy byte values into chunk
      // ---------------------------
      byte[] byteData = chunk.getByteData();
      Byte missing = chunk.getMissing();
      if (missing != null && isMissingArray != null) {
        byte missingValue = missing;
        for (int i = 0; i < byteData.length; i++) { byteData[i] = (isMissingArray[i] ? missingValue : byteArray[i]); }
      } // if
      else {
        for (int i = 0; i < byteData.length; i++) { byteData[i] = byteArray[i]; }
      } // else

    } // else

  } // visitByteChunk

  ////////////////////////////////////////////////////////////

  @Override
  public void visitShortChunk (ShortChunk chunk) {

    // Pack data to short
    // ------------------
    PackingScheme packing = chunk.getPackingScheme();
    if (packing != null) {
      packing.accept (new PackingSchemeVisitor () {

        @Override
        public void visitFloatPackingScheme (FloatPackingScheme scheme) {
          if (floatArray == null) throw new RuntimeException ("No float data available for packing (type mismatch)");
          short[] shortData = chunk.getShortData();
          scheme.packToShortData (floatArray, shortData, chunk.getMissing(), chunk.isUnsigned());
        } // visitFloatPackingScheme

        @Override
        public void visitDoublePackingScheme (DoublePackingScheme scheme) {
          if (doubleArray == null) throw new RuntimeException ("No double data available for packing (type mismatch)");
          short[] shortData = chunk.getShortData();
          scheme.packToShortData (doubleArray, shortData, chunk.getMissing(), chunk.isUnsigned());
        } // visitDoublePackingScheme

      });
    } // if

    // Copy short data
    // ---------------
    else {

      // Convert int data to unsigned short
      // ----------------------------------
      if (chunk.isUnsigned() && intArray != null) {
        shortArray = new short[intArray.length];
        for (int i = 0; i < intArray.length; i++) shortArray[i] = (short) (intArray[i] & 0xffff);
        intArray = null;
      } // if
      
      // Check for short data
      // --------------------
      else {
        if (shortArray == null) throw new RuntimeException ("No short data available (type mismatch)");
      } // else

      // Copy short values into chunk
      // ----------------------------
      short[] shortData = chunk.getShortData();
      Short missing = chunk.getMissing();
      if (missing != null && isMissingArray != null) {
        short missingValue = missing;
        for (int i = 0; i < shortData.length; i++) { shortData[i] = (isMissingArray[i] ? missingValue : shortArray[i]); }
      } // if
      else {
        for (int i = 0; i < shortData.length; i++) { shortData[i] = shortArray[i]; }
      } // else
      
    } // else

  } // visitShortChunk

  ////////////////////////////////////////////////////////////

  @Override
  public void visitIntChunk (IntChunk chunk) {

    // Pack data to int
    // ----------------
    PackingScheme packing = chunk.getPackingScheme();
    if (packing != null) {
      packing.accept (new PackingSchemeVisitor () {

        @Override
        public void visitFloatPackingScheme (FloatPackingScheme scheme) {
          if (floatArray == null) throw new RuntimeException ("No float data available for packing (type mismatch)");
          if (chunk.isUnsigned()) throw new RuntimeException ("Packing float to unsigned int not supported");
          int[] intData = chunk.getIntData();
          scheme.packToIntData (floatArray, intData, chunk.getMissing());
        } // visitFloatPackingScheme

        @Override
        public void visitDoublePackingScheme (DoublePackingScheme scheme) {
          if (doubleArray == null) throw new RuntimeException ("No double data available for packing (type mismatch)");
          int[] intData = chunk.getIntData();
          scheme.packToIntData (doubleArray, intData, chunk.getMissing(), chunk.isUnsigned());
        } // visitDoublePackingScheme

      });
    } // if

    // Copy int data
    // -------------
    else {
    
      // Convert long data to unsigned int
      // ---------------------------------
      if (chunk.isUnsigned() && longArray != null) {
        intArray = new int[longArray.length];
        for (int i = 0; i < longArray.length; i++) intArray[i] = (int) (longArray[i] & 0xffffffff);
        longArray = null;
      } // if
      
      // Check for int data
      // ------------------
      else {
        if (intArray == null) throw new RuntimeException ("No int data available (type mismatch)");
      } // else

      // Copy int values into chunk
      // --------------------------
      int[] intData = chunk.getIntData();
      Integer missing = chunk.getMissing();
      if (missing != null && isMissingArray != null) {
        int missingValue = missing;
        for (int i = 0; i < intData.length; i++) { intData[i] = (isMissingArray[i] ? missingValue : intArray[i]); }
      } // if
      else {
        for (int i = 0; i < intData.length; i++) { intData[i] = intArray[i]; }
      } // else
    } // else

  } // visitIntChunk

  ////////////////////////////////////////////////////////////

  @Override
  public void visitLongChunk (LongChunk chunk) {

    // Pack data to long
    // -----------------
    PackingScheme packing = chunk.getPackingScheme();
    if (packing != null) {
      packing.accept (new PackingSchemeVisitor () {

        @Override
        public void visitFloatPackingScheme (FloatPackingScheme scheme) {
          throw new RuntimeException ("Packing long data to float data not supported");
        } // visitFloatPackingScheme

        @Override
        public void visitDoublePackingScheme (DoublePackingScheme scheme) {
          if (doubleArray == null) throw new RuntimeException ("No double data available for packing (type mismatch)");
          if (chunk.isUnsigned()) throw new RuntimeException ("Packing double to unsigned long not supported");
          long[] longData = chunk.getLongData();
          scheme.packToLongData (doubleArray, longData, chunk.getMissing());
        } // visitDoublePackingScheme

      });
    } // if

    // Copy long data
    // --------------
    else {
      if (longArray == null) throw new RuntimeException ("No long data available (type mismatch)");
      long[] longData = chunk.getLongData();
      Long missing = chunk.getMissing();
      if (missing != null && isMissingArray != null) {
        long missingValue = missing;
        for (int i = 0; i < longData.length; i++) { longData[i] = (isMissingArray[i] ? missingValue : longArray[i]); }
      } // if
      else {
        for (int i = 0; i < longData.length; i++) { longData[i] = longArray[i]; }
      } // else
    } // else

  } // visitLongChunk

  ////////////////////////////////////////////////////////////

  @Override
  public void visitFloatChunk (FloatChunk chunk) {

    if (floatArray == null) throw new RuntimeException ("No float data found (type mismatch)");

    // Unscale data
    // ------------
    var srcFloatArray = floatArray;
    float[] floatData = chunk.getFloatData();
    ScalingScheme scaling = chunk.getScalingScheme();
    if (scaling != null) {
      scaling.accept (new ScalingSchemeVisitor () {

        @Override
        public void visitFloatScalingScheme (FloatScalingScheme scheme) {
          scheme.unscaleFloatData (floatArray, floatData);
        } // visitFloatScalingScheme

        @Override
        public void visitDoubleScalingScheme (DoubleScalingScheme scheme) {
          throw new RuntimeException ("Double scaling for float data not supported");
        } // visitDoublePackingScheme

      });
      srcFloatArray = floatData;
    } // if

    // Copy float data
    // ---------------
    Float missing = chunk.getMissing();
    if (missing != null) {
      float missingValue = missing;
      for (int i = 0; i < floatData.length; i++) { floatData[i] = (Float.isNaN (srcFloatArray[i]) ? missingValue : srcFloatArray[i]); }
    } // if
    else {
      for (int i = 0; i < floatData.length; i++) { floatData[i] = srcFloatArray[i]; }
    } // else

  } // visitFloatChunk

  ////////////////////////////////////////////////////////////

  @Override
  public void visitDoubleChunk (DoubleChunk chunk) {

    if (doubleArray == null) throw new RuntimeException ("No double data found (type mismatch)");

    // Unscale data
    // ------------
    var srcDoubleArray = doubleArray;
    double[] doubleData = chunk.getDoubleData();
    ScalingScheme scaling = chunk.getScalingScheme();
    if (scaling != null) {
      scaling.accept (new ScalingSchemeVisitor () {

        @Override
        public void visitFloatScalingScheme (FloatScalingScheme scheme) {
          throw new RuntimeException ("Float scaling for double data not supported");
        } // visitFloatScalingScheme

        @Override
        public void visitDoubleScalingScheme (DoubleScalingScheme scheme) {
          scheme.unscaleDoubleData (doubleArray, doubleData);
        } // visitDoublePackingScheme

      });
      srcDoubleArray = doubleData;
    } // if

    // Copy double data
    // ----------------
    Double missing = chunk.getMissing();
    if (missing != null) {
      double missingValue = missing;
      for (int i = 0; i < doubleData.length; i++) { doubleData[i] = (Double.isNaN (srcDoubleArray[i]) ? missingValue : srcDoubleArray[i]); }
    } // if
    else {
      for (int i = 0; i < doubleData.length; i++) { doubleData[i] = srcDoubleArray[i]; }
    } // else

  } // visitDoubleChunk

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
    logger.startClass (ChunkDataModifier.class);

    ChunkDataModifier modify;
    DataChunk chunk;
    var factory = DataChunkFactory.getInstance();

    var floatPacking = new FloatPackingScheme (0.1f, 1.0f);
    var doublePacking = new DoublePackingScheme (0.1, 1.0);

    var floatScaling = new FloatScalingScheme (0.1f, 1.0f);
    var doubleScaling = new DoubleScalingScheme (0.1, 1.0);

    float[] floatValues = new float[] {0, 0.1f, Float.NaN, 0.3f, 0.4f};
    double[] doubleValues = new double[] {0, 0.1, Double.NaN, 0.3, 0.4};
    boolean[] missingValues = new boolean[] {false, false, true, false, false};
    int values = 5;

    Comparator<Float> floatCompare = (a,b) -> a.isNaN() ? (b.isNaN() ? 0 : 1) : (Math.abs (a - b) < 5*Math.ulp (a) ? 0 : 1);
    Comparator<Double> doubleCompare = (a,b) -> a.isNaN() ? (b.isNaN() ? 0 : 1) : (Math.abs (a - b) < 5*Math.ulp (a) ? 0 : 1);

    byte[] byteValues = new byte[] {1,2,3,4,5};
    byte byteMissing = 3;
    short[] shortValues = new short[] {1,2,3,4,5};
    short shortMissing = 3;
    int[] intValues = new int[] {1,2,3,4,5};
    int intMissing = 3;
    long[] longValues = new long[] {1,2,3,4,5};
    long longMissing = 3;
    float[] rawFloatValues = new float[] {1,2,3,4,5};
    float floatMissing = 3;
    double[] rawDoubleValues = new double[] {1,2,3,4,5};
    double doubleMissing = 3;

    logger.test ("visitByteChunk");

    modify = new ChunkDataModifier();
    modify.setMissingData (missingValues);
    modify.setByteData (byteValues);
    chunk = factory.create (Byte.TYPE, values, false, byteMissing, null, null);
    chunk.accept (modify);
    assert (Arrays.equals (byteValues, (byte[]) chunk.getPrimitiveData()));

    modify = new ChunkDataModifier();
    modify.setMissingData (missingValues);
    modify.setShortData (shortValues);
    chunk = factory.create (Byte.TYPE, values, true, byteMissing, null, null);
    chunk.accept (modify);
    assert (Arrays.equals (byteValues, (byte[]) chunk.getPrimitiveData()));

    modify = new ChunkDataModifier();
    modify.setMissingData (missingValues);
    modify.setFloatData (floatValues);
    chunk = factory.create (Byte.TYPE, values, false, byteMissing, floatPacking, null);
    chunk.accept (modify);
    assert (Arrays.equals (byteValues, (byte[]) chunk.getPrimitiveData()));

    modify = new ChunkDataModifier();
    modify.setMissingData (missingValues);
    modify.setDoubleData (doubleValues);
    chunk = factory.create (Byte.TYPE, values, false, byteMissing, doublePacking, null);
    chunk.accept (modify);
    assert (Arrays.equals (byteValues, (byte[]) chunk.getPrimitiveData()));

    logger.passed();

    logger.test ("visitShortChunk");

    modify = new ChunkDataModifier();
    modify.setMissingData (missingValues);
    modify.setShortData (shortValues);
    chunk = factory.create (Short.TYPE, values, false, shortMissing, null, null);
    chunk.accept (modify);
    assert (Arrays.equals (shortValues, (short[]) chunk.getPrimitiveData()));

    modify = new ChunkDataModifier();
    modify.setMissingData (missingValues);
    modify.setIntData (intValues);
    chunk = factory.create (Short.TYPE, values, true, shortMissing, null, null);
    chunk.accept (modify);
    assert (Arrays.equals (shortValues, (short[]) chunk.getPrimitiveData()));

    modify = new ChunkDataModifier();
    modify.setMissingData (missingValues);
    modify.setFloatData (floatValues);
    chunk = factory.create (Short.TYPE, values, false, shortMissing, floatPacking, null);
    chunk.accept (modify);
    assert (Arrays.equals (shortValues, (short[]) chunk.getPrimitiveData()));

    modify = new ChunkDataModifier();
    modify.setMissingData (missingValues);
    modify.setDoubleData (doubleValues);
    chunk = factory.create (Short.TYPE, values, false, shortMissing, doublePacking, null);
    chunk.accept (modify);
    assert (Arrays.equals (shortValues, (short[]) chunk.getPrimitiveData()));

    logger.passed();

    logger.test ("visitIntChunk");

    modify = new ChunkDataModifier();
    modify.setMissingData (missingValues);
    modify.setIntData (intValues);
    chunk = factory.create (Integer.TYPE, values, false, intMissing, null, null);
    chunk.accept (modify);
    assert (Arrays.equals (intValues, (int[]) chunk.getPrimitiveData()));

    modify = new ChunkDataModifier();
    modify.setMissingData (missingValues);
    modify.setLongData (longValues);
    chunk = factory.create (Integer.TYPE, values, true, intMissing, null, null);
    chunk.accept (modify);
    assert (Arrays.equals (intValues, (int[]) chunk.getPrimitiveData()));

    modify = new ChunkDataModifier();
    modify.setMissingData (missingValues);
    modify.setFloatData (floatValues);
    chunk = factory.create (Integer.TYPE, values, false, intMissing, floatPacking, null);
    chunk.accept (modify);
    assert (Arrays.equals (intValues, (int[]) chunk.getPrimitiveData()));

    modify = new ChunkDataModifier();
    modify.setMissingData (missingValues);
    modify.setDoubleData (doubleValues);
    chunk = factory.create (Integer.TYPE, values, false, intMissing, doublePacking, null);
    chunk.accept (modify);
    assert (Arrays.equals (intValues, (int[]) chunk.getPrimitiveData()));

    logger.passed();

    logger.test ("visitLongChunk");

    modify = new ChunkDataModifier();
    modify.setMissingData (missingValues);
    modify.setLongData (longValues);
    chunk = factory.create (Long.TYPE, values, false, longMissing, null, null);
    chunk.accept (modify);
    assert (Arrays.equals (longValues, (long[]) chunk.getPrimitiveData()));

    modify = new ChunkDataModifier();
    modify.setMissingData (missingValues);
    modify.setLongData (longValues);
    chunk = factory.create (Long.TYPE, values, true, longMissing, null, null);
    chunk.accept (modify);
    assert (Arrays.equals (longValues, (long[]) chunk.getPrimitiveData()));

    modify = new ChunkDataModifier();
    modify.setMissingData (missingValues);
    modify.setDoubleData (doubleValues);
    chunk = factory.create (Long.TYPE, values, false, longMissing, doublePacking, null);
    chunk.accept (modify);
    assert (Arrays.equals (longValues, (long[]) chunk.getPrimitiveData()));

    logger.passed();
    
    logger.test ("visitFloatChunk");

    modify = new ChunkDataModifier();
    modify.setMissingData (missingValues);
    modify.setFloatData (floatValues);
    chunk = factory.create (Float.TYPE, values, false, floatMissing, null, null);
    chunk.accept (modify);
    for (int i = 0; i < values; i++) {
      float expected = Float.isNaN (floatValues[i]) ? floatMissing : floatValues[i];
      float actual = ((float[]) chunk.getPrimitiveData())[i];
      assert (floatCompare.compare (expected, actual) == 0) : actual + " != " + expected + " at i = " + i;
    } // for

    modify = new ChunkDataModifier();
    modify.setMissingData (missingValues);
    modify.setFloatData (floatValues);
    chunk = factory.create (Float.TYPE, values, false, floatMissing, null, floatScaling);
    chunk.accept (modify);
    for (int i = 0; i < values; i++) {
      float expected = rawFloatValues[i];
      float actual = ((float[]) chunk.getPrimitiveData())[i];
      assert (floatCompare.compare (expected, actual) == 0) : actual + " != " + expected + " at i = " + i;
    } // for

    logger.passed();

    logger.test ("visitDoubleChunk");

    modify = new ChunkDataModifier();
    modify.setMissingData (missingValues);
    modify.setDoubleData (doubleValues);
    chunk = factory.create (Double.TYPE, values, false, doubleMissing, null, null);
    chunk.accept (modify);
    for (int i = 0; i < values; i++) {
      double expected = Double.isNaN (doubleValues[i]) ? doubleMissing : doubleValues[i];
      double actual = ((double[]) chunk.getPrimitiveData())[i];
      assert (doubleCompare.compare (expected, actual) == 0) : actual + " != " + expected + " at i = " + i;
    } // for

    modify = new ChunkDataModifier();
    modify.setMissingData (missingValues);
    modify.setDoubleData (doubleValues);
    chunk = factory.create (Double.TYPE, values, false, doubleMissing, null, doubleScaling);
    chunk.accept (modify);
    for (int i = 0; i < values; i++) {
      double expected = rawDoubleValues[i];
      double actual = ((double[]) chunk.getPrimitiveData())[i];
      assert (doubleCompare.compare (expected, actual) == 0) : actual + " != " + expected + " at i = " + i;
    } // for

    logger.passed();

  } // main
  
  ////////////////////////////////////////////////////////////

} // ChunkDataModifier class

////////////////////////////////////////////////////////////////////////


