///////////////////////////////////////////////////////////////////////
/*
     FILE: DataVariable.java
  PURPOSE: A class to act as a container for variable data and
           attributes.  The hash map acts as a catch-all container for
           attribute key/value pairs.
   AUTHOR: Peter Hollemans
     DATE: 2002/04/15
  CHANGES: 2002/04/25, MSR, null pointer fix
           2002/05/14, PFH, added javadoc, package
           2002/05/31, PFH, added data coordinate routines
           2002/06/06, PFH, rearranged and added getSubset and getValues
           2002/07/25, PFH, simplified, added location class
           2002/09/03, PFH, renamed to DataVariable
           2002/10/06, PFH, added statistics class, added stride to
             statistics calculation, added getRank
           2002/10/21, PFH, optimized scaleValue
           2002/10/22, PFH, added start and end to getStatistics
           2002/10/31, PFH, removed get/set checks for invalid indices
           2002/12/26, PFH, added getDecimals
           2003/02/20, PFH, modified getStatistics to set Double.NaN values,
             fixed max statistic problem
           2003/02/21, PFH, added check in getStatistics for valid locations
           2003/09/08, PFH, moved Statistics to its own class
           2003/11/22, PFH, fixed Javadoc comments
           2004/02/16, PFH, added unsigned type handling
           2004/03/11, PFH, added toString() method
           2004/03/27, PFH, modified to use DataVariableIterator and
             StrideLocationIterator in getStatistics()
           2004/03/28, PFH, added getOptimalStride() method
           2004/04/12, PFH, added lookup tables
           2004/10/05, PFH, modified to extend MetadataContainer
           2005/06/03, PFH, added convertUnits() method
           2006/05/25, PFH, added setAccessHint() method
           2007/06/20, PFH, added various set methods
           2014/04/09, PFH
           - Changes: Removed method setIsCFConvention
           - Issue: There are various operations to do with units
             conversion and setting values that aren't set up to handle CF
             scaling.  It was introduced for reading CF convention files, but
             not fully implemented.  The fix is to re-arrange the scaling factor
             and offset when reading CF data and then pass in the re-arranged
             scaling to the data variable constructor.
           2014/08/26, PFH
           - Changes: Added dispose() method.
           - Issue: We added a dispose() method at the DataVariable level to
             better handle disposing of resources, rather than relying on 
             finalize() which is inherently unsafe because there is no guarantee
             that it will ever be called by the VM.
 
  CoastWatch Software Library and Utilities
  Copyright 1998-2014, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.util;

// Imports
// -------
import java.util.*;
import java.text.*;
import java.lang.reflect.*;
import ucar.units.*;

/**
 * A data variable is a container class to hold data values
 * and a specific set of metadata.  The required metadata includes the
 * variable name, units, dimensions, number format, scaling, and
 * missing data value.  Other metadata may also be attached using the
 * <code>TreeMap</code> functions, where it is expected that the key
 * be a Java <code>String</code> object.
 *
 * @author Peter Hollemans
 * @since 3.1.0
 */
