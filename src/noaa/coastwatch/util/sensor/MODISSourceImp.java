////////////////////////////////////////////////////////////////////////
/*

     File: MODISSourceImp.java
   Author: Peter Hollemans
     Date: 2019/03/13

  CoastWatch Software Library and Utilities
  Copyright (c) 2019 National Oceanic and Atmospheric Administration
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

// Imports
// -------
import noaa.coastwatch.util.trans.EarthTransform;
import noaa.coastwatch.util.sensor.MODISSourceImp;

import static noaa.coastwatch.util.Grid.ROW;
import static noaa.coastwatch.util.Grid.COL;

/**
 * <p>The <code>MODISSourceImp</code> helps resample MODIS data.</p>
 *
 * @author Peter Hollemans
 * @since 3.5.0
 */
public class MODISSourceImp extends GenericSourceImp {

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new MODIS resampling helper using the specified transform.
   *
   * @param sourceTrans the source transform to use for MODIS swath location
   * data.
   */
  public static MODISSourceImp getInstance (
    EarthTransform sourceTrans
  ) {
  
    // Check dimensions
    // ----------------
    int[] sourceDims = sourceTrans.getDimensions();
    if (sourceDims[ROW]%10 != 0)
      throw new RuntimeException ("Invalid number of rows for MODIS data");
    if (sourceDims[COL] != 1354)
      throw new RuntimeException ("Invalid number of columns for MODIS data");

    return (new MODISSourceImp (sourceTrans));
  
  } // getInstance

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new MODIS resampling helper using the specified transform.
   *
   * @param sourceTrans the source transform to use for MODIS swath location
   * data.
   *
   * @return the new resampling helper.
   */
  private MODISSourceImp (
    EarthTransform sourceTrans
  ) {

    super (sourceTrans);
    
  } // MODISSourceImp constructor

  ////////////////////////////////////////////////////////////

  @Override
  public int getWindowSize() { return (23); }

  ////////////////////////////////////////////////////////////

} // MODISSourceImp class

////////////////////////////////////////////////////////////////////////


