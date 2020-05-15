////////////////////////////////////////////////////////////////////////
/*

     File: VIIRSMBandEDRParams.java
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
 * <p>The <code>VIIRSMBandEDRParams</code> class provides parameters
 * for the VIIRS M-band Environmental Data Record (EDR) scan and deletion
 * pattern.  The M-band EDR scan is 3200 pixels wide by 16 pixels high and
 * has some pixels deleted on the top and bottom four rows of a scan which
 * results in less overlap than the SDR scan.  The deletion pattern is as
 * follows:</p>
 *
 * <ul>
 *   <li>1st row - First and last 1090 pixels deleted</li>
 *   <li>2nd row - First and last 820 pixels deleted</li>
 *   <li>3rd row - First and last 520 pixels deleted</li>
 *   <li>4th row - First and last 130 pixels deleted</li>
 *   <li>5-12th rows - No deletions</li>
 *   <li>13st row - First and last 130 pixels deleted</li>
 *   <li>14th row - First and last 520 pixels deleted</li>
 *   <li>15th row - First and last 820 pixels deleted</li>
 *   <li>16th row - First and last 1090 pixels deleted</li>
 * </ul>
 * 
 * <p>The pattern results in a scan that looks as follows:</p>
 * <pre>
 *     0    130    520   820   1090     2110  2380  2680   3070  3199
 *  0   *************************---------*************************
 *  1   *******************---------------------*******************
 *  2   *************---------------------------------*************
 *  3   ******-----------------------------------------------******
 *  4   -----------------------------------------------------------
 *  ..  -----------------------------------------------------------
 *  ..  -----------------------------------------------------------
 *  11  -----------------------------------------------------------
 *  12  ******-----------------------------------------------******
 *  13  *************---------------------------------*************
 *  14  *******************---------------------*******************
 *  15  *************************---------*************************
 * </pre>
 *
 * @author Peter Hollemans
 * @since 3.6.0
 */
public class VIIRSMBandEDRParams implements VIIRSSensorParams {

  /** The start/end pixel deletion count for each row. */
  private static final int[] DELETIONS = new int[] {
    1090, // row 1
    820,  // row 2
    520,  // row 3
    130,  // row 4
    0, 0, 0, 0, 0, 0, 0, 0, // rows 5-12
    130,  // row 13
    520,  // row 14
    820,  // row 15
    1090  // row 16
  };

  /** The top non-deleted row at each scan column. */
  private int[] topRowData;

  ////////////////////////////////////////////////////////////

  @Override
  public int getScanWidth() { return (3200); }

  ////////////////////////////////////////////////////////////

  @Override
  public int getScanHeight() { return (16); }
  
  ////////////////////////////////////////////////////////////

  /** Creates a new parameters object. */
  public VIIRSMBandEDRParams() {
  
    // Compute top row data
    // --------------------
    topRowData = new int[3200];
    for (int col = 0; col < 3200; col++) {
      int row = 0;
      while (col < DELETIONS[row] || col >= (3200 - DELETIONS[row])) row++;
      topRowData[col] = row;
    } // for
  
  } // VIIRSMBandEDRParams

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
  
    boolean[][] pattern = new boolean[16][3200];
    for (int row = 0; row < 16; row++) {
      for (int col = 0; col < 3200; col++) {
        pattern[row][col] = (col < DELETIONS[row]) || col >= (3200 - DELETIONS[row]);
      } // for
    } // for

    return (pattern);

  } // getDeletionPattern

  ////////////////////////////////////////////////////////////

} // VIIRSMBandEDRParams class

////////////////////////////////////////////////////////////////////////