public abstract class DataVariable
  extends MetadataContainer
  implements ValueSource {

  // Constants
  // ---------

  /** The number of histogram bins for statistics. */
  private static final int HISTOGRAM_BINS = 100;

  /** The unsigned type code for byte values. */
  private static final int UNSIGNED_BYTE = 0;

  /** The unsigned type code for byte values. */
  private static final int UNSIGNED_SHORT = 1;

  /** The unsigned type code for byte values. */
  private static final int UNSIGNED_INT = 2;

  /** The unsigned type code for byte values. */
  private static final int UNSIGNED_LONG = 3;

  // Variables
  // ---------
  /** Short variable name. */
  private String name;

  /** Descriptive variable name. */
  private String longName;

  /** Real world variable units. */
  private String units;

  /** Array of data dimension sizes. */
  protected int[] dims;

  /** Array of data values. */
  protected Object data;

  /** Data value format specification. */
  private NumberFormat format;

  /** Scaling factor and offset. */
  private double[] scaling;

  /** Missing data value. */
  private Object missing;

  /** The unsigned flag, true if the data values are unsigned. */
  protected boolean isUnsigned;

  /** The unsigned type. */
  protected int unsignedType;

  /** The lookup table. */
  protected double[] lookup;
  
  ////////////////////////////////////////////////////////////

  /** Gets the short variable name. */
  public String getName () { return (name); }

  ////////////////////////////////////////////////////////////

  /** Sets the short variable name. */
  public void setName (String name) { this.name = name; }
  
  ////////////////////////////////////////////////////////////

  /** Gets the descriptive variable name. */
  public String getLongName () { return (longName); }

  ////////////////////////////////////////////////////////////

  /** Sets the descriptive variable name. */
  public void setLongName (String longName) { this.longName = longName; }

  ////////////////////////////////////////////////////////////

  /** Gets the variable units. */
  public String getUnits () { return (units); }

  ////////////////////////////////////////////////////////////

  /** Sets the variable units. */
  public void setUnits (String units) { this.units = units; }

  ////////////////////////////////////////////////////////////

  /** Gets the variable data array dimensions. */
  public int[] getDimensions () { return ((int[]) dims.clone ()); }

  ////////////////////////////////////////////////////////////

  /** Gets the variable data array rank. */
  public int getRank () { return (dims.length); }

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the variable data array.  The returned data is read-only -- the
   * result of attempting to set values in the data is undefined. 
   */
  public Object getData () { return (data); }

  ////////////////////////////////////////////////////////////

  /** Sets the variable data array. */
  public void setData (Object data) { this.data = data; }

  ////////////////////////////////////////////////////////////

  /** Gets the scaling factor and offset. */
  public double[] getScaling () { 
    return ((scaling == null ? null : (double[]) scaling.clone ())); 
  } // getScaling

  ////////////////////////////////////////////////////////////

  /** Gets the data number format. */
  public NumberFormat getFormat () { return ((NumberFormat) format.clone ()); }

  ////////////////////////////////////////////////////////////

  /** Sets the data number format. */
  public void setFormat (NumberFormat format) { 

    this.format = (NumberFormat) format.clone(); 

  } // setFormat

  ////////////////////////////////////////////////////////////

  /** Gets the data missing value. */
  public Object getMissing () { return (missing); }

  ////////////////////////////////////////////////////////////

  /** Sets the data missing value. */
  public void setMissing (Object missing) { this.missing = missing; }

  ////////////////////////////////////////////////////////////

  /** 
   * Sets the lookup table.  The lookup table is an alternative to the
   * scaling factor and offset, and may be used to translate integer
   * valued data to floating point data values.  If a lookup table is
   * employed, the <code>setValue()</code> may not be used.  If both
   * lookup and scaling are set, the lookup takes precedence over the
   * scaling and the scaling is ignored.
   *
   * @param lookup the lookup table to use.
   */
  public void setLookup (
    double[] lookup
  ) {

    this.lookup = lookup;

  } // setLookup

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the class associated with components of the data array. 
   *
   * @return the data class or null if the data array is null.
   */
  public Class getDataClass () { 

    if (data == null) 
      return (null);
    else 
      return (data.getClass().getComponentType()); 

  } // getDataClass

  ////////////////////////////////////////////////////////////

  /**
   * Sets the unsigned data flag.  This may only be used if the data
   * array is of type byte[], short[], int[], or long[].  If the data
   * class is null, this method has no effect.
   *
   * @param flag the unsigned flag, true is the data array contains
   * unsigned data.
   *
   * @throws IllegalArgumentException if the data class is not allowed
   * to be unsigned.
   *
   * @see #getDataClass
   * @see #getUnsigned
   */
  public void setUnsigned (
    boolean flag
  ) {

    // Check data array class
    // ----------------------
    Class dataClass = getDataClass();
    if (dataClass == null) return;

    // Set flag
    // --------
    isUnsigned = flag;
    
    // Set class (for fast switch statement testing)
    // ---------------------------------------------
    if (isUnsigned) {
      if (dataClass.equals (Byte.TYPE)) 
        unsignedType = UNSIGNED_BYTE;
      else if (dataClass.equals (Short.TYPE)) 
        unsignedType = UNSIGNED_SHORT;
      else if (dataClass.equals (Integer.TYPE)) 
        unsignedType = UNSIGNED_INT;
      else if (dataClass.equals (Long.TYPE)) 
        unsignedType = UNSIGNED_LONG;
      else
        throw new IllegalArgumentException ("Unsupported unsigned type");
    } // if

  } // setUnsigned

  ////////////////////////////////////////////////////////////

  /** Gets the unsigned flag. */
  public boolean getUnsigned () { return (isUnsigned); }

  ////////////////////////////////////////////////////////////

  /**
   * Gets the memory required by a primitive class.
   *
   * @param c the primitive class.
   *
   * @return the number of bits needed for each primitive value.  The
   * value 0 is returned if the class is not primitive.
   */
  public static int getClassBits (
    Class c
  ) {

    if (c.equals (Boolean.TYPE)) return (1);
    else if (c.equals (Character.TYPE)) return (16);
    else if (c.equals (Byte.TYPE)) return (8);
    else if (c.equals (Short.TYPE)) return (16);
    else if (c.equals (Integer.TYPE)) return (32);
    else if (c.equals (Long.TYPE)) return (64);
    else if (c.equals (Float.TYPE)) return (32);
    else if (c.equals (Double.TYPE)) return (64);
    else return (0);

  } // getClassBits

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new data variable with the specified properties.
   *
   * @param name a short variable name.  The variable name should
   * reflect what the data values represent without being overly
   * verbose.  For example, <code>avhrr_ch4</code> would describe data
   * from the AVHRR sensor, channel 4.
   * @param longName a long variable name.  The long name is a verbose
   * string to describe the variable in common terms.  For example,
   * the variable named <code>sst</code> might have the long name
   * <code>sea surface temperature</code>.
   * @param units the real world variable units.  For example if the
   * variable data describes a temperature in Celsius, the variable
   * units might be <code>Celsius</code>.
   * @param dimensions the variable data array dimensions.  For a 2D
   * image data array, the dimensions would be the number of rows and
   * columns in the image.
   * @param data the variable data array.  The data should be stored
   * as a 1D array of values.  It is up to the subclass to determine
   * the mapping between data coordinate and index into the data array
   * for data with dimensions greater than 1.  If the passed array is
   * <code>null</code>, data get/set/format methods are undefined.
   * @param format a number format specification.  The number format
   * will be used to convert data values from their numerical
   * representation to a string representation.
   * @param scaling the scaling factor and offset.  These values will
   * be used to convert between data stored in the data array and real
   * world values.  In general, scaling is only necessary when
   * floating point data is stored as integers.  When data is
   * retrieved from the data array, the following formula is used to
   * scale the values: <code>(val - scaling[1])*scaling[0]</code>.
   * When setting values in the data array, the following formula is
   * used: <code>val/scaling[0] + scaling[1]</code>.  If
   * <code>scaling</code> is <code>null</code>, no scaling is
   * performed.
   * @param missing the missing data value.  The missing value is used
   * to represent data values in the data array that have purposely
   * not been recorded or were flagged as invalid.  If
   * <code>missing</code> is <code>null</code>, no missing value is
   * used.
   */
  public DataVariable (
    String name,
    String longName,
    String units, 
    int[] dimensions, 
    Object data, 
    NumberFormat format, 
    double[] scaling, 
    Object missing
  ) {

    this.name = name;
    this.longName = longName;
    this.units = units;
    this.dims = (int[]) dimensions.clone ();
    this.data = data;
    this.format = (NumberFormat) format.clone ();
    this.scaling = (scaling == null ? null : (double[]) scaling.clone ());
    this.missing = missing;

  } // DataVariable constructor

  ////////////////////////////////////////////////////////////

  /**
   * Reads an interpolated data value.
   *
   * @param loc the data value location.
   *
   * @return the scaled data value as a <code>double</code>.  The
   * <code>Double.NaN</code> value is used if the data value is
   * missing.
   *
   * @see #getValue(int)
   * @see #getValue(DataLocation)
   */
  public abstract double interpolate (
    DataLocation loc
  );

  ////////////////////////////////////////////////////////////

  /**
   * Reads a scaled data value.  The data value is read
   * from the data array and scaled according to the scaling factor
   * and offset.
   *
   * @param loc the data value location.
   *
   * @return the scaled data value as a <code>double</code>.  The
   * <code>Double.NaN</code> value is used if the data value is
   * missing.
   *
   * @see #getValue(int)
   */
  public double getValue (
    DataLocation loc
  ) {

    return (getValue (loc.getIndex (dims)));

  } // getValue

  ////////////////////////////////////////////////////////////

  /**
   * Gets the total number of data values indicated by the dimensions.
   *
   * @return the number of data values.
   */
  public int getValues () {

    int vals = 1;
    for (int i = 0; i < dims.length; i++)
      vals *= dims[i];
    return (vals);

  } // getValues

  ////////////////////////////////////////////////////////////

  /**
   * Reads a scaled data value.
   * 
   * @param index the index into the data array.  
   * @param data the data array to use for raw data.
   * 
   * @return the scaled data value as a <code>double</code>.  The
   * <code>Double.NaN</code> value is used if the data value is
   * missing.
   */
  protected double getValue (
    int index,
    Object data
  ) {

    // Check for missing
    // -----------------
    Object ob = Array.get (data, index);
    if (missing != null && ob.equals (missing)) 
      return (Double.NaN);

    // Get value and scale
    // -------------------
    double val;
    if (isUnsigned) {
      switch (unsignedType) {
      case UNSIGNED_BYTE:
        val = (short) (((Number) ob).byteValue() & 0xff);
        break;
      case UNSIGNED_SHORT:
        val = (int) (((Number) ob).shortValue() & 0xffff);
        break;
      case UNSIGNED_INT:
        val = (long) (((Number) ob).intValue() & 0xffffffff);
        break;
      case UNSIGNED_LONG:
        val = ((Number) ob).doubleValue();
        break;
      default:
        throw new RuntimeException ("Unsupported unsigned type");
      } // switch
    } // if
    else
      val = ((Number) ob).doubleValue();
    if (lookup != null)
      val = lookup[(int) val];
    else if (scaling != null)
      val = (val - scaling[1])*scaling[0];
  
    return (val);

  } // getValue  

  ////////////////////////////////////////////////////////////

  /** 
   * Reads a scaled data value.  The data value is read from the data
   * array and scaled according to the scaling factor and offset.
   *
   * @param index the index into the data array.  
   *
   * @return the scaled data value as a <code>double</code>.  The
   * <code>Double.NaN</code> value is used if the data value is
   * missing.
   *
   * @see #getValue(DataLocation)
   */
  public double getValue (
    int index
  ) {

    return (getValue (index, data));

  } // getValue

  ////////////////////////////////////////////////////////////

  /** 
   * Writes a scaled data value.  The data value is scaled according
   * to the scaling factor and offset and written to the data array.
   *
   * @param loc the data location.
   * @param val the data value as a double.  If the data value is
   * <code>Double.NaN</code> and the missing value is non-null, the
   * missing value is written to the array.
   *
   * @see #setValue(int,double)
   */
  public void setValue (
    DataLocation loc,
    double val
  ) {

    setValue (loc.getIndex(dims), val);

  } // setValue

  ////////////////////////////////////////////////////////////

  /** 
   * Writes a scaled data value.  The data value is scaled according
   * to the scaling factor and offset and written to the data array.
   *
   * @param index the index into the data array.
   * @param val the data value as a double.  If the data value is
   * <code>Double.NaN</code> and the missing value is non-null, the
   * missing value is written to the array.
   *
   * @see #setValue(DataLocation,double)
   */
  public void setValue (
    int index,
    double val
  ) {

    setValue (index, data, val);

  } // setValue 

  ////////////////////////////////////////////////////////////

  /** 
   * Writes a scaled data value.  The data value is scaled according
   * to the scaling factor and offset and written to the data array.
   *
   * @param index the index into the data array.
   * @param data the data array to use for raw data.
   * @param val the data value as a double.  If the data value is
   * <code>Double.NaN</code> and the missing value is non-null, the
   * missing value is written to the array.
   */
  protected void setValue (
    int index,
    Object data,
    double val
  ) {

    // Check for lookup table
    // ----------------------
    if (lookup != null) 
      throw new UnsupportedOperationException (
        "Cannot set value with lookup table");

    // Check for missing
    // -----------------
    if (Double.isNaN (val)) {
      if (missing != null) Array.set (data, index, missing);
      return;
    } // if

    // Scale value and set
    // -------------------
    if (scaling != null)
      val = val/scaling[0] + scaling[1];
    if (isUnsigned) {
      switch (unsignedType) {
      case UNSIGNED_BYTE:
        ((byte[]) data)[index] = (byte) ((short) Math.round (val) & 0xff);
        break;
      case UNSIGNED_SHORT:
        ((short[]) data)[index] = (short) ((int) Math.round (val) & 0xffff);
        break;
      case UNSIGNED_INT:
        ((int[]) data)[index] = (int) ((long) Math.round (val) & 0xffffffff);
        break;
      case UNSIGNED_LONG:
        ((long[]) data)[index] = (long) Math.round (val);
        break;
      default:
        throw new RuntimeException ("Unsupported unsigned type");
      } // switch
    } // if
    else {
      if (data instanceof double[])
        ((double[]) data)[index] = val;
      else if (data instanceof float[])
        ((float[]) data)[index] = (float) val;
      else if (data instanceof long[])
        ((long[]) data)[index] = (long) Math.round (val);
      else if (data instanceof int[])
        ((int[]) data)[index] = (int) Math.round (val);
      else if (data instanceof short[]) 
        ((short[]) data)[index] = (short) Math.round (val);
      else if (data instanceof byte[]) 
        ((byte[]) data)[index] = (byte) Math.round (val);
    } // else

  } // setValue

  ////////////////////////////////////////////////////////////

  /**
   * Formats a data value to a string.
   *
   * @param loc the data value location.
   *
   * @return the formatted data value string.
   *
   * @see #format
   * @see #format(int)
   */
  public String format (
    DataLocation loc
  ) {

    return (format (getValue (loc)));

  } // format

  ////////////////////////////////////////////////////////////

  /**
   * Formats a data value to a string.
   *
   * @param index the index into the data array.  
   *
   * @return the formatted data value string.
   *
   * @see #format
   * @see #format(DataLocation)
   */
  public String format (
    int index
  ) {

    return (format.format (getValue (index)));

  } // format

  ////////////////////////////////////////////////////////////

  /**
   * Formats a data value to a string.
   *
   * @param val the data value as a double.
   *
   * @return the formatted data value string.
   *
   * @see #format(DataLocation)
   * @see #format(int)
   */
  public String format (
    double val
  ) {

    return (format.format (val));

  } // format

  ////////////////////////////////////////////////////////////

  /**
   * Gets the data variable statistics.  The computed statistics
   * include the number of valid data values sampled, minimum,
   * maximum, mean, and standard deviation.
   *
   * @param factor the sample size as a fraction of the total number of
   * values.  For example, to sample 1 percent of the data the sample
   * size is 0.01.  The sample size is used to compute an optimal
   * stride while preserving sample frequency uniformity in each
   * dimension.  The total number of sampled values is guaranteed to
   * be bounded below by the requested sample size.
   * 
   * @return the data statistics.  If the number of valid data values
   * is zero, the minimum, maximum, mean, and standard deviation
   * values are undefined.  
   *
   * @see #getStatistics(int[])
   */
  public Statistics getStatistics (
    double factor
  ) {

    return (getStatistics (null, null, factor));

  } // getStatistics

  ////////////////////////////////////////////////////////////

  /**
   * Gets the optimal statistical sampling stride for this variable
   * based on a desired sampling factor and minimum number of values.
   * This method may be used in the case where statistics are desired
   * for a subset of the variable, but the sampling factor produces a
   * stride which is too large and results in too few data values
   * being retrieved.
   *
   * @param start the starting data location.
   * @param end the ending data location.
   * @param factor the sampling factor as a value in the range [0..1].
   * @param minCount the minimum desired number of values.  The stride
   * should ensure that the number of values actually sampled is
   * greater than this quantity.
   *
   * @return the optimal stride vector.
   * 
   * @see #getStatistics(DataLocation,DataLocation,double)
   * @see #getStatistics(DataLocation,DataLocation,int[])
   */
  public int[] getOptimalStride (
    DataLocation start,
    DataLocation end,
    double factor,
    int minCount
  ) {

    // Calculate proposed stride
    // -------------------------
    int rank = getRank();
    int stride = (int) Math.floor (Math.pow (1/factor, 1.0/rank));

    // Calculate actual number of sampled values
    // -----------------------------------------
    int sampleCount = 1;
    for (int i = 0; i < rank; i++) {
      sampleCount *= 1 +
        (int) Math.floor (Math.abs (start.get(i) - end.get(i))) / stride;
    } // for

    // Adjust stride if sampled values too low
    // ---------------------------------------
    if (sampleCount < minCount) {
      int totalCount = 1;
      for (int i = 0; i < rank; i++) {
        totalCount *= 1 +
          (int) Math.floor (Math.abs (start.get(i) - end.get(i)));
      } // for
      double newFactor = ((double) minCount) / totalCount;
      stride = (int) Math.floor (Math.pow (1/newFactor, 1.0/rank));
      if (stride < 1) stride = 1;
    } // if

    // Create stride array
    // -------------------
    int[] strideArray = new int[rank];
    Arrays.fill (strideArray, stride);

    return (strideArray);

  } // getOptimalStride

  ////////////////////////////////////////////////////////////

  /**
   * Gets the data variable statistics.  The computed statistics
   * include the number of valid data values sampled, minimum,
   * maximum, mean, and standard deviation.
   *
   * @param start the starting data location.  If null, the start is
   * the first location.
   * @param end the ending data location.  If null, the end is
   * the last location.
   * @param factor the sample size as a fraction of the total number of
   * values.  For example, to sample 1 percent of the data the sample
   * size is 0.01.  The sample size is used to compute an optimal
   * stride while preserving sample frequency uniformity in each
   * dimension.  The total number of sampled values is guaranteed to
   * be bounded below by the requested sample size.
   * 
   * @return the data statistics.  If the number of valid data values
   * is zero, the minimum, maximum, mean, and standard deviation
   * values are undefined.  
   *
   * @see #getStatistics(DataLocation,DataLocation,int[])
   */
  public Statistics getStatistics (
    DataLocation start,
    DataLocation end,
    double factor
  ) {

    // Compute stride
    // --------------
    int rank = getRank();
    int[] stride = new int[rank];
    Arrays.fill (stride, (int) Math.floor (Math.pow (1/factor, 1.0/rank)));

    // Get statistics
    // --------------    
    return (getStatistics (start, end, stride));

  } // getStatistics

  ////////////////////////////////////////////////////////////

  /**
   * Gets the data variable statistics.  The computed statistics
   * include the number of valid data values sampled, minimum,
   * maximum, mean, and standard deviation.
   *
   * @param stride the data location stride in each dimension.  The
   * stride is used to reduce the number of calculations required to
   * compute the statistics.  The data variable is sampled at every
   * nth value along each dimension.  For example if the stride is 4,
   * every 4th value is sampled.  If null, the stride is 1 in each
   * dimension.
   * 
   * @return the data statistics.  If the number of valid data values
   * is zero, the minimum, maximum, mean, and standard deviation
   * values are undefined.
   *
   * @see #getStatistics(double)
   */
  public Statistics getStatistics (
    int[] stride
  ) {

    return (getStatistics (null, null, stride));

  } // getStatistics

  ////////////////////////////////////////////////////////////

  /**
   * Gets the data variable statistics.  The computed statistics
   * include the number of valid data values sampled, minimum,
   * maximum, mean, and standard deviation.
   *
   * @param start the starting data location.  If null, the start is
   * the first location.
   * @param end the ending data location.  If null, the end is
   * the last location.
   * @param stride the data location stride in each dimension.  The
   * stride is used to reduce the number of calculations required to
   * compute the statistics.  The data variable is sampled at every
   * nth value along each dimension.  For example if the stride is 4,
   * every 4th value is sampled.  If null, the stride is 1 in each
   * dimension.
   * 
   * @return the data statistics.  If the number of valid data values
   * is zero, the minimum, maximum, mean, and standard deviation
   * values are set to <code>Double.NaN</code>.
   *
   * @throws RuntimeException if the start or end locations are
   * invalid.
   *
   * @see #getStatistics(DataLocation,DataLocation,double)
   */
  public Statistics getStatistics (
    DataLocation start,
    DataLocation end,
    int[] stride
  ) {

    // Check start and end
    // -------------------
    if (start == null) 
      start = new DataLocation(dims.length);
    else if (!start.isValid())
      throw new RuntimeException ("Starting data location is invalid");
    if (end == null) { 
      end = new DataLocation(dims.length);
      for (int i = 0; i < dims.length; i++) end.set (i, dims[i]-1);
    } // if
    else if (!end.isValid())
      throw new RuntimeException ("Ending data location is invalid");

    // Set stride
    // ----------
    if (stride == null) { 
      stride = new int[dims.length]; 
      Arrays.fill (stride, 1);
    } // if

    // Calculate statistics
    // --------------------
    DataVariableIterator iter = new DataVariableIterator (this, 
      new StrideLocationIterator (start, end, stride));
    Statistics stats = new Statistics (iter);
    return (stats);

  } // getStatistics

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the number of digits after the decimal place in a string
   * representing a fractional number with possible exponential part.
   *
   * @param number the fractional number string.
   *
   * @return the number of decimal digits.
   */
  public static int getDecimals (
    String number  
  ) {

    int i = number.indexOf ('.');
    int digits = 0;
    if (i != -1) {
      i++;
      while (i < number.length() && 
        Character.toUpperCase (number.charAt (i)) != 'E') {
        i++;
        digits++;
      } // while
    } // if

    return (digits);

  } // getDecimals

  ////////////////////////////////////////////////////////////

  /**
   * Converts the units of this variable to the new units.  The
   * scaling factor and offset are adjusted accordingly.  If the new
   * units are identical to the existing units, no operation is
   * performed.
   *
   * @param newUnitSpec the new units to convert to.
   *
   * @throws IllegalArgumentException if the existing or new data
   * units cannot be recognized, or if the new units are incompatible
   * with the existing units.
   */
  public void convertUnits (
    String newUnitSpec
  ) {

    // Check for string equality
    // -------------------------
    if (newUnitSpec.equals (units)) return;

    // Check for compatibility
    // -----------------------
    Unit oldUnit = UnitFactory.create (units);
    Unit newUnit = UnitFactory.create (newUnitSpec);
    if (!newUnit.isCompatible (oldUnit)) {
      throw new IllegalArgumentException ("New units '" + newUnitSpec + 
        "' are incompatible with existing units '" + units + "'");
    } // if

    // Check if conversion needed
    // --------------------------
    if (newUnit.equals (oldUnit)) return;

    // Get conversion coefficients
    // ---------------------------
    double a, b;
    try {
      b = oldUnit.convertTo (0, newUnit);
      a = oldUnit.convertTo (1, newUnit) - b;
    } // try
    catch (ConversionException e) {
      throw new RuntimeException (e);
    } // catch

    // Modify units and scaling
    // ------------------------
    units = newUnitSpec;
    if (scaling == null) scaling = new double[] {1, 0};
    scaling[1] = scaling[1] - b/(scaling[0]*a);
    scaling[0] = a*scaling[0];

  } // convertUnits

  ////////////////////////////////////////////////////////////

  /**
   * Sets a hint to the variable that subsequent data access will
   * only read/write values within the specified data extents.
   * This method performs no action in the abstract class, but
   * may be used by subclasses that wish to setup internal
   * structures prior to some sort of intensive data access.  For
   * example, if data rendering data is to occur, it might be
   * useful to read in only the data that will be rendered and
   * make it availalble.
   * 
   * @param start the starting data coordinates.
   * @param end the ending data coordinates.
   * @param stride the data stride along each dimension.
   */
  public void setAccessHint (
    int[] start,
    int[] end,
    int[] stride
  ) {

    // Do nothing

  } // setAccessHint

  ////////////////////////////////////////////////////////////

  /** Gets a string representation of this variable. */
  public String toString () { return (name); }

  ////////////////////////////////////////////////////////////

  /**
   * Disposes of any resources used by this variable prior to being
   * finalized.  This method does nothing at this level, but may be
   * overridden in the subclass to release resources.
   */
  public void dispose () { }

  ////////////////////////////////////////////////////////////

} // DataVariable class

////////////////////////////////////////////////////////////////////////
