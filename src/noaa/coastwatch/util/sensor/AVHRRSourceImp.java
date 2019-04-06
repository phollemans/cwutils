////////////////////////////////////////////////////////////////////////
/*

     File: AVHRRSourceImp.java
   Author: Peter Hollemans
     Date: 2019/03/05

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
import noaa.coastwatch.util.sensor.GenericSourceImp;

import static noaa.coastwatch.util.Grid.ROW;
import static noaa.coastwatch.util.Grid.COL;

/**
 * <p>The <code>AVHRRSourceImp</code> helps resample AVHRR data.</p>
 *
 * @author Peter Hollemans
 * @since 3.5.0
 */
public class AVHRRSourceImp extends GenericSourceImp {

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new AVHRR resampling helper using the specified transform.
   *
   * @param sourceTrans the source transform to use for AVHRR swath location
   * data.
   *
   * @return the new resampling helper.
   */
  public static AVHRRSourceImp getInstance (
    EarthTransform sourceTrans
  ) {
  
    // Check dimensions
    // ----------------
    int[] sourceDims = sourceTrans.getDimensions();
    if (sourceDims[COL] != 2048)
      throw new RuntimeException ("Invalid number of columns for AVHRR data");

    return (new AVHRRSourceImp (sourceTrans));
  
  } // getInstance

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new AVHRR resampling helper using the specified transform.
   *
   * @param sourceTrans the source transform to use for AVHRR swath location
   * data.
   */
  private AVHRRSourceImp (
    EarthTransform sourceTrans
  ) {

    super (sourceTrans);
    
  } // AVHRRSourceImp constructor

  ////////////////////////////////////////////////////////////

  @Override
  public int getWindowSize() { return (5); }

  ////////////////////////////////////////////////////////////

} // AVHRRSourceImp class

////////////////////////////////////////////////////////////////////////

