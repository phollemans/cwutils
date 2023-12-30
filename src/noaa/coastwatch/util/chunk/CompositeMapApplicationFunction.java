/*
 * CoastWatch Software Library and Utilities
 * Copyright (c) 2023 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.util.chunk;

import java.util.List;
import java.util.Comparator;

import java.util.logging.Logger;

import noaa.coastwatch.util.chunk.DataChunk;
import noaa.coastwatch.util.chunk.DataChunk.DataType;
import noaa.coastwatch.util.chunk.ChunkFunction;
import noaa.coastwatch.util.chunk.ChunkDataAccessor;

// Testing
import noaa.coastwatch.test.TestLogger;
import java.util.Arrays;
import java.util.ArrayList;

/**
 * <p>The <code>CompositeMapApplicationFunction</code> class applies the 
 * integer composite map created by the {@link CompositeMapFunction} class
 * to a list of chunks representing a time series or spatial series of 
 * partially overlapping data.</p>

 * <p>Input data chunks to this function should be combined into a list 
 * as follows:</p>
 * 
 * <ul>
 * 
 *   <li> Short integer chunk first with map index data </li>
 * 
 *   <li> Variable data chunks </li>
 * 
 * </ul>
 *
 * @author Peter Hollemans
 * @since 3.8.1
 */
public class CompositeMapApplicationFunction implements ChunkFunction {

  private static final Logger LOGGER = Logger.getLogger (CompositeMapApplicationFunction.class.getName());

  // Variables
  // ---------
  
  /** The chunk count for all variables. */
  private int chunkCount;
  
