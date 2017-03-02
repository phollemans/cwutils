////////////////////////////////////////////////////////////////////////
/*

     File: Palette.java
   Author: Peter Hollemans
     Date: 2002/07/23

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
package noaa.coastwatch.render;

// Imports
// -------
import java.awt.Color;
import java.awt.image.IndexColorModel;

/**
 * The <code>Palette</code> allows the user to associate a name and
 * index color model together as a palette and perform manipulations
 * on the palette colors.
 *
 * @author Peter Hollemans
 * @since 3.1.1
 */
public class Palette { 

  // Variables
  // ---------
  /** The color palette name. */
  private String name;

  /** The color palette index color model. */
  private IndexColorModel model; 

  ////////////////////////////////////////////////////////////

  /** Gets the palette name. */
  public String getName () { return (name); }

  ////////////////////////////////////////////////////////////

  /** Gets the palette model data. */
  public IndexColorModel getModel () { return (model); }

  ////////////////////////////////////////////////////////////

  /**
   * Constructs a new palette from the specified parameters.
   *
   * @param name the color palette name.
   * @param model the color palette model.
   */
  public Palette (
    String name,
    IndexColorModel model
  ) {

    this.name = name;
    this.model = model;
   
  } // Palette constructor

  ////////////////////////////////////////////////////////////

  /**
   * Remaps a color palette to a new number of colors.
   *
   * @param length the size of the new palette.
   *
   * @return a new palette of the specified size.
   */
  public Palette remap (
    int length
  ) {

    // Create new byte arrays
    // ----------------------
    byte[] r = new byte[length];
    byte[] g = new byte[length];
    byte[] b = new byte[length];

    // Remap original values
    // ---------------------
    int mapSize = model.getMapSize();
    float inc = (float) mapSize/length;
    for (int i = 0; i < length; i++) {
      int index = Math.round (i*inc);
      if (index > mapSize-1) index = mapSize-1;
      r[i] = (byte) model.getRed (index);
      g[i] = (byte) model.getGreen (index);
      b[i] = (byte) model.getBlue (index);
    } // for

    return (new Palette (this.name, new IndexColorModel (8, length, r, g, b)));

  } // remap

  ////////////////////////////////////////////////////////////

  /**
   * Adds a number of colors to the end of the palette.  The
   * palette may be extended up to the maximum of 256 colors.
   *
   * @param colors an array of color objects to add.
   */ 
  public void add (
    Color[] colors
  ) {

    // Calculate new length
    // --------------------
    int length = model.getMapSize();
    if (length == 256) return;
    int newLength = length + colors.length;
    if (newLength > 256) newLength = 256;

    // Get existing colors
    // -------------------
    byte[] r = new byte[newLength];
    byte[] g = new byte[newLength];
    byte[] b = new byte[newLength];
    model.getReds (r);    
    model.getGreens (g);    
    model.getBlues (b);    

    // Add new colors
    // --------------
    for (int i = length; i < newLength; i++) {
      r[i] = (byte) colors[i-length].getRed();
      g[i] = (byte) colors[i-length].getGreen();
      b[i] = (byte) colors[i-length].getBlue();
    } // for

    // Create new model
    // ----------------
    model = new IndexColorModel (8, newLength, r, g, b);

  } // add

  ////////////////////////////////////////////////////////////

  /**
   * Adds a single color to the end of the palette.  The
   * palette may be extended up to the maximum of 256 colors.
   *
   * @param color the color to add.
   *
   * @see #add(Color[])
   */ 
  public void add (
    Color color
  ) {

    add (new Color[] {color});

  } // add

  ////////////////////////////////////////////////////////////

  /** Indicates whether some other object is "equal to" this one. */
  public boolean equals (
    Object obj
  ) { 

    if (!(obj instanceof Palette)) return (false);
    Palette paletteObj = (Palette) obj;
    if (!name.equals (paletteObj.name)) return (false);
    if (!model.equals (paletteObj.model)) return (false);
    return (true);

  } // equals 

  ////////////////////////////////////////////////////////////

} // Palette class

////////////////////////////////////////////////////////////////////////
