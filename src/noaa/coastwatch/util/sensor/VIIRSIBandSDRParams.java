////////////////////////////////////////////////////////////////////////
/*

     File: VIIRSIBandSDRParams.java
   Author: Peter Hollemans
     Date: 2020/04/16

  CoastWatch Software Library and Utilities
  Copyright (c) 2020 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.util.sensor;

/**
 * <p>The <code>VIIRSIBandSDRParams</code> class provides parameters
 * for the VIIRS I-band Scientific Data Record (SDR) scan and deletion pattern.
 * The I-band SDR scan is 6400 pixels wide by 32 pixels high and has some
 * pixels deleted on the top and bottom four rows which results in less overlap
 * of the scan than for example MODIS.  The deletion pattern is as follows:</p>
 *
 * <ul>
 *   <li>1st row - First and last 2016 pixels deleted</li>
 *   <li>2nd row - First and last 2016 pixels deleted</li>
 *   <li>3rd row - First and last 1280 pixels deleted</li>
 *   <li>4th row - First and last 1280 pixels deleted</li>
 *   <li>5-28th rows - No deletions</li>
 *   <li>29th row - First and last 1280 pixels deleted</li>
 *   <li>30th row - First and last 1280 pixels deleted</li>
 *   <li>31st row - First and last 2016 pixels deleted</li>
 *   <li>32nd row - First and last 2016 pixels deleted</li>
 * </ul>
 *
 * <p>The pattern results in a scan that looks as follows:</p>
 * <pre>
 *     0       1280   2016                       4384   5120     6399
 *  0   ****************---------------------------****************
 *  1   ****************---------------------------****************
 *  2   *********-----------------------------------------*********
 *  3   *********-----------------------------------------*********
 *  4   -----------------------------------------------------------
 *  ..  -----------------------------------------------------------
 *  ..  -----------------------------------------------------------
 *  28  -----------------------------------------------------------
 *  29  *********-----------------------------------------*********
 *  30  *********-----------------------------------------*********
 *  31  ****************---------------------------****************
 *  32  ****************---------------------------****************
 * </pre>
 *
 * @author Peter Hollemans
 * @since 3.6.0
 */
public class VIIRSIBandSDRParams implements VIIRSSensorParams {

  /** The start/end pixel deletion count for each row. */
  private static final int[] DELETIONS = new int[] {
    2016,   // row 1
    2016,   // row 2
    1280,   // row 3
    1280,   // row 4
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // rows 5-28
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    1280,   // row 29
    1280,   // row 30
    2016,   // row 31
    2016,   // row 32
  };

  /** The top non-deleted row at each scan column. */
  private int[] topRowData;

  ////////////////////////////////////////////////////////////

  @Override
  public int getScanWidth() { return (6400); }

  ////////////////////////////////////////////////////////////

  @Override
  public int getScanHeight() { return (32); }
  
  ////////////////////////////////////////////////////////////

  /** Creates a new parameters object. */
  public VIIRSIBandSDRParams() {
  
    // Compute top row data
    // --------------------
    topRowData = new int[6400];
    for (int col = 0; col < 6400; col++) {
      int row = 0;
      while (col < DELETIONS[row] || col >= (6400 - DELETIONS[row])) row++;
      topRowData[col] = row;
    } // for
  
  } // VIIRSIBandSDRParams

  ////////////////////////////////////////////////////////////

  @Override
  public int getTopRowAtColumn (
    int col
  ) {
  
    return (topRowData[col]);

  } // getTopRowAtColumn

  ////////////////////////////////////////////////////////////

  @Override
  public boolean[][] getDeletionPattern() {
  
    boolean[][] pattern = new boolean[32][6400];
    for (int row = 0; row < 32; row++) {
      for (int col = 0; col < 6400; col++) {
        pattern[row][col] = (col < DELETIONS[row]) || col >= (6400 - DELETIONS[row]);
      } // for
    } // for

    return (pattern);

  } // getDeletionPattern

  ////////////////////////////////////////////////////////////

} // VIIRSIBandSDRParams class

////////////////////////////////////////////////////////////////////////

