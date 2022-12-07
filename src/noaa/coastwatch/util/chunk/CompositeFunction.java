////////////////////////////////////////////////////////////////////////
/*

     File: CompositeFunction.java
   Author: Peter Hollemans
     Date: 2017/11/01

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
// --------
import java.util.List;
import java.util.ArrayList;
import java.util.function.BiFunction;
import java.util.logging.Logger;

import noaa.coastwatch.util.ArrayReduction;
import noaa.coastwatch.util.chunk.DataChunk;
import noaa.coastwatch.util.chunk.DataChunk.DataType;
import noaa.coastwatch.util.chunk.ChunkFunction;
import noaa.coastwatch.util.chunk.ChunkDataAccessor;
import noaa.coastwatch.util.chunk.ChunkDataModifier;

/**
 * The <code>CompositeFunction</code> class implements a composite function
 * that takes many chunks and collapses them into just one chunk using a
 * reduction operator.  If the number of input chunks is less than the minimum
 * valid, or the input chunks contain all invalid data, the function returns
 * null from the {@link #apply} method.  All input chunks must have the same
 * external data type.
 *
 * @author Peter Hollemans
 * @since 3.5.0
 */
public class CompositeFunction implements ChunkFunction {

  private static final Logger LOGGER = Logger.getLogger (CompositeFunction.class.getName());

  // Variables
  // ---------
  
  /** The reduction operator. */
  private ArrayReduction operator;
  
  /** The minimum valid values to perform the composite. */
  private int minValid;

