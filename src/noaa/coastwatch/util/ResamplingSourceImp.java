////////////////////////////////////////////////////////////////////////
/*

     File: ResamplingSourceImp.java
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
package noaa.coastwatch.util;

// Imports
// -------
import noaa.coastwatch.util.DataLocation;
import noaa.coastwatch.util.EarthLocation;

/**
 * <p>The <code>ResamplingSourceImp</code> class provides an extra set
 * of query methods about the source transform in a resampling
 * operation.</p>
 *
 * @author Peter Hollemans
 * @since 3.5.0
 */
public interface ResamplingSourceImp {

  ////////////////////////////////////////////////////////////

  /**
   * Determines if a source data location is valid to be used in the
   * resampling operation.
   *
   * @param loc the location to determine for use.
   *
   * @return true if the location should be used, or false if not.
   */
  public boolean isValidLocation (DataLocation loc);

  ////////////////////////////////////////////////////////////

  /**
   * Determines if the nearest source transform earth location is valid to be
   * used for resampling to the specified destination earth location.  To be
   * valid, the destination earth location must fall within the bounds of the
   * pixel defined by the nearest source location.
   *
   * @param earthLoc the destination earth location to check.
   * @param nearestDataLoc the nearest source data location to the
   * specified destination earth location.
   * @param contextObj a context object obtained from {@link getContext}.
   *
   * @return true if the destination location falls within the area of the
   * nearest source location, or false if not.
   */
  public boolean isValidNearestLocation (
    EarthLocation earthLoc,
    DataLocation nearestDataLoc,
    Object contextObj
  );

  ////////////////////////////////////////////////////////////

  /**
   * Gets the width and height of the window to search for the next nearest
   * location during resampling.  This is the window inside which the second
   * nearest location may be found.
   *
   * @return the window size in pixels.
   */
  public int getWindowSize();

  ////////////////////////////////////////////////////////////

  /**
   * Gets a context object used to store state between subsequent calls
   * to {@link isValidNearestLocation}.
   *
   * @return the new context object.
   */
  public Object getContext();

  ////////////////////////////////////////////////////////////

} // ResamplingSourceImp class

////////////////////////////////////////////////////////////////////////
