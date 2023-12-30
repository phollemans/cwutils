/*
 * CoastWatch Software Library and Utilities
 * Copyright (c) 2022 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.util.chunk;

import noaa.coastwatch.util.chunk.ChunkDataAccessor;
import noaa.coastwatch.util.chunk.DataChunk;

/**
 * Performs data type casting operations to and from {@link DataChunk} objects.
 * 
 * @author Peter Hollemans
 * @since 3.8.0
 */
public class ChunkDataCast {

  ////////////////////////////////////////////////////////////

  /**
   * Casts the data values from a {@link DataChunk} object into a primitive 
   * float array.
   * 
   * @param chunk the chunk to cast.
   * @param floatArray the primitive float array to fill with data.  Missing
   * values in the chunk are marked as Float.NaN in the primitive array.
   */
  public static void toFloatArray (DataChunk chunk, float[] floatArray) {

    var access = new ChunkDataAccessor();
    chunk.accept (access);

    int values = chunk.getValues();
    switch (chunk.getExternalType()) {
    case BYTE:
      for (int i = 0; i < values; i++) 
        floatArray[i] = access.isMissingValue (i) ? Float.NaN : access.getByteValue (i);
      break;
    case SHORT:
      for (int i = 0; i < values; i++) 
        floatArray[i] = access.isMissingValue (i) ? Float.NaN : access.getShortValue (i);
      break;
    case INT:
      for (int i = 0; i < values; i++) 
        floatArray[i] = access.isMissingValue (i) ? Float.NaN : access.getIntValue (i);
      break;
    case LONG:
      for (int i = 0; i < values; i++) 
        floatArray[i] = access.isMissingValue (i) ? Float.NaN : access.getLongValue (i);
      break;
    case FLOAT:
      for (int i = 0; i < values; i++) floatArray[i] = access.getFloatValue (i);
      break;
    case DOUBLE:
      for (int i = 0; i < values; i++) floatArray[i] = (float) access.getDoubleValue (i);
      break;
    } // switch

  } // toFloatArray

  ////////////////////////////////////////////////////////////

  /**
   * Casts the data values from a {@link DataChunk} object into a primitive 
   * double array.
   * 
   * @param chunk the chunk to cast.
   * @param doubleArray the primitive double array to fill with data.  Missing
   * values in the chunk are marked as Double.NaN in the primitive array.
   * 
   * @since 3.8.1
   */
  public static void toDoubleArray (DataChunk chunk, double[] doubleArray) {

    var access = new ChunkDataAccessor();
    chunk.accept (access);

    int values = chunk.getValues();
    switch (chunk.getExternalType()) {
    case BYTE:
      for (int i = 0; i < values; i++) 
        doubleArray[i] = access.isMissingValue (i) ? Double.NaN : access.getByteValue (i);
      break;
    case SHORT:
      for (int i = 0; i < values; i++) 
        doubleArray[i] = access.isMissingValue (i) ? Double.NaN : access.getShortValue (i);
      break;
    case INT:
      for (int i = 0; i < values; i++) 
        doubleArray[i] = access.isMissingValue (i) ? Double.NaN : access.getIntValue (i);
      break;
    case LONG:
      for (int i = 0; i < values; i++) 
        doubleArray[i] = access.isMissingValue (i) ? Double.NaN : access.getLongValue (i);
      break;
    case FLOAT:
      for (int i = 0; i < values; i++) doubleArray[i] = access.getFloatValue (i);
      break;
    case DOUBLE:
      for (int i = 0; i < values; i++) doubleArray[i] = access.getDoubleValue (i);
      break;
    } // switch

  } // toDoubleArray

  ////////////////////////////////////////////////////////////

  /**
   * Casts the data values to a {@link DataChunk} object from a primitive 
   * float array.
   * 
   * @param floatArray the primitive float array to use for data.  Float.NaN
   * values in the primitive array are marked as missing in the chunk.
   * @param chunk the chunk to modify.
   */
  public static void fromFloatArray (float[] floatArray, DataChunk chunk) {

    var modify = new ChunkDataModifier();
    int values = chunk.getValues();
    switch (chunk.getExternalType()) {
    case BYTE:
      byte[] byteArray = new byte[values];
      for (int i = 0; i < values; i++) byteArray[i] = (byte) floatArray[i];
      modify.setByteData (byteArray);
      break;
    case SHORT:
      short[] shortArray = new short[values];
      for (int i = 0; i < values; i++) shortArray[i] = (short) floatArray[i];
      modify.setShortData (shortArray);
      break;
    case INT:
      int[] intArray = new int[values];
      for (int i = 0; i < values; i++) intArray[i] = (int) floatArray[i];
      modify.setIntData (intArray);
      break;
    case LONG:
      long[] longArray = new long[values];
      for (int i = 0; i < values; i++) longArray[i] = (long) floatArray[i];
      modify.setLongData (longArray);
      break;
    case FLOAT:
      float[] newFloatArray = new float[values];
      for (int i = 0; i < values; i++) newFloatArray[i] = floatArray[i];
      modify.setFloatData (newFloatArray);
      break;
    case DOUBLE:
      double[] doubleArray = new double[values];
      for (int i = 0; i < values; i++) doubleArray[i] = floatArray[i];
      modify.setDoubleData (doubleArray);
      break;
    } // switch

    boolean[] missingArray = new boolean[values];
    for (int i = 0; i < values; i++) missingArray[i] = Float.isNaN (floatArray[i]);
    modify.setMissingData (missingArray);

    chunk.accept (modify);

  } // toFloatArray

  ////////////////////////////////////////////////////////////

} // ChunkDataCast