  /** The prototype chunk to use for creating results. */
  private DataChunk protoChunk;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new chunk compositing function.
   *
   * @param operator the reduction operator to use for compositing input chunk
   * values.
   * @param minValid the minimum number of valid input values that must be
   * present to form a composite value, at least 1.  If the valid input count
   * falls below the minimum, the output at that chunk location is marked as
   * missing.
   * @param protoChunk the prototype chunk for the function result.  The chunk
   * returned by the {@link #apply} method will be compatible with the 
   * prototype.
   */
  public CompositeFunction (
    ArrayReduction operator,
    int minValid,
    DataChunk protoChunk
  ) {
  
    this.operator = operator;
    if (minValid < 1) throw new IllegalArgumentException ("Minimum valid values must be >= 1");
    this.minValid = minValid;
    this.protoChunk = protoChunk;

  } // CompositeFunction constructor

  ////////////////////////////////////////////////////////////

  @Override
  public DataChunk apply (List<DataChunk> inputChunks) {

    DataChunk resultChunk;

    // Check input chunk
    // -----------------
    if (inputChunks.size() == 0)
      throw new RuntimeException ("No input chunks to process");

    // Create list of valid chunks
    // ---------------------------
    ValidChunkDetector detector = new ValidChunkDetector();
    List<DataChunk> validInputChunks = new ArrayList<>();
    inputChunks.forEach (chunk -> {
      chunk.accept (detector);
      if (detector.isValid()) validInputChunks.add (chunk);
    });
    
    if (validInputChunks.size() < minValid) {
      LOGGER.fine ("Insufficient chunks for composite: " + validInputChunks.size() + " < " + minValid);
      resultChunk = null;
    } // if
    
    else {

      inputChunks = validInputChunks;
      int chunkCount = inputChunks.size();

      // Create accessors
      // ----------------
      ChunkDataAccessor[] accessors = new ChunkDataAccessor[chunkCount];
      for (int chunkIndex = 0; chunkIndex < chunkCount; chunkIndex++) {
        accessors[chunkIndex] = new ChunkDataAccessor();
        inputChunks.get (chunkIndex).accept (accessors[chunkIndex]);
      } // for

      // Create result chunk
      // -------------------
      int chunkValues = inputChunks.get (0).getValues();
      resultChunk = protoChunk.blankCopyWithValues (chunkValues);
      ChunkDataModifier modifier = new ChunkDataModifier();
    
      // Compute
      // -------
      DataType chunkType = resultChunk.getExternalType();
      LOGGER.fine ("Compositing using " + operator + " on " + chunkCount + " chunks to result type " + chunkType);
      boolean[] isMissingArray;
      int valueIndex;
      switch (chunkType) {

      // Handle byte data
      // ----------------
      case BYTE:
        byte[] outputByteArray = new byte[chunkValues];
        byte[] inputByteArray = new byte[chunkCount];
        isMissingArray = new boolean[chunkValues];
        for (valueIndex = 0; valueIndex < chunkValues; valueIndex++) {
        
          // Gather values from each chunk
          // -----------------------------
          int validValues = 0;
          for (int chunkIndex = 0; chunkIndex < chunkCount; chunkIndex++) {
            if (!accessors[chunkIndex].isMissingValue (valueIndex)) {
              inputByteArray[validValues] = accessors[chunkIndex].getByteValue (valueIndex);
              validValues++;
            } // if
          } // for

          // Compute composite
          // -----------------
          if (validValues >= minValid)
            outputByteArray[valueIndex] = operator.reduce (inputByteArray, 0, validValues);
          else
            isMissingArray[valueIndex] = true;
          
        } // for
        modifier.setByteData (outputByteArray);
        modifier.setMissingData (isMissingArray);
        break;

      // Handle short data
      // -----------------
      case SHORT:
        short[] outputShortArray = new short[chunkValues];
        short[] inputShortArray = new short[chunkCount];
        isMissingArray = new boolean[chunkValues];
        for (valueIndex = 0; valueIndex < chunkValues; valueIndex++) {
        
          // Gather values from each chunk
          // -----------------------------
          int validValues = 0;
          for (int chunkIndex = 0; chunkIndex < chunkCount; chunkIndex++) {
            if (!accessors[chunkIndex].isMissingValue (valueIndex)) {
              inputShortArray[validValues] = accessors[chunkIndex].getShortValue (valueIndex);
              validValues++;
            } // if
          } // for

          // Compute composite
          // -----------------
          if (validValues >= minValid)
            outputShortArray[valueIndex] = operator.reduce (inputShortArray, 0, validValues);
          else
            isMissingArray[valueIndex] = true;
          
        } // for
        modifier.setShortData (outputShortArray);
        modifier.setMissingData (isMissingArray);
        break;

      // Handle int data
      // ---------------
      case INT:
        int[] outputIntArray = new int[chunkValues];
        int[] inputIntArray = new int[chunkCount];
        isMissingArray = new boolean[chunkValues];
        for (valueIndex = 0; valueIndex < chunkValues; valueIndex++) {
        
          // Gather values from each chunk
          // -----------------------------
          int validValues = 0;
          for (int chunkIndex = 0; chunkIndex < chunkCount; chunkIndex++) {
            if (!accessors[chunkIndex].isMissingValue (valueIndex)) {
              inputIntArray[validValues] = accessors[chunkIndex].getIntValue (valueIndex);
              validValues++;
            } // if
          } // for

          // Compute composite
          // -----------------
          if (validValues >= minValid)
            outputIntArray[valueIndex] = operator.reduce (inputIntArray, 0, validValues);
          else
            isMissingArray[valueIndex] = true;
          
        } // for
        modifier.setIntData (outputIntArray);
        modifier.setMissingData (isMissingArray);
        break;

      // Handle long data
      // ----------------
      case LONG:
        long[] outputLongArray = new long[chunkValues];
        long[] inputLongArray = new long[chunkCount];
        isMissingArray = new boolean[chunkValues];
        for (valueIndex = 0; valueIndex < chunkValues; valueIndex++) {
        
          // Gather values from each chunk
          // -----------------------------
          int validValues = 0;
          for (int chunkIndex = 0; chunkIndex < chunkCount; chunkIndex++) {
            if (!accessors[chunkIndex].isMissingValue (valueIndex)) {
              inputLongArray[validValues] = accessors[chunkIndex].getLongValue (valueIndex);
              validValues++;
            } // if
          } // for

          // Compute composite
          // -----------------
          if (validValues >= minValid)
            outputLongArray[valueIndex] = operator.reduce (inputLongArray, 0, validValues);
          else
            isMissingArray[valueIndex] = true;
          
        } // for
        modifier.setLongData (outputLongArray);
        modifier.setMissingData (isMissingArray);
        break;

      // Handle float data
      // -----------------
      case FLOAT:
        float[] outputFloatArray = new float[chunkValues];
        float[] inputFloatArray = new float[chunkCount];
        for (valueIndex = 0; valueIndex < chunkValues; valueIndex++) {
        
          // Gather values from each chunk
          // -----------------------------
          int validValues = 0;
          for (int chunkIndex = 0; chunkIndex < chunkCount; chunkIndex++) {
            if (!accessors[chunkIndex].isMissingValue (valueIndex)) {
              inputFloatArray[validValues] = accessors[chunkIndex].getFloatValue (valueIndex);
              validValues++;
            } // if
          } // for

          // Compute composite
          // -----------------
          if (validValues >= minValid)
            outputFloatArray[valueIndex] = operator.reduce (inputFloatArray, 0, validValues);
          else
            outputFloatArray[valueIndex] = Float.NaN;
          
        } // for
        modifier.setFloatData (outputFloatArray);
        break;

      // Handle double data
      // ------------------
      case DOUBLE:
        double[] outputDoubleArray = new double[chunkValues];
        double[] inputDoubleArray = new double[chunkCount];
        for (valueIndex = 0; valueIndex < chunkValues; valueIndex++) {
        
          // Gather values from each chunk
          // -----------------------------
          int validValues = 0;
          for (int chunkIndex = 0; chunkIndex < chunkCount; chunkIndex++) {
            if (!accessors[chunkIndex].isMissingValue (valueIndex)) {
              inputDoubleArray[validValues] = accessors[chunkIndex].getDoubleValue (valueIndex);
              validValues++;
            } // if
          } // for

          // Compute composite
          // -----------------
          if (validValues >= minValid)
            outputDoubleArray[valueIndex] = operator.reduce (inputDoubleArray, 0, validValues);
          else
            outputDoubleArray[valueIndex] = Double.NaN;
          
        } // for
        modifier.setDoubleData (outputDoubleArray);
        break;

      default: throw new RuntimeException ("Unsupported chunk external type: " + chunkType);

      } // switch

      // Set chunk values
      // ----------------
      resultChunk.accept (modifier);

    } // else

    return (resultChunk);

  } // apply

  ////////////////////////////////////////////////////////////

} // CompositeFunction class

////////////////////////////////////////////////////////////////////////