  /** The prototype chunk to use for creating results. */
  private DataChunk protoChunk;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new integer composite map application function.
   *
   * @param chunkCount the number of chunks that are being composited
   * together.
   *
   * @param protoChunk the prototype chunk for the function result.  The chunk
   * returned by the {@link #apply} method will be compatible with the 
   * prototype.
   */
  public CompositeMapApplicationFunction (
    int chunkCount,
    DataChunk protoChunk
  ) {
  
    this.chunkCount = chunkCount;
    this.protoChunk = protoChunk;

  } // CompositeMapApplicationFunction constructor

  ////////////////////////////////////////////////////////////

  @Override
  public DataChunk apply (List<DataChunk> inputChunks) {

    DataChunk resultChunk;

    // Check the input chunk list size here.  We expect a full set of chunks 
    // to properly apply the mapping, plus the initial map chunk.
    int expectedInputChunks = 1 + chunkCount;
    if (inputChunks.size() != expectedInputChunks)
      throw new IllegalArgumentException ("Found " + inputChunks.size() + " input chunks but expected " + expectedInputChunks);

    // Access the integer map chunk and the data chunks.
    short[] mapArray = (short[]) ((inputChunks.get (0)).getPrimitiveData());
    ChunkDataAccessor[] accessors = new ChunkDataAccessor[chunkCount];
    for (int chunkIndex = 0; chunkIndex < chunkCount; chunkIndex++) {
      accessors[chunkIndex] = new ChunkDataAccessor();
      inputChunks.get (1+chunkIndex).accept (accessors[chunkIndex]);
    } // for

    // Create the result chunk.
    int chunkValues = inputChunks.get (0).getValues();
    resultChunk = protoChunk.blankCopyWithValues (chunkValues);
    ChunkDataModifier modifier = new ChunkDataModifier();
  
    // Now apply the integer map to the input chunks and copy the data into
    // the result chunk.  In each case we check for a missing value or a 
    // negative chunk index, indicating that we should fill with missing data.
    DataType chunkType = resultChunk.getExternalType();
    LOGGER.fine ("Compositing using integer map on " + chunkCount + " chunks to result type " + chunkType);
    boolean[] isMissingArray;
    int valueIndex;
    switch (chunkType) {

    // Why do we not use the Visitor pattern here with the ChunkVisitor 
    // interface?  It's because that pattern is determined by the DataChunk
    // subclass, where as we're computing data composites using the 
    // chunk external data type here.  We don't have a visitor pattern for 
    // the external data type.

    // Handle a chunk with external type of primitive byte.
    case BYTE:
      byte[] outputByteArray = new byte[chunkValues];
      isMissingArray = new boolean[chunkValues];
      for (valueIndex = 0; valueIndex < chunkValues; valueIndex++) {
        short chunkIndex = mapArray[valueIndex];
        if (chunkIndex >= 0 && !accessors[chunkIndex].isMissingValue (valueIndex))
          outputByteArray[valueIndex] = accessors[chunkIndex].getByteValue (valueIndex);
        else
          isMissingArray[valueIndex] = true;        
      } // for
      modifier.setByteData (outputByteArray);
      modifier.setMissingData (isMissingArray);
      break;

    // Handle a chunk with external type of primitive short.
    case SHORT:
      short[] outputShortArray = new short[chunkValues];
      isMissingArray = new boolean[chunkValues];
      for (valueIndex = 0; valueIndex < chunkValues; valueIndex++) {
        short chunkIndex = mapArray[valueIndex];
        if (chunkIndex >= 0 && !accessors[chunkIndex].isMissingValue (valueIndex))
          outputShortArray[valueIndex] = accessors[chunkIndex].getShortValue (valueIndex);
        else
          isMissingArray[valueIndex] = true;
      } // for
      modifier.setShortData (outputShortArray);
      modifier.setMissingData (isMissingArray);
      break;

    // Handle a chunk with external type of primitive int.
    case INT:
      int[] outputIntArray = new int[chunkValues];
      isMissingArray = new boolean[chunkValues];
      for (valueIndex = 0; valueIndex < chunkValues; valueIndex++) {
        short chunkIndex = mapArray[valueIndex];
        if (chunkIndex >= 0 && !accessors[chunkIndex].isMissingValue (valueIndex))
          outputIntArray[valueIndex] = accessors[chunkIndex].getIntValue (valueIndex);
        else
          isMissingArray[valueIndex] = true;
      } // for
      modifier.setIntData (outputIntArray);
      modifier.setMissingData (isMissingArray);
      break;

    // Handle a chunk with external type of primitive long.
    case LONG:
      long[] outputLongArray = new long[chunkValues];
      isMissingArray = new boolean[chunkValues];
      for (valueIndex = 0; valueIndex < chunkValues; valueIndex++) {
        short chunkIndex = mapArray[valueIndex];
        if (chunkIndex >= 0 && !accessors[chunkIndex].isMissingValue (valueIndex))
          outputLongArray[valueIndex] = accessors[chunkIndex].getLongValue (valueIndex);
        else
          isMissingArray[valueIndex] = true;        
      } // for
      modifier.setLongData (outputLongArray);
      modifier.setMissingData (isMissingArray);
      break;

    // Handle a chunk with external type of primitive float.
    case FLOAT:
      float[] outputFloatArray = new float[chunkValues];
      for (valueIndex = 0; valueIndex < chunkValues; valueIndex++) {
        short chunkIndex = mapArray[valueIndex];
        if (chunkIndex >= 0 && !accessors[chunkIndex].isMissingValue (valueIndex))
          outputFloatArray[valueIndex] = accessors[chunkIndex].getFloatValue (valueIndex);
        else
          outputFloatArray[valueIndex] = Float.NaN;
      } // for
      modifier.setFloatData (outputFloatArray);
      break;

    // Handle a chunk with external type of primitive double.
    case DOUBLE:
      double[] outputDoubleArray = new double[chunkValues];
      for (valueIndex = 0; valueIndex < chunkValues; valueIndex++) {
        short chunkIndex = mapArray[valueIndex];
        if (chunkIndex >= 0 && !accessors[chunkIndex].isMissingValue (valueIndex))
          outputDoubleArray[valueIndex] = accessors[chunkIndex].getDoubleValue (valueIndex);
        else
          outputDoubleArray[valueIndex] = Double.NaN;
      } // for
      modifier.setDoubleData (outputDoubleArray);
      break;

    default: throw new RuntimeException ("Unsupported chunk external type: " + chunkType);

    } // switch

    // Set the result chunk values and return.
    resultChunk.accept (modifier);
    return (resultChunk);

  } // apply

  ////////////////////////////////////////////////////////////

  /**
   * Tests this class.
   *
   * @param argv the array of command line parameters.
   */
  public static void main (String[] argv) throws Exception {

    TestLogger logger = TestLogger.getInstance();
    logger.startClass (CompositeMapApplicationFunction.class);

    logger.test ("apply() with FLOAT external type");
    var m = Short.MIN_VALUE;
    var fact = DataChunkFactory.getInstance();
    var map = fact.create (new short[] {0,1,2,3,4}, false, m, null);
    var packing = new DoublePackingScheme (0.01, 0);
    var proto = fact.create (new short[0], false, m, packing);
    var var0 = fact.create (new short[] {0,1,2,3,4}, false, m, packing);
    var var1 = fact.create (new short[] {5,6,7,8,9}, false, m, packing);
    var var2 = fact.create (new short[] {10,11,12,13,14}, false, m, packing);
    var var3 = fact.create (new short[] {15,16,17,18,19}, false, m, packing);
    var var4 = fact.create (new short[] {20,21,22,23,m}, false, m, packing);
    var inputChunks = List.of (map, var0, var1, var2, var3, var4);
    var func = new CompositeMapApplicationFunction (inputChunks.size()-1, proto);
    var result = func.apply (inputChunks);
    short[] resultData = (short[]) result.getPrimitiveData();
    LOGGER.fine ("resultData = " + Arrays.toString (resultData));
    assert (Arrays.equals (resultData, new short[] {0,6,12,18,m}));
    logger.passed();

    logger.test ("apply() with incorrect chunk list length");
    boolean failed = false;
    try { func.apply (inputChunks.subList (1, inputChunks.size())); }
    catch (Exception e) { failed = true; }
    assert (failed);
    logger.passed();

  } // main

  ////////////////////////////////////////////////////////////

} // CompositeMapApplicationFunction class

////////////////////////////////////////////////////////////////////////

