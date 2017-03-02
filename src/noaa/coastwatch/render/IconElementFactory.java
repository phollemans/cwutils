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
import java.io.File;
import java.io.IOException;
import noaa.coastwatch.render.IconElement;

/**
 * The <code>IconElementFactory</code> class creates
 * <code>IconElement</code> objects from either a user-specified file
 * or from built in images.
 *
 * @author Peter Hollemans
 * @since 3.1.9
 */
public class IconElementFactory { 

  ////////////////////////////////////////////////////////////

  /**
   * Creates a new <code>IconElement</code> from the specified icon.
   * If the named icon is from the predefined icon resources, it is
   * used as such.  Otherwise, it is assumed that the icon is a file
   * name that should be read.
   *
   * @param icon the icon resource or file name.
   *
   * @return the new image icon.
   *
   * @throws IOException if the icon could not be created from the
   * icon resources or from a file.
   */
  public static IconElement create (
    String icon
  ) throws IOException {

    // Create from resources
    // ---------------------
    try { 
      String ext = (icon.indexOf ('.') != -1 ? "" : ".gif");
      return (new IconElement (icon + ext)); 
    } // try
    catch (IOException e) { }

    // Create from file
    // ----------------
    try { 
      return (new IconElement (new File (icon)));
    } // try
    catch (IOException e) {
      throw new IOException ("Cannot create icon for '" + icon + "'");
    } // catch

  } // create

  ////////////////////////////////////////////////////////////

} // IconElementFactory class

////////////////////////////////////////////////////////////////////////
