////////////////////////////////////////////////////////////////////////
/*

     File: ExpressionFunction.java
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
import noaa.coastwatch.util.expression.ExpressionParser;
import noaa.coastwatch.util.expression.ExpressionParser.ResultType;
import noaa.coastwatch.util.expression.EvaluateImp;

/**
 * The <code>ExpressionFunction</code> class implements the
 * {@link ChunkFunction} interface to perform mathematical expression
 * calculations on chunk data.
 *
 * @author Peter Hollemans
 * @since 3.4.0
 */
public class ExpressionFunction implements ChunkFunction {

  // Variables
  // ---------

  /** The parser to use for getting expression values. */
  private ExpressionParser parser;

  /** The prototype to use for creating new result chunks. */
  private DataChunk resultPrototype;

  /**
   * The skip missing flag, true to skip the computation if any input
   * values are set to missing.
   */
  private boolean skipMissing = false;

  ////////////////////////////////////////////////////////////

  /**
   * Sets the skip missing flag (by default set to false).
   *
   * @param flag the new flag value.  When the skip missing flag is set to
   * true, the expression is not evaluated at all if any of the input values
   * are set to missing.  This has the advantage of saving computation time.
   * When the flag is set to false, the expression is always evaluated and
   * any missing values are used in the computation.  This is important in
   * cases when the expression includes a test for missing values such as an
   * isNaN test, or if the data contains missing values that should actually
   * be used in a computation (in some cases, flag variables are assigned a
   * missing value of zero when they shouldn't be). Essentially the skip
   * missing flag acts as an optimization in certain cases.
   */
  public void setSkipMissing (boolean flag) { skipMissing = flag; }

  ////////////////////////////////////////////////////////////

  /** Provides expression variable values for this function to run. */
  public class VariableValueSource implements EvaluateImp {

    /** The value index to use in retrieving a variable value. */
    public int valueIndex;

    /** The accessors to use for data, these are set up in the constructor. */
    private ChunkDataAccessor[] accessors;

    //////////////////////////////////////////////////

    /**
     * Creates a new source of variable values from a list of chunks.
     *
     * @param chunks the data chunks to use for variable values, in order by
     * index in the expression.
     */
    public VariableValueSource (List<DataChunk> chunks) {

      int chunkCount = chunks.size();
      accessors = new ChunkDataAccessor[chunkCount];
      for (int i = 0; i < chunkCount; i++) {
        accessors[i] = new ChunkDataAccessor();
        chunks.get (i).accept (accessors[i]);
      } // for

    } // VariableValueSource constructor

    //////////////////////////////////////////////////

    /**
     * Gets the status of variable data missing flags.
     *
     * @return true if any variable has a missing value at the currently
     * set value index, or false if not.
     */
    public boolean isMissingAnyValue() {

      boolean isMissing = false;
      if (skipMissing) {
        for (int i = 0; i < accessors.length; i++) {
          if (accessors[i].isMissingValue (valueIndex)) {
            isMissing = true;
            break;
          } // if
        } // for
      } // if

      return (isMissing);

    } // isMissingAnyValue

    //////////////////////////////////////////////////

    @Override
    public byte getByteProperty (int varIndex) { return (accessors[varIndex].getByteValue (valueIndex)); }

    @Override
    public short getShortProperty (int varIndex) { return (accessors[varIndex].getShortValue (valueIndex)); }

    @Override
    public int getIntegerProperty (int varIndex) { return (accessors[varIndex].getIntValue (valueIndex)); }

    @Override
    public long getLongProperty (int varIndex) { return (accessors[varIndex].getLongValue (valueIndex)); }

    @Override
    public float getFloatProperty (int varIndex) { return (accessors[varIndex].getFloatValue (valueIndex)); }

    @Override
    public double getDoubleProperty (int varIndex) { return (accessors[varIndex].getDoubleValue (valueIndex)); }

    //////////////////////////////////////////////////

  } // VariableValueSource class

  ////////////////////////////////////////////////////////////

  /**
   * Initializes this function with the specified parser that will be used
   * to evaluate expression values.  The parser should be initialized
   * and the expression parsed before calling this method.
   *
   * @param parser the initialized and parsed expression parser.
   * @param resultPrototype the result chunk prototype.  The prototype will be
   * used to generate new results when the {@link #apply} method is called.
   */
  public void init (
    ExpressionParser parser,
    DataChunk resultPrototype
  ) {
  
    this.parser = parser;
    this.resultPrototype = resultPrototype;
    
  } // init

  ////////////////////////////////////////////////////////////

  @Override
  public long getMemory (
    ChunkPosition pos, 
    int chunks
  ) { 

    long mem = 0;
    
    // Add in chunk accessor data used to store the external chunk data
    // prior to expression evaluation, plus the missing data.
    int chunkValues = pos.getValues();
    var varNames = parser.getVariables();

    if (varNames.size() != chunks) {
      throw new IllegalArgumentException ("Variable name count " + varNames.size() + 
        " does not match input chunk count " + chunks);
    } // if

    for (var name : varNames) {

      int bytesPerValue = 0;
      var type = parser.getVariableType (name);
      if (type != null) {
        if (type.equals ("Byte")) bytesPerValue = 1;
        else if (type.equals ("Short")) bytesPerValue = 2;
        else if (type.equals ("Integer")) bytesPerValue = 4;
        else if (type.equals ("Long")) bytesPerValue = 8;
        else if (type.equals ("Float")) bytesPerValue = 4;
        else if (type.equals ("Double")) bytesPerValue = 8;
      } // if
      mem += bytesPerValue*chunkValues;
      mem += chunkValues;

    } // for

    // Add in the temporary array used to store the boolean-valued missing 
    // flags for the output integer data.
    switch (parser.getResultType()) {
    case BOOLEAN: mem += chunkValues; break;
    case BYTE: mem += chunkValues; break;
    case SHORT: mem += chunkValues; break;
    case INT: mem += chunkValues; break;
    case LONG: mem += chunkValues; break;
    } // switch

    return (mem);

  } // getMemory

