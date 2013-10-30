////////////////////////////////////////////////////////////////////////
/*
     FILE: Statistics.java
  PURPOSE: To hold statistical data measurements.
   AUTHOR: Peter Hollemans
     DATE: 2003/09/08
  CHANGES: 2004/03/27, PFH, modified to use DataIterator
           2004/03/29, PFH, added getData() method
           2004/06/11, PFH, added null histogram check in getNormalizedCount()
           2005/09/07, PFH, added average deviation
           2007/06/18, PFH, added median value

  CoastWatch Software Library and Utilities
  Copyright 2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.util;

// Imports
// -------
import java.util.*;

/**
 * The statistics class is a container for various data variable
 * statistics such as minimum, maximum, mean, standard deviation,
 * histogram counts, and so on.
 *
 * @author Peter Hollemans
 * @since 3.1.6
 */
public class Statistics {

  // Constants
  // ---------

  /** The number of histogram bins. */
  private static final int HISTOGRAM_BINS = 100;

  // Variables
  // ---------

  /** The total number of data values sampled, including invalid data. */
  private int values;

  /** The total number of valid data values sampled. */
  private int valid;

  /** The minimum data value. */
  private double min;

  /** The maximum data value. */
  private double max;

  /** The mean data value. */
  private double mean;

  /** The standard deviation from the mean. */
  private double stdev;

  /** The average deviation from the mean. */
  private double adev;

  /** The histogram data bins. */
  private int[] histogram;

  /** The histogram bin width in data units. */
  private double binWidth;

  /** The maximum count histogram bin. */
  private int maxCountBin;

  /** The data values array. */
  private double[] dataArray;

  /** The median value of the data. */
  private double median;

  ////////////////////////////////////////////////////////////

  /** Gets a test statistics object with normal distribution. */
  public static Statistics getTestData (
    final long seed
  ) {

    final Random rand = new Random (seed);
    Statistics stats = new Statistics (new DataIterator () {
        private int i = 0;
        public boolean hasNext() { return (i < 1000); }
        public Object next() { return (new Double (nextDouble())); }
        public double nextDouble () {
          i++;
          return (50+rand.nextGaussian()*10);
        } // next
        public void reset() { i = 0; rand.setSeed (seed); }
        public void remove () { throw new UnsupportedOperationException(); }
      });
    return (stats);

  } // getTestData

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the total number of data values sampled, including invalid
   * data.
   */
  public int getValues () { return (values); }

  ////////////////////////////////////////////////////////////

  /** Gets the total number of valid data values sampled. */
  public int getValid () { return (valid); }

  ////////////////////////////////////////////////////////////

  /** Gets the minimum data value. */
  public double getMin () { return (min); }

  ////////////////////////////////////////////////////////////

  /** Gets the maximum data value. */
  public double getMax () { return (max); }

  ////////////////////////////////////////////////////////////

  /** Gets the mean data value. */
  public double getMean () { return (mean); }

  ////////////////////////////////////////////////////////////

  /** Gets the standard deviation from the mean. */
  public double getStdev () { return (stdev); }

  ////////////////////////////////////////////////////////////

  /** Gets the average deviation from the mean. */
  public double getAdev () { return (adev); }

  ////////////////////////////////////////////////////////////

  /** Gets the median value of the data. */
  public double getMedian () { return (median); }

  ////////////////////////////////////////////////////////////

