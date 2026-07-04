////////////////////////////////////////////////////////////////////////
/*

     File: PackingSchemeVisitor.java
   Author: Peter Hollemans
     Date: 2017/11/25

  CoastWatch Software Library and Utilities
  Copyright (c) 2017 National Oceanic and Atmospheric Administration
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
 * The <code>PackingSchemeVisitor</code> interface is implemented by any class
 * that participates in the visitor pattern to perform operations on
 * {@link PackingScheme} instances.
 *
 * @author Peter Hollemans
 * @since 3.4.0
 */
public interface PackingSchemeVisitor {

  /**
   * Visits a float packing scheme.
   *
   * @param scheme the packing scheme to visit.
   */
  public void visitFloatPackingScheme (FloatPackingScheme scheme);

  /**
   * Visits a double packing scheme.
   *
   * @param scheme the packing scheme to visit.
   */
  public void visitDoublePackingScheme (DoublePackingScheme scheme);

} // PackingSchemeVisitor interface

////////////////////////////////////////////////////////////////////////
