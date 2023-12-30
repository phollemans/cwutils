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
import noaa.coastwatch.util.chunk.ChunkFunction;
import noaa.coastwatch.util.chunk.ChunkDataAccessor;

// Testing
import noaa.coastwatch.test.TestLogger;
import java.util.Arrays;
import java.util.ArrayList;

/**
 * <p>The <code>CompositeMapFunction</code> class outputs an integer composite 
 * map using a list of input chunks.  The output map contains the integer
 * chunk index from the list that each data value in the composite should come 
 * from.  Note that this function does not apply the integer map, it only 
 * creates it. This is useful to selectively assemble data values from a time series 
 * or spatial series of partially overlapping data.</p>
 * 
 * <p>The function uses two methods to select chunk index values:</p>
 * 
 * <ul>
 *
 *   <li>Optimization variable data chunks.  The data values in the optimization 
 *   chunks are used with a comparator to determine which chunk index at each
 *   data location the composite should come from.  The optimization chunk
 *   with the maximum value according to the comparator is the one selected
 *   to provide the final composite integer value.</li>
 * 
 *   <li>Priority variable data chunks. The data values in the priority chunks
 *   are checked for valid data.  In the absence of optimization, the last chunk 
 *   with a valid value is used to provide the final composite integer value.
 *   With optimization, the chunk with maximum comparator value is used.
 *   If no chunk has valid data in the first priority variable chunks, the 
 *   next variable is checked, and so on until a chunk can be selected for 
 *   the integer map. </li>
 * 
 * </ul>
 * 
 * <p>In both cases above, if no chunk can be found with valid priority or
 * optimization data, then a value of -1 is written to the output map.  Otherwise,
 * the index of the input chunk is written.  Input data chunks to this function 
 * should be combined into a list as follows:</p>
 * 
 * <ul>
 * 
 *   <li> Optimization variable chunks fist, if applicable </li>
 * 
 *   <li> Priority variable chunks, one variable at a time, if applicable </li>
 * 
 * </ul>
 * 
 * <p>Output chunks from this method are {@link ShortChunk} objects with the 
 * missing value set to {@link java.lang.Short.MIN_VALUE}.</p>
 *
 * @author Peter Hollemans
 * @since 3.8.1
 */
@noaa.coastwatch.test.Testable
public class CompositeMapFunction implements ChunkFunction {

  private static final Logger LOGGER = Logger.getLogger (CompositeMapFunction.class.getName());

  // Variables
  // ---------
  
  /** The chunk count for all variables. */
  private int chunkCount;
  
  /** The optimization comparator. */
  private Comparator<Double> optComparator;

  /** The number of priority variables. */
  private int priorityVars;

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new integer composite map function.
   *
   * @param chunkCount the number of chunks that are being composited
   * together.
   * @param optComparator the comparator to use to determine which
   * chunk data value is optimal, or null to not perform an optimization.
   * @param priorityVars the count of priority variables to use, or zero
   * for none.
   */
  public CompositeMapFunction (
    int chunkCount,
    Comparator<Double> optComparator,
    int priorityVars
  ) {
  
    this.chunkCount = chunkCount;
    this.optComparator = optComparator;
    this.priorityVars = priorityVars;

    if (optComparator == null && priorityVars == 0)
      throw new IllegalArgumentException ("Either optimal comparator or priority variables must be used");

  } // CompositeMapFunction constructor

  ////////////////////////////////////////////////////////////