  /**
   * Gets the specified data value.  This is only possible if the
   * <code>saveData</code> flag was specified to be true in the
   * constructor.
   *
   * @param index the data value index.
   *
   * @return the data value or <code>Double.NAN</code> if no data was
   * saved.
   */
  public double getData (
    int index                         
  ) { 

    if (dataArray == null) return (Double.NaN);
    else return (dataArray[index]);

  } // getData

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new set of statistics.  Using this constructor, the
   * data values are not saved.
   *
   * @param iter an iterator over all data values required for the
   * statistics calculations.
   */
  public Statistics (
    DataIterator iter
  ) {

    this (iter, false);

  } // Statistics constructor

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new set of statistics.
   *
   * @param iter an iterator over all data values required for the
   * statistics calculations.
   * @param saveData the save data flag, true if the data values used
   * in the statistics are to be saved.  If the values are saved, they
   * will be available from the {@link #getData} method.
   */
  public Statistics (
    DataIterator iter,
    boolean saveData
  ) {

    // Initialize
    // ----------
    values = valid = 0;
    min = Double.MAX_VALUE;
    max = -Double.MAX_VALUE;
    mean = stdev = median = Double.NaN;
    histogram = null;
    binWidth = 0;

    // Loop over each sample value
    // ---------------------------
    double sum = 0;
    while (iter.hasNext()) {

      // Increment sampled values
      // ------------------------
      values++;

      // Check for missing value
      // -----------------------
      double val = iter.nextDouble();
      if (Double.isNaN (val)) continue;
      valid++;

      // Set min and max
      // ---------------
      min = Math.min (min, val);
      max = Math.max (max, val);

      // Accumulate sum
      // --------------
      sum += val;

    } // while

    // Check for zero valid
    // --------------------
    if (valid == 0) {
      min = Double.NaN;
      max = Double.NaN;
      return;
    } // if

    // Calculate mean
    // --------------
    mean = sum / valid;

    // Check for single valid value
    // ----------------------------
    if (valid == 1) {
      stdev = 0;
      median = mean;
      return;
    } // if

    // Create histogram data
    // ---------------------
    histogram = new int[HISTOGRAM_BINS];
    binWidth = (max-min)/HISTOGRAM_BINS;

    // Create data array
    // -----------------
    if (saveData) {
      dataArray = new double[values];
    } // if

    // Create median data array
    // ------------------------
    double[] medianDataArray = new double[valid];

    // Loop over each sample value again
    // ---------------------------------
    sum = 0;
    double asum = 0;
    double correction = 0;
    int dataIndex = 0;
    int medianIndex = 0;
    iter.reset();
    while (iter.hasNext()) {

      // Get value
      // ---------
      double val = iter.nextDouble();
      if (saveData) { dataArray[dataIndex] = val; dataIndex++; }
      if (Double.isNaN (val)) continue;

      // Accumulate sums for standard deviation
      // --------------------------------------
      double diff = val - mean;
      sum += Math.pow (diff, 2);
      correction += (diff);

      // Accumulate sum for average deviation
      // ------------------------------------
      asum += Math.abs (diff);

      // Add to histogram bins
      // ---------------------
      int bin = (int) ((val-min) / binWidth);
      if (bin == HISTOGRAM_BINS) bin--;
      histogram[bin]++;

      // Add to median data array
      // ------------------------
      medianDataArray[medianIndex] = val;
      medianIndex++;

    } // while

    // Calculate standard deviation
    // ----------------------------
    sum -= (1.0/valid) * Math.pow (correction, 2);
    stdev = Math.sqrt (sum/(valid - 1));

    // Calculate average deviation
    // ---------------------------
    adev = asum/valid;

    // Find maximum count bin
    // ----------------------
    maxCountBin = 0;
    for (int i = 1; i < HISTOGRAM_BINS; i++) {
      if (histogram[i] > histogram[maxCountBin]) maxCountBin = i;
    } // for

    // Calculate median value
    // ----------------------
    Arrays.sort (medianDataArray, 0, valid);
    if (valid%2 == 0) 
      median = (medianDataArray[valid/2 - 1] + medianDataArray[valid/2]) / 2;
    else
      median = medianDataArray[(valid+1)/2 - 1];

  } // Statistics constructor

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the histogram count for a data value.
   * 
   * @param val the data value for the desired bin.
   *
   * @return the histogram bin count or 0 if the data value is
   * out of range.
   */
  public int getCount (
    double val
  ) {

    if (histogram == null) return (0);
    int bin = (int) ((val-min) / binWidth);
    if (bin < 0 || bin > HISTOGRAM_BINS-1) return (0);
    return (histogram[bin]);

  } // getCount

  ////////////////////////////////////////////////////////////

  /** 
   * Gets the normalized histogram count for a data value.
   * 
   * @param val the data value for the desired bin.
   *
   * @return the histogram bin count divided by the count of the
   * maximum bin or 0 if the data value is out of range.
   */
  public double getNormalizedCount (
    double val
  ) {

    if (histogram == null) return (0);
    return ((double) getCount (val) / histogram[maxCountBin]);

  } // getNormalizedCount

  ////////////////////////////////////////////////////////////

  /** Converts this statistics object to a string. */
  public String toString () {

    String str = "Statistics[";
    str += "values=" + values + ",";
    str += "valid=" + valid + ",";
    str += "min=" + min + ",";
    str += "max=" + max + ",";
    str += "mean=" + mean + ",";
    str += "stdev=" + stdev + ",";
    str += "histogram=[";
    for (int i = 0; i < HISTOGRAM_BINS; i++) {
      str += Integer.toString (histogram[i]);
      if (i < HISTOGRAM_BINS-1) str += ",";
    } // for
    str += "],";
    str += "binWidth=" + binWidth + ",";
    str += "maxCountBin=" + maxCountBin + ",";
    str += "median=" + median + "]";
    return (str);

  } // toString

  ////////////////////////////////////////////////////////////

  /** Tests this class with a very simple data set. */
  public static void main (String[] argv) {

    Statistics stats = new Statistics (new DataIterator () {
        private int index = 0;
        private double[] data = new double[] {7,1,Double.NaN,2,4,5,6,3,Double.NaN};
        public double nextDouble () {
          return (data[index++]);
        } // nextDouble
        public void reset () { index = 0; }
        public boolean hasNext() { return (index < data.length); }
        public void remove () { throw new RuntimeException(); }
        public Object next () { return (new Double (nextDouble())); }
      });
    System.out.println (stats);

  } // main

  ////////////////////////////////////////////////////////////

} // Statistics class

////////////////////////////////////////////////////////////////////////
