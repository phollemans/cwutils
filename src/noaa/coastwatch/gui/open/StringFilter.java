////////////////////////////////////////////////////////////////////////
/*

     File: StringFilter.java
   Author: Peter Hollemans
     Date: 2005/06/30

  CoastWatch Software Library and Utilities
  Copyright (c) 2005 National Oceanic and Atmospheric Administration
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
package noaa.coastwatch.gui.open;

/**
 * The <code>StringFilter</code> interface specifies methods for
 * filtering an input string to an output string.
 *
 * @author Peter Hollemans
 * @since 3.2.0
 */
public interface StringFilter {

  /** Filters the input string. */
  public String filter (String input);

} // StringFilter interface

////////////////////////////////////////////////////////////////////////