  @Override
  public DataChunk apply (List<DataChunk> inputChunks) {

    // Check the input chunk list size here.  We expect a full set of chunks 
    // for the optimization, plus each priority variable.
    int optChunkCount = chunkCount*(optComparator != null ? 1 : 0);
    int priorityChunkCount = chunkCount*priorityVars;
    int expectedInputChunks = optChunkCount + priorityChunkCount;
    if (inputChunks.size() != expectedInputChunks)
      throw new IllegalArgumentException ("Found " + inputChunks.size() + " input chunks but expected " + expectedInputChunks);

    // The final result chunk will be a short integer map of the indices of
    // the input chunks that should be used in a final composite assembly.
    // We don't actually perform the assembly here, we just create the map.
    int chunkValues = inputChunks.get (0).getValues();
    short[] outputIndexArray = new short[chunkValues];

    // Start by extracting the data to use for the optimization if we've
    // been given a comparator.  We need the data values as something that
    // a comparator can use, so we cast them here to double arrays.
    double[][] inputOptArray = null;
    if (optComparator != null) {
      inputOptArray = new double[chunkCount][chunkValues];
      var optChunks = inputChunks.subList (0, chunkCount);
      for (int chunkIndex = 0; chunkIndex < chunkCount; chunkIndex++) {
        ChunkDataCast.toDoubleArray (optChunks.get (chunkIndex), inputOptArray[chunkIndex]);
      } // for
    } // if

    // Extract the data for the priority variables -- we only need to save 
    // information about which data values are valid versus invalid.  We use
    // this later on to determine which chunk in the priority variable should
    // be selected.
    boolean[][][] inputPriorityArray = null;    
    if (priorityVars != 0) {

      inputPriorityArray = new boolean[priorityVars][chunkCount][chunkValues];
      int optChunks = optComparator == null ? 0 : chunkCount;
      for (int priorityVarIndex = 0; priorityVarIndex < priorityVars; priorityVarIndex++) {

        int startChunk = optChunks + priorityVarIndex*chunkCount;
        var priorityChunks = inputChunks.subList (startChunk, startChunk + chunkCount);
        for (int chunkIndex = 0; chunkIndex < chunkCount; chunkIndex++) {
          var accessor = new ChunkDataAccessor();
          priorityChunks.get (chunkIndex).accept (accessor);
          for (int valueIndex = 0; valueIndex < chunkValues; valueIndex++) {
            inputPriorityArray[priorityVarIndex][chunkIndex][valueIndex] = !accessor.isMissingValue (valueIndex);
          } // for
        } // for

      } // for

    } // if

    // We deal with the first scenario here of when we've been supplied with
    // both optimization data and priority data.
    if (optComparator != null && priorityVars != 0) {

      LOGGER.fine ("Creating integer composite map using " + chunkCount + " optimization chunks and " + priorityVars + " priority variables");

      for (int valueIndex = 0; valueIndex < chunkValues; valueIndex++) {

        outputIndexArray[valueIndex] = -1;
        double maxOpt = Double.NaN;
        int priorityVarIndex = 0;

        while (priorityVarIndex < priorityVars && outputIndexArray[valueIndex] == -1) {

          for (int chunkIndex = 0; chunkIndex < chunkCount; chunkIndex++) {
            if (inputPriorityArray[priorityVarIndex][chunkIndex][valueIndex]) {
              double optValue = inputOptArray[chunkIndex][valueIndex];
              if (!Double.isNaN (optValue)) {
                if (Double.isNaN (maxOpt) || optComparator.compare (optValue, maxOpt) > 0) {
                  maxOpt = optValue;
                  outputIndexArray[valueIndex] = (short) chunkIndex;
                } // if
              } // if
            } // if
          } // for
          priorityVarIndex++;

        } // while

      } // for

    } // if

    // The next scenario is when we've been supplied with just optimization 
    // data.
    else if (optComparator != null) {

      LOGGER.fine ("Creating integer composite map using " + chunkCount + " optimization chunks");

      for (int valueIndex = 0; valueIndex < chunkValues; valueIndex++) {

        outputIndexArray[valueIndex] = -1;
        double maxOpt = Double.NaN;

        for (int chunkIndex = 0; chunkIndex < chunkCount; chunkIndex++) {
          double optValue = inputOptArray[chunkIndex][valueIndex];
          if (!Double.isNaN (optValue)) {
            if (Double.isNaN (maxOpt) || optComparator.compare (optValue, maxOpt) > 0) {
              maxOpt = optValue;
              outputIndexArray[valueIndex] = (short) chunkIndex;
            } // if
          } // if

        } // for

      } // for

    } // else if

    // The final scenario is when we've been supplied with just priority
    // data -- in this case we use the last valid value found in the
    // priority variables.  
    else if (priorityVars != 0) {

      LOGGER.fine ("Creating integer composite map using " + priorityVars + " priority variables");

      for (int valueIndex = 0; valueIndex < chunkValues; valueIndex++) {

        outputIndexArray[valueIndex] = -1;
        int priorityVarIndex = 0;

        while (priorityVarIndex < priorityVars && outputIndexArray[valueIndex] == -1) {

          for (int chunkIndex = chunkCount-1; chunkIndex >=0 ; chunkIndex--) {
            if (inputPriorityArray[priorityVarIndex][chunkIndex][valueIndex]) {
              outputIndexArray[valueIndex] = (short) chunkIndex;
              break;
            } // if
          } // for
          priorityVarIndex++;

        } // while

      } // for

    } // else if

    // Finally, we package up the raw integer map data and send it back 
    // as the result.
    DataChunk resultChunk = DataChunkFactory.getInstance().create (outputIndexArray, false, Short.MIN_VALUE, null);
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
    logger.startClass (CompositeMapFunction.class);

    logger.test ("apply() with max optimization");
    var packing = new DoublePackingScheme (0.01, 0);
    var m = Short.MIN_VALUE;
    var fact = DataChunkFactory.getInstance();
    var opt0 = fact.create (new short[] {m,1,2,3,m}, false, m, packing);
    var opt1 = fact.create (new short[] {4,5,1,2,m}, false, m, packing);
    var opt2 = fact.create (new short[] {3,4,5,1,m}, false, m, packing);
    var opt3 = fact.create (new short[] {2,3,4,5,m}, false, m, packing);
    var opt4 = fact.create (new short[] {1,2,3,4,m}, false, m, packing);
    var optChunks = List.of (opt0, opt1, opt2, opt3, opt4);
    var optFunc = new CompositeMapFunction (optChunks.size(), (a,b) -> Double.compare (a, b), 0);
    var optResult = optFunc.apply (optChunks);
    short[] optResultData = (short[]) optResult.getPrimitiveData();
    LOGGER.fine ("optResultData = " + Arrays.toString (optResultData));
    assert (Arrays.equals (optResultData, new short[] {1,1,2,3,-1}));
    logger.passed();

    logger.test ("apply() with min optimization");
    optFunc = new CompositeMapFunction (optChunks.size(), (a,b) -> Double.compare (b, a), 0);
    optResult = optFunc.apply (optChunks);
    optResultData = (short[]) optResult.getPrimitiveData();
    LOGGER.fine ("optResultData = " + Arrays.toString (optResultData));
    assert (Arrays.equals (optResultData, new short[] {4,0,1,2,-1}));
    logger.passed();

    logger.test ("apply() with incorrect chunk list length");
    boolean failed = false;
    try { optFunc.apply (optChunks.subList (0, optChunks.size()-1)); }
    catch (Exception e) { failed = true; }
    assert (failed);
    logger.passed();

    logger.test ("apply() with priority (one variable)");
    var pri0 = fact.create (new short[] {1,1,1,1,m}, false, m, packing);
    var pri1 = fact.create (new short[] {1,1,1,m,m}, false, m, packing);
    var pri2 = fact.create (new short[] {1,1,m,m,m}, false, m, packing);
    var pri3 = fact.create (new short[] {1,m,m,m,m}, false, m, packing);
    var pri4 = fact.create (new short[] {m,m,m,m,m}, false, m, packing);
    var priChunks = List.of (pri0, pri1, pri2, pri3, pri4);
    var priFunc = new CompositeMapFunction (priChunks.size(), null, 1);
    var priResult = priFunc.apply (priChunks);
    short[] priResultData = (short[]) priResult.getPrimitiveData();
    LOGGER.fine ("priResultData = " + Arrays.toString (priResultData));
    assert (Arrays.equals (priResultData, new short[] {3,2,1,0,-1}));
    logger.passed();

    logger.test ("apply() with priority (two variables)");
    var pri1_0 = fact.create (new short[] {m,1,m,1,m}, false, m, packing);
    var pri1_1 = fact.create (new short[] {m,1,m,m,m}, false, m, packing);
    var pri1_2 = fact.create (new short[] {m,1,m,m,m}, false, m, packing);
    var pri1_3 = fact.create (new short[] {m,m,m,m,m}, false, m, packing);
    var pri1_4 = fact.create (new short[] {m,m,m,m,m}, false, m, packing);
    var pri2_0 = fact.create (new short[] {1,1,1,1,m}, false, m, packing);
    var pri2_1 = fact.create (new short[] {1,1,1,1,m}, false, m, packing);
    var pri2_2 = fact.create (new short[] {1,m,m,m,m}, false, m, packing);
    var pri2_3 = fact.create (new short[] {1,m,m,m,m}, false, m, packing);
    var pri2_4 = fact.create (new short[] {m,m,m,m,m}, false, m, packing);
    var pri12Chunks = List.of (pri1_0, pri1_1, pri1_2, pri1_3, pri1_4, pri2_0, pri2_1, pri2_2, pri2_3, pri2_4);
    var pri12Func = new CompositeMapFunction (pri12Chunks.size()/2, null, 2);
    var pri12Result = pri12Func.apply (pri12Chunks);
    short[] pri12ResultData = (short[]) pri12Result.getPrimitiveData();
    LOGGER.fine ("pri12ResultData = " + Arrays.toString (pri12ResultData));
    assert (Arrays.equals (pri12ResultData, new short[] {3,2,1,0,-1}));
    logger.passed();

    logger.test ("apply() with max optimization and priority (two variables)");
    var optPri12Chunks = new ArrayList<DataChunk>();
    optPri12Chunks.addAll (optChunks);
    optPri12Chunks.addAll (pri12Chunks);
    var optPri12Func = new CompositeMapFunction (optChunks.size(), (a,b) -> Double.compare (a, b), 2);
    var optPri12Result = optPri12Func.apply (optPri12Chunks);
    short[] optPri12ResultData = (short[]) optPri12Result.getPrimitiveData();

    // var opt0 = fact.create (new short[] {m,1,2,3,m}, false, m, packing);
    // var opt1 = fact.create (new short[] {4,5,1,2,m}, false, m, packing);
    // var opt2 = fact.create (new short[] {3,4,5,1,m}, false, m, packing);
    // var opt3 = fact.create (new short[] {2,3,4,5,m}, false, m, packing);
    // var opt4 = fact.create (new short[] {1,2,3,4,m}, false, m, packing);

    // var pri1_0 = fact.create (new short[] {m,1,m,1,m}, false, m, packing);
    // var pri1_1 = fact.create (new short[] {m,1,m,m,m}, false, m, packing);
    // var pri1_2 = fact.create (new short[] {m,1,m,m,m}, false, m, packing);
    // var pri1_3 = fact.create (new short[] {m,m,m,m,m}, false, m, packing);
    // var pri1_4 = fact.create (new short[] {m,m,m,m,m}, false, m, packing);

    // var pri2_0 = fact.create (new short[] {1,1,1,1,m}, false, m, packing);
    // var pri2_1 = fact.create (new short[] {1,1,1,1,m}, false, m, packing);
    // var pri2_2 = fact.create (new short[] {1,m,m,m,m}, false, m, packing);
    // var pri2_3 = fact.create (new short[] {1,m,m,m,m}, false, m, packing);
    // var pri2_4 = fact.create (new short[] {m,m,m,m,m}, false, m, packing);

    LOGGER.fine ("optPri12ResultData = " + Arrays.toString (optPri12ResultData));
    assert (Arrays.equals (optPri12ResultData, new short[] {1,1,0,0,-1}));
    logger.passed();

    logger.test ("apply() with min optimization and priority (two variables)");
    optPri12Func = new CompositeMapFunction (optChunks.size(), (a,b) -> Double.compare (b, a), 2);
    optPri12Result = optPri12Func.apply (optPri12Chunks);
    optPri12ResultData = (short[]) optPri12Result.getPrimitiveData();
    LOGGER.fine ("optPri12ResultData = " + Arrays.toString (optPri12ResultData));
    assert (Arrays.equals (optPri12ResultData, new short[] {3,0,1,0,-1}));
    logger.passed();

  } // main

  ////////////////////////////////////////////////////////////

} // CompositeMapFunction class

////////////////////////////////////////////////////////////////////////

