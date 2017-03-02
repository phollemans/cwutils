////////////////////////////////////////////////////////////////////////
/*

     File: MetadataContainer.java
   Author: Peter Hollemans
     Date: 2004/09/29

  CoastWatch Software Library and Utilities
  Copyright (c) 2004 National Oceanic and Atmospheric Administration
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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The <code>MetadataContainer</code> class is designed to be extended
 * by other classes that need to hold user metadata in the form of a
 * mapping from attribute name to attribute value.  The metadata map
 * is available to the user using a get method.  This is an
 * alternative to extending an existing <code>java.util.Map</code>
 * implementing class.
 *
 * @author Peter Hollemans
 * @since 3.1.8
 */
public class MetadataContainer
  implements Cloneable {

  // Variables
  // ---------

  /** The private map of user metadata. */
  private Map metadataMap = new LinkedHashMap();

  ////////////////////////////////////////////////////////////

  /** Gets the user metadata map. */
  public Map getMetadataMap () { return (metadataMap); }

  ////////////////////////////////////////////////////////////

  public Object clone () {

    try {
      MetadataContainer container = (MetadataContainer) super.clone();
      container.metadataMap = new LinkedHashMap (this.metadataMap);
      return (container);
    } // try
    catch (CloneNotSupportedException e) {
      return (null);
    } // catch

  } // clone

  ////////////////////////////////////////////////////////////

} // MetadataContainer class

////////////////////////////////////////////////////////////////////////
