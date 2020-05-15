////////////////////////////////////////////////////////////////////////
/*

     File: VIIRSSensorParams.java
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
 * The <code>VIIRSSensorParams</code> class provides methods that describe
 * the VIIRS scan pattern and deleted pixels for a specific VIIRS sensor scan
 * mode and deletion pattern.
 *
 * @author Peter Hollemans
 * @since 3.6.0
 */
public interface VIIRSSensorParams {

  ////////////////////////////////////////////////////////////

  /**
   * Gets the scan width.
   *
   * @return the scan width in pixels.
   */
  public int getScanWidth();

  ////////////////////////////////////////////////////////////

  /**
   * Gets the scan height.
   *
   * @return the scan height in pixels of one simultenous scan within a
   * VIIRS granule.
   */
  public int getScanHeight();
  
  ////////////////////////////////////////////////////////////

  /**
   * Gets the top edge row of the swath at the specified column for any scan.
   *
   * @param col the column to query, [0..scanWidth].
   *
   * @return the row within the data that forms the top of the non-deleted
   * part of the swath at the specified column.
   */
  public int getTopRowAtColumn (
    int col
  );

  ////////////////////////////////////////////////////////////

  /**
   * Gets an array containing the deleted pixel pattern.
   *
   * @return the pixel deletion pattern for this sensor as an array of
   * booleans of size [scanHeight][scanWidth] in which true indicates a
   * pixel is deleted, and false indicates the pixel is valid.
   */
  public boolean[][] getDeletionPattern();

  ////////////////////////////////////////////////////////////

} // VIIRSSensorParams class

////////////////////////////////////////////////////////////////////////

