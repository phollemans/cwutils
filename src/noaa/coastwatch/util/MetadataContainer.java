////////////////////////////////////////////////////////////////////////
/*
     FILE: MetadataContainer.java
  PURPOSE: Abstract class to set the functionality of all metadata
           containers.
   AUTHOR: Peter Hollemans
     DATE: 2004/09/29
  CHANGES: 2004/10/05, PFH, added clone()

  CoastWatch Software Library and Utilities
  Copyright 2004, USDOC/NOAA/NESDIS CoastWatch

*/
////////////////////////////////////////////////////////////////////////

// Package
// -------
package noaa.coastwatch.util;

// Imports
// -------
import java.util.*;

/**
 * The <code>MetadataContainer</code> class is designed to be extended
 * by other classes that need to hold user metadata in the form of a
 * mapping from attribute name to attribute value.  The metadata map
 * is available to the user using a get method.  This is an
 * alternative to extending an existing <code>java.util.Map</code>
 * implementing class.
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
