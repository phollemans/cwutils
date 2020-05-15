////////////////////////////////////////////////////////////////////////
/*

     File: VIIRSMBandSDRParams.java
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
 * <p>The <code>VIIRSMBandSDRParams</code> class provides parameters
 * for the VIIRS M-band Scientific Data Record (SDR) scan and deletion pattern.
 * The M-band SDR scan is 3200 pixels wide by 16 pixels high and has some
 * pixels deleted on the top and bottom two rows which results in less overlap
 * of the scan than for example MODIS.The deletion pattern is as follows:</p>
 *
 * <ul>
 *   <li>1st row - First and last 1008 pixels deleted</li>
 *   <li>2nd row - First and last 640 pixels deleted</li>
 *   <li>3rd-14th rows - No deletions</li>
 *   <li>15th row - First and last 640 pixels deleted</li>
 *   <li>16th row - First and last 1008 pixels deleted</li>
 * </ul>
 *
 * <p>The pattern results in a scan that looks as follows:</p>
 * <pre>
 *     0    640       1008                       2192      2560   3199
 *  0   ****************---------------------------****************
 *  1   ******-----------------------------------------------******
 *  2   -----------------------------------------------------------
 *  ..  -----------------------------------------------------------
 *  ..  -----------------------------------------------------------
 *  13  -----------------------------------------------------------
 *  14  ******-----------------------------------------------******
 *  15  ****************---------------------------****************
 * </pre>
 *
 * @author Peter Hollemans
 * @since 3.6.0
 */
public class VIIRSMBandSDRParams implements VIIRSSensorParams {

  /** The start/end pixel deletion count for each row. */
  private static final int[] DELETIONS = new int[] {
    1008, // row 1
    640,  // row 2
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // rows 3-14
    640,  // row 15
    1008  // row 16
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
  public VIIRSMBandSDRParams() {
  
    // Compute top row data
    // --------------------
    topRowData = new int[3200];
    for (int col = 0; col < 3200; col++) {
      int row = 0;
      while (col < DELETIONS[row] || col >= (3200 - DELETIONS[row])) row++;
      topRowData[col] = row;
    } // for
  
  } // VIIRSMBandSDRParams

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

} // VIIRSMBandSDRParams class

////////////////////////////////////////////////////////////////////////




