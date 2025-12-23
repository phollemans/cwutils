////////////////////////////////////////////////////////////////////////
/*

     File: IconElementFactory.java
   Author: Peter Hollemans
     Date: 2005/02/14

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
package noaa.coastwatch.render;

// Imports
// -------
import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;

import noaa.coastwatch.render.IconElement;

/**
 * The <code>IconElementFactory</code> class creates
 * <code>IconElement</code> objects from either a user-specified file
 * or from built in resource images.
 *
 * @author Peter Hollemans
 * @since 3.1.9
 */
public class IconElementFactory { 

  /** The map of predefined icon resources. */
  private Map<String, String> resourceMap;

  /** The singleton instance of this class. */
  private static IconElementFactory instance;

  ////////////////////////////////////////////////////////////

  public static IconElementFactory getInstance() {
    if (instance == null) instance = new IconElementFactory();
    return (instance);
  } // getInstance

  ////////////////////////////////////////////////////////////

  /**
   * Gets the list of names for the built-in icon resource images.
   * 
   * @return the list of names for use in the {@link #create} method.
   * 
   * @since 3.7.1
   */
  public List<String> getResourceNames() {

    return (List.copyOf (resourceMap.keySet()));

  } // getResourceNames

  ////////////////////////////////////////////////////////////

  protected IconElementFactory () {

    resourceMap = new LinkedHashMap<>();
    resourceMap.put ("NOAA", "logos/noaa.png");
    resourceMap.put ("NASA", "logos/nasa.gif");
    resourceMap.put ("NWS", "logos/nws.gif");
    resourceMap.put ("DOC", "logos/doc.gif");

  } // IconElementFactory

  ////////////////////////////////////////////////////////////

  /**
   * Gets the default icon name to be used in plot legends.
   * 
   * @return the default icon name.
   *
   * @see #getResourceNames
   * 
   * @since 3.7.1
   */
  public String getDefaultIcon() { return ("NOAA"); }

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new {@link noaa.coastwatch.render.IconElement} from the 
   * specified icon name. If the icon name is not one of the predefined 
   * icon resource names, it is assumed that the icon is a file name that 
   * should be read.
   *
   * @param icon the icon resource name or file name.
   *
   * @return the new image icon.
   *
   * @throws IOException if the icon could not be created from the
   * icon resources or from a file.
   * 
   * @see #getDefaultIcon
   * @see #getResourceNames
   * 
   * @since 3.7.1
   */
  public IconElement create (
    String icon
  ) throws IOException {

    var file = resourceMap.get (icon);
    InputStream stream = null;
    if (file != null) stream = getClass().getResourceAsStream (file);
    else stream = new FileInputStream (icon);

    return (IconElement.create (stream));

  } // create

  ////////////////////////////////////////////////////////////

} // IconElementFactory class

////////////////////////////////////////////////////////////////////////
