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
 * missing value set to -1.</p>
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
  public long getMemory (
    ChunkPosition pos, 
    int chunks
  ) { 

    // Note that in this function, we ignore the chunks parameter passed in 
    // because we have other information about the number of chunks being 
    // composited together from the constructor.  The chunks parameter is 
    // not correct here because it takes into account all the chunks passed 
    // in and doesn't distinguish optimization chunks versus priority chunks 
    // which we handle differently.

    long mem = 0;

    // Add in the temporary array used to store the double-valued 
    // optimization data.
    int chunkValues = pos.getValues();
    if (optComparator != null) {
      mem += chunkCount*chunkValues*8;
    } // if

    // Add in the temporary array used to store the boolean-valued 
    // priority data and the data accessor memory.
    if (priorityVars != 0) {
      mem += priorityVars*chunkCount*chunkValues;
      mem += chunkCount*chunkValues;
    } // if

    return (mem);

  } // getMemory

  ////////////////////////////////////////////////////////////

  @Override
  public DataChunk apply (
    ChunkPosition pos,
    List<DataChunk> inputChunks
  ) {

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
    int chunkValues = pos.getValues();
    short[] outputIndexArray = new short[chunkValues];

    // Start by extracting the data to use for the optimization if we've
    // been given a comparator.  We need the data values as something that
    // a comparator can use, so we cast them here to double arrays.
    double[][] inputOptArray = null;
    int[] optChunkIndexMap = new int[chunkCount];
    int[] optChunkBackIndexMap = null;
    int validOptChunkCount = 0;
    if (optComparator != null) {

      // First detect all the chunks with entirely invalid data so
      // that we can skip expanding those into the arrays we'll use for 
      // comparison.  We need to save the original index of each chunk so 
      // that it can be used to populate the coherent map.
      var optChunks = inputChunks.subList (0, chunkCount);
      for (int chunkIndex = 0; chunkIndex < chunkCount; chunkIndex++) {
        var optChunk = optChunks.get (chunkIndex);
        var detector = new ValidChunkDetector();
        optChunk.accept (detector);
        if (detector.isValid()) {
          optChunkIndexMap[chunkIndex] = validOptChunkCount;
          validOptChunkCount++;
        } // if
        else {
          optChunkIndexMap[chunkIndex] = -1;
        } // else
      } // for
      optChunkBackIndexMap = new int[validOptChunkCount];
      for (int chunkIndex = 0, validChunkIndex = 0; chunkIndex < chunkCount; chunkIndex++) {
        if (optChunkIndexMap[chunkIndex] != -1) {
          optChunkBackIndexMap[validChunkIndex] = chunkIndex;
          validChunkIndex++;
        } // if
      } // for

      // Now expand only the valid optimization chunks that we need
      // into an array that we can use for comparison.
      inputOptArray = new double[validOptChunkCount][chunkValues];
      for (int validChunkIndex = 0; validChunkIndex < validOptChunkCount; validChunkIndex++) {        
        ChunkDataCast.toDoubleArray (optChunks.get (optChunkBackIndexMap[validChunkIndex]), inputOptArray[validChunkIndex]);
      } // for

    } // if

    // Extract the data for the priority variables -- we only need to save 
    // information about which data values are valid versus invalid.  We use
    // this later on to determine which chunk in the priority variable should
    // be selected.
    boolean[][][] inputPriorityArray = null;    
    int[][] priorityChunkIndexMap = new int[priorityVars][chunkCount];
    int[][] priorityChunkBackIndexMap = new int[priorityVars][];
    int[] validPriorityChunkCount = new int[priorityVars];
    if (priorityVars != 0) {

      // The same as above, first detect all the priority chunks with entirely 
      // invalid data so that we can skip expanding those into the arrays 
      // we'll use for checking for priority.  We again save maps between 
      // chunk indices.
      int optChunks = optComparator == null ? 0 : chunkCount;
      for (int priorityVarIndex = 0; priorityVarIndex < priorityVars; priorityVarIndex++) {

        int startChunk = optChunks + priorityVarIndex*chunkCount;
        var priorityChunks = inputChunks.subList (startChunk, startChunk + chunkCount);
        for (int chunkIndex = 0; chunkIndex < chunkCount; chunkIndex++) {

          var priorityChunk = priorityChunks.get (chunkIndex);
          var detector = new ValidChunkDetector();
          priorityChunk.accept (detector);
          if (detector.isValid()) {
            priorityChunkIndexMap[priorityVarIndex][chunkIndex] = validPriorityChunkCount[priorityVarIndex];
            validPriorityChunkCount[priorityVarIndex]++;
          } // if
          else {
            priorityChunkIndexMap[priorityVarIndex][chunkIndex] = -1;
          } // else

        } // for

        priorityChunkBackIndexMap[priorityVarIndex] = new int[validPriorityChunkCount[priorityVarIndex]];
        for (int chunkIndex = 0, validChunkIndex = 0; chunkIndex < chunkCount; chunkIndex++) {
          if (priorityChunkIndexMap[priorityVarIndex][chunkIndex] != -1) {
            priorityChunkBackIndexMap[priorityVarIndex][validChunkIndex] = chunkIndex;
            validChunkIndex++;
          } // if
        } // for

      } // for

      // Now as before, expand only the valid priority chunks that we need
      // into an array that we can use for comparison.
      inputPriorityArray = new boolean[priorityVars][][];
      for (int priorityVarIndex = 0; priorityVarIndex < priorityVars; priorityVarIndex++) {

        int startChunk = optChunks + priorityVarIndex*chunkCount;
        var priorityChunks = inputChunks.subList (startChunk, startChunk + chunkCount);

        int validChunkCount = validPriorityChunkCount[priorityVarIndex];
        inputPriorityArray[priorityVarIndex] = new boolean[validChunkCount][chunkValues];

        for (int validChunkIndex = 0; validChunkIndex < validChunkCount; validChunkIndex++) {
          var accessor = new ChunkDataAccessor();
          accessor.setMissingMode (true);
          int chunkIndex = priorityChunkBackIndexMap[priorityVarIndex][validChunkIndex];
          priorityChunks.get (chunkIndex).accept (accessor);
          for (int valueIndex = 0; valueIndex < chunkValues; valueIndex++) {
            inputPriorityArray[priorityVarIndex][validChunkIndex][valueIndex] = !accessor.isMissingValue (valueIndex);
          } // for
        } // for

      } // for

    } // if

    // We deal with the first scenario here of when we've been supplied with
    // both optimization data and priority data.
    if (optComparator != null && priorityVars != 0) {

      int validPriorityChunkCountTotal = Arrays.stream (validPriorityChunkCount).sum();
      LOGGER.fine ("Creating integer composite map using optimization and " + priorityVars + " priority variables, chunk counts " + 
        "{opt=(" + validOptChunkCount + "/" + optChunkCount + ")" +
        ", priority=(" + validPriorityChunkCountTotal + "/" + priorityChunkCount + ")" +
        ", total=(" + (validOptChunkCount + validPriorityChunkCountTotal) + "/" + expectedInputChunks + ")}"
      );

      for (int valueIndex = 0; valueIndex < chunkValues; valueIndex++) {

        outputIndexArray[valueIndex] = -1;
        double maxOpt = Double.NaN;
        int priorityVarIndex = 0;

        while (priorityVarIndex < priorityVars && outputIndexArray[valueIndex] == -1) {

          int thisValidPriorityChunkCount = validPriorityChunkCount[priorityVarIndex];

          for (int validPriorityChunkIndex = 0; validPriorityChunkIndex < thisValidPriorityChunkCount; validPriorityChunkIndex++) {
            if (inputPriorityArray[priorityVarIndex][validPriorityChunkIndex][valueIndex]) {

              int chunkIndex = priorityChunkBackIndexMap[priorityVarIndex][validPriorityChunkIndex];
              int validOptChunkIndex = optChunkIndexMap[chunkIndex];

              if (validOptChunkIndex != -1) {
                double optValue = inputOptArray[validOptChunkIndex][valueIndex];
                if (!Double.isNaN (optValue)) {
                  if (Double.isNaN (maxOpt) || optComparator.compare (optValue, maxOpt) > 0) {
                    maxOpt = optValue;
                    outputIndexArray[valueIndex] = (short) chunkIndex;
                  } // if
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

      LOGGER.fine ("Creating integer composite map using optimization, chunk count (" + validOptChunkCount + "/" + optChunkCount + ")");

      for (int valueIndex = 0; valueIndex < chunkValues; valueIndex++) {

        outputIndexArray[valueIndex] = -1;
        double maxOpt = Double.NaN;

        for (int validOptChunkIndex = 0; validOptChunkIndex < validOptChunkCount; validOptChunkIndex++) {
          double optValue = inputOptArray[validOptChunkIndex][valueIndex];
          if (!Double.isNaN (optValue)) {
            if (Double.isNaN (maxOpt) || optComparator.compare (optValue, maxOpt) > 0) {
              maxOpt = optValue;
              outputIndexArray[valueIndex] = (short) optChunkBackIndexMap[validOptChunkIndex];
            } // if
          } // if
        } // for

      } // for

    } // else if

    // The final scenario is when we've been supplied with just priority
    // data -- in this case we use the last valid value found in the
    // priority variables.  
    else if (priorityVars != 0) {

      int validPriorityChunkCountTotal = Arrays.stream (validPriorityChunkCount).sum();
      LOGGER.fine ("Creating integer composite map using " + priorityVars + " priority variables, chunk count (" + validPriorityChunkCountTotal + "/" + priorityChunkCount + ")");

      for (int valueIndex = 0; valueIndex < chunkValues; valueIndex++) {

        outputIndexArray[valueIndex] = -1;
        int priorityVarIndex = 0;

        while (priorityVarIndex < priorityVars && outputIndexArray[valueIndex] == -1) {

          int thisValidPriorityChunkCount = validPriorityChunkCount[priorityVarIndex];
          for (int validPriorityChunkIndex = thisValidPriorityChunkCount-1; validPriorityChunkIndex >=0 ; validPriorityChunkIndex--) {
            if (inputPriorityArray[priorityVarIndex][validPriorityChunkIndex][valueIndex]) {
              outputIndexArray[valueIndex] = (short) priorityChunkBackIndexMap[priorityVarIndex][validPriorityChunkIndex];
              break;
            } // if
          } // for
          priorityVarIndex++;

        } // while

      } // for

    } // else if

    // Finally, we package up the raw integer map data and send it back 
    // as the result.
    DataChunk resultChunk = DataChunkFactory.getInstance().create (outputIndexArray, false, (short) -1, null);
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
    var pos = new ChunkPosition (1); pos.length[0] = 5;
    var optResult = optFunc.apply (pos, optChunks);
    short[] optResultData = (short[]) optResult.getPrimitiveData();
    LOGGER.fine ("optResultData = " + Arrays.toString (optResultData));
    assert (Arrays.equals (optResultData, new short[] {1,1,2,3,-1}));
    logger.passed();

    logger.test ("apply() with min optimization");
    optFunc = new CompositeMapFunction (optChunks.size(), (a,b) -> Double.compare (b, a), 0);
    optResult = optFunc.apply (pos, optChunks);
    optResultData = (short[]) optResult.getPrimitiveData();
    LOGGER.fine ("optResultData = " + Arrays.toString (optResultData));
    assert (Arrays.equals (optResultData, new short[] {4,0,1,2,-1}));
    logger.passed();

    logger.test ("apply() with incorrect chunk list length");
    boolean failed = false;
    try { optFunc.apply (pos, optChunks.subList (0, optChunks.size()-1)); }
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
    var priResult = priFunc.apply (pos, priChunks);
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
    var pri12Result = pri12Func.apply (pos, pri12Chunks);
    short[] pri12ResultData = (short[]) pri12Result.getPrimitiveData();
    LOGGER.fine ("pri12ResultData = " + Arrays.toString (pri12ResultData));
    assert (Arrays.equals (pri12ResultData, new short[] {3,2,1,0,-1}));
    logger.passed();

    logger.test ("apply() with max optimization and priority (two variables)");
    var optPri12Chunks = new ArrayList<DataChunk>();
    optPri12Chunks.addAll (optChunks);
    optPri12Chunks.addAll (pri12Chunks);
    var optPri12Func = new CompositeMapFunction (optChunks.size(), (a,b) -> Double.compare (a, b), 2);
    var optPri12Result = optPri12Func.apply (pos, optPri12Chunks);
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
    optPri12Result = optPri12Func.apply (pos, optPri12Chunks);
    optPri12ResultData = (short[]) optPri12Result.getPrimitiveData();
    LOGGER.fine ("optPri12ResultData = " + Arrays.toString (optPri12ResultData));
    assert (Arrays.equals (optPri12ResultData, new short[] {3,0,1,0,-1}));
    logger.passed();

  } // main

  ////////////////////////////////////////////////////////////

} // CompositeMapFunction class

