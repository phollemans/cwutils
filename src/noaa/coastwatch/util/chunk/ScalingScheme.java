////////////////////////////////////////////////////////////////////////
/*

     File: ScalingScheme.java
   Author: Peter Hollemans
     Date: 2021/03/06

  CoastWatch Software Library and Utilities
  Copyright (c) 2021 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.util.chunk;

/**
 * The <code>ScalingScheme</code> interface is implemented by concrete classes
 * that have a way of scaling floating point data.
 *
 * @author Peter Hollemans
 * @since 3.6.1
 */
public interface ScalingScheme {

  /**
   * Accepts a visitor in this scheme.
   *
   * @param visitor the visitor to accept.
   */
  public void accept (ScalingSchemeVisitor visitor);
  
} // ScalingScheme interface

////////////////////////////////////////////////////////////////////////
