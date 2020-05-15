////////////////////////////////////////////////////////////////////////
/*

     File: VIIRSIBandEDRParams.java
   Author: Peter Hollemans
     Date: 2020/04/20

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
 * <p>The <code>VIIRSIBandEDRParams</code> class provides parameters
 * for the VIIRS I-band Environmental Data Record (EDR) scan and deletion pattern.
 * The I-band EDR scan is 6400 pixels wide by 32 pixels high and has some
 * pixels deleted on the top and bottom eight rows of the scan which results
 * in less overlap than the SDR scan.  The deletion pattern is as follows:</p>
 *
 * <ul>
 *   <li>1st row - First and last 2180 pixels deleted</li>
 *   <li>2nd row - First and last 2180 pixels deleted</li>
 *   <li>3rd row - First and last 1640 pixels deleted</li>
 *   <li>4th row - First and last 1640 pixels deleted</li>
 *   <li>5th row - First and last 1040 pixels deleted</li>
 *   <li>6th row - First and last 1040 pixels deleted</li>
 *   <li>7th row - First and last 260 pixels deleted</li>
 *   <li>8th row - First and last 260 pixels deleted</li>
 *   <li>9-24th rows - No deletions</li>
 *   <li>25th row - First and last 260 pixels deleted</li>
 *   <li>26th row - First and last 260 pixels deleted</li>
 *   <li>27th row - First and last 1040 pixels deleted</li>
 *   <li>28th row - First and last 1040 pixels deleted</li>
 *   <li>29th row - First and last 1640 pixels deleted</li>
 *   <li>30th row - First and last 1640 pixels deleted</li>
 *   <li>31st row - First and last 2180 pixels deleted</li>
 *   <li>32nd row - First and last 2180 pixels deleted</li>
 * </ul>
 *
 * <p>The pattern results in a scan that looks as follows:</p>
 * <pre>
 *     0    260    1040  1640  2180     4220  4760  5360   6140  6399
 *  0   *************************---------*************************
 *  1   *************************---------*************************
 *  2   *******************---------------------*******************
 *  3   *******************---------------------*******************
 *  4   *************---------------------------------*************
 *  5   *************---------------------------------*************
 *  6   ******-----------------------------------------------******
 *  7   ******-----------------------------------------------******
 *  8   -----------------------------------------------------------
 *  ..  -----------------------------------------------------------
 *  ..  -----------------------------------------------------------
 *  23  -----------------------------------------------------------
 *  24  ******-----------------------------------------------******
 *  25  ******-----------------------------------------------******
 *  26  *************---------------------------------*************
 *  27  *************---------------------------------*************
 *  28  *******************---------------------*******************
 *  29  *******************---------------------*******************
 *  30  *************************---------*************************
 *  31  *************************---------*************************
 * </pre>
 *
 * @author Peter Hollemans
 * @since 3.6.0
 */
public class VIIRSIBandEDRParams implements VIIRSSensorParams {

  /** The start/end pixel deletion count for each row. */
  private static final int[] DELETIONS = new int[] {
    2180,   // row 1
    2180,   // row 2
    1640,   // row 3
    1640,   // row 4
    1040,   // row 5
    1040,   // row 6
    260,    // row 7
    260,    // row 8
    0, 0, 0, 0, 0, 0, 0, 0, // rows 9-24
    0, 0, 0, 0, 0, 0, 0, 0,
    260,    // row 25
    260,    // row 26
    1040,   // row 27
    1040,   // row 28
    1640,   // row 29
    1640,   // row 30
    2180,   // row 31
    2180    // row 32
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
  public VIIRSIBandEDRParams() {
  
    // Compute top row data
    // --------------------
    topRowData = new int[6400];
    for (int col = 0; col < 6400; col++) {
      int row = 0;
      while (col < DELETIONS[row] || col >= (6400 - DELETIONS[row])) row++;
      topRowData[col] = row;
    } // for
  
  } // VIIRSIBandEDRParams

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

} // VIIRSIBandEDRParams class

////////////////////////////////////////////////////////////////////////