  ////////////////////////////////////////////////////////////

  @Override
  public DataChunk apply (
    ChunkPosition pos,
    List<DataChunk> inputChunks
  ) {

    // Initialize
    // ----------
    VariableValueSource valueSource = new VariableValueSource (inputChunks);
    DataChunk resultChunk = resultPrototype.blankCopyWithValues (pos.getValues());
    ChunkDataModifier modifier = new ChunkDataModifier();
    int count = resultChunk.getValues();
    int i;
  
    // Compute
    // -------
    ResultType type = parser.getResultType();
    boolean[] isMissingArray;
    switch (type) {

    case BOOLEAN:
      byte[] booleanAsByteArray = new byte[count];
      isMissingArray = new boolean[count];
      for (i = 0; i < count; i++) {
        valueSource.valueIndex = i;
        if (valueSource.isMissingAnyValue()) {
          isMissingArray[i] = true;
        } // if
        else {
          try { booleanAsByteArray[i] = (byte) (parser.evaluateToBoolean (valueSource) ? 1 : 0); }
          catch (Throwable t) { throw new RuntimeException (t); }
        } // else
      } // for
      modifier.setByteData (booleanAsByteArray);
      modifier.setMissingData (isMissingArray);
      break;

    case BYTE:
      byte[] byteArray = new byte[count];
      isMissingArray = new boolean[count];
      for (i = 0; i < count; i++) {
        valueSource.valueIndex = i;
        if (valueSource.isMissingAnyValue()) {
          isMissingArray[i] = true;
        } // if
        else {
          try { byteArray[i] = parser.evaluateToByte (valueSource); }
          catch (Throwable t) { throw new RuntimeException (t); }
        } // else
      } // for
      modifier.setByteData (byteArray);
      modifier.setMissingData (isMissingArray);
      break;

    case SHORT:
      short[] shortArray = new short[count];
      isMissingArray = new boolean[count];
      for (i = 0; i < count; i++) {
        valueSource.valueIndex = i;
        if (valueSource.isMissingAnyValue()) {
          isMissingArray[i] = true;
        } // if
        else {
          try { shortArray[i] = parser.evaluateToShort (valueSource); }
          catch (Throwable t) { throw new RuntimeException (t); }
        } // else
      } // for
      modifier.setShortData (shortArray);
      modifier.setMissingData (isMissingArray);
      break;

    case INT:
      int[] intArray = new int[count];
      isMissingArray = new boolean[count];
      for (i = 0; i < count; i++) {
        valueSource.valueIndex = i;
        if (valueSource.isMissingAnyValue()) {
          isMissingArray[i] = true;
        } // if
        else {
          try { intArray[i] = parser.evaluateToInt (valueSource); }
          catch (Throwable t) { throw new RuntimeException (t); }
        } // else
      } // for
      modifier.setIntData (intArray);
      modifier.setMissingData (isMissingArray);
      break;

    case LONG:
      long[] longArray = new long[count];
      isMissingArray = new boolean[count];
      for (i = 0; i < count; i++) {
        valueSource.valueIndex = i;
        if (valueSource.isMissingAnyValue()) {
          isMissingArray[i] = true;
        } // if
        else {
          try { longArray[i] = parser.evaluateToLong (valueSource); }
          catch (Throwable t) { throw new RuntimeException (t); }
        } // else
      } // for
      modifier.setLongData (longArray);
      modifier.setMissingData (isMissingArray);
      break;

    case FLOAT:
      float[] floatArray = new float[count];
      for (i = 0; i < count; i++) {
        valueSource.valueIndex = i;
        if (valueSource.isMissingAnyValue()) {
          floatArray[i] = Float.NaN;
        } // if
        else {
          try { floatArray[i] = parser.evaluateToFloat (valueSource); }
          catch (Throwable t) { throw new RuntimeException (t); }
        } // else
      } // for
      modifier.setFloatData (floatArray);
      break;

    case DOUBLE:
      double[] doubleArray = new double[count];
      for (i = 0; i < count; i++) {
        valueSource.valueIndex = i;
        if (valueSource.isMissingAnyValue()) {
          doubleArray[i] = Double.NaN;
        } // if
        else {
          try { doubleArray[i] = parser.evaluateToDouble (valueSource); }
          catch (Throwable t) { throw new RuntimeException (t); }
        } // else
      } // for
      modifier.setDoubleData (doubleArray);
      break;

    default: throw new RuntimeException ("Unsupported expression result type: " + type);

    } // swtich

    // Set chunk values
    // ----------------
    resultChunk.accept (modifier);
    return (resultChunk);

  } // apply

  ////////////////////////////////////////////////////////////

} // ExpressionFunction class

////////////////////////////////////////////////////////////////////////
