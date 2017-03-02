////////////////////////////////////////////////////////////////////////
/*

     File: Encodable.java
   Author: Peter Hollemans
     Date: 2002/06/13

  CoastWatch Software Library and Utilities
  Copyright (c) 2002 National Oceanic and Atmospheric Administration
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

/**
 * An encodable object is one whose representation can be encoded in a
 * data structure or array, for example as a number of integers or
 * floating point values.  The idea is that an encodable object may
 * have its encoding written to a file or data stream which will later
 * be used to recreate the object.  Normally this procedure is only
 * necessary if the encoding is more compact or less complicated than
 * the information used in the original object constructor.  To allow
 * for maximum flexibility, the encoding is specified using an object
 * whose structure must be fully documented by the implementing class.
 *
 * @author Peter Hollemans
 * @since 3.1.0
 */
public interface Encodable {

  ////////////////////////////////////////////////////////////

  /**
   * Gets an encoded representation of this object.
   *
   * @return the object encoding.
   *
   * @see #useEncoding
   */
  public Object getEncoding ();

  ////////////////////////////////////////////////////////////

  /**
   * Uses an encoded representation of this object to recreate the
   * object contents.
   *
   * @param obj the object encoding.
   *
   * @see #getEncoding
   */
  public void useEncoding (
    Object obj
  );

  ////////////////////////////////////////////////////////////

} // Encodable class

////////////////////////////////////////////////////////////////////////
