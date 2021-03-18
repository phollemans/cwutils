////////////////////////////////////////////////////////////////////////
/*

     File: ScalingSchemeVisitor.java
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
 * The <code>ScalingSchemeVisitor</code> interface is implemented by any class
 * that participates in the visitor pattern to perform operations on
 * {@link ScalingScheme} instances.
 *
 * @author Peter Hollemans
 * @since 3.6.1
 */
public interface ScalingSchemeVisitor {
  public void visitFloatScalingScheme (FloatScalingScheme scheme);
  public void visitDoubleScalingScheme (DoubleScalingScheme scheme);
} // ScalingSchemeVisitor interface

////////////////////////////////////////////////////////////////////////

